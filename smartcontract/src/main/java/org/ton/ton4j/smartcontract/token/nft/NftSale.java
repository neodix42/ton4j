package org.ton.ton4j.smartcontract.token.nft;

import java.math.BigInteger;
import lombok.Builder;
import lombok.Getter;
import org.ton.ton4j.adnl.AdnlLiteClient;
import org.ton.ton4j.toncenter.TonCenter;
import org.ton.ton4j.address.Address;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellBuilder;
import org.ton.ton4j.smartcontract.types.NftSaleData;
import org.ton.ton4j.smartcontract.wallet.Contract;
import org.ton.ton4j.tonlib.Tonlib;
import org.ton.ton4j.tonlib.types.RunResult;
import org.ton.ton4j.tonlib.types.TvmStackEntryNumber;
import org.ton.ton4j.tonlib.types.TvmStackEntrySlice;
import org.ton.ton4j.utils.Utils;

@Builder
@Getter
public class NftSale implements Contract {
  public static final String NFT_SALE_HEX_CODE =
      "B5EE9C7241020A010001B4000114FF00F4A413F4BCF2C80B01020120020302014804050004F2300202CD0607002FA03859DA89A1F481F481F481F401A861A1F401F481F4006101F7D00E8698180B8D8492F82707D201876A2687D207D207D207D006A18116BA4E10159C71D991B1B2990E382C92F837028916382F970FA01698FC1080289C6C8895D7970FAE99F98FD2018201A642802E78B2801E78B00E78B00FD016664F6AA701363804C9B081B2299823878027003698FE99F9810E000C92F857010C0801F5D41081DCD650029285029185F7970E101E87D007D207D0018384008646582A804E78B28B9D090D0A85AD08A500AFD010AE5B564B8FD80384008646582AC678B2803FD010B65B564B8FD80384008646582A802E78B00FD0109E5B564B8FD80381041082FE61E8A10C00C646582A802E78B117D010A65B509E58F8A40900C8C0029A3110471036454012F004E032363704C0038E4782103B9ACA0015BEF2E1C95312C70559C705B1F2E1CA702082105FCC3D14218010C8CB055006CF1622FA0215CB6A14CB1F14CB3F21CF1601CF16CA0021FA02CA00C98100A0FB00E05F06840FF2F0002ACB3F22CF1658CF16CA0021FA02CA00C98100A0FB00AECABAD1";

  Address marketplaceAddress;
  Address nftItemAddress;

  BigInteger fullPrice;
  BigInteger marketplaceFee;
  BigInteger royaltyAmount;
  Address royaltyAddress;

  /**
   *
   *
   * <pre>
   * marketplaceAddress nftAddress
   * fullPrice
   * marketplaceFee
   * royaltyAddress
   * royaltyAmount
   * </pre>
   */
  public static class NftSaleBuilder {}

  public static NftSaleBuilder builder() {
    return new CustomNftSaleBuilder();
  }

  private static class CustomNftSaleBuilder extends NftSaleBuilder {
    @Override
    public NftSale build() {
      return super.build();
    }
  }

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

  public String getName() {
    return "nftSale";
  }

  /**
   * @return Cell cell contains sale data
   */
  @Override
  public Cell createDataCell() {
    return CellBuilder.beginCell()
        .storeAddress(marketplaceAddress)
        .storeAddress(nftItemAddress)
        .storeAddress(null) // nft_owner_address
        .storeCoins(fullPrice)
        .storeRef(
            CellBuilder.beginCell() // fees cell
                .storeCoins(marketplaceFee)
                .storeAddress(royaltyAddress)
                .storeCoins(royaltyAmount)
                .endCell())
        .endCell();
  }

  @Override
  public Cell createCodeCell() {
    return CellBuilder.beginCell().fromBoc(NFT_SALE_HEX_CODE).endCell();
  }

  /**
   * Calls get_sale_data against nft-sale contract
   *
   * @return NftSaleData
   */
  public NftSaleData getData(Tonlib tonlib) {
    Address myAddress = this.getAddress();
    
    if (java.util.Objects.nonNull(tonCenterClient)) {
      try {
        // Use TonCenter API to get sale data
        java.util.List<java.util.List<Object>> stack = new java.util.ArrayList<>();
        org.ton.ton4j.toncenter.model.RunGetMethodResponse response = 
            tonCenterClient.runGetMethod(myAddress.toBounceable(), "get_sale_data", stack).getResult();
        
        // Parse marketplace address
        String marketplaceAddrHex = ((String) new java.util.ArrayList<>(response.getStack().get(0)).get(1));
        Address marketplaceAddress = Address.of(marketplaceAddrHex);
        
        // Parse NFT address
        String nftAddrHex = ((String) new java.util.ArrayList<>(response.getStack().get(1)).get(1));
        Address nftAddress = Address.of(nftAddrHex);
        
        // Parse NFT owner address
        String nftOwnerAddrHex = ((String) new java.util.ArrayList<>(response.getStack().get(2)).get(1));
        Address nftOwnerAddress = null;
        try {
          nftOwnerAddress = Address.of(nftOwnerAddrHex);
        } catch (Exception e) {
          // Owner address might be null
        }
        
        // Parse full price
        BigInteger fullPrice = new BigInteger(((String) new java.util.ArrayList<>(response.getStack().get(3)).get(1)).substring(2), 16);
        
        // Parse marketplace fee
        BigInteger marketplaceFee = new BigInteger(((String) new java.util.ArrayList<>(response.getStack().get(4)).get(1)).substring(2), 16);
        
        // Parse royalty address
        String royaltyAddrHex = ((String) new java.util.ArrayList<>(response.getStack().get(5)).get(1));
        Address royaltyAddress = Address.of(royaltyAddrHex);
        
        // Parse royalty amount
        BigInteger royaltyAmount = new BigInteger(((String) new java.util.ArrayList<>(response.getStack().get(6)).get(1)).substring(2), 16);
        
        return NftSaleData.builder()
            .marketplaceAddress(marketplaceAddress)
            .nftAddress(nftAddress)
            .nftOwnerAddress(nftOwnerAddress)
            .fullPrice(fullPrice)
            .marketplaceFee(marketplaceFee)
            .royaltyAddress(royaltyAddress)
            .royaltyAmount(royaltyAmount)
            .build();
      } catch (Exception e) {
        throw new Error("Error getting NFT sale data: " + e.getMessage());
      }
    } else if (java.util.Objects.nonNull(adnlLiteClient)) {
      // Implementation for AdnlLiteClient
      // Not implemented yet
      throw new Error("AdnlLiteClient implementation for getData not yet available");
    }
    
    // Fallback to Tonlib
    RunResult result = tonlib.runMethod(myAddress, "get_sale_data");

    if (result.getExit_code() != 0) {
      throw new Error("method get_sale_data, returned an exit code " + result.getExit_code());
    }

    TvmStackEntrySlice marketplaceAddressCell = (TvmStackEntrySlice) result.getStack().get(0);
    Address marketplaceAddress =
        NftUtils.parseAddress(
            CellBuilder.beginCell()
                .fromBoc(Utils.base64ToBytes(marketplaceAddressCell.getSlice().getBytes()))
                .endCell());

    TvmStackEntrySlice nftAddressCell = (TvmStackEntrySlice) result.getStack().get(1);
    Address nftAddress =
        NftUtils.parseAddress(
            CellBuilder.beginCell()
                .fromBoc(Utils.base64ToBytes(nftAddressCell.getSlice().getBytes()))
                .endCell());

    TvmStackEntrySlice nftOwnerAddressCell = (TvmStackEntrySlice) result.getStack().get(2);
    Address nftOwnerAddress = null;
    try {
      nftOwnerAddress =
          NftUtils.parseAddress(
              CellBuilder.beginCell()
                  .fromBoc(Utils.base64ToBytes(nftOwnerAddressCell.getSlice().getBytes()))
                  .endCell());
    } catch (Exception e) {
      // todo
    }

    TvmStackEntryNumber fullPriceNumber = (TvmStackEntryNumber) result.getStack().get(3);
    BigInteger fullPrice = fullPriceNumber.getNumber();

    TvmStackEntryNumber marketplaceFeeNumber = (TvmStackEntryNumber) result.getStack().get(4);
    BigInteger marketplaceFee = marketplaceFeeNumber.getNumber();

    TvmStackEntrySlice royaltyAddressCell = (TvmStackEntrySlice) result.getStack().get(5);
    Address royaltyAddress =
        NftUtils.parseAddress(
            CellBuilder.beginCell()
                .fromBoc(Utils.base64ToBytes(royaltyAddressCell.getSlice().getBytes()))
                .endCell());

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
}
