package io.accelerate.tracking.app.events;

public interface ExternalEventListener {
    void onExternalEvent(String eventPayload) throws Exception;
}
