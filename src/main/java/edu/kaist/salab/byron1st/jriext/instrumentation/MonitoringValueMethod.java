package edu.kaist.salab.byron1st.jriext.instrumentation;

/**
 * Created by byron1st on 2016. 1. 10..
 */
public class MonitoringValueMethod extends MonitoringValue {
    private boolean isVirtual;
    private String methodName;
    private String methodDesc;
    private String returnType;

    public MonitoringValueMethod(String className, String methodNameDesc) {
        //TODO: Signal들을 어디서 제거할지 (여기 또는 InstApp) 결정
        this(methodNameDesc.startsWith(C.VIRTUAL), className, methodNameDesc.substring(C.SSIZE));
    }

    public MonitoringValueMethod(boolean isVirtual, String className, String methodNameDesc) {
        super(className);
        this.isVirtual = isVirtual;
        int index0 = methodNameDesc.indexOf("(");
        int index1 = methodNameDesc.indexOf(")");
        this.methodName = methodNameDesc.substring(0, index0);
        this.methodDesc = methodNameDesc.substring(index0);
        this.returnType = methodNameDesc.substring(index1 + 1);
    }

    public MonitoringValueMethod(boolean isVirtual, String className, String methodName, String methodDesc, String returnType) {
        super(className);
        this.isVirtual = isVirtual;
        this.methodName = methodName;
        this.methodDesc = methodDesc;
        this.returnType = returnType;
    }

    public boolean isVirtual() {
        return isVirtual;
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

    public boolean isFinal() {
        return this.nextMethod == null;
    }
}
