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
import org.ton.java.adnl.AdnlLiteClient;
import org.ton.ton4j.address.Address;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellBuilder;
import org.ton.ton4j.smartcontract.token.nft.NftUtils;
import org.ton.ton4j.smartcontract.types.JettonMinterData;
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
  public JettonMinterData getJettonData(Tonlib tonlib) {
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
    CellBuilder cell = CellBuilder.beginCell();
    cell.storeAddress(ownerAddress);

    Deque<String> stack = new ArrayDeque<>();

    stack.offer("[slice, " + cell.endCell().toHex(true) + "]");

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
    Address jettonWalletAddress =
        NftUtils.parseAddress(
            CellBuilder.beginCell()
                .fromBoc(Utils.base64ToBytes(addr.getSlice().getBytes()))
                .endCell());

    return JettonWallet.builder().tonlib(tonlib).address(jettonWalletAddress).build();
  }
}
