package io.accelerate.tracking.app;

public class TaskNotExecutableException extends Exception {
    public TaskNotExecutableException(String message, Throwable cause) {
        super(message, cause);
    }
}
