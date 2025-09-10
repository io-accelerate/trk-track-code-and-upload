package io.accelerate.tracking.app.logging;

import ch.qos.logback.classic.LoggerContext;

import static org.slf4j.LoggerFactory.getILoggerFactory;

public class LocalFileLogging {

    private final String localStorageFolder;

    public LocalFileLogging(String localStorageFolder) {
        this.localStorageFolder = localStorageFolder;
    }

    public void start() {
        LoggerContext loggerContext = (LoggerContext) getILoggerFactory();
        LockableFileLoggingAppender.addToContext(loggerContext, localStorageFolder);
    }

    public void forceRotation() {
        stop();
        start();
    }

    public void stop() {
        LoggerContext loggerContext = (LoggerContext) getILoggerFactory();
        LockableFileLoggingAppender.removeFromContext(loggerContext);
    }
}

