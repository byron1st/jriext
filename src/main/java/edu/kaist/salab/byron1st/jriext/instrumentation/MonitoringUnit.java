package edu.kaist.salab.byron1st.jriext.instrumentation;

import java.util.ArrayList;

/**
 * Created by byron1st on 2016. 1. 7..
 */
public class MonitoringUnit {
    private boolean isEnter;
    private boolean isVirtual;
    private String id;
    private String className;
    private String methodName;
    private String methodDesc;
    private String returnType;
    private ArrayList<MonitoringValue> monitoringValues = new ArrayList<>();

    public MonitoringUnit(String className, String methodNameDesc, boolean isVirtual) {
        this.className = className;
        int index = methodNameDesc.indexOf("(");
        this.methodName = methodNameDesc.substring(0, index);
        this.methodDesc = methodNameDesc.substring(index);
        this.returnType = methodNameDesc.substring(methodNameDesc.indexOf(")") + 1);
        this.isVirtual = isVirtual;
        this.id = this.className + "_" + this.methodName + this.methodDesc;
    }

    public boolean isEnter() {
        return isEnter;
    }

    public void setEnter(boolean enter) {
        isEnter = enter;
    }

    public boolean isVirtual() {
        return isVirtual;
    }

    public String getClassName() {
        return className;
    }

    public String getMethodName() {
        return methodName;
    }

    public String getMethodDesc() {
        return methodDesc;
    }

    public String getReturnType() {
        return returnType;
    }

    public String getId() {
        return id;
    }

    public void addMonitoringValue(MonitoringValue monitoringValue) {
        this.monitoringValues.add(monitoringValue);
    }

    public ArrayList<MonitoringValue> getMonitoringValues() {
        return monitoringValues;
    }
}
