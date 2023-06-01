package org.ton.java.tlb.types;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.math.BigInteger;

@Builder
@Getter
@Setter
@ToString
public class ActionPhase {
    boolean success; // `tlb:"bool"`
    boolean valid; // `tlb:"bool"`
    boolean noFunds; // `tlb:"bool"`
    BigInteger totalFwdFees;//           `tlb:"maybe ."`
    BigInteger totalActionFees;//        `tlb:"maybe ."`
    long resultCode; //      int32            `tlb:"## 32"`
    long resultArg; //       *int32           `tlb:"maybe ## 32"`
    long totalActions; //    uint16           `tlb:"## 16"`
    long specActions; //     uint16           `tlb:"## 16"`
    long skippedActions; //  uint16           `tlb:"## 16"`
    long messagesCreated; // uint16           `tlb:"## 16"`
    byte[] actionListHash; //  []byte           `tlb:"bits 256"`
    StorageUsedShort totalMsgSize; //  `tlb:"."`
}
