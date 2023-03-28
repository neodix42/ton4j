package org.ton.java.smartcontract.highload;

import org.ton.java.address.Address;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.TonHashMap;
import org.ton.java.smartcontract.types.Destination;
import org.ton.java.smartcontract.types.ExternalMessage;
import org.ton.java.smartcontract.types.HighloadConfig;
import org.ton.java.smartcontract.types.WalletCodes;
import org.ton.java.smartcontract.wallet.Contract;
import org.ton.java.smartcontract.wallet.Options;
import org.ton.java.smartcontract.wallet.WalletContract;
import org.ton.java.tonlib.Tonlib;
import org.ton.java.tonlib.types.ExtMessageInfo;
import org.ton.java.tonlib.types.RunResult;
import org.ton.java.tonlib.types.TvmStackEntryNumber;

import java.math.BigInteger;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

public class HighloadWallet implements WalletContract {

    //https://github.com/ton-blockchain/ton/blob/master/crypto/smartcont/highload-wallet-v2-code.fc
    Options options;
    Address address;

    /**
     * interface to <a href="https://github.com/ton-blockchain/ton/blob/master/crypto/smartcont/highload-wallet-v2-code.fc">highload smart-contract</a>
     *
     * @param options Options - mandatory -  highloadQueryId, walletId, publicKey
     */
    public HighloadWallet(Options options) {
        this.options = options;
        options.code = Cell.fromBoc(WalletCodes.highload.getValue());
    }

    @Override
    public String getName() {
        return "highload-v2";
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
     *
     * @return cell Cell
     */
    @Override
    public Cell createDataCell() {
        CellBuilder cell = CellBuilder.beginCell();

        cell.storeUint(BigInteger.valueOf(getOptions().walletId), 32); // sub-wallet id
        cell.storeUint(BigInteger.ZERO, 64); // last_cleaned
        cell.storeBytes(getOptions().getPublicKey()); // 256 bits
        cell.storeBit(false); // initial storage has old_queries dict empty

        return cell.endCell();
    }

    @Override
    public Cell createSigningMessage(long seqno) {
        CellBuilder message = CellBuilder.beginCell();
        message.storeUint(BigInteger.valueOf(getOptions().walletId), 32);

        message.storeUint(getOptions().getHighloadQueryId(), 64);
        message.storeBit(false);

        return message.endCell();
    }

    public Cell createSigningMessageInternal(HighloadConfig highloadConfig) {
        CellBuilder message = CellBuilder.beginCell();
        message.storeUint(BigInteger.valueOf(getOptions().walletId), 32);

        message.storeUint(highloadConfig.getQueryId(), 64);

        message.storeBit(true);
        message.storeRef(createDict(highloadConfig));

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

    /**
     * Sends to up to 84 destinations
     *
     * @param tonlib         Tonlib
     * @param secretKey      byte[]
     * @param highloadConfig HighloadConfig
     */
    public ExtMessageInfo sendTonCoins(Tonlib tonlib, byte[] secretKey, HighloadConfig highloadConfig) {
        Cell signingMessageAll = createSigningMessageInternal(highloadConfig);
        ExternalMessage msg = createExternalMessage(signingMessageAll, secretKey, 1);
        return tonlib.sendRawMessage(msg.message.toBocBase64(false));
    }

    private Cell createDict(HighloadConfig highloadConfig) {
        int dictKeySize = 16;
        TonHashMap dictDestinations = new TonHashMap(dictKeySize);

        long i = 0; // key, index 16bit
        for (Destination destination : highloadConfig.getDestinations()) {

            Cell orderHeader = Contract.createInternalMessageHeader(destination.getAddress(), destination.getAmount());
            Cell order;
            if (nonNull(destination.getComment())) {
                order = Contract.createCommonMsgInfo(orderHeader, null, CellBuilder.beginCell().storeUint(0, 32).storeString(destination.getComment()).endCell());
            } else {
                order = Contract.createCommonMsgInfo(orderHeader);
            }

            CellBuilder p = CellBuilder.beginCell();
            p.storeUint(destination.getMode(), 8); // mode
            p.storeRef(order);

            dictDestinations.elements.put(i++, p.endCell());
        }

        Cell cellDict = dictDestinations.serialize(
                k -> CellBuilder.beginCell().storeUint((Long) k, dictKeySize).bits,
                v -> (Cell) v
        );

        return cellDict;
    }

    public void deploy(Tonlib tonlib, byte[] secretKey) {
        tonlib.sendRawMessage(createInitExternalMessage(secretKey).message.toBocBase64(false));
    }
}
