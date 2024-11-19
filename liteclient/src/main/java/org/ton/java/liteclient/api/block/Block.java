package org.ton.java.liteclient.api.block;

import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Builder
@Data
@Slf4j
public class Block implements Serializable {
    Long globalId;
    Info info;
    ValueFlow valueFlow;
    String shardState;
    Extra extra;

    public List<Transaction> listBlockTrans() {
        return extra.getAccountBlock().getTransactionsWithBlockInfo(info);
    }

    public List<Transaction> listBlockTrans(String address) {
        return extra.getAccountBlock().getTransactionsWithBlockInfo(info, address);
    }

    public List<ShardHash> allShards() {
        return extra.getMasterchainBlock().getShardHashes();
    }

    public void printAllTransactions() {
        List<Transaction> txs = listBlockTrans();
        if (txs.isEmpty()) {
            log.info("No transactions");
            return;
        }
        Transaction.printTxHeader("");
        for (Transaction tx : txs) {
            tx.printTransactionFees();
        }
        Transaction.printTxFooter();
    }

    public List<MessageFees> getAllMessageFees() {
        List<Transaction> txs = listBlockTrans();
        List<MessageFees> msgFees = new ArrayList<>();
        for (Transaction tx : txs) {
            msgFees.addAll(tx.getAllMessageFees());
        }

        return msgFees;
    }

    public void printAllMessages() {
        List<MessageFees> msgFees = getAllMessageFees();
        if (msgFees.isEmpty()) {
            log.info("No messages");
            return;
        }
        MessageFees.printMessageFeesHeader();
        for (MessageFees msgFee : msgFees) {
            msgFee.printMessageFees();
        }
        MessageFees.printMessageFeesFooter();
    }
}
