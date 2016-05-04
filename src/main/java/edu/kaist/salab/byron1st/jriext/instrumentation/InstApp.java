package edu.kaist.salab.byron1st.jriext.instrumentation;

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
    private static void deleteCacheFolderIfExists() {
        Path cache = Paths.get(System.getProperty("user.dir"), "cache");
        if(Files.exists(cache)) {
            try {
                Files.walkFileTree(cache, new FileVisitor<Path>() {
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

    public static Path CACHE_ROOT = Paths.get(System.getProperty("user.dir"), "cache");
    public static String defaultDirName = System.getProperty("user.dir") + File.separator + "cache";

    private HashSet<String> isCopied = new HashSet<>();

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

    private static void addNextMethods(String[] elementsToBeMonitored, MonitoringValueMethod beginValue, int beginIndex) {
        for(int k = beginIndex; k < elementsToBeMonitored.length; k += 2) {
            MonitoringValueMethod nextMethod = new MonitoringValueMethod(elementsToBeMonitored[k], elementsToBeMonitored[k + 1]);
            beginValue.setNextMethod(nextMethod);
            beginValue = nextMethod;
        }
    }

    public void instrument(final Path classpath, final ArrayList<MonitoringUnit> monitoringUnits, boolean isDebugMode) throws NotValidInputException, IOException {
        if(!validateInputs(classpath, monitoringUnits))
            throw new NotValidInputException();

        for(MonitoringUnit monitoringUnit : monitoringUnits) {
            String className = monitoringUnit.getClassName();
            ClassReader classReader = getClassReader(classpath, className);
            ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS);
            JRIEXTClassVisitor classVisitor = new JRIEXTClassVisitor(classWriter, monitoringUnit, isDebugMode);
            classReader.accept(classVisitor, ClassReader.SKIP_FRAMES);
            printClass(className, classWriter);
        }

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

    private boolean validateInputs(final Path classpath, Object additionalInput) {
        return classpath != null
                && Files.exists(classpath)
                && Files.isDirectory(classpath)
                && additionalInput != null;
    }

    public class NotValidInputException extends Exception {}

    /*Test*******************************************************************/
    void runPrintClass(String className, ClassWriter classWriter) throws IOException { printClass(className, classWriter); }
}
