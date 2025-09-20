package org.ton.ton4j.tlb;

import java.io.Serializable;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.StopWatch;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellBuilder;
import org.ton.ton4j.cell.CellSlice;

/**
 *
 *
 * <pre>
 * value_flow#b8e48dfb
 *  ^[
 *   from_prev_blk:CurrencyCollection
 *   to_next_blk:CurrencyCollection
 *   imported:CurrencyCollection
 *   exported:CurrencyCollection ]
 *
 *   fees_collected:CurrencyCollection
 *
 *   ^[
 *   fees_imported:CurrencyCollection
 *   recovered:CurrencyCollection
 *   created:CurrencyCollection
 *   minted:CurrencyCollection
 *   ] = ValueFlow;
 *   </pre>
 */
@Builder
@Data
@Slf4j
public class ValueFlow implements Serializable {
  long magic;
  CurrencyCollection fromPrevBlk;
  CurrencyCollection toNextBlk;
  CurrencyCollection imported;
  CurrencyCollection exported;
  CurrencyCollection feesCollected;
  CurrencyCollection burned;
  CurrencyCollection feesImported;
  CurrencyCollection recovered;
  CurrencyCollection created;
  CurrencyCollection minted;

  private String getMagic() {
    return Long.toHexString(magic);
  }

  public Cell toCell() {
    Cell cell1 =
        CellBuilder.beginCell()
            .storeCell(fromPrevBlk.toCell())
            .storeCell(toNextBlk.toCell())
            .storeCell(imported.toCell())
            .storeCell(exported.toCell())
            .endCell();

    Cell cell2 =
        CellBuilder.beginCell()
            .storeCell(feesImported.toCell())
            .storeCell(recovered.toCell())
            .storeCell(created.toCell())
            .storeCell(minted.toCell())
            .endCell();

    return CellBuilder.beginCell()
        .storeUint(0xb8e48dfbL, 32)
        .storeRef(cell1)
        .storeCell(feesCollected.toCell())
        .storeRef(cell2)
        .endCell();
  }

  public static ValueFlow deserialize(CellSlice cs) {
    StopWatch stopWatch = new StopWatch();
    stopWatch.start();
    long magic = cs.loadUint(32).longValue();
    if (magic == 0xb8e48dfbL) {

      CellSlice c1 = CellSlice.beginParse(cs.loadRef());
      CellSlice c2 = CellSlice.beginParse(cs.loadRef());

      CurrencyCollection fromPrevBlk = CurrencyCollection.deserialize(c1);
      CurrencyCollection toNextBlk = CurrencyCollection.deserialize(c1);
      CurrencyCollection imported = CurrencyCollection.deserialize(c1);
      CurrencyCollection exported = CurrencyCollection.deserialize(c1);

      CurrencyCollection feesCollected = CurrencyCollection.deserialize(cs);

      CurrencyCollection feesImported = CurrencyCollection.deserialize(c2);
      CurrencyCollection recovered = CurrencyCollection.deserialize(c2);
      CurrencyCollection created = CurrencyCollection.deserialize(c2);
      CurrencyCollection minted = CurrencyCollection.deserialize(c2);
      log.info("{} deserialized in {}ms", ValueFlow.class.getSimpleName(), stopWatch.getTime());
      return ValueFlow.builder()
          .magic(0xb8e48dfbL)
          .fromPrevBlk(fromPrevBlk)
          .toNextBlk(toNextBlk)
          .imported(imported)
          .exported(exported)
          .feesCollected(feesCollected)
          .feesImported(feesImported)
          .recovered(recovered)
          .created(created)
          .minted(minted)
          .build();
    }
    if (magic == 0x3ebf98b7L) {
      CellSlice c1 = CellSlice.beginParse(cs.loadRef());
      CellSlice c2 = CellSlice.beginParse(cs.loadRef());

      CurrencyCollection fromPrevBlk = CurrencyCollection.deserialize(c1);
      CurrencyCollection toNextBlk = CurrencyCollection.deserialize(c1);
      CurrencyCollection imported = CurrencyCollection.deserialize(c1);
      CurrencyCollection exported = CurrencyCollection.deserialize(c1);

      CurrencyCollection feesCollected = CurrencyCollection.deserialize(cs);
      CurrencyCollection burned = CurrencyCollection.deserialize(cs);

      CurrencyCollection feesImported = CurrencyCollection.deserialize(c2);
      CurrencyCollection recovered = CurrencyCollection.deserialize(c2);
      CurrencyCollection created = CurrencyCollection.deserialize(c2);
      CurrencyCollection minted = CurrencyCollection.deserialize(c2);

      log.info("{} deserialized in {}ms", ValueFlow.class.getSimpleName(), stopWatch.getTime());

      return ValueFlow.builder()
          .magic(0xb8e48dfbL)
          .fromPrevBlk(fromPrevBlk)
          .toNextBlk(toNextBlk)
          .imported(imported)
          .exported(exported)
          .feesCollected(feesCollected)
          .burned(burned)
          .feesImported(feesImported)
          .recovered(recovered)
          .created(created)
          .minted(minted)
          .build();
    } else {
      throw new Error(
          "ValueFlow: magic not equal to 0xb8e48dfb, found 0x" + Long.toHexString(magic));
    }
  }
}
