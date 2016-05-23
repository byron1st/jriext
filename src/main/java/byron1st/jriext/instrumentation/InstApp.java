package byron1st.jriext.instrumentation;

import byron1st.jriext.util.JRiExtException;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;

/**
 * Created by byron1st on 2015. 11. 6..
 */
public class InstApp {
    private static InstApp instApp = new InstApp();
    private InstApp() {}
    public static InstApp getInstance() { return instApp; }

    public static final class ParseMonitoringUnitsException extends JRiExtException {
        ParseMonitoringUnitsException(String message) { super(message); }
        ParseMonitoringUnitsException(String message, Exception e) { super(message, e); }
    }
    public static final class InstrumentationException extends Exception {
        InstrumentationException(String message) { super(message); }
        InstrumentationException(String message, Exception e) { super(message, e); }
    }
    public static final class ConvertLogsToJSONException extends Exception {
        ConvertLogsToJSONException(String message) { super(message); }
        ConvertLogsToJSONException(String message, Exception e) { super(message, e); }
    }

    public static final String defaultDirName = System.getProperty("user.dir") + File.separator + "jriext_userdata";
    public static final Path CACHE_ROOT = Paths.get(defaultDirName + File.separator + "cache");

    static final String ENTER = "+E+";
    static final String EXIT = "+X+";
    static final int BEGIN_INDEX_LENGTH = 3;
    static final String DDELIM = ",";
    static final String STATIC = "<static:";

    private static final String VKIND_FIELD = "F";
    private static final String VKIND_RETURN = "R";
    private static final String VKIND_PARAMETER = "P";
    private static final String VKIND_METHODS = "M";
    private static final String LOC_ENTER= "E";
    private static final String LOC_EXIT= "X";
    private static final String LOC_BOTH= "B";

    public static void deleteCacheFolderIfExists() {
        if(Files.exists(CACHE_ROOT)) {
            try {
                Files.walkFileTree(CACHE_ROOT, new FileVisitor<Path>() {
                    @Override
                    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        Files.deleteIfExists(file);
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                        Files.deleteIfExists(dir);
                        return FileVisitResult.CONTINUE;
                    }
                });
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    static {
        deleteCacheFolderIfExists();
    }

    public static ArrayList<MonitoringUnit> parse(Path monitoringUnitsJson) throws ParseMonitoringUnitsException {
        JSONArray monitoringUnitsJsonList;
        try(BufferedReader br = Files.newBufferedReader(monitoringUnitsJson)) {
            monitoringUnitsJsonList = (JSONArray) (new JSONParser()).parse(br);
        } catch (ParseException | IOException e) { throw new ParseMonitoringUnitsException("Cannot read the monitoring units json file.", e); }
        if(monitoringUnitsJsonList == null) throw new ParseMonitoringUnitsException("Cannot read the monitoring units json file.");

        ArrayList<MonitoringUnit> monitoringUnitsList = new ArrayList<>();
        try{
            for (Object o1 : monitoringUnitsJsonList) {
                JSONObject monitoringUnitObj = (JSONObject) o1;
                ImmutablePair<MonitoringUnit, MonitoringUnit> builtMonitoringUnits = buildMonitoringUnit(monitoringUnitObj);
                MonitoringUnit monitoringUnitLeft = builtMonitoringUnits.getLeft();
                MonitoringUnit monitoringUnitRight = builtMonitoringUnits.getRight();
                JSONArray monitoringValuesList = (JSONArray) monitoringUnitObj.get("values");
                for (Object o2 : monitoringValuesList) {
                    JSONObject monitoringValuesObj = (JSONObject) o2;
                    MonitoringValue monitoringValue = buildMonitoringValue(monitoringValuesObj);
                    monitoringUnitLeft.addMonitoringValue(monitoringValue);
                    if(monitoringUnitRight != null) monitoringUnitRight.addMonitoringValue(monitoringValue);
                }
                monitoringUnitsList.add(monitoringUnitLeft);
                if(monitoringUnitRight != null) monitoringUnitsList.add(monitoringUnitRight);
            }
        } catch (ClassCastException e) { throw new ParseMonitoringUnitsException("Cannot parse the content. Check the syntax.", e); }
        return monitoringUnitsList;
    }

    public static ImmutablePair<JSONArray, ArrayList<ImmutablePair<String, String>>> convertLogs2JSON(Path logFile) throws ConvertLogsToJSONException {
        ArrayList<ImmutablePair<String, String>> crackedLogs = new ArrayList<>();
        JSONArray jsonConverted = new JSONArray();
        try(BufferedReader br = Files.newBufferedReader(logFile)) {
            JSONObject lineObject = new JSONObject();
            String line;
            String lineBefore = null;
            while((line = br.readLine()) != null) {
                boolean isEnter;
                if (line.startsWith(ENTER)) isEnter = true;
                else if (line.startsWith(EXIT)) isEnter = false;
                else {
                    addCrackedLogs(crackedLogs, line, lineBefore);
                    continue;
                }

                String[] elementsList = line.substring(BEGIN_INDEX_LENGTH).split(",");
                if (elementsList.length < 6) {
                    addCrackedLogs(crackedLogs, line, lineBefore);
                    continue;
                }

                Integer objectId = null;
                try {
                    objectId = Integer.parseInt(elementsList[3]);
                } catch (NumberFormatException e) {
                    if(!elementsList[3].startsWith(STATIC)) throw new NumberFormatException();
                }

                JSONArray values = null;
                if (elementsList.length > 6) {
                    values = new JSONArray();
                    values.addAll(Arrays.asList(elementsList).subList(6, elementsList.length));
                }

                lineObject.put("timestamp", Long.parseLong(elementsList[0]));
                lineObject.put("threadName", elementsList[1]);
                lineObject.put("threadId", Integer.parseInt(elementsList[2]));
                if (objectId == null) lineObject.put("objectId", elementsList[3]);
                else lineObject.put("objectId", objectId);
                lineObject.put("className", elementsList[4]);
                lineObject.put("methodDesc", elementsList[5]);
                lineObject.put("isEnter", isEnter);
                if (values != null) lineObject.put("values", values);
                jsonConverted.add(lineObject);

                lineBefore = line;
            }
        } catch (IOException | NumberFormatException e) {
            throw new ConvertLogsToJSONException("Reading a log file has been failed: " + logFile.toString(), e);
        }

        return new ImmutablePair<>(jsonConverted, crackedLogs);
    }

    private static void addCrackedLogs(ArrayList<ImmutablePair<String, String>> crackedLogs, String line, String lineBefore) {
        if (lineBefore == null) crackedLogs.add(new ImmutablePair<>(null, line));
        else crackedLogs.add(new ImmutablePair<>(lineBefore, line));
    }

    private static ImmutablePair<MonitoringUnit, MonitoringUnit> buildMonitoringUnit(JSONObject monitoringUnitObj) throws ParseMonitoringUnitsException, ClassCastException {
        String className = (String) monitoringUnitObj.get("class");
        JSONObject methodDefinition = (JSONObject) monitoringUnitObj.get("method");
        String location = (String) monitoringUnitObj.get("location");
        MonitoringUnit monitoringUnitLeft = new MonitoringUnit(className, (String) methodDefinition.get("desc"), (boolean) methodDefinition.get("isVirtual"));
        MonitoringUnit monitoringUnitRight = null;
        switch(location) {
            case LOC_ENTER: monitoringUnitLeft.setEnter(true); break;
            case LOC_EXIT: monitoringUnitLeft.setEnter(false); break;
            case LOC_BOTH:
                monitoringUnitLeft.setEnter(true);
                monitoringUnitRight = monitoringUnitLeft.duplicateThisWithOppositeLocation();
                break;
            default: throw new ParseMonitoringUnitsException("Cannot parse the content. Check its syntax.");
        }
        return new ImmutablePair<>(monitoringUnitLeft, monitoringUnitRight);
    }

    private static MonitoringValue buildMonitoringValue(JSONObject monitoringValuesObj) throws ParseMonitoringUnitsException {
        MonitoringValue monitoringValue;
        String type = (String) monitoringValuesObj.get("type");
        JSONArray methodsList = (JSONArray) monitoringValuesObj.get("methods");
        switch((String) monitoringValuesObj.get("kind")) {
            case VKIND_FIELD: monitoringValue = new MonitoringValueField((String) monitoringValuesObj.get("info"), type); break;
            case VKIND_PARAMETER: monitoringValue = new MonitoringValueParameter((int) monitoringValuesObj.get("info"), type); break;
            case VKIND_RETURN: monitoringValue = new MonitoringValueReturn(type); break;
            case VKIND_METHODS: monitoringValue = getMonitoringValueMethod((JSONObject) methodsList.get(0)); break;
            default: throw new ParseMonitoringUnitsException("Cannot parse the content. Check its syntax.");
        }

        if (!type.equals("") && isObject(type)) buildMonitoringValueMethodsChain(methodsList, 0, monitoringValue);
        else if (type.equals("")) buildMonitoringValueMethodsChain(methodsList, 1, monitoringValue);

        return monitoringValue;
    }

    private static void buildMonitoringValueMethodsChain(JSONArray methodsList, int fromIdx, MonitoringValue monitoringValue) throws ParseMonitoringUnitsException {
        if (methodsList.size() == 0) throw new ParseMonitoringUnitsException("Cannot parse the content. Check its syntax.");
        MonitoringValueMethod temp = null;
        for (int i = fromIdx; i < methodsList.size(); i++) {
            MonitoringValueMethod monitoringValueMethod = getMonitoringValueMethod((JSONObject) methodsList.get(i));
            if (temp != null) {
                temp.setNextMethod(monitoringValueMethod);
                temp = monitoringValueMethod;
            } else {
                monitoringValue.setNextMethod(monitoringValueMethod);
                temp = monitoringValueMethod;
            }
        }
    }

    private static MonitoringValueMethod getMonitoringValueMethod(JSONObject jsonObject) {
        JSONObject methodInfoObj = (JSONObject) jsonObject.get("method");
        String className = (String) jsonObject.get("class");
        boolean isVirtual = (boolean) methodInfoObj.get("isVirtual");
        String desc = (String) methodInfoObj.get("desc");

        return new MonitoringValueMethod(isVirtual, className, desc);
    }

    private static boolean isObject(String type) {
        return type.startsWith("L");
    }


    private HashSet<String> isCopied = new HashSet<>();

    public void instrument(final Path classpath, final ArrayList<MonitoringUnit> monitoringUnits, boolean isDebugMode) throws InstrumentationException {
        for(MonitoringUnit monitoringUnit : monitoringUnits) {
            String className = monitoringUnit.getClassName();
            ClassReader classReader;
            try{
                classReader = getClassReader(classpath, className);
            } catch (IOException e) { throw new InstrumentationException("Reading a class is failed: " + className, e); }
            ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS);
            JRIEXTClassVisitor classVisitor = new JRIEXTClassVisitor(classWriter, monitoringUnit, isDebugMode);
            classReader.accept(classVisitor, ClassReader.SKIP_FRAMES);
            try{
                printClass(className, classWriter);
            } catch (IOException e) { throw new InstrumentationException("Writing a class to the cache folder is failed: " + className, e); }
        }

        try{
            Files.walkFileTree(classpath, new FileVisitor<Path>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    if(!isCopied.contains(file.toString()) && file.toString().endsWith(".class")) copyClassFile(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) { throw new InstrumentationException("Copying classes that are not instrumented is failed.", e); }

    }

    private void copyClassFile(Path file) throws IOException {
        ClassReader reader = new ClassReader(new FileInputStream(file.toFile()));
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        reader.accept(writer, ClassReader.SKIP_FRAMES);
        printClass(reader.getClassName(), writer);
    }

    private ClassReader getClassReader(Path classpath, String className) throws IOException {
        Path inCache = CACHE_ROOT.resolve(className + ".class");
        if(Files.exists(inCache)) return new ClassReader(new FileInputStream(inCache.toFile()));
        else {
            Path inClasspath = classpath.resolve(className + ".class");
            if(Files.exists(inClasspath)) {
                isCopied.add(inClasspath.toString());
                return new ClassReader(new FileInputStream(inClasspath.toFile()));
            }
            else return new ClassReader(className);
        }
    }

    private void printClass(String className, ClassWriter classWriter) throws IOException {
        String fileName;
        String dirName = CACHE_ROOT.toString();
        int del = className.lastIndexOf('/');
        if(del != -1) {
            fileName = className.substring(className.lastIndexOf('/') + 1);
            dirName += File.separator + className.substring(0, className.lastIndexOf('/'));
        } else
            fileName = className;
        Files.createDirectories(Paths.get(dirName));
        Path path = Paths.get(dirName, fileName + ".class");
        Files.write(path, classWriter.toByteArray());
    }

    /*Test*******************************************************************/
    void runPrintClass(String className, ClassWriter classWriter) throws IOException { printClass(className, classWriter); }
}
