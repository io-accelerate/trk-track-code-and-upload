package io.accelerate.tracking.app.tasks;

public interface Stoppable {

    boolean isAlive();

    void join() throws InterruptedException;

    void signalStop() throws Exception;
}
