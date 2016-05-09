package byron1st.jriext;

import byron1st.jriext.instrumentation.InstApp;
import byron1st.jriext.instrumentation.MonitoringUnit;
import byron1st.jriext.run.ProcessDeathDetector;
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
import java.util.HashMap;

/**
 * Created by byron1st on 2016. 5. 4..
 */
public class JRiExt {
    private static JRiExt jRiExtApp = new JRiExt();
    private JRiExt() {}
    public static JRiExt getInstance() { return jRiExtApp; }

    public static class ConfigFileException extends Exception {
        ConfigFileException(String message) {
            super(message);
        }
    }
    public static class ProcessRunException extends Exception {
        public ProcessRunException(String message) {
            super(message);
        }
    }

    private static final int CONFIG_SIZE = 3;

    private static final String CLASSPATH = "classpath";
    private static final String MONITORING_UNITS = "monitoringUnits";
    private static final String LIBRARIES = "libraries";

    private static HashMap<String, Object> validateConfig(String configFilePath) throws ConfigFileException {
        Path config = Paths.get(configFilePath);
        if (!Files.exists(config)) throw new ConfigFileException("A config file does not exists.");
        String classpath;
        String monitoringUnitsFile;
        JSONArray libraries;
        try (BufferedReader br = Files.newBufferedReader(config)){
            JSONObject configObj = (JSONObject) (new JSONParser()).parse(br);
            if (configObj.size() != CONFIG_SIZE) throw new ConfigFileException("The content of the config file is wrong.");
            try {
                classpath = (String) configObj.get(CLASSPATH); // If it is not an array, 'java.lang.ClassCastException' is thrown.
                monitoringUnitsFile = (String) configObj.get(MONITORING_UNITS); // If it is not a string, 'java.lang.ClassCastException' is thrown.
                libraries = (JSONArray) configObj.get(LIBRARIES); // If it is not an array, 'java.lang.ClassCastException' is thrown.
            } catch(ClassCastException e) { throw new ConfigFileException("The content of the config file is wrong."); }
            if (classpath == null || monitoringUnitsFile == null || libraries == null) throw new ConfigFileException("The content of the config file is wrong.");
        } catch (IOException | ParseException e) { throw new ConfigFileException("The config file cannot be read."); }
        HashMap<String, Object> returnedMap = new HashMap<>();
        returnedMap.put(CLASSPATH, classpath);
        returnedMap.put(MONITORING_UNITS, monitoringUnitsFile);
        returnedMap.put(LIBRARIES, libraries);
        return returnedMap;
    }

    private static Path extractClasspathList(String classpathString) throws ProcessRunException {
        Path classpath;
        classpath = Paths.get(classpathString);
        if (!Files.exists(classpath) || !Files.isDirectory(classpath)) throw new ProcessRunException("A classpath does not exists.");
        return classpath;
    }

    private static ArrayList<String> extractLibrariesList(HashMap<String, Object> parsedValues) throws ClassCastException {
        ArrayList<String> librariesList = new ArrayList<>();
        ((JSONArray) parsedValues.get(LIBRARIES)).forEach((library) -> librariesList.add((String) library));
        return librariesList;
    }

    private Path classpath;
    private ArrayList<MonitoringUnit> monitoringUnits;
    private ArrayList<String> libraries;
    private HashMap<String, Process> processes = new HashMap<>();
    private Observer status;

    private int count = 0;

    public void attachObserver(Observer observer) {
        this.status = observer;
    }

    public void loadConfigFile(String configFilePath) throws ProcessRunException, InstApp.ParseMonitoringUnitsException, ConfigFileException {
        HashMap<String, Object> parsedValues = validateConfig(configFilePath);
        try {
            Path monitoringUnitsFilePath = Paths.get((String) parsedValues.get(MONITORING_UNITS));
            if (!Files.exists(monitoringUnitsFilePath) || !Files.isRegularFile(monitoringUnitsFilePath)) throw new ProcessRunException("A monitoring units file is wrong.");

            classpath = extractClasspathList((String) parsedValues.get(CLASSPATH));
            updateStatus("Classpaths are extracted.");
            monitoringUnits = InstApp.parse(monitoringUnitsFilePath);
            updateStatus("Monitoring units are extracted.");
            libraries = extractLibrariesList(parsedValues);
            if (libraries.size() == 0) updateStatus("There is no external libarary.");
            else updateStatus("External libraries are extracted.");
        } catch (ClassCastException e) { throw new ConfigFileException("The content of the config file is wrong."); }
    }

    public void instrument() throws InstApp.InstrumentationException, ConfigFileException {
        if (classpath == null) throw new ConfigFileException("A list of classpaths was not extracted.");
        InstApp.getInstance().instrument(classpath, monitoringUnits, false);
        updateStatus(classpath.toString() + " has been instrumented.");
    }

    /**
     *
     * @param mainClassName Its delimiter should be ".", not "/".
     * @throws ProcessRunException
     */
    public void runMainClass(String mainClassName) throws ProcessRunException {
        if (!Files.exists(InstApp.CACHE_ROOT)) throw new ProcessRunException("Instrumentation should be done prior to the execution of the target system.");
        if (!Files.exists(InstApp.CACHE_ROOT.resolve(mainClassName + ".class"))) throw new ProcessRunException("The main class does not exist.");

        String processId = mainClassName + count;
        String extension = ".murecords";
        String recordsFileName = mainClassName.replaceAll("/", "_") + count + "-" + System.currentTimeMillis();
        String recordsErrorFileName = recordsFileName +  ".error";
        Path recordsDirectory = Paths.get(InstApp.defaultDirName, "records");
        Path monitoringRecordsFile = Paths.get(InstApp.defaultDirName, "records", recordsFileName + extension);
        Path monitoringErrorRecordsFile = Paths.get(InstApp.defaultDirName, "records", recordsErrorFileName + extension);
        try {
            if(!Files.exists(recordsDirectory)) Files.createDirectory(recordsDirectory);
            Files.createFile(monitoringRecordsFile);
            Files.createFile(monitoringErrorRecordsFile);
        } catch (IOException e) {
            e.printStackTrace();
            throw new ProcessRunException("Files for recording cannot be created in the cache folder.");
        }


        String xbootclasspathCmd = InstApp.CACHE_ROOT.toString();
        for (String library : libraries) {
            xbootclasspathCmd += ":" + library;
        }

        ProcessBuilder builder = new ProcessBuilder("java",
                "-Xbootclasspath/p:" + xbootclasspathCmd,
                mainClassName.replaceAll("/", "."));
        builder.directory(InstApp.CACHE_ROOT.toFile());
        builder.redirectOutput(monitoringRecordsFile.toFile());
        builder.redirectError(monitoringErrorRecordsFile.toFile());
        Process process;
        try {
            process = builder.start();
            count++;
            updateStatus(processId + " is running.");
        } catch (IOException e) {
            throw new ProcessRunException("A process cannot be run: " + mainClassName);
        }
        processes.put(processId, process);
        ProcessDeathDetector deathDetector = new ProcessDeathDetector(process);
        deathDetector.addListener(() -> processes.remove(processId));
        deathDetector.start();
    }

    public void stopMainClass(String mainClassName) throws ProcessRunException {
        Process process = processes.get(mainClassName);
        if (process != null) process.destroy();
        else throw new ProcessRunException("There is no such process.");
    }

    public ArrayList<String> getListofRunningProcess() {
        ArrayList<String> returnedList = new ArrayList<>();
        processes.keySet().forEach(returnedList::add);
        return returnedList;
    }

    private void updateStatus(String message) {
        if (status != null) status.update(message);
    }

    //Test method
    static HashMap<String, Object> testValidateConfig(String[] args) throws ProcessRunException, ConfigFileException {
        return validateConfig(args[0]);
    }

    //Test method
    static Path testExtractClasspathList(String[] args) throws ProcessRunException, ConfigFileException {
        return extractClasspathList((String) validateConfig(args[0]).get(CLASSPATH));
    }
}
