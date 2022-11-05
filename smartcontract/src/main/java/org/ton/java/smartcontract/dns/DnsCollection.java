package org.ton.java.smartcontract.dns;

import org.ton.java.address.Address;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.smartcontract.nft.NftUtils;
import org.ton.java.smartcontract.types.CollectionInfo;
import org.ton.java.smartcontract.types.DnsData;
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

public class DnsCollection implements Contract {

    Options options;
    Address address;

    /**
     * CollectionContent: Cell
     * dnsItemCodeHex: string
     * address: Address | string
     * code: Cell
     */
    public DnsCollection(Options options) {
        this.options = options;
        this.options.wc = 0;

        if (isNull(options.getCollectionContent()) && isNull(options.getAddress())) {
            throw new Error("Required collecntionContent cell");
        }
    }

    public String getName() {
        return "dnsCollection";
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
     * @return Cell cell contains dns collection data
     */
    @Override
    public Cell createDataCell() {
        CellBuilder cell = CellBuilder.beginCell();
        cell.storeRef(options.getCollectionContent());
        cell.storeRef(Cell.fromBoc(options.getDnsItemCodeHex()));
        return cell.endCell();
    }

    /**
     * @return CollectionInfo
     */
    public CollectionInfo getCollectionData(Tonlib tonlib) {
        Address myAddress = this.getAddress();

        RunResult result = tonlib.runMethod(myAddress, "get_collection_data");
        TvmStackEntryNumber nextItemIndexResult = (TvmStackEntryNumber) result.getStackEntry().get(0);
        long nextItemIndex = nextItemIndexResult.getNumber().longValue();

        TvmStackEntryCell collectionContentResult = (TvmStackEntryCell) result.getStackEntry().get(1); // cell or slice
        Cell collectionContent = Cell.fromBoc(Utils.base64SafeUrlToBytes(collectionContentResult.getCell().getBytes()));
//        Cell collectionContent = Cell.fromBoc(Utils.base64ToBytes(collectionContentResult.getCell().getBytes()));
        String collectionContentUri = NftUtils.parseOffchainUriCell(collectionContent);

        return CollectionInfo.builder()
                .collectionContentUri(collectionContentUri)
                .collectionContent(collectionContent)
                .ownerAddress(null)
                .nextItemIndex(nextItemIndex)
                .build();
    }

    /**
     * @param nftItem DnsItem
     * @return NftItemInfo
     */
    public DnsData getNftItemContent(Tonlib tonlib, DnsItem nftItem) {
        return nftItem.getData(tonlib);
    }

    /**
     * @param index BigInteger
     * @return Address
     */
    public Address getNftItemAddressByIndex(Tonlib tonlib, BigInteger index) {
        Address myAddress = this.getAddress();
        Deque<String> stack = new ArrayDeque<>();

        stack.offer("[num, " + index.toString() + "]");
        RunResult result = tonlib.runMethod(myAddress, "get_nft_address_by_index", stack);
        TvmStackEntrySlice addr = (TvmStackEntrySlice) result.getStackEntry().get(0); //cell or slice
        return NftUtils.parseAddress(CellBuilder.fromBoc(Utils.base64ToBytes(addr.getSlice().getBytes()))); // todo
    }

    /**
     * @param domain    String e.g "sub.alice.ton"
     * @param category? String category of requested DNS record, null for all categories
     * @param oneStep?  bboolean non-recursive
     * @return Cell | Address | AdnlAddress | null
     */
    public Object resolve(Tonlib tonlib, String domain, String category, boolean oneStep) {
        Address myAddress = this.getAddress();
        return DnsUtils.dnsResolve(tonlib, myAddress, domain, category, oneStep);
    }
}
