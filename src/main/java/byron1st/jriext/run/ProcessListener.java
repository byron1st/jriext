package byron1st.jriext.run;

import byron1st.jriext.instrumentation.InstApp;

import java.io.IOException;

/**
 * Created by byron1st on 2016. 5. 8..
 */
public interface ProcessListener {
    void listenProcessDeath() throws InstApp.ConvertLogsToJSONException, IOException;
}
