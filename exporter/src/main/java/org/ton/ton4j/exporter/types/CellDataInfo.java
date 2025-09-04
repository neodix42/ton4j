package org.ton.ton4j.exporter.types;

import lombok.Getter;
import org.ton.ton4j.tl.liteserver.responses.BlockIdExt;

/** Information about a cell data entry and its relationship to metadata. */
public class CellDataInfo {
  // Getters
  @Getter private final String metadataKeyHash;
  @Getter private final String rootHash;
  @Getter private final BlockIdExt blockId;
  private final boolean hasCellData;
  @Getter private final int cellDataSize;

  public CellDataInfo(
      String metadataKeyHash,
      String rootHash,
      BlockIdExt blockId,
      boolean hasCellData,
      int cellDataSize) {
    this.metadataKeyHash = metadataKeyHash;
    this.rootHash = rootHash;
    this.blockId = blockId;
    this.hasCellData = hasCellData;
    this.cellDataSize = cellDataSize;
  }

  public boolean hasCellData() {
    return hasCellData;
  }

  @Override
  public String toString() {
    return String.format(
        "CellDataInfo{keyHash=%s, rootHash=%s, blockId=%s, hasData=%s, size=%d}",
        metadataKeyHash, rootHash, blockId, hasCellData, cellDataSize);
  }
}
