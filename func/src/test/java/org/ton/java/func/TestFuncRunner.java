package org.ton.java.func;

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
public class TestFuncRunner {
  /**
   * Make sure you have fift and func installed in your system. See <a
   * href="https://github.com/ton-blockchain/packages">packages</a> for instructions.
   */
  @Test
  public void testFuncRunner() throws URISyntaxException {

    URL resource = TestFuncRunner.class.getResource("/test.fc");
    File funcFile = Paths.get(resource.toURI()).toFile();
    String absolutePath = funcFile.getAbsolutePath();

    String resourcePath = System.getProperty("user.dir") + "2.ton-test-artifacts/func.exe";

    FuncRunner funcRunner = FuncRunner.builder().funcExecutablePath(resourcePath).build();

    String result = funcRunner.run(funcFile.getParent(), "-PA", absolutePath);
    log.info("output: {}", result);
  }
}
