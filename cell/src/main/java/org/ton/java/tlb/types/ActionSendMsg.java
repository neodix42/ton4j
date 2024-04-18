package org.ton.java.tlb.types;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.CellSlice;

@Builder
@Getter
@Setter
@ToString
/**
 action_send_msg#0ec3c86d mode:(## 8) out_msg:^(MessageRelaxed Any) = OutAction;
 */
public class ActionSendMsg implements OutAction {
    long magic;
    int mode;
    MessageRelaxed outMsgRef;

    @Override
    public Cell toCell() {
        return CellBuilder.beginCell()
                .storeUint(0x0ec3c86d, 32)
                .storeUint(mode, 8)
                .storeRef(outMsgRef.toCell())
                .endCell();
    }

    static ActionSendMsg deserialize(CellSlice cs) {
        return ActionSendMsg.builder()
                .magic(cs.loadUint(32).intValue())
                .mode(cs.loadUint(8).intValue())
                .outMsgRef(MessageRelaxed.deserialize(CellSlice.beginParse(cs.loadRef())))
                .build();
    }
}