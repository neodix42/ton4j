package org.ton.java.smartcontract.wallet.v2;

import com.iwebpp.crypto.TweetNaclFast;
import lombok.Builder;
import lombok.Getter;
import org.ton.java.address.Address;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.smartcontract.types.WalletCodes;
import org.ton.java.smartcontract.types.WalletV2Config;
import org.ton.java.smartcontract.utils.MsgUtils;
import org.ton.java.smartcontract.wallet.Contract;
import org.ton.java.tlb.types.ExternalMessageInfo;
import org.ton.java.tlb.types.Message;
import org.ton.java.tonlib.Tonlib;
import org.ton.java.tonlib.types.ExtMessageInfo;
import org.ton.java.tonlib.types.RunResult;
import org.ton.java.tonlib.types.TvmStackEntryNumber;
import org.ton.java.utils.Utils;

import java.math.BigInteger;
import java.util.Date;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

@Builder
@Getter
public class WalletV2R2 implements Contract {

    public TweetNaclFast.Signature.KeyPair keyPair;

    long initialSeqno;
    long walletId;
    Address address;


    public static class WalletV2R2Builder {
        WalletV2R2Builder() {
            if (isNull(keyPair)) {
                keyPair = Utils.generateSignatureKeyPair();
            }
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
        return "V2R2";
    }

    @Override
    public Cell createCodeCell() {
        return CellBuilder.beginCell().
                fromBoc(WalletCodes.V2R2.getValue()).
                endCell();
    }

    public String getPublicKey() {

        Address myAddress = this.getAddress();
        RunResult result = tonlib.runMethod(myAddress, "get_public_key");

        if (result.getExit_code() != 0) {
            throw new Error("method get_public_key, returned an exit code " + result.getExit_code());
        }

        TvmStackEntryNumber publicKeyNumber = (TvmStackEntryNumber) result.getStack().get(0);
        return publicKeyNumber.getNumber().toString(16);
    }


    @Override
    public Cell createDataCell() {
        CellBuilder cell = CellBuilder.beginCell();
        cell.storeUint(initialSeqno, 32); // seqno
        cell.storeBytes(keyPair.getPublicKey());
        return cell.endCell();
    }

    public Cell createDeployMessage() {
        CellBuilder message = CellBuilder.beginCell();
        message.storeUint(initialSeqno, 32);
        for (int i = 0; i < 32; i++) { // valid-until
            message.storeBit(true);
        }
        return message.endCell();
    }

    /**
     * Creates message payload with seqno and validUntil fields
     */
    public Cell createTransferBody(WalletV2Config config) {

        CellBuilder message = CellBuilder.beginCell();
        message.storeUint(BigInteger.valueOf(config.getSeqno()), 32);

        Date date = new Date();
        long timestamp = (long) Math.floor(date.getTime() / 1e3);
        message.storeUint(BigInteger.valueOf(timestamp + 60L), 32);

        if (nonNull(config.getDestination1())) {
            Message order = MsgUtils.createInternalMessage(config.getDestination1(), config.getAmount1(), null, null);
            message.storeUint((config.getMode() == 0) ? 3 : config.getMode(), 8);
            message.storeRef(order.toCell());
        }
        if (nonNull(config.getDestination2())) {
            Message order = MsgUtils.createInternalMessage(config.getDestination2(), config.getAmount2(), null, null);
            message.storeUint((config.getMode() == 0) ? 3 : config.getMode(), 8);
            message.storeRef(order.toCell());
        }
        if (nonNull(config.getDestination3())) {
            Message order = MsgUtils.createInternalMessage(config.getDestination3(), config.getAmount3(), null, null);
            message.storeUint((config.getMode() == 0) ? 3 : config.getMode(), 8);
            message.storeRef(order.toCell());
        }
        if (nonNull(config.getDestination4())) {
            Message order = MsgUtils.createInternalMessage(config.getDestination4(), config.getAmount3(), null, null);
            message.storeUint((config.getMode() == 0) ? 3 : config.getMode(), 8);
            message.storeRef(order.toCell());
        }

        return message.endCell();
    }

    public ExtMessageInfo deploy() {
        Cell body = createDeployMessage();

        Message externalMessage = Message.builder()
                .info(ExternalMessageInfo.builder()
                        .dstAddr(getAddressIntStd())
                        .build())
                .init(getStateInit())
                .body(CellBuilder.beginCell()
                        .storeBytes(Utils.signData(keyPair.getPublicKey(), keyPair.getSecretKey(), body.hash()))
                        .storeCell(body)
                        .endCell())
                .build();

        return tonlib.sendRawMessage(externalMessage.toCell().toBase64());
    }


    public ExtMessageInfo sendTonCoins(WalletV2Config config) {
        Cell body = createTransferBody(config);

        Message externalMessage = Message.builder()
                .info(ExternalMessageInfo.builder()
                        .dstAddr(getAddressIntStd())
                        .build())
                .body(CellBuilder.beginCell()
                        .storeBytes(Utils.signData(keyPair.getPublicKey(), keyPair.getSecretKey(), body.hash()))
                        .storeCell(body)
                        .endCell())
                .build();

        return tonlib.sendRawMessage(externalMessage.toCell().toBase64());
    }

    public Message prepareExternalMsg(WalletV2Config config) {
        Cell body = isNull(config.getBody()) ? createTransferBody(config) : config.getBody();
        return MsgUtils.createExternalMessageWithSignedBody(keyPair, getAddress(), null, body);
    }
}
