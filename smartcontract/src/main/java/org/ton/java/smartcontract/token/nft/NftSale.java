package org.ton.java.smartcontract.token.nft;

import com.iwebpp.crypto.TweetNaclFast;
import org.ton.java.address.Address;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.smartcontract.types.ExternalMessage;
import org.ton.java.smartcontract.types.NftSaleData;
import org.ton.java.smartcontract.wallet.Contract;
import org.ton.java.smartcontract.wallet.Options;
import org.ton.java.smartcontract.wallet.WalletContract;
import org.ton.java.tonlib.Tonlib;
import org.ton.java.tonlib.types.RunResult;
import org.ton.java.tonlib.types.TvmStackEntryCell;
import org.ton.java.tonlib.types.TvmStackEntryNumber;
import org.ton.java.utils.Utils;

import java.math.BigInteger;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

public class NftSale implements Contract {
    public static final String NFT_SALE_HEX_CODE = "B5EE9C7241020A010001B4000114FF00F4A413F4BCF2C80B01020120020302014804050004F2300202CD0607002FA03859DA89A1F481F481F481F401A861A1F401F481F4006101F7D00E8698180B8D8492F82707D201876A2687D207D207D207D006A18116BA4E10159C71D991B1B2990E382C92F837028916382F970FA01698FC1080289C6C8895D7970FAE99F98FD2018201A642802E78B2801E78B00E78B00FD016664F6AA701363804C9B081B2299823878027003698FE99F9810E000C92F857010C0801F5D41081DCD650029285029185F7970E101E87D007D207D0018384008646582A804E78B28B9D090D0A85AD08A500AFD010AE5B564B8FD80384008646582AC678B2803FD010B65B564B8FD80384008646582A802E78B00FD0109E5B564B8FD80381041082FE61E8A10C00C646582A802E78B117D010A65B509E58F8A40900C8C0029A3110471036454012F004E032363704C0038E4782103B9ACA0015BEF2E1C95312C70559C705B1F2E1CA702082105FCC3D14218010C8CB055006CF1622FA0215CB6A14CB1F14CB3F21CF1601CF16CA0021FA02CA00C98100A0FB00E05F06840FF2F0002ACB3F22CF1658CF16CA0021FA02CA00C98100A0FB00AECABAD1";

    Options options;
    Address address;

    /**
     * @param options Options, requires:
     *                marketplaceAddress
     *                nftAddress
     *                fullPrice
     *                marketplaceFee
     *                royaltyAddress
     *                royaltyAmount
     */
    public NftSale(Options options) {
        this.options = options;
        this.options.wc = 0;
        if (nonNull(options.address)) {
            this.address = Address.of(options.address);
        }

        if (isNull(options.code)) {
            options.code = Cell.fromBoc(NFT_SALE_HEX_CODE);
        }
    }

    public String getName() {
        return "nftSale";
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
     * @return Cell cell contains sale data
     */
    @Override
    public Cell createDataCell() {
        CellBuilder cell = CellBuilder.beginCell();
        cell.storeAddress(options.marketplaceAddress);
        cell.storeAddress(options.nftItemAddress);
        cell.storeAddress(null); //nft_owner_address
        cell.storeCoins(options.fullPrice);

        CellBuilder feesCell = CellBuilder.beginCell();
        feesCell.storeCoins(options.marketplaceFee);
        feesCell.storeAddress(options.royaltyAddress);
        feesCell.storeCoins(options.royaltyAmount);
        cell.storeRef(feesCell);

        return cell.endCell();
    }


    /**
     * Calls get_sale_data against nft-sale contract
     *
     * @return NftSaleData
     */
    public NftSaleData getData(Tonlib tonlib) {
        Address myAddress = this.getAddress();
        RunResult result = tonlib.runMethod(myAddress, "get_sale_data");

        if (result.getExit_code() != 0) {
            throw new Error("method get_sale_data, returned an exit code " + result.getExit_code());
        }

        TvmStackEntryCell marketplaceAddressCell = (TvmStackEntryCell) result.getStack().get(0);
        Address marketplaceAddress = NftUtils.parseAddress(CellBuilder.fromBoc(marketplaceAddressCell.getCell().getBytes()));

        TvmStackEntryCell nftAddressCell = (TvmStackEntryCell) result.getStack().get(1);
        Address nftAddress = NftUtils.parseAddress(CellBuilder.fromBoc(nftAddressCell.getCell().getBytes()));

        TvmStackEntryCell nftOwnerAddressCell = (TvmStackEntryCell) result.getStack().get(2);
        Address nftOwnerAddress = null;
        try {
            nftOwnerAddress = NftUtils.parseAddress(CellBuilder.fromBoc(nftOwnerAddressCell.getCell().getBytes()));
        } catch (Exception e) {
            //todo
        }

        TvmStackEntryNumber fullPriceNumber = (TvmStackEntryNumber) result.getStack().get(3);
        BigInteger fullPrice = fullPriceNumber.getNumber();

        TvmStackEntryNumber marketplaceFeeNumber = (TvmStackEntryNumber) result.getStack().get(4);
        BigInteger marketplaceFee = marketplaceFeeNumber.getNumber();

        TvmStackEntryCell royaltyAddressCell = (TvmStackEntryCell) result.getStack().get(5);
        Address royaltyAddress = NftUtils.parseAddress(CellBuilder.fromBoc(royaltyAddressCell.getCell().getBytes()));

        TvmStackEntryNumber royaltyAmountNumber = (TvmStackEntryNumber) result.getStack().get(6);
        BigInteger royaltyAmount = royaltyAmountNumber.getNumber();


        return NftSaleData.builder()
                .marketplaceAddress(marketplaceAddress)
                .nftAddress(nftAddress)
                .nftOwnerAddress(nftOwnerAddress)
                .fullPrice(fullPrice)
                .marketplaceFee(marketplaceFee)
                .royaltyAddress(royaltyAddress)
                .royaltyAmount(royaltyAmount)
                .build();
    }

    public static Cell createCancelBody(long queryId) {
        CellBuilder cell = CellBuilder.beginCell();
        cell.storeUint(3, 32); // cancel OP
        cell.storeUint(queryId, 64);
        return cell.endCell();
    }

    private byte[] buildSignature(TweetNaclFast.Signature.KeyPair keyPair, Cell stateInit, Cell msgBody) {
        Cell c = CellBuilder.beginCell()
                .storeRef(stateInit)
                .storeRef(msgBody)
                .endCell();
        return Utils.signData(keyPair.getPublicKey(), keyPair.getSecretKey(), c.hash());
    }

    /**
     * Deploys nft-sale smc to marketplaceAddress
     */
    public void deploy(Tonlib tonlib, WalletContract wallet, BigInteger msgValue, Address marketplaceAddress, TweetNaclFast.Signature.KeyPair keyPair) {

        long seqno = wallet.getSeqno(tonlib);

        Cell emptyBody = CellBuilder.beginCell().endCell();

        byte[] signature = buildSignature(keyPair, this.createStateInit().stateInit, emptyBody);

        CellBuilder body = CellBuilder.beginCell();
        body.storeUint(1, 32);
//        body.storeBytes(signature);
        body.storeCoins(msgValue);
        body.storeRef(this.createStateInit().stateInit);
        body.storeRef(emptyBody);

        ExternalMessage extMsg = wallet.createTransferMessage(
                keyPair.getSecretKey(),
                marketplaceAddress,
                msgValue,
                seqno,
                body.endCell()
        );

        tonlib.sendRawMessage(Utils.bytesToBase64(extMsg.message.toBoc()));
    }

    public void cancel(Tonlib tonlib, WalletContract wallet, BigInteger msgValue, Address saleAddress, long queryId, TweetNaclFast.Signature.KeyPair keyPair) {

        long seqno = wallet.getSeqno(tonlib);

        ExternalMessage extMsg = wallet.createTransferMessage(
                keyPair.getSecretKey(),
                saleAddress,
                msgValue,
                seqno,
                NftSale.createCancelBody(queryId)
        );

        tonlib.sendRawMessage(Utils.bytesToBase64(extMsg.message.toBoc()));
    }
}