package org.ton.java.tlb;

import static java.util.Objects.isNull;

import java.io.Serializable;
import lombok.Builder;
import lombok.Data;
import org.ton.java.cell.CellSlice;
import org.ton.java.cell.TonHashMapE;

@Builder
@Data
public class MessagesList implements Serializable {
  TonHashMapE list;

  public static MessagesList deserialize(CellSlice cs) {
    if (isNull(cs)) {
      return MessagesList.builder().build();
    }
    return MessagesList.builder()
        .list(
            cs.loadDictE(15, k -> k.readInt(15), v -> Message.deserialize(CellSlice.beginParse(v))))
        .build();
  }
}
