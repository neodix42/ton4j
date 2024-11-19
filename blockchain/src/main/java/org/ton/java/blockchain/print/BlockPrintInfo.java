package org.ton.java.blockchain.print;

import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.ton.java.tlb.types.Block;
import org.ton.java.tlb.types.Transaction;

import java.util.ArrayList;
import java.util.List;

@Builder
@Data
@Slf4j
public class BlockPrintInfo {

    public static void printAllTransactions(Block block) {
        List<Transaction> txs = block.getAllTransactions();
        if (txs.isEmpty()) {
            log.info("No transactions");
            return;
        }
        TransactionPrintInfo.printTxHeaderWithoutBlock("");
        for (Transaction tx : txs) {
            TransactionPrintInfo.printTransactionInfo(tx);
        }
        TransactionPrintInfo.printTxFooterWithoutBlock();
    }

    public static List<MessagePrintInfo> getAllMessages(Block block) {
        List<Transaction> txs = block.getAllTransactions();
        List<MessagePrintInfo> msgFees = new ArrayList<>();
        for (Transaction tx : txs) {
            msgFees.addAll(TransactionPrintInfo.getAllMessagePrintInfo(tx));
        }

        return msgFees;
    }

    public static void printAllMessages(Block block) {
        List<MessagePrintInfo> msgFees = getAllMessages(block);
        if (msgFees.isEmpty()) {
            log.info("No messages");
            return;
        }
        MessagePrintInfo.printMessageInfoHeader();
        for (MessagePrintInfo msgFee : msgFees) {
            msgFee.printMessageInfo();
        }
        MessagePrintInfo.printMessageInfoFooter();
    }
}
