package org.ton.java.smartcontract.highload;

import com.iwebpp.crypto.TweetNaclFast;
import org.ton.java.address.Address;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.CellSlice;
import org.ton.java.cell.TonHashMap;
import org.ton.java.smartcontract.token.ft.JettonWallet;
import org.ton.java.smartcontract.types.Destination;
import org.ton.java.smartcontract.types.ExternalMessage;
import org.ton.java.smartcontract.wallet.Options;
import org.ton.java.smartcontract.wallet.WalletContract;
import org.ton.java.tonlib.Tonlib;
import org.ton.java.tonlib.types.RunResult;
import org.ton.java.tonlib.types.TvmStackEntryNumber;
import org.ton.java.utils.Utils;

import java.math.BigInteger;
import java.time.Instant;

public class HighloadWallet implements WalletContract {

    public static final String HIGHLOAD_WALLET_V2_CODE_HEX = "B5EE9C724101090100E5000114FF00F4A413F4BCF2C80B010201200203020148040501EAF28308D71820D31FD33FF823AA1F5320B9F263ED44D0D31FD33FD3FFF404D153608040F40E6FA131F2605173BAF2A207F901541087F910F2A302F404D1F8007F8E16218010F4786FA5209802D307D43001FB009132E201B3E65B8325A1C840348040F4438AE63101C8CB1F13CB3FCBFFF400C9ED54080004D03002012006070017BD9CE76A26869AF98EB85FFC0041BE5F976A268698F98E99FE9FF98FA0268A91040207A0737D098C92DBFC95DD1F140034208040F4966FA56C122094305303B9DE2093333601926C21E2B39F9E545A";
    Options options;
    Address address;

    /**
     * @param options Options
     */
    public HighloadWallet(Options options) {
        this.options = options;
        options.code = Cell.fromBoc(HIGHLOAD_WALLET_V2_CODE_HEX);
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
        if (address == null) {
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
        cell.storeBit(false);

        return cell.endCell();
    }

    @Override
    public Cell createSigningMessage(long seqno) {
        CellBuilder message = CellBuilder.beginCell();
        message.storeUint(BigInteger.valueOf(getOptions().walletId), 32);

        BigInteger i = BigInteger.valueOf((long) Math.pow(Instant.now().getEpochSecond() + 5 * 60L, 32))
                .add(new BigInteger(String.valueOf(Instant.now().getEpochSecond())));
        System.out.println("queryId --------------------------------- " + i);
        message.storeUint(i, 64);
        message.storeBit(false);

        return message.endCell();
    }

    public Cell createSigningMessageInternal() {
        CellBuilder message = CellBuilder.beginCell();
        message.storeUint(BigInteger.valueOf(getOptions().walletId), 32);

        BigInteger i = BigInteger.valueOf((long) Math.pow(Instant.now().getEpochSecond() + 5 * 60L, 32))
                .add(new BigInteger(String.valueOf(Instant.now().getEpochSecond())));
        System.out.println("queryId --------------------------------- " + i);
        message.storeUint(i, 64);
        message.storeBit(true); // true ------------

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
    
    public void sendTonCoins(Tonlib tonlib, byte[] secretKey) {

        long seqno = 1; // dummy

        CellBuilder payload = CellBuilder.beginCell();

        payload.storeUint(BigInteger.valueOf(getOptions().getWalletId()), 32);
        payload.storeUint(getOptions().getQueryId(), 64);

        int dictKeySize = 16;
        // same error with TonHashMap and TonHashMapE
//        Failed Compute Phase 💻: (exit_code 10: ✕ Dictionary error)
        TonHashMap dictDestinations = new TonHashMap(dictKeySize);

        long i = 0; // key, index 16bit
        for (Destination destination : getOptions().getHighloadConfig().getDestinations()) {

            ExternalMessage extMsg = createTransferMessage(
                    secretKey,
                    destination.getAddress(),
                    destination.getAmount(),
                    seqno); // redundant

            CellBuilder p = CellBuilder.beginCell();
            p.storeUint(destination.getMode(), 8); // mode
            p.storeRef(extMsg.message);

            dictDestinations.elements.put(i++, p.endCell());
        }

        Cell cellDict = dictDestinations.serialize(
                k -> CellBuilder.beginCell().storeUint((Long) k, dictKeySize).bits,
                v -> CellBuilder.beginCell().storeSlice(CellSlice.beginParse((Cell) v)) // value dummy
        );

        payload.storeDict(cellDict);

//        Cell orderHeader = Contract.createInternalMessageHeader(this.getAddress(), Utils.toNano(0.5));
//        Cell order = Contract.createCommonMsgInfo(orderHeader, null, null);

        Cell signingMessageAll = createSigningMessageInternal();
        signingMessageAll.refs.add(payload);

        ExternalMessage msg = createExternalMessage(signingMessageAll, secretKey, seqno, false);

//        ExternalMessage msg = createTransferMessage(
//                secretKey,
//                this.getAddress(),
//                amount,
//                seqno,
//                payload.endCell()
//        );

        tonlib.sendRawMessage(msg.message.toBocBase64(false));
    }

    private void transfer(Tonlib tonlib, WalletContract admin, Address jettonWalletAddress, Address toAddress, BigInteger jettonAmount, TweetNaclFast.Signature.KeyPair keyPair) {

        long seqno = admin.getSeqno(tonlib);

        ExternalMessage extMsg = admin.createTransferMessage(
                keyPair.getSecretKey(),
                Address.of(jettonWalletAddress),
                Utils.toNano(0.05),
                seqno,
                JettonWallet.createTransferBody(
                        0,
                        jettonAmount,
                        Address.of(toAddress), // destination
                        admin.getAddress(), // response address
                        Utils.toNano("0.01"), // forward amount
                        "gift".getBytes() // forward payload
                ));

        tonlib.sendRawMessage(extMsg.message.toBocBase64(false));
    }

    public void deploy(Tonlib tonlib, byte[] secretKey) {
        tonlib.sendRawMessage(createInitExternalMessage(secretKey).message.toBocBase64(false));
    }
}
