package io.accelerate.tracking.app;

import org.slf4j.Logger;

public interface MonitoredSubject {

    /**
     * Flag to indicate that the thread is actively doing work (recording/writing)
     */
    boolean isActive();

    void displayErrors(Logger log);
    void displayMetrics(StringBuilder displayBuffer);
}
