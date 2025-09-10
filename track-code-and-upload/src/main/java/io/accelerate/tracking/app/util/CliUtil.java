package io.accelerate.tracking.app.util;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterDescription;
import com.beust.jcommander.Parameters;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public final class CliUtil {

    /** Print only required options for the given scope. If commandName is null, prints global requireds. */
    public static void printRequiredOnly(JCommander root, String commandName) {
        JCommander scope = (commandName == null) ? root : root.getCommands().get(commandName);
        if (scope == null) {
            root.usage(); // fallback if unknown
            return;
        }

        List<ParameterDescription> required = scope.getParameters().stream()
                .filter(pd -> pd.getParameter().required())
                .sorted(Comparator.comparing(pd -> firstName(pd)))
                .collect(Collectors.toList());

        String title = (commandName == null) ? "Required global options:" : "Required options for '" + commandName + "':";
        System.err.println(title);

        if (required.isEmpty()) {
            System.err.println("  (none)");
            return;
        }

        int width = required.stream()
                .map(pd -> namesJoined(pd))
                .mapToInt(String::length)
                .max().orElse(0);

        for (ParameterDescription pd : required) {
            String names = namesJoined(pd);
            String desc  = safeDesc(pd);
            System.err.printf("  %-" + (width + 2) + "s%s%n", names, desc);
        }
    }

    /** Show a minimal example using only required options. */
    public static void printMinimalExample(JCommander root, String commandName) {
        JCommander scope = (commandName == null) ? root : root.getCommands().get(commandName);
        if (scope == null) return;

        List<String> requiredFlags = scope.getParameters().stream()
                .filter(pd -> pd.getParameter().required())
                .map(pd -> firstName(pd) + " " + valuePlaceholder(pd))
                .collect(Collectors.toList());

        String prog = root.getProgramName() != null ? root.getProgramName() : "program";
        String cmd  = commandName == null ? "" : " " + commandName;
        String args = requiredFlags.isEmpty() ? "" : " " + String.join(" ", requiredFlags);

        System.err.println();
        System.err.println("Minimal example:");
        System.err.println("  " + prog + cmd + args);
    }

    public static void printAvailableCommands(JCommander commander) {
        System.err.println("Available commands:");
        commander.getCommands().forEach((name, sub) -> {
            String desc = getCommandDescription(commander, name);
            System.err.printf("  %-20s %s%n", name, desc);
        });
        System.err.println("Use 'help <command>' for details.");
    }

    private static String getCommandDescription(JCommander root, String name) {
        JCommander sub = root.getCommands().get(name);
        if (sub == null || sub.getObjects().isEmpty()) return "";
        Parameters ann = sub.getObjects().get(0).getClass().getAnnotation(Parameters.class);
        return (ann != null) ? ann.commandDescription() : "";
    }


    // Helpers

    private static String namesJoined(ParameterDescription pd) {
        String[] names = pd.getParameter().names();
        String joined = String.join(", ", names);
        return pd.getParameter().arity() != 0 ? joined + " " + valuePlaceholder(pd).trim() : joined;
    }

    private static String firstName(ParameterDescription pd) {
        String[] names = pd.getParameter().names();
        return names.length > 0 ? names[0] : "";
    }

    private static String valuePlaceholder(ParameterDescription pd) {
        // Simple generic placeholder. You can specialize by type if you want.
        return pd.getParameter().arity() == 0 ? "" : "<value>";
    }

    private static String safeDesc(ParameterDescription pd) {
        String d = pd.getDescription();
        return d == null || d.isBlank() ? "" : d;
    }
}
