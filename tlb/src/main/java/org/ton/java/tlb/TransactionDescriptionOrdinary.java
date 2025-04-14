package org.ton.java.tlb;

import static java.util.Objects.nonNull;

import java.io.Serializable;
import lombok.Builder;
import lombok.Data;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.CellSlice;

/**
 *
 *
 * <pre>
 * trans_ord$0000
 *   credit_first:Bool
 *   storage_ph:(Maybe TrStoragePhase)
 *   credit_ph:(Maybe TrCreditPhase)
 *   compute_ph:TrComputePhase
 *   action:(Maybe ^TrActionPhase)
 *   aborted:Bool
 *   bounce:(Maybe TrBouncePhase)
 *   destroyed:Bool
 *   = TransactionDescr;
 *   </pre>
 */
@Builder
@Data
public class TransactionDescriptionOrdinary implements TransactionDescription, Serializable {
  int magic;
  boolean creditFirst;
  StoragePhase storagePhase;
  CreditPhase creditPhase;
  ComputePhase computePhase;
  ActionPhase actionPhase;
  boolean aborted;
  BouncePhase bouncePhase;
  boolean destroyed;

  private String getMagic() {
    return Long.toBinaryString(magic);
  }

  public Cell toCell() {
    return CellBuilder.beginCell()
        .storeUint(0b0000, 4)
        .storeBit(creditFirst)
        .storeCellMaybe(nonNull(storagePhase) ? storagePhase.toCell() : null)
        .storeCellMaybe(nonNull(creditPhase) ? creditPhase.toCell() : null)
        .storeCell(computePhase.toCell())
        .storeRefMaybe(nonNull(actionPhase) ? actionPhase.toCell() : null)
        .storeBit(aborted)
        .storeCellMaybe(nonNull(bouncePhase) ? bouncePhase.toCell() : null)
        .storeBit(destroyed)
        .endCell();
  }

  public static TransactionDescriptionOrdinary deserialize(CellSlice cs) {
    long magic = cs.loadUint(4).intValue();
    assert (magic == 0b0000)
        : "TransactionDescriptionOrdinary: magic not equal to 0b0000, found 0x"
            + Long.toHexString(magic);

    return TransactionDescriptionOrdinary.builder()
        .magic(0b0000)
        .creditFirst(cs.loadBit())
        .storagePhase(cs.loadBit() ? StoragePhase.deserialize(cs) : null)
        .creditPhase(cs.loadBit() ? CreditPhase.deserialize(cs) : null)
        .computePhase(ComputePhase.deserialize(cs))
        .actionPhase(
            cs.loadBit() ? ActionPhase.deserialize(CellSlice.beginParse(cs.loadRef())) : null)
        .aborted(cs.loadBit())
        .bouncePhase(cs.loadBit() ? BouncePhase.deserialize(cs) : null)
        .destroyed(cs.loadBit())
        .build();
  }

  @Override
  public String getType() {
    return "ordinary";
  }
}
