package org.ton.java.tlb;

import java.io.Serializable;
import java.math.BigInteger;
import lombok.Builder;
import lombok.Data;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.CellSlice;

/**
 *
 *
 * <pre>
 * storage_extra_info$001 dict_hash:uint256 = StorageExtraInfo;
 *
 *   </pre>
 */
@Builder
@Data
public class StorageExtraInformation implements StorageExtraInfo, Serializable {
  int magic;
  BigInteger dictHash;

  public Cell toCell() {
    return CellBuilder.beginCell().storeUint(1, 3).storeUint(dictHash, 256).endCell();
  }

  public static StorageExtraInformation deserialize(CellSlice cs) {
    return StorageExtraInformation.builder()
        .magic(cs.loadUint(3).intValue())
        .dictHash(cs.loadUint(256))
        .build();
  }
}
