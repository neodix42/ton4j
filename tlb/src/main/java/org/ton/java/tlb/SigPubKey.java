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
 * ed25519_pubkey#8e81278a pubkey:bits256 = SigPubKey;  // 288 bits
 * </pre>
 */
@Builder
@Data
public class SigPubKey implements Serializable {
  long magic;
  BigInteger pubkey;

  public Cell toCell() {
    return CellBuilder.beginCell().storeUint(0x8e81278a, 32).storeUint(pubkey, 256).endCell();
  }

  public static SigPubKey deserialize(CellSlice cs) {
    return SigPubKey.builder().magic(cs.loadUint(32).longValue()).pubkey(cs.loadUint(256)).build();
  }
}
