package org.ton.ton4j.tlb;

import static java.util.Objects.nonNull;

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
 * validator_addr#73 public_key:SigPubKey weight:uint64 adnl_addr:bits256 = ValidatorDescr;
 * </pre>
 */
@Builder
@Data
public class ValidatorAddr implements ValidatorDescr, Serializable {
  int magic;
  SigPubKey publicKey;
  BigInteger weight;
  BigInteger adnlAddr;

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

  @Override
  public String getPublicKeyHex() {
    return publicKey.getPubkey().toString(16);
  }

  @Override
  public String getAdnlAddressHex() {
    return nonNull(adnlAddr) ? adnlAddr.toString(16) : "";
  }
}
