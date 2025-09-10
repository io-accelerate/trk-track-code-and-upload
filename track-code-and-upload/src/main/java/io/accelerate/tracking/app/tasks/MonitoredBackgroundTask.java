package io.accelerate.tracking.app.tasks;

import io.accelerate.tracking.app.events.ExternalEventListener;

public interface MonitoredBackgroundTask extends Stoppable, MonitoredSubject, ExternalEventListener {

    void start();
}
