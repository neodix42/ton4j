package org.ton.java.tlb.types;

import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.CellSlice;
import org.ton.java.cell.TonHashMapE;
import org.ton.java.utils.Utils;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

/**
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
@Slf4j
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

    private static String txHeaderInfoWithBlockTop = "|                                                                                                                  |         Storage Phase        |                  Compute Phase               |                          Action Phase                      |                                                 |";
    private static String txHeaderInfoWithBlock = "| txHash  | timestamp | lt             | op        | type         | valueIn        | valueOut       | totalFees    | fees         | dueFees       | success | gasFees       | vmSteps | exitCode | success | fordwardFees | actionFees   | actions | exitCode | account       | block                           |";

    private static String txHeaderInfoWithoutBlockTop = "|                                                                                                                  |         Storage Phase        |                  Compute Phase               |                          Action Phase                      |               | ";
    private static String txHeaderInfoWithoutBlock = "| txHash  | timestamp | lt             | op        | type         | valueIn        | valueOut       | totalFees    | fees         | dueFees       | success | gasFees       | vmSteps | exitCode | success | fordwardFees | actionFees   | actions | exitCode | account       |";

    private String getAccountAddrShort() {
        if (nonNull(accountAddr)) {
            String str64 = StringUtils.leftPad(accountAddr.toString(16), 64, "0");
            return str64.substring(0, 5)
                    + "..."
                    + str64.substring(str64.length() - 5);
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

        tx.setTotalFees(CurrencyCollection.deserialize(cs));
        tx.setStateUpdate(HashUpdate.deserialize(CellSlice.beginParse(cs.loadRef())));
        tx.setDescription(TransactionDescription.deserialize(CellSlice.beginParse(cs.loadRef())));

        return tx;
    }

    public static Cell serializeAccountState(AccountStates state) {
        switch (state) {
            case UNINIT: {
                return CellBuilder.beginCell().storeUint(0, 2).endCell();
            }
            case FROZEN: {
                return CellBuilder.beginCell().storeUint(1, 2).endCell();
            }
            case ACTIVE: {
                return CellBuilder.beginCell().storeUint(2, 2).endCell();
            }
            case NON_EXIST: {
                return CellBuilder.beginCell().storeUint(3, 2).endCell();
            }
        }
        return null;
    }

    public static AccountStates deserializeAccountState(byte state) {
        switch (state) {
            case 0: {
                return AccountStates.UNINIT;
            }
            case 1: {
                return AccountStates.FROZEN;
            }
            case 2: {
                return AccountStates.ACTIVE;
            }
            case 3: {
                return AccountStates.NON_EXIST;
            }
        }
        return null;
    }

    public TransactionPrintInfo getTransactionPrintInfo() {
        Transaction tx = this;

        BigInteger totalFees = tx.getTotalFees().getCoins();

        StoragePhase storagePhase = tx.getStoragePhase(tx.getDescription());
        ComputePhaseVM computePhase = tx.getComputePhaseVm(tx.getDescription());
        ActionPhase actionPhase = tx.getActionPhase(tx.getDescription());

        BigInteger storageFeesCollected = nonNull(storagePhase) ? storagePhase.getStorageFeesCollected() : null;
        BigInteger storageDueFees = nonNull(storagePhase) ? storagePhase.getStorageFeesDue() : null;
        String storageStatus = nonNull(storagePhase) ? storagePhase.getStatusChange().getType() : "N/A";

        BigInteger computeGasFees = nonNull(computePhase) ? computePhase.getGasFees() : null;
        long computeVmSteps = nonNull(computePhase) ? computePhase.getDetails().getVMSteps() : 0;
        String computeSuccess = nonNull(computePhase) ? computePhase.isSuccess() ? "yes" : "no" :
                nonNull(tx.getComputePhaseSkipReason(tx.getDescription())) ? "skipped" : "N/A";

        BigInteger actionTotalFwdFees = nonNull(actionPhase) ? actionPhase.getTotalFwdFees() : null;
        BigInteger actionTotalActionFees = nonNull(actionPhase) ? actionPhase.getTotalActionFees() : null;
        String actionSuccess = nonNull(actionPhase) ? actionPhase.isSuccess() ? "yes" : "no" : "N/A";
        long actionResultCode = nonNull(actionPhase) ? actionPhase.getResultCode() : 0;

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
        String hash = "not available";

        if (nonNull(tx.getInOut())) {
            Message inMsg = tx.getInOut().getIn();
            if (nonNull(inMsg)) {
                Cell body = inMsg.getBody();
                hash = "todome";

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
        }

        return TransactionPrintInfo.builder()
                .hash(hash)
                .now(now)
                .op(
                        (isNull(op))
                                ? "N/A"
                                : (op.compareTo(BigInteger.ONE.negate()) != 0) ? op.toString(16) : "no body")
                .type(
                        nonNull(tx.getDescription())
                                ? tx.getDescription().getClass().getSimpleName().substring(22)
                                : "")
                .valueIn(valueIn)
                .valueOut(valueOut)
                .totalFees(totalFees)
                .storageFeesCollected(storageFeesCollected)
                .storageDueFees(storageDueFees)
                .storageStatus(storageStatus)
                .computeSuccess(computeSuccess)
                .computeGasFees(computeGasFees)
                .computeVmSteps(computeVmSteps)
                .actionSuccess(actionSuccess)
                .actionTotalFwdFees(actionTotalFwdFees)
                .actionTotalActionFees(actionTotalActionFees)
                .actionTotalActions(totalActions)
                .actionResultCode(actionResultCode)
                .inForwardFee(inForwardFees)
                .exitCode(exitCode)
                .actionCode(actionCode)
                .lt(lt)
                .account(nonNull(tx.getAccountAddr()) ? tx.getAccountAddrShort() : "")
                .build();
    }

    private StoragePhase getStoragePhase(TransactionDescription txDesc) {
        if (txDesc instanceof TransactionDescriptionOrdinary) {
            return ((TransactionDescriptionOrdinary) txDesc).getStoragePhase();
        } else if (txDesc instanceof TransactionDescriptionStorage) {
            return ((TransactionDescriptionStorage) txDesc).getStoragePhase();
        } else if (txDesc instanceof TransactionDescriptionSplitPrepare) {
            return ((TransactionDescriptionSplitPrepare) txDesc).getStoragePhase();
        } else if (txDesc instanceof TransactionDescriptionTickTock) {
            return ((TransactionDescriptionTickTock) txDesc).getStoragePhase();
        } else if (txDesc instanceof TransactionDescriptionMergeInstall) {
            return ((TransactionDescriptionMergeInstall) txDesc).getStoragePhase();
        } else if (txDesc instanceof TransactionDescriptionMergePrepare) {
            return ((TransactionDescriptionMergePrepare) txDesc).getStoragePhase();
        }
        return null;
    }


    private ComputePhaseVM getComputePhaseVm(TransactionDescription txDesc) {
        if (txDesc instanceof TransactionDescriptionOrdinary) {
            ComputePhase computePhase = ((TransactionDescriptionOrdinary) txDesc).getComputePhase();
            if (computePhase instanceof ComputePhaseVM) {
                return ((ComputePhaseVM) computePhase);
            }

        } else if (txDesc instanceof TransactionDescriptionSplitPrepare) {
            ComputePhase computePhase = ((TransactionDescriptionSplitPrepare) txDesc).getComputePhase();
            if (computePhase instanceof ComputePhaseVM) {
                return ((ComputePhaseVM) computePhase);
            }
        } else if (txDesc instanceof TransactionDescriptionTickTock) {
            ComputePhase computePhase = ((TransactionDescriptionTickTock) txDesc).getComputePhase();
            if (computePhase instanceof ComputePhaseVM) {
                return ((ComputePhaseVM) computePhase);
            }
        } else if (txDesc instanceof TransactionDescriptionMergeInstall) {
            ComputePhase computePhase = ((TransactionDescriptionMergeInstall) txDesc).getComputePhase();
            if (computePhase instanceof ComputePhaseVM) {
                return ((ComputePhaseVM) computePhase);
            }
        }
        return null;
    }

    private ComputeSkipReason getComputePhaseSkipReason(TransactionDescription txDesc) {
        if (txDesc instanceof TransactionDescriptionOrdinary) {
            ComputePhase computePhase = ((TransactionDescriptionOrdinary) txDesc).getComputePhase();
            if (computePhase instanceof ComputeSkipReason) {
                return ((ComputeSkipReason) computePhase);
            }

        } else if (txDesc instanceof TransactionDescriptionSplitPrepare) {
            ComputePhase computePhase = ((TransactionDescriptionSplitPrepare) txDesc).getComputePhase();
            if (computePhase instanceof ComputeSkipReason) {
                return ((ComputeSkipReason) computePhase);
            }
        } else if (txDesc instanceof TransactionDescriptionTickTock) {
            ComputePhase computePhase = ((TransactionDescriptionTickTock) txDesc).getComputePhase();
            if (computePhase instanceof ComputeSkipReason) {
                return ((ComputeSkipReason) computePhase);
            }
        } else if (txDesc instanceof TransactionDescriptionMergeInstall) {
            ComputePhase computePhase = ((TransactionDescriptionMergeInstall) txDesc).getComputePhase();
            if (computePhase instanceof ComputeSkipReason) {
                return ((ComputeSkipReason) computePhase);
            }
        }
        return null;
    }

    private ActionPhase getActionPhase(TransactionDescription txDesc) {
        if (txDesc instanceof TransactionDescriptionOrdinary) {
            return ((TransactionDescriptionOrdinary) txDesc).getActionPhase();
        } else if (txDesc instanceof TransactionDescriptionSplitPrepare) {
            return ((TransactionDescriptionSplitPrepare) txDesc).getActionPhase();
        } else if (txDesc instanceof TransactionDescriptionTickTock) {
            return ((TransactionDescriptionTickTock) txDesc).getActionPhase();
        } else if (txDesc instanceof TransactionDescriptionMergeInstall) {
            return ((TransactionDescriptionMergeInstall) txDesc).getActionPhase();
        }
        return null;
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


    /**
     * Print txs data without header, footer, balance and block.
     */

    public void printTransactionInfo() {
        printTransactionInfo(false, false);
    }

    /**
     * Print txs data without header, footer and balance, with block only.
     *
     * @param block String
     */
    public void printTransactionInfo(String block) {
        printTransactionInfo(false, false, "", block);
    }

    /**
     * Print txs data.
     *
     * @param withHeader boolean
     * @param withFooter boolean
     * @param balance    String
     * @param block      String
     */
    public void printTransactionInfo(boolean withHeader, boolean withFooter, String balance, String block) {
        TransactionPrintInfo txFees = getTransactionPrintInfo();

        if (withHeader) {
            if (StringUtils.isNotEmpty(balance)) {
                printTxHeader(" (initial balance " + balance + ")");
            } else {
                printTxHeader("");
            }
        }
        printTxData(txFees, block);

        if (withFooter) {
            printTxFooter();
        }
    }

    public void printTransactionInfo(boolean withHeader, boolean withFooter, String balance) {
        TransactionPrintInfo txFees = getTransactionPrintInfo();

        if (withHeader) {
            printTxHeaderWithoutBlock(" (initial balance " + balance + ")");
        }
        printTxData(txFees);

        if (withFooter) {
            printTxFooterWithoutBlock();
        }
    }

    public void printTransactionInfo(boolean withHeader, boolean withFooter) {
        TransactionPrintInfo txFees = getTransactionPrintInfo();

        if (withHeader) {
            printTxHeaderWithoutBlock("");
        }
        printTxData(txFees);

        if (withFooter) {
            printTxFooterWithoutBlock();
        }
    }

    private BigInteger getOperationFromBody(Cell body) {
        if (nonNull(body) && body.getBits().getUsedBits() >= 32) {
            return CellSlice.beginParse(body).preloadInt(32);
        } else {
            return BigInteger.ONE.negate();
        }
    }

    private String getCommentFromBody(Cell body) {
        try {
            if (nonNull(body) && body.getBits().getUsedBits() >= 32) {
                String comment = CellSlice.beginParse(body).skipBits(32).loadSnakeString();
                if ((StringUtils.contains(comment, '\0')) || (StringUtils.contains(comment, '\1'))
                        || (StringUtils.contains(comment, '\2')) || (StringUtils.contains(comment, '\3'))
                        || (StringUtils.contains(comment, '\uFFFD'))) {
                    return "";
                } else {
                    if (comment.length() > 26) {
                        return comment.substring(0, 26);
                    } else {
                        return comment;
                    }
                }
            } else {
                return "";
            }
        } catch (Exception e) {
            return "error";
        }
    }

    public List<MessagePrintInfo> getAllMessagePrintInfo() {
        List<MessagePrintInfo> messagePrintInfo = new ArrayList<>();
        Transaction tx = this;

        BigInteger op;
        String comment;

        if (nonNull(tx.getInOut())) {

            Message inMsg = tx.getInOut().getIn();
            if (nonNull(inMsg)) {
                Cell body = inMsg.getBody();

                op = getOperationFromBody(body);
                comment = getCommentFromBody(body);

                if (inMsg.getInfo() instanceof InternalMessageInfo) {
                    InternalMessageInfo msgInfo = ((InternalMessageInfo) inMsg.getInfo());
                    MessagePrintInfo msgPrintInfo =
                            MessagePrintInfo.builder()
                                    .direction("in")
                                    .type(formatMsgType(inMsg.getInfo().getClass().getSimpleName()))
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
                                    .src(msgInfo.getSrcAddr().toAddress().toRaw())
                                    .dst(msgInfo.getDstAddr().toAddress().toRaw())
                                    .comment(comment)
                                    .build();
                    messagePrintInfo.add(msgPrintInfo);
                }
                if (inMsg.getInfo() instanceof ExternalMessageOutInfo) {
                    ExternalMessageOutInfo msgInfo = ((ExternalMessageOutInfo) inMsg.getInfo());
                    MessagePrintInfo msgPrintInfo =
                            MessagePrintInfo.builder()
                                    .direction("in")
                                    .type(formatMsgType(inMsg.getInfo().getClass().getSimpleName()))
                                    .op(
                                            (isNull(op))
                                                    ? "N/A"
                                                    : (op.compareTo(BigInteger.ONE.negate()) != 0)
                                                    ? op.toString(16)
                                                    : "no body")
                                    .createdLt(msgInfo.getCreatedLt())
                                    .createdAt(BigInteger.valueOf(msgInfo.getCreatedAt()))
                                    .src(msgInfo.getSrcAddr().toAddress().toRaw())
                                    .dst(msgInfo.getDstAddr().toString())
                                    .comment(comment)
                                    .build();
                    messagePrintInfo.add(msgPrintInfo);
                }
                if (inMsg.getInfo() instanceof ExternalMessageInInfo) {
                    ExternalMessageInInfo msgInfo = ((ExternalMessageInInfo) inMsg.getInfo());
                    MessagePrintInfo msgPrintInfo =
                            MessagePrintInfo.builder()
                                    .direction("in")
                                    .type(formatMsgType(inMsg.getInfo().getClass().getSimpleName()))
                                    .op(
                                            (isNull(op))
                                                    ? "N/A"
                                                    : (op.compareTo(BigInteger.ONE.negate()) != 0)
                                                    ? op.toString(16)
                                                    : "no body")
                                    .importFee(msgInfo.getImportFee())
                                    .src(msgInfo.getSrcAddr().toString())
                                    .dst(msgInfo.getDstAddr().toString())
                                    .comment(comment)
                                    .build();
                    messagePrintInfo.add(msgPrintInfo);
                }
            }

            for (Message outMsg : tx.getInOut().getOutMessages()) {

                Cell body = outMsg.getBody();

                op = getOperationFromBody(body);
                comment = getCommentFromBody(body);

                if (outMsg.getInfo() instanceof InternalMessageInfo) {
                    InternalMessageInfo intMsgInfo = (InternalMessageInfo) outMsg.getInfo();

                    MessagePrintInfo msgPrintInfo =
                            MessagePrintInfo.builder()
                                    .direction("out")
                                    .type(formatMsgType(outMsg.getInfo().getClass().getSimpleName()))
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
                                    .src(intMsgInfo.getSrcAddr().toAddress().toRaw())
                                    .dst(intMsgInfo.getDstAddr().toAddress().toRaw())
                                    .comment(comment)
                                    .build();
                    messagePrintInfo.add(msgPrintInfo);
                }
                if (outMsg.getInfo() instanceof ExternalMessageOutInfo) {
                    ExternalMessageOutInfo outMsgInfo = (ExternalMessageOutInfo) outMsg.getInfo();

                    MessagePrintInfo msgPrintInfo =
                            MessagePrintInfo.builder()
                                    .direction("out")
                                    .type(formatMsgType(outMsg.getInfo().getClass().getSimpleName()))
                                    .op(
                                            (isNull(op))
                                                    ? "N/A"
                                                    : (op.compareTo(BigInteger.ONE.negate()) != 0)
                                                    ? op.toString(16)
                                                    : "no body")
                                    .createdLt(outMsgInfo.getCreatedLt())
                                    .createdAt(BigInteger.valueOf(outMsgInfo.getCreatedAt()))
                                    .src(outMsgInfo.getSrcAddr().toAddress().toRaw())
                                    .dst(outMsgInfo.getDstAddr().toString())
                                    .comment(comment)
                                    .build();
                    messagePrintInfo.add(msgPrintInfo);
                }
                if (outMsg.getInfo() instanceof ExternalMessageInInfo) {
                    ExternalMessageInInfo outMsgInfo = (ExternalMessageInInfo) outMsg.getInfo();

                    MessagePrintInfo msgPrintInfo =
                            MessagePrintInfo.builder()
                                    .direction("out")
                                    .type(formatMsgType(outMsg.getInfo().getClass().getSimpleName()))
                                    .op(
                                            (isNull(op))
                                                    ? "N/A"
                                                    : (op.compareTo(BigInteger.ONE.negate()) != 0)
                                                    ? op.toString(16)
                                                    : "no body")
                                    .importFee(outMsgInfo.getImportFee())
                                    .src(outMsgInfo.getSrcAddr().toString())
                                    .dst(outMsgInfo.getDstAddr().toString())
                                    .comment(comment)
                                    .build();
                    messagePrintInfo.add(msgPrintInfo);
                }
            }
        }
        return messagePrintInfo;
    }

    public void printAllMessages(boolean withHeader) {
        printAllMessages(withHeader, false);
    }

    public void printAllMessages(boolean withHeader, boolean withFooter) {
        List<MessagePrintInfo> msgsPrintInfo = getAllMessagePrintInfo();
        if (msgsPrintInfo.isEmpty()) {
            log.info("No messages");
            return;
        }

        if (withHeader) {
            MessagePrintInfo.printMessageInfoHeader();
        }

        for (MessagePrintInfo msgPrintInfo : msgsPrintInfo) {
            msgPrintInfo.printMessageInfo();
        }
        if (withFooter) {
            MessagePrintInfo.printMessageInfoFooter();
        }
    }

    public static void printTxHeader(String balance) {
        log.info("");
        log.info("Transactions" + balance);

        log.info(StringUtils.repeat("-", txHeaderInfoWithBlock.length()));
        log.info(txHeaderInfoWithBlockTop);
        log.info(txHeaderInfoWithBlock);
        log.info(StringUtils.repeat("-", txHeaderInfoWithBlock.length()));
    }

    public static void printTxHeaderWithoutBlock(String balance) {
        log.info("");
        log.info("Transactions" + balance);

        log.info(StringUtils.repeat("-", txHeaderInfoWithoutBlock.length()));
        log.info(txHeaderInfoWithoutBlockTop);
        log.info(txHeaderInfoWithoutBlock);
        log.info(StringUtils.repeat("-", txHeaderInfoWithoutBlock.length()));
    }

    public static void printTxFooter() {
        log.info(StringUtils.repeat("-", txHeaderInfoWithBlock.length()));
    }

    public static void printTxFooterWithoutBlock() {
        log.info(StringUtils.repeat("-", txHeaderInfoWithoutBlock.length()));
    }

    public static void printTxData(TransactionPrintInfo txPrintInfo) {
        String str =
                String.format(
                        "| %-8s| %-10s| %-15s| %-10s| %-13s| %-15s| %-15s| %-13s| %-13s| %-14s| %-8s| %-14s| %-8s| %-9s| %-8s| %-13s| %-13s| %-8s| %-9s| %-14s|",
                        txPrintInfo.getHash().substring(0, 6),
                        Utils.toUTCTimeOnly(txPrintInfo.getNow()),
                        txPrintInfo.getLt(),
                        txPrintInfo.getOp(),
                        txPrintInfo.getType(),
                        Utils.formatNanoValueZero(txPrintInfo.getValueIn()),
                        Utils.formatNanoValueZero(txPrintInfo.getValueOut()),
                        Utils.formatNanoValueZero(txPrintInfo.getTotalFees()),
                        Utils.formatNanoValueZero(txPrintInfo.getStorageFeesCollected()),
                        Utils.formatNanoValueZero(txPrintInfo.getStorageDueFees()),
//                        txPrintInfo.getStorageStatus(),
                        txPrintInfo.getComputeSuccess(),
                        Utils.formatNanoValueZero(txPrintInfo.getComputeGasFees()),
                        txPrintInfo.getComputeVmSteps(),
                        Utils.formatNanoValueZero(txPrintInfo.getComputeExitCode()),
                        txPrintInfo.getActionSuccess(),
                        Utils.formatNanoValueZero(txPrintInfo.getActionTotalFwdFees()),
                        Utils.formatNanoValueZero(txPrintInfo.getActionTotalActionFees()),
                        txPrintInfo.getActionTotalActions(),
                        txPrintInfo.getActionResultCode(),
                        txPrintInfo.getAccount());
        log.info(str);
    }

    public static void printTxData(TransactionPrintInfo txPrintInfo, String block) {
        String str =
                String.format(
                        "| %-8s| %-10s| %-15s| %-10s| %-13s| %-15s| %-15s| %-13s| %-13s| %-14s| %-8s| %-14s| %-8s| %-9s| %-8s| %-13s| %-13s| %-8s| %-9s| %-14s| %-32s|",
                        txPrintInfo.getHash().substring(0, 5),
                        Utils.toUTCTimeOnly(txPrintInfo.getNow()),
                        txPrintInfo.getLt(),
                        txPrintInfo.getOp(),
                        txPrintInfo.getType(),
                        Utils.formatNanoValueZero(txPrintInfo.getValueIn()),
                        Utils.formatNanoValueZero(txPrintInfo.getValueOut()),
                        Utils.formatNanoValueZero(txPrintInfo.getTotalFees()),
                        Utils.formatNanoValueZero(txPrintInfo.getStorageFeesCollected()),
                        Utils.formatNanoValueZero(txPrintInfo.getStorageDueFees()),
//                        txPrintInfo.getStorageStatus(),
                        txPrintInfo.getComputeSuccess(),
                        Utils.formatNanoValueZero(txPrintInfo.getComputeGasFees()),
                        txPrintInfo.getComputeVmSteps(),
                        Utils.formatNanoValueZero(txPrintInfo.getComputeExitCode()),
                        txPrintInfo.getActionSuccess(),
                        Utils.formatNanoValueZero(txPrintInfo.getActionTotalFwdFees()),
                        Utils.formatNanoValueZero(txPrintInfo.getActionTotalActionFees()),
                        txPrintInfo.getActionTotalActions(),
                        txPrintInfo.getActionResultCode(),
                        txPrintInfo.getAccount(),
                        StringUtils.isEmpty(block) ? "N/A" : block);
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
