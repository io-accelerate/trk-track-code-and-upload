package io.accelerate.tracking.app.commands;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterDescription;
import com.beust.jcommander.Parameters;
import io.accelerate.tracking.app.events.ExternalEventServerThread;
import io.accelerate.tracking.app.logging.LocalFileLogging;
import io.accelerate.tracking.app.sourcecode.NoOpSourceCodeThread;
import io.accelerate.tracking.app.sourcecode.SourceCodeRecordingThread;
import io.accelerate.tracking.app.tasks.*;
import io.accelerate.tracking.app.upload.*;
import io.accelerate.tracking.app.util.DiskSpaceUtil;
import io.accelerate.tracking.sync.credentials.AWSSecretProperties;
import io.accelerate.tracking.sync.sync.progress.UploadStatsProgressListener;
import org.slf4j.Logger;

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

import static org.slf4j.LoggerFactory.getLogger;

@Parameters(commandDescription = "Start tracking source code and uploading")
public class TrackAndUploadCommand implements HasHelp {
    private static final Logger log = getLogger(TrackAndUploadCommand.class);
    private static final DateTimeFormatter fileTimestampFormatter = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss");

    @Parameter(names = {"-h", "--help"}, help = true, description = "Show help for this command")
    private boolean help;

    @Parameter(names = {"--debug"}, description = "Enable verbose debug output")
    private boolean debug = false;

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

    //~~ Test helpers

    @Parameter(names = "--soft-stop", description = "Attempt to stop without killing the JVM")
    private boolean doSoftStop = false;

    @Override
    public boolean isHelpRequested() {
        return help;
    }
    
    public void run() {
        if (!DiskSpaceUtil.hasEnoughFreeDiskspace(minimumRequiredDiskspaceInGB)) {
            System.exit(-1);
        }
        
        try {
            // Prepare source folder
            createMissingParentDirectories(localStorageFolder);
            removeOldLocks(localStorageFolder);
            LocalFileLogging localFileLogging = new LocalFileLogging(localStorageFolder);
            localFileLogging.start();


            // Prepare remote destination
            boolean doNotSync = "none".equals(configFile);
            RemoteDestination uploadDestination;
            if (doNotSync) {
                uploadDestination = new NoOpDestination();
            } else {
                AWSSecretProperties awsSecretProperties = AWSSecretProperties
                        .fromPlainTextFile(Paths.get(configFile));
                uploadDestination = new S3BucketDestination(awsSecretProperties.createClient(),
                        awsSecretProperties.getS3Bucket(),
                        awsSecretProperties.getS3Prefix());
            }


            // Validate destination
            log.info("Start S3 Sync session");
            uploadDestination.startS3SyncSession();

            // Timestamp
            String timestamp = LocalDateTime.now().format(fileTimestampFormatter);

            // Source code recording
            boolean doNotTrackSourcecode = "none".equals(localSourceCodeFolder);
            MonitoredBackgroundTask sourceCodeRecordingTask;
            if (doNotTrackSourcecode) {
                sourceCodeRecordingTask = new NoOpSourceCodeThread();
            } else {
                Path sourceCodeFolder = Paths.get(localSourceCodeFolder);
                Path sourceCodeRecordingFile = Paths.get(
                        localStorageFolder,
                        String.format("sourcecode_%s.srcs", timestamp)
                );
                sourceCodeRecordingTask = new SourceCodeRecordingThread(sourceCodeFolder, sourceCodeRecordingFile);
            }

            // Start processing
            runAllTasks(localStorageFolder,
                    listeningHost,
                    listeningPort,
                    localFileLogging,
                    uploadDestination,
                    sourceCodeRecordingTask
            );

            // Stop the additional file logging
            localFileLogging.stop();

            // Stop the S3 Sync session from above
            log.info("Stop S3 Sync session");
            uploadDestination.stopS3SyncSession();
        } catch (DestinationOperationException e) {
            if (debug) {
                log.error("User does not have enough permissions to upload.", e);
            } else {
                log.error("User does not have enough permissions to upload. Reason: {}", e.getMessage());
            }
        } catch (Exception e) {
            log.error("Exception encountered. Stopping now.", e);
        } finally {
            boolean hardStop = !doSoftStop;
            if (hardStop) {
                // Forcefully stop. A problem with Jetty finalisation might prevent the JVM from stopping
                Runtime.getRuntime().halt(0);
            }
        }
    }

    // ~~~~~ The main execution logic

    private static void runAllTasks(String localStorageFolder,
                    String listeningHost,
                    int listeningPort,
                    LocalFileLogging localFileLogging, RemoteDestination remoteDestination,
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
        localFileLogging.forceRotation(); // <-- to close the current log file and get it to upload cleanly
        remoteSyncTask.finalRun();
        metricsReportingTask.cancel();

        // Join the event thread
        externalEventServerThread.join();
        log.warn("~~~~~~ Stopped ~~~~~~");
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
