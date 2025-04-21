package org.ton.java.tlb;

import lombok.Builder;
import lombok.Data;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.CellSlice;

import java.io.Serializable;

/**
 *
 *
 * <pre>
 * capabilities#c4
 * version:uint32
 * capabilities:uint64 = GlobalVersion;
 * _ GlobalVersion = ConfigParam 8; // all zero if absent
 * </pre>
 */
@Builder
@Data
public class ConfigParams8 implements Serializable {
  GlobalVersion globalVersion;

  public Cell toCell() {
    return CellBuilder.beginCell().storeCell(globalVersion.toCell()).endCell();
  }

  public static ConfigParams8 deserialize(CellSlice cs) {
    return ConfigParams8.builder().globalVersion(GlobalVersion.deserialize(cs)).build();
  }
}
