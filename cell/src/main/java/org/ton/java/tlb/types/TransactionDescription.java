package org.ton.java.tlb.types;

import org.ton.java.cell.Cell;
import org.ton.java.cell.CellSlice;

/**
 * <pre>
 * trans_ord$0000 credit_first:Bool
 *   storage_ph:(Maybe TrStoragePhase)
 *   credit_ph:(Maybe TrCreditPhase)
 *   compute_ph:TrComputePhase
 *   action:(Maybe ^TrActionPhase)
 *   aborted:Bool
 *   bounce:(Maybe TrBouncePhase)
 *   destroyed:Bool
 *   = TransactionDescr;
 *
 * trans_storage$0001
 *   storage_ph:TrStoragePhase
 *   = TransactionDescr;
 *
 * trans_tick_tock$001 is_tock:Bool
 *   storage_ph:TrStoragePhase
 *   compute_ph:TrComputePhase
 *   action:(Maybe ^TrActionPhase)
 *   aborted:Bool
 *   destroyed:Bool = TransactionDescr;
 *
 * split_merge_info$_
 *   cur_shard_pfx_len:(## 6)
 *   acc_split_depth:(## 6)
 *   this_addr:bits256
 *   sibling_addr:bits256
 *   = SplitMergeInfo;
 *
 * trans_split_prepare$0100
 *   split_info:SplitMergeInfo
 *   storage_ph:(Maybe TrStoragePhase)
 *   compute_ph:TrComputePhase
 *   action:(Maybe ^TrActionPhase)
 *   aborted:Bool
 *   destroyed:Bool
 *   = TransactionDescr;
 *
 * trans_split_install$0101
 *   split_info:SplitMergeInfo
 *   prepare_transaction:^Transaction
 *   installed:Bool = TransactionDescr;
 *
 * trans_merge_prepare$0110
 * split_info:SplitMergeInfo
 *   storage_ph:TrStoragePhase
 *   aborted:Bool
 *   = TransactionDescr;
 *
 * trans_merge_install$0111 split_info:SplitMergeInfo
 *   prepare_transaction:^Transaction
 *   storage_ph:(Maybe TrStoragePhase)
 *   credit_ph:(Maybe TrCreditPhase)
 *   compute_ph:TrComputePhase
 *   action:(Maybe ^TrActionPhase)
 *   aborted:Bool
 *   destroyed:Bool
 *   = TransactionDescr;
 *   </pre>
 */
public interface TransactionDescription {

    Cell toCell();

    static TransactionDescription deserialize(CellSlice cs) {
        int pfx = cs.preloadUint(3).intValue();
        switch (pfx) {
            case 0b000: {
                boolean isStorage = cs.preloadBit();
                if (isStorage) {
                    return TransactionDescriptionStorage.deserialize(cs);
                }
                return TransactionDescriptionOrdinary.deserialize(cs);
            }
            case 0b001: {
                return TransactionDescriptionTickTock.deserialize(cs);
            }
            case 0b010: {
                boolean isInstall = cs.preloadBit();
                if (isInstall) {
                    return TransactionDescriptionSplitInstall.deserialize(cs);
                }
                return TransactionDescriptionSplitPrepare.deserialize(cs);
            }
            case 0b011: {
                boolean isInstall = cs.preloadBit();
                if (isInstall) {
                    return TransactionDescriptionMergeInstall.deserialize(cs);
                }
                return TransactionDescriptionMergePrepare.deserialize(cs);
            }
        }
        throw new Error(
                "unknown transaction description type (must be in range [0..3], found 0x"
                        + Integer.toBinaryString(pfx));
    }
}
