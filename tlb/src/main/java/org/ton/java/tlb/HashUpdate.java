package org.ton.java.tlb;

import lombok.Builder;
import lombok.Data;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.CellSlice;

import java.math.BigInteger;

/**
 *
 *
 * <pre>
 * update_hashes#72 {X:Type} old_hash:bits256 new_hash:bits256 = HASH_UPDATE X;
 * </pre>
 */
@Builder
@Data
public class HashUpdate {
  int magic;
  BigInteger oldHash;
  BigInteger newHash;

  private String getMagic() {
    return Long.toHexString(magic);
  }

  private String getOldHash() {
    return oldHash.toString(16);
  }

  private String getNewHash() {
    return newHash.toString(16);
  }

  public Cell toCell() {
    return CellBuilder.beginCell()
        .storeUint(0x72, 32)
        .storeUint(oldHash, 256)
        .storeUint(newHash, 256)
        .endCell();
  }

  public static HashUpdate deserialize(CellSlice cs) {
    long magic = cs.loadUint(8).intValue();
    assert (magic == 0x72)
        : "HashUpdate: magic not equal to 0x72, found 0x" + Long.toHexString(magic);

    return HashUpdate.builder()
        .magic(0x72)
        .oldHash(cs.loadUint(256))
        .newHash(cs.loadUint(256))
        .build();
  }
}
