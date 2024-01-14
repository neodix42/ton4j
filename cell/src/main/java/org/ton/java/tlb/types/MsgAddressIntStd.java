package org.ton.java.tlb.types;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.ton.java.address.Address;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;

import java.math.BigInteger;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

@Builder
@Getter
@Setter
@ToString
/**
 * addr_std$10 anycast:(Maybe Anycast)  workchain_id:int8 address:bits256  = MsgAddressInt;
 *
 */
public class MsgAddressIntStd implements MsgAddressInt {
    int magic;
    Anycast anycast;
    int addrLen;
    int workchainId;
    BigInteger address;

    @Override
    public String toString() {
        return nonNull(address) ? (workchainId + ":" + address.toString(16)) : null;
    }

    public Cell toCell() {
        CellBuilder result = CellBuilder.beginCell();
        result.storeUint(2, 2);
        if (isNull(anycast)) {
            result.storeBit(false);
        } else {
            result.storeBit(true);
            result.writeCell(anycast.toCell());
        }
        result.storeUint(workchainId, 8)
                .storeUint(address, 256)
                .endCell();
        return result;
    }


    public Address toAddress() {
        return Address.of(toString());
    }
}
