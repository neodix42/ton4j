package org.ton.ton4j.smartcontract.dns;

import static java.util.Objects.nonNull;

import java.math.BigInteger;
import lombok.Builder;
import lombok.Getter;
import org.ton.java.adnl.AdnlLiteClient;
import org.ton.ton4j.toncenter.TonCenter;
import org.ton.ton4j.address.Address;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellBuilder;
import org.ton.ton4j.smartcontract.token.nft.NftUtils;
import org.ton.ton4j.smartcontract.types.AuctionInfo;
import org.ton.ton4j.smartcontract.types.ItemData;
import org.ton.ton4j.smartcontract.types.WalletCodes;
import org.ton.ton4j.smartcontract.wallet.Contract;
import org.ton.ton4j.tonlib.Tonlib;
import org.ton.ton4j.tonlib.types.RunResult;
import org.ton.ton4j.tonlib.types.TvmStackEntryCell;
import org.ton.ton4j.tonlib.types.TvmStackEntryNumber;
import org.ton.ton4j.tonlib.types.TvmStackEntrySlice;
import org.ton.ton4j.utils.Utils;

@Builder
@Getter
public class DnsItem implements Contract {

  // should be this https://github.com/ton-blockchain/dns-contract/blob/main/func/nft-item.fc
  // https://github.com/ton-blockchain/token-contract/blob/1ad314a98d20b41241d5329e1786fc894ad811de/nft/nft-item.fc
  BigInteger index;
  Address collectionAddress;

  private Tonlib tonlib;
  private long wc;
  private AdnlLiteClient adnlLiteClient;
  private TonCenter tonCenterClient;

  @Override
  public AdnlLiteClient getAdnlLiteClient() {
    return adnlLiteClient;
  }

  @Override
  public void setAdnlLiteClient(AdnlLiteClient pAdnlLiteClient) {
    adnlLiteClient = pAdnlLiteClient;
  }
  
  @Override
  public TonCenter getTonCenterClient() {
    return tonCenterClient;
  }

  @Override
  public void setTonCenterClient(TonCenter pTonCenterClient) {
    tonCenterClient = pTonCenterClient;
  }

  @Override
  public Tonlib getTonlib() {
    return tonlib;
  }

  @Override
  public void setTonlib(Tonlib pTonlib) {
    tonlib = pTonlib;
  }

  @Override
  public long getWorkchain() {
    return wc;
  }

  public static class DnsItemBuilder {}

  public static DnsItemBuilder builder() {
    return new CustomDnsItemBuilder();
  }

  private static class CustomDnsItemBuilder extends DnsItemBuilder {
    @Override
    public DnsItem build() {
      return super.build();
    }
  }

  public String getName() {
    return "dnsItem";
  }

  /**
   * @return Cell cell contains nft data
   */
  @Override
  public Cell createDataCell() {
    return CellBuilder.beginCell().storeUint(index, 256).storeAddress(collectionAddress).endCell();
  }

  @Override
  public Cell createCodeCell() {
    return CellBuilder.beginCell().fromBoc(WalletCodes.dnsItem.getValue()).endCell();
  }

  /**
   * @return DnsData
   */
  /**
   * Get NFT data using instance methods with TonCenter client if available
   * @return ItemData
   */
  public ItemData getData() {
    if (java.util.Objects.nonNull(tonCenterClient)) {
      try {
        // Use TonCenter API to get NFT data
        java.util.List<java.util.List<Object>> stack = new java.util.ArrayList<>();
        org.ton.ton4j.toncenter.model.RunGetMethodResponse response = 
            tonCenterClient.runGetMethod(getAddress().toBounceable(), "get_nft_data", stack).getResult();
        
        // Parse is_initialized
        boolean isInitialized = Long.parseLong(((String) new java.util.ArrayList<>(response.getStack().get(0)).get(1)).substring(2), 16) == -1;
        
        // Parse index
        BigInteger index = new BigInteger(((String) new java.util.ArrayList<>(response.getStack().get(1)).get(1)).substring(2), 16);
        
        // Parse collection address
        String collectionAddrHex = ((String) new java.util.ArrayList<>(response.getStack().get(2)).get(1));
        Address collectionAddress = Address.of(collectionAddrHex);
        
        // Parse owner address
        String ownerAddrHex = ((String) new java.util.ArrayList<>(response.getStack().get(3)).get(1));
        Address ownerAddress = isInitialized ? Address.of(ownerAddrHex) : null;
        
        // Parse content cell
        String contentCellHex = ((String) new java.util.ArrayList<>(response.getStack().get(4)).get(1));
        Cell contentCell = CellBuilder.beginCell().fromBoc(Utils.base64ToBytes(contentCellHex)).endCell();
        
        return ItemData.builder()
            .isInitialized(isInitialized)
            .index(index)
            .collectionAddress(collectionAddress)
            .ownerAddress(ownerAddress)
            .contentCell(contentCell)
            .build();
      } catch (Exception e) {
        throw new Error("Error getting NFT data: " + e.getMessage());
      }
    }
    
    // Fallback to Tonlib
    return getData(tonlib);
  }
  
  /**
   * Get NFT data using instance Tonlib
   * @param tonlib Tonlib instance
   * @return ItemData
   */
  public ItemData getData(Tonlib tonlib) {
    return getData(tonlib, getAddress());
  }
  
  /**
   * Static method to get NFT data
   * @param tonlib Tonlib instance
   * @param dnsItemAddress Address of the DNS item
   * @return ItemData
   */
  public static ItemData getData(Tonlib tonlib, Address dnsItemAddress) {
    // We can only use Tonlib here since we don't have access to TonCenter client
    // in the static method context
    RunResult result = tonlib.runMethod(dnsItemAddress, "get_nft_data");

    if (result.getExit_code() != 0) {
      throw new Error("method get_nft_data, returned an exit code " + result.getExit_code());
    }

    TvmStackEntryNumber isInitializedNumber = (TvmStackEntryNumber) result.getStack().get(0);
    boolean isInitialized = isInitializedNumber.getNumber().longValue() == -1;

    BigInteger index = ((TvmStackEntryNumber) result.getStack().get(1)).getNumber();

    TvmStackEntrySlice collectionAddr = (TvmStackEntrySlice) result.getStack().get(2);
    Address collectionAddress =
        NftUtils.parseAddress(
            CellBuilder.beginCell()
                .fromBoc(Utils.base64ToBytes(collectionAddr.getSlice().getBytes()))
                .endCell());

    TvmStackEntrySlice ownerAddr = (TvmStackEntrySlice) result.getStack().get(3);
    Address ownerAddress =
        isInitialized
            ? NftUtils.parseAddress(
                CellBuilder.beginCell()
                    .fromBoc(Utils.base64ToBytes(ownerAddr.getSlice().getBytes()))
                    .endCell())
            : null;

    TvmStackEntryCell contentC = (TvmStackEntryCell) result.getStack().get(4);
    Cell contentCell =
        CellBuilder.beginCell()
            .fromBoc(Utils.base64ToBytes(contentC.getCell().getBytes()))
            .endCell();

    return ItemData.builder()
        .isInitialized(isInitialized)
        .index(index)
        .collectionAddress(collectionAddress)
        .ownerAddress(ownerAddress)
        .contentCell(contentCell)
        .build();
  }

  /**
   * @param queryId long optional, default 0
   * @param newOwnerAddress Address
   * @param forwardAmount BigInteger optional, default 0
   * @param forwardPayload byte[] optional, default null
   * @param responseAddress Address
   */
  public static Cell createTransferBody(
      long queryId,
      Address newOwnerAddress,
      BigInteger forwardAmount,
      byte[] forwardPayload,
      Address responseAddress) {
    CellBuilder cell = CellBuilder.beginCell();
    cell.storeUint(0x5fcc3d14, 32); // transfer op
    cell.storeUint(queryId, 64);
    cell.storeAddress(newOwnerAddress);
    cell.storeAddress(responseAddress);
    cell.storeBit(false); // null custom_payload
    cell.storeCoins(forwardAmount); //
    cell.storeBit(false); // forward_payload in this slice, not separate cell

    if (nonNull(forwardPayload)) {
      cell.storeBytes(forwardPayload);
    }
    return cell.endCell();
  }

  /**
   * @param queryId long
   * @return Cell
   */
  public static Cell createStaticDataBody(long queryId) {
    return CellBuilder.beginCell()
        .storeUint(0x2fcb26a2, 32) // OP
        .storeUint(queryId, 64) // query_id
        .endCell();
  }

  /**
   * @return String
   */
  /**
   * Get domain using instance methods with TonCenter client if available
   * @return String
   */
  public String getDomain() {
    if (java.util.Objects.nonNull(tonCenterClient)) {
      try {
        // Use TonCenter API to get domain
        java.util.List<java.util.List<Object>> stack = new java.util.ArrayList<>();
        org.ton.ton4j.toncenter.model.RunGetMethodResponse response = 
            tonCenterClient.runGetMethod(getAddress().toBounceable(), "get_domain", stack).getResult();
        
        // Parse domain
        String domainCellHex = ((String) new java.util.ArrayList<>(response.getStack().get(0)).get(1));
        return new String(
            CellBuilder.beginCell()
                .fromBoc(Utils.base64ToBytes(domainCellHex))
                .endCell()
                .getBits()
                .toByteArray());
      } catch (Exception e) {
        throw new Error("Error getting domain: " + e.getMessage());
      }
    }
    
    // Fallback to Tonlib
    return getDomain(tonlib, getAddress());
  }
  
  /**
   * Static method to get domain
   * @param tonlib Tonlib instance
   * @param dnsItemAddress Address of the DNS item
   * @return String
   */
  public static String getDomain(Tonlib tonlib, Address dnsItemAddress) {
    // We can only use Tonlib here since we don't have access to TonCenter client
    // in the static method context
    RunResult result = tonlib.runMethod(dnsItemAddress, "get_domain");

    if (result.getExit_code() != 0) {
      throw new Error("method get_domain, returned an exit code " + result.getExit_code());
    }

    TvmStackEntrySlice domainCell = (TvmStackEntrySlice) result.getStack().get(0);
    return new String(
        CellBuilder.beginCell()
            .fromBoc(Utils.base64ToBytes(domainCell.getSlice().getBytes()))
            .endCell()
            .getBits()
            .toByteArray());
  }

  public static Address getEditor(Tonlib tonlib, Address dnsItemAddress) {
    RunResult result = tonlib.runMethod(dnsItemAddress, "get_editor");

    if (result.getExit_code() != 0) {
      throw new Error("method get_editor, returned an exit code " + result.getExit_code());
    }

    TvmStackEntrySlice editorCell = (TvmStackEntrySlice) result.getStack().get(0);
    return NftUtils.parseAddress(
        CellBuilder.beginCell()
            .fromBoc(Utils.base64ToBytes(editorCell.getSlice().getBytes()))
            .endCell());
  }

  /**
   * @return AuctionInfo
   */
  public static AuctionInfo getAuctionInfo(Tonlib tonlib, Address dnsItemAddress) {
    RunResult result = tonlib.runMethod(dnsItemAddress, "get_auction_info");

    if (result.getExit_code() != 0) {
      throw new Error("method get_auction_info, returned an exit code " + result.getExit_code());
    }

    TvmStackEntrySlice maxBidAddressCell = (TvmStackEntrySlice) result.getStack().get(0);
    Address maxBidAddress =
        NftUtils.parseAddress(
            CellBuilder.beginCell()
                .fromBoc(Utils.base64ToBytes(maxBidAddressCell.getSlice().getBytes()))
                .endCell());

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

  public static long getLastFillUpTime(Tonlib tonlib, Address dnsItemAddress) {
    RunResult result = tonlib.runMethod(dnsItemAddress, "get_last_fill_up_time");

    if (result.getExit_code() != 0) {
      throw new Error(
          "method get_last_fill_up_time, returned an exit code " + result.getExit_code());
    }

    TvmStackEntryNumber time = (TvmStackEntryNumber) result.getStack().get(0);
    return time.getNumber().longValue();
  }

  /**
   * @param domain String e.g "sub.alice.ton"
   * @param category String category of requested DNS record, null for all categories
   * @param oneStep boolean non-recursive
   * @return Cell | Address | AdnlAddress | null
   */
  public static Object resolve(
      Tonlib tonlib, String domain, String category, boolean oneStep, Address dnsItemAddress) {
    return DnsUtils.dnsResolve(tonlib, dnsItemAddress, domain, category, oneStep);
  }

  public static Object resolve(Tonlib tonlib, String domain, Address dnsItemAddress) {
    return DnsUtils.dnsResolve(tonlib, dnsItemAddress, domain, null, false);
  }

  /**
   * Creates op::change_dns_record = 0x4eb1f0f9; body request
   *
   * @param category String
   * @param value Cell
   * @param queryId long
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
