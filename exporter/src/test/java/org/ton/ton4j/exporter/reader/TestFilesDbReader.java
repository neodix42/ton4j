package org.ton.ton4j.exporter.reader;

import java.io.IOException;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

@Slf4j
public class TestFilesDbReader {

  @Test
  public void testReadAllEntriesMethod() throws IOException {
    PackageReader packageReader =
        new PackageReader(
            "/home/neodix/gitProjects/MyLocalTon/myLocalTon/genesis/db/files/packages/temp.archive.1756836000.pack");
    Map<String, byte[]> result = packageReader.readAllEntries();
    log.info("result {}", result.size());
    packageReader.close();
  }

  @Test
  public void testFileDbReader() throws IOException {
    FilesDbReader filesDbReader =
        new FilesDbReader("/home/neodix/gitProjects/MyLocalTon/myLocalTon/genesis/db");
    for (String file : filesDbReader.listPackageFiles()) {
      log.info("{}", file);
    }

    filesDbReader.close();
  }

  @Test
  public void testFileDbReaderAll() throws IOException {
    FilesDbReader filesDbReader =
        new FilesDbReader("/home/neodix/gitProjects/MyLocalTon/myLocalTon/genesis/db");
    for (Map.Entry<String, BlockLocation> kv : filesDbReader.getAllBlockLocations().entrySet()) {
      log.info("{} {}", kv.getKey(), kv.getValue());
    }

    filesDbReader.close();
  }
}
