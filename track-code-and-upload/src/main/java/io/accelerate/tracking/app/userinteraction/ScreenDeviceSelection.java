package io.accelerate.tracking.app.userinteraction;

import java.awt.*;

public class ScreenDeviceSelection {

    public static int askUserToSelectScreen(GraphicsDevice[] screenDevices) {
        Prompt prompt = new Prompt();

        prompt.displayOnScreen("Multiple displays detected:");
        for (int i = 0; i < screenDevices.length; i++) {
            GraphicsDevice screenDevice = screenDevices[i];
            Rectangle bounds = screenDevice.getDefaultConfiguration().getBounds();
            String msg = "Screen " + (i + 1) + " - " + bounds.width + "x" + bounds.height;
            prompt.displayOnScreen(msg);
        }

        return prompt.askForIntegerInput("Please choose the screen you wish to record. Type the screen number: ",
                0, screenDevices.length);
    }


    public static int numDisplays() {
        return GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices().length;
    }

    public static GraphicsDevice[] getScreenDevices() {
        return GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices();
    }
}
