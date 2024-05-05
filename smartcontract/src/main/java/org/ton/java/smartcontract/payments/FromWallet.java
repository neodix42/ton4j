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

    public FromWallet deploy(FromWalletConfig config) {
//        transfer(null, true, amount);
        Address ownAddress = getAddress();

        Cell body = createTransferBody(config);

        Message externalMessage = Message.builder()
                .info(ExternalMessageInfo.builder()
                        .srcAddr(MsgAddressExtNone.builder().build())
                        .dstAddr(MsgAddressIntStd.builder()
                                .workchainId(ownAddress.wc)
                                .address(ownAddress.toBigInteger())
                                .build())
                        .build())
                .init(createStateInit())
                .body(CellBuilder.beginCell()
                        .storeBytes(Utils.signData(getOptions().getPublicKey(), options.getSecretKey(), body.hash()))
                        .storeRef(body)
                        .endCell())
                .build();

        tonlib.sendRawMessage(externalMessage.toCell().toBase64());
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

    public FromWallet estimateFee() {
        if (nonNull(extMsg)) {
            tonlib.estimateFees(extMsg.address.toString(), extMsg.message.toBase64(), extMsg.code.toBase64(), extMsg.data.toBase64(), false);
        } else {
            throw new Error("cannot send empty external message");
        }
        return this;
    }

    public void close(ChannelState channelState, byte[] hisSignature, BigInteger amount) {
        transfer(this.createCooperativeCloseChannel(hisSignature, channelState).getCell(), amount);
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

    private void transfer(Cell payload, BigInteger amount) {
//        extMsg = createExtMsg(payload, false, amount);
//        Cell stateInit = needStateInit ? (this.createStateInit()).stateInit : null;

//        long seqno = wallet.getSeqno(tonlib);
//
//        extMsg = wallet.createTransferMessage(
//                secretKey,
//                myAddress.toString(true, true, true), //to payment channel
//                amount,
//                seqno,
//                payload, // body
//                (byte) 3,
//                stateInit);

        Address ownAddress = getAddress();
//        Cell body = payload;
        Cell body = this.createInternalMessage(ownAddress, amount, payload).toCell();


        Message externalMessage = Message.builder()
                .info(ExternalMessageInfo.builder()
                        .srcAddr(MsgAddressExtNone.builder().build())
                        .dstAddr(MsgAddressIntStd.builder()
                                .workchainId(ownAddress.wc)
                                .address(ownAddress.toBigInteger())
                                .build())
                        .build())
                .init(null)
                .body(CellBuilder.beginCell()
                        .storeBytes(Utils.signData(getOptions().getPublicKey(), options.getSecretKey(), body.hash()))
                        .storeRef(body)
                        .endCell())
                .build();

        tonlib.sendRawMessage(externalMessage.toCell().toBase64());
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
