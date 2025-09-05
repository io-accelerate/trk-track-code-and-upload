package io.accelerate.tracking.app;

import io.accelerate.tracking.app.events.ExternalEventListener;

public interface MonitoredBackgroundTask extends Stoppable, MonitoredSubject, ExternalEventListener {

    void start();
}
