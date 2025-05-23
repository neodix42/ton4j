package org.ton.ton4j.liteclient.api.block;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.io.Serializable;

@Builder
@ToString
@Getter
public class ShardFee implements Serializable {
    String label;
    Value extraFees;
    Value extraCreate;
    Value valueFees;
    Value valueCreate;
}
