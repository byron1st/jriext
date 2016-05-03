package edu.kaist.salab.jriext.digraph;

import edu.kaist.salab.jriext.GraphGenApp;
import edu.kaist.salab.jriext.instrumentation.InstApp;
import edu.kaist.salab.jriext.instrumentation.MonitoringUnit;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.net.URL;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by byron1st on 2016. 1. 11..
 */
public class GraphGenAppTest {
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
        assertEquals("java/lang/Thread", graph.getNode("1463801669").getName());
        assertEquals("java/io/PipedInputStream", graph.getNode("545423422").getName());
        boolean exist = false;
        for (JRIEXTEdge edge : graph.getNode("2135163782").getOutEdges())
            if(edge.getSink().getId().equals("545423422")) exist = true;
        assertTrue(exist);
    }
}
