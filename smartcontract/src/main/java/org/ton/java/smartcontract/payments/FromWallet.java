package org.ton.java.smartcontract.payments;

import org.ton.java.cell.Cell;
import org.ton.java.smartcontract.types.ChannelState;
import org.ton.java.smartcontract.types.ExternalMessage;
import org.ton.java.smartcontract.types.FromWalletConfig;
import org.ton.java.smartcontract.wallet.Contract;
import org.ton.java.tonlib.Tonlib;
import org.ton.java.tonlib.types.ExtMessageInfo;

import java.math.BigInteger;

import static java.util.Objects.nonNull;

public class FromWallet {
    Contract wallet;
    byte[] secretKey;

    Tonlib tonlib;

    ExternalMessage extMsg;

//    public FromWallet(Tonlib tonlib, Contract wallet, byte[] secretKey, Options options) {
//        this.tonlib = tonlib;
//        this.wallet = wallet;
//        this.secretKey = secretKey;
//    }

    public ExtMessageInfo deploy(FromWalletConfig config) {
//        transfer(null, true, amount);
        return transfer(null, true, config.getAmount());
    }

    public ExtMessageInfo topUp(BigInteger balanceA, BigInteger balanceB, BigInteger amount) {
//        return transfer(this.createTopUpBalance(balanceA, balanceB), amount);
        return null;
    }

//    public ExtMessageInfo init(BigInteger balanceA, BigInteger balanceB, BigInteger amount) {
//        return transfer(this.createInitChannel(balanceA, balanceB).getCell(), amount);
//    }

    public FromWallet estimateFee() {
        if (nonNull(extMsg)) {
            tonlib.estimateFees(extMsg.address.toString(), extMsg.message.toBase64(), extMsg.code.toBase64(), extMsg.data.toBase64(), false);
        } else {
            throw new Error("cannot send empty external message");
        }
        return this;
    }

    public ExtMessageInfo close(ChannelState channelState, byte[] hisSignature, BigInteger amount) {
//        return transfer(PaymentsUtils.createCooperativeCloseChannel(hisSignature, channelState).getCell(), amount);
        return null;
    }

    public void commit(byte[] hisSignature, BigInteger seqnoA, BigInteger seqnoB, BigInteger amount) {
//        transfer(this.createCooperativeCommit(hisSignature, seqnoA, seqnoB).getCell(), amount);
    }

    public void startUncooperativeClose(Cell signedSemiChannelStateA, Cell signedSemiChannelStateB, BigInteger amount) {
//        transfer(this.createStartUncooperativeClose(signedSemiChannelStateA, signedSemiChannelStateB).getCell(), amount);
    }

    public void challengeQuarantinedState(Cell signedSemiChannelStateA, Cell signedSemiChannelStateB, BigInteger amount) {
//        transfer(this.createChallengeQuarantinedState(signedSemiChannelStateA, signedSemiChannelStateB).getCell(), amount);
    }

//    public void settleConditionals(Options options, Cell conditionalsToSettle, BigInteger amount) {
//        transfer(PaymentsUtils.createSettleConditionals(options, conditionalsToSettle).getCell(), amount);
//    }

    public void finishUncooperativeClose(BigInteger amount) {
        // transfer(this.createFinishUncooperativeClose(), amount);
        return;
    }

//    private void transfer(Cell payload, boolean needStateInit, BigInteger amount) {
//        extMsg = createExtMsg(payload, needStateInit, amount);
//    }

    private ExtMessageInfo transfer(Cell payload, BigInteger amount) {
        return transfer(payload, false, amount);
    }

    private ExtMessageInfo transfer(Cell payload, boolean needStateInit, BigInteger amount) {
//        Cell stateInit = false ? (this.createStateInit()).stateInit : null;
//        Address myAddress = this.getAddress();
//        long seqno = wallet.getSeqno(tonlib);
//
//        return wallet.createTransferMessage(
//                secretKey,
//                myAddress.toString(true, true, true), //to payment channel
//                amount,
//                seqno,
//                payload, // body
//                (byte) 3,
//                stateInit);
        return null;
    }

//    public ExternalMessage createExtMsg(Cell payload, boolean needStateInit, BigInteger amount) {
//
//        Cell stateInit = needStateInit ? (this.createStateInit()).stateInit : null;
//        Address myAddress = this.getAddress();
//        long seqno = wallet.getSeqno(tonlib);
//
//        return wallet.createTransferMessage(
//                secretKey,
//                myAddress.toString(true, true, true), //to payment channel
//                amount,
//                seqno,
//                payload, // body
//                (byte) 3,
//                stateInit);
//    }
}
