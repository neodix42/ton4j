package org.ton.ton4j.liteclient.api.block;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.io.Serializable;
import java.math.BigDecimal;

@Builder
@ToString
@Getter
public class TransactionStorage implements Serializable {
    BigDecimal feesCollected;
    BigDecimal feesDue;
    String statusChange;
}
