package byron1st.jriext.instrumentation;

import org.junit.BeforeClass;
import org.junit.Test;

import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Created by byron1st on 2016. 5. 5..
 */
public class InstAppTest {
    private static Path monitoringUnitsPath;

    @BeforeClass
    public static void setUp() throws Exception {
        URI configURI = Thread.currentThread().getContextClassLoader().getResource("monitoringUnits.json").toURI();
        monitoringUnitsPath = Paths.get(configURI);
    }

    @Test
    public void parse() throws Exception {
        InstApp.parse(monitoringUnitsPath);
    }
}