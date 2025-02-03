package org.ton.java.fift;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@Slf4j
@RunWith(JUnit4.class)
public class TestFiftRunner {

  /**
   * Make sure you have fift and func installed in your system. See <a
   * href="https://github.com/ton-blockchain/packages">packages</a> for instructions.
   */
  @Test
  public void testFiftRunner() throws URISyntaxException {

    URL resource = TestFiftRunner.class.getResource("/test.fift");
    File fiftFile = Paths.get(resource.toURI()).toFile();
    String absolutePath = fiftFile.getAbsolutePath();

    String fiftPath = "https://github.com/neodix42/ton/releases/download/v2025.01-19/fift.exe";
    String libPath = System.getProperty("user.dir") + "/../2.ton-test-artifacts/lib";
    String smartcontPath = System.getProperty("user.dir") + "/../2.ton-test-artifacts/smartcont";

    FiftRunner fiftRunner =
        FiftRunner.builder()
            .fiftExecutablePath(fiftPath)
            .fiftAsmLibraryPath(libPath)
            .fiftSmartcontLibraryPath(smartcontPath)
            .build();

    String result = fiftRunner.run(fiftFile.getParent(), "-s", absolutePath);
    log.info("output: {}", result);
  }

  @Test
  public void testFiftRunnerDownload() throws URISyntaxException {

    URL resource = TestFiftRunner.class.getResource("/test.fift");
    File fiftFile = Paths.get(resource.toURI()).toFile();
    String absolutePath = fiftFile.getAbsolutePath();

    String fiftPath = "https://github.com/neodix42/ton/releases/download/v2025.01-19/fift.exe";

    FiftRunner fiftRunner = FiftRunner.builder().fiftExecutablePath(fiftPath).build();

    String result = fiftRunner.run(fiftFile.getParent(), "-s", absolutePath);
    log.info("output: {}", result);
  }
}
