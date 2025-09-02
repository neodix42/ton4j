package org.ton.ton4j.indexer;

import static java.util.Objects.isNull;

import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.List;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.ton.ton4j.indexer.reader.ArchiveDbReader;
import org.ton.ton4j.indexer.reader.DbReader;
import org.ton.ton4j.tlb.Block;

@Builder
@Slf4j
public class Indexer {
  /**
   * usually located in /var/ton-work/db on server or myLocalTon/genesis/db in MyLocalTon app.
   * Specify absolute path
   */
  private String tonDatabaseRootPath;

  /** number of parallel threads used to read TON DB, default 32 */
  private Integer parallelThreads;

  /** whether to show blocks' reading progress every second, default false */
  private Boolean showProgress;

  private String outputToFile;
  private Boolean outputToStdout;

  private static DbReader dbReader;

  public static class IndexerBuilder {}

  public static IndexerBuilder builder() {
    return new CustomIndexerBuilder();
  }

  private static class CustomIndexerBuilder extends IndexerBuilder {

    @Override
    public Indexer build() {
      if (isNull(super.tonDatabaseRootPath)) {
        throw new Error("tonDatabaseRootPath is null");
      }

      if (isNull(super.parallelThreads)) {
        super.parallelThreads = 32;
      }

      if (isNull(super.showProgress)) {
        super.showProgress = false;
      }

      if (isNull(super.outputToStdout)) {
        super.outputToStdout = false;
      }
      return super.build();
    }
  }

  public String getDatabasePath() {
    return tonDatabaseRootPath;
  }

  public String run() throws IOException {
    dbReader = new DbReader(tonDatabaseRootPath);

    ArchiveDbReader archiveDbReader = dbReader.getArchiveDbReader();

    // Get all available archive keys
    List<String> archiveKeys = archiveDbReader.getArchiveKeys();
    log.info("Available archive keys: {}", archiveKeys);

    LinkedHashSet<Block> parallelBlocks = archiveDbReader.getAllBlocksParallel(parallelThreads);

    dbReader.close();
    return tonDatabaseRootPath;
  }
}
