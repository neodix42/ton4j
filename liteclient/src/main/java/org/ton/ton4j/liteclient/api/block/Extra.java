package org.ton.ton4j.liteclient.api.block;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.io.Serializable;

/**
 * Masterchain block not included
 */
@Builder
@ToString
@Getter
public class Extra implements Serializable {
    private InMsgDescr inMsgDescrs;
    private OutMsgDescr outMsgsDescrs;
    private AccountBlock accountBlock;
    String randSeed;
    String createdBy;
    MasterchainBlock masterchainBlock;
}
