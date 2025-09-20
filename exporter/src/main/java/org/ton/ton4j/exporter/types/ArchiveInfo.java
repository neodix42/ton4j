package org.ton.ton4j.exporter.types;

import lombok.Data;

@Data
public class ArchiveInfo {
  private final int id;
  private final String indexPath;
  private final String packagePath;
  private final long packageSize;

  public ArchiveInfo(int id, String indexPath, String packagePath, long packageSize) {
    this.id = id;
    this.indexPath = indexPath;
    this.packagePath = packagePath;
    this.packageSize = packageSize;
  }
}
