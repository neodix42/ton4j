package org.ton.ton4j.indexer;

import static org.assertj.core.api.Assertions.assertThat;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@Slf4j
@RunWith(JUnit4.class)
public class TestIndexer {

  @Test
  public void testIndexerBuilder() {
    Indexer indexer =
        Indexer.builder()
            .tonDatabaseRootPath("/home/neodix/gitProjects/MyLocalTon/myLocalTon/genesis/db")
            .build();
    assertThat(indexer).isNotNull();
    log.info("indexer={}", indexer.getDatabasePath());
  }

  @Test
  public void testIndexerStdout() {
    Indexer indexer =
        Indexer.builder()
            .tonDatabaseRootPath("/home/neodix/gitProjects/MyLocalTon/myLocalTon/genesis/db")
            .outputToStdout(true)
            .build();
    assertThat(indexer).isNotNull();
    log.info("indexer={}", indexer.getDatabasePath());
  }

  @Test
  public void testIndexerRun() {
    Indexer indexer =
        Indexer.builder()
            .tonDatabaseRootPath("/home/neodix/gitProjects/MyLocalTon/myLocalTon/genesis/db")
            .outputToStdout(true)
            .build();
    assertThat(indexer).isNotNull();
    log.info("indexer={}", indexer.getDatabasePath());
  }
}
