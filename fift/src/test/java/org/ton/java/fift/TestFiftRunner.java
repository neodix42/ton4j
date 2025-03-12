package org.ton.java.fift;

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
public class TestFiftRunner {

  @Test
  public void testFiftRunner() throws URISyntaxException {

    URL resource = TestFiftRunner.class.getResource("/test.fift");
    File fiftFile = Paths.get(resource.toURI()).toFile();
    String absolutePath = fiftFile.getAbsolutePath();

    FiftRunner fiftRunner =
        FiftRunner.builder().fiftExecutablePath(Utils.getFiftGithubUrl()).build();

    String result = fiftRunner.run(fiftFile.getParent(), "-s", absolutePath);
    log.info("output: {}", result);
  }

  @Test
  public void testFiftRunnerDownload() throws URISyntaxException {

    URL resource = TestFiftRunner.class.getResource("/test.fift");
    File fiftFile = Paths.get(resource.toURI()).toFile();
    String absolutePath = fiftFile.getAbsolutePath();

    FiftRunner fiftRunner =
        FiftRunner.builder().fiftExecutablePath(Utils.getFiftGithubUrl()).build();

    String result = fiftRunner.run(fiftFile.getParent(), "-s", absolutePath);
    log.info("output: {}", result);
  }
}
