package edu.kaist.salab.byron1st.jriext.digraph;

/**
 * Created by byron1st on 2016. 1. 11..
 */
public class JRIEXTEdge {
    private JRIEXTNode source;
    private JRIEXTNode sink;
    private String name;
    private String time;
    private String desc = "";
    private String threadID;

    public JRIEXTEdge(String name, String time, JRIEXTNode source, JRIEXTNode sink, String threadID) {
        this.name = name;
        this.time = time;
        this.source = source;
        this.sink = sink;
        if(source != null) source.addOutEdge(this);
        if(sink != null) sink.addInEdge(this);
        this.threadID = threadID;
    }

    public JRIEXTNode getSource() {
        return source;
    }

    public JRIEXTNode getSink() {
        return sink;
    }

    public String getName() {
        return name;
    }

    public String getTime() {
        return time;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public String getThreadID() {
        return threadID;
    }
}
