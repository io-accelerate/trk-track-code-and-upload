package io.accelerate.tracking.app.upload;

import org.slf4j.Logger;
import io.accelerate.tracking.app.tasks.MonitoredSubject;
import io.accelerate.tracking.sync.sync.progress.UploadStatsProgressListener;

import java.text.NumberFormat;

public class UploadStatsProgressStatus implements MonitoredSubject {
    private static final NumberFormat percentageFormatter = NumberFormat.getPercentInstance();
    private static final NumberFormat sizeFormatter = NumberFormat.getNumberInstance();
    private static final NumberFormat uploadSpeedFormatter = NumberFormat.getNumberInstance();

    static {
        setFormatter(percentageFormatter, 1);
        setFormatter(sizeFormatter, 2);
        setFormatter(uploadSpeedFormatter, 3);
    }

    private static void setFormatter(NumberFormat formatter, int digits) {
        formatter.setMinimumFractionDigits(digits);
        formatter.setMaximumFractionDigits(digits);
    }

    private UploadStatsProgressListener uploadStatsProgressListener;

    public UploadStatsProgressStatus(UploadStatsProgressListener uploadStatsProgressListener) {
        this.uploadStatsProgressListener = uploadStatsProgressListener;
    }

    @Override
    public boolean isActive() {
        return uploadStatsProgressListener.isCurrentlyUploading();
    }

    @Override
    public void displayErrors(Logger log) {
        // No error
    }

    @Override
    public void displayMetrics(StringBuilder displayBuffer) {
        uploadStatsProgressListener.getCurrentStats().ifPresent(fileUploadStat ->
                displayBuffer.append(
                        String.format("Uploaded %3s of %3s MB at %5s MB/sec",
                                percentageFormatter.format(fileUploadStat.getUploadRatio()),
                                sizeFormatter.format(bytes_to_mb(fileUploadStat.getTotalBytes())),
                                uploadSpeedFormatter.format(fileUploadStat.getMegabytesPerSecond()))
                )
        );
    }

    //~~~ Helpers

    private static double bytes_to_mb(double totalSize) {
        return totalSize/((double)1024*1024);
    }
}
