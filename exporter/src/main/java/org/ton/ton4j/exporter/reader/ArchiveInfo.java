package org.ton.ton4j.exporter.reader;

import lombok.Data;

@Data
public class ArchiveInfo {
  private final int id;
  private final String indexPath;
  private final String packagePath;

  public ArchiveInfo(int id, String indexPath, String packagePath) {
    this.id = id;
    this.indexPath = indexPath;
    this.packagePath = packagePath;
  }
}
