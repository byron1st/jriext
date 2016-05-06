package edu.kaist.salab.byron1st.jriext;

import edu.kaist.salab.byron1st.jriext.instrumentation.InstApp;
import org.apache.commons.cli.*;

import java.util.Scanner;

/**
 * Created by byron1st on 2016. 5. 6..
 */
public class ApplicationCLI {
    private static void handleException(Exception e) {
        System.out.println(e.getMessage());
//        e.printStackTrace();
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
    }

    public static void main(String[] args) {
        initializeCMD();
        CommandLineParser parser = new DefaultParser();
        JRiExt jRiExt = new JRiExt();
        jRiExt.attachObserver(new StatusObserver());
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
                    System.out.println(line.getOptionValue("r"));
                } else if (line.hasOption("d")) {
                    System.out.println(line.getOptionValue("d"));
                } else if (line.hasOption("show")) {
                    System.out.println("show");
                } else if (line.hasOption("h")) {
                    HelpFormatter formatter = new HelpFormatter();
                    formatter.printHelp("Use one command at a time.", options);
                } else if (line.hasOption("q")) {
                    System.exit(0);
                } else {
                    System.out.println("Cannot understand your command.");
                }
            } catch (InstApp.ParseMonitoringUnitsException | JRiExt.ProcessRunException | JRiExt.ConfigFileException | InstApp.InstrumentationException e) {
                handleException(e);
            }
        }
    }
}
