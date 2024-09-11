package org.ton.java.tlb.types;

import lombok.Builder;
import lombok.Data;
import org.ton.java.cell.CellSlice;
import org.ton.java.cell.TonHashMapE;

import static java.util.Objects.isNull;

@Builder
@Data
public class MessagesList {
    TonHashMapE list;

    public static MessagesList deserialize(CellSlice cs) {
        if (isNull(cs)) {
            return MessagesList.builder().build();
        }
        return MessagesList.builder()
                .list(cs.loadDictE(15,
                        k -> k.readInt(15),
                        v -> Message.deserialize(CellSlice.beginParse(v))))
                .build();
    }

}
