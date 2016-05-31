package byron1st.jriext.instrumentation;

/**
 * Created by byron1st on 2016. 1. 10..
 */
public class MonitoringValueField extends MonitoringValue {
    private String fieldName;

    public MonitoringValueField(String valueID, String fieldName, String typeClassName) {
        super(valueID, typeClassName);
        this.fieldName = fieldName;
    }

    public String getFieldName() {
        return fieldName;
    }
}
