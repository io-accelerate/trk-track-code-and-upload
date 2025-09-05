package io.accelerate.tracking.app.util;

import org.slf4j.Logger;
import io.accelerate.tracking.app.MonitoredBackgroundTask;

public class NoOpThread extends Thread implements MonitoredBackgroundTask {
    private boolean isRunning;
    private int tick;
    private NoOpMessageProvider noOpMessageProvider;
    private String lastReceivedExternalEvent;


    public NoOpThread(NoOpMessageProvider noOpMessageProvider) {
        this.noOpMessageProvider = noOpMessageProvider;
        isRunning = true;
        tick = 0;
        this.lastReceivedExternalEvent = "";
    }

    @Override
    public void run() {
        while(isRunning) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public boolean isActive() {
        return isRunning;
    }

    @Override
    public void displayErrors(Logger log) {
        //No errors
    }

    @Override
    public void displayMetrics(StringBuilder displayBuffer) {
        tick += 1;
        displayBuffer.append(noOpMessageProvider.messageFor(tick, lastReceivedExternalEvent));
    }

    @Override
    public void signalStop() {
        isRunning = false;
    }

    @Override
    public void onExternalEvent(String eventPayload) {
        this.lastReceivedExternalEvent = eventPayload;
    }
}
