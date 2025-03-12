package org.ton.java.func;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.ton.java.utils.Utils;

@Slf4j
@RunWith(JUnit4.class)
public class TestFuncRunner {

  @Test
  public void testFuncRunner() throws URISyntaxException {

    URL resource = TestFuncRunner.class.getResource("/test.fc");
    File funcFile = Paths.get(resource.toURI()).toFile();
    String absolutePath = funcFile.getAbsolutePath();

    FuncRunner funcRunner =
        FuncRunner.builder().funcExecutablePath(Utils.getFuncGithubUrl()).build();

    String result = funcRunner.run(funcFile.getParent(), "-PA", absolutePath);
    log.info("output: {}", result);
  }

  @Test
  public void testFuncRunnerDownload() throws URISyntaxException {

    URL resource = TestFuncRunner.class.getResource("/test.fc");
    File funcFile = Paths.get(resource.toURI()).toFile();
    String absolutePath = funcFile.getAbsolutePath();

    FuncRunner funcRunner =
        FuncRunner.builder().funcExecutablePath(Utils.getFuncGithubUrl()).build();

    String result = funcRunner.run(funcFile.getParent(), "-PA", absolutePath);
    log.info("output: {}", result);
  }
}
