package org.ton.ton4j.tolk;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.ton.ton4j.utils.Utils;

@Slf4j
@RunWith(JUnit4.class)
public class TestTolkRunner {

  @Test
  public void testTolkRunner() throws URISyntaxException {

    URL resource = TestTolkRunner.class.getResource("/test.tolk");
    File tolkFile = Paths.get(resource.toURI()).toFile();
    String absolutePath = tolkFile.getAbsolutePath();

    TolkRunner tolkRunner =
        TolkRunner.builder().tolkExecutablePath(Utils.getArtifactGithubUrl("tolk","tolk-1.0.0")).build();

    String result = tolkRunner.run(tolkFile.getParent(), absolutePath);
    log.info("output: {}", result);
  }

  @Test
  public void testTolkRunner1v() throws URISyntaxException {

    URL resource = TestTolkRunner.class.getResource("/test-1.0.tolk");
    File tolkFile = Paths.get(resource.toURI()).toFile();
    String absolutePath = tolkFile.getAbsolutePath();

    TolkRunner tolkRunner =
            TolkRunner.builder()
                    .tolkExecutablePath(Utils.getArtifactGithubUrl("tolk","tolk-1.0.0")).build();

    String result = tolkRunner.run(tolkFile.getParent(), absolutePath);
    log.info("output: {}", result);
  }

  @Test
  public void testTolkRunnerCompiler() throws URISyntaxException {

    URL resource = TestTolkRunner.class.getResource("/test-1.0.tolk");
    File tolkFile = Paths.get(resource.toURI()).toFile();
    String absolutePath = tolkFile.getAbsolutePath();

    TolkRunner tolkRunner =
            TolkRunner.builder().tolkExecutablePath(Utils.getArtifactGithubUrl("tolk","tolk-1.0.0")).build();

    String result = tolkRunner.run(tolkFile.getParent(), absolutePath);
    log.info("output: {}", result);
  }
}
