package io.accelerate.tracking.app.sourcecode;

import org.slf4j.Logger;
import io.accelerate.tracking.code.content.CopyFromDirectorySourceCodeProvider;
import io.accelerate.tracking.code.metrics.SourceCodeRecordingMetricsCollector;
import io.accelerate.tracking.code.record.SourceCodeRecorder;
import io.accelerate.tracking.code.time.SystemMonotonicTimeSource;
import io.accelerate.tracking.app.MonitoredBackgroundTask;

import java.nio.file.Path;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;

import static org.slf4j.LoggerFactory.*;

public class SourceCodeRecordingThread extends Thread implements MonitoredBackgroundTask {
    private static final Duration MAX_RECORDING_DURATION = Duration.of(12, ChronoUnit.HOURS);
    private static final Logger log = getLogger(SourceCodeRecordingThread.class);

    private final SourceCodeRecorder sourceCodeRecorder;
    private final SourceCodeRecordingStatus sourceCodeRecordingStatus;

    public SourceCodeRecordingThread(Path sourceCodeFolder, Path sourceCodeRecordingFile) {
        super("SourceRec");

        SourceCodeRecordingMetricsCollector sourceCodeRecordingMetricsCollector = new SourceCodeRecordingMetricsCollector();
        sourceCodeRecordingStatus = new SourceCodeRecordingStatus(sourceCodeRecordingMetricsCollector);
        CopyFromDirectorySourceCodeProvider sourceCodeProvider = new CopyFromDirectorySourceCodeProvider(
                sourceCodeFolder, 1);
        sourceCodeRecorder = new SourceCodeRecorder.Builder(sourceCodeProvider, sourceCodeRecordingFile)
                .withTimeSource(new SystemMonotonicTimeSource())
                .withSnapshotEvery(3, TimeUnit.MINUTES)
                .withKeySnapshotSpacing(10)
                .withRecordingListener(sourceCodeRecordingMetricsCollector)
                .build();
    }

    @Override
    public void run() {
        try {
            sourceCodeRecorder.start(MAX_RECORDING_DURATION);
            sourceCodeRecorder.close();
        } catch (Exception e) {
            log.error("SourceCode recorder encountered exception. Recording has been stopped.", e);
        }
    }

    @Override
    public void signalStop() {
        sourceCodeRecorder.stop();
    }


    // ~~~~ Implement the monitored interface

    @Override
    public boolean isActive() {
        return sourceCodeRecordingStatus.isActive();
    }

    @Override
    public void displayErrors(Logger log) {
        sourceCodeRecordingStatus.displayErrors(log);
    }

    @Override
    public void displayMetrics(StringBuilder displayBuffer) {
        sourceCodeRecordingStatus.displayMetrics(displayBuffer);
    }

    // ~~~~ Implement the external events interface

    @Override
    public void onExternalEvent(String eventPayload) {
        sourceCodeRecorder.tagCurrentState(eventPayload);
    }
}
