package org.ton.java.smartcontract.unittests;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.ton.java.smartcontract.FiftRunner;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;

@Slf4j
@RunWith(JUnit4.class)
public class TestFiftRunner {

    /**
     * Make sure you have fift and func installed in your system. See <a href="https://github.com/ton-blockchain/packages">packages</a> for instructions.
     */
    @Test
    public void testFiftRunner() throws URISyntaxException {

        URL resource = TestFiftRunner.class.getResource("/fift/test.fift");
        File fiftFile = Paths.get(resource.toURI()).toFile();
        String absolutePath = fiftFile.getAbsolutePath();

        FiftRunner fiftRunner = FiftRunner.builder().build();

        String result = fiftRunner.run(fiftFile.getParent(), "-s", absolutePath);
        log.info("output: {}", result);
    }
}
