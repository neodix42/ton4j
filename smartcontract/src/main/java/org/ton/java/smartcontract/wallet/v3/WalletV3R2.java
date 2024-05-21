package org.ton.java.smartcontract.wallet.v3;

import com.iwebpp.crypto.TweetNaclFast;
import lombok.Builder;
import lombok.Getter;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.smartcontract.types.WalletCodes;
import org.ton.java.smartcontract.types.WalletV3Config;
import org.ton.java.smartcontract.utils.MsgUtils;
import org.ton.java.smartcontract.wallet.Contract;
import org.ton.java.tlb.types.*;
import org.ton.java.tonlib.Tonlib;
import org.ton.java.tonlib.types.ExtMessageInfo;
import org.ton.java.tonlib.types.RunResult;
import org.ton.java.tonlib.types.TvmStackEntryNumber;
import org.ton.java.utils.Utils;

import java.time.Instant;

import static java.util.Objects.isNull;

@Builder
@Getter
public class WalletV3R2 implements Contract {

    TweetNaclFast.Signature.KeyPair keyPair;
    long initialSeqno;
    long walletId;

    public static class WalletV3R2Builder {
        WalletV3R2Builder() {
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
        return "V3R2";
    }

    @Override
    public Cell createCodeCell() {
        return CellBuilder.beginCell().
                fromBoc(WalletCodes.V3R2.getValue()).
                endCell();
    }

    @Override
    public Cell createDataCell() {
        CellBuilder cell = CellBuilder.beginCell();
        cell.storeUint(initialSeqno, 32); // seqno
        cell.storeUint(walletId, 32);
        cell.storeBytes(keyPair.getPublicKey());
        return cell.endCell();
    }

    public String getPublicKey() {

        RunResult result = tonlib.runMethod(getAddress(), "get_public_key");

        if (result.getExit_code() != 0) {
            throw new Error("method get_public_key, returned an exit code " + result.getExit_code());
        }

        TvmStackEntryNumber publicKeyNumber = (TvmStackEntryNumber) result.getStack().get(0);
        return publicKeyNumber.getNumber().toString(16);
    }


    /**
     * Creates message payload with subwallet-id, valid-until and seqno, equivalent to:
     * <b subwallet-id 32 u, timestamp 32 i, seqno 32 u, b> // signing message
     */
    public Cell createTransferBody(WalletV3Config config) {

        Cell order = Message.builder()
                .info(InternalMessageInfo.builder()
                        .bounce(config.isBounce())
                        .srcAddr(isNull(config.getSource()) ? null :
                                MsgAddressIntStd.builder()
                                        .workchainId(config.getSource().wc)
                                        .address(config.getSource().toBigInteger())
                                        .build())
                        .dstAddr(MsgAddressIntStd.builder()
                                .workchainId(config.getDestination().wc)
                                .address(config.getDestination().toBigInteger())
                                .build())
                        .value(CurrencyCollection.builder().coins(config.getAmount()).build())
                        .build())
                .init(config.getStateInit())
                .body(config.getBody())
                .build().toCell();

        return CellBuilder.beginCell()
                .storeUint(config.getWalletId(), 32)
                .storeUint((config.getValidUntil() == 0) ? Instant.now().getEpochSecond() + 60 : config.getValidUntil(), 32)
                .storeUint(config.getSeqno(), 32)
                .storeUint((config.getMode() == 0) ? 3 : config.getMode(), 8)
                .storeRef(order)
                .endCell();
    }

    public Cell createDeployMessage() {
        CellBuilder message = CellBuilder.beginCell();
        message.storeUint(walletId, 32); //wallet-id

        for (int i = 0; i < 32; i++) { // valid-until
            message.storeBit(true);
        }
        message.storeUint(initialSeqno, 32); //seqno
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

    public ExtMessageInfo sendTonCoins(WalletV3Config config) {

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

    public Message prepareMsg(WalletV3Config config) {
        Cell body = isNull(config.getBody()) ? createTransferBody(config) : config.getBody();
        return MsgUtils.createExternalMessageWithSignedBody(keyPair, getAddress(), null, body);
    }
}
