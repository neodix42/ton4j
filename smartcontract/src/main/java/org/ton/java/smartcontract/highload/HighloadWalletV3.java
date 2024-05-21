package org.ton.java.smartcontract.highload;

import com.iwebpp.crypto.TweetNaclFast;
import lombok.Builder;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.ton.java.address.Address;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.smartcontract.types.*;
import org.ton.java.smartcontract.wallet.Contract;
import org.ton.java.tlb.types.*;
import org.ton.java.tonlib.Tonlib;
import org.ton.java.tonlib.types.ExtMessageInfo;
import org.ton.java.tonlib.types.RunResult;
import org.ton.java.tonlib.types.TvmStackEntryNumber;
import org.ton.java.utils.Utils;

import java.math.BigInteger;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

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


    public static class HighloadWalletV3Builder {
    }

    public static HighloadWalletV3Builder builder() {
        return new CustomHighloadWalletV3Builder();
    }

    private static class CustomHighloadWalletV3Builder extends HighloadWalletV3Builder {
        @Override
        public HighloadWalletV3 build() {
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
        return "highload-v3";
    }

    /**
     * initial contract storage
     * storage$_ public_key:bits256
     * subwallet_id:uint32
     * old_queries:(HashmapE 14 ^Cell)
     * queries:(HashmapE 14 ^Cell)
     * last_clean_time:uint64
     * timeout:uint22 = Storage;
     *
     * @return Cell
     */
    @Override
    public Cell createDataCell() {
        return CellBuilder.beginCell()
                .storeBytes(keyPair.getPublicKey())
                .storeUint(walletId, 32)
                .storeBit(false) // old queries
                .storeBit(false) // queries
                .storeUint(0, 64) // last clean time
                .storeUint((timeout == 0) ? 5 * 60 : timeout, 22) //time out
                .endCell();
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
    public Cell createTransferMessage(HighloadV3Config highloadConfig) {
        return CellBuilder.beginCell()
                .storeUint(highloadConfig.getWalletId(), 32)
                .storeRef(highloadConfig.getBody())
                .storeUint((highloadConfig.getMode() == 0) ? 3 : highloadConfig.getMode(), 8)
                .storeUint(highloadConfig.getQueryId(), 23)
                .storeUint((highloadConfig.getCreatedAt() == 0) ? Instant.now().getEpochSecond() - 60 : highloadConfig.getCreatedAt(), 64)
                .storeUint((highloadConfig.getTimeOut() == 0) ? 5 * 60 : highloadConfig.getTimeOut(), 22)
                .endCell();
    }

    /**
     * @param highloadConfig HighloadV3Config
     */
    public ExtMessageInfo sendTonCoins(HighloadV3Config highloadConfig) {
        Address ownAddress = getAddress();

        Cell body = createTransferMessage(highloadConfig);

        Message externalMessage = Message.builder()
                .info(ExternalMessageInfo.builder()
                        .dstAddr(MsgAddressIntStd.builder()
                                .workchainId(ownAddress.wc)
                                .address(ownAddress.toBigInteger())
                                .build())
                        .build())
                .body(CellBuilder.beginCell()
                        .storeBytes(Utils.signData(keyPair.getPublicKey(), keyPair.getSecretKey(), body.hash()))
                        .storeRef(body)
                        .endCell())
                .build();

        return tonlib.sendRawMessage(externalMessage.toCell().toBase64());
    }

    public ExtMessageInfo deploy(HighloadV3Config highloadConfig) {
        Address ownAddress = getAddress();

        if (isNull(highloadConfig.getBody())) {
            //dummy deploy msg
            highloadConfig.setBody(MessageRelaxed.builder()
                    .info(InternalMessageInfoRelaxed.builder()
                            .dstAddr(MsgAddressIntStd.builder()
                                    .workchainId(ownAddress.wc)
                                    .address(ownAddress.toBigInteger())
                                    .build())
                            .createdAt((highloadConfig.getCreatedAt() == 0) ? Instant.now().getEpochSecond() - 60 : highloadConfig.getCreatedAt())
                            .build())
                    .build().toCell());
        }

        Cell innerMsg = createTransferMessage(highloadConfig);

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
        return CellBuilder.beginCell()
                .storeUint(0, 32)
                .storeSnakeString(text)
                .endCell();
    }

    public static Cell createJettonTransferBody(long queryId, BigInteger amount, Address destination, Address responseDestination, Cell customPayload, BigInteger forwardAmount, Cell forwardPayload) {
        return CellBuilder.beginCell()
                .storeUint(0x0f8a7ea5, 32)
                .storeUint(queryId, 64)
                .storeCoins(amount)
                .storeAddress(destination)
                .storeAddress(nonNull(responseDestination) ? responseDestination : destination)
                .storeRefMaybe(customPayload)
                .storeCoins(forwardAmount)
                .storeRefMaybe(forwardPayload)
                .endCell();
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

        return CellBuilder.beginCell()
                .storeUint(0xae42e5a4, 32)
                .storeUint(queryId, 64)
                .storeRef(prev)
                .endCell();
    }

    public Cell createSingleTransfer(Address destAddress, double amount, Boolean bounce) {

        CommonMsgInfoRelaxed internalMsgInfo = InternalMessageInfoRelaxed.builder()
                .bounce(bounce)
                .srcAddr(MsgAddressExtNone.builder().build())
                .dstAddr(MsgAddressIntStd.builder()
                        .workchainId(destAddress.wc)
                        .address(destAddress.toBigInteger())
                        .build())
                .value(CurrencyCollection.builder().coins(Utils.toNano(amount)).build())
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

    public Cell createBulkTransfer(BigInteger totalAmount, Cell bulkMessages) {
        Address ownAddress = getAddress();
        return MessageRelaxed.builder()
                .info(InternalMessageInfoRelaxed.builder()
                        .dstAddr(MsgAddressIntStd.builder()
                                .workchainId(ownAddress.wc)
                                .address(ownAddress.toBigInteger())
                                .build())
                        .value(CurrencyCollection.builder().coins(totalAmount).build())
                        .build())
                .body(bulkMessages)
                .build().toCell();
    }

    public Cell createBulkTransfer(List<Destination> recipients, BigInteger queryId) {

        if (recipients.size() > 1000) {
            throw new IllegalArgumentException("Maximum number of recipients should be less than 1000");
        }

        BigInteger totalAmount = BigInteger.ZERO;

        for (Destination destination : recipients) {
            totalAmount = totalAmount.add(destination.getAmount());
        }

        List<Destination> tmpRecipients = new ArrayList<>(recipients);
        Cell chunk1, chunk2, chunk3, chunk4;

        chunk1 = addChunk(tmpRecipients.subList(0, Math.min(tmpRecipients.size(), 250)), queryId, null);
        tmpRecipients.subList(0, Math.min(tmpRecipients.size(), 250)).clear();
        if (tmpRecipients.isEmpty()) {
            return MessageRelaxed.builder()
                    .info(InternalMessageInfoRelaxed.builder()
                            .dstAddr(getAddressIntStd())
                            .value(CurrencyCollection.builder()
                                    .coins(totalAmount)
                                    .build())
                            .build())
                    .body(chunk1)
                    .build().toCell();
        } else {
            chunk2 = addChunk(tmpRecipients.subList(0, Math.min(tmpRecipients.size(), 250)), queryId, chunk1);
            tmpRecipients.subList(0, Math.min(tmpRecipients.size(), 250)).clear();
        }

        if (tmpRecipients.isEmpty()) {
            return MessageRelaxed.builder()
                    .info(InternalMessageInfoRelaxed.builder()
                            .dstAddr(getAddressIntStd())
                            .value(CurrencyCollection.builder()
                                    .coins(totalAmount.add(BigInteger.valueOf(Utils.toNano(0.01).longValue())))
                                    .build())
                            .build())
                    .body(chunk2)
                    .build().toCell();
        } else {
            chunk3 = addChunk(tmpRecipients.subList(0, Math.min(tmpRecipients.size(), 250)), queryId, chunk2);
            tmpRecipients.subList(0, Math.min(tmpRecipients.size(), 250)).clear();
        }

        if (tmpRecipients.isEmpty()) {
            return MessageRelaxed.builder()
                    .info(InternalMessageInfoRelaxed.builder()
                            .dstAddr(getAddressIntStd())
                            .value(CurrencyCollection.builder()
                                    .coins(totalAmount.add(BigInteger.valueOf(Utils.toNano(0.02).longValue())))
                                    .build())
                            .build())
                    .body(chunk3)
                    .build().toCell();
        } else {
            chunk4 = addChunk(tmpRecipients.subList(0, Math.min(tmpRecipients.size(), 250)), queryId, chunk3);
            tmpRecipients.subList(0, Math.min(tmpRecipients.size(), 250)).clear();
        }

        return MessageRelaxed.builder()
                .info(InternalMessageInfoRelaxed.builder()
                        .dstAddr(getAddressIntStd())
                        .value(CurrencyCollection.builder()
                                .coins(totalAmount.add(BigInteger.valueOf(Utils.toNano(0.03).longValue())))
                                .build())
                        .build())
                .body(chunk4)
                .build().toCell();
    }

    private Cell addChunk(List<Destination> destinations, BigInteger queryId, Cell enclosedMessages) {
        List<OutAction> outActions = new ArrayList<>();

        if (isNull(enclosedMessages)) {
            for (Destination destination : destinations) {
                outActions.add(convertDestinationToOutAction(destination, null));
            }
        } else {
            for (int i = 0; i < destinations.size() - 1; i++) {
                outActions.add(convertDestinationToOutAction(destinations.get(i), null));
            }
            outActions.add(convertDestinationToOutAction(destinations.get(destinations.size() - 1), enclosedMessages));
        }

        return HighloadV3InternalMessageBody.builder()
                .queryId(queryId)
                .actions(OutList.builder()
                        .actions(outActions)
                        .build())
                .build().toCell();
    }

    private OutAction convertDestinationToOutAction(Destination destination, Cell enclosedMessages) {
        Address dstAddress = Address.of(destination.getAddress());

        if (isNull(enclosedMessages)) {
            return ActionSendMsg.builder()
                    .mode(destination.getMode())
                    .outMsg(MessageRelaxed.builder()
                            .info(InternalMessageInfoRelaxed.builder()
                                    .bounce(destination.isBounce())
                                    .dstAddr(MsgAddressIntStd.builder()
                                            .workchainId(dstAddress.wc)
                                            .address(dstAddress.toBigInteger())
                                            .build())
                                    .value(CurrencyCollection.builder()
                                            .coins(destination.getAmount())
                                            .build())
                                    .build())
                            //.init() is not supported
                            .body((isNull(destination.getBody()) && StringUtils.isNotEmpty(destination.getComment())) ?
                                    CellBuilder.beginCell()
                                            .storeUint(0, 32)
                                            .storeString(destination.getComment())
                                            .endCell() :
                                    destination.getBody()
                            )
                            .build())
                    .build();
        } else {
            return ActionSendMsg.builder()
                    .mode(3)
                    .outMsg(MessageRelaxed.builder()
                            .info(InternalMessageInfoRelaxed.builder()
                                    .dstAddr(getAddressIntStd())
                                    .value(CurrencyCollection.builder()
                                            .coins(destination.getAmount())
                                            .build())
                                    .build())
                            .body(enclosedMessages)
                            .build())
                    .build();
        }

    }
}
