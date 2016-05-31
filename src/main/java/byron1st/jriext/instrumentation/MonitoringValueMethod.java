package byron1st.jriext.instrumentation;

/**
 * Created by byron1st on 2016. 1. 10..
 */
public class MonitoringValueMethod extends MonitoringValue {
    private boolean isVirtual;
    private String methodName;
    private String methodDesc;
    private String returnType;

    public MonitoringValueMethod(boolean isVirtual, String className, String methodNameDesc, String valueID) {
        super(valueID, className);
        this.isVirtual = isVirtual;
        int index0 = methodNameDesc.indexOf("(");
        int index1 = methodNameDesc.indexOf(")");
        this.methodName = methodNameDesc.substring(0, index0);
        this.methodDesc = methodNameDesc.substring(index0);
        this.returnType = methodNameDesc.substring(index1 + 1);
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
