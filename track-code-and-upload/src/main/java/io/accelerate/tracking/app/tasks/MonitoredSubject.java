package io.accelerate.tracking.app.tasks;

import org.slf4j.Logger;

public interface MonitoredSubject {

    /**
     * Flag to indicate that the thread is actively doing work
     */
    boolean isActive();

    void displayErrors(Logger log);
    void displayMetrics(StringBuilder displayBuffer);
}
