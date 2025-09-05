package io.accelerate.tracking.app;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public class ManualTrackCodeAndUploadAppTest {

    /**
     * Start the app
     *
     * Check output, you should see:
     *  - recording metrics for frames
     *  - once every 3 minutes you see a snapshot being taken
     *  - once every 5 minutes you should see a SyncTask uploading files
     *
     * Stop the recording (CTRL+C), you should see:
     *  - a log message saying that the screen recording is stopping
     *  - a log message saying that the source code recording is stopping
     *  - upload messages for the video and the log file
     *
     * Download the sourceStream file, and use the cli tool to:
     *  - list the snapshots
     *  - convert the file to a git repo
     */
    @Disabled("Manual acceptance")
    @Test
    public void record_and_upload() {
    }


    /**
     *
     * Use Minio as S3 alternative
     * Prepare the localSource tempFolder
     *
     * Start the app
     *
     * Check storage tempFolder, you should see:
     *  - a sourceStream file with timestamp plus a corresponding .lock file
     *  - a log file plus a corresponding .lock file
     *
     * Stop the recording (CTRL+C)
     *
     * Check storage tempFolder, you should see:
     *  - no .lock file
     *
     * Check AWS S3, you should see:
     *  - a sourceStream file
     *  - a log file*
     *
     * Download the sourceStream file, and use the cli tool to:
     *  - list the snapshots
     *  - convert the file to a git repo
     *
     * Check the logs:
     *  - you should see Starting messages, Metrics, Upload
     */

    @Disabled("TODO write the test")
    @Test
    public void record_and_upload_X() {
    }
}