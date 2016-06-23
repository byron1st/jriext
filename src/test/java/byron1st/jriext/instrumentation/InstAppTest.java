package byron1st.jriext.instrumentation;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.json.simple.JSONArray;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.BufferedWriter;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by byron1st on 2016. 5. 5..
 */
public class InstAppTest {
    private static ArrayList<MonitoringUnit> monitoringUnits;

    @BeforeClass
    public static void setUp() throws Exception {
        URI muURI = Thread.currentThread().getContextClassLoader().getResource("netty/monitoringUnits.json").toURI();
        monitoringUnits = InstApp.parse(Paths.get(muURI));
    }

    @Test
    public void convertLogs2JSON() throws Exception {
        Path logPath = Paths.get(Thread.currentThread().getContextClassLoader().getResource("netty/io_netty_example_echo_EchoServer0-1466657407477.murecords").toURI());

        HashMap<String, ArrayList<String>> returnedList = new HashMap<>();

        assert monitoringUnits != null;
        monitoringUnits.forEach(monitoringUnit -> returnedList.put(monitoringUnit.getMuID(), monitoringUnit.getValueIDs()));

        ImmutablePair<JSONArray, ArrayList<ImmutablePair<String, String>>> result = InstApp.convertLogs2JSON(logPath, returnedList);

        Path jsonFileOutput = Paths.get("/Users/byron1st/Downloads/netty-records.json");
        Files.createFile(jsonFileOutput);
        try(BufferedWriter bw = Files.newBufferedWriter(jsonFileOutput)) {
            result.getLeft().writeJSONString(bw);
        }
    }
}