package org.ton.java.smartcontract.payments;

import org.ton.java.address.Address;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.smartcontract.types.ChannelState;
import org.ton.java.smartcontract.types.ExternalMessage;
import org.ton.java.smartcontract.types.FromWalletConfig;
import org.ton.java.smartcontract.wallet.Contract;
import org.ton.java.smartcontract.wallet.Options;
import org.ton.java.tlb.types.ExternalMessageInfo;
import org.ton.java.tlb.types.Message;
import org.ton.java.tlb.types.MsgAddressExtNone;
import org.ton.java.tlb.types.MsgAddressIntStd;
import org.ton.java.tonlib.Tonlib;
import org.ton.java.tonlib.types.ExtMessageInfo;
import org.ton.java.utils.Utils;

import java.math.BigInteger;

import static java.util.Objects.nonNull;

public class FromWallet extends PaymentChannel {

    Options options;
    Contract wallet;
    byte[] secretKey;

    Tonlib tonlib;

    ExternalMessage extMsg;

    public FromWallet(Tonlib tonlib, Contract wallet, byte[] secretKey, Options options) {
        super(options);
        this.options = options;
        this.tonlib = tonlib;
        this.wallet = wallet;
        this.secretKey = secretKey;
    }

    public ExtMessageInfo deploy(FromWalletConfig config) {
//        transfer(null, true, amount);
        return transfer(null, true, config.getAmount());
    }

    public ExtMessageInfo topUp(BigInteger balanceA, BigInteger balanceB, BigInteger amount) {
        return transfer(this.createTopUpBalance(balanceA, balanceB), amount);

    }

    public ExtMessageInfo init(BigInteger balanceA, BigInteger balanceB, BigInteger amount) {
        return transfer(this.createInitChannel(balanceA, balanceB).getCell(), amount);
    }

    public FromWallet estimateFee() {
        if (nonNull(extMsg)) {
            tonlib.estimateFees(extMsg.address.toString(), extMsg.message.toBase64(), extMsg.code.toBase64(), extMsg.data.toBase64(), false);
        } else {
            throw new Error("cannot send empty external message");
        }
        return this;
    }

    public ExtMessageInfo close(ChannelState channelState, byte[] hisSignature, BigInteger amount) {
        return transfer(this.createCooperativeCloseChannel(hisSignature, channelState).getCell(), amount);
    }

    public void commit(byte[] hisSignature, BigInteger seqnoA, BigInteger seqnoB, BigInteger amount) {
        transfer(this.createCooperativeCommit(hisSignature, seqnoA, seqnoB).getCell(), amount);
    }

    public void startUncooperativeClose(Cell signedSemiChannelStateA, Cell signedSemiChannelStateB, BigInteger amount) {
        transfer(this.createStartUncooperativeClose(signedSemiChannelStateA, signedSemiChannelStateB).getCell(), amount);
    }

    public void challengeQuarantinedState(Cell signedSemiChannelStateA, Cell signedSemiChannelStateB, BigInteger amount) {
        transfer(this.createChallengeQuarantinedState(signedSemiChannelStateA, signedSemiChannelStateB).getCell(), amount);
    }

    public void settleConditionals(Cell conditionalsToSettle, BigInteger amount) {
        transfer(this.createSettleConditionals(conditionalsToSettle).getCell(), amount);
    }

    public void finishUncooperativeClose(BigInteger amount) {
        transfer(this.createFinishUncooperativeClose(), amount);
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

        Address ownAddress = getAddress();
//        Cell body = payload;
        Cell order = this.createInternalMessage(ownAddress, amount, payload).toCell();

        Cell signingMessage = CellBuilder.beginCell()
                .storeUint(0, 32) // seqno
                .storeUint(3, 8) // send mode
                .storeRef(order)
                .endCell();

        Message externalMessage = Message.builder()
                .info(ExternalMessageInfo.builder()
                        .srcAddr(MsgAddressExtNone.builder().build())
                        .dstAddr(MsgAddressIntStd.builder()
                                .workchainId(ownAddress.wc)
                                .address(ownAddress.toBigInteger())
                                .build())
                        .build())
                .init(needStateInit ? (this.createStateInit()) : null)
                .body(CellBuilder.beginCell()
                        .storeBytes(Utils.signData(getOptions().getPublicKey(), options.getSecretKey(), signingMessage.hash()))
                        .storeCell(signingMessage)
                        .endCell())
                .build();

        return tonlib.sendRawMessage(externalMessage.toCell().toBase64());
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
