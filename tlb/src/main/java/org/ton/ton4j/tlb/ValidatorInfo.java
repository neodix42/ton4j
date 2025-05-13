package org.ton.ton4j.tlb;

import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellBuilder;
import org.ton.ton4j.cell.CellSlice;

import java.io.Serializable;

/**
 *
 *
 * <pre>
 * validator_info$_
 * validator_list_hash_short:uint32
 * catchain_seqno:uint32
 * nx_cc_updated:Bool
 * = ValidatorInfo;
 * </pre>
 */
@Builder
@Data
public class ValidatorInfo implements Serializable {
  long validatorListHashShort;
  long catchainSeqno;
  boolean nXCcUpdated;

  public Cell toCell() {
    return CellBuilder.beginCell()
        .storeUint(validatorListHashShort, 32)
        .storeUint(catchainSeqno, 32)
        .storeBit(nXCcUpdated)
        .endCell();
  }

  public static ValidatorInfo deserialize(CellSlice cs) {
    return ValidatorInfo.builder()
        .validatorListHashShort(cs.loadUint(32).longValue())
        .catchainSeqno(cs.loadUint(32).longValue())
        .nXCcUpdated(cs.loadBit())
        .build();
  }
}
