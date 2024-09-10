package org.ton.java.tlb.types;

import lombok.Builder;
import lombok.Data;
import org.ton.java.address.Address;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.CellSlice;

@Builder
@Data
public class ExtendedAction {
    ExtendedActionType actionType;
    Address address;
    Boolean isSignatureAllowed;

    public Cell toCell() {
        CellBuilder cb = CellBuilder.beginCell();

        if (actionType == ExtendedActionType.ADD_EXTENSION) {
            cb.storeUint(2, 8).storeAddress(address);
        } else if (actionType == ExtendedActionType.REMOVE_EXTENSION) {
            cb.storeUint(3, 8).storeAddress(address);
        } else {
            cb.storeUint(4, 8).storeBit(isSignatureAllowed);
        }

        return cb.endCell();
    }

    public static ExtendedAction deserialize(CellSlice cs) {
        ExtendedAction extendedAction = ExtendedAction.builder().build();
        int actionType = cs.loadUint(8).intValue();
        if ((actionType == 2) || (actionType == 3)) {
            extendedAction.setActionType(ExtendedActionType.getExtensionType(actionType));
            extendedAction.setAddress(cs.loadAddress());
        } else if (actionType == 4) {
            extendedAction.setIsSignatureAllowed(cs.loadBit());
        } else {
            throw new Error("Wrong action type");
        }
        return extendedAction;
    }
}