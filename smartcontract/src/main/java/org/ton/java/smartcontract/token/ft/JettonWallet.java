package org.ton.java.smartcontract.token.ft;

import org.ton.java.address.Address;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.smartcontract.token.nft.NftUtils;
import org.ton.java.smartcontract.types.JettonWalletData;
import org.ton.java.smartcontract.types.WalletCodes;
import org.ton.java.smartcontract.wallet.Contract;
import org.ton.java.smartcontract.wallet.Options;
import org.ton.java.tonlib.Tonlib;
import org.ton.java.tonlib.types.RunResult;
import org.ton.java.tonlib.types.TvmStackEntryCell;
import org.ton.java.tonlib.types.TvmStackEntryNumber;

import java.math.BigInteger;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

public class JettonWallet implements Contract {

    Options options;
    Address address;

    /**
     * @param options Options
     */
    public JettonWallet(Options options) {
        this.options = options;
        this.options.wc = 0;

        if (nonNull(options.address)) {
            this.address = Address.of(options.address);
        }

        if (isNull(options.code)) {
            options.code = Cell.fromBoc(WalletCodes.jettonWallet.getValue());
        }
    }

    public String getName() {
        return "jettonWallet";
    }

    @Override
    public Options getOptions() {
        return options;
    }

    @Override
    public Address getAddress() {
        if (isNull(this.address)) {
            return (createStateInit()).address;
        }
        return this.address;
    }

    @Override
    public Cell createDataCell() {
        return CellBuilder.beginCell().endCell();
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
            cell.bits.writeBytes(forwardPayload);
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
        cell.bits.writeUint(queryId, 64);
        cell.bits.writeCoins(jettonAmount);
        cell.bits.writeAddress(responseAddress);
        return cell;
    }

    public JettonWalletData getData(Tonlib tonlib) {
        Address myAddress = this.getAddress();
        RunResult result = tonlib.runMethod(myAddress, "get_wallet_data");

        if (result.getExit_code() != 0) {
            throw new Error("method get_wallet_data, returned an exit code " + result.getExit_code());
        }

        TvmStackEntryNumber balanceNumber = (TvmStackEntryNumber) result.getStack().get(0);
        BigInteger balance = balanceNumber.getNumber();

        TvmStackEntryCell ownerAddr = (TvmStackEntryCell) result.getStack().get(1);
        Address ownerAddress = NftUtils.parseAddress(CellBuilder.fromBoc(ownerAddr.getCell().getBytes()));

        TvmStackEntryCell jettonMinterAddr = (TvmStackEntryCell) result.getStack().get(2);
        Address jettonMinterAddress = NftUtils.parseAddress(CellBuilder.fromBoc(jettonMinterAddr.getCell().getBytes()));

        TvmStackEntryCell jettonWallet = (TvmStackEntryCell) result.getStack().get(3);
        Cell jettonWalletCode = CellBuilder.fromBoc(jettonWallet.getCell().getBytes());
        return JettonWalletData.builder()
                .balance(balance)
                .ownerAddress(ownerAddress)
                .jettonMinterAddress(jettonMinterAddress)
                .jettonWalletCode(jettonWalletCode)
                .build();
    }

    public BigInteger getBalance(Tonlib tonlib) {
        Address myAddress = this.getAddress();
        RunResult result = tonlib.runMethod(myAddress, "get_wallet_data");

        if (result.getExit_code() != 0) {
            throw new Error("method get_wallet_data, returned an exit code " + result.getExit_code());
        }

        TvmStackEntryNumber balanceNumber = (TvmStackEntryNumber) result.getStack().get(0);
        return balanceNumber.getNumber();
    }
}