package org.ton.java.tlb.types;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.ton.java.cell.TonHashMap;

@Builder
@Getter
@Setter
@ToString
public class MessagesList {
    TonHashMap list;
}
