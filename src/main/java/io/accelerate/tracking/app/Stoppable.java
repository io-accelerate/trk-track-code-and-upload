package io.accelerate.tracking.app;

public interface Stoppable {

    boolean isAlive();

    void join() throws InterruptedException;

    void signalStop() throws Exception;
}
