package org.ton.ton4j.tlb;

import java.io.Serializable;
import java.math.BigInteger;
import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellBuilder;
import org.ton.ton4j.cell.CellSlice;

/** validator#53 public_key:SigPubKey weight:uint64 = ValidatorDescr; */
@Builder
@Data
public class Validator implements ValidatorDescr, Serializable {
  long magic;
  SigPubKey publicKey;
  BigInteger weight;

  @Override
  public Cell toCell() {
    return CellBuilder.beginCell()
        .storeUint(0x53, 8)
        .storeCell(publicKey.toCell())
        .storeUint(weight, 64)
        .endCell();
  }

  public static ValidatorAddr deserialize(CellSlice cs) {
    return ValidatorAddr.builder()
        .magic(cs.loadUint(8).intValue())
        .publicKey(SigPubKey.deserialize(cs))
        .weight(cs.loadUint(64))
        .build();
  }

  @Override
  public String getPublicKeyHex() {
    return publicKey.getPubkey().toString(16);
  }

  @Override
  public String getAdnlAddressHex() {
    return "";
  }
}
