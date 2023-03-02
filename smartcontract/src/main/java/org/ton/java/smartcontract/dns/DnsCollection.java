package org.ton.java.smartcontract.dns;

import com.iwebpp.crypto.TweetNaclFast;
import org.ton.java.address.Address;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.smartcontract.token.nft.NftUtils;
import org.ton.java.smartcontract.types.CollectionData;
import org.ton.java.smartcontract.types.ExternalMessage;
import org.ton.java.smartcontract.types.ItemData;
import org.ton.java.smartcontract.types.WalletCodes;
import org.ton.java.smartcontract.wallet.Contract;
import org.ton.java.smartcontract.wallet.Options;
import org.ton.java.smartcontract.wallet.WalletContract;
import org.ton.java.tonlib.Tonlib;
import org.ton.java.tonlib.types.RunResult;
import org.ton.java.tonlib.types.TvmStackEntryCell;
import org.ton.java.tonlib.types.TvmStackEntryNumber;
import org.ton.java.utils.Utils;

import java.math.BigInteger;
import java.util.ArrayDeque;
import java.util.Deque;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

public class DnsCollection implements Contract {

    //https://github.com/ton-blockchain/dns-contract/blob/main/func/nft-collection.fc
    Options options;
    Address address;

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
            options.code = Cell.fromBoc(WalletCodes.nftItem.getValue());
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
    public CollectionData getCollectionData(Tonlib tonlib) {
        Address myAddress = this.getAddress();

        RunResult result = tonlib.runMethod(myAddress, "get_collection_data");

        if (result.getExit_code() != 0) {
            throw new Error("method get_collection_data, returned an exit code " + result.getExit_code());
        }

        TvmStackEntryNumber nextItemIndexResult = (TvmStackEntryNumber) result.getStack().get(0);
        long nextItemIndex = nextItemIndexResult.getNumber().longValue();

        TvmStackEntryCell collectionContentResult = (TvmStackEntryCell) result.getStack().get(1); // cell or slice
        Cell collectionContent = Cell.fromBoc(Utils.base64ToBytes(collectionContentResult.getCell().getBytes()));
        String collectionContentUri = NftUtils.parseOffchainUriCell(collectionContent);

        return CollectionData.builder()
                .collectionContentUri(collectionContentUri)
                .collectionContentCell(collectionContent)
                .ownerAddress(null)
                .nextItemIndex(nextItemIndex) //always -1
                .build();
    }

    /**
     * @param nftItem DnsItem
     * @return NftItemInfo
     */
    public ItemData getNftItemContent(Tonlib tonlib, DnsItem nftItem) {
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

        if (result.getExit_code() != 0) {
            throw new Error("method get_nft_address_by_index, returned an exit code " + result.getExit_code());
        }

        TvmStackEntryCell addr = (TvmStackEntryCell) result.getStack().get(0);
        return NftUtils.parseAddress(CellBuilder.fromBoc(Utils.base64ToBytes(addr.getCell().getBytes())));
    }

    public Address getNftItemAddressByDomain(Tonlib tonlib, String domain) {
        CellBuilder cell = CellBuilder.beginCell();
        cell.storeString(domain);
        String cellHash = Utils.bytesToHex(cell.endCell().hash());
        return getNftItemAddressByIndex(tonlib, new BigInteger(cellHash, 16));
    }

    /**
     * @param domain    String e.g "sub.alice.ton"
     * @param category? String category of requested DNS record, null for all categories
     * @param oneStep?  boolean non-recursive
     * @return Cell | Address | AdnlAddress | null
     */
    public Object resolve(Tonlib tonlib, String domain, String category, boolean oneStep) {
        Address myAddress = this.getAddress();
        return DnsUtils.dnsResolve(tonlib, myAddress, domain, category, oneStep);
    }

    public void deploy(Tonlib tonlib, WalletContract wallet, BigInteger msgValue, TweetNaclFast.Signature.KeyPair keyPair) {

        long seqno = wallet.getSeqno(tonlib);

        CellBuilder payload = CellBuilder.beginCell();
        payload.storeUint(0x370fec51, 32); // op::fill_up, https://github.com/ton-blockchain/dns-contract/blob/main/func/dns-utils.fc
        payload.storeUint(0, 6);

        ExternalMessage extMsg = wallet.createTransferMessage(
                keyPair.getSecretKey(),
                this.getAddress(),
                msgValue,
                seqno,
                payload.endCell(),
                (byte) 3, //send mode
                this.createStateInit().stateInit
        );

        tonlib.sendRawMessage(Utils.bytesToBase64(extMsg.message.toBoc(false)));
    }
}
