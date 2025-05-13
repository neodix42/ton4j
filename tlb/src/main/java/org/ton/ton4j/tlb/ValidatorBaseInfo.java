package org.ton.ton4j.tlb;

import java.io.Serializable;
import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellBuilder;
import org.ton.ton4j.cell.CellSlice;

/**
 *
 *
 * <pre>
 * validator_base_info$_
 * validator_list_hash_short:uint32
 * catchain_seqno:uint32
 * = ValidatorBaseInfo;
 * </pre>
 */
@Builder
@Data
public class ValidatorBaseInfo implements Serializable {
  int magic;
  long validatorListHashShort;
  long catchainSeqno;

  public Cell toCell() {
    return CellBuilder.beginCell()
        .storeUint(validatorListHashShort, 32)
        .storeUint(catchainSeqno, 32)
        .endCell();
  }

  public static ValidatorBaseInfo deserialize(CellSlice cs) {
    return ValidatorBaseInfo.builder()
        .validatorListHashShort(cs.loadUint(32).longValue())
        .catchainSeqno(cs.loadUint(32).longValue())
        .build();
  }
}
