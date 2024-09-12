package org.ton.java.address;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.ton.java.fift.FiftRunner;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;

@Slf4j
@RunWith(JUnit4.class)
public class TestAddressWithFift {
    @Test
    public void testAddressWithFift() throws URISyntaxException {

        URL resource = TestAddressWithFift.class.getResource("/test-address.fif");
        File fiftFile = Paths.get(resource.toURI()).toFile();
        String absolutePath = fiftFile.getAbsolutePath();

        FiftRunner fiftRunner = FiftRunner.builder().build();

        String result = fiftRunner.run(fiftFile.getParent(), "-s", absolutePath, "EQDkZIvi6fkgNVxLOgw5hzquGhxhUvhvJ4B836p-NQ-iLKCv");
        log.info("output: {}", result);

        result = fiftRunner.run(fiftFile.getParent(), "-s", absolutePath, "Ef--_tW1zCy5ehTig0JrBEAiE67bbnSmqVtkuU8FFqlNUT9n");
        log.info("output: {}", result);

        result = fiftRunner.run(fiftFile.getParent(), "-s", absolutePath, "-1:0000d5b5cc2cb97a14e283426b04402213aedb6e74a6a95b64b94f0516a90000");
        log.info("output: {}", result);

        result = fiftRunner.run(fiftFile.getParent(), "-s", absolutePath, "EQCnuv-ZuR0QsIh5vwxUBuzzocSowbCa7ctdwl6QizBKzGVM");
        log.info("output: {}", result);
    }
}
