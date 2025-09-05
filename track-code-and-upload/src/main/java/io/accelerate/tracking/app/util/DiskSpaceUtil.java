package io.accelerate.tracking.app.util;

import java.io.IOException;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class DiskSpaceUtil {
    public static long getFreeSpaceKb(String directory) throws IOException, IOException {
        Path path = Paths.get(directory);
        FileStore fileStore = Files.getFileStore(path);
        return fileStore.getUsableSpace() / 1024; // Convert bytes to kilobytes
    }
}
