package org.ton.java.smartcontract.token.nft;

import com.iwebpp.crypto.TweetNaclFast;
import org.ton.java.address.Address;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.smartcontract.types.CollectionData;
import org.ton.java.smartcontract.types.ExternalMessage;
import org.ton.java.smartcontract.types.ItemData;
import org.ton.java.smartcontract.types.Royalty;
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
    public static final String NFT_COLLECTION_CODE_HEX = "B5EE9C724102140100021F000114FF00F4A413F4BCF2C80B0102016202030202CD04050201200E0F04E7D10638048ADF000E8698180B8D848ADF07D201800E98FE99FF6A2687D20699FEA6A6A184108349E9CA829405D47141BAF8280E8410854658056B84008646582A802E78B127D010A65B509E58FE59F80E78B64C0207D80701B28B9E382F970C892E000F18112E001718112E001F181181981E0024060708090201200A0B00603502D33F5313BBF2E1925313BA01FA00D43028103459F0068E1201A44343C85005CF1613CB3FCCCCCCC9ED54925F05E200A6357003D4308E378040F4966FA5208E2906A4208100FABE93F2C18FDE81019321A05325BBF2F402FA00D43022544B30F00623BA9302A402DE04926C21E2B3E6303250444313C85005CF1613CB3FCCCCCCC9ED54002C323401FA40304144C85005CF1613CB3FCCCCCCC9ED54003C8E15D4D43010344130C85005CF1613CB3FCCCCCCC9ED54E05F04840FF2F00201200C0D003D45AF0047021F005778018C8CB0558CF165004FA0213CB6B12CCCCC971FB008002D007232CFFE0A33C5B25C083232C044FD003D0032C03260001B3E401D3232C084B281F2FFF2742002012010110025BC82DF6A2687D20699FEA6A6A182DE86A182C40043B8B5D31ED44D0FA40D33FD4D4D43010245F04D0D431D430D071C8CB0701CF16CCC980201201213002FB5DAFDA89A1F481A67FA9A9A860D883A1A61FA61FF480610002DB4F47DA89A1F481A67FA9A9A86028BE09E008E003E00B01A500C6E";

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
            options.code = Cell.fromBoc(NFT_COLLECTION_CODE_HEX);
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
        if (address == null) {
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
        contentCell.storeRef(commonContentCell);
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
        cell.storeRef(Cell.fromBoc(options.nftItemCodeHex));
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

        TvmStackEntryNumber itemsCountNumber = (TvmStackEntryNumber) result.getStackEntry().get(0);
        long nextItemIndex = itemsCountNumber.getNumber().longValue();


        TvmStackEntryCell collectionContent = (TvmStackEntryCell) result.getStackEntry().get(1);
        Cell collectionContentCell = CellBuilder.fromBoc(Utils.base64SafeUrlToBytes(collectionContent.getCell().getBytes()));

        String collectionContentUri = null;
        try {
            collectionContentUri = NftUtils.parseOffchainUriCell(collectionContentCell);
        } catch (Error e) {
            //todo
        }

        TvmStackEntryCell ownerAddressCell = (TvmStackEntryCell) result.getStackEntry().get(2);
        Address ownerAddress = NftUtils.parseAddress(CellBuilder.fromBoc(Utils.base64SafeUrlToBytes(ownerAddressCell.getCell().getBytes())));

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

            TvmStackEntryCell contentCell = (TvmStackEntryCell) result.getStackEntry().get(2);
            Cell content = CellBuilder.fromBoc(Utils.base64SafeUrlToBytes(contentCell.getCell().getBytes()));

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

        TvmStackEntryCell addrCell = (TvmStackEntryCell) result.getStackEntry().get(0);
        return NftUtils.parseAddress(CellBuilder.fromBoc(Utils.base64SafeUrlToBytes(addrCell.getCell().getBytes())));
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
                false, //dummy signature
                this.createStateInit().stateInit
        );

        tonlib.sendRawMessage(Utils.bytesToBase64(extMsg.message.toBoc(false)));
    }
}
