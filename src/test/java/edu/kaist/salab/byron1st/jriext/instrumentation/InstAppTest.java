package edu.kaist.salab.byron1st.jriext.instrumentation;

import org.junit.BeforeClass;
import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

import static org.junit.Assert.assertEquals;

/**
 * Created by byron1st on 2015. 11. 13..
 */
public class InstAppTest {
    private InstApp app = InstApp.getInstance();
    private static Path classpath;
    private static ArrayList<MonitoringUnit> newCombinedMonitoringUnits = new ArrayList<>();

    @BeforeClass
    public static void setUp() throws Exception {
        URL testDataClasspath = Thread.currentThread().getContextClassLoader().getResource("bin");
        classpath = Paths.get(testDataClasspath.toURI());
        URL testDataCombined = Thread.currentThread().getContextClassLoader().getResource("testCombined.csv");
        newCombinedMonitoringUnits = InstApp.getMonitoringUnitsFromFile(Paths.get(testDataCombined.toURI()));
        URL testParameters = Thread.currentThread().getContextClassLoader().getResource("testParameters.csv");
        newCombinedMonitoringUnits.addAll(InstApp.getMonitoringUnitsFromFile(Paths.get(testParameters.toURI())));
        URL testReturn = Thread.currentThread().getContextClassLoader().getResource("testReturn.csv");
        newCombinedMonitoringUnits.addAll(InstApp.getMonitoringUnitsFromFile(Paths.get(testReturn.toURI())));
    }

    @Test
    public void testGetMonitoringUnitsFromFile() throws Exception {
        assertEquals(25, newCombinedMonitoringUnits.size());
        assertEquals(0, newCombinedMonitoringUnits.get(9).getMonitoringValues().size());
        assertEquals(6, newCombinedMonitoringUnits.get(1).getMonitoringValues().size());
        MonitoringValueMethod method = (MonitoringValueMethod) newCombinedMonitoringUnits.get(1).getMonitoringValues().get(2);
        assertEquals("java/net/Socket", method.getClassName());
        MonitoringValueMethod method2 = method.getNextMethod();
        assertEquals("java/net/InetAddress", method2.getClassName());
        assertEquals(true, newCombinedMonitoringUnits.get(22).getMonitoringValues().get(0) instanceof MonitoringValueField);
        assertEquals("target", ((MonitoringValueField) newCombinedMonitoringUnits.get(22).getMonitoringValues().get(0)).getFieldName());
    }

    @Test(expected = InstApp.NotValidInputException.class)
    public void testNotValidInputNull1() throws Exception {
        app.instrument(null, newCombinedMonitoringUnits, false);
    }

    @Test(expected = InstApp.NotValidInputException.class)
    public void testNotValidInputNull2() throws Exception {
        app.instrument(classpath, null, false);
    }

    @Test(expected = InstApp.NotValidInputException.class)
    public void testNotValidInputNotDirectory() throws Exception {
        app.instrument(Paths.get(System.getProperty("user.dir"), "jpav-rev.iml"), newCombinedMonitoringUnits, false);
    }

    @Test(expected = InstApp.NotValidInputException.class)
    public void testNotValidInputNoFile() throws Exception {
        app.instrument(Paths.get(System.getProperty("user.dir"), "noSuchFile.txt"), newCombinedMonitoringUnits, false);
    }

    @Test
    public void testPrintClass() throws Exception {
        String className = "java/lang/Integer";
        Path path = Paths.get(System.getProperty("user.dir"), "cache", "java", "lang", "Integer.class");

        assertEquals(false, Files.exists(path));
        app.runPrintClass(className, new ClassWriter(new ClassReader(className), ClassWriter.COMPUTE_MAXS));
        assertEquals(true, Files.exists(path));
    }

    @Test
    public void testInstrument() throws Exception {
        app.instrument(classpath, newCombinedMonitoringUnits, false);
    }
}