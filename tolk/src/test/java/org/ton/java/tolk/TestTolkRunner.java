package org.ton.java.tolk;

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
public class TestTolkRunner {
  /**
   * Make sure you have tolk and func installed in your system. See <a
   * href="https://github.com/ton-blockchain/packages">packages</a> for instructions.
   */
  @Test
  public void testTolkRunner() throws URISyntaxException {

    URL resource = TestTolkRunner.class.getResource("/test.tolk");
    File tolkFile = Paths.get(resource.toURI()).toFile();
    String absolutePath = tolkFile.getAbsolutePath();

    String tolkPath = System.getProperty("user.dir") + "/2.ton-test-artifacts/tolk.exe";
    TolkRunner tolkRunner = TolkRunner.builder().tolkExecutablePath(tolkPath).build();

    String result = tolkRunner.run(tolkFile.getParent(), absolutePath);
    log.info("output: {}", result);
  }

  @Test
  public void testTolkRunnerDownload() throws URISyntaxException {

    URL resource = TestTolkRunner.class.getResource("/test.tolk");
    File tolkFile = Paths.get(resource.toURI()).toFile();
    String absolutePath = tolkFile.getAbsolutePath();

    TolkRunner tolkRunner =
        TolkRunner.builder()
            .tolkExecutablePath(
                "https://github.com/ton-blockchain/ton/releases/download/v2024.12-1/tolk.exe")
            .build();

    String result = tolkRunner.run(tolkFile.getParent(), absolutePath);
    log.info("output: {}", result);
  }
}
