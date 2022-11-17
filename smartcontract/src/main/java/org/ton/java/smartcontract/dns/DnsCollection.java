package org.ton.java.smartcontract.dns;

import com.iwebpp.crypto.TweetNaclFast;
import org.ton.java.address.Address;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.smartcontract.token.nft.NftUtils;
import org.ton.java.smartcontract.types.CollectionData;
import org.ton.java.smartcontract.types.ExternalMessage;
import org.ton.java.smartcontract.types.ItemData;
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
    public static final String DNS_COLLECTION_CODE_HEX = "B5EE9C7241020D010001D0000114FF00F4A413F4BCF2C80B0102016202030202CE04050009A11F9FE00502012006070201200B0C02D70C8871C02497C0F83434C0C05C6C2497C0F83E903E900C7E800C5C75C87E800C7E800C3C00812CE3850C1B088D148CB1C17CB865407E90350C0408FC00F801B4C7F4CFE08417F30F45148C2EA3A1CC840DD78C9004F80C0D0D0D4D60840BF2C9A884AEB8C097C12103FCBC20080900113E910C1C2EBCB8536001F65135C705F2E191FA4021F001FA40D20031FA00820AFAF0801BA121945315A0A1DE22D70B01C300209206A19136E220C2FFF2E192218E3E821005138D91C85009CF16500BCF16712449145446A0708010C8CB055007CF165005FA0215CB6A12CB1FCB3F226EB39458CF17019132E201C901FB00104794102A375BE20A00727082108B77173505C8CBFF5004CF1610248040708010C8CB055007CF165005FA0215CB6A12CB1FCB3F226EB39458CF17019132E201C901FB000082028E3526F0018210D53276DB103744006D71708010C8CB055007CF165005FA0215CB6A12CB1FCB3F226EB39458CF17019132E201C901FB0093303234E25502F003003B3B513434CFFE900835D27080269FC07E90350C04090408F80C1C165B5B60001D00F232CFD633C58073C5B3327B5520BF75041B";
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
            options.code = Cell.fromBoc(DNS_COLLECTION_CODE_HEX);
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

        TvmStackEntryNumber nextItemIndexResult = (TvmStackEntryNumber) result.getStackEntry().get(0);
        long nextItemIndex = nextItemIndexResult.getNumber().longValue();

        TvmStackEntryCell collectionContentResult = (TvmStackEntryCell) result.getStackEntry().get(1); // cell or slice
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

        TvmStackEntryCell addr = (TvmStackEntryCell) result.getStackEntry().get(0);
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
                false, //dummy signature
                this.createStateInit().stateInit
        );

        tonlib.sendRawMessage(Utils.bytesToBase64(extMsg.message.toBoc(false)));
    }
}
