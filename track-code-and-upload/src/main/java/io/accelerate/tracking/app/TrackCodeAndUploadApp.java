package io.accelerate.tracking.app;

import ch.qos.logback.classic.LoggerContext;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterDescription;
import com.beust.jcommander.ParameterException;
import io.accelerate.tracking.app.upload.*;
import io.accelerate.tracking.code.record.SourceCodeRecorder;
import org.slf4j.Logger;
import io.accelerate.tracking.app.events.ExternalEventServerThread;
import io.accelerate.tracking.app.logging.LockableFileLoggingAppender;
import io.accelerate.tracking.app.sourcecode.NoOpSourceCodeThread;
import io.accelerate.tracking.app.sourcecode.SourceCodeRecordingThread;
import io.accelerate.tracking.app.util.DiskSpaceUtil;
import io.accelerate.tracking.sync.credentials.AWSSecretProperties;
import io.accelerate.tracking.sync.sync.progress.UploadStatsProgressListener;
import software.amazon.awssdk.auth.credentials.AnonymousCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.slf4j.LoggerFactory.*;

public class TrackCodeAndUploadApp {
    private static final Logger log = getLogger(TrackCodeAndUploadApp.class);
    private static final int ONE_GB = 1024 * 1024;
    private static final int ONE_MB = 1024;

    private static class Params {
        
        //~~ Discovery  
        
        @Parameter(names = {"--help"}, help = true, description = "Displays help information")
        private boolean help = false;

        //~~ Mandatory parameters
        
        @Parameter(names = {"--store"}, required = true, description = "The folder that will cache the code snapshots")
        private String localStorageFolder;

        @Parameter(names = {"--config"}, required = true, description = "The file containing the AWS parameters")
        private String configFile;

        @Parameter(names = {"--sourcecode"}, required = true, description = "The folder that contains the source code that needs to be tracked")
        private String localSourceCodeFolder;

        //~~ Minimum requirements

        @Parameter(names = {"--minimum-required-diskspace-gb"}, description = "Minimum required diskspace (in GB) on the current volume (or drive) for the app to run")
        private long minimumRequiredDiskspaceInGB = 1;

        //~~ Webserver params

        @Parameter(names = {"--listening-host"}, description = "Listening host to be used for the event server")
        private String listeningHost = "127.0.0.1";

        @Parameter(names = {"--listening-port"}, description = "Listening port to be used for the event server")
        private int listeningPort = 41375;
        
        //~~ Graceful degradation flags

        @Parameter(names = "--no-sourcecode", description = "Disable source code tracking")
        private boolean doNotTrackSourceCode = false;

        @Parameter(names = "--no-sync", description = "Do not sync target folder")
        private boolean doNotSync = false;

        //~~ Test helpers

        @Parameter(names = "--run-self-test", description = "Run some basic checks then stop")
        private boolean runSelfTest = false;

        @Parameter(names = "--soft-stop", description = "Attempt to stop without killing the JVM")
        private boolean doSoftStop = false;
    }


    public static void main(String[] args) {
        log.info("Starting the source code tracking app");

        Params params = new Params();
        JCommander jCommander = new JCommander(params);
        
        // Parse the cli args
        try {
            jCommander.parse(args);
        } catch (ParameterException e) {
            log.error(e.getMessage());
            printRequiredOnly(jCommander);
            System.exit(2);
        }

        // Check if help is requested
        if (params.help) {
            jCommander.usage(); // Display the usage information
            return; // Exit the application after showing help
        }

        checkDiskspaceRequirements(params.minimumRequiredDiskspaceInGB);

        if (params.runSelfTest) {
            runS3SanityCheck();
            SourceCodeRecorder.runSanityCheck();
            log.info("~~~~~~ Self test completed successfully ~~~~~~");
            return;
        }

        try {
            // Prepare source folder
            createMissingParentDirectories(params.localStorageFolder);
            removeOldLocks(params.localStorageFolder);
            startFileLogging(params.localStorageFolder);


            // Prepare remote destination
            boolean syncFolder = !params.doNotSync;
            RemoteDestination uploadDestination;
            if (syncFolder) {
                AWSSecretProperties awsSecretProperties = AWSSecretProperties
                        .fromPlainTextFile(Paths.get(params.configFile));
                uploadDestination = new S3BucketDestination(awsSecretProperties.createClient(),
                        awsSecretProperties.getS3Bucket(),
                        awsSecretProperties.getS3Prefix());
            } else {
                uploadDestination = new NoOpDestination();
            }


            // Validate destination
            log.info("Start S3 Sync session");
            uploadDestination.startS3SyncSession();

            // Timestamp
            String timestamp = LocalDateTime.now().format(fileTimestampFormatter);

            // Source code recording
            boolean recordSourceCode = !params.doNotTrackSourceCode;
            MonitoredBackgroundTask sourceCodeRecordingTask;
            if (recordSourceCode) {
                Path sourceCodeFolder = Paths.get(params.localSourceCodeFolder);
                Path sourceCodeRecordingFile = Paths.get(
                        params.localStorageFolder,
                        String.format("sourcecode_%s.srcs", timestamp)
                );
                sourceCodeRecordingTask = new SourceCodeRecordingThread(sourceCodeFolder, sourceCodeRecordingFile);
            } else {
                sourceCodeRecordingTask = new NoOpSourceCodeThread();
            }

            // Start processing
            run(params.localStorageFolder,
                    params.listeningHost,
                    params.listeningPort, 
                    uploadDestination,
                    sourceCodeRecordingTask
            );

            // Stop the S3 Sync session from above
            log.info("Stop S3 Sync session");
            uploadDestination.stopS3SyncSession();
        } catch (DestinationOperationException e) {
            log.error("User does not have enough permissions to upload. Reason: {}", e.getMessage());
        } catch (Exception e) {
            log.error("Exception encountered. Stopping now.", e);
        } finally {
            boolean hardStop = !params.doSoftStop;
            if (hardStop) {
                // Forcefully stop. A problem with Jetty finalisation might prevent the JVM from stopping
                Runtime.getRuntime().halt(0);
            }
        }
    }

    private static void checkDiskspaceRequirements(long minimumRequiredDiskspaceHumanReadable) {
        if (minimumRequiredDiskspaceHumanReadable == 0) {
            // Exit early if we don't require any disk space
            log.info("Skipping diskspace check and proceeding to run the app.");
            return;
        }
        
        log.info("Checking diskspace");
        
        long minimumRequiredDiskspace = minimumRequiredDiskspaceHumanReadable * ONE_GB;
        String userDirectory = System.getProperty("user.dir");
        String userDriveOrVolume = Paths.get(userDirectory).getRoot().toString();
        long availableDiskspace = getAvailableDiskspaceFor(userDriveOrVolume);
        long availableDiskspaceInGB = availableDiskspace / ONE_GB;
        float availableDiskspaceInMB = (float) (availableDiskspace / ONE_MB);
        log.info(String.format("Available disk space on the volume (or drive) '%s': %dGB (%.3fMB)", userDriveOrVolume, availableDiskspaceInGB, availableDiskspaceInMB));
        if (availableDiskspace < minimumRequiredDiskspace) {
            log.error(String.format("Sorry, you need at least %dGB of free disk space on this volume (or drive), in order to run the source code tracking app.", minimumRequiredDiskspaceHumanReadable));
            log.warn("Please make free up some disk space on this volume (or drive) and try running the source code tracking app again.");

            System.exit(-1);
        }
    }

    private static long getAvailableDiskspaceFor(String directory) {
        try {
            return DiskSpaceUtil.getFreeSpaceKb(directory);
        } catch (IOException ex) {
            throw new RuntimeException(
                    String.format("Exception when trying to fetch available " +
                            "free disk space for volume (or drive): %s, error: %s",
                            directory, ex.getMessage())
            );
        }
    }

    private static final DateTimeFormatter fileTimestampFormatter = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss");
    private static void run(String localStorageFolder,
                            String listeningHost, int listeningPort, RemoteDestination remoteDestination,
                            MonitoredBackgroundTask sourceCodeRecordingTask) throws Exception {
        List<Stoppable> serviceThreadsToStop = new ArrayList<>();
        List<MonitoredSubject> monitoredSubjects = new ArrayList<>();
        ExternalEventServerThread externalEventServerThread = new ExternalEventServerThread(listeningHost, listeningPort);

        // Start background tasks
        for (MonitoredBackgroundTask monitoredBackgroundTask:
                Collections.singletonList(sourceCodeRecordingTask)) {
            monitoredBackgroundTask.start();
            serviceThreadsToStop.add(monitoredBackgroundTask);
            monitoredSubjects.add(monitoredBackgroundTask);
            externalEventServerThread.addNotifyListener(monitoredBackgroundTask);
            externalEventServerThread.addStopListener(eventPayload -> monitoredBackgroundTask.signalStop());
        }

        // Start sync folder
        UploadStatsProgressListener uploadStatsProgressListener = new UploadStatsProgressListener();
        BackgroundRemoteSyncTask remoteSyncTask = new BackgroundRemoteSyncTask(
                localStorageFolder, remoteDestination, uploadStatsProgressListener);
        remoteSyncTask.scheduleSyncEvery(Duration.of(5, ChronoUnit.MINUTES));
        monitoredSubjects.add(new UploadStatsProgressStatus(uploadStatsProgressListener));

        // Start the metrics reporting
        MetricsReportingTask metricsReportingTask = new MetricsReportingTask(monitoredSubjects);
        metricsReportingTask.scheduleReportMetricsEvery(Duration.of(3, ChronoUnit.SECONDS));

        // Start the health check thread
        HealthCheckTask healthCheckTask = new HealthCheckTask(serviceThreadsToStop);
        healthCheckTask.scheduleHealthCheckEvery(Duration.of(3, ChronoUnit.SECONDS));
        externalEventServerThread.addStopListener(eventPayload -> healthCheckTask.cancel());

        // Start the event server
        externalEventServerThread.start();

        // Wait for the stop signal and trigger a graceful shutdown
        registerShutdownHook(serviceThreadsToStop, healthCheckTask);
        for (Stoppable stoppable : serviceThreadsToStop) {
            stoppable.join();
        }
        healthCheckTask.cancel();

        // If all are joined, signal the event thread to stop
        externalEventServerThread.signalStop();

        // Finalise the upload and cancel tasks
        forceLoggingFileRotation(localStorageFolder);
        remoteSyncTask.finalRun();
        metricsReportingTask.cancel();

        // Join the event thread
        externalEventServerThread.join();
        log.warn("~~~~~~ Stopped ~~~~~~");
        stopFileLogging();
    }

    private static void registerShutdownHook(List<Stoppable> servicesToStop, HealthCheckTask healthCheckTask) {
        final Thread mainThread = Thread.currentThread();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.warn("Shutdown signal received - please wait for the upload to complete");
            try {
                for (Stoppable stoppable : servicesToStop) {
                    stoppable.signalStop();
                }
                healthCheckTask.cancel();
            } catch (Exception e) {
                log.error("Error sending the stop signals.", e);
            }

            try {
                mainThread.join();
            } catch (InterruptedException e) {
                log.error("Could not join main thread.  Stopping now.", e);
            }
        }, "Shutdown"));
    }

    // ~~~~~ Helpers

    private static void startFileLogging(String localStorageFolder) {
        LoggerContext loggerContext = (LoggerContext) getILoggerFactory();
        LockableFileLoggingAppender.addToContext(loggerContext, localStorageFolder);
    }

    private static void forceLoggingFileRotation(String localStorageFolder) {
        stopFileLogging();
        startFileLogging(localStorageFolder);
    }

    private static void stopFileLogging() {
        LoggerContext loggerContext = (LoggerContext) getILoggerFactory();
        LockableFileLoggingAppender.removeFromContext(loggerContext);
    }


    private static void createMissingParentDirectories(String storageFolder) throws IOException {
        File folder = new File(storageFolder);
        if (folder.exists()) {
            return;
        }

        boolean folderCreated = folder.mkdirs();
        if(!folderCreated) {
            throw new IOException("Failed to created storage folder");
        }
    }

    private static void removeOldLocks(String localStorageFolder) {
        Path rootPath = Paths.get(localStorageFolder);
        try {
            //noinspection ResultOfMethodCallIgnored
            Files.walk(rootPath)
                    .filter(path -> path.getFileName().toString().endsWith(".lock"))
                    .map(Path::toFile)
                    .forEach(File::delete);
        } catch (IOException e) {
            log.error("Failed to clean old locks", e);
        }
    }

    public static void runS3SanityCheck() {
        // Touch S3 to fail fast if the service is unreachable in this environment.
        // Using anonymous creds mirrors the v1 “null creds” idea without needing real IAM.
        try (S3Client s3 = S3Client.builder()
                .region(Region.EU_WEST_2)
                .credentialsProvider(AnonymousCredentialsProvider.create())
                .build()) {

            // Cheapest probe
            s3.headBucket(HeadBucketRequest.builder()
                    .bucket("ping.s3.accelerate.io")
                    .build());
        }
    }

    private static void printRequiredOnly(JCommander jc) {
        System.err.println("Required parameters:");
        for (ParameterDescription pd : jc.getParameters()) {
            if (pd.getParameter().required()) {
                String names = String.join(", ", pd.getParameter().names());
                System.err.printf("  %-20s %s%n", names, pd.getDescription());
            }
        }
    }
}
