package org.ton.java.smartcontract.highload;

import com.iwebpp.crypto.TweetNaclFast;
import org.ton.java.address.Address;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.CellSlice;
import org.ton.java.smartcontract.types.ExternalMessage;
import org.ton.java.smartcontract.types.HighloadV3BatchItem;
import org.ton.java.smartcontract.types.HighloadV3Config;
import org.ton.java.smartcontract.types.WalletCodes;
import org.ton.java.smartcontract.wallet.Contract;
import org.ton.java.smartcontract.wallet.Options;
import org.ton.java.smartcontract.wallet.WalletContract;
import org.ton.java.tlb.types.*;
import org.ton.java.tonlib.Tonlib;
import org.ton.java.tonlib.types.ExtMessageInfo;
import org.ton.java.tonlib.types.RunResult;
import org.ton.java.tonlib.types.TvmStackEntryNumber;
import org.ton.java.utils.Utils;

import java.math.BigInteger;
import java.time.Instant;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

public class HighloadWalletV3 implements WalletContract {

    Options options;
    Address address;

    /**
     * interface to <a href="https://github.com/ton-blockchain/highload-wallet-contract-v3/blob/main/contracts/highload-wallet-v3.func">highload-v3 smart-contract</a>
     *
     * @param options Options - mandatory -  highloadQueryId, walletId, publicKey
     */
    public HighloadWalletV3(Options options) {
        this.options = options;
        options.code = CellBuilder.beginCell().fromBoc(WalletCodes.highloadV3.getValue()).endCell();
    }

    @Override
    public String getName() {
        return "highload-v3";
    }

    @Override
    public Options getOptions() {
        return options;
    }

    @Override
    public Address getAddress() {
        if (isNull(address)) {
            return (createStateInit()).address;
        }
        return address;
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

        cell.storeBytes(getOptions().getPublicKey());
        cell.storeUint(BigInteger.valueOf(getOptions().getWalletId()), 32);
        cell.storeBit(false); // old queries
        cell.storeBit(false); // queries
        cell.storeUint(0, 64); // last clean time
        cell.storeUint(getOptions().getTimeout(), 22); //time out

        return cell.endCell();
    }

    @Override
    public CellBuilder createSigningMessage(long seqno) {
        CellBuilder message = CellBuilder.beginCell();
        message.storeUint(BigInteger.valueOf(getOptions().walletId), 32);

        message.storeUint(getOptions().getHighloadQueryId(), 64);
        message.storeBit(false);

        return message;
    }


    /**
     * Construct MsgInner according to TL-B:
     * <pre>
     *     _ {n:#}  subwallet_id:uint32 message_to_send:^Cell send_mode:uint8 query_id:QueryId created_at:uint64 timeout:uint22 = MsgInner;`
     * </pre>
     *
     * @param highloadConfig HighloadV3Config
     * @return Cell
     */
    public Cell createSigningMessageInternal(HighloadV3Config highloadConfig) {
        CellBuilder message = CellBuilder.beginCell();
        message.storeUint(BigInteger.valueOf(getOptions().walletId), 32);

        Cell messageHeader = Contract.createInternalMessageHeader(highloadConfig.getDestination(), highloadConfig.getAmount());
        Cell intmsg = Contract.createCommonMsgInfo(messageHeader, null, highloadConfig.getBody());

        message.storeRef(intmsg);
        message.storeUint(highloadConfig.getMode(), 8);
        message.storeUint(highloadConfig.getQueryId(), 23);
        message.storeUint(highloadConfig.getCreatedAt(), 64);
        message.storeUint(getOptions().getTimeout(), 22);

        return message.endCell();
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

    public Cell createSendTonCoinsMessage(byte[] secretKey, HighloadV3Config highloadConfig) {
        Cell signingMessageAll = createSigningMessageInternal(highloadConfig);
        System.out.println("sending external...");
        ExternalMessage msg = createExternalMessage(signingMessageAll, secretKey, highloadConfig.getQueryId() == 0 ? 0 : 1);
        return msg.message;
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
    public Cell createMessageInner(Cell msgToSend, int queryId) {
        CellBuilder message = CellBuilder.beginCell();
        message.storeUint(BigInteger.valueOf(getOptions().walletId), 32);
        message.storeRef(msgToSend); // message_to_send
        message.storeUint(3, 8); // send mode
        message.storeUint(queryId, 23); // query id
        message.storeUint(Instant.now().getEpochSecond() - 10, 64); // created at
        message.storeUint(60 * 60, 22); // timeout
        return message.endCell();
    }

    /**
     * @param tonlib         Tonlib
     * @param secretKey      byte[]
     * @param highloadConfig HighloadV3Config
     */
    public ExtMessageInfo sendTonCoins(Tonlib tonlib, byte[] secretKey, HighloadV3Config highloadConfig) {

        Address ownAddress = getAddress();

        CommonMsgInfo externalMessageInfo = ExternalMessageInfo.builder()
                .srcAddr(MsgAddressExtNone.builder().build())
                .dstAddr(MsgAddressIntStd.builder()
                        .workchainId(ownAddress.wc)
                        .address(ownAddress.toBigInteger())
                        .build())
                .build();

        Cell innerMsg = createMessageInner(highloadConfig.getBody(), highloadConfig.getQueryId());
        byte[] signature = new TweetNaclFast.Signature(getOptions().publicKey, secretKey).detached(innerMsg.hash());


        Cell externalMessageBody = CellBuilder.beginCell()
                .storeBytes(signature)
                .storeRef(innerMsg)
                .endCell();

        Message externalMessage = Message.builder()
                .info(externalMessageInfo)
                .init(null)
                .body(externalMessageBody)
                .build();

//        return tonlib.sendRawMessage(createInitExternalMessage(secretKey).message.toBase64());
        return tonlib.sendRawMessage(externalMessage.toCell().toBase64());
//        return tonlib.sendRawMessage(createSendTonCoinsMessage(secretKey, highloadConfig).toBase64());
    }

    public ExtMessageInfo deploy(Tonlib tonlib, byte[] secretKey, Cell msgToSend) {
        StateInit stateInit = StateInit.builder()
                .code(getOptions().code)
                .data(createDataCell())
                .build();
        Address ownAddress = getAddress();

        CommonMsgInfo externalMessageInfo = ExternalMessageInfo.builder()
                .srcAddr(MsgAddressExtNone.builder().build())
                .dstAddr(MsgAddressIntStd.builder()
                        .workchainId(ownAddress.wc)
                        .address(ownAddress.toBigInteger())
                        .build())
                .importFee(BigInteger.ZERO)
                .build();

        Cell innerMsg = createMessageInner(msgToSend, 0);
        byte[] signature = new TweetNaclFast.Signature(getOptions().publicKey, secretKey).detached(innerMsg.hash());

        Cell externalMessageBody = CellBuilder.beginCell()
                .storeBytes(signature)
                .storeRef(innerMsg)
                .endCell();

        Message externalMessage = Message.builder()
                .info(externalMessageInfo)
                .init(stateInit) // required for deploy only
                .body(externalMessageBody)
                .build();

//        return tonlib.sendRawMessage(createInitExternalMessage(secretKey).message.toBase64());
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
}
