package io.accelerate.tracking.app;

import io.accelerate.tracking.app.events.ExternalEventServerThread;
import io.accelerate.tracking.app.logging.LocalFileLogging;
import io.accelerate.tracking.app.upload.BackgroundRemoteSyncTask;
import io.accelerate.tracking.app.upload.RemoteDestination;
import io.accelerate.tracking.app.upload.UploadStatsProgressStatus;
import io.accelerate.tracking.sync.sync.progress.UploadStatsProgressListener;
import org.slf4j.Logger;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.slf4j.LoggerFactory.getILoggerFactory;
import static org.slf4j.LoggerFactory.getLogger;

public class TrackAndUploadAction {
    private static final Logger log = getLogger(TrackAndUploadAction.class);


    static void run(String localStorageFolder,
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
}
