package io.accelerate.tracking.app;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;
import io.accelerate.tracking.app.commands.HasHelp;
import io.accelerate.tracking.app.commands.SelfTestCommand;
import io.accelerate.tracking.app.commands.TrackAndUploadCommand;
import io.accelerate.tracking.app.util.CliUtil;
import org.slf4j.Logger;

import static io.accelerate.tracking.app.util.CliUtil.printAvailableCommands;
import static org.slf4j.LoggerFactory.getLogger;

public class TrackCodeAndUploadApp {
    private static final Logger log = getLogger(TrackCodeAndUploadApp.class);
    
    public static void main(String[] args) {
        log.info("Starting the source code tracking app");

        JCommander commander = new JCommander();
        commander.setProgramName("TrackCodeAndUploadApp"); // Set program name in usage output

        // Create Command objects
        SelfTestCommand selfTestCommand = new SelfTestCommand();
        TrackAndUploadCommand trackAndUploadCommand = new TrackAndUploadCommand();

        // Add commands to JCommander
        commander.addCommand("self-test", selfTestCommand);
        commander.addCommand("track-and-upload", trackAndUploadCommand);

        try {
            if (args.length == 0) {
                throw new ParameterException("No command provided.");
            }

            commander.parse(args); // Parse the provided arguments

            // Determine which command is selected
            String parsedCommand = commander.getParsedCommand();

            if ("self-test".equals(parsedCommand)) {
                if (selfTestCommand.isHelpRequested()) {
                    commander.usage("self-test");
                    System.exit(0);
                }
                selfTestCommand.run();
            } else if ("track-and-upload".equals(parsedCommand)) {
                if (trackAndUploadCommand.isHelpRequested()) {
                    commander.usage("track-and-upload");
                    System.exit(0);
                }
                trackAndUploadCommand.run();
            } else {
                throw new ParameterException("Invalid command provided."); // Invalid command scenario
            }
        } catch (ParameterException e) {
            log.error("Argument parsing failed: {}", e.getMessage());
            String parsed = commander.getParsedCommand();

            if (parsed != null) {
                // Known command but invalid args
                CliUtil.printRequiredOnly(commander, parsed);
                CliUtil.printMinimalExample(commander, parsed);
            } else if (args.length > 0 && commander.getCommands().containsKey(args[0])) {
                // First token is a known command even though parse failed
                CliUtil.printRequiredOnly(commander, args[0]);
                CliUtil.printMinimalExample(commander, args[0]);
            } else {
                // No or unknown command
                printAvailableCommands(commander);
            }
            System.exit(2);
        }
    }
}
