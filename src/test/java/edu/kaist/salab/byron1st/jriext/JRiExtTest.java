package edu.kaist.salab.byron1st.jriext;

import org.junit.BeforeClass;
import org.junit.Test;

import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Created by byron1st on 2016. 5. 5..
 */
public class JRiExtTest {
    private static Path configFile;

    @BeforeClass
    public static void setUp() throws Exception {
        URI configURI = Thread.currentThread().getContextClassLoader().getResource("config.json").toURI();
        configFile = Paths.get(configURI);
    }

    @Test
    public void testValidateConfig() throws Exception {
        String[] correctArgs = {configFile.toString()};

        JRiExt.testValidateConfig(correctArgs);
    }

    @Test
    public void testExtractClasspathList() throws Exception {
        String[] correctArgs = {configFile.toString()};

        JRiExt.testExtractClasspathList(correctArgs);
    }

}