package edu.kaist.salab.byron1st.jriext;

import edu.kaist.salab.byron1st.jriext.instrumentation.InstApp;
import edu.kaist.salab.byron1st.jriext.instrumentation.MonitoringUnit;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by byron1st on 2016. 5. 4..
 */
public class JRiExt {
    public static class ConfigFileException extends Exception {
        ConfigFileException(String message) {
            super(message);
        }
    }
    public static class ProcessRunException extends Exception {
        ProcessRunException(String message) {
            super(message);
        }
    }

    private static ImmutablePair<JSONArray, String> validateConfig(String configFilePath) throws ProcessRunException {
        Path config = Paths.get(configFilePath);
        if (!Files.exists(config)) throw new ProcessRunException("A config file does not exists.");
        JSONArray classpaths;
        String monitoringUnitsFile;
        try (BufferedReader br = Files.newBufferedReader(config)){
            JSONObject configObj = (JSONObject) (new JSONParser()).parse(br);
            if (configObj.size() != 2) throw new ProcessRunException("The content of the config file is wrong.");
            try {
                classpaths = (JSONArray) configObj.get("classpaths"); // If it is not an array, 'java.lang.ClassCastException' is thrown.
                monitoringUnitsFile = (String) configObj.get("monitoringUnits"); // If it is not a string, 'java.lang.ClassCastException' is thrown.
            } catch(ClassCastException e) { throw new ProcessRunException("The content of the config file is wrong."); }
            if (classpaths == null || monitoringUnitsFile == null) throw new ProcessRunException("The content of the config file is wrong.");
        } catch (IOException | ParseException e) { throw new ProcessRunException("The config file cannot be read."); }
        return new ImmutablePair<>(classpaths, monitoringUnitsFile);
    }
    private static ArrayList<Path> extractClasspathList(JSONArray jsonArray) throws ProcessRunException {
        ArrayList<Path> list = new ArrayList<>();
        for (Object obj : jsonArray) {
            Path classpath = Paths.get((String) obj);
            if (!Files.exists(classpath) || !Files.isDirectory(classpath)) throw new ProcessRunException("A classpath does not exists.");
            list.add(classpath);
        }
        return list;
    }

    private ArrayList<Path> classpathList;
    private ArrayList<MonitoringUnit> monitoringUnits;
    private HashMap<String, Process> processes = new HashMap<>();
    private Observer status;

    public void attachObserver(Observer observer) {
        this.status = observer;
    }

    public void loadConfigFile(String configFilePath) throws ProcessRunException, InstApp.ParseMonitoringUnitsException {
        ImmutablePair<JSONArray, String> parsedValues = validateConfig(configFilePath);
        Path monitoringUnitsFilePath = Paths.get(parsedValues.getRight());
        if (!Files.exists(monitoringUnitsFilePath) || !Files.isRegularFile(monitoringUnitsFilePath)) throw new ProcessRunException("A monitoring units file is wrong.");

        classpathList = extractClasspathList(parsedValues.getLeft());
        updateStatus("Classpaths are extracted.");
        monitoringUnits = InstApp.parse(monitoringUnitsFilePath);
        updateStatus("Monitoring units are extracted.");
    }

    public void instrument() throws InstApp.InstrumentationException, ConfigFileException {
        if (classpathList == null) throw new ConfigFileException("A list of classpaths was not extracted.");
        for (Path classpath : classpathList) {
            InstApp.getInstance().instrument(classpath, monitoringUnits, false);
            updateStatus(classpath.toString() + " has been instrumented.");
        }
    }

    /**
     *
     * @param mainClassName Its delimiter should be ".", not "/".
     * @throws ProcessRunException
     */
    public void runMainClass(String mainClassName) throws ProcessRunException {
        String extension = ".murecords";
        String recordsFileName = mainClassName + System.currentTimeMillis();
        String recordsErrorFileName = recordsFileName +  ".error";
        File monitoringRecordsFile = Paths.get(InstApp.defaultDirName, "records", recordsFileName + extension).toFile();
        File monitoringErrorRecordsFile = Paths.get(InstApp.defaultDirName, "records", recordsErrorFileName + extension).toFile();

        String xbootclasspathCmd = InstApp.defaultDirName;
//        Iterator<String> iterator = jarFiles.iterator();
//        while(iterator.hasNext()) {
//            xbootclasspathCmd += ":" + iterator.next();
//        }

        ProcessBuilder builder = new ProcessBuilder("java",
                "-Xbootclasspath/p:" + xbootclasspathCmd,
                mainClassName.replaceAll("/", "."));
        builder.directory(new File(InstApp.defaultDirName));
//        builder.redirectErrorStream(true);
        builder.redirectOutput(monitoringRecordsFile);
        builder.redirectError(monitoringErrorRecordsFile);
        Process process;
        try {
            process = builder.start();
        } catch (IOException e) { throw new ProcessRunException("A process cannot be run: " + mainClassName); }
        processes.put(mainClassName, process);
    }

    public void stopMainClass(String mainClassName) {
        Process process = processes.get(mainClassName);
        if (process != null) process.destroy();
    }

    private void updateStatus(String message) {
        if (status != null) status.update(message);
    }

    //Test method
    static ImmutablePair<JSONArray, String> testValidateConfig(String[] args) throws ProcessRunException {
        return validateConfig(args[0]);
    }

    //Test method
    static ArrayList<Path> testExtractClasspathList(String[] args) throws ProcessRunException {
        return extractClasspathList(validateConfig(args[0]).getLeft());
    }
}
