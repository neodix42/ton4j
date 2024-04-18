package org.ton.java.smartcontract.types;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.CellSlice;
import org.ton.java.tlb.types.OutList;

import java.math.BigInteger;

@Builder
@Getter
@Setter
@ToString
/**
 * internal_transfer#ae42e5a4 {n:#} query_id:uint64 actions:^(OutList n) = InternalMsgBody n;
 */
public class HighloadV3InternalMessageBody {

    long magic;
    BigInteger queryId;
    OutList actions;

    public Cell toCell(int n) {
        return CellBuilder.beginCell()
                .storeUint(0xae42e5a4, 32)
                .storeUint(queryId, 64)
                .storeRef(actions.toCell(n))
                .endCell();
    }

    static HighloadV3InternalMessageBody deserialize(CellSlice cs, int n) {
        return HighloadV3InternalMessageBody.builder()
                .magic(cs.loadUint(32).longValue())
                .queryId(cs.loadUint(64))
                .actions(OutList.deserialize(CellSlice.beginParse(cs.loadRef()), n))
                .build();
    }
}
