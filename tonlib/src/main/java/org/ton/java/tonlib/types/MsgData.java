package org.ton.java.tonlib.types;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class MsgData {
    private String type;// = "msg.dataRaw"; can be also msg.dataText
    private String body;
    private String init_state;

}

