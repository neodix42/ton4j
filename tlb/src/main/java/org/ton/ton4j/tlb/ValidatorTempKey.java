package org.ton.ton4j.tlb;

import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellBuilder;
import org.ton.ton4j.cell.CellSlice;

import java.io.Serializable;
import java.math.BigInteger;

@Builder
@Data
public class ValidatorTempKey implements Serializable {
  int magic;
  BigInteger adnlAddr;
  SigPubKey tempPublicKey;
  long seqno;
  long validUntil;

  public Cell toCell() {
    return CellBuilder.beginCell()
        .storeUint(0x3, 4)
        .storeUint(adnlAddr, 256)
        .storeCell(tempPublicKey.toCell())
        .storeUint(seqno, 32) // 32?
        .storeUint(validUntil, 32)
        .endCell();
  }

  public static ValidatorTempKey deserialize(CellSlice cs) {
    return ValidatorTempKey.builder()
        .magic(cs.loadUint(4).intValue())
        .adnlAddr(cs.loadUint(256))
        .tempPublicKey(SigPubKey.deserialize(cs))
        .seqno(cs.loadUint(32).longValue())
        .validUntil(cs.loadUint(32).longValue())
        .build();
  }
}
