package org.ton.ton4j.cell;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class CellSerializationInfo {
  int refsCount;
  LevelMask levelMask;
  boolean special;
  boolean withHashes;
  int dataOffset;
  int dataLength;
  boolean dataWithBits;
  int refsOffset;
  int endOffset;

  public static CellSerializationInfo create(int d1, int d2) {

    int refsNum = d1 & 7;
    boolean special = (d1 & 8) != 0;
    boolean withHashes = (d1 & 16) != 0;
    LevelMask levelMask = new LevelMask(d1 >> 5);

    if (refsNum > 4) {
      throw new Error("too many refs in cell");
    }
    int hashesOffset = 4 + 2;
    int n = levelMask.getHashesCount();
    int depthOffset = hashesOffset + (withHashes ? n * 32 : 0);
    int dataOffset = depthOffset + (withHashes ? n * 2 : 0);

    int dataLength = (d2 >> 1) + (d2 & 1);
    boolean dataWithBits = (d2 & 1) != 0;
    int refsOffset = dataOffset + dataLength;
    int endOffset = refsOffset + (refsNum * 0); // +ref_byte_size

    return CellSerializationInfo.builder()
        .refsCount(refsNum)
        .levelMask(levelMask)
        .special(special)
        .withHashes(withHashes)
        .dataOffset(dataOffset)
        .dataLength(dataLength)
        .dataWithBits(dataWithBits)
        .refsOffset(refsOffset)
        .endOffset(endOffset)
        .build();
  }
}
