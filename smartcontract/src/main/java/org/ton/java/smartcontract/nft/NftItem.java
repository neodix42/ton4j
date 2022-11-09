package org.ton.java.smartcontract.nft;

import org.ton.java.address.Address;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.smartcontract.types.ItemData;
import org.ton.java.smartcontract.types.Royalty;
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

public class NftItem implements Contract {
    // https://github.com/ton-blockchain/token-contract/blob/1ad314a98d20b41241d5329e1786fc894ad811de/nft/nft-item.fc
    public static final String NFT_ITEM_CODE_HEX = "B5EE9C7241020D010001D0000114FF00F4A413F4BCF2C80B0102016202030202CE04050009A11F9FE00502012006070201200B0C02D70C8871C02497C0F83434C0C05C6C2497C0F83E903E900C7E800C5C75C87E800C7E800C3C00812CE3850C1B088D148CB1C17CB865407E90350C0408FC00F801B4C7F4CFE08417F30F45148C2EA3A1CC840DD78C9004F80C0D0D0D4D60840BF2C9A884AEB8C097C12103FCBC20080900113E910C1C2EBCB8536001F65135C705F2E191FA4021F001FA40D20031FA00820AFAF0801BA121945315A0A1DE22D70B01C300209206A19136E220C2FFF2E192218E3E821005138D91C85009CF16500BCF16712449145446A0708010C8CB055007CF165005FA0215CB6A12CB1FCB3F226EB39458CF17019132E201C901FB00104794102A375BE20A00727082108B77173505C8CBFF5004CF1610248040708010C8CB055007CF165005FA0215CB6A12CB1FCB3F226EB39458CF17019132E201C901FB000082028E3526F0018210D53276DB103744006D71708010C8CB055007CF165005FA0215CB6A12CB1FCB3F226EB39458CF17019132E201C901FB0093303234E25502F003003B3B513434CFFE900835D27080269FC07E90350C04090408F80C1C165B5B60001D00F232CFD633C58073C5B3327B5520BF75041B";

    Options options;
    Address address;

    /**
     * @param options Options
     */
    public NftItem(Options options) {
        this.options = options;

        if (nonNull(options.address)) {
            this.address = Address.of(options.address);
        }
        if (isNull(options.wc)) {
            options.wc = nonNull(this.address) ? this.address.wc : 0;
        }

        if (isNull(options.code)) {
            options.code = Cell.fromBoc(NFT_ITEM_CODE_HEX);
        }
    }

    public String getName() {
        return "nftItem";
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
     * @return Cell cell contains nft data
     */
    @Override
    public Cell createDataCell() {
        CellBuilder cell = CellBuilder.beginCell();
        cell.storeUint(options.index, 64);
        cell.storeAddress(options.collectionAddress);
        return cell.endCell();
    }


    /**
     * @return DnsData
     */
    public ItemData getData(Tonlib tonlib) {
        Address myAddress = this.getAddress();
        RunResult result = tonlib.runMethod(myAddress, "get_nft_data");

        if (result.getExit_code() != 0) {
            throw new Error("method get_nft_data, returned an exit code " + result.getExit_code());
        }

        TvmStackEntryNumber isInitializedNumber = (TvmStackEntryNumber) result.getStackEntry().get(0);
        boolean isInitialized = isInitializedNumber.getNumber().longValue() == -1;

        BigInteger index = ((TvmStackEntryNumber) result.getStackEntry().get(1)).getNumber(); // todo  https://github.com/toncenter/tonweb/blob/a821484df7c5f52c022f8c6864f039c7e0ec9b44/src/contract/token/nft/NftItem.js#L43

        TvmStackEntryCell collectionAddr = (TvmStackEntryCell) result.getStackEntry().get(2);
        Address collectionAddress = NftUtils.parseAddress(CellBuilder.fromBoc(Utils.base64SafeUrlToBytes(collectionAddr.getCell().getBytes())));

        TvmStackEntryCell ownerAddr = (TvmStackEntryCell) result.getStackEntry().get(3);
        Address ownerAddress = isInitialized ? NftUtils.parseAddress(CellBuilder.fromBoc(Utils.base64SafeUrlToBytes(ownerAddr.getCell().getBytes()))) : null;

        TvmStackEntryCell contentCell = (TvmStackEntryCell) result.getStackEntry().get(4);
        Cell cell = CellBuilder.fromBoc(Utils.base64SafeUrlToBytes(contentCell.getCell().getBytes()));

        String contentUri = null;
        try {
            if (isInitialized && nonNull(collectionAddress)) {
                contentUri = NftUtils.parseOffchainUriCell(cell);
            }
        } catch (Error e) {
            //todo
        }
        return ItemData.builder()
                .isInitialized(isInitialized)
                .index(index)
                .collectionAddress(collectionAddress)
                .ownerAddress(ownerAddress)
                .contentCell(cell)
                .contentUri(contentUri)
                .build();
    }


    /**
     * @param queryId         long optional, default 0
     * @param newOwnerAddress Address
     * @param forwardAmount   BigInteger optional, default 0
     * @param forwardPayload  byte[] optional, default null
     * @param responseAddress Address
     */
    public static Cell createTransferBody(long queryId, Address newOwnerAddress, BigInteger forwardAmount, byte[] forwardPayload, Address responseAddress) {
        CellBuilder cell = CellBuilder.beginCell();
        cell.storeUint(0x5fcc3d14, 32); // transfer op
        cell.storeUint(queryId, 64);
        cell.storeAddress(newOwnerAddress);
        cell.storeAddress(responseAddress);
        cell.storeBit(false); // null custom_payload
        cell.storeCoins(forwardAmount); //
        cell.storeBit(false); // forward_payload in this slice, not separate cell

        if (nonNull(forwardPayload)) {
            cell.bits.writeBytes(forwardPayload);
        }
        return cell.endCell();
    }

    /**
     * @param queryId long, default 0
     * @return Cell
     */
    public static Cell createGetStaticDataBody(long queryId) {
        CellBuilder body = CellBuilder.beginCell();
        body.storeUint(0x2fcb26a2, 32); // op::get_static_data() asm "0x2fcb26a2 PUSHINT";
        body.storeUint(queryId, 64); // query_id
        return body.endCell();
    }


    /**
     * for single nft without collection
     *
     * @return Roaylty
     */
    public Royalty getRoyaltyParams(Tonlib tonlib) {
        Address myAddress = this.getAddress();
        return NftUtils.getRoyaltyParams(tonlib, myAddress);
    }
}
