package org.ton.java.smartcontract.token.nft;

import com.iwebpp.crypto.TweetNaclFast;
import org.ton.java.address.Address;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.smartcontract.types.*;
import org.ton.java.smartcontract.wallet.Contract;
import org.ton.java.smartcontract.wallet.Options;
import org.ton.java.smartcontract.wallet.WalletContract;
import org.ton.java.tonlib.Tonlib;
import org.ton.java.tonlib.types.RunResult;
import org.ton.java.tonlib.types.TvmStackEntryCell;
import org.ton.java.tonlib.types.TvmStackEntryNumber;
import org.ton.java.utils.Utils;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Deque;

import static java.util.Objects.isNull;

public class NftCollection implements Contract {
    // https://github.com/ton-blockchain/token-contract/blob/1ad314a98d20b41241d5329e1786fc894ad811de/nft/nft-collection.fc
    // not editable
    long royaltyBase = 1000;
    double royaltyFactor;
    Options options;
    Address address;

    /**
     * Creates editable NFT collection
     * required parameters:
     * ownerAddress: Address
     * collectionContentUri: String
     * collectionContentBaseUri: String
     * nftItemCodeHex: String
     * royalty: double
     * royaltyAddress: Address
     * address: Address | string
     * code: Cell
     *
     * @param options Options
     */
    public NftCollection(Options options) {
        this.options = options;
        this.options.wc = 0;

        if (options.royalty > 1) {
            throw new Error("royalty > 1");
        }

        royaltyFactor = Math.floor(options.royalty * royaltyBase);

        if (isNull(options.code)) {
            options.code = CellBuilder.beginCell().fromBoc(WalletCodes.nftCollection.getValue()).endCell();
        }
    }

    public String getName() {
        return "nftCollection";
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
     * @param collectionContentUri:     String
     * @param collectionContentBaseUri: String
     * @return Cell
     */
    private static Cell createContentCell(String collectionContentUri, String collectionContentBaseUri) {
        Cell collectionContentCell = NftUtils.createOffchainUriCell(collectionContentUri);
        CellBuilder commonContentCell = CellBuilder.beginCell();
        commonContentCell.storeBytes(collectionContentBaseUri.getBytes(StandardCharsets.UTF_8));

        CellBuilder contentCell = CellBuilder.beginCell();
        contentCell.storeRef(collectionContentCell);
        contentCell.storeRef(commonContentCell.endCell());
        return contentCell.endCell();
    }

    /**
     * @param royaltyAddress: Address
     * @return {Cell}
     */
    private static Cell createRoyaltyCell(Address royaltyAddress, double royaltyFactor, long royaltyBase) {
        CellBuilder royaltyCell = CellBuilder.beginCell();
        royaltyCell.storeUint((long) (royaltyFactor * 100), 16); // double to long
        royaltyCell.storeUint(royaltyBase, 16);
        royaltyCell.storeAddress(royaltyAddress);
        return royaltyCell.endCell();
    }

    /**
     * @return Cell cell contains nft data
     */
    @Override
    public Cell createDataCell() {
        CellBuilder cell = CellBuilder.beginCell();
        cell.storeAddress(options.adminAddress);
        cell.storeUint(0, 64); // next_item_index
        cell.storeRef(createContentCell(options.collectionContentUri, options.collectionContentBaseUri));
        cell.storeRef(CellBuilder.beginCell().fromBoc(options.nftItemCodeHex).endCell());
        cell.storeRef(createRoyaltyCell(options.royaltyAddress, options.royalty, royaltyBase));
        return cell.endCell();
    }

    public static Cell createMintBody(long queryId, BigInteger itemIndex, BigInteger amount, Address nftItemOwnerAddress, String nftItemContentUri) {
        CellBuilder body = CellBuilder.beginCell();
        body.storeUint(1, 32);  // OP deploy new nft
        body.storeUint(queryId, 64);
        body.storeUint(itemIndex, 64);
        body.storeCoins(amount);

        CellBuilder nftItemContent = CellBuilder.beginCell();
        nftItemContent.storeAddress(nftItemOwnerAddress);

        CellBuilder uriContent = CellBuilder.beginCell();
        uriContent.storeBytes(NftUtils.serializeUri(nftItemContentUri));

        nftItemContent.storeRef(uriContent.endCell());

        body.storeRef(nftItemContent.endCell());

        return body.endCell();
    }

    public static Cell createGetRoyaltyParamsBody(long queryId) {
        CellBuilder body = CellBuilder.beginCell();
        body.storeUint(0x693d3950, 32);  // OP deploy new nft
        body.storeUint(BigInteger.valueOf(queryId), 64);
        return body.endCell();
    }

    public static Cell createChangeOwnerBody(long queryId, Address newOwnerAddress) {

        if (isNull(newOwnerAddress)) {
            throw new Error("Specify newOwnerAddress");
        }
        CellBuilder body = CellBuilder.beginCell();
        body.storeUint(3, 32);
        body.storeUint(BigInteger.valueOf(queryId), 64);
        body.storeAddress(newOwnerAddress);
        return body.endCell();
    }


    /**
     * param
     * collectionContentUri: string
     * nftItemContentBaseUri: string
     * royalty: number
     * royaltyAddress: Address
     * queryId: number
     *
     * @return {Cell}
     */
    public static Cell createEditContentBody(long queryId, String collectionContentUri, String nftItemContentBaseUri, double royalty, Address royaltyAddress) {

        if (royalty > 1) {
            throw new Error("royalty > 1");
        }

        int royaltyBase = 1000;
        double royaltyFactor = Math.floor(royalty * royaltyBase);

        CellBuilder body = CellBuilder.beginCell();
        body.storeUint(4, 32); //OP
        body.storeUint(BigInteger.valueOf(queryId), 64);
        body.storeRef(createContentCell(collectionContentUri, nftItemContentBaseUri));
        body.storeRef(createRoyaltyCell(royaltyAddress, royaltyFactor, royaltyBase));

        return body.endCell();
    }

    /**
     * @return CollectionData
     */
    public CollectionData getCollectionData(Tonlib tonlib) {
        Address myAddress = this.getAddress();
        RunResult result = tonlib.runMethod(myAddress, "get_collection_data");

        if (result.getExit_code() != 0) {
            throw new Error("method get_collection_data, returned an exit code " + result.getExit_code());
        }

        TvmStackEntryNumber itemsCountNumber = (TvmStackEntryNumber) result.getStack().get(0);
        long nextItemIndex = itemsCountNumber.getNumber().longValue();


        TvmStackEntryCell collectionContent = (TvmStackEntryCell) result.getStack().get(1);
        Cell collectionContentCell = CellBuilder.beginCell().fromBoc(collectionContent.getCell().getBytes()).endCell();

        String collectionContentUri = null;
        try {
            collectionContentUri = NftUtils.parseOffchainUriCell(collectionContentCell);
        } catch (Error e) {
            //todo
        }

        TvmStackEntryCell ownerAddressCell = (TvmStackEntryCell) result.getStack().get(2);
        Address ownerAddress = NftUtils.parseAddress(CellBuilder.beginCell().fromBoc(ownerAddressCell.getCell().getBytes()).endCell());

        return CollectionData.builder()
                .nextItemIndex(nextItemIndex)
                .ownerAddress(ownerAddress)
                .collectionContentCell(collectionContentCell)
                .collectionContentUri(collectionContentUri)
                .build();
    }

    /**
     * @return DnsData
     */
    public ItemData getNftItemContent(Tonlib tonlib, NftItem nftItem) {
        Address myAddress = this.getAddress();
        ItemData nftData = nftItem.getData(tonlib);

        if (nftData.isInitialized()) {
            Deque<String> stack = new ArrayDeque<>();
            stack.offer("[num, " + nftData.getIndex() + "]");
            stack.offer("[slice, " + nftData.getContentCell().toHex(true) + "]");

            RunResult result = tonlib.runMethod(myAddress, "get_nft_content", stack);

            if (result.getExit_code() != 0) {
                throw new Error("method get_nft_content, returned an exit code " + result.getExit_code());
            }

            TvmStackEntryCell contentCell = (TvmStackEntryCell) result.getStack().get(2);
            Cell content = CellBuilder.beginCell().fromBoc(contentCell.getCell().getBytes()).endCell();

            try {
                nftData.setContentUri(NftUtils.parseOffchainUriCell(content));
            } catch (Error e) {
                //todo
            }
        }

        return nftData;
    }

    /**
     * @return DnsData
     */
    public Address getNftItemAddressByIndex(Tonlib tonlib, BigInteger index) {
        Address myAddress = this.getAddress();
        Deque<String> stack = new ArrayDeque<>();

        stack.offer("[num, " + index.toString(10) + "]");
        RunResult result = tonlib.runMethod(myAddress, "get_nft_address_by_index", stack);

        if (result.getExit_code() != 0) {
            throw new Error("method get_nft_address_by_index, returned an exit code " + result.getExit_code());
        }

        TvmStackEntryCell addrCell = (TvmStackEntryCell) result.getStack().get(0);
        return NftUtils.parseAddress(CellBuilder.beginCell().fromBoc(addrCell.getCell().getBytes()).endCell());
    }

    public Royalty getRoyaltyParams(Tonlib tonlib) {
        Address myAddress = this.getAddress();
        return NftUtils.getRoyaltyParams(tonlib, myAddress);
    }


    public void deploy(Tonlib tonlib, WalletContract wallet, BigInteger msgValue, TweetNaclFast.Signature.KeyPair keyPair) {

        long seqno = wallet.getSeqno(tonlib);

        Cell payload = null;

        ExternalMessage extMsg = wallet.createTransferMessage(
                keyPair.getSecretKey(),
                this.getAddress().toString(true, true, false),
                msgValue,
                seqno,
                payload,
                (byte) 3, //send mode
                this.createStateInit().stateInit
        );

        tonlib.sendRawMessage(Utils.bytesToBase64(extMsg.message.toBoc()));
    }
}
