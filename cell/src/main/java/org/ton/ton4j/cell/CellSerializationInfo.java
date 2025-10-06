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

    int flags = d1;
    int refsNum = flags & 0b111;
    boolean special = (flags & 0b1000) != 0;
    boolean withHashes = (flags & 0b10000) != 0;
    LevelMask levelMask = new LevelMask(flags >> 5);

    if (refsNum > 4) {
      throw new Error("too many refs in cell");
    }
    int dataOffset =
        4 + 2; // originally 2, but we add 4 since parsing from cell db and read previously flags
    // (int)
    int dataLength = (d2 >> 1) + (d2 & 1);
    boolean dataWithBits = (d2 & 1) != 0;
    int refsOffset = dataOffset + dataLength;

    return CellSerializationInfo.builder()
        .refsCount(refsNum)
        .levelMask(levelMask)
        .special(special)
        .withHashes(withHashes)
        .dataOffset(dataOffset)
        .dataLength(dataLength)
        .dataWithBits(dataWithBits)
        .refsOffset(refsOffset)
        .build();
  }
}
