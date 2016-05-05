package edu.kaist.salab.byron1st.jriext.instrumentation;

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
import java.util.HashSet;

/**
 * Created by byron1st on 2015. 11. 6..
 */
public class InstApp {
    private static InstApp instApp = new InstApp();
    public static InstApp getInstance() { return instApp; }
    public static class ParseMonitoringUnitsException extends Exception {
        ParseMonitoringUnitsException(String message) { super(message); }
    }
    public static class InstrumentationException extends Exception {
        InstrumentationException(String message) { super(message); }
    }
    public static String defaultDirName = System.getProperty("user.dir") + File.separator + "cache";
    public static final Path CACHE_ROOT = Paths.get(defaultDirName);
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

//    private static final Path CACHE_ROOT = Paths.get(System.getProperty("user.dir"), "cache");
    private static final String VKIND_FIELD = "F";
    private static final String VKIND_RETURN = "R";
    private static final String VKIND_PARAMETER = "P";
    private static final String LOC_ENTER= "E";
    private static final String LOC_EXIT= "X";
    private static final String LOC_BOTH= "B";

    public static ArrayList<MonitoringUnit> parse(Path monitoringUnitsJson) throws ParseMonitoringUnitsException {
        JSONArray monitoringUnitsJsonList;
        try(BufferedReader br = Files.newBufferedReader(monitoringUnitsJson)) {
            monitoringUnitsJsonList = (JSONArray) (new JSONParser()).parse(br);
        } catch (ParseException | IOException e) { throw new ParseMonitoringUnitsException("Cannot read the monitoring units json file."); }
        if(monitoringUnitsJsonList == null) throw new ParseMonitoringUnitsException("Cannot read the monitoring units json file.");

        ArrayList<MonitoringUnit> monitoringUnitsList = new ArrayList<>();
        try{
            for (Object o1 : monitoringUnitsJsonList) {
                JSONObject monitoringUnitObj = (JSONObject) o1;
                MonitoringUnit monitoringUnit = buildMonitoringUnit(monitoringUnitObj);
                JSONArray monitoringValuesList = (JSONArray) monitoringUnitObj.get("values");
                for (Object o2 : monitoringValuesList) {
                    JSONObject monitoringValuesObj = (JSONObject) o2;
                    MonitoringValue monitoringValue = buildMonitoringValue(monitoringValuesObj);
                    monitoringUnit.addMonitoringValue(monitoringValue);
                }
                monitoringUnitsList.add(monitoringUnit);
            }
        } catch (ClassCastException e) { throw new ParseMonitoringUnitsException("Cannot parse the content. Check the syntax."); }
        return monitoringUnitsList;
    }

    private static MonitoringUnit buildMonitoringUnit(JSONObject monitoringUnitObj) throws ParseMonitoringUnitsException, ClassCastException {
        String className = (String) monitoringUnitObj.get("class");
        JSONObject methodDefinition = (JSONObject) monitoringUnitObj.get("method");
        String location = (String) monitoringUnitObj.get("location");
        MonitoringUnit monitoringUnit = new MonitoringUnit(className, (String) methodDefinition.get("desc"), (boolean) methodDefinition.get("isVirtual"));
        switch(location) {
            case LOC_ENTER: monitoringUnit.setEnter(true); break;
            case LOC_EXIT: monitoringUnit.setEnter(false); break;
            case LOC_BOTH: break;
            default: throw new ParseMonitoringUnitsException("Cannot parse the content. Check its syntax.");
        }
        return monitoringUnit;
    }

    private static MonitoringValue buildMonitoringValue(JSONObject monitoringValuesObj) throws ParseMonitoringUnitsException {
        MonitoringValue monitoringValue;
        String type = (String) monitoringValuesObj.get("type");
        switch((String) monitoringValuesObj.get("kind")) {
            case VKIND_FIELD: monitoringValue = new MonitoringValueField((String) monitoringValuesObj.get("info"), type); break;
            case VKIND_PARAMETER: monitoringValue = new MonitoringValueParameter((int) monitoringValuesObj.get("info"), type); break;
            case VKIND_RETURN: monitoringValue = new MonitoringValueReturn(type); break;
            default: throw new ParseMonitoringUnitsException("Cannot parse the content. Check its syntax.");
        }

        if (isObject(type)) buildMonitoringValueMethodsChain(monitoringValuesObj, monitoringValue);

        return monitoringValue;
    }

    private static void buildMonitoringValueMethodsChain(JSONObject monitoringValuesObj, MonitoringValue monitoringValue) throws ParseMonitoringUnitsException {
        JSONArray methodsList = (JSONArray) monitoringValuesObj.get("methods");
        if (methodsList.size() == 0) throw new ParseMonitoringUnitsException("Cannot parse the content. Check its syntax.");
        MonitoringValueMethod temp = null;
        for (Object o : methodsList) {
            JSONObject methodObj = (JSONObject) o;
            JSONObject methodInfoObj = (JSONObject) methodObj.get("method");
            String className = (String) methodObj.get("class");
            boolean isVirtual = (boolean) methodInfoObj.get("isVirtual");
            String desc = (String) methodInfoObj.get("desc");

            MonitoringValueMethod monitoringValueMethod = new MonitoringValueMethod(isVirtual, className, desc);
            if (temp != null) {
                temp.setNextMethod(monitoringValueMethod);
                temp = monitoringValueMethod;
            } else {
                monitoringValue.setNextMethod(monitoringValueMethod);
                temp = monitoringValueMethod;
            }
        }
    }

    private static boolean isObject(String type) {
        return type.startsWith("L");
    }

    @Deprecated
    public static ArrayList<MonitoringUnit> getMonitoringUnitsFromFile(Path inputFile) throws IOException {
        ArrayList<MonitoringUnit> monitoringUnitList = new ArrayList<>();
        try (BufferedReader br = Files.newBufferedReader(inputFile)) {
            String monitoringUnitsString;
            while((monitoringUnitsString = br.readLine()) != null) {
                if(!monitoringUnitsString.startsWith(C.ENTER) && !monitoringUnitsString.startsWith(C.EXIT)) throw new IOException("The input file's format is wrong.");
                String[] elements = monitoringUnitsString.split(C.DDELIM);
                if(elements.length < 2) throw new IOException("The input file's format is wrong.");
                String className = elements[0].substring(C.SSIZE);
                boolean isVirtual = elements[1].startsWith(C.VIRTUAL);
                String methodNameDesc = elements[1].substring(C.SSIZE);

                MonitoringUnit monitoringUnit = new MonitoringUnit(className, methodNameDesc, isVirtual);
                monitoringUnit.setEnter(monitoringUnitsString.startsWith(C.ENTER));

                for(int i = 2; i < elements.length; i++) {
                    if(!elements[i].startsWith(C.VO) || !elements[i].endsWith(C.VC)) throw new IOException("The input file's format is wrong.");
                    elements[i] = elements[i].substring(1, elements[i].length() - 1);
                    String[] elementsToBeMonitored = elements[i].split(C.VDELIM);
                    if(elementsToBeMonitored.length == 0 || elementsToBeMonitored.length % 2 != 0) throw new IOException("The input file's format is wrong.");

                    MonitoringValue newValue = null;
                    if(elementsToBeMonitored[0].startsWith(C.VFIELD)
                            || elementsToBeMonitored[0].startsWith(C.VPARAMETER)
                            || elementsToBeMonitored[0].startsWith(C.VRETURN)) {
                        boolean isObject = elementsToBeMonitored[1].startsWith("L");
                        if((isObject && elementsToBeMonitored.length <= 2)
                                || !isObject && elementsToBeMonitored.length > 2)
                            throw new IOException("The input file's format is wrong.");

                        if(elementsToBeMonitored[0].startsWith(C.VFIELD))
                            newValue = new MonitoringValueField(elementsToBeMonitored[0].substring(1), elementsToBeMonitored[1]);
                        else if (elementsToBeMonitored[0].startsWith(C.VPARAMETER))
                            newValue = new MonitoringValueParameter(Integer.parseInt(elementsToBeMonitored[0].substring(1)), elementsToBeMonitored[1]);
                        else
                            newValue = new MonitoringValueReturn(elementsToBeMonitored[1]);

                        if(isObject) {
                            MonitoringValueMethod tempValue = new MonitoringValueMethod(elementsToBeMonitored[2], elementsToBeMonitored[3]);
                            newValue.setNextMethod(tempValue);
                            addNextMethods(elementsToBeMonitored, tempValue, 4);
                        }
                    } else {
                        newValue = new MonitoringValueMethod(elementsToBeMonitored[0], elementsToBeMonitored[1]);;

                        MonitoringValueMethod tempValue = (MonitoringValueMethod) newValue;
                        addNextMethods(elementsToBeMonitored, tempValue, 2);
                    }
                    monitoringUnit.addMonitoringValue(newValue);
                }
                monitoringUnitList.add(monitoringUnit);
            }
        }
        return monitoringUnitList;
    }

    @Deprecated
    private static void addNextMethods(String[] elementsToBeMonitored, MonitoringValueMethod beginValue, int beginIndex) {
        for(int k = beginIndex; k < elementsToBeMonitored.length; k += 2) {
            MonitoringValueMethod nextMethod = new MonitoringValueMethod(elementsToBeMonitored[k], elementsToBeMonitored[k + 1]);
            beginValue.setNextMethod(nextMethod);
            beginValue = nextMethod;
        }
    }


    private HashSet<String> isCopied = new HashSet<>();

    public void instrument(final Path classpath, final ArrayList<MonitoringUnit> monitoringUnits, boolean isDebugMode) throws InstrumentationException {
        for(MonitoringUnit monitoringUnit : monitoringUnits) {
            String className = monitoringUnit.getClassName();
            ClassReader classReader;
            try{
                classReader = getClassReader(classpath, className);
            } catch (IOException e) { throw new InstrumentationException("Reading a class is failed: " + className); }
            ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS);
            JRIEXTClassVisitor classVisitor = new JRIEXTClassVisitor(classWriter, monitoringUnit, isDebugMode);
            classReader.accept(classVisitor, ClassReader.SKIP_FRAMES);
            try{
                printClass(className, classWriter);
            } catch (IOException e) { throw new InstrumentationException("Writing a class to the cache folder is failed: " + className); }
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
        } catch (IOException e) { throw new InstrumentationException("Copying classes that are not instrumented is failed."); }

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
        String dirName = defaultDirName;
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
