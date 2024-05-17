package org.ton.java.smartcontract.dns;

import com.iwebpp.crypto.TweetNaclFast;
import org.ton.java.address.Address;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.smartcontract.token.nft.NftUtils;
import org.ton.java.smartcontract.types.CollectionData;
import org.ton.java.smartcontract.types.DnsCollectionConfig;
import org.ton.java.smartcontract.types.ItemData;
import org.ton.java.smartcontract.types.WalletCodes;
import org.ton.java.smartcontract.wallet.Contract;
import org.ton.java.smartcontract.wallet.Options;
import org.ton.java.tonlib.Tonlib;
import org.ton.java.tonlib.types.RunResult;
import org.ton.java.tonlib.types.TvmStackEntryCell;
import org.ton.java.tonlib.types.TvmStackEntryNumber;
import org.ton.java.tonlib.types.TvmStackEntrySlice;
import org.ton.java.utils.Utils;

import java.math.BigInteger;
import java.util.ArrayDeque;
import java.util.Deque;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

public class DnsCollection implements Contract<DnsCollectionConfig> {

    //https://github.com/ton-blockchain/dns-contract/blob/main/func/nft-collection.fc
    Options options;
    Address address;

    TweetNaclFast.Signature.KeyPair keyPair;

    /**
     * Options
     * collectionContent: Cell
     * dnsItemCodeHex String
     * address: Address String
     */
    public DnsCollection(Options options) {
        this.options = options;
        this.options.wc = 0;

        if (nonNull(options.address)) {
            this.address = Address.of(options.address);
        }

        if (isNull(options.code)) {
            options.code = CellBuilder.beginCell().fromBoc(WalletCodes.nftItem.getValue()).endCell();
        }

        if (isNull(options.getCollectionContent()) && isNull(options.getAddress())) {
            throw new Error("Required collectionContent cell");
        }
    }

    public String getName() {
        return "dnsCollection";
    }

    @Override
    public Options getOptions() {
        return options;
    }

    /**
     * @return Cell cell contains dns collection data
     */
    @Override
    public Cell createDataCell() {
        return CellBuilder.beginCell()
                .storeRef(options.getCollectionContent())
                .storeRef(CellBuilder.beginCell().fromBoc(options.getDnsItemCodeHex()).endCell())
                .endCell();
    }

    @Override
    public Cell createCodeCell() {
        return CellBuilder.beginCell().
                fromBoc(WalletCodes.dnsCollection.getValue()).
                endCell();
    }

    /**
     * @return CollectionInfo
     */
    public static CollectionData getCollectionData(Tonlib tonlib, Address dnsCollectionAddress) {
        Address myAddress = dnsCollectionAddress;

        RunResult result = tonlib.runMethod(myAddress, "get_collection_data");

        if (result.getExit_code() != 0) {
            throw new Error("method get_collection_data, returned an exit code " + result.getExit_code());
        }

        TvmStackEntryNumber nextItemIndexResult = (TvmStackEntryNumber) result.getStack().get(0);
        long nextItemIndex = nextItemIndexResult.getNumber().longValue();

        TvmStackEntryCell collectionContentResult = (TvmStackEntryCell) result.getStack().get(1); // cell or slice
        Cell collectionContent = CellBuilder.beginCell().fromBoc(Utils.base64ToBytes(collectionContentResult.getCell().getBytes())).endCell();
        String collectionContentUri = NftUtils.parseOffchainUriCell(collectionContent);

        return CollectionData.builder()
                .collectionContentUri(collectionContentUri)
                .collectionContentCell(collectionContent)
                .ownerAddress(null)
                .nextItemIndex(nextItemIndex) //always -1
                .build();
    }

    public static ItemData getNftItemContent(Tonlib tonlib, Address dnsItemAddress) {
        return DnsItem.getData(tonlib, dnsItemAddress);
    }

    /**
     * @param index BigInteger
     * @return Address
     */
    public static Address getNftItemAddressByIndex(Tonlib tonlib, Address collectionAddress, BigInteger index) {
        Address myAddress = collectionAddress;
        Deque<String> stack = new ArrayDeque<>();

        stack.offer("[num, " + index.toString() + "]");
        RunResult result = tonlib.runMethod(myAddress, "get_nft_address_by_index", stack);

        if (result.getExit_code() != 0) {
            throw new Error("method get_nft_address_by_index, returned an exit code " + result.getExit_code());
        }

        TvmStackEntrySlice addr = (TvmStackEntrySlice) result.getStack().get(0);
        return NftUtils.parseAddress(CellBuilder.beginCell().fromBoc(Utils.base64ToBytes(addr.getSlice().getBytes())).endCell());
    }

    public static Address getNftItemAddressByDomain(Tonlib tonlib, Address dnsCollectionAddress, String domain) {
        CellBuilder cell = CellBuilder.beginCell();
        cell.storeString(domain);
        String cellHash = Utils.bytesToHex(cell.endCell().hash());
        return getNftItemAddressByIndex(tonlib, dnsCollectionAddress, new BigInteger(cellHash, 16));
    }

    /**
     * @param domain    String e.g "sub.alice.ton"
     * @param category? String category of requested DNS record, null for all categories
     * @param oneStep?  boolean non-recursive
     * @return Cell | Address | AdnlAddress | null
     */
    public static Object resolve(Tonlib tonlib, Address dnsCollectionAddress, String domain, String category, boolean oneStep) {
        Address myAddress = dnsCollectionAddress;
        return DnsUtils.dnsResolve(tonlib, myAddress, domain, category, oneStep);
    }

//    @Override
////    public ExtMessageInfo deploy(Tonlib tonlib, Contract wallet, BigInteger msgValue, TweetNaclFast.Signature.KeyPair keyPair) {
//    public ExtMessageInfo deploy(Tonlib tonlib, DnsCollectionConfig config) {
//
//        long seqno = this.getSeqno(tonlib);
//
//        Cell payload = CellBuilder.beginCell()
//                .storeUint(0x370fec51, 32) // op::fill_up, https://github.com/ton-blockchain/dns-contract/blob/main/func/dns-utils.fc
//                .storeUint(0, 6)
//                .endCell();
//
//        Message extMsg = MsgUtils.createExternalMessageWithSignedBody(config., getAddress(), false, payload);
//
//        return tonlib.sendRawMessage(extMsg.toCell().toBase64());
//    }
}
