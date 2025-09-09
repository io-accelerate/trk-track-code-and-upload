package io.accelerate.tracking.app;

import org.slf4j.Logger;

import java.time.Duration;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import static org.slf4j.LoggerFactory.*;

class MetricsReportingTask {
    private static final Logger log = getLogger(MetricsReportingTask.class);
    private final Timer metricsTimer;
    private final StringBuilder displayBuffer;
    private final List<MonitoredSubject> monitoredSubjects;
    private int tick;

    MetricsReportingTask(List<MonitoredSubject> monitoredSubjects) {
        this.metricsTimer = new Timer("Metrics");
        this.displayBuffer = new StringBuilder();
        this.monitoredSubjects = monitoredSubjects;
        this.tick = 0;
    }

    void scheduleReportMetricsEvery(Duration delayBetweenRuns) {
        metricsTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    displayErrors();
                    displayMetrics();
                } catch (Exception e) {
                    log.error("Unexpected problem while gathering metrics: {}", e.getMessage());
                }
            }
        }, 0, delayBetweenRuns.toMillis());
    }

    private void displayErrors() {
        for (MonitoredSubject monitoredSubject : monitoredSubjects) {
            monitoredSubject.displayErrors(log);
        }
    }

    private void displayMetrics() {
        displayBuffer.setLength(0);
        displayBuffer.append(String.format("tick %4d", tick));
        tick ++;
        
        for (MonitoredSubject monitoredSubject : monitoredSubjects) {
            if (monitoredSubject.isActive()) {
                if (!displayBuffer.isEmpty()) {
                    displayBuffer.append(" | ");
                }

                monitoredSubject.displayMetrics(displayBuffer);
            }
        }
        log.info(displayBuffer.toString());
    }

    void cancel() {
        metricsTimer.cancel();
    }
}
