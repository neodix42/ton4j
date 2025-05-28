package org.ton.ton4j.tlb;

import java.io.Serializable;
import java.math.BigInteger;
import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellBuilder;
import org.ton.ton4j.cell.CellSlice;

/**
 *
 *
 * <pre>
 * capabilities#c4 version:uint32 capabilities:uint64 = GlobalVersion;
 * </pre>
 */
@Builder
@Data
public class GlobalVersion implements Serializable {
  long magic;
  long version;
  BigInteger capabilities;

  private String getMagic() {
    return Long.toHexString(magic);
  }

  public Cell toCell() {
    return CellBuilder.beginCell()
        .storeUint(0xc4, 32)
        .storeUint(version, 32)
        .storeUint(capabilities, 64)
        .endCell();
  }

  public static GlobalVersion deserialize(CellSlice cs) {
    long magic = cs.loadUint(8).longValue();
    assert (magic == 0xc4L)
        : "GlobalVersion: magic not equal to 0xc4, found 0x" + Long.toHexString(magic);

    return GlobalVersion.builder()
        .magic(0xc4L)
        .version(cs.loadUint(32).longValue())
        .capabilities(cs.loadUint(64))
        .build();
  }
}
