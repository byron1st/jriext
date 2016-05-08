package byron1st.jriext.instrumentation;

/**
 * Created by byron1st on 2016. 1. 10..
 */
public class MonitoringValue {
    protected String className;
    protected MonitoringValueMethod nextMethod = null;

    protected MonitoringValue(String className) {
        this.className = className;
    }

    public String getClassName() {
        return className;
    }

    public void setNextMethod(MonitoringValueMethod nextMethod) {
        this.nextMethod = nextMethod;
    }

    public MonitoringValueMethod getNextMethod() {
        return nextMethod;
    }
}
