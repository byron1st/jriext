package edu.kaist.salab.byron1st.jriext.digraph;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;

/**
 * Created by byron1st on 2015. 11. 19..
 */
public class DOTGenApp {
    private static DOTGenApp ourInstance = new DOTGenApp();
    public static DOTGenApp getInstance() {
        return ourInstance;
    }
    private DOTGenApp() {
    }

    public File generate(JRIEXTGraph graph, String outputFileName) throws IOException {
        HashMap<String, String> edgeIDs = new HashMap<>();
        HashMap<String, JRIEXTEdge> edges = new HashMap<>();

        Path outputDOTFile = Paths.get(System.getProperty("user.dir"), outputFileName + ".dot");
        try(BufferedWriter bw = Files.newBufferedWriter(outputDOTFile)) {
            bw.write("strict digraph ViewGraph {\n");
            bw.write("node [shape=circle]; ");
            for (String id : graph.getNodes().keySet()) {
                bw.write(id + "; ");
            }

            int index = 0;
            for (JRIEXTEdge edge : graph.getEdges()) {
                boolean isinThread = edge.getSource() == null;
                String sinkID = edge.getSink().getId();
                String methodName;
                String edgeID;

                if(isinThread) methodName = edge.getName() + edge.getThreadID() + sinkID;
                else methodName = edge.getName() + edge.getSource().getId() + sinkID;

                if(edgeIDs.containsKey(methodName)) edgeID = edgeIDs.get(methodName);
                else {
                    edgeID = "e" + index++;
                    edgeIDs.put(methodName, edgeID);
                    edges.put(edgeID, edge);
                }

                if(isinThread) bw.write(edge.getThreadID() + "->" + edgeID + " [arrowhead=none, style=dashed];\n");
                else bw.write(edge.getSource().getId() + "->" + edgeID + ";\n");
                bw.write(edgeID + "->" + sinkID + ";\n");
            }

            bw.write("\n}");
        }

        Path outputSupplementsForNodes = Paths.get(System.getProperty("user.dir"), outputFileName + "_nodes.csv");
        try(BufferedWriter bw = Files.newBufferedWriter(outputSupplementsForNodes)) {
            for (JRIEXTNode node : graph.getNodes().values()) {
                bw.write(node.getId() + "," + node.getName() + "," + node.getDesc() + "\n");
            }
        }

        Path outputSupplementsForEdges = Paths.get(System.getProperty("user.dir"), outputFileName + "_edges.csv");
        try(BufferedWriter bw = Files.newBufferedWriter(outputSupplementsForEdges)) {
            for (String edgeID : edgeIDs.values()) {
                JRIEXTEdge edge = edges.get(edgeID);
                bw.write(edgeID + "," + edge.getName() + "," + edge.getThreadID() + "," + edge.getDesc() + "\n");
            }
        }

        return outputDOTFile.toFile();
    }
}
