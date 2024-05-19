package org.ton.java.smartcontract.highload;

import com.iwebpp.crypto.TweetNaclFast;
import lombok.Builder;
import lombok.Getter;
import org.ton.java.address.Address;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.smartcontract.types.HighloadV3BatchItem;
import org.ton.java.smartcontract.types.HighloadV3Config;
import org.ton.java.smartcontract.types.WalletCodes;
import org.ton.java.smartcontract.wallet.Contract;
import org.ton.java.tlb.types.*;
import org.ton.java.tonlib.Tonlib;
import org.ton.java.tonlib.types.ExtMessageInfo;
import org.ton.java.tonlib.types.RunResult;
import org.ton.java.tonlib.types.TvmStackEntryNumber;
import org.ton.java.utils.Utils;

import java.math.BigInteger;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

@Builder
@Getter
public class HighloadWalletV3 implements Contract {

    TweetNaclFast.Signature.KeyPair keyPair;
    long walletId;
    long timeout;

    /**
     * interface to <a href="https://github.com/ton-blockchain/highload-wallet-contract-v3/blob/main/contracts/highload-wallet-v3.func">highload-v3 smart-contract</a>
     * <p>
     * Options - mandatory -  highloadQueryId, walletId, publicKey
     */
//    public HighloadWalletV3(Options options) {
//        options.code = CellBuilder.beginCell().fromBoc(WalletCodes.highloadV3.getValue()).endCell();
//    }

    public static class HighloadWalletV3Builder {
        HighloadWalletV3Builder() {
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
        return "highload-v3";
    }

    /**
     * initial contract storage
     * storage$_ public_key:bits256 subwallet_id:uint32 old_queries:(HashmapE 14 ^Cell)
     * queries:(HashmapE 14 ^Cell) last_clean_time:uint64 timeout:uint22
     * = Storage;
     *
     * @return Cell
     */
    @Override
    public Cell createDataCell() {
        CellBuilder cell = CellBuilder.beginCell();

        cell.storeBytes(keyPair.getPublicKey());
        cell.storeUint(walletId, 32);
        cell.storeBit(false); // old queries
        cell.storeBit(false); // queries
        cell.storeUint(0, 64); // last clean time
        cell.storeUint(timeout, 22); //time out

        return cell.endCell();
    }

    @Override
    public Cell createCodeCell() {
        return CellBuilder.beginCell().
                fromBoc(WalletCodes.highloadV3.getValue()).
                endCell();
    }

    public String getPublicKey(Tonlib tonlib) {

        Address myAddress = this.getAddress();
        RunResult result = tonlib.runMethod(myAddress, "get_public_key");

        if (result.getExit_code() != 0) {
            throw new Error("method get_public_key, returned an exit code " + result.getExit_code());
        }

        TvmStackEntryNumber publicKeyNumber = (TvmStackEntryNumber) result.getStack().get(0);
        return publicKeyNumber.getNumber().toString(16);
    }

    /**
     * _ {n:#}  subwallet_id:uint32
     * message_to_send:^Cell
     * send_mode:uint8
     * query_id:QueryId
     * created_at:uint64
     * timeout:uint22 = MsgInner;
     *
     * @return Cell
     */
    public Cell createMessageInner(HighloadV3Config highloadConfig) { // todo rename
        return CellBuilder.beginCell()
                .storeUint(walletId, 32)
                .storeRef(highloadConfig.getBody())
                .storeUint(highloadConfig.getMode(), 8)
                .storeUint(highloadConfig.getQueryId(), 23)
                .storeUint(highloadConfig.getCreatedAt(), 64)
                .storeUint(highloadConfig.getTimeOut(), 22)
                .endCell();
    }

    /**
     * @param tonlib         Tonlib
     * @param highloadConfig HighloadV3Config
     */
    public ExtMessageInfo sendTonCoins(Tonlib tonlib, HighloadV3Config highloadConfig) {
        Address ownAddress = getAddress();

        Cell innerMsg = createMessageInner(highloadConfig);

        Message externalMessage = Message.builder()
                .info(ExternalMessageInfo.builder()
                        .dstAddr(MsgAddressIntStd.builder()
                                .workchainId(ownAddress.wc)
                                .address(ownAddress.toBigInteger())
                                .build())
                        .build())
                .body(CellBuilder.beginCell()
                        .storeBytes(Utils.signData(keyPair.getPublicKey(), keyPair.getSecretKey(), innerMsg.hash()))
                        .storeRef(innerMsg)
                        .endCell())
                .build();

        return tonlib.sendRawMessage(externalMessage.toCell().toBase64());
    }

    public ExtMessageInfo deploy(Tonlib tonlib, HighloadV3Config highloadConfig) {
        Address ownAddress = getAddress();

        if (isNull(highloadConfig.getBody())) {
            //dummy deploy msg
            highloadConfig.setBody(MessageRelaxed.builder()
                    .info(InternalMessageInfoRelaxed.builder()
                            .dstAddr(MsgAddressIntStd.builder()
                                    .workchainId(ownAddress.wc)
                                    .address(ownAddress.toBigInteger())
                                    .build())
                            .createdAt(highloadConfig.getCreatedAt())
                            .build())
                    .build().toCell());
        }

        Cell innerMsg = createMessageInner(highloadConfig);

        Message externalMessage = Message.builder()
                .info(ExternalMessageInfo.builder()
                        .dstAddr(MsgAddressIntStd.builder()
                                .workchainId(ownAddress.wc)
                                .address(ownAddress.toBigInteger())
                                .build())
                        .build())
                .init(getStateInit())
                .body(CellBuilder.beginCell()
                        .storeBytes(Utils.signData(keyPair.getPublicKey(), keyPair.getSecretKey(), innerMsg.hash()))
                        .storeRef(innerMsg)
                        .endCell())
                .build();

        return tonlib.sendRawMessage(externalMessage.toCell().toBase64());
    }

    public static Cell createTextMessageBody(String text) {
        CellBuilder cb = CellBuilder.beginCell();
        cb.storeUint(0, 32);
        cb.storeSnakeString(text);
        return cb.endCell();
    }

    public static Cell createJettonTransferBody(long queryId, BigInteger amount, Address destination, Address responseDestination, Cell customPayload, BigInteger forwardAmount, Cell forwardPayload) {
        CellBuilder cb = CellBuilder.beginCell();
        cb.storeUint(0x0f8a7ea5, 32);
        cb.storeUint(queryId, 64);
        cb.storeCoins(amount);
        cb.storeAddress(destination);
        cb.storeAddress(nonNull(responseDestination) ? responseDestination : destination);
        cb.storeRefMaybe(customPayload);
        cb.storeCoins(forwardAmount);
        cb.storeRefMaybe(forwardPayload);
        return cb.endCell();
    }

    public static Cell createInternalTransferBody(HighloadV3BatchItem[] items, long queryId) {
        Cell prev = CellBuilder.beginCell().endCell();
        for (int i = items.length - 1; i >= 0; i--) {
            CellBuilder cb = CellBuilder.beginCell();
            cb.storeRef(prev);
            cb.storeUint(0x0ec3c86d, 32);
            cb.storeUint(items[i].getMode(), 8);
            cb.storeRef(items[i].getMessage());
            prev = cb.endCell();
        }

        CellBuilder body = CellBuilder.beginCell();
        body.storeUint(0xae42e5a4, 32);
        body.storeUint(queryId, 64);
        body.storeRef(prev);
        return body.endCell();
    }

    public Cell createMessageToSend(Address destAddress, double amount, long createdAt, TweetNaclFast.Signature.KeyPair keyPair) {

        CommonMsgInfoRelaxed internalMsgInfo = InternalMessageInfoRelaxed.builder()
                .srcAddr(MsgAddressExtNone.builder().build())
                .dstAddr(MsgAddressIntStd.builder()
                        .workchainId(destAddress.wc)
                        .address(destAddress.toBigInteger())
                        .build())
                .value(CurrencyCollection.builder().coins(Utils.toNano(amount)).build())
                .createdAt(createdAt)
                .build();

        Cell innerMsg = internalMsgInfo.toCell();

        return MessageRelaxed.builder()
                .info(internalMsgInfo)
                .body(CellBuilder.beginCell()
                        .storeBytes(Utils.signData(keyPair.getPublicKey(), keyPair.getSecretKey(), innerMsg.hash()))
                        .storeRef(innerMsg)
                        .endCell())
                .build().toCell();
    }

    public Cell createMessagesToSend(BigInteger totalAmount, Cell bulkMessages, long createdAt) {
        Address ownAddress = getAddress();
        return MessageRelaxed.builder()
                .info(InternalMessageInfoRelaxed.builder()
                        .dstAddr(MsgAddressIntStd.builder()
                                .workchainId(ownAddress.wc)
                                .address(ownAddress.toBigInteger())
                                .build())
                        .value(CurrencyCollection.builder().coins(totalAmount).build())
                        .createdAt(createdAt)
                        .build())
                .body(bulkMessages)
                .build().toCell();
    }
}
