package org.ton.java.smartcontract.dns;

import org.ton.java.address.Address;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.smartcontract.token.nft.NftUtils;
import org.ton.java.smartcontract.types.AuctionInfo;
import org.ton.java.smartcontract.types.ItemData;
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

public class DnsItem implements Contract {

    // should be this https://github.com/ton-blockchain/dns-contract/blob/main/func/nft-item.fc
    // https://github.com/ton-blockchain/token-contract/blob/1ad314a98d20b41241d5329e1786fc894ad811de/nft/nft-item.fc
    Options options;
    Address address;

    /**
     * @param options Options - index, collectionAddress
     */
    public DnsItem(Options options) {
        this.options = options;
        if (nonNull(options.address)) {
            this.address = Address.of(options.address);
        }
        if (options.wc == 0) {
            options.wc = nonNull(this.address) ? this.address.wc : 0;
        }
        if (isNull(options.code)) {
            options.code = Cell.fromBoc(WalletCodes.nftItem.getValue());
        }
    }

    public String getName() {
        return "dnsItem";
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
     * @return Cell cell contains nft data
     */
    @Override
    public Cell createDataCell() {
        CellBuilder cell = CellBuilder.beginCell();
        cell.storeUint(new BigInteger(options.index, 16), 256);
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

        TvmStackEntryNumber isInitializedNumber = (TvmStackEntryNumber) result.getStack().get(0);
        boolean isInitialized = isInitializedNumber.getNumber().longValue() == -1;

        BigInteger index = ((TvmStackEntryNumber) result.getStack().get(1)).getNumber();

        TvmStackEntryCell collectionAddr = (TvmStackEntryCell) result.getStack().get(2);
        Address collectionAddress = NftUtils.parseAddress(CellBuilder.fromBoc(collectionAddr.getCell().getBytes()));

        TvmStackEntryCell ownerAddr = (TvmStackEntryCell) result.getStack().get(3);
        Address ownerAddress = isInitialized ? NftUtils.parseAddress(CellBuilder.fromBoc(ownerAddr.getCell().getBytes())) : null;

        TvmStackEntryCell contentC = (TvmStackEntryCell) result.getStack().get(4);
        Cell contentCell = CellBuilder.fromBoc(contentC.getCell().getBytes());

        return ItemData.builder()
                .isInitialized(isInitialized)
                .index(index)
                .collectionAddress(collectionAddress)
                .ownerAddress(ownerAddress)
                .contentCell(contentCell)
                .build();
    }

    /**
     * @param queryId         long optional, default 0
     * @param newOwnerAddress Address
     * @param forwardAmount   BigInteger optional, default 0
     * @param forwardPayload  byte[] optional, default null
     * @param responseAddress Address
     */
    public Cell createTransferBody(long queryId, Address newOwnerAddress, BigInteger forwardAmount, byte[] forwardPayload, Address responseAddress) {
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
     * @param queryId long
     * @return Cell
     */
    public Cell createStaticDataBody(long queryId) {
        CellBuilder body = CellBuilder.beginCell();
        body.storeUint(0x2fcb26a2, 32); // OP
        body.storeUint(queryId, 64); // query_id
        return body.endCell();
    }

    /**
     * @return String
     */
    public String getDomain(Tonlib tonlib) {
        Address myAddress = this.getAddress();
        RunResult result = tonlib.runMethod(myAddress, "get_domain");

        if (result.getExit_code() != 0) {
            throw new Error("method get_domain, returned an exit code " + result.getExit_code());
        }

        TvmStackEntryCell domainCell = (TvmStackEntryCell) result.getStack().get(0);
        return new String(CellBuilder.fromBoc(domainCell.getCell().getBytes()).bits.toByteArray());
    }

    public Address getEditor(Tonlib tonlib) {
        Address myAddress = this.getAddress();
        RunResult result = tonlib.runMethod(myAddress, "get_editor");

        if (result.getExit_code() != 0) {
            throw new Error("method get_editor, returned an exit code " + result.getExit_code());
        }

        TvmStackEntryCell editorCell = (TvmStackEntryCell) result.getStack().get(0);
        return NftUtils.parseAddress(CellBuilder.fromBoc(editorCell.getCell().getBytes()));
    }

    /**
     * @return AuctionInfo
     */
    public AuctionInfo getAuctionInfo(Tonlib tonlib) {
        Address myAddress = this.getAddress();
        RunResult result = tonlib.runMethod(myAddress, "get_auction_info");

        if (result.getExit_code() != 0) {
            throw new Error("method get_auction_info, returned an exit code " + result.getExit_code());
        }

        TvmStackEntryCell maxBidAddressCell = (TvmStackEntryCell) result.getStack().get(0);
        Address maxBidAddress = NftUtils.parseAddress(CellBuilder.fromBoc(maxBidAddressCell.getCell().getBytes()));

        TvmStackEntryNumber maxBidAmountNumber = (TvmStackEntryNumber) result.getStack().get(1);
        BigInteger maxBidAmount = maxBidAmountNumber.getNumber();

        TvmStackEntryNumber auctionEndTimeNumber = (TvmStackEntryNumber) result.getStack().get(2);
        long auctionEndTime = auctionEndTimeNumber.getNumber().longValue();

        return AuctionInfo.builder()
                .maxBidAddress(maxBidAddress)
                .maxBidAmount(maxBidAmount)
                .auctionEndTime(auctionEndTime)
                .build();
    }

    public long getLastFillUpTime(Tonlib tonlib) {
        Address myAddress = this.getAddress();
        RunResult result = tonlib.runMethod(myAddress, "get_last_fill_up_time");

        if (result.getExit_code() != 0) {
            throw new Error("method get_last_fill_up_time, returned an exit code " + result.getExit_code());
        }

        TvmStackEntryNumber time = (TvmStackEntryNumber) result.getStack().get(0);
        return time.getNumber().longValue();
    }

    /**
     * @param domain   String e.g "sub.alice.ton"
     * @param category String category of requested DNS record, null for all categories
     * @param oneStep  boolean non-recursive
     * @return Cell | Address | AdnlAddress | null
     */
    public Object resolve(Tonlib tonlib, String domain, String category, boolean oneStep) {
        Address myAddress = this.getAddress();
        return DnsUtils.dnsResolve(tonlib, myAddress, domain, category, oneStep);
    }

    public Object resolve(Tonlib tonlib, String domain) {
        Address myAddress = this.getAddress();
        return DnsUtils.dnsResolve(tonlib, myAddress, domain, null, false);
    }

    /**
     * Creates op::change_dns_record = 0x4eb1f0f9; body request
     *
     * @param category String
     * @param value:   Cell
     * @param queryId  long
     * @return Cell
     */
    public static Cell createChangeContentEntryBody(String category, Cell value, long queryId) {
        CellBuilder body = CellBuilder.beginCell();
        body.storeUint(0x4eb1f0f9, 32); // op::change_dns_record = 0x4eb1f0f9;
        body.storeUint(queryId, 64); // query_id
        body.storeUint(DnsUtils.categoryToInt(category), 256);
        if (nonNull(value)) {
            body.storeRef(value);
        }
        return body.endCell();
    }
}