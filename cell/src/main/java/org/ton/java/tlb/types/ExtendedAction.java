package org.ton.java.tlb.types;

import lombok.Builder;
import lombok.Data;
import org.ton.java.address.Address;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;

@Builder
@Data
public class ExtendedAction {
    int actionType; // 2 - add extension, 3 - remove extension, 4 - change signature allowed flag
    Address dstAddress;
    boolean isSigAllowed;

    public ExtendedAction(int actionType, Address dstAddress, boolean isSignatureAllowed) {
        this.actionType = actionType;
        this.dstAddress = dstAddress;
        this.isSigAllowed = isSignatureAllowed;
    }

    public Cell toCell() {
        CellBuilder cb = CellBuilder.beginCell();

        // prefix code == magic
        cb.storeUint(0x6578746e, 32);

        if (actionType == 2) {
            cb.storeUint(2, 8).storeAddress(dstAddress);
        }
        if (actionType == 3) {
            cb.storeUint(3, 8).storeAddress(dstAddress);
        }
        if (actionType == 4) {
            cb.storeUint(4, 8).storeBit(isSigAllowed);
        }

        return cb.endCell();
    }
}