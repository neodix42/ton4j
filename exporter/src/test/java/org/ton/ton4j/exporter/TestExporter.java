package org.ton.ton4j.exporter;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
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
    log.info("exporter root db path {}", exporter.getDatabasePath());
  }

  @Test
  public void testExporterArchiveStats() throws IOException {
    Exporter exporter =
        Exporter.builder()
            .tonDatabaseRootPath("/home/neodix/gitProjects/MyLocalTon/myLocalTon/genesis/db")
            .build();
    assertThat(exporter).isNotNull();
    log.info("exporter root db path {}", exporter.getDatabasePath());
    exporter.printADbStats();
  }

  @Test
  public void testExporterToFile() throws IOException {
    Exporter exporter =
        Exporter.builder()
            .tonDatabaseRootPath("/home/neodix/gitProjects/MyLocalTon/myLocalTon/genesis/db")
            .build();
    assertThat(exporter).isNotNull();
    FileUtils.deleteQuietly(new File("local.txt"));
    exporter.exportToFile("local.txt", false, 20);
  }

  @Test
  public void testExporterToStdout() throws IOException {
    Exporter exporter =
        Exporter.builder()
            .tonDatabaseRootPath("/home/neodix/gitProjects/MyLocalTon/myLocalTon/genesis/db")
            .build();
    exporter.exportToStdout(true, 20);
  }
}
