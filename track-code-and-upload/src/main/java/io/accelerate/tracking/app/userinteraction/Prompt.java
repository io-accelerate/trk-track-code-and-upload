package io.accelerate.tracking.app.userinteraction;

import java.io.PrintStream;
import java.util.Scanner;

class Prompt {

    private Scanner scanner;
    private PrintStream printStream;

    Prompt() {
        scanner = new Scanner(System.in);
        printStream = System.out;
    }

    void displayOnScreen(String msg) {
        printStream.println(msg);
    }

    @SuppressWarnings("SameParameterValue")
    int askForIntegerInput(String message, int minIncl, int maxExcl) {
        boolean askForInput = true;
        int selection = 0;
        do {
            try {
                printStream.print(message);
                printStream.flush();
                String userInput = scanner.next();
                selection = Integer.parseInt(userInput) - 1;

                if (selection >= minIncl && selection < maxExcl) {
                    askForInput = false;
                } else {
                    printStream.println("Input out of bounds");
                }
            } catch (NumberFormatException e) {
                printStream.println("Input is not a valid number");
                askForInput = true;
            }
        } while (askForInput);
        return selection;
    }

}
