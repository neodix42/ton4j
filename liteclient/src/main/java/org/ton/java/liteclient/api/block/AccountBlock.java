package org.ton.java.liteclient.api.block;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Builder
@ToString
@Getter
public class AccountBlock implements Serializable {
    private List<Transaction> transactions;

    public List<Transaction> getTransactionsWithBlockInfo(Info blockInfo) {
        List<Transaction> txs = transactions;
        for (Transaction tx : txs) {
            tx.setBlockInfo(blockInfo);
        }
        return txs;
    }

    public List<Transaction> getTransactionsWithBlockInfo(Info blockInfo, String address) {
        List<Transaction> result = new ArrayList<>();
        for (Transaction tx : transactions) {
            // if (tx.accountAddr.equals(address)) {
            if (address.contains(tx.accountAddr.toLowerCase())) {
                tx.setBlockInfo(blockInfo);
                result.add(tx);
            }
        }
        return result;
    }
}
