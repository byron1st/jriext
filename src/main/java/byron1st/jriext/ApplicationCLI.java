package byron1st.jriext;

import byron1st.jriext.instrumentation.InstApp;
import org.apache.commons.cli.*;

import java.util.Objects;
import java.util.Scanner;

/**
 * Created by byron1st on 2016. 5. 6..
 */
public class ApplicationCLI {
    private static void handleException(Exception e) {
        System.out.println(e.getMessage());
        if (e instanceof InstApp.ParseMonitoringUnitsException)
            if (((InstApp.ParseMonitoringUnitsException) e).getOriginalException() != null)
                ((InstApp.ParseMonitoringUnitsException) e).getOriginalException().printStackTrace();
    }

    private static final Options options = new Options();
    private static void initializeCMD() {
        options.addOption("c", "config", true, "Load a config file.");
        options.addOption("inst", false, "Instrument files according to the config file.");
        options.addOption("r", "run", true, "Run a main class.");
        options.addOption("d", "destroy", true, "Destroy a running process.");
        options.addOption("show", false, "Show running processes.");
        options.addOption("q", "quit", false, "Quit JRiExtractor.");
        options.addOption("h", "help", false, "Print the instruction.");
        options.addOption("runAll", false, "Run all main classes documented in the config file.");
        options.addOption("reset", false, "Reset all details of the configuration.");
    }

    private static void printLaunchingMessage() {
        System.out.println(
                "/***************************************************JRiExt*********************************************************/\n" +
                "/* The running folder will be \"" + InstApp.defaultDirName + "\".\n" +
                "/* If you have additional resources(i.e. images), which are necessary to run the program,\n" +
                "/* please copy them to appropriate location considering the running folder.\n" +
                "/******************************************************************************************************************/");
    }

    public static void main(String[] args) {
        initializeCMD();
        CommandLineParser parser = new DefaultParser();
        JRiExt jRiExt = JRiExt.getInstance();
        jRiExt.attachObserver(new StatusObserver());

        printLaunchingMessage();

        while(true) {
            Scanner in = new Scanner(System.in);
            System.out.print("jriext> ");
            String[] cmd = in.nextLine().split(" ");
            if(cmd.length > 2) {
                System.out.println("Use a command once at a time.");
                continue;
            }
            CommandLine line;
            try {
                line = parser.parse(options, cmd);
            } catch (ParseException e) {
                handleException(e);
                continue;
            }

            try {
                if (line.hasOption("c")) {
                    jRiExt.loadConfigFile(line.getOptionValue("c"));
                } else if (line.hasOption("inst")) {
                    jRiExt.instrument();
                } else if (line.hasOption("r")) {
                    jRiExt.runMainClass(line.getOptionValue("r"));
                } else if (line.hasOption("runAll")) {
                    jRiExt.runAllMainClass();
                } else if (line.hasOption("d")) {
                    jRiExt.stopMainClass(line.getOptionValue("d"));
                } else if (line.hasOption("show")) {
                    jRiExt.getListofRunningProcess().forEach(System.out::println);
                } else if (line.hasOption("h")) {
                    HelpFormatter formatter = new HelpFormatter();
                    formatter.printHelp("Use one command at a time.", options);
                } else if (line.hasOption("reset")) {
                    jRiExt.reset();
                } else if (line.hasOption("q")) {
                    System.exit(0);
                } else if (Objects.equals(line.getArgs()[0], "")) {
                } else {
                    System.out.println("Cannot understand your command.");
                }
            } catch (InstApp.ParseMonitoringUnitsException | JRiExt.ProcessRunException | JRiExt.ConfigFileException | InstApp.InstrumentationException e) {
                handleException(e);
            }
        }
    }
}
