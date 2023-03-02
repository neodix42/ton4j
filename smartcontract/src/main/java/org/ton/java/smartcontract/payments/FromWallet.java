package org.ton.java.smartcontract.payments;

import org.ton.java.address.Address;
import org.ton.java.cell.Cell;
import org.ton.java.smartcontract.types.ChannelState;
import org.ton.java.smartcontract.types.ExternalMessage;
import org.ton.java.smartcontract.wallet.Options;
import org.ton.java.smartcontract.wallet.WalletContract;
import org.ton.java.tonlib.Tonlib;

import java.math.BigInteger;

import static java.util.Objects.nonNull;

public class FromWallet extends PaymentChannel {

    Options options;
    WalletContract wallet;
    byte[] secretKey;

    Tonlib tonlib;

    ExternalMessage extMsg;

    public FromWallet(Tonlib tonlib, WalletContract wallet, byte[] secretKey, Options options) {
        super(options);
        this.options = options;
        this.tonlib = tonlib;
        this.wallet = wallet;
        this.secretKey = secretKey;
    }

    public FromWallet deploy(BigInteger amount) {
        transfer(null, true, amount);
        return this;
    }

    public FromWallet topUp(BigInteger balanceA, BigInteger balanceB, BigInteger amount) {
        transfer(this.createTopUpBalance(balanceA, balanceB), amount);
        return this;
    }

    public FromWallet init(BigInteger balanceA, BigInteger balanceB, BigInteger amount) {
        transfer(this.createInitChannel(balanceA, balanceB).getCell(), amount);
        return this;
    }


    public FromWallet send() {
        if (nonNull(extMsg)) {
            tonlib.sendRawMessage(extMsg.message.toBocBase64(false));
        } else {
            throw new Error("cannot send empty external message");
        }
        return this;
    }

    public FromWallet estimateFee() {
        if (nonNull(extMsg)) {
            tonlib.estimateFees(extMsg.address.toString(), extMsg.message.toBocBase64(false), extMsg.code.toBocBase64(false), extMsg.data.toBocBase64(false), false);
        } else {
            throw new Error("cannot send empty external message");
        }
        return this;
    }

    public FromWallet close(ChannelState channelState, byte[] hisSignature, BigInteger amount) {
        transfer(this.createCooperativeCloseChannel(hisSignature, channelState).getCell(), amount);
        return this;
    }

    public FromWallet commit(byte[] hisSignature, BigInteger seqnoA, BigInteger seqnoB, BigInteger amount) {
        transfer(this.createCooperativeCommit(hisSignature, seqnoA, seqnoB).getCell(), amount);
        return this;
    }

    public FromWallet startUncooperativeClose(Cell signedSemiChannelStateA, Cell signedSemiChannelStateB, BigInteger amount) {
        transfer(this.createStartUncooperativeClose(signedSemiChannelStateA, signedSemiChannelStateB).getCell(), amount);
        return this;
    }

    public FromWallet challengeQuarantinedState(Cell signedSemiChannelStateA, Cell signedSemiChannelStateB, BigInteger amount) {
        transfer(this.createChallengeQuarantinedState(signedSemiChannelStateA, signedSemiChannelStateB).getCell(), amount);
        return this;
    }

    public FromWallet settleConditionals(Cell conditionalsToSettle, BigInteger amount) {
        transfer(this.createSettleConditionals(conditionalsToSettle).getCell(), amount);
        return this;
    }

    public FromWallet finishUncooperativeClose(BigInteger amount) {
        transfer(this.createFinishUncooperativeClose(), amount);
        return this;
    }

    private void transfer(Cell payload, boolean needStateInit, BigInteger amount) {
        extMsg = createExtMsg(payload, needStateInit, amount);
    }

    private void transfer(Cell payload, BigInteger amount) {
        extMsg = createExtMsg(payload, false, amount);
    }

    public ExternalMessage createExtMsg(Cell payload, boolean needStateInit, BigInteger amount) {

        Cell stateInit = needStateInit ? (this.createStateInit()).stateInit : null;
        Address myAddress = this.getAddress();
        long seqno = wallet.getSeqno(tonlib);

        return wallet.createTransferMessage(
                secretKey,
                myAddress.toString(true, true, true), //to payment channel
                amount,
                seqno,
                payload, // body
                (byte) 3,
                stateInit);
    }
}
