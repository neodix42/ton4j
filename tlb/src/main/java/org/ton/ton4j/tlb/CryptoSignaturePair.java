package org.ton.ton4j.tlb;

import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellBuilder;
import org.ton.ton4j.cell.CellSlice;

import java.io.Serializable;
import java.math.BigInteger;

/**
 *
 *
 * <pre>
 * sig_pair$_ node_id_short:bits256 sign:CryptoSignature = CryptoSignaturePair; // 256+x ~ 772 bits
 * </pre>
 */
@Builder
@Data
public class CryptoSignaturePair implements Serializable {
  BigInteger nodeIdShort;
  CryptoSignature sign;

  public Cell toCell() {
    return CellBuilder.beginCell().storeUint(nodeIdShort, 256).storeCell(sign.toCell()).endCell();
  }

  public static CryptoSignaturePair deserialize(CellSlice cs) {
    return CryptoSignaturePair.builder()
        .nodeIdShort(cs.loadUint(256))
        .sign(CryptoSignature.deserialize(cs))
        .build();
  }
}
