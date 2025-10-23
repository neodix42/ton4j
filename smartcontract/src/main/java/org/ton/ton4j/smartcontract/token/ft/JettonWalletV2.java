package org.ton.ton4j.smartcontract.token.ft;

import static java.util.Objects.nonNull;

import java.math.BigInteger;
import java.util.ArrayList;
import lombok.Builder;
import lombok.Getter;
import org.ton.ton4j.address.Address;
import org.ton.ton4j.adnl.AdnlLiteClient;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellBuilder;
import org.ton.ton4j.cell.CellSlice;
import org.ton.ton4j.smartcontract.token.nft.NftUtils;
import org.ton.ton4j.smartcontract.types.JettonWalletDataV2;
import org.ton.ton4j.smartcontract.types.WalletCodes;
import org.ton.ton4j.smartcontract.wallet.Contract;
import org.ton.ton4j.tl.liteserver.responses.RunMethodResult;
import org.ton.ton4j.tlb.VmCellSlice;
import org.ton.ton4j.toncenter.TonCenter;
import org.ton.ton4j.toncenter.model.RunGetMethodResponse;
import org.ton.ton4j.tonlib.Tonlib;
import org.ton.ton4j.tonlib.types.RunResult;
import org.ton.ton4j.tonlib.types.TvmStackEntryCell;
import org.ton.ton4j.tonlib.types.TvmStackEntryNumber;
import org.ton.ton4j.tonlib.types.TvmStackEntrySlice;
import org.ton.ton4j.utils.Utils;

@Builder
@Getter
public class JettonWalletV2 implements Contract {

  Address address;

  public static Cell CODE_CELL =
      CellBuilder.beginCell().fromBoc(WalletCodes.jettonWalletV2.getValue()).endCell();

  public static Cell CODE_LIB_CELL =
      CellBuilder.beginCell()
          .fromBoc(
              "b5ee9c7241010101002300084202ba2918c8947e9b25af9ac1b883357754173e5812f807a3d6e642a14709595395237ae3c3")
          .endCell();

  public static class JettonWalletV2Builder {}

  public static JettonWalletV2Builder builder() {
    return new CustomJettonWalletV2Builder();
  }

  private static class CustomJettonWalletV2Builder extends JettonWalletV2Builder {
    @Override
    public JettonWalletV2 build() {

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
    return "jettonWallet";
  }

  @Override
  public Cell createDataCell() {
    return CellBuilder.beginCell().endCell();
  }

  @Override
  public Cell createCodeCell() {
    return CellBuilder.beginCell().fromBoc(WalletCodes.jettonWalletV2.getValue()).endCell();
  }

  /**
   * @return Cell cell contains nft data
   */
  public static Cell createTransferBody(
      long queryId,
      BigInteger jettonAmount,
      Address toAddress,
      Address responseAddress,
      Cell customPayload,
      BigInteger forwardAmount,
      Cell forwardPayload) {
    return CellBuilder.beginCell()
        .storeUint(0xf8a7ea5, 32)
        .storeUint(queryId, 64) // default
        .storeCoins(jettonAmount)
        .storeAddress(toAddress)
        .storeAddress(responseAddress)
        .storeRefMaybe(customPayload)
        .storeCoins(forwardAmount) // default 0
        //        .storeBit(true)
        .storeRefMaybe(forwardPayload) // forward_payload in ref cell
        .endCell();
  }

  /**
   * @param queryId long
   * @param jettonAmount BigInteger
   * @param responseAddress Address
   */
  public static Cell createBurnBody(
      long queryId, BigInteger jettonAmount, Address responseAddress) {
    return CellBuilder.beginCell()
        .storeUint(0x595f07bc, 32) // burn up
        .storeUint(queryId, 64)
        .storeCoins(jettonAmount)
        .storeAddress(responseAddress)
        .endCell();
  }

  public JettonWalletDataV2 getData() {
    if (nonNull(tonCenterClient)) {
      try {
        // Use TonCenter API to get jetton wallet data
        RunGetMethodResponse response =
            tonCenterClient
                .runGetMethod(address.toBounceable(), "get_wallet_data", new ArrayList<>())
                .getResult();

        // Parse the response
        BigInteger balance =
            new BigInteger(
                ((String) new ArrayList<>(response.getStack().get(0)).get(1)).substring(2), 16);

        // Parse owner address from stack
        String ownerAddrHex = ((String) new ArrayList<>(response.getStack().get(1)).get(1));
        Address ownerAddress = Address.of(ownerAddrHex);

        // Parse jetton minter address from stack
        String minterAddrHex = ((String) new ArrayList<>(response.getStack().get(2)).get(1));
        Address jettonMinterAddress = Address.of(minterAddrHex);

        // Parse jetton wallet code from stack
        String codeHex = ((String) new ArrayList<>(response.getStack().get(3)).get(1));
        Cell jettonWalletCode =
            CellBuilder.beginCell().fromBoc(Utils.base64ToBytes(codeHex)).endCell();

        return JettonWalletDataV2.builder()
            .balance(balance)
            .ownerAddress(ownerAddress)
            .jettonMinterAddress(jettonMinterAddress)
            .jettonWalletCode(jettonWalletCode)
            .build();
      } catch (Exception e) {
        throw new Error("Error getting jetton wallet data: " + e.getMessage());
      }
    } else if (nonNull(adnlLiteClient)) {
      RunMethodResult runMethodResult = adnlLiteClient.runMethod(address, "get_wallet_data");
      BigInteger balance = runMethodResult.getIntByIndex(0);
      VmCellSlice slice = runMethodResult.getSliceByIndex(1);
      Address ownerAddress =
          CellSlice.beginParse(slice.getCell()).skipBits(slice.getStBits()).loadAddress();
      slice = runMethodResult.getSliceByIndex(2);
      Address jettonMinterAddress =
          CellSlice.beginParse(slice.getCell()).skipBits(slice.getStBits()).loadAddress();

      Cell jettonWalletCode = runMethodResult.getCellByIndex(3);
      return JettonWalletDataV2.builder()
          .balance(balance)
          .ownerAddress(ownerAddress)
          .jettonMinterAddress(jettonMinterAddress)
          .jettonWalletCode(jettonWalletCode)
          .build();
    }

    RunResult result = tonlib.runMethod(address, "get_wallet_data");

    if (result.getExit_code() != 0) {
      throw new Error("method get_wallet_data, returned an exit code " + result.getExit_code());
    }

    TvmStackEntryNumber balanceNumber = (TvmStackEntryNumber) result.getStack().get(0);
    BigInteger balance = balanceNumber.getNumber();

    TvmStackEntrySlice ownerAddr = (TvmStackEntrySlice) result.getStack().get(1);
    Address ownerAddress =
        NftUtils.parseAddress(
            CellBuilder.beginCell()
                .fromBoc(Utils.base64ToBytes(ownerAddr.getSlice().getBytes()))
                .endCell());

    TvmStackEntrySlice jettonMinterAddr = (TvmStackEntrySlice) result.getStack().get(2);
    Address jettonMinterAddress =
        NftUtils.parseAddress(
            CellBuilder.beginCell()
                .fromBoc(Utils.base64ToBytes(jettonMinterAddr.getSlice().getBytes()))
                .endCell());

    TvmStackEntryCell jettonWallet = (TvmStackEntryCell) result.getStack().get(3);
    Cell jettonWalletCode =
        CellBuilder.beginCell()
            .fromBoc(Utils.base64ToBytes(jettonWallet.getCell().getBytes()))
            .endCell();
    return JettonWalletDataV2.builder()
        .balance(balance)
        .ownerAddress(ownerAddress)
        .jettonMinterAddress(jettonMinterAddress)
        .jettonWalletCode(jettonWalletCode)
        .build();
  }

  public BigInteger getBalance() {
    if (nonNull(tonCenterClient)) {
      try {
        // Use TonCenter API to get jetton wallet balance
        RunGetMethodResponse response =
            tonCenterClient
                .runGetMethod(address.toBounceable(), "get_wallet_data", new ArrayList<>())
                .getResult();

        // Parse the balance from the response
        String balanceHex = ((String) new ArrayList<>(response.getStack().get(0)).get(1));
        return new BigInteger(balanceHex.substring(2), 16);
      } catch (Exception e) {
        throw new Error("Error getting jetton wallet balance: " + e.getMessage());
      }
    } else if (nonNull(adnlLiteClient)) {
      RunMethodResult runMethodResult = adnlLiteClient.runMethod(address, "get_wallet_data");
      return runMethodResult.getIntByIndex(0);
    }

    RunResult result = tonlib.runMethod(address, "get_wallet_data");

    if (result.getExit_code() != 0) {
      throw new Error("method get_wallet_data, returned an exit code " + result.getExit_code());
    }

    TvmStackEntryNumber balanceNumber = (TvmStackEntryNumber) result.getStack().get(0);
    return balanceNumber.getNumber();
  }

  public BigInteger getStatus() {
    if (nonNull(tonCenterClient)) {
      try {
        // Use TonCenter API to get jetton wallet balance
        RunGetMethodResponse response =
            tonCenterClient
                .runGetMethod(address.toBounceable(), "get_status", new ArrayList<>())
                .getResult();

        // Parse the balance from the response
        String balanceHex = ((String) new ArrayList<>(response.getStack().get(0)).get(1));
        return new BigInteger(balanceHex.substring(2), 16);
      } catch (Exception e) {
        throw new Error("Error getting jetton wallet status: " + e.getMessage());
      }
    } else if (nonNull(adnlLiteClient)) {
      RunMethodResult runMethodResult = adnlLiteClient.runMethod(address, "get_status");
      return runMethodResult.getIntByIndex(0);
    }

    RunResult result = tonlib.runMethod(address, "get_status");

    if (result.getExit_code() != 0) {
      throw new Error("method get_status, returned an exit code " + result.getExit_code());
    }

    TvmStackEntryNumber balanceNumber = (TvmStackEntryNumber) result.getStack().get(0);
    return balanceNumber.getNumber();
  }

  public static Cell packJettonWalletData(
      int status, BigInteger balance, Address ownerAddress, Address jettonMasterAddress) {
    return CellBuilder.beginCell()
        .storeUint(status, 4)
        .storeCoins(balance)
        .storeAddress(ownerAddress)
        .storeAddress(jettonMasterAddress)
        .endCell();
  }

  public static Cell calculateJettonWalletStateInit(
      Address ownerAddress, Address jettonMasterAddress, Cell jettonWalletCode) {
    return CellBuilder.beginCell()
        .storeUint(0, 2) // 0b00 - No split_depth; No special
        .storeRefMaybe(jettonWalletCode)
        .storeRefMaybe(packJettonWalletData(0, BigInteger.ZERO, ownerAddress, jettonMasterAddress))
        .storeUint(0, 1) // Empty libraries
        .endCell();
  }

  public static Cell calculateJettonWalletAddress(int wc, Cell stateInit) {
    return CellBuilder.beginCell()
        .storeUint(4, 3) // 0b100 = addr_std$10 tag; No anycast
        .storeInt(wc, 8)
        .storeBytes(stateInit.getHash())
        .endCell();
  }

  public static Address calculateUserJettonWalletAddress(
      int wc, Address ownerAddress, Address jettonMasterAddress, Cell jettonWalletCode) {
    Cell cell =
        calculateJettonWalletAddress(
            wc,
            calculateJettonWalletStateInit(ownerAddress, jettonMasterAddress, jettonWalletCode));
    CellSlice cs = CellSlice.beginParse(cell);
    cs.skipBits(3);
    return Address.of(Address.BOUNCEABLE_TAG, cs.loadUint(8).intValue(), cs.loadBytes(256));
  }
}
