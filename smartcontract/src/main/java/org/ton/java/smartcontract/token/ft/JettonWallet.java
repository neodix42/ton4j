package org.ton.java.smartcontract.token.ft;

import com.iwebpp.crypto.TweetNaclFast;
import lombok.Builder;
import lombok.Getter;
import org.ton.java.address.Address;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.smartcontract.token.nft.NftUtils;
import org.ton.java.smartcontract.types.JettonWalletConfig;
import org.ton.java.smartcontract.types.JettonWalletData;
import org.ton.java.smartcontract.types.WalletCodes;
import org.ton.java.smartcontract.wallet.Contract;
import org.ton.java.tonlib.Tonlib;
import org.ton.java.tonlib.types.*;
import org.ton.java.utils.Utils;

import java.math.BigInteger;

import static java.util.Objects.isNull;

@Builder
@Getter
public class JettonWallet implements Contract {

    TweetNaclFast.Signature.KeyPair keyPair;
    Address address;

//    public JettonWallet(Options options) {
//        this.options = options;
//        this.options.wc = 0;
//
//        if (nonNull(options.address)) {
//            this.address = Address.of(options.address);
//        }
//
//        if (isNull(options.code)) {
//            options.code = CellBuilder.beginCell().fromBoc(WalletCodes.jettonWallet.getValue()).endCell();
//        }
//    }

    public static class JettonWalletBuilder {
        JettonWalletBuilder() {
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

    public String getName() {
        return "jettonWallet";
    }

    @Override
    public Cell createDataCell() {
        return CellBuilder.beginCell().endCell();
    }

    @Override
    public Cell createCodeCell() {
        return CellBuilder.beginCell().
                fromBoc(WalletCodes.jettonWallet.getValue()).
                endCell();
    }

    public ExtMessageInfo deploy(Tonlib tonlib, JettonWalletConfig config) {
        return null;
    }

    /**
     * @return Cell cell contains nft data
     */
    public static Cell createTransferBody(long queryId, BigInteger jettonAmount, Address toAddress, Address responseAddress, BigInteger forwardAmount, byte[] forwardPayload) {
        CellBuilder cell = CellBuilder.beginCell();
        cell.storeUint(0xf8a7ea5, 32);
        cell.storeUint(queryId, 64); // default
        cell.storeCoins(jettonAmount);
        cell.storeAddress(toAddress);
        cell.storeAddress(responseAddress);
        cell.storeBit(false); // null custom_payload
        cell.storeCoins(forwardAmount); // default 0
        cell.storeBit(false); // forward_payload in this slice, not separate cell
        if (forwardPayload.length != 0) {
            cell.storeBytes(forwardPayload);
        }
        return cell.endCell();
    }

    /**
     * @param queryId         long
     * @param jettonAmount    BigInteger
     * @param responseAddress Address
     */
    public static Cell createBurnBody(long queryId, BigInteger jettonAmount, Address responseAddress) {
        CellBuilder cell = CellBuilder.beginCell();
        cell.storeUint(0x595f07bc, 32); //burn up
        cell.storeUint(queryId, 64);
        cell.storeCoins(jettonAmount);
        cell.storeAddress(responseAddress);
        return cell.endCell();
    }

    public JettonWalletData getData(Tonlib tonlib) {
        Address myAddress = address;
        System.out.println("jetton wallet? : " + myAddress.toString(true, true));
        RunResult result = tonlib.runMethod(myAddress, "get_wallet_data");

        if (result.getExit_code() != 0) {
            throw new Error("method get_wallet_data, returned an exit code " + result.getExit_code());
        }

        TvmStackEntryNumber balanceNumber = (TvmStackEntryNumber) result.getStack().get(0);
        BigInteger balance = balanceNumber.getNumber();

        TvmStackEntrySlice ownerAddr = (TvmStackEntrySlice) result.getStack().get(1);
        Address ownerAddress = NftUtils.parseAddress(CellBuilder.beginCell().fromBoc(Utils.base64ToBytes(ownerAddr.getSlice().getBytes())).endCell());

        TvmStackEntrySlice jettonMinterAddr = (TvmStackEntrySlice) result.getStack().get(2);
        Address jettonMinterAddress = NftUtils.parseAddress(CellBuilder.beginCell().fromBoc(Utils.base64ToBytes(jettonMinterAddr.getSlice().getBytes())).endCell());

        TvmStackEntryCell jettonWallet = (TvmStackEntryCell) result.getStack().get(3);
        Cell jettonWalletCode = CellBuilder.beginCell().fromBoc(Utils.base64ToBytes(jettonWallet.getCell().getBytes())).endCell();
        return JettonWalletData.builder()
                .balance(balance)
                .ownerAddress(ownerAddress)
                .jettonMinterAddress(jettonMinterAddress)
                .jettonWalletCode(jettonWalletCode)
                .build();
    }

    public BigInteger getBalance(Tonlib tonlib) {
        Address myAddress = address;
        RunResult result = tonlib.runMethod(myAddress, "get_wallet_data");

        if (result.getExit_code() != 0) {
            throw new Error("method get_wallet_data, returned an exit code " + result.getExit_code());
        }

        TvmStackEntryNumber balanceNumber = (TvmStackEntryNumber) result.getStack().get(0);
        return balanceNumber.getNumber();
    }
}