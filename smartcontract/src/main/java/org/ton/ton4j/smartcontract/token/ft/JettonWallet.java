package org.ton.ton4j.smartcontract.token.ft;

import static java.util.Objects.nonNull;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import lombok.Builder;
import lombok.Getter;
import org.ton.ton4j.adnl.AdnlLiteClient;
import org.ton.ton4j.address.Address;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellBuilder;
import org.ton.ton4j.cell.CellSlice;
import org.ton.ton4j.smartcontract.token.nft.NftUtils;
import org.ton.ton4j.smartcontract.types.JettonWalletData;
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
public class JettonWallet implements Contract {

  Address address;

  public static class JettonWalletBuilder {}

  public static JettonWalletBuilder builder() {
    return new CustomJettonWalletBuilder();
  }

  private static class CustomJettonWalletBuilder extends JettonWalletBuilder {
    @Override
    public JettonWallet build() {

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
    return CellBuilder.beginCell().fromBoc(WalletCodes.jettonWallet.getValue()).endCell();
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

  public JettonWalletData getData() {
    if (nonNull(tonCenterClient)) {
      try {
        // Use TonCenter API to get jetton wallet data
        RunGetMethodResponse response =
            tonCenterClient
                .runGetMethod(address.toBounceable(), "get_wallet_data", new ArrayList<>())
                .getResult();

        // Parse the response
        BigInteger balance = new BigInteger(((String) new ArrayList<>(response.getStack().get(0)).get(1)).substring(2), 16);
        
        // Parse owner address from stack
        String ownerAddrHex = ((String) new ArrayList<>(response.getStack().get(1)).get(1));
        Address ownerAddress = Address.of(ownerAddrHex);
        
        // Parse jetton minter address from stack
        String minterAddrHex = ((String) new ArrayList<>(response.getStack().get(2)).get(1));
        Address jettonMinterAddress = Address.of(minterAddrHex);
        
        // Parse jetton wallet code from stack
        String codeHex = ((String) new ArrayList<>(response.getStack().get(3)).get(1));
        Cell jettonWalletCode = CellBuilder.beginCell().fromBoc(Utils.base64ToBytes(codeHex)).endCell();
        
        return JettonWalletData.builder()
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
      return JettonWalletData.builder()
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
    return JettonWalletData.builder()
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
}
