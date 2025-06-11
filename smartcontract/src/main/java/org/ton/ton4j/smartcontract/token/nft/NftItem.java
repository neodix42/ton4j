package org.ton.ton4j.smartcontract.token.nft;

import static java.util.Objects.nonNull;

import java.math.BigInteger;
import lombok.Builder;
import lombok.Getter;
import org.ton.java.adnl.AdnlLiteClient;
import org.ton.ton4j.address.Address;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellBuilder;
import org.ton.ton4j.smartcontract.types.ItemData;
import org.ton.ton4j.smartcontract.types.Royalty;
import org.ton.ton4j.smartcontract.types.WalletCodes;
import org.ton.ton4j.smartcontract.wallet.Contract;
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

  @Override
  public AdnlLiteClient getAdnlLiteClient() {
    return adnlLiteClient;
  }

  @Override
  public void setAdnlLiteClient(AdnlLiteClient pAdnlLiteClient) {
    adnlLiteClient = pAdnlLiteClient;
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
  public ItemData getData(Tonlib tonlib) {
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
    return NftUtils.getRoyaltyParams(tonlib, myAddress);
  }
}
