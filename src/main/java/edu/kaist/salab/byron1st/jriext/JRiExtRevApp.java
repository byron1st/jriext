package edu.kaist.salab.byron1st.jriext;

import edu.kaist.salab.byron1st.jriext.instrumentation.InstApp;
import edu.kaist.salab.byron1st.jriext.instrumentation.MonitoringUnit;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

/**
 * Created by byron1st on 2016. 5. 4..
 */
public class JRiExtRevApp {
    private static class ConfigFileException extends Exception {
        ConfigFileException(String message) {
            super(message);
        }
    }

    private static ImmutablePair<JSONArray, String> validateConfig(String configFilePath) throws ConfigFileException {
        Path config = Paths.get(configFilePath);
        if (!Files.exists(config)) throw new ConfigFileException("A config file does not exists.");
        JSONArray classpaths;
        String monitoringUnitsFile;
        try (BufferedReader br = Files.newBufferedReader(config)){
            JSONObject configObj = (JSONObject) (new JSONParser()).parse(br);
            if (configObj.size() != 2) throw new ConfigFileException("The content of the config file is wrong.");
            try {
                classpaths = (JSONArray) configObj.get("classpaths"); // If it is not an array, 'java.lang.ClassCastException' is thrown.
                monitoringUnitsFile = (String) configObj.get("monitoringUnits"); // If it is not a string, 'java.lang.ClassCastException' is thrown.
            } catch(ClassCastException e) { throw new ConfigFileException("The content of the config file is wrong."); }
            if (classpaths == null || monitoringUnitsFile == null) throw new ConfigFileException("The content of the config file is wrong.");
        } catch (IOException | ParseException e) { throw new ConfigFileException("The config file cannot be read."); }
        return new ImmutablePair<>(classpaths, monitoringUnitsFile);
    }

    private static ArrayList<Path> extractClasspathList(JSONArray jsonArray) throws ConfigFileException {
        ArrayList<Path> list = new ArrayList<>();
        for (Object obj : jsonArray) {
            Path classpath = Paths.get((String) obj);
            if (!Files.exists(classpath) || !Files.isDirectory(classpath)) throw new ConfigFileException("A classpath does not exists.");
            list.add(classpath);
        }
        return list;
    }

    private static void handleException(Exception e) {
        System.out.println(e.getMessage());
        e.printStackTrace();
    }

    private ArrayList<Path> classpathList;
    private ArrayList<MonitoringUnit> monitoringUnits;

    public JRiExtRevApp(String configFilePath) {
        initialize(configFilePath);
    }

    public void instrument() {
        assert classpathList != null;
        classpathList.forEach((classpath) -> {
            try {
                InstApp.getInstance().instrument(classpath, monitoringUnits, false);
            } catch (InstApp.InstrumentationException e) { handleException(e); }
        });
    }

    /*
    private void run() {
            statusLabel.setText("::RUNNING::");
            mainBtn.setText(STOP);
            generateBtn.setDisable(true);
            viewBtn.setDisable(true);
            try {
                if(isCached) {
                    Path tempLogs = Paths.get(System.getProperty("user.dir"), mainClassNameString + "_temp.txt");
                    if(Files.notExists(tempLogs))
                        Files.createFile(tempLogs);
                    logTempFile = tempLogs.toFile();

                    String xbootclasspathCmd = InstApp.defaultDirName + ":";
                    Iterator<String> iterator = jarFiles.iterator();
                    while(iterator.hasNext()) {
                        xbootclasspathCmd += iterator.next();
                        if(iterator.hasNext()) xbootclasspathCmd += ":";
                    }

                    ProcessBuilder builder = new ProcessBuilder("java",
                            "-Xbootclasspath/p:" + xbootclasspathCmd,
                            mainClassName.replaceAll("/", "."));
                    builder.directory(new File(InstApp.defaultDirName));
                    builder.redirectErrorStream(true);
                    builder.redirectOutput(logTempFile);
                    process = builder.start();
                } else
                    throw new IOException();
            } catch (IOException e) {
                showException(e, "An exception occurs during the execution.");
            }
        }

        private void stop() {
            statusLabel.setText("::PREPARED::");
            mainBtn.setText(RUN);
            generateBtn.setDisable(false);
            process.destroy();

            if(!isDebugMode.isSelected())
                refineLogTemp(logTempFile, logFile);
        }
     */

    private void initialize(String configFilePath) {
        try {
            ImmutablePair<JSONArray, String> parsedValues = validateConfig(configFilePath);
            Path monitoringUnitsFilePath = Paths.get(parsedValues.getRight());
            if (!Files.exists(monitoringUnitsFilePath) || !Files.isRegularFile(monitoringUnitsFilePath)) throw new ConfigFileException("A monitoring units file is wrong.");

            classpathList = extractClasspathList(parsedValues.getLeft());
            monitoringUnits = InstApp.parse(monitoringUnitsFilePath);
        } catch (ConfigFileException | InstApp.ParseMonitoringUnitsException e) { handleException(e); }
    }

    /**
     * config.json
     * {
     *     "classpaths": [
     *          "classpath1",
     *          "classpath2",
     *          ...
     *          ],
     *     "monitoringUnits": "path/to/monitoringUnits"
     * }
     * @param args
     */
    public static void main(String[] args) {
        if (args.length == 0) {
            try { throw new ConfigFileException("A config file is not determined.");
            } catch (ConfigFileException e) { handleException(e); }
        }

        new JRiExtRevApp(args[0]);
    }

    //Test method
    static ImmutablePair<JSONArray, String> testValidateConfig(String[] args) throws ConfigFileException {
        return validateConfig(args[0]);
    }

    //Test method
    static ArrayList<Path> testExtractClasspathList(String[] args) throws ConfigFileException {
        return extractClasspathList(validateConfig(args[0]).getLeft());
    }
}
