package org.ton.java.smartcontract.token.ft;

import org.ton.java.address.Address;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.smartcontract.token.nft.NftUtils;
import org.ton.java.smartcontract.types.JettonWalletData;
import org.ton.java.smartcontract.wallet.Contract;
import org.ton.java.smartcontract.wallet.Options;
import org.ton.java.tonlib.Tonlib;
import org.ton.java.tonlib.types.RunResult;
import org.ton.java.tonlib.types.TvmStackEntryCell;
import org.ton.java.tonlib.types.TvmStackEntryNumber;
import org.ton.java.utils.Utils;

import java.math.BigInteger;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

public class JettonWallet implements Contract {

    public static final String JETTON_WALLET_CODE_HEX = "B5EE9C7241021201000328000114FF00F4A413F4BCF2C80B0102016202030202CC0405001BA0F605DA89A1F401F481F481A8610201D40607020148080900BB0831C02497C138007434C0C05C6C2544D7C0FC02F83E903E900C7E800C5C75C87E800C7E800C00B4C7E08403E29FA954882EA54C4D167C0238208405E3514654882EA58C511100FC02780D60841657C1EF2EA4D67C02B817C12103FCBC2000113E910C1C2EBCB853600201200A0B020120101101F500F4CFFE803E90087C007B51343E803E903E90350C144DA8548AB1C17CB8B04A30BFFCB8B0950D109C150804D50500F214013E809633C58073C5B33248B232C044BD003D0032C032483E401C1D3232C0B281F2FFF274013E903D010C7E801DE0063232C1540233C59C3E8085F2DAC4F3208405E351467232C7C6600C03F73B51343E803E903E90350C0234CFFE80145468017E903E9014D6F1C1551CDB5C150804D50500F214013E809633C58073C5B33248B232C044BD003D0032C0327E401C1D3232C0B281F2FFF274140371C1472C7CB8B0C2BE80146A2860822625A020822625A004AD822860822625A028062849F8C3C975C2C070C008E00D0E0F009ACB3F5007FA0222CF165006CF1625FA025003CF16C95005CC2391729171E25008A813A08208989680AA008208989680A0A014BCF2E2C504C98040FB001023C85004FA0258CF1601CF16CCC9ED5400705279A018A182107362D09CC8CB1F5230CB3F58FA025007CF165007CF16C9718018C8CB0524CF165006FA0215CB6A14CCC971FB0010241023000E10491038375F040076C200B08E218210D53276DB708010C8CB055008CF165004FA0216CB6A12CB1F12CB3FC972FB0093356C21E203C85004FA0258CF1601CF16CCC9ED5400DB3B51343E803E903E90350C01F4CFFE803E900C145468549271C17CB8B049F0BFFCB8B0A0822625A02A8005A805AF3CB8B0E0841EF765F7B232C7C572CFD400FE8088B3C58073C5B25C60063232C14933C59C3E80B2DAB33260103EC01004F214013E809633C58073C5B3327B55200083200835C87B51343E803E903E90350C0134C7E08405E3514654882EA0841EF765F784EE84AC7CB8B174CFCC7E800C04E81408F214013E809633C58073C5B3327B55205ECCF23D";

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
            options.code = Cell.fromBoc(JETTON_WALLET_CODE_HEX);
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
        if (this.address == null) {
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

        TvmStackEntryNumber balanceNumber = (TvmStackEntryNumber) result.getStackEntry().get(0);
        BigInteger balance = balanceNumber.getNumber();

        TvmStackEntryCell ownerAddr = (TvmStackEntryCell) result.getStackEntry().get(1);
        Address ownerAddress = NftUtils.parseAddress(CellBuilder.fromBoc(Utils.base64SafeUrlToBytes(ownerAddr.getCell().getBytes())));

        TvmStackEntryCell jettonMinterAddr = (TvmStackEntryCell) result.getStackEntry().get(2);
        Address jettonMinterAddress = NftUtils.parseAddress(CellBuilder.fromBoc(Utils.base64SafeUrlToBytes(jettonMinterAddr.getCell().getBytes())));

        TvmStackEntryCell jettonWallet = (TvmStackEntryCell) result.getStackEntry().get(3);
        Cell jettonWalletCode = CellBuilder.fromBoc(Utils.base64SafeUrlToBytes(jettonWallet.getCell().getBytes()));
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

        TvmStackEntryNumber balanceNumber = (TvmStackEntryNumber) result.getStackEntry().get(0);
        return balanceNumber.getNumber();
    }
}