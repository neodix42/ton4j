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
 * validator_addr#73 public_key:SigPubKey weight:uint64 adnl_addr:bits256 = ValidatorDescr;
 * </pre>
 */
@Builder
@Data
public class ValidatorAddr implements ValidatorDescr {
  int magic;
  SigPubKey publicKey;
  BigInteger weight;
  BigInteger adnlAddr;

  private String getMagic() {
    return Long.toHexString(magic);
  }

  private String getAdnlAddr() {
    return adnlAddr.toString(16);
  }

  @Override
  public Cell toCell() {
    return CellBuilder.beginCell()
        .storeUint(0x73, 8)
        .storeCell(publicKey.toCell())
        .storeUint(weight, 64)
        .storeUint(adnlAddr, 256)
        .endCell();
  }

  public static ValidatorAddr deserialize(CellSlice cs) {
    return ValidatorAddr.builder()
        .magic(cs.loadUint(8).intValue())
        .publicKey(SigPubKey.deserialize(cs))
        .weight(cs.loadUint(64))
        .adnlAddr(cs.loadUint(256))
        .build();
  }
}
