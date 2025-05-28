package org.ton.ton4j.tlb;

import static java.util.Objects.isNull;

import java.io.Serializable;
import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.cell.CellSlice;
import org.ton.ton4j.cell.TonHashMapE;

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
