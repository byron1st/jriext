package edu.kaist.salab.byron1st.jriext.view;

import edu.kaist.salab.byron1st.jriext.GraphGenApp;
import edu.kaist.salab.byron1st.jriext.digraph.DOTGenApp;
import edu.kaist.salab.byron1st.jriext.digraph.JRIEXTGraph;
import edu.kaist.salab.byron1st.jriext.instrumentation.InstApp;
import edu.kaist.salab.byron1st.jriext.instrumentation.MonitoringUnit;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.net.URL;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by byron1st on 2016. 1. 12..
 */
public class DOTGenAppTest {
    private static JRIEXTGraph graph;
    private static HashMap<String, Boolean> isComponent;

    @BeforeClass
    public static void setUp() throws Exception {
        URL testData = Thread.currentThread().getContextClassLoader().getResource("framework_PFSystemMain_temp.txt");
        URL testDataComponents = Thread.currentThread().getContextClassLoader().getResource("testComponents.csv");
        URL testDataConnectors = Thread.currentThread().getContextClassLoader().getResource("testConnectors.csv");

        ArrayList<MonitoringUnit> components = InstApp.getMonitoringUnitsFromFile(Paths.get(testDataComponents.toURI()));
        ArrayList<MonitoringUnit> connectors = InstApp.getMonitoringUnitsFromFile(Paths.get(testDataConnectors.toURI()));
        isComponent = new HashMap<>();
        components.forEach(monitoringUnit -> isComponent.put(monitoringUnit.getId(), true));
        connectors.forEach(monitoringUnit -> isComponent.put(monitoringUnit.getId(), false));

        graph = GraphGenApp.getInstance().generate(new File(testData.toURI()), isComponent);
    }


    @Test
    public void testGenerate() throws Exception {
        DOTGenApp.getInstance().generate(graph, "testDOT");
    }
}