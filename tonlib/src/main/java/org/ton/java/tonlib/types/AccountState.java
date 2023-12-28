package org.ton.java.tonlib.types;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AccountState {
    private String code;
    private String data;
    private String frozen_hash;
    int seqno;
}
