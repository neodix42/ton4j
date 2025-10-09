package org.ton.ton4j.exporter.lazy;

import java.io.Serializable;
import java.math.BigInteger;
import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellBuilder;

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
public class StorageExtraInformationLazy implements StorageExtraInfoLazy, Serializable {
  int magic;
  BigInteger dictHash;

  public Cell toCell() {
    return CellBuilder.beginCell().storeUint(1, 3).storeUint(dictHash, 256).endCell();
  }

  public static StorageExtraInformationLazy deserialize(CellSliceLazy cs) {
    return StorageExtraInformationLazy.builder()
        .magic(cs.loadUint(3).intValue())
        .dictHash(cs.loadUint(256))
        .build();
  }
}
