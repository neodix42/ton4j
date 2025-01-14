package org.ton.java.liteclient.api.block;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

import java.io.Serializable;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.ton.java.utils.Utils;

@Builder
@Data
@Slf4j
public class Transaction implements Serializable {

  String origStatus;
  String endStatus;
  String accountAddr;
  Long outMsgsCount;

  // A reference to exactly one inbound message (which must be present in
  // InMsgDescr as well) that has been processed by the transaction.
  Message inMsg;

  // References to several (maybe zero) outbound messages (also present
  // in OutMsgDescr and most likely included in OutMsgQueue) that have
  // been generated by the transaction.
  private List<Message> outMsgs;

  String prevTxHash;
  BigInteger lt;
  BigInteger prevTxLt;
  Long now;
  Value totalFees;
  String oldHash;
  String newHash;
  TransactionDescription description;

  Info blockInfo;

  private String getAccountAddrShort() {
    try {
      if (StringUtils.isNotEmpty(accountAddr)) {

        String str64 = StringUtils.leftPad(accountAddr, 64, "0");
        return str64.substring(0, 5)
            + "..."
            + str64.substring(str64.length() - 6, str64.length() - 1);
      } else {
        return "N/A";
      }
    } catch (Exception e) {
      return "error";
    }
  }

  private BigInteger getTotalForwardFees(Transaction tx) {
    try {
      return tx.getDescription().getAction().getTotalFwdFee().toBigInteger();
    } catch (Exception e) {
      return BigInteger.ZERO;
    }
  }

  private BigInteger getComputeFees(Transaction tx) {
    try {
      return tx.getDescription().getCompute().getGasFees().toBigInteger();
    } catch (Exception e) {
      return BigInteger.ZERO;
    }
  }

  private long getExitCode(Transaction tx) {
    try {
      return tx.getDescription().getCompute().getExitCode().longValue();
    } catch (Exception e) {
      return 0;
    }
  }

  private long getActionCode(Transaction tx) {
    try {
      return tx.getDescription().getAction().getResultCode().longValue();
    } catch (Exception e) {
      return 0;
    }
  }

  private long getTotalActions(Transaction tx) {
    try {
      return tx.getDescription().getAction().getTotActions().longValue();
    } catch (Exception e) {
      return 0;
    }
  }

  public TransactionFees getTransactionFees() {
    Transaction tx = this;

    BigInteger totalFees = tx.totalFees.getToncoins().toBigInteger();
    BigInteger totalForwardFees = getTotalForwardFees(tx);
    BigInteger computeFees = getComputeFees(tx);

    BigInteger inForwardFees = BigInteger.ZERO;
    BigInteger valueIn = BigInteger.ZERO;
    BigInteger valueOut = BigInteger.ZERO;
    BigInteger op = null;
    long exitCode = getExitCode(tx);
    long actionCode = getActionCode(tx);
    long totalActions = getTotalActions(tx);
    long now = tx.getNow();
    BigInteger lt = tx.getLt();
    long outMsgs = tx.getOutMsgsCount();

    Message inMsg = tx.getInMsg();
    if (nonNull(inMsg)) {

      op =
          nonNull(inMsg.getBody())
              ? new BigInteger(inMsg.getBody().getCells().get(0), 16)
              : BigInteger.ONE.negate();

      valueIn = inMsg.getValue().getToncoins().toBigInteger();
      inForwardFees = inMsg.getFwdFee().toBigInteger();
    }

    for (Message outMsg : tx.getOutMsgs()) {
      if (outMsg.getType().equals("Internal")) {
        valueOut = valueOut.add(outMsg.getValue().getToncoins().toBigInteger());
      }
    }

    return TransactionFees.builder()
        .op(
            (isNull(op))
                ? "N/A"
                : (op.compareTo(BigInteger.ONE.negate()) != 0) ? op.toString(16) : "no body")
        .type(tx.getDescription().getType())
        .valueIn(valueIn)
        .valueOut(valueOut)
        .totalFees(totalFees)
        .outForwardFee(totalForwardFees)
        .computeFee(computeFees)
        .inForwardFee(inForwardFees)
        .exitCode(exitCode)
        .actionCode(actionCode)
        .totalActions(totalActions)
        .aborted(nonNull(tx.getDescription().aborted) && tx.getDescription().aborted == 1)
        .now(now)
        .lt(lt)
        .account(nonNull(tx.getAccountAddr()) ? tx.getAccountAddr() : "")
        .block(tx.getBlockInfo().getShortBlockSeqno())
        .hash("can't get")
        .build();
  }

  public void printTransactionFees() {
    printTransactionFees(false, false);
  }

  public void printTransactionFees(boolean withHeader, boolean withFooter, String balance) {
    TransactionFees txFees = getTransactionFees();

    if (withHeader) {
      printTxHeader(" (initial balance " + balance + ")");
    }
    printTxData(txFees);

    if (withFooter) {
      printTxFooter();
    }
  }

  public void printTransactionFees(boolean withHeader, boolean withFooter) {
    TransactionFees txFees = getTransactionFees();

    if (withHeader) {
      printTxHeader("");
    }
    printTxData(txFees);

    if (withFooter) {
      printTxFooter();
    }
  }

  public List<MessageFees> getAllMessageFees() {
    List<MessageFees> messageFees = new ArrayList<>();
    Transaction tx = this;

    BigInteger op;

    Message inMsg = tx.getInMsg();
    if (nonNull(inMsg)) {

      op =
          nonNull(inMsg.getBody())
              ? new BigInteger(inMsg.getBody().getCells().get(0), 16)
              : BigInteger.ONE.negate();

      if (inMsg.getType().equals("Internal")) {
        MessageFees msgFee =
            MessageFees.builder()
                .direction("in")
                .type("internal-in")
                .op(
                    (isNull(op))
                        ? "N/A"
                        : (op.compareTo(BigInteger.ONE.negate()) != 0)
                            ? op.toString(16)
                            : "no body")
                .value(inMsg.getValue().getToncoins().toBigInteger())
                .fwdFee(inMsg.getFwdFee().toBigInteger())
                .ihrFee(inMsg.getIhrFee().toBigInteger())
                .createdLt(inMsg.getCreatedLt())
                .createdAt(inMsg.getCreatedAt())
                .src(inMsg.getSrcAddr().getAddress())
                .dst(inMsg.getDestAddr().getAddress())
                .build();
        messageFees.add(msgFee);
      }
      if (inMsg.getType().equals("External Out")) {
        MessageFees msgFee =
            MessageFees.builder()
                .direction("in")
                .type("external-out")
                .op(
                    (isNull(op))
                        ? "N/A"
                        : (op.compareTo(BigInteger.ONE.negate()) != 0)
                            ? op.toString(16)
                            : "no body")
                .createdLt(inMsg.getCreatedLt())
                .createdAt(inMsg.getCreatedAt())
                .src(inMsg.getSrcAddr().getAddress())
                .dst(inMsg.getDestAddr().getAddress())
                .build();
        messageFees.add(msgFee);
      }
      if (inMsg.getType().equals("External In")) {
        MessageFees msgFee =
            MessageFees.builder()
                .direction("in")
                .type("external-in")
                .op(
                    (isNull(op))
                        ? "N/A"
                        : (op.compareTo(BigInteger.ONE.negate()) != 0)
                            ? op.toString(16)
                            : "no body")
                .importFee(inMsg.getImportFee().toBigInteger())
                .src(inMsg.getSrcAddr().getAddress())
                .dst(inMsg.getDestAddr().getAddress())
                .build();
        messageFees.add(msgFee);
      }
    }

    for (Message outMsg : tx.getOutMsgs()) {

      op =
          nonNull(outMsg.getBody())
              ? new BigInteger(outMsg.getBody().getCells().get(0), 16)
              : BigInteger.ONE.negate();

      if (outMsg.getType().equals("InternalMessageInfo")) {

        MessageFees msgFee =
            MessageFees.builder()
                .direction("out")
                .type(formatMsgType("InternalMessageInfo"))
                .op(
                    (isNull(op))
                        ? "N/A"
                        : (op.compareTo(BigInteger.ONE.negate()) != 0)
                            ? op.toString(16)
                            : "no body")
                .value(outMsg.getValue().getToncoins().toBigInteger())
                .fwdFee(outMsg.getFwdFee().toBigInteger())
                .ihrFee(outMsg.getIhrFee().toBigInteger())
                .createdLt(outMsg.getCreatedLt())
                .createdAt(outMsg.getCreatedAt())
                .src(outMsg.getSrcAddr().getAddress())
                .dst(outMsg.getDestAddr().getAddress())
                .build();
        messageFees.add(msgFee);
      }
      if (outMsg.getType().equals("ExternalMessageOutInfo")) {

        MessageFees msgFee =
            MessageFees.builder()
                .direction("out")
                .type(formatMsgType("ExternalMessageOutInfo"))
                .op(
                    (isNull(op))
                        ? "N/A"
                        : (op.compareTo(BigInteger.ONE.negate()) != 0)
                            ? op.toString(16)
                            : "no body")
                .createdLt(outMsg.getCreatedLt())
                .createdAt(outMsg.getCreatedAt())
                .src(outMsg.getSrcAddr().getAddress())
                .dst(outMsg.getDestAddr().getAddress())
                .build();
        messageFees.add(msgFee);
      }
      if (outMsg.getType().equals("ExternalMessageInInfo")) {

        MessageFees msgFee =
            MessageFees.builder()
                .direction("out")
                .type(formatMsgType("ExternalMessageInInfo"))
                .op(
                    (isNull(op))
                        ? "N/A"
                        : (op.compareTo(BigInteger.ONE.negate()) != 0)
                            ? op.toString(16)
                            : "no body")
                .importFee(outMsg.getImportFee().toBigInteger())
                .src(outMsg.getSrcAddr().getAddress())
                .dst(outMsg.getDestAddr().getAddress())
                .build();
        messageFees.add(msgFee);
      }
    }

    return messageFees;
  }

  public void printAllMessages(boolean withHeader) {
    List<MessageFees> msgFees = getAllMessageFees();
    if (msgFees.isEmpty()) {
      //      log.info("No messages");
      return;
    }

    if (withHeader) {
      MessageFees.printMessageFeesHeader();
    }

    for (MessageFees msgFee : msgFees) {
      msgFee.printMessageFees();
    }
    //    MessageFees.printMessageFeesFooter();
  }

  public static void printTxHeader(String balance) {
    log.info("");
    log.info("Transactions" + balance);
    log.info(
        "------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------");
    log.info(
        "| txHash   | time     | op       | type         | valueIn        | valueOut       | totalFees           | inForwardFee | outForwardFee | outActions | outMsgs | computeFee          | aborted | exitCode | actionCode | account                                                          | block                           |");
    log.info(
        "------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------");
  }

  public static void printTxFooter() {
    log.info(
        "------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------");
  }

  public static void printTxData(TransactionFees txFees) {
    String str =
        String.format(
            "| %-8s | %-8s | %-9s| %-13s| %-15s| %-15s| %-20s| %-13s| %-14s| %-11s| %-8s| %-20s| %-8s| %-9s| %-11s| %-13s | %-31s |",
            txFees.getHash().substring(0, 5),
            Utils.toUTCTimeOnly(txFees.getNow()),
            txFees.getOp(),
            txFees.getType(),
            Utils.formatNanoValueZero(txFees.getValueIn()),
            Utils.formatNanoValueZero(txFees.getValueOut()),
            Utils.formatNanoValueZero(txFees.getTotalFees()),
            Utils.formatNanoValueZero(txFees.getInForwardFee()),
            Utils.formatNanoValueZero(txFees.getOutForwardFee()),
            txFees.getTotalActions(),
            txFees.getOutMsgs(),
            Utils.formatNanoValueZero(txFees.getComputeFee()),
            txFees.isAborted(),
            txFees.getExitCode(),
            txFees.getActionCode(),
            txFees.getAccount(),
            txFees.getBlock());
    log.info(str);
  }

  private String formatMsgType(String fullMsgType) {
    if (fullMsgType.equals("InternalMessageInfo")) {
      return "internal-in";
    } else if (fullMsgType.equals("ExternalMessageOutInfo")) {
      return "external-out";
    } else {
      return "external-in";
    }
  }
}
