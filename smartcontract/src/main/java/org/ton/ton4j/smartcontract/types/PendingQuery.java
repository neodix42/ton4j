package org.ton.ton4j.smartcontract.types;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import org.ton.ton4j.cell.Cell;

import java.math.BigInteger;

@Builder
@Getter
@ToString
public class PendingQuery {
    BigInteger queryId;
    long creatorI;
    long cnt; // current number of collected confirmations

    // bits of length n, with active bit at position of public keys array. 101 - signed with pubkey[0] and pubkey[2]
    long cntBits;
    Cell msg;
}
