package io.accelerate.tracking.app.logging;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.FileAppender;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static java.nio.file.StandardOpenOption.CREATE;

public class LockableFileLoggingAppender<E> extends FileAppender<E> {
    private static final String APPENDER_NAME = "REMOTE_SYNC_FILE";

    public static void addToContext(LoggerContext loggerContext, String localStorageFolder) {
        LockableFileLoggingAppender<ILoggingEvent> fileAppender = new LockableFileLoggingAppender<>();
        fileAppender.setContext(loggerContext);
        fileAppender.setName(APPENDER_NAME);

        // set the file name
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss"));
        Path path = Paths.get(localStorageFolder, "track-code-and-upload-" + timestamp + ".log");
        fileAppender.setFile(path.toAbsolutePath().toString());
        PatternLayoutEncoder encoder = new PatternLayoutEncoder();
        encoder.setContext(loggerContext);
        encoder.setPattern("%d{HH:mm:ss.SSS} %-5level %-15([%thread]) - %msg%n");
        encoder.start();

        fileAppender.setEncoder(encoder);
        fileAppender.start();

        // attach the rolling file appender to the logger of your choice
        Logger logbackLogger = loggerContext.getLogger(Logger.ROOT_LOGGER_NAME);
        logbackLogger.addAppender(fileAppender);
    }

    public static void removeFromContext(LoggerContext loggerContext) {
        Logger logbackLogger = loggerContext.getLogger(Logger.ROOT_LOGGER_NAME);
        Appender<ILoggingEvent> appender = logbackLogger.getAppender(APPENDER_NAME);
        if (appender != null) {
            LockableFileLoggingAppender<ILoggingEvent> lockableAppender = (LockableFileLoggingAppender<ILoggingEvent>) appender;
            logbackLogger.detachAppender(lockableAppender);
            lockableAppender.stop();
        }
    }

    @Override
    public void openFile(String file_name) throws IOException {
        //create *.lock file
        Files.write(lockFor(file_name), new byte[0], CREATE);
        super.openFile(file_name);
    }

    @Override
    public void stop() {
        super.stop();
        try {
            Files.delete(lockFor(fileName));
        } catch (IOException ignored) {}
    }

    //~~~~ Helper

    private static Path lockFor(String first) {
        return Paths.get(first + ".lock");
    }
}
