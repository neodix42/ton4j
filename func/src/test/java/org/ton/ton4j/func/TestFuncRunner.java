package org.ton.ton4j.func;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.Locale;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.ton.ton4j.utils.Utils;

@Slf4j
@RunWith(JUnit4.class)
public class TestFuncRunner {

  @Test
  public void testFuncRunner() throws URISyntaxException {
    String operSys = System.getProperty("os.name").toLowerCase(Locale.ENGLISH);
    String operArch = System.getProperty("os.arch").toLowerCase(Locale.ENGLISH);

    log.info("operSys {}, operArch {}", operSys, operArch);

    log.info("OS {}", Utils.getOS());

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
