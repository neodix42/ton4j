package org.ton.java.smartcontract.types;

import lombok.Builder;
import lombok.Data;

import java.math.BigInteger;
import java.util.List;

@Builder
@Data
public class MultiSigConfig {

    BigInteger queryId;
    /**
     * Whitelist of allowed destinations
     */
    public List<OwnerInfo> owners;
    public List<PendingQuery> pendingQueries;

    public long rootI;
    /**
     * Minimum amount of signatures for order to execute.
     * <p>
     * E.g. n = 5, k = 3, means at least 3 out of 5 signatures must be collected
     */
    public int k;
    /**
     * total amount of private kyes
     */
    public int n;
}
