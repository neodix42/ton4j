package org.ton.java.smartcontract.highload;

import com.iwebpp.crypto.TweetNaclFast;
import lombok.Builder;
import lombok.Getter;
import org.ton.java.address.Address;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.TonHashMap;
import org.ton.java.smartcontract.types.Destination;
import org.ton.java.smartcontract.types.HighloadConfig;
import org.ton.java.smartcontract.types.WalletCodes;
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

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

@Builder
@Getter
public class HighloadWallet implements Contract {

    //https://github.com/ton-blockchain/ton/blob/master/crypto/smartcont/highload-wallet-v2-code.fc
    TweetNaclFast.Signature.KeyPair keyPair;
    long walletId;
    byte[] publicKey;
    BigInteger queryId;

    public static class HighloadWalletBuilder {
    }

    public static HighloadWalletBuilder builder() {
        return new CustomHighloadWalletBuilder();
    }

    private static class CustomHighloadWalletBuilder extends HighloadWalletBuilder {
        @Override
        public HighloadWallet build() {
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

    /**
     * interface to <a href="https://github.com/ton-blockchain/ton/blob/master/crypto/smartcont/highload-wallet-v2-code.fc">highload smart-contract</a>
     */
//    public HighloadWallet(Options options) {
//        options.code = CellBuilder.beginCell().fromBoc(WalletCodes.highload.getValue()).endCell();
//    }
    @Override
    public String getName() {
        return "highload-v2";
    }

    /**
     * initial contract storage
     *
     * @return Cell
     */
    @Override
    public Cell createDataCell() {
        CellBuilder cell = CellBuilder.beginCell();
        cell.storeUint(walletId, 32); // wallet id
        cell.storeUint(BigInteger.ZERO, 64); // last_cleaned
        cell.storeBytes(isNull(keyPair) ? publicKey : keyPair.getPublicKey()); // 256 bits
        cell.storeBit(false); // initial storage has old_queries dict empty
        return cell.endCell();
    }

    @Override
    public Cell createCodeCell() {
        return CellBuilder.beginCell().
                fromBoc(WalletCodes.highload.getValue()).
                endCell();
    }

    public Cell createDeployMessage() {
        return CellBuilder.beginCell()
                .storeUint(walletId, 32)
                .storeUint(queryId, 64) // query id
                .storeBit(false)
                .endCell();
    }

    public Cell createTransferBody(HighloadConfig config) {
        CellBuilder body = CellBuilder.beginCell();
        body.storeUint(config.getWalletId(), 32);
        body.storeUint(config.getQueryId(), 64);
        if (nonNull(config.getDestinations())) {
            body.storeBit(true);
            body.storeRef(createDict(config));
        } else {
            body.storeBit(false);
        }
        return body.endCell();
    }

//    public Cell createSigningMessageInternal(HighloadConfig highloadConfig) {
//        CellBuilder message = CellBuilder.beginCell();
//        message.storeUint(BigInteger.valueOf(getOptions().walletId), 32);
//        message.storeUint(highloadConfig.getQueryId(), 64);
//        message.storeBit(true);
//        message.storeRef(createDict(highloadConfig));
//        return message.endCell();
//    }

    public String getPublicKey() {
        Address myAddress = getAddress();
        RunResult result = tonlib.runMethod(myAddress, "get_public_key");

        if (result.getExit_code() != 0) {
            throw new Error("method get_public_key, returned an exit code " + result.getExit_code());
        }

        TvmStackEntryNumber publicKeyNumber = (TvmStackEntryNumber) result.getStack().get(0);
        return publicKeyNumber.getNumber().toString(16);
    }

    /**
     * Sends to up to 84 destinations
     *
     * @param highloadConfig HighloadConfig
     */
    public ExtMessageInfo sendTonCoins(HighloadConfig highloadConfig) {
        Cell body = createTransferBody(highloadConfig);

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

    private Cell createDict(HighloadConfig config) {
        int dictKeySize = 16;
        TonHashMap dictDestinations = new TonHashMap(dictKeySize);

        long i = 0; // key, index 16bit
        for (Destination destination : config.getDestinations()) {

            Cell order;
            if (destination.isBounce()) {
                if (isNull(destination.getBody()) && nonNull(destination.getComment())) {
                    order = MsgUtils.createInternalMessage(Address.of(destination.getAddress()), destination.getAmount(), null,
                            CellBuilder.beginCell()
                                    .storeUint(0, 32)
                                    .storeString(destination.getComment())
                                    .endCell()).toCell();
                } else if (nonNull(destination.getBody())) {
                    order = MsgUtils.createInternalMessage(Address.of(destination.getAddress()), destination.getAmount(), null, destination.getBody()).toCell();
                } else {
                    order = MsgUtils.createInternalMessage(Address.of(destination.getAddress()), destination.getAmount(), null, null).toCell();
                }
            } else {
                if (isNull(destination.getBody()) && nonNull(destination.getComment())) {
                    order = MsgUtils.createNonBounceableInternalMessage(Address.of(destination.getAddress()), destination.getAmount(), null,
                            CellBuilder.beginCell()
                                    .storeUint(0, 32)
                                    .storeString(destination.getComment())
                                    .endCell()).toCell();
                } else if (nonNull(destination.getBody())) {
                    order = MsgUtils.createNonBounceableInternalMessage(Address.of(destination.getAddress()), destination.getAmount(), null, destination.getBody()).toCell();
                } else {
                    order = MsgUtils.createNonBounceableInternalMessage(Address.of(destination.getAddress()), destination.getAmount(), null, null).toCell();
                }
            }

            CellBuilder p = CellBuilder.beginCell()
                    .storeUint((destination.getMode() == 0) ? 3 : destination.getMode() & 0xff, 8)
                    .storeRef(order);

            dictDestinations.elements.put(i++, p.endCell());
        }

        Cell cellDict = dictDestinations.serialize(
                k -> CellBuilder.beginCell().storeUint((Long) k, dictKeySize).endCell().getBits(),
                v -> (Cell) v
        );

        return cellDict;
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
}
