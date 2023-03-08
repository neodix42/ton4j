package org.ton.java.tonlib.types;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;

@Builder
@Setter
@Getter
@ToString
public class AccountState implements Serializable {
    String code;
    String data;
    String frozen_hash;
    long wallet_id;
    int seqno;
}
