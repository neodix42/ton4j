package org.ton.java.tlb.types;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.CellSlice;
import org.ton.java.cell.TonHashMapE;
import org.ton.java.utils.Utils;

/**
 *
 *
 * <pre>
 * transaction$0111
 *   account_addr:bits256
 *   lt:uint64
 *   prev_trans_hash:bits256
 *   prev_trans_lt:uint64
 *   now:uint32
 *   outmsg_cnt:uint15
 *   orig_status:AccountStatus
 *   end_status:AccountStatus
 *   ^[
 *     in_msg:(Maybe ^(Message Any))
 *     out_msgs:(HashmapE 15 ^(Message Any))
 *     ]
 *   total_fees:CurrencyCollection
 *   state_update:^(HASH_UPDATE Account)
 *   description:^TransactionDescr = Transaction;
 *   </pre>
 */
@Builder
@Data
public class Transaction {
  int magic;
  BigInteger accountAddr;
  BigInteger lt;
  BigInteger prevTxHash;
  BigInteger prevTxLt;
  long now;
  long outMsgCount;
  AccountStates origStatus;
  AccountStates endStatus;
  TransactionIO inOut;
  CurrencyCollection totalFees;
  HashUpdate stateUpdate;
  TransactionDescription description;

  // not in scheme, but might be filled based on request data for flexibility
  byte[] hash;

  private String getMagic() {
    return Long.toBinaryString(magic);
  }

  private String getAccountAddr() {
    if (nonNull(accountAddr)) {
      return StringUtils.leftPad(accountAddr.toString(16), 64, "0");
    } else {
      return "N/A";
    }
  }

  private String getPrevTxHash() {
    if (nonNull(accountAddr)) {
      return prevTxHash.toString(16);
    } else {
      return "null";
    }
  }

  public Cell toCell() {
    CellBuilder c = CellBuilder.beginCell();
    c.storeUint(0b0111, 4);
    c.storeUint(accountAddr, 256);
    c.storeUint(lt, 64);
    c.storeUint(prevTxHash, 256);
    c.storeUint(prevTxLt, 64);
    c.storeUint(now, 32);
    c.storeUint(outMsgCount, 15);
    c.storeCell(serializeAccountState(origStatus));
    c.storeCell(serializeAccountState(endStatus));
    c.storeCell(totalFees.toCell());

    c.storeRef(inOut.toCell());
    c.storeRef(stateUpdate.toCell());
    c.storeRef(description.toCell());

    return c.endCell();
  }

  public static Transaction deserialize(CellSlice cs) {
    long magic = cs.loadUint(4).intValue();
    assert (magic == 0b0111)
        : "Transaction: magic not equal to 0b0111, found 0b" + Long.toBinaryString(magic);

    Transaction tx =
        Transaction.builder()
            .magic(0b0111)
            .accountAddr(cs.loadUint(256))
            .lt(cs.loadUint(64))
            .prevTxHash(cs.loadUint(256))
            .prevTxLt(cs.loadUint(64))
            .now(cs.loadUint(32).longValue())
            .outMsgCount(cs.loadUint(15).intValue())
            .origStatus(deserializeAccountState(cs.loadUint(2).byteValue()))
            .endStatus(deserializeAccountState(cs.loadUint(2).byteValueExact()))
            .build();

    CellSlice inOutMsgs = CellSlice.beginParse(cs.loadRef());
    Message msg =
        inOutMsgs.loadBit() ? Message.deserialize(CellSlice.beginParse(inOutMsgs.loadRef())) : null;
    TonHashMapE out =
        inOutMsgs.loadDictE(
            15,
            k -> k.readInt(15),
            v -> Message.deserialize(CellSlice.beginParse(CellSlice.beginParse(v).loadRef())));

    tx.setInOut(TransactionIO.builder().in(msg).out(out).build());

    //        if (nonNull(tx.getInOut().getOut())) { // todo cleanup
    //            for (Map.Entry<Object, Object> entry : tx.getInOut().getOut().elements.entrySet())
    // {
    //                System.out.println("key " + entry.getKey() + ", value " + ((Message)
    // entry.getValue()));
    //            }
    //        }

    tx.setTotalFees(CurrencyCollection.deserialize(cs));
    tx.setStateUpdate(HashUpdate.deserialize(CellSlice.beginParse(cs.loadRef())));
    tx.setDescription(TransactionDescription.deserialize(CellSlice.beginParse(cs.loadRef())));

    return tx;
  }

  public static Cell serializeAccountState(AccountStates state) {
    switch (state) {
      case UNINIT:
        {
          return CellBuilder.beginCell().storeUint(0, 2).endCell();
        }
      case FROZEN:
        {
          return CellBuilder.beginCell().storeUint(1, 2).endCell();
        }
      case ACTIVE:
        {
          return CellBuilder.beginCell().storeUint(2, 2).endCell();
        }
      case NON_EXIST:
        {
          return CellBuilder.beginCell().storeUint(3, 2).endCell();
        }
    }
    return null;
  }

  public static AccountStates deserializeAccountState(byte state) {
    switch (state) {
      case 0:
        {
          return AccountStates.UNINIT;
        }
      case 1:
        {
          return AccountStates.FROZEN;
        }
      case 2:
        {
          return AccountStates.ACTIVE;
        }
      case 3:
        {
          return AccountStates.NON_EXIST;
        }
    }
    return null;
  }

  public TransactionFees getTransactionFees() {
    Transaction tx = this;

    BigInteger totalFees = tx.getTotalFees().getCoins();
    BigInteger totalForwardFees = getForwardFees(tx.getDescription());
    BigInteger computeFees = getComputeFees(tx.getDescription());

    BigInteger inForwardFees = BigInteger.ZERO;
    BigInteger valueIn = BigInteger.ZERO;
    BigInteger valueOut = BigInteger.ZERO;
    BigInteger op = null;
    long exitCode = getExitCode(tx.getDescription());
    long actionCode = getActionCode(tx.getDescription());
    long totalActions = getTotalActions(tx.getDescription());
    long now = tx.getNow();
    BigInteger lt = tx.getLt();
    long outMsgs = tx.getOutMsgCount();

    Message inMsg = tx.getInOut().getIn();
    if (nonNull(inMsg)) {
      Cell body = inMsg.getBody();

      if (nonNull(body) && body.getBits().getUsedBits() >= 32) {
        op = CellSlice.beginParse(body).preloadInt(32);
      } else {
        op = BigInteger.ONE.negate();
      }

      if (inMsg.getInfo() instanceof InternalMessageInfo) {
        valueIn = ((InternalMessageInfo) inMsg.getInfo()).getValue().getCoins();
        inForwardFees = ((InternalMessageInfo) inMsg.getInfo()).getFwdFee();
      }
    }

    for (Message outMsg : tx.getInOut().getOutMessages()) {
      if (outMsg.getInfo() instanceof InternalMessageInfo) {
        InternalMessageInfo intMsgInfo = (InternalMessageInfo) outMsg.getInfo();
        valueOut = valueOut.add(intMsgInfo.getValue().getCoins());
      }
    }

    return TransactionFees.builder()
        .op(
            (isNull(op))
                ? "N/A"
                : (op.compareTo(BigInteger.ONE.negate()) != 0) ? op.toString(16) : "no body")
        .type(tx.getDescription().getClass().getSimpleName().substring(22))
        .valueIn(valueIn)
        .valueOut(valueOut)
        .totalFees(totalFees)
        .outForwardFee(totalForwardFees)
        .computeFee(computeFees)
        .inForwardFee(inForwardFees)
        .exitCode(exitCode)
        .actionCode(actionCode)
        .totalActions(totalActions)
        .now(now)
        .lt(lt)
        .account(tx.getAccountAddr())
        .build();
  }

  private BigInteger getComputeFees(TransactionDescription txDesc) {
    if (txDesc instanceof TransactionDescriptionOrdinary) {
      ComputePhase computePhase = ((TransactionDescriptionOrdinary) txDesc).getComputePhase();
      if (computePhase instanceof ComputePhaseVM) {
        return ((ComputePhaseVM) computePhase).getGasFees();
      }
    } else if (txDesc instanceof TransactionDescriptionSplitPrepare) {
      ComputePhase computePhase = ((TransactionDescriptionSplitPrepare) txDesc).getComputePhase();
      if (computePhase instanceof ComputePhaseVM) {
        return ((ComputePhaseVM) computePhase).getGasFees();
      }
    } else if (txDesc instanceof TransactionDescriptionTickTock) {
      ComputePhase computePhase = ((TransactionDescriptionTickTock) txDesc).getComputePhase();
      if (computePhase instanceof ComputePhaseVM) {
        return ((ComputePhaseVM) computePhase).getGasFees();
      }
    } else if (txDesc instanceof TransactionDescriptionMergeInstall) {
      ComputePhase computePhase = ((TransactionDescriptionMergeInstall) txDesc).getComputePhase();
      if (computePhase instanceof ComputePhaseVM) {
        return ((ComputePhaseVM) computePhase).getGasFees();
      }
    } else {
      return BigInteger.ZERO;
    }
    return BigInteger.ZERO;
  }

  private BigInteger getForwardFees(TransactionDescription txDesc) {
    if (txDesc instanceof TransactionDescriptionOrdinary) {
      ActionPhase actionPhase = ((TransactionDescriptionOrdinary) txDesc).getActionPhase();
      return isNull(actionPhase) ? BigInteger.ZERO : actionPhase.getTotalFwdFees();
    } else if (txDesc instanceof TransactionDescriptionSplitPrepare) {
      ActionPhase actionPhase = ((TransactionDescriptionSplitPrepare) txDesc).getActionPhase();
      return isNull(actionPhase) ? BigInteger.ZERO : actionPhase.getTotalFwdFees();
    } else if (txDesc instanceof TransactionDescriptionTickTock) {
      ActionPhase actionPhase = ((TransactionDescriptionTickTock) txDesc).getActionPhase();
      return isNull(actionPhase) ? BigInteger.ZERO : actionPhase.getTotalFwdFees();
    } else if (txDesc instanceof TransactionDescriptionMergeInstall) {
      ActionPhase actionPhase = ((TransactionDescriptionMergeInstall) txDesc).getActionPhase();
      return isNull(actionPhase) ? BigInteger.ZERO : actionPhase.getTotalFwdFees();
    } else {
      return BigInteger.ZERO;
    }
  }

  private long getTotalActions(TransactionDescription txDesc) {
    if (txDesc instanceof TransactionDescriptionOrdinary) {
      ActionPhase actionPhase = ((TransactionDescriptionOrdinary) txDesc).getActionPhase();
      return isNull(actionPhase) ? 0 : actionPhase.getTotalActions();
    } else if (txDesc instanceof TransactionDescriptionSplitPrepare) {
      ActionPhase actionPhase = ((TransactionDescriptionSplitPrepare) txDesc).getActionPhase();
      return isNull(actionPhase) ? 0 : actionPhase.getTotalActions();
    } else if (txDesc instanceof TransactionDescriptionTickTock) {
      ActionPhase actionPhase = ((TransactionDescriptionTickTock) txDesc).getActionPhase();
      return isNull(actionPhase) ? 0 : actionPhase.getTotalActions();
    } else if (txDesc instanceof TransactionDescriptionMergeInstall) {
      ActionPhase actionPhase = ((TransactionDescriptionMergeInstall) txDesc).getActionPhase();
      return isNull(actionPhase) ? 0 : actionPhase.getTotalActions();
    } else {
      return -1;
    }
  }

  private long getActionCode(TransactionDescription txDesc) {
    if (txDesc instanceof TransactionDescriptionOrdinary) {
      ActionPhase actionPhase = ((TransactionDescriptionOrdinary) txDesc).getActionPhase();
      return isNull(actionPhase) ? 0 : actionPhase.getResultCode();
    } else if (txDesc instanceof TransactionDescriptionSplitPrepare) {
      ActionPhase actionPhase = ((TransactionDescriptionSplitPrepare) txDesc).getActionPhase();
      return isNull(actionPhase) ? 0 : actionPhase.getResultCode();
    } else if (txDesc instanceof TransactionDescriptionTickTock) {
      ActionPhase actionPhase = ((TransactionDescriptionTickTock) txDesc).getActionPhase();
      return isNull(actionPhase) ? 0 : actionPhase.getResultCode();
    } else if (txDesc instanceof TransactionDescriptionMergeInstall) {
      ActionPhase actionPhase = ((TransactionDescriptionMergeInstall) txDesc).getActionPhase();
      return isNull(actionPhase) ? 0 : actionPhase.getResultCode();
    } else {
      return -1;
    }
  }

  private long getExitCode(TransactionDescription txDesc) {
    if (txDesc instanceof TransactionDescriptionOrdinary) {
      ComputePhase computePhase = ((TransactionDescriptionOrdinary) txDesc).getComputePhase();
      if (computePhase instanceof ComputePhaseVM) {
        return ((ComputePhaseVM) computePhase).getDetails().getExitCode();
      }
    } else if (txDesc instanceof TransactionDescriptionSplitPrepare) {
      ComputePhase computePhase = ((TransactionDescriptionSplitPrepare) txDesc).getComputePhase();
      if (computePhase instanceof ComputePhaseVM) {
        return ((ComputePhaseVM) computePhase).getDetails().getExitCode();
      }
    } else if (txDesc instanceof TransactionDescriptionTickTock) {
      ComputePhase computePhase = ((TransactionDescriptionTickTock) txDesc).getComputePhase();
      if (computePhase instanceof ComputePhaseVM) {
        return ((ComputePhaseVM) computePhase).getDetails().getExitCode();
      }
    } else if (txDesc instanceof TransactionDescriptionMergeInstall) {
      ComputePhase computePhase = ((TransactionDescriptionMergeInstall) txDesc).getComputePhase();
      if (computePhase instanceof ComputePhaseVM) {
        return ((ComputePhaseVM) computePhase).getDetails().getExitCode();
      }
    } else {
      return -1;
    }
    return -1;
  }

  public void printTransactionFees() {
    printTransactionFees(false, false);
  }

  public void printTransactionFees(boolean withHeader, boolean withFooter) {
    TransactionFees txFees = getTransactionFees();
    if (withHeader) {
      printTxHeader();
    }
    String str =
        String.format(
            "| %-9s| %-13s| %-15s| %-15s| %-13s| %-13s| %-14s| %-11s| %-8s| %-14s| %-9s| %-11s| %-64s |",
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
            txFees.getExitCode(),
            txFees.getActionCode(),
            txFees.getAccount());
    System.out.println(str);
    if (withFooter) {
      printTxFooter();
    }
  }

  public List<MessageFees> getAllMessageFees() {
    List<MessageFees> messageFees = new ArrayList<>();
    Transaction tx = this;

    BigInteger op;

    Message inMsg = tx.getInOut().getIn();
    if (nonNull(inMsg)) {
      Cell body = inMsg.getBody();

      if (nonNull(body) && body.getBits().getUsedBits() >= 32) {
        op = CellSlice.beginParse(body).preloadInt(32);
      } else {
        op = BigInteger.ONE.negate();
      }

      if (inMsg.getInfo() instanceof InternalMessageInfo) {
        InternalMessageInfo msgInfo = ((InternalMessageInfo) inMsg.getInfo());
        MessageFees msgFee =
            MessageFees.builder()
                .direction("in")
                .type(inMsg.getInfo().getClass().getSimpleName())
                .op(
                    (isNull(op))
                        ? "N/A"
                        : (op.compareTo(BigInteger.ONE.negate()) != 0)
                            ? op.toString(16)
                            : "no body")
                .value(msgInfo.getValue().getCoins())
                .fwdFee(msgInfo.getFwdFee())
                .ihrFee(msgInfo.getIHRFee())
                .createdLt(msgInfo.getCreatedLt())
                .createdAt(BigInteger.valueOf(msgInfo.getCreatedAt()))
                .build();
        messageFees.add(msgFee);
      }
      if (inMsg.getInfo() instanceof ExternalMessageOutInfo) {
        ExternalMessageOutInfo msgInfo = ((ExternalMessageOutInfo) inMsg.getInfo());
        MessageFees msgFee =
            MessageFees.builder()
                .direction("in")
                .type(inMsg.getInfo().getClass().getSimpleName())
                .op(
                    (isNull(op))
                        ? "N/A"
                        : (op.compareTo(BigInteger.ONE.negate()) != 0)
                            ? op.toString(16)
                            : "no body")
                .createdLt(msgInfo.getCreatedLt())
                .createdAt(BigInteger.valueOf(msgInfo.getCreatedAt()))
                .build();
        messageFees.add(msgFee);
      }
      if (inMsg.getInfo() instanceof ExternalMessageInInfo) {
        ExternalMessageInInfo msgInfo = ((ExternalMessageInInfo) inMsg.getInfo());
        MessageFees msgFee =
            MessageFees.builder()
                .direction("in")
                .type(inMsg.getInfo().getClass().getSimpleName())
                .op(
                    (isNull(op))
                        ? "N/A"
                        : (op.compareTo(BigInteger.ONE.negate()) != 0)
                            ? op.toString(16)
                            : "no body")
                .importFee(msgInfo.getImportFee())
                .build();
        messageFees.add(msgFee);
      }
    }

    for (Message outMsg : tx.getInOut().getOutMessages()) {

      Cell body = outMsg.getBody();

      if (nonNull(body) && body.getBits().getUsedBits() >= 32) {
        op = CellSlice.beginParse(body).preloadInt(32);
      } else {
        op = BigInteger.ONE.negate();
      }

      if (outMsg.getInfo() instanceof InternalMessageInfo) {
        InternalMessageInfo intMsgInfo = (InternalMessageInfo) outMsg.getInfo();

        MessageFees msgFee =
            MessageFees.builder()
                .direction("out")
                .type(outMsg.getInfo().getClass().getSimpleName())
                .op(
                    (isNull(op))
                        ? "N/A"
                        : (op.compareTo(BigInteger.ONE.negate()) != 0)
                            ? op.toString(16)
                            : "no body")
                .value(intMsgInfo.getValue().getCoins())
                .fwdFee(intMsgInfo.getFwdFee())
                .ihrFee(intMsgInfo.getIHRFee())
                .createdLt(intMsgInfo.getCreatedLt())
                .createdAt(BigInteger.valueOf(intMsgInfo.getCreatedAt()))
                .build();
        messageFees.add(msgFee);
      }
      if (outMsg.getInfo() instanceof ExternalMessageOutInfo) {
        ExternalMessageOutInfo outMsgInfo = (ExternalMessageOutInfo) outMsg.getInfo();

        MessageFees msgFee =
            MessageFees.builder()
                .direction("out")
                .type(outMsg.getInfo().getClass().getSimpleName())
                .op(
                    (isNull(op))
                        ? "N/A"
                        : (op.compareTo(BigInteger.ONE.negate()) != 0)
                            ? op.toString(16)
                            : "no body")
                .createdLt(outMsgInfo.getCreatedLt())
                .createdAt(BigInteger.valueOf(outMsgInfo.getCreatedAt()))
                .build();
        messageFees.add(msgFee);
      }
      if (outMsg.getInfo() instanceof ExternalMessageInInfo) {
        ExternalMessageInInfo outMsgInfo = (ExternalMessageInInfo) outMsg.getInfo();

        MessageFees msgFee =
            MessageFees.builder()
                .direction("out")
                .type(outMsg.getInfo().getClass().getSimpleName())
                .op(
                    (isNull(op))
                        ? "N/A"
                        : (op.compareTo(BigInteger.ONE.negate()) != 0)
                            ? op.toString(16)
                            : "no body")
                .importFee(outMsgInfo.getImportFee())
                .build();
        messageFees.add(msgFee);
      }
    }

    return messageFees;
  }

  public void printAllMessages(boolean withHeader) {
    List<MessageFees> msgFees = getAllMessageFees();

    if (withHeader) {
      MessageFees.printMessageFeesHeader();
    }

    for (MessageFees msgFee : msgFees) {
      msgFee.printMessageFees();
    }
    MessageFees.printMessageFeesFooter();
  }

  public static void printTxHeader() {
    System.out.println("Transactions");
    System.out.println(
        "---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------");
    System.out.println(
        "| op       | type         | valueIn        | valueOut       | totalFees    | inForwardFee | outForwardFee | outActions | outMsgs | computeFee    | exitCode | actionCode | account                                                          |");
    System.out.println(
        "---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------");
  }

  public static void printTxFooter() {
    System.out.println(
        "---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------");
  }
}
