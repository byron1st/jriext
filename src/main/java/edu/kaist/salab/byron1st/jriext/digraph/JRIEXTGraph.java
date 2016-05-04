package edu.kaist.salab.byron1st.jriext.digraph;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by byron1st on 2016. 1. 11..
 */
public class JRIEXTGraph {
    private HashMap<String, JRIEXTNode> nodes = new HashMap<>();
    private ArrayList<JRIEXTEdge> edges = new ArrayList<>();

    public void addNewNode(JRIEXTNode newNode) {
        this.nodes.put(newNode.getId(), newNode);
    }

    public boolean containsNode(String id) {
        return this.nodes.containsKey(id);
    }

    public JRIEXTNode getNode(String id) {
        return this.nodes.get(id);
    }

    public void addNewEdge(JRIEXTEdge newEdge) {
        this.edges.add(newEdge);
    }

    public HashMap<String, JRIEXTNode> getNodes() {
        return nodes;
    }

    public ArrayList<JRIEXTEdge> getEdges() {
        return edges;
    }

    public void printEdges() throws IOException {
        try(BufferedWriter bw = Files.newBufferedWriter(Paths.get(System.getProperty("user.dir"), "testResult.txt"))) {
            for (JRIEXTEdge edge : edges) {
                String printLine = edge.getTime() + " " + edge.getThreadID() + " " + edge.getName() + ": ";
                if(edge.getSource() != null) printLine += edge.getSource().getId();
                else printLine += "null";
                printLine += " ";
                if(edge.getSink() != null) printLine += edge.getSink().getId();
                else printLine += "null";
                printLine += " " + edge.getDesc() + "\n";
                bw.write(printLine);
            }
        }
    }
}
