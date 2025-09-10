package io.accelerate.tracking.app.util;

import io.accelerate.tracking.app.TrackCodeAndUploadApp;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.slf4j.LoggerFactory.getLogger;

public class DiskSpaceUtil {
    private static final int ONE_MB = 1024;
    private static final int ONE_GB = 1024 * ONE_MB;
    
    private static final Logger log = getLogger(DiskSpaceUtil.class);

    public static boolean hasEnoughFreeDiskspace(long minimumRequiredDiskspaceHumanReadable) {
        if (minimumRequiredDiskspaceHumanReadable == 0) {
            // Exit early if we don't require any disk space
            log.info("Skipping diskspace check and proceeding to run the app.");
            return true;
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
            return false;
        }
        
        return true;
    }

    public static long getAvailableDiskspaceFor(String directory) {
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
    
    public static long getFreeSpaceKb(String directory) throws IOException, IOException {
        Path path = Paths.get(directory);
        FileStore fileStore = Files.getFileStore(path);
        return fileStore.getUsableSpace() / 1024; // Convert bytes to kilobytes
    }
}
