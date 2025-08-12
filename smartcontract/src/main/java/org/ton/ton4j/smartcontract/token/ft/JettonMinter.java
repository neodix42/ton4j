package org.ton.ton4j.smartcontract.token.ft;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

import java.math.BigInteger;
import java.util.ArrayDeque;
import java.util.Deque;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.ton.ton4j.adnl.AdnlLiteClient;
import org.ton.ton4j.address.Address;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellBuilder;
import org.ton.ton4j.cell.CellSlice;
import org.ton.ton4j.smartcontract.token.nft.NftUtils;
import org.ton.ton4j.smartcontract.types.JettonMinterData;
import org.ton.ton4j.smartcontract.types.WalletCodes;
import org.ton.ton4j.smartcontract.wallet.Contract;
import org.ton.ton4j.tl.liteserver.responses.RunMethodResult;
import org.ton.ton4j.tlb.VmCellSlice;
import org.ton.ton4j.tlb.VmStackValueSlice;
import org.ton.ton4j.toncenter.TonCenter;
import org.ton.ton4j.tonlib.Tonlib;
import org.ton.ton4j.tonlib.types.RunResult;
import org.ton.ton4j.tonlib.types.TvmStackEntryCell;
import org.ton.ton4j.tonlib.types.TvmStackEntryNumber;
import org.ton.ton4j.tonlib.types.TvmStackEntrySlice;
import org.ton.ton4j.utils.Utils;

@Builder
@Getter
@Slf4j
public class JettonMinter implements Contract {
  Address adminAddress;
  Cell content;
  String jettonWalletCodeHex;
  Address customAddress;
  String code;

  public static class JettonMinterBuilder {}

  public static JettonMinterBuilder builder() {
    return new CustomJettonMinterBuilder();
  }

  private static class CustomJettonMinterBuilder extends JettonMinterBuilder {
    @Override
    public JettonMinter build() {
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
    return "jettonMinter";
  }

  /**
   * @return Cell cell - contains jetton data cell
   */
  @Override
  public Cell createDataCell() {
    if (StringUtils.isNotEmpty(code)) {
      return CellBuilder.beginCell()
          .storeCoins(BigInteger.ZERO)
          .storeAddress(adminAddress)
          .storeRef(content)
          .storeRef(CellBuilder.beginCell().fromBoc(code).endCell())
          .endCell();
    } else {
      return CellBuilder.beginCell()
          .storeCoins(BigInteger.ZERO)
          .storeAddress(adminAddress)
          .storeRef(content)
          .storeRef(CellBuilder.beginCell().fromBoc(jettonWalletCodeHex).endCell())
          .endCell();
    }
  }

  @Override
  public Cell createCodeCell() {
    if (StringUtils.isNotEmpty(code)) {
      log.info("Using custom JettonMinter");
      return CellBuilder.beginCell().fromBoc(code).endCell();
    }
    return CellBuilder.beginCell().fromBoc(WalletCodes.jettonMinter.getValue()).endCell();
  }

  /**
   * @param queryId long
   * @param destination Address
   * @param amount BigInteger
   * @param jettonAmount BigInteger
   * @param fromAddress Address
   * @param responseAddress Address
   * @param forwardTonAmount BigInteger
   * @return Cell
   */
  public static Cell createMintBody(
      long queryId,
      Address destination,
      BigInteger amount,
      BigInteger jettonAmount,
      Address fromAddress,
      Address responseAddress,
      BigInteger forwardTonAmount,
      Cell forwardPayload) {
    return CellBuilder.beginCell()
        .storeUint(21, 32) // OP mint
        .storeUint(queryId, 64) // query_id, default 0
        .storeAddress(destination)
        .storeCoins(amount)
        .storeRef(
            CellBuilder.beginCell()
                .storeUint(0x178d4519, 32) // internal_transfer op
                .storeUint(queryId, 64) // default 0
                .storeCoins(jettonAmount)
                .storeAddress(fromAddress) // from_address
                .storeAddress(responseAddress) // response_address
                .storeCoins(forwardTonAmount) // forward_ton_amount
                //                .storeBit(true) // store payload in the same cell
                .storeRefMaybe(forwardPayload) // forward payload
                .endCell())
        .endCell();
  }

  /**
   * @param queryId long
   * @param newAdminAddress Address
   * @return Cell
   */
  public Cell createChangeAdminBody(long queryId, Address newAdminAddress) {
    if (isNull(newAdminAddress)) {
      throw new Error("Specify newAdminAddress");
    }

    return CellBuilder.beginCell()
        .storeUint(3, 32) // OP
        .storeUint(queryId, 64) // query_id
        .storeAddress(newAdminAddress)
        .endCell();
  }

  /**
   * @param jettonContentUri String
   * @param queryId long
   * @return Cell
   */
  public Cell createEditContentBody(String jettonContentUri, long queryId) {
    return CellBuilder.beginCell()
        .storeUint(4, 32) // OP change content
        .storeUint(queryId, 64) // query_id
        .storeRef(NftUtils.createOffChainUriCell(jettonContentUri))
        .endCell();
  }

  /**
   * @return JettonData
   */
  public JettonMinterData getJettonData() {
    if (nonNull(tonCenterClient)) {
      try {
        org.ton.ton4j.toncenter.model.JettonMinterData data;
        if (nonNull(customAddress)) {
          data = tonCenterClient.getJettonData(customAddress.toBounceable());
        } else {
          data = tonCenterClient.getJettonData(getAddress().toBounceable());
        }
        return JettonMinterData.builder()
                .adminAddress(data.getAdminAddress())
                .isMutable(data.isMutable())
                .jettonContentCell(data.getJettonContentCell())
                .jettonContentUri(data.getJettonContentUri())
                .jettonWalletCode(data.getJettonWalletCode())
                .totalSupply(data.getTotalSupply())
                .build();
      } catch (Exception e) {
        throw new Error("Error getting jetton data: " + e.getMessage());
      }
    } else if (nonNull(adnlLiteClient)) {
      RunMethodResult runMethodResult;
      if (nonNull(customAddress)) {
        runMethodResult = adnlLiteClient.runMethod(customAddress, "get_jetton_data");
      } else {
        runMethodResult = adnlLiteClient.runMethod(getAddress(), "get_jetton_data");
      }

      BigInteger totalSupply = runMethodResult.getIntByIndex(0);
      boolean isMutable = runMethodResult.getIntByIndex(1).intValue() == -1;
      VmCellSlice slice = runMethodResult.getSliceByIndex(2);
      Address adminAddress =
          CellSlice.beginParse(slice.getCell()).skipBits(slice.getStBits()).loadAddress();

      Cell jettonContentCell = runMethodResult.getCellByIndex(3);
      String jettonContentUri = null;
      try {
        jettonContentUri = NftUtils.parseOffChainUriCell(jettonContentCell);
      } catch (Error e) {
        // todo
      }

      Cell jettonWalletCode = runMethodResult.getCellByIndex(4);

      return JettonMinterData.builder()
          .totalSupply(totalSupply)
          .isMutable(isMutable)
          .adminAddress(adminAddress)
          .jettonContentCell(jettonContentCell)
          .jettonContentUri(jettonContentUri)
          .jettonWalletCode(jettonWalletCode)
          .build();
    }

    RunResult result;
    if (nonNull(customAddress)) {
      result = tonlib.runMethod(customAddress, "get_jetton_data");
    } else {
      result = tonlib.runMethod(getAddress(), "get_jetton_data");
    }

    if (result.getExit_code() != 0) {
      throw new Error("method get_jetton_data, returned an exit code " + result.getExit_code());
    }

    TvmStackEntryNumber totalSupplyNumber = (TvmStackEntryNumber) result.getStack().get(0);
    BigInteger totalSupply = totalSupplyNumber.getNumber();

    boolean isMutable =
        ((TvmStackEntryNumber) result.getStack().get(1)).getNumber().longValue() == -1;

    TvmStackEntrySlice adminAddr = (TvmStackEntrySlice) result.getStack().get(2);
    Address adminAddress =
        NftUtils.parseAddress(
            CellBuilder.beginCell()
                .fromBoc(Utils.base64ToBytes(adminAddr.getSlice().getBytes()))
                .endCell());

    TvmStackEntryCell jettonContent = (TvmStackEntryCell) result.getStack().get(3);
    Cell jettonContentCell =
        CellBuilder.beginCell()
            .fromBoc(Utils.base64ToBytes(jettonContent.getCell().getBytes()))
            .endCell();
    String jettonContentUri = null;
    try {
      jettonContentUri = NftUtils.parseOffChainUriCell(jettonContentCell);
    } catch (Error e) {
      // todo
    }

    TvmStackEntryCell contentC = (TvmStackEntryCell) result.getStack().get(4);
    Cell jettonWalletCode =
        CellBuilder.beginCell()
            .fromBoc(Utils.base64ToBytes(contentC.getCell().getBytes()))
            .endCell();

    return JettonMinterData.builder()
        .totalSupply(totalSupply)
        .isMutable(isMutable)
        .adminAddress(adminAddress)
        .jettonContentCell(jettonContentCell)
        .jettonContentUri(jettonContentUri)
        .jettonWalletCode(jettonWalletCode)
        .build();
  }

  public BigInteger getTotalSupply() {
    if (nonNull(tonCenterClient)) {
      try {
        if (nonNull(customAddress)) {
          return tonCenterClient.getJettonData(customAddress.toBounceable()).getTotalSupply();
        } else {
          return tonCenterClient.getJettonData(getAddress().toBounceable()).getTotalSupply();
        }
      } catch (Exception e) {
        throw new Error("Error getting total supply: " + e.getMessage());
      }
    } else if (nonNull(adnlLiteClient)) {
      RunMethodResult runMethodResult;
      if (nonNull(customAddress)) {
        runMethodResult = adnlLiteClient.runMethod(customAddress, "get_jetton_data");
      } else {
        runMethodResult = adnlLiteClient.runMethod(getAddress(), "get_jetton_data");
      }
      return runMethodResult.getIntByIndex(0);
    }

    RunResult result;
    if (nonNull(customAddress)) {
      result = tonlib.runMethod(customAddress, "get_jetton_data");
    } else {
      result = tonlib.runMethod(getAddress(), "get_jetton_data");
    }
    if (result.getExit_code() != 0) {
      throw new Error("method get_jetton_data, returned an exit code " + result.getExit_code());
    }

    TvmStackEntryNumber totalSupplyNumber = (TvmStackEntryNumber) result.getStack().get(0);
    return totalSupplyNumber.getNumber();
  }

  /**
   * @param ownerAddress Address
   * @return Address user_jetton_wallet_address
   */
  public JettonWallet getJettonWallet(Address ownerAddress) {
    Cell cellAddr = CellBuilder.beginCell().storeAddress(ownerAddress).endCell();
    if (nonNull(tonCenterClient)) {
      try {
        Address jettonWalletAddress;
        if (nonNull(customAddress)) {
          jettonWalletAddress = tonCenterClient.getJettonWalletAddress(customAddress.toBounceable(), ownerAddress.toBounceable());
        } else {
          jettonWalletAddress = tonCenterClient.getJettonWalletAddress(getAddress().toBounceable(), ownerAddress.toBounceable());
        }
        
        return JettonWallet.builder()
            .tonCenterClient(tonCenterClient)
            .address(jettonWalletAddress)
            .build();
      } catch (Exception e) {
        throw new Error("Error getting jetton wallet address: " + e.getMessage());
      }
    } else if (nonNull(adnlLiteClient)) {
      RunMethodResult runMethodResult;
      if (nonNull(customAddress)) {
        runMethodResult =
            adnlLiteClient.runMethod(
                customAddress,
                "get_wallet_address",
                //                VmStackValueCell.builder().cell(cellAddr).build());
                VmStackValueSlice.builder()
                    .cell(VmCellSlice.builder().cell(cellAddr).build())
                    .build());
      } else {
        runMethodResult =
            adnlLiteClient.runMethod(
                getAddress(),
                "get_wallet_address",
                //                VmStackValueCell.builder().cell(cellAddr).build());
                VmStackValueSlice.builder()
                    .cell(VmCellSlice.builder().cell(cellAddr).build())
                    .build());
      }

      if (runMethodResult.getExitCode() != 0) {
        throw new Error(
            "method get_wallet_address returned an exit code " + runMethodResult.getExitCode());
      }
      VmCellSlice slice = runMethodResult.getSliceByIndex(0);
      //      System.out.println("adnl sliceBytes " + Utils.bytesToHex(slice.getCell().toBoc()));
      Address jettonWalletAddress =
          CellSlice.beginParse(slice.getCell()).skipBits(slice.getStBits()).loadAddress();

      return JettonWallet.builder()
          .adnlLiteClient(adnlLiteClient)
          .address(jettonWalletAddress)
          .build();
    }

    Deque<String> stack = new ArrayDeque<>();
    stack.offer("[slice, " + cellAddr.toHex(true) + "]");

    RunResult result;

    if (nonNull(customAddress)) {
      result = tonlib.runMethod(customAddress, "get_wallet_address", stack);
    } else {
      result = tonlib.runMethod(getAddress(), "get_wallet_address", stack);
    }

    if (result.getExit_code() != 0) {
      throw new Error("method get_wallet_address, returned an exit code " + result.getExit_code());
    }

    TvmStackEntrySlice addr = (TvmStackEntrySlice) result.getStack().get(0);
    byte[] sliceBytes = Utils.base64ToBytes(addr.getSlice().getBytes());
    //    System.out.println("tonlib sliceBytes " + Utils.bytesToHex(sliceBytes));
    Address jettonWalletAddress =
        NftUtils.parseAddress(CellBuilder.beginCell().fromBoc(sliceBytes).endCell());

    return JettonWallet.builder().tonlib(tonlib).address(jettonWalletAddress).build();
  }
}
