package org.ton.ton4j.smartcontract.token.nft;

import static java.util.Objects.isNull;

import lombok.Builder;
import lombok.Getter;
import org.ton.java.adnl.AdnlLiteClient;
import org.ton.ton4j.toncenter.TonCenter;
import org.ton.ton4j.address.Address;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellBuilder;
import org.ton.ton4j.smartcontract.wallet.Contract;
import org.ton.ton4j.tonlib.Tonlib;

@Builder
@Getter
public class NftMarketplace implements Contract {
  public static final String NFT_MARKETPLACE_CODE_HEX =
      "B5EE9C7241010401006D000114FF00F4A413F4BCF2C80B01020120020300AAD23221C700915BE0D0D3030171B0915BE0FA40ED44D0FA403012C705F2E19101D31F01C0018E2BFA003001D4D43021F90070C8CA07CBFFC9D077748018C8CB05CB0258CF165004FA0213CB6BCCCCC971FB00915BE20004F2308EF7CCE7";

  Address adminAddress;

  public static class NftMarketplaceBuilder {}

  public static NftMarketplaceBuilder builder() {
    return new CustomNftMarketplaceBuilder();
  }

  private static class CustomNftMarketplaceBuilder extends NftMarketplaceBuilder {
    @Override
    public NftMarketplace build() {
      if (isNull(super.adminAddress)) {
        throw new IllegalArgumentException("adminAddress parameter is mandatory.");
      }
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
    return "nftMarketplace";
  }

  /**
   * @return Cell cell contains nft marketplace data
   */
  @Override
  public Cell createDataCell() {
    return CellBuilder.beginCell().storeAddress(adminAddress).endCell();
  }

  @Override
  public Cell createCodeCell() {
    return CellBuilder.beginCell().fromBoc(NFT_MARKETPLACE_CODE_HEX).endCell();
  }
}
