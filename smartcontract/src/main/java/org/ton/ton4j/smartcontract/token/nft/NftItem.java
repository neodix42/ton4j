package org.ton.ton4j.smartcontract.token.nft;

import static java.util.Objects.nonNull;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import lombok.Builder;
import lombok.Getter;
import org.ton.ton4j.adnl.AdnlLiteClient;
import org.ton.ton4j.toncenter.TonCenter;
import org.ton.ton4j.address.Address;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellBuilder;
import org.ton.ton4j.smartcontract.types.ItemData;
import org.ton.ton4j.smartcontract.types.Royalty;
import org.ton.ton4j.smartcontract.types.WalletCodes;
import org.ton.ton4j.smartcontract.wallet.Contract;
import org.ton.ton4j.toncenter.model.RunGetMethodResponse;
import org.ton.ton4j.tonlib.Tonlib;
import org.ton.ton4j.tonlib.types.RunResult;
import org.ton.ton4j.tonlib.types.TvmStackEntryNumber;
import org.ton.ton4j.tonlib.types.TvmStackEntrySlice;

@Builder
@Getter
public class NftItem implements Contract {
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

  public static class NftItemBuilder {}

  public static NftItemBuilder builder() {
    return new CustomNftItemBuilder();
  }

  private static class CustomNftItemBuilder extends NftItemBuilder {
    @Override
    public NftItem build() {
      return super.build();
    }
  }

  public String getName() {
    return "nftItem";
  }

  /**
   * @return Cell cell contains nft data
   */
  @Override
  public Cell createDataCell() {
    return CellBuilder.beginCell().storeUint(index, 64).storeAddress(collectionAddress).endCell();
  }

  @Override
  public Cell createCodeCell() {
    return CellBuilder.beginCell().fromBoc(WalletCodes.nftItem.getValue()).endCell();
  }

  /**
   * @return DnsData
   */
  public ItemData getData() {
    if (nonNull(tonCenterClient)) {
      try {
        // Use TonCenter API to get NFT data
        RunGetMethodResponse response =
            tonCenterClient.runGetMethod(getAddress().toBounceable(), "get_nft_data", new ArrayList<>()).getResult();
        
        // Parse the response
        boolean isInitialized = "0xffffffffffffffff".equals(((String) new ArrayList<>(response.getStack().get(0)).get(1)));
        
        // Parse index
        String indexHex = ((String) new ArrayList<>(response.getStack().get(1)).get(1));
        BigInteger index = new BigInteger(indexHex.substring(2), 16);
        
        // Parse collection address
        String collectionAddrHex = ((String) new ArrayList<>(response.getStack().get(2)).get(1));
        Address collectionAddress = Address.of(collectionAddrHex);
        
        // Parse owner address
        String ownerAddrHex = ((String) new ArrayList<>(response.getStack().get(3)).get(1));
        Address ownerAddress = isInitialized ? Address.of(ownerAddrHex) : null;
        
        // Parse content cell
        String contentCellHex = ((String) new ArrayList<>(response.getStack().get(4)).get(1));
        Cell cell = CellBuilder.beginCell().fromBoc(org.ton.ton4j.utils.Utils.base64ToBytes(contentCellHex)).endCell();
        
        String contentUri = null;
        try {
          if (isInitialized && nonNull(collectionAddress)) {
            contentUri = NftUtils.parseOffChainUriCell(cell);
          }
        } catch (Error e) {
          // todo
        }
        
        return ItemData.builder()
            .isInitialized(isInitialized)
            .index(index)
            .collectionAddress(collectionAddress)
            .ownerAddress(ownerAddress)
            .contentCell(cell)
            .contentUri(contentUri)
            .build();
      } catch (Exception e) {
        throw new Error("Error getting NFT data: " + e.getMessage());
      }
    } else if (nonNull(adnlLiteClient)) {
      // Implementation for AdnlLiteClient
      // Not implemented yet
      throw new Error("AdnlLiteClient implementation for getData not yet available");
    }
    
    // Fallback to Tonlib
    RunResult result = tonlib.runMethod(getAddress(), "get_nft_data");

    if (result.getExit_code() != 0) {
      throw new Error("method get_nft_data, returned an exit code " + result.getExit_code());
    }

    TvmStackEntryNumber isInitializedNumber = (TvmStackEntryNumber) result.getStack().get(0);
    boolean isInitialized = isInitializedNumber.getNumber().longValue() == -1;

    BigInteger index = ((TvmStackEntryNumber) result.getStack().get(1)).getNumber();

    TvmStackEntrySlice collectionAddressSlice = (TvmStackEntrySlice) result.getStack().get(2);
    Address collectionAddress =
        NftUtils.parseAddress(
            CellBuilder.beginCell()
                .fromBoc(collectionAddressSlice.getSlice().getBytes())
                .endCell());

    TvmStackEntrySlice ownerAddressSlice = (TvmStackEntrySlice) result.getStack().get(3);
    Address ownerAddress =
        isInitialized
            ? NftUtils.parseAddress(
                CellBuilder.beginCell().fromBoc(ownerAddressSlice.getSlice().getBytes()).endCell())
            : null;

    TvmStackEntrySlice contentCell = (TvmStackEntrySlice) result.getStack().get(4);
    Cell cell = CellBuilder.beginCell().fromBoc(contentCell.getSlice().getBytes()).endCell();

    String contentUri = null;
    try {
      if (isInitialized && nonNull(collectionAddress)) {
        contentUri = NftUtils.parseOffChainUriCell(cell);
      }
    } catch (Error e) {
      // todo
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
   * @param queryId BigInteger optional, default 0
   * @param newOwnerAddress Address
   * @param forwardAmount BigInteger optional, default 0
   * @param forwardPayload byte[] optional, default null
   * @param responseAddress Address
   */
  public static Cell createTransferBody(
      BigInteger queryId,
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
    cell.storeCoins(forwardAmount);
    cell.storeBit(false); // forward_payload in this slice, not separate cell
    if (nonNull(forwardPayload)) {
      cell.storeBytes(forwardPayload);
    }
    return cell.endCell();
  }

  /**
   * @param queryId long, default 0
   * @return Cell
   */
  public static Cell createGetStaticDataBody(BigInteger queryId) {
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
    
    if (nonNull(tonCenterClient)) {
      try {
        // Use TonCenter API to get royalty params
        List<List<Object>> stack = new ArrayList<>();
        RunGetMethodResponse response =
            tonCenterClient.runGetMethod(myAddress.toBounceable(), "royalty_params", stack).getResult();
        
        // Parse royalty numerator
        long royaltyNumerator = Long.parseLong(((String) new ArrayList<>(response.getStack().get(0)).get(1)).substring(2), 16);
        
        // Parse royalty denominator
        long royaltyDenominator = Long.parseLong(((String) new ArrayList<>(response.getStack().get(1)).get(1)).substring(2), 16);
        
        // Parse royalty address
        String royaltyAddrHex = ((String) new ArrayList<>(response.getStack().get(2)).get(1));
        Address royaltyAddress = Address.of(royaltyAddrHex);
        
        return Royalty.builder()
            .royaltyFactor(BigInteger.valueOf(royaltyNumerator))
            .royaltyBase(BigInteger.valueOf(royaltyDenominator))
            .royaltyAddress(royaltyAddress)
            .build();
      } catch (Exception e) {
        throw new Error("Error getting royalty params: " + e.getMessage());
      }
    } else if (nonNull(adnlLiteClient)) {
      // Implementation for AdnlLiteClient
      // Not implemented yet
      throw new Error("AdnlLiteClient implementation for getRoyaltyParams not yet available");
    }
    
    // Fallback to Tonlib
    return NftUtils.getRoyaltyParams(tonlib, myAddress);
  }
}
