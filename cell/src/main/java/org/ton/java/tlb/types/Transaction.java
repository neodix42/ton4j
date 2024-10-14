package org.ton.java.tlb.types;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

import java.math.BigInteger;
import lombok.Builder;
import lombok.Data;
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
      return accountAddr.toString(16);
    } else {
      return "null";
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

    Message inMsg = tx.getInOut().getIn();
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

    for (Message outMsg : tx.getInOut().getOutMessages()) {
      InternalMessageInfo intMsgInfo = (InternalMessageInfo) outMsg.getInfo();
      valueOut = valueOut.add(intMsgInfo.getValue().getCoins());
    }

    return TransactionFees.builder()
        .op(
            (isNull(op))
                ? "N/A"
                : (op.compareTo(BigInteger.ONE.negate()) != 0) ? op.toString(16) : "no body")
        .valueIn(valueIn)
        .valueOut(valueOut)
        .totalFees(totalFees)
        .outForwardFee(totalForwardFees)
        .computeFee(computeFees)
        .inForwardFee(inForwardFees)
        .exitCode(exitCode)
        .actionCode(actionCode)
        .totalActions(totalActions)
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
      return actionPhase.getTotalFwdFees();
    } else if (txDesc instanceof TransactionDescriptionSplitPrepare) {
      ActionPhase actionPhase = ((TransactionDescriptionSplitPrepare) txDesc).getActionPhase();
      return actionPhase.getTotalFwdFees();
    } else if (txDesc instanceof TransactionDescriptionTickTock) {
      ActionPhase actionPhase = ((TransactionDescriptionTickTock) txDesc).getActionPhase();
      return actionPhase.getTotalFwdFees();
    } else if (txDesc instanceof TransactionDescriptionMergeInstall) {
      ActionPhase actionPhase = ((TransactionDescriptionMergeInstall) txDesc).getActionPhase();
      return actionPhase.getTotalFwdFees();
    } else {
      return BigInteger.ZERO;
    }
  }

  private long getTotalActions(TransactionDescription txDesc) {
    if (txDesc instanceof TransactionDescriptionOrdinary) {
      ActionPhase actionPhase = ((TransactionDescriptionOrdinary) txDesc).getActionPhase();
      return actionPhase.getTotalActions();
    } else if (txDesc instanceof TransactionDescriptionSplitPrepare) {
      ActionPhase actionPhase = ((TransactionDescriptionSplitPrepare) txDesc).getActionPhase();
      return actionPhase.getTotalActions();
    } else if (txDesc instanceof TransactionDescriptionTickTock) {
      ActionPhase actionPhase = ((TransactionDescriptionTickTock) txDesc).getActionPhase();
      return actionPhase.getTotalActions();
    } else if (txDesc instanceof TransactionDescriptionMergeInstall) {
      ActionPhase actionPhase = ((TransactionDescriptionMergeInstall) txDesc).getActionPhase();
      return actionPhase.getTotalActions();
    } else {
      return -1;
    }
  }

  private long getActionCode(TransactionDescription txDesc) {
    if (txDesc instanceof TransactionDescriptionOrdinary) {
      ActionPhase actionPhase = ((TransactionDescriptionOrdinary) txDesc).getActionPhase();
      return actionPhase.getResultCode();
    } else if (txDesc instanceof TransactionDescriptionSplitPrepare) {
      ActionPhase actionPhase = ((TransactionDescriptionSplitPrepare) txDesc).getActionPhase();
      return actionPhase.getResultCode();
    } else if (txDesc instanceof TransactionDescriptionTickTock) {
      ActionPhase actionPhase = ((TransactionDescriptionTickTock) txDesc).getActionPhase();
      return actionPhase.getResultCode();
    } else if (txDesc instanceof TransactionDescriptionMergeInstall) {
      ActionPhase actionPhase = ((TransactionDescriptionMergeInstall) txDesc).getActionPhase();
      return actionPhase.getResultCode();
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

  public void printTransactionFees(boolean withHeader) {
    TransactionFees txFees = getTransactionFees();
    String header =
        "| op       | valueIn        | valueOut       | totalFees    | inForwardFee | outForwardFee | outActions | computeFee    | exitCode | actionCode |";
    if (withHeader) {
      System.out.println(
          "_________________________________________________________________________________________________________________________________________________");
      System.out.println(header);
      System.out.println(
          "-------------------------------------------------------------------------------------------------------------------------------------------------");
    }
    String str =
        String.format(
            "| %-9s| %-15s| %-15s| %-13s| %-13s| %-14s| %-11s| %-14s| %-9s| %-11s|",
            txFees.getOp(),
            Utils.formatNanoValue(txFees.getValueIn()),
            Utils.formatNanoValue(txFees.getValueOut()),
            Utils.formatNanoValue(txFees.getTotalFees()),
            Utils.formatNanoValue(txFees.getInForwardFee().toString()),
            Utils.formatNanoValue(txFees.getOutForwardFee()),
            txFees.getTotalActions(),
            Utils.formatNanoValue(txFees.getComputeFee()),
            txFees.getExitCode(),
            txFees.getActionCode());
    System.out.println(str);
  }
}
