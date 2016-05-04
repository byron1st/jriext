package edu.kaist.salab.byron1st.jriext;

import edu.kaist.salab.byron1st.jriext.digraph.JRIEXTGraph;
import edu.kaist.salab.byron1st.jriext.digraph.JRIEXTEdge;
import edu.kaist.salab.byron1st.jriext.digraph.JRIEXTNode;
import edu.kaist.salab.byron1st.jriext.instrumentation.C;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Stack;

/**
 * Created by byron1st on 2016. 1. 11..
 */
public class GraphGenApp {
    private static GraphGenApp ourInstance = new GraphGenApp();
    public static GraphGenApp getInstance() {
        return ourInstance;
    }
    private GraphGenApp() {
    }

    public JRIEXTGraph generate(File logFile, HashMap<String, Boolean> isComponent) throws IOException {
        JRIEXTGraph graph = new JRIEXTGraph();
        HashMap<String, LinkedList<LogLine>> classifiedLogLines = new HashMap<>();
        try(BufferedReader br = Files.newBufferedReader(logFile.toPath())) {
            String line;
            while((line = br.readLine()) != null) {
                LogLine logLine = new LogLine(line);
                LinkedList<LogLine> logLines;
                if(classifiedLogLines.containsKey(logLine.threadID))
                    logLines = classifiedLogLines.get(logLine.threadID);
                else {
                    logLines = new LinkedList<>();
                    classifiedLogLines.put(logLine.threadID, logLines);
                }
                logLines.add(logLine);
            }
        }

        for (String threadID : classifiedLogLines.keySet()) {
            LinkedList<LogLine> threadQueue = classifiedLogLines.get(threadID);
            Stack<LogLine> stack = new Stack<>();

            while(!threadQueue.isEmpty()) {
                LogLine currentLogLine = threadQueue.poll();
                if(isComponent.get(currentLogLine.id)) {
                    //TODO: static 일 경우 고려 안됨
                    getEdgeBetweenThreadAndObject(graph, currentLogLine);
                } else {
                    if(!currentLogLine.isExit) {
                        getEdgeBetweenThreadAndObject(graph, currentLogLine);
                        stack.push(currentLogLine);
                        continue;
                    }

                    stack.pop();
                    if(!stack.isEmpty()) {
                        LogLine sourceLogLine = stack.peek();
                        getEdgeBetweenObjectAndObject(graph, currentLogLine, sourceLogLine);
                    }
                }
            }
        }


        return graph;
    }

    private void getEdgeBetweenObjectAndObject(JRIEXTGraph graph, LogLine currentLogLine, LogLine sourceLogLine) {
        JRIEXTNode sourceNode = getNode(graph, sourceLogLine);
        JRIEXTNode sinkNode = getNode(graph, currentLogLine);
        JRIEXTEdge edge = new JRIEXTEdge(currentLogLine.method, currentLogLine.time, sourceNode, sinkNode, currentLogLine.threadID);
        edge.setDesc(currentLogLine.threadName + "," + currentLogLine.desc);
        graph.addNewEdge(edge);
    }

    private void getEdgeBetweenThreadAndObject(JRIEXTGraph graph, LogLine currentLogLine) {
        JRIEXTNode node = getNode(graph, currentLogLine);
        JRIEXTEdge edge = new JRIEXTEdge(currentLogLine.method, currentLogLine.time, null, node, currentLogLine.threadID);
        edge.setDesc(currentLogLine.threadName + "," + currentLogLine.desc);
        graph.addNewEdge(edge);
    }

    private JRIEXTNode getNode(JRIEXTGraph graph, LogLine currentLogLine) {
        JRIEXTNode node;
        if(graph.containsNode(currentLogLine.objectID)) {
            node = graph.getNode(currentLogLine.objectID);
            if(!node.getDesc().equals(currentLogLine.desc))
                node.setDesc(node.getDesc() + C.DDELIM + currentLogLine.desc);
        }
        else {
            node = new JRIEXTNode(currentLogLine.objectID, currentLogLine.className);
            node.setDesc(currentLogLine.desc);
            graph.addNewNode(node);
        }
        return node;
    }

    private class LogLine {
        private boolean isExit;
        private String id;
        private String time;
        private String threadName;
        private String threadID;
        private String objectID;
        private String className;
        private String method;
        private String desc;

        LogLine(String logString) {
            String[] elements = logString.split(C.DDELIM);
            this.isExit = logString.startsWith(C.EXIT);
            this.time = elements[0].substring(1);
            this.threadName = elements[1];
            this.threadID = elements[2];
            this.objectID = elements[3];
            this.className = elements[4];
            this.method = elements[5];
            this.desc = "";
            for(int i = 6; i < elements.length; i++) {
                this.desc += elements[i] + C.DDELIM;
            }
            this.id = className + "_" + method;
        }
    }
}
