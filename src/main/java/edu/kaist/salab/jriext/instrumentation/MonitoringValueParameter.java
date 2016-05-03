package edu.kaist.salab.jriext.instrumentation;

/**
 * Created by byron1st on 2016. 1. 13..
 */
public class MonitoringValueParameter extends MonitoringValue {
    private int index;

    public MonitoringValueParameter(int index, String className) {
        super(className);
        this.index = index;
    }

    public int getIndex() {
        return index;
    }
}
