package org.ton.ton4j.tlb.print;

import java.util.ArrayList;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.ton.ton4j.tlb.*;

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
    TransactionPrintInfo.printTxHeader();
    for (Transaction tx : txs) {
      TransactionPrintInfo.printTransactionInfo(tx);
    }
    TransactionPrintInfo.printTxFooter();
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
