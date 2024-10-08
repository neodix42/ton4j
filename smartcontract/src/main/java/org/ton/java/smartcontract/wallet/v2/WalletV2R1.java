package org.ton.java.smartcontract.wallet.v2;


import com.iwebpp.crypto.TweetNaclFast;
import lombok.Builder;
import lombok.Getter;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.smartcontract.types.WalletCodes;
import org.ton.java.smartcontract.types.WalletV2R1Config;
import org.ton.java.smartcontract.utils.MsgUtils;
import org.ton.java.smartcontract.wallet.Contract;
import org.ton.java.tlb.types.CurrencyCollection;
import org.ton.java.tlb.types.ExternalMessageInfo;
import org.ton.java.tlb.types.InternalMessageInfo;
import org.ton.java.tlb.types.Message;
import org.ton.java.tonlib.Tonlib;
import org.ton.java.tonlib.types.ExtMessageInfo;
import org.ton.java.utils.Utils;

import java.math.BigInteger;
import java.time.Instant;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;


@Builder
@Getter
public class WalletV2R1 implements Contract {

    public TweetNaclFast.Signature.KeyPair keyPair;
    long initialSeqno;

    public static class WalletV2R1Builder {
    }

    public static WalletV2R1Builder builder() {
        return new CustomWalletV2R1Builder();
    }

    private static class CustomWalletV2R1Builder extends WalletV2R1Builder {
        @Override
        public WalletV2R1 build() {
            if (isNull(super.keyPair)) {
                super.keyPair = Utils.generateSignatureKeyPair();
            }
            return super.build();
        }
    }

    private Tonlib tonlib;
    private long wc;

    @Override
    public Tonlib getTonlib() {
        return tonlib;
    }

    @Override
    public long getWorkchain() {
        return wc;
    }

    @Override
    public String getName() {
        return "V2R1";
    }

    @Override
    public Cell createCodeCell() {
        return CellBuilder.beginCell().
                fromBoc(WalletCodes.V2R1.getValue()).
                endCell();
    }

    @Override
    public Cell createDataCell() {
        CellBuilder cell = CellBuilder.beginCell();
        cell.storeUint(initialSeqno, 32); // seqno
        cell.storeBytes(keyPair.getPublicKey());
        return cell.endCell();
    }

    public Cell createDeployMessage() {
        return CellBuilder.beginCell()
                .storeUint(0, 32)
                .storeInt(-1, 32)
                .endCell();
    }

    /**
     * Creates message payload with seqno and validUntil fields
     */
    public Cell createTransferBody(WalletV2R1Config config) {

        CellBuilder message = CellBuilder.beginCell();
        message.storeUint(BigInteger.valueOf(config.getSeqno()), 32);

        message.storeUint((config.getValidUntil() == 0) ? Instant.now().getEpochSecond() + 60 : config.getValidUntil(), 32);

        if (nonNull(config.getDestination1())) {
            Message order = MsgUtils.createInternalMessage(config.getDestination1(), config.getAmount1(), config.getStateInit(), config.getBody(), config.getBounce());
            message.storeUint((config.getMode() == 0) ? 3 : config.getMode(), 8);
            message.storeRef(order.toCell());
        }
        if (nonNull(config.getDestination2())) {
            Message order = MsgUtils.createInternalMessage(config.getDestination2(), config.getAmount2(), config.getStateInit(), config.getBody(), config.getBounce());
            message.storeUint((config.getMode() == 0) ? 3 : config.getMode(), 8);
            message.storeRef(order.toCell());
        }
        if (nonNull(config.getDestination3())) {
            Message order = MsgUtils.createInternalMessage(config.getDestination3(), config.getAmount3(), config.getStateInit(), config.getBody(), config.getBounce());
            message.storeUint((config.getMode() == 0) ? 3 : config.getMode(), 8);
            message.storeRef(order.toCell());
        }
        if (nonNull(config.getDestination4())) {
            Message order = MsgUtils.createInternalMessage(config.getDestination4(), config.getAmount3(), config.getStateInit(), config.getBody(), config.getBounce());
            message.storeUint((config.getMode() == 0) ? 3 : config.getMode(), 8);
            message.storeRef(order.toCell());
        }

        return message.endCell();
    }

    public ExtMessageInfo send(WalletV2R1Config config) {

        return tonlib.sendRawMessage(prepareExternalMsg(config).toCell().toBase64());
    }

    public Message prepareExternalMsg(WalletV2R1Config config) {
        Cell body = createTransferBody(config);
        return MsgUtils.createExternalMessageWithSignedBody(keyPair, getAddress(), null, body);
    }


    public ExtMessageInfo deploy() {
        return tonlib.sendRawMessage(prepareDeployMsg().toCell().toBase64());
    }

    public Message prepareDeployMsg() {
        Cell body = createDeployMessage();

        return Message.builder()
                .info(ExternalMessageInfo.builder()
                        .dstAddr(getAddressIntStd())
                        .build())
                .init(getStateInit())
                .body(CellBuilder.beginCell()
                        .storeBytes(Utils.signData(keyPair.getPublicKey(), keyPair.getSecretKey(), body.hash()))
                        .storeCell(body)
                        .endCell())
                .build();
    }

    public Cell createInternalSignedBody(WalletV2R1Config config) {
        Cell body = createTransferBody(config);
        byte[] signature = Utils.signData(keyPair.getPublicKey(), keyPair.getSecretKey(), body.hash());

        return CellBuilder.beginCell().storeCell(body).storeBytes(signature).endCell();
    }

    public Message prepareInternalMsg(WalletV2R1Config config) {
        Cell body = createInternalSignedBody(config);

        return Message.builder()
                .info(InternalMessageInfo.builder()
                        .srcAddr(getAddressIntStd())
                        .dstAddr(getAddressIntStd())
                        .value(CurrencyCollection.builder().coins(config.getAmount()).build())
                        .build())
                .body(body)
                .build();
    }
}
