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

    private static ImmutablePair<JSONArray, String> validateConfig(String[] args) throws ConfigFileException {
        if (args.length == 0) throw new ConfigFileException("A config file is not determined.");
        Path config = Paths.get(args[0]);
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
        try {
            ImmutablePair<JSONArray, String> parsedValues = validateConfig(args);
            Path monitoringUnitsFilePath = Paths.get(parsedValues.getRight());
            if (!Files.exists(monitoringUnitsFilePath) || !Files.isRegularFile(monitoringUnitsFilePath)) throw new ConfigFileException("A monitoring units file is wrong.");

            ArrayList<Path> classpathList = extractClasspathList(parsedValues.getLeft());
            ArrayList<MonitoringUnit> monitoringUnits = InstApp.parse(monitoringUnitsFilePath);

            classpathList.forEach((classpath) -> {
                try {
                    InstApp.getInstance().instrument(classpath, monitoringUnits, false);
                } catch (InstApp.InstrumentationException e) { handleException(e); }
            });
        } catch (ConfigFileException | InstApp.ParseMonitoringUnitsException e) { handleException(e); }
    }

    //Test method
    static ImmutablePair<JSONArray, String> testValidateConfig(String[] args) throws ConfigFileException {
        return validateConfig(args);
    }

    //Test method
    static ArrayList<Path> testExtractClasspathList(String[] args) throws ConfigFileException {
        return extractClasspathList(validateConfig(args).getLeft());
    }
}
