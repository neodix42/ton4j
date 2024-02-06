package org.ton.java.tlb.types;

import org.ton.java.cell.Cell;
import org.ton.java.cell.CellSlice;

/**
 * interm_addr_regular$0 use_dest_bits:(#<= 96) = IntermediateAddress;
 * interm_addr_simple$10 workchain_id:int8 addr_pfx:uint64 = IntermediateAddress;
 * interm_addr_ext$11    workchain_id:int32 addr_pfx:uint64 = IntermediateAddress;
 */

public interface IntermediateAddress {

    public Cell toCell();

    public static IntermediateAddress deserialize(CellSlice cs) {
        if (!cs.loadBit()) {
            return IntermediateAddressRegular.builder()
                    .use_dest_bits(cs.loadUint(7).intValue())
                    .build();
        }
        if (!cs.loadBit()) {
            return IntermediateAddressSimple.builder()
                    .workchainId(cs.loadUint(8).intValue())
                    .addrPfx(cs.loadUint(64))
                    .build();
        }
        return IntermediateAddressExt.builder()
                .workchainId(cs.loadUint(32).intValue())
                .addrPfx(cs.loadUint(64))
                .build();
    }
}
