package org.ton.java.tlb.types;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.ton.java.address.Address;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.CellSlice;
import org.ton.java.utils.Utils;

import java.math.BigInteger;

import static java.util.Objects.nonNull;

/**
 * <pre>
 * _ split_depth:(Maybe (## 5))
 *   special:(Maybe TickTock)
 *   code:(Maybe ^Cell)
 *   data:(Maybe ^Cell)
 *   library:(Maybe ^Cell) = StateInit;
 *   </pre>
 */
@Builder
@Getter
@Setter
@ToString

public class StateInit {
    BigInteger depth;
    TickTock tickTock;
    Cell code;
    Cell data;
    Cell lib;

    public Cell toCell() {
        if (nonNull(depth)) {
            return CellBuilder.beginCell()
                    .storeBit(true)
                    .storeUint(depth, 5)
                    .storeCellMaybe(nonNull(tickTock) ? tickTock.toCell() : null)
                    .storeRefMaybe(code)
                    .storeRefMaybe(data)
                    .storeRefMaybe(lib)
                    .endCell();

        } else {
            return CellBuilder.beginCell()
                    .storeBit(false)
                    .storeCellMaybe(nonNull(tickTock) ? tickTock.toCell() : null)
                    .storeRefMaybe(code)
                    .storeRefMaybe(data)
                    .storeRefMaybe(lib)
                    .endCell();
        }
    }

    public static StateInit deserialize(CellSlice cs) {
        return StateInit.builder()
                .depth(cs.loadBit() ? cs.loadUint(5) : BigInteger.ZERO)
                .tickTock(cs.loadBit() ? TickTock.deserialize(cs) : null)
                .code(cs.loadMaybeRefX())
                .data(cs.loadMaybeRefX())
                .lib(cs.loadMaybeRefX())
                .build();
    }

    public Address getAddress(long wc) {
        return Address.of(wc + ":" + Utils.bytesToHex(this.toCell().getHash()));
    }

    public Address getAddress() {
        return Address.of("0:" + Utils.bytesToHex(this.toCell().getHash()));
    }

    public Address getAddressNonBounceable(boolean isTestOnly) {
        Address address = Address.of("0:" + Utils.bytesToHex(this.toCell().getHash()));
        return Address.of(address.toString(true, true, false, isTestOnly));
    }
}
