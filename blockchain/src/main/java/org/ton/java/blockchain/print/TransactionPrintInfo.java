package org.ton.java.blockchain.print;

import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellSlice;
import org.ton.java.tlb.types.*;
import org.ton.java.utils.Utils;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

@Builder
@Data
@Slf4j
public class TransactionPrintInfo {
    String hash; // in_msg.hash
    long now;
    String op;
    String type;
    BigInteger valueIn;
    BigInteger valueOut;

    BigInteger totalFees;
    BigInteger storageFeesCollected;
    BigInteger storageDueFees;
    String storageStatus;
    String computeSuccess;
    BigInteger computeGasFees;
    BigInteger computeGasUsed;
    long computeVmSteps;
    BigInteger computeExitCode;
    String actionSuccess;
    BigInteger actionTotalFwdFees;
    BigInteger actionTotalActionFees;
    long actionTotalActions;
    long actionResultCode;
    BigInteger inForwardFee;
    BigInteger outForwardFee;
    long exitCode;
    long actionCode;
    long outMsgs;
    BigInteger lt;
    String account;
    BigInteger balance;
    Transaction tx;

    private static String txHeaderInfoWithBlockTop = "|                                                                                                                  |         Storage Phase        |                  Compute Phase               |                          Action Phase                      |                                                 |";
    private static String txHeaderInfoWithBlock = "| txHash  | timestamp | lt             | op        | type         | valueIn        | valueOut       | totalFees    | fees         | dueFees       | success | gasFees       | vmSteps | exitCode | success | fordwardFees | actionFees   | actions | exitCode | account       | block                           |";

    private static String txHeaderInfoWithoutBlockTop = "|                                                                                                                  |         Storage Phase        |                  Compute Phase               |                          Action Phase                      |               | ";
    private static String txHeaderInfoWithoutBlock = "| txHash  | timestamp | lt             | op        | type         | valueIn        | valueOut       | totalFees    | fees         | dueFees       | success | gasFees       | vmSteps | exitCode | success | fordwardFees | actionFees   | actions | exitCode | account       |";


    public static TransactionPrintInfo getTransactionPrintInfo(Transaction tx) {

        BigInteger totalFees = tx.getTotalFees().getCoins();

        StoragePhase storagePhase = getStoragePhase(tx.getDescription());
        ComputePhaseVM computePhase = getComputePhaseVm(tx.getDescription());
        ActionPhase actionPhase = getActionPhase(tx.getDescription());

        BigInteger storageFeesCollected = nonNull(storagePhase) ? storagePhase.getStorageFeesCollected() : null;
        BigInteger storageDueFees = nonNull(storagePhase) ? storagePhase.getStorageFeesDue() : null;
        String storageStatus = nonNull(storagePhase) ? storagePhase.getStatusChange().getType() : "N/A";

        BigInteger computeGasFees = nonNull(computePhase) ? computePhase.getGasFees() : null;
        long computeVmSteps = nonNull(computePhase) ? computePhase.getDetails().getVMSteps() : 0;
        String computeSuccess = nonNull(computePhase) ? computePhase.isSuccess() ? "yes" : "no" :
                nonNull(getComputePhaseSkipReason(tx.getDescription())) ? "skipped" : "N/A";

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

    private static StoragePhase getStoragePhase(TransactionDescription txDesc) {
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


    private static ComputePhaseVM getComputePhaseVm(TransactionDescription txDesc) {
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

    private static ComputeSkipReason getComputePhaseSkipReason(TransactionDescription txDesc) {
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

    private static ActionPhase getActionPhase(TransactionDescription txDesc) {
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

    private static long getTotalActions(TransactionDescription txDesc) {
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

    private static long getActionCode(TransactionDescription txDesc) {
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

    private static long getExitCode(TransactionDescription txDesc) {
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

    public static void printTransactionInfo(Transaction transaction) {
        printTransactionInfo(transaction, false, false);
    }

    /**
     * Print txs data without header, footer and balance, with block only.
     *
     * @param block String
     */
    public void printTransactionInfo(Transaction transaction, String block) {
        printTransactionInfo(transaction, false, false, "", block);
    }

    /**
     * Print txs data.
     *
     * @param withHeader boolean
     * @param withFooter boolean
     * @param balance    String
     * @param block      String
     */
    public static void printTransactionInfo(Transaction transaction, boolean withHeader, boolean withFooter, String balance, String block) {
        TransactionPrintInfo txFees = getTransactionPrintInfo(transaction);

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

    public static void printTransactionInfo(Transaction transaction, boolean withHeader, boolean withFooter, String balance) {
        TransactionPrintInfo txFees = getTransactionPrintInfo(transaction);

        if (withHeader) {
            printTxHeaderWithoutBlock(" (initial balance " + balance + ")");
        }
        printTxData(txFees);

        if (withFooter) {
            printTxFooterWithoutBlock();
        }
    }

    public static void printTransactionInfo(Transaction transaction, boolean withHeader, boolean withFooter) {
        TransactionPrintInfo txFees = getTransactionPrintInfo(transaction);

        if (withHeader) {
            printTxHeaderWithoutBlock("");
        }
        printTxData(txFees);

        if (withFooter) {
            printTxFooterWithoutBlock();
        }
    }

    private static BigInteger getOperationFromBody(Cell body) {
        if (nonNull(body) && body.getBits().getUsedBits() >= 32) {
            return CellSlice.beginParse(body).preloadInt(32);
        } else {
            return BigInteger.ONE.negate();
        }
    }

    private static String getCommentFromBody(Cell body) {
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

    public static List<MessagePrintInfo> getAllMessagePrintInfo(Transaction tx) {
        List<MessagePrintInfo> messagePrintInfo = new ArrayList<>();

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

    public static void printAllMessages(Transaction transaction, boolean withHeader) {
        printAllMessages(transaction, withHeader, false);
    }

    public static void printAllMessages(Transaction transaction, boolean withHeader, boolean withFooter) {
        List<MessagePrintInfo> msgsPrintInfo = getAllMessagePrintInfo(transaction);
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

    private static String formatMsgType(String fullMsgType) {
        if (fullMsgType.equals("InternalMessageInfo")) {
            return "internal-in";
        } else if (fullMsgType.equals("ExternalMessageOutInfo")) {
            return "external-out";
        } else {
            return "external-in";
        }
    }
}
