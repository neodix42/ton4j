package org.ton.java.smartcontract.token.ft;

import com.iwebpp.crypto.TweetNaclFast;
import lombok.Builder;
import lombok.Getter;
import org.ton.java.address.Address;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.smartcontract.token.nft.NftUtils;
import org.ton.java.smartcontract.types.JettonWalletData;
import org.ton.java.smartcontract.types.WalletCodes;
import org.ton.java.smartcontract.wallet.Contract;
import org.ton.java.tonlib.Tonlib;
import org.ton.java.tonlib.types.RunResult;
import org.ton.java.tonlib.types.TvmStackEntryCell;
import org.ton.java.tonlib.types.TvmStackEntryNumber;
import org.ton.java.tonlib.types.TvmStackEntrySlice;
import org.ton.java.utils.Utils;

import java.math.BigInteger;

import static java.util.Objects.isNull;

@Builder
@Getter
public class JettonWallet implements Contract {

    TweetNaclFast.Signature.KeyPair keyPair;
    Address address;

    public static class JettonWalletBuilder {
    }

    public static JettonWalletBuilder builder() {
        return new CustomJettonWalletBuilder();
    }

    private static class CustomJettonWalletBuilder extends JettonWalletBuilder {
        @Override
        public JettonWallet build() {
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

    /**
     * @return Cell cell contains nft data
     */
    public static Cell createTransferBody(long queryId, BigInteger jettonAmount, Address toAddress, Address responseAddress, BigInteger forwardAmount, Cell forwardPayload) {
        return CellBuilder.beginCell()
                .storeUint(0xf8a7ea5, 32)
                .storeUint(queryId, 64) // default
                .storeCoins(jettonAmount)
                .storeAddress(toAddress)
                .storeAddress(responseAddress)
                .storeBit(false) // null custom_payload
                .storeCoins(forwardAmount) // default 0
                .storeRefMaybe(forwardPayload) // forward_payload in ref cell
                .endCell();
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

    public JettonWalletData getData() {
        RunResult result = tonlib.runMethod(address, "get_wallet_data");

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

    public BigInteger getBalance() {
        RunResult result = tonlib.runMethod(address, "get_wallet_data");

        if (result.getExit_code() != 0) {
            throw new Error("method get_wallet_data, returned an exit code " + result.getExit_code());
        }

        TvmStackEntryNumber balanceNumber = (TvmStackEntryNumber) result.getStack().get(0);
        return balanceNumber.getNumber();
    }
}