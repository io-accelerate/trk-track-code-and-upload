package io.accelerate.tracking.app;

import org.slf4j.Logger;

import java.time.Duration;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import static org.slf4j.LoggerFactory.getLogger;

class HealthCheckTask {
    private static final Logger log = getLogger(HealthCheckTask.class);
    private final Timer metricsTimer;
    private final List<Stoppable> serviceThreads;

    HealthCheckTask(List<Stoppable> recordingThreads) {
        this.metricsTimer = new Timer("HealthCheck");
        this.serviceThreads = recordingThreads;
    }

    void scheduleHealthCheckEvery(Duration delayBetweenRuns) {
        metricsTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    if (!allServiceThreadsAlive()) {
                        log.warn("One or more threads are not running correctly. Stopping all threads.");
                        stopAllThreads();
                    }
                } catch (Exception e) {
                    log.error("Unexpected problem running health checks: {}", e.getMessage());
                }
            }
        }, 0, delayBetweenRuns.toMillis());
    }

    private boolean allServiceThreadsAlive() {
        boolean isHealthy = true;
        for (Stoppable serviceThread : serviceThreads) {
            isHealthy &= serviceThread.isAlive();
        }
        return isHealthy;
    }

    private void stopAllThreads() {
        serviceThreads.forEach(stoppable -> {
            try {
                stoppable.signalStop();
            } catch (Exception e) {
                log.error("Failed to stop thread of type "+ stoppable.getClass().getSimpleName(), e);
            }
        });
    }

    void cancel() {
        log.info("Stopping health check timer");
        metricsTimer.cancel();
    }
}
