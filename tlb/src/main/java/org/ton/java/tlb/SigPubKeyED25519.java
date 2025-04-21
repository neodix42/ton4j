package org.ton.java.tlb;

import lombok.Builder;
import lombok.Data;
import org.ton.java.cell.CellSlice;

import java.io.Serializable;

/**
 *
 *
 * <pre>
 * ed25519_pubkey#8e81278a pubkey:bits256 = SigPubKey;  // 288 bits
 * </pre>
 */
@Builder
@Data
public class SigPubKeyED25519 implements Serializable {
  long magic;
  byte[] key;

  private String getMagic() {
    return Long.toHexString(magic);
  }

  public static SigPubKeyED25519 deserialize(CellSlice cs) {
    return null;
  }
}
