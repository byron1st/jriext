package edu.kaist.salab.jriext.digraph;

import java.util.ArrayList;

/**
 * Created by byron1st on 2016. 1. 11..
 */
public class JRIEXTNode {
    private String id;
    private String name;
    private String desc = "";
    private ArrayList<JRIEXTEdge> inEdges = new ArrayList<>();
    private ArrayList<JRIEXTEdge> outEdges = new ArrayList<>();

    public JRIEXTNode(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public void addInEdge(JRIEXTEdge newEdge) {
        inEdges.add(newEdge);
    }

    public void addOutEdge(JRIEXTEdge newEdge) {
        outEdges.add(newEdge);
    }

    public ArrayList<JRIEXTEdge> getInEdges() {
        return inEdges;
    }

    public ArrayList<JRIEXTEdge> getOutEdges() {
        return outEdges;
    }
}
