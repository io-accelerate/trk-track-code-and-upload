package io.accelerate.tracking.app.sourcecode;

import org.slf4j.Logger;
import io.accelerate.tracking.code.metrics.SourceCodeRecordingMetricsCollector;

import java.util.concurrent.TimeUnit;

class SourceCodeRecordingStatus  {

    private SourceCodeRecordingMetricsCollector sourceCodeRecordingMetricsCollector;

    SourceCodeRecordingStatus(SourceCodeRecordingMetricsCollector sourceCodeRecordingMetricsCollector) {
        this.sourceCodeRecordingMetricsCollector = sourceCodeRecordingMetricsCollector;
    }

    boolean isActive() {
        return sourceCodeRecordingMetricsCollector.isCurrentlyRecording();
    }

    void displayErrors(Logger log) {
        // No error
    }

    void displayMetrics(StringBuilder displayBuffer) {
        displayBuffer.append(
                String.format("%2d source capture%s, %3d ms/capture",
                        sourceCodeRecordingMetricsCollector.getTotalSnapshots(),
                        maybePlural(sourceCodeRecordingMetricsCollector.getTotalSnapshots()),
                        TimeUnit.NANOSECONDS.toMillis(sourceCodeRecordingMetricsCollector.getLastSnapshotProcessingTimeNano()))
        );
    }

    private static String maybePlural(long value) {
        return value > 1 ? "s" : "";
    }
}
