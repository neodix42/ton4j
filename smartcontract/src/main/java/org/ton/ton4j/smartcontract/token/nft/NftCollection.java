package org.ton.ton4j.smartcontract.token.nft;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import java.util.ArrayDeque;
import java.util.Deque;
import lombok.Builder;
import lombok.Getter;
import org.ton.java.adnl.AdnlLiteClient;
import org.ton.ton4j.toncenter.TonCenter;
import org.ton.ton4j.address.Address;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellBuilder;
import org.ton.ton4j.smartcontract.types.*;
import org.ton.ton4j.smartcontract.wallet.Contract;
import org.ton.ton4j.tlb.*;
import org.ton.ton4j.tonlib.Tonlib;
import org.ton.ton4j.tonlib.types.*;
import org.ton.ton4j.utils.Utils;

@Builder
@Getter
public class NftCollection implements Contract {
  // https://github.com/ton-blockchain/token-contract/blob/1ad314a98d20b41241d5329e1786fc894ad811de/nft/nft-collection.fc
  // not editable
  long royaltyBase; // default 1000
  double royaltyFactor;
  Address adminAddress;
  String collectionContentUri;
  String collectionContentBaseUri;
  String nftItemCodeHex;
  Double royalty;
  Address royaltyAddress;

  /**
   *
   *
   * <pre>
   * Creates editable NFT collection
   * required parameters:
   * ownerAddress: Address
   * collectionContentUri: String
   * collectionContentBaseUri: String
   * nftItemCodeHex: String
   * royalty: double
   * royaltyAddress: Address
   * address: Address | string
   * code: Cell
   * </pre>
   */
  public static class NftCollectionBuilder {}

  public static NftCollectionBuilder builder() {
    return new CustomNftCollectionBuilder();
  }

  private static class CustomNftCollectionBuilder extends NftCollectionBuilder {
    @Override
    public NftCollection build() {
      super.royaltyBase = 1000;
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
    return "nftCollection";
  }

  @Override
  public Cell createCodeCell() {
    return CellBuilder.beginCell().fromBoc(WalletCodes.nftCollection.getValue()).endCell();
  }

  /**
   * @param collectionContentUri: String
   * @param collectionContentBaseUri: String
   * @return Cell
   */
  private static Cell createContentCell(
      String collectionContentUri, String collectionContentBaseUri) {
    Cell collectionContentCell = NftUtils.createOffChainUriCell(collectionContentUri);
    CellBuilder commonContentCell = CellBuilder.beginCell();
    commonContentCell.storeBytes(collectionContentBaseUri.getBytes(StandardCharsets.UTF_8));

    CellBuilder contentCell = CellBuilder.beginCell();
    contentCell.storeRef(collectionContentCell);
    contentCell.storeRef(commonContentCell.endCell());
    return contentCell.endCell();
  }

  /**
   * @param royaltyAddress: Address
   * @return {Cell}
   */
  private static Cell createRoyaltyCell(
      Address royaltyAddress, double royaltyFactor, long royaltyBase) {
    CellBuilder royaltyCell = CellBuilder.beginCell();
    royaltyCell.storeUint((long) (royaltyFactor * 100), 16); // double to long
    royaltyCell.storeUint(royaltyBase, 16);
    royaltyCell.storeAddress(royaltyAddress);
    return royaltyCell.endCell();
  }

  /**
   * @return Cell cell contains nft data
   */
  @Override
  public Cell createDataCell() {
    CellBuilder cell = CellBuilder.beginCell();
    cell.storeAddress(adminAddress);
    cell.storeUint(0, 64); // next_item_index
    cell.storeRef(createContentCell(collectionContentUri, collectionContentBaseUri));
    cell.storeRef(CellBuilder.beginCell().fromBoc(nftItemCodeHex).endCell());
    cell.storeRef(createRoyaltyCell(royaltyAddress, royalty, royaltyBase));
    return cell.endCell();
  }

  public static Cell createMintBody(
      long queryId,
      long itemIndex,
      BigInteger amount,
      Address nftItemOwnerAddress,
      String nftItemContentUri) {
    CellBuilder body = CellBuilder.beginCell();
    body.storeUint(1, 32); // OP deploy new nft
    body.storeUint(queryId, 64);
    body.storeUint(itemIndex, 64);
    body.storeCoins(amount);

    CellBuilder nftItemContent = CellBuilder.beginCell();
    nftItemContent.storeAddress(nftItemOwnerAddress);

    CellBuilder uriContent = CellBuilder.beginCell();
    uriContent.storeBytes(NftUtils.serializeUri(nftItemContentUri));

    nftItemContent.storeRef(uriContent.endCell());

    body.storeRef(nftItemContent.endCell());

    return body.endCell();
  }

  public static Cell createGetRoyaltyParamsBody(long queryId) {
    CellBuilder body = CellBuilder.beginCell();
    body.storeUint(0x693d3950, 32); // OP deploy new nft
    body.storeUint(BigInteger.valueOf(queryId), 64);
    return body.endCell();
  }

  public static Cell createChangeOwnerBody(long queryId, Address newOwnerAddress) {

    if (isNull(newOwnerAddress)) {
      throw new Error("Specify newOwnerAddress");
    }
    CellBuilder body = CellBuilder.beginCell();
    body.storeUint(3, 32);
    body.storeUint(BigInteger.valueOf(queryId), 64);
    body.storeAddress(newOwnerAddress);
    return body.endCell();
  }

  /**
   * param collectionContentUri: string nftItemContentBaseUri: string royalty: number
   * royaltyAddress: Address queryId: number
   *
   * @return {Cell}
   */
  public static Cell createEditContentBody(
      long queryId,
      String collectionContentUri,
      String nftItemContentBaseUri,
      double royalty,
      Address royaltyAddress) {

    if (royalty > 1) {
      throw new Error("royalty > 1");
    }

    int royaltyBase = 1000;
    double royaltyFactor = Math.floor(royalty * royaltyBase);

    CellBuilder body = CellBuilder.beginCell();
    body.storeUint(4, 32); // OP
    body.storeUint(BigInteger.valueOf(queryId), 64);
    body.storeRef(createContentCell(collectionContentUri, nftItemContentBaseUri));
    body.storeRef(createRoyaltyCell(royaltyAddress, royaltyFactor, royaltyBase));

    return body.endCell();
  }

  /**
   * @return CollectionData
   */
  public CollectionData getCollectionData() {
    Address myAddress = this.getAddress();
    
    if (nonNull(tonCenterClient)) {
      try {
        // Use TonCenter API to get collection data
        List<List<Object>> stack = new ArrayList<>();
        org.ton.ton4j.toncenter.model.RunGetMethodResponse response = 
            tonCenterClient.runGetMethod(myAddress.toBounceable(), "get_collection_data", stack).getResult();
        
        // Parse the response
        long nextItemIndex = Long.parseLong(((String) new ArrayList<>(response.getStack().get(0)).get(1)).substring(2), 16);
        
        // Parse collection content cell
        String contentCellHex = ((String) new ArrayList<>(response.getStack().get(1)).get(1));
        Cell collectionContentCell = CellBuilder.beginCell().fromBoc(Utils.base64ToBytes(contentCellHex)).endCell();
        
        String collectionContentUri = null;
        try {
          collectionContentUri = NftUtils.parseOffChainUriCell(collectionContentCell);
        } catch (Error e) {
          // todo
        }
        
        // Parse owner address
        String ownerAddrHex = ((String) new ArrayList<>(response.getStack().get(2)).get(1));
        Address ownerAddress = Address.of(ownerAddrHex);
        
        return CollectionData.builder()
            .nextItemIndex(nextItemIndex)
            .ownerAddress(ownerAddress)
            .collectionContentCell(collectionContentCell)
            .collectionContentUri(collectionContentUri)
            .build();
      } catch (Exception e) {
        throw new Error("Error getting collection data: " + e.getMessage());
      }
    } else if (nonNull(adnlLiteClient)) {
      // Implementation for AdnlLiteClient
      // This would be similar to the TonCenter implementation but using adnlLiteClient.runMethod
      // Not implemented yet
      throw new Error("AdnlLiteClient implementation for getCollectionData not yet available");
    }
    
    // Fallback to Tonlib
    RunResult result = tonlib.runMethod(myAddress, "get_collection_data");

    if (result.getExit_code() != 0) {
      throw new Error("method get_collection_data, returned an exit code " + result.getExit_code());
    }

    TvmStackEntryNumber itemsCountNumber = (TvmStackEntryNumber) result.getStack().get(0);
    long nextItemIndex = itemsCountNumber.getNumber().longValue();

    TvmStackEntryCell collectionContent = (TvmStackEntryCell) result.getStack().get(1);
    Cell collectionContentCell =
        CellBuilder.beginCell()
            .fromBoc(Utils.base64ToBytes(collectionContent.getCell().getBytes()))
            .endCell();

    String collectionContentUri = null;
    try {
      collectionContentUri = NftUtils.parseOffChainUriCell(collectionContentCell);
    } catch (Error e) {
      // todo
    }

    TvmStackEntrySlice ownerAddressCell = (TvmStackEntrySlice) result.getStack().get(2);
    Address ownerAddress =
        NftUtils.parseAddress(
            CellBuilder.beginCell()
                .fromBoc(Utils.base64ToBytes(ownerAddressCell.getSlice().getBytes()))
                .endCell());

    return CollectionData.builder()
        .nextItemIndex(nextItemIndex)
        .ownerAddress(ownerAddress)
        .collectionContentCell(collectionContentCell)
        .collectionContentUri(collectionContentUri)
        .build();
  }

  public ItemData getNftItemContent(NftItem nftItem) {
    ItemData nftData;
    
    if (nonNull(tonCenterClient)) {
      try {
        // For now, we'll use tonlib since getData(TonCenter) is not implemented
        nftData = nftItem.getData(); // todo
      } catch (Exception e) {
        throw new Error("Error getting NFT data: " + e.getMessage());
      }
    } else if (nonNull(adnlLiteClient)) {
      // For now, we'll use tonlib since getData(AdnlLiteClient) is not implemented
      nftData = nftItem.getData(); // todo
    } else {
      nftData = nftItem.getData();
    }

    if (nftData.isInitialized()) {
      if (nonNull(tonCenterClient)) {
        try {
          // Use TonCenter API to get NFT content
          List<List<Object>> stack = new ArrayList<>();
          stack.add(Arrays.asList("num", nftData.getIndex().toString()));
          
          // Convert content cell to hex and add to stack
          List<Object> contentCellParam = new ArrayList<>();
          contentCellParam.add("slice");
          contentCellParam.add(nftData.getContentCell().toHex(true));
          stack.add(contentCellParam);
          
          org.ton.ton4j.toncenter.model.RunGetMethodResponse response = 
              tonCenterClient.runGetMethod(getAddress().toBounceable(), "get_nft_content", stack).getResult();
          
          // Parse content cell from response
          String contentCellHex = ((String) new ArrayList<>(response.getStack().get(2)).get(1));
          Cell content = CellBuilder.beginCell().fromBoc(Utils.base64ToBytes(contentCellHex)).endCell();
          
          try {
            nftData.setContentUri(NftUtils.parseOffChainUriCell(content));
          } catch (Error e) {
            // todo
          }
          
          return nftData;
        } catch (Exception e) {
          throw new Error("Error getting NFT content: " + e.getMessage());
        }
      } else if (nonNull(adnlLiteClient)) {
        // Implementation for AdnlLiteClient
        // Not implemented yet
        throw new Error("AdnlLiteClient implementation for getNftItemContent not yet available");
      }
      
      // Fallback to Tonlib
      Deque<String> stack = new ArrayDeque<>();
      stack.offer("[num, " + nftData.getIndex() + "]");
      stack.offer("[slice, " + nftData.getContentCell().toHex(true) + "]");

      RunResult result = tonlib.runMethod(getAddress(), "get_nft_content", stack);

      if (result.getExit_code() != 0) {
        throw new Error("method get_nft_content, returned an exit code " + result.getExit_code());
      }

      TvmStackEntryCell contentCell = (TvmStackEntryCell) result.getStack().get(2);
      Cell content =
          CellBuilder.beginCell()
              .fromBoc(Utils.base64ToBytes(contentCell.getCell().getBytes()))
              .endCell();

      try {
        nftData.setContentUri(NftUtils.parseOffChainUriCell(content));
      } catch (Error e) {
        // todo
      }
    }

    return nftData;
  }

  public Address getNftItemAddressByIndex(BigInteger index) {
    Address myAddress = this.getAddress();
    
    if (nonNull(tonCenterClient)) {
      try {
        // Use TonCenter API to get NFT address by index
        List<List<Object>> stack = new ArrayList<>();
        stack.add(Arrays.asList("num", index.toString(10)));
        
        org.ton.ton4j.toncenter.model.RunGetMethodResponse response = 
            tonCenterClient.runGetMethod(myAddress.toBounceable(), "get_nft_address_by_index", stack).getResult();
        
        // Parse address from response
        String addrHex = ((String) new ArrayList<>(response.getStack().get(0)).get(1));
        return Address.of(addrHex);
      } catch (Exception e) {
        throw new Error("Error getting NFT address by index: " + e.getMessage());
      }
    } else if (nonNull(adnlLiteClient)) {
      // Implementation for AdnlLiteClient
      // Not implemented yet
      throw new Error("AdnlLiteClient implementation for getNftItemAddressByIndex not yet available");
    }
    
    // Fallback to Tonlib
    Deque<String> stack = new ArrayDeque<>();
    stack.offer("[num, " + index.toString(10) + "]");
    RunResult result = tonlib.runMethod(myAddress, "get_nft_address_by_index", stack);

    if (result.getExit_code() != 0) {
      throw new Error(
          "method get_nft_address_by_index, returned an exit code " + result.getExit_code());
    }

    TvmStackEntrySlice addrCell = (TvmStackEntrySlice) result.getStack().get(0);
    return NftUtils.parseAddress(
        CellBuilder.beginCell()
            .fromBoc(Utils.base64ToBytes(addrCell.getSlice().getBytes()))
            .endCell());
  }

  public Royalty getRoyaltyParams() {
    Address myAddress = this.getAddress();
    
    if (nonNull(tonCenterClient)) {
      try {
        // Use TonCenter API to get royalty params
        List<List<Object>> stack = new ArrayList<>();
        org.ton.ton4j.toncenter.model.RunGetMethodResponse response = 
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


  public ExtMessageInfo deploy(NftCollectionConfig config) {
    long seqno = getSeqno();
    config.setSeqno(seqno);
    Address ownAddress = getAddress();

    Message externalMessage =
        Message.builder()
            .info(
                ExternalMessageInInfo.builder()
                    .srcAddr(MsgAddressExtNone.builder().build())
                    .dstAddr(
                        MsgAddressIntStd.builder()
                            .workchainId(ownAddress.wc)
                            .address(ownAddress.toBigInteger())
                            .build())
                    .build())
            .init(getStateInit())
            .build();

    if (nonNull(tonCenterClient)) {
      try {
        // Send via TonCenter and return a success message
        tonCenterClient.sendBoc(externalMessage.toCell().toBase64());
        return ExtMessageInfo.builder()
            .error(TonlibError.builder().code(0).build()) // success
            .build();
      } catch (Exception e) {
        throw new Error("Error deploying NFT collection: " + e.getMessage());
      }
    } else if (nonNull(adnlLiteClient)) {
      // This is a placeholder - actual implementation would depend on AdnlLiteClient's API
      throw new Error("AdnlLiteClient implementation for deploy not yet available");
    }
    
    return tonlib.sendRawMessage(externalMessage.toCell().toBase64());
  }
  
  public ExtMessageInfo deploy(Tonlib tonlib, NftCollectionConfig config) {

    return deploy(config);
  }
}
