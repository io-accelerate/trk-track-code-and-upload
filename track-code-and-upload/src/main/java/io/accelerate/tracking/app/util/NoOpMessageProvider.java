package io.accelerate.tracking.app.util;

public interface NoOpMessageProvider {
    String messageFor(int tick, String lastReceivedExternalEvent);
}
