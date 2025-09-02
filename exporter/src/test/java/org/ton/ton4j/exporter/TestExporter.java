package org.ton.ton4j.exporter;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@Slf4j
@RunWith(JUnit4.class)
public class TestExporter {

  @Test
  public void testExporterBuilder() {
    Exporter exporter =
        Exporter.builder()
            .tonDatabaseRootPath("/home/neodix/gitProjects/MyLocalTon/myLocalTon/genesis/db")
            .build();
    assertThat(exporter).isNotNull();
    log.info("exporter={}", exporter.getDatabasePath());
  }

  @Test
  public void testExporterStdout() {
    Exporter exporter =
        Exporter.builder()
            .tonDatabaseRootPath("/home/neodix/gitProjects/MyLocalTon/myLocalTon/genesis/db")
            .outputToStdout(true)
            .build();
    assertThat(exporter).isNotNull();
    log.info("exporter={}", exporter.getDatabasePath());
  }

  @Test
  public void testExporterRun() throws IOException {
    Exporter exporter =
        Exporter.builder()
            .tonDatabaseRootPath("/home/neodix/gitProjects/MyLocalTon/myLocalTon/genesis/db")
            .outputToStdout(true)
            .build();
    assertThat(exporter).isNotNull();
    log.info("exporter={}", exporter.getDatabasePath());
    exporter.run();
  }
}
