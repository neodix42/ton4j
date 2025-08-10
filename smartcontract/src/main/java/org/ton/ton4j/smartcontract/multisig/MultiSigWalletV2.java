package org.ton.ton4j.smartcontract.multisig;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

import java.math.BigInteger;
import java.util.*;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.ton.java.adnl.AdnlLiteClient;
import org.ton.ton4j.address.Address;
import org.ton.ton4j.cell.*;
import org.ton.ton4j.smartcontract.types.*;
import org.ton.ton4j.smartcontract.wallet.Contract;
import org.ton.ton4j.tl.liteserver.responses.RunMethodResult;
import org.ton.ton4j.tlb.*;
import org.ton.ton4j.toncenter.TonCenter;
import org.ton.ton4j.tonlib.Tonlib;
import org.ton.ton4j.tonlib.types.*;
import org.ton.ton4j.tonlib.types.ExtraCurrency;

/** <a href="https://github.com/ton-blockchain/multisig-contract-v2">multisig-v2</a> */
@Builder
@Getter
@Slf4j
public class MultiSigWalletV2 implements Contract {

  MultiSigV2Config config;

  public static class MultiSigWalletV2Builder {}

  public static MultiSigWalletV2Builder builder() {
    return new CustomMultiSigWalletV2Builder();
  }

  private static class CustomMultiSigWalletV2Builder extends MultiSigWalletV2Builder {
    @Override
    public MultiSigWalletV2 build() {

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
  public org.ton.ton4j.toncenter.TonCenter getTonCenterClient() {
    return tonCenterClient;
  }

  @Override
  public void setTonCenterClient(org.ton.ton4j.toncenter.TonCenter pTonCenterClient) {
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

  @Override
  public String getName() {
    return "multisig-v2";
  }

  @Override
  public Cell createDataCell() {
    return CellBuilder.beginCell()
        .storeUint(config.getNextOrderSeqno(), 256)
        .storeUint(config.getThreshold(), 8)
        .storeRef(toSignersDict(config.getSigners()))
        .storeUint(config.getNumberOfSigners(), 8)
        .storeDict(toProposersDict(config.getProposers()))
        .storeBit(config.isAllowArbitraryOrderSeqno())
        .endCell();
  }

  @Override
  public Cell createCodeCell() {
    return CellBuilder.beginCell().fromBoc(WalletCodes.multisigV2.getValue()).endCell();
  }

  public ExtMessageInfo deploy() {

    Message externalMessage =
        Message.builder()
            .info(ExternalMessageInInfo.builder().dstAddr(getAddressIntStd()).build())
            .init(getStateInit())
            .build();

    if (nonNull(tonCenterClient)) {
      return send(externalMessage);
    }
    if (nonNull(adnlLiteClient)) {
      return send(externalMessage);
    }
    return tonlib.sendRawMessage(externalMessage.toCell().toBase64());
  }

  private static TonHashMapE convertExtraCurrenciesToMap(List<ExtraCurrency> extraCurrencies) {

    if (isNull(extraCurrencies)) {
      return null;
    }
    TonHashMapE x = new TonHashMapE(32);

    for (ExtraCurrency ec : extraCurrencies) {
      x.elements.put(ec.getId(), ec.getAmount());
    }
    return x;
  }

  public static MultiSigV2Action createSendMessageAction(int mode, Cell message) {
    return MultiSigV2SendMessageAction.builder().mode(mode).message(message).build();
  }

  public static Cell createOrder(List<MultiSigV2Action> multiSigV2Actions) {
    int dictKeySize = 8;
    TonHashMap dictOrders = new TonHashMap(dictKeySize);
    int i = 0;
    for (MultiSigV2Action multiSigV2Action : multiSigV2Actions) {
      dictOrders.elements.put(i++, multiSigV2Action.toCell());
    }
    return dictOrders.serialize(
        k -> CellBuilder.beginCell().storeUint((Integer) k, dictKeySize).endCell().getBits(),
        v -> CellBuilder.beginCell().storeRef((Cell) v).endCell());
  }

  public static MultiSigV2UpdateParamsAction updateMultiSigParam(
      long newThreshold, List<Address> newSigners, List<Address> newProposers) {
    return MultiSigV2UpdateParamsAction.builder()
        .newThreshold(newThreshold)
        .newSigners(newSigners)
        .newProposers(newProposers)
        .build();
  }

  /**
   *
   *
   * <pre>
   * new_order#f718510f query_id:uint64
   *                    order_seqno:uint256
   *                    signer:(## 1)
   *                    index:uint8
   *                    expiration_date:uint48
   *                    order:^Order = InternalMsgBody;
   * </pre>
   */
  public static Cell newOrder(
      long queryId,
      BigInteger orderSeqno,
      boolean isSigner,
      long signerIndex,
      long expirationDate,
      Cell order) {
    return CellBuilder.beginCell()
        .storeUint(0xf718510fL, 32)
        .storeUint(queryId, 64)
        .storeUint(orderSeqno, 256)
        .storeBit(isSigner)
        .storeUint(signerIndex, 8)
        .storeUint(expirationDate, 48)
        .storeRef(order)
        .endCell();
  }

  public static Cell approve(long queryId, long signerIndex) {
    return CellBuilder.beginCell()
        .storeUint(0xa762230fL, 32)
        .storeUint(queryId, 64)
        .storeUint(signerIndex, 8)
        .endCell();
  }

  /**
   *
   *
   * <pre>
   * execute#75097f5d query_id:uint64
   *                  order_seqno:uint256
   *                  expiration_date:uint48
   *                  approvals_num:uint8
   *                  signers_hash:bits256
   *                  order:^Order = InternalMsgBody;
   * </pre>
   */
  private Cell executeOrder(
      long queryId,
      BigInteger orderSeqno,
      long expirationDate,
      long numberOfApprovals,
      Cell signersCell,
      Cell order) {
    return CellBuilder.beginCell()
        .storeUint(0x75097f5dL, 32)
        .storeUint(queryId, 64)
        .storeUint(orderSeqno, 256)
        .storeUint(expirationDate, 48)
        .storeUint(numberOfApprovals, 8)
        .storeBytes(signersCell.getHash(), 256)
        .storeRef(order)
        .endCell();
  }

  public Address getOrderAddress(BigInteger orderSeqno) {
    Cell orderAddrCell;
    if (nonNull(adnlLiteClient)) {
      RunMethodResult runMethodResult =
          adnlLiteClient.runMethod(
              getAddress(),
              "get_order_address",
              VmStackValueInt.builder().value(orderSeqno).build());
      VmCellSlice slice = runMethodResult.getSliceByIndex(0);
      return CellSlice.beginParse(slice.getCell()).skipBits(slice.getStBits()).loadAddress();
    } else {
      Deque<String> stackData = new ArrayDeque<>();
      stackData.offer("[num, " + orderSeqno + "]");
      RunResult runResult = tonlib.runMethod(getAddress(), "get_order_address", stackData);
      TvmStackEntrySlice orderAddrCellT = (TvmStackEntrySlice) runResult.getStack().get(0);
      orderAddrCell = Cell.fromBocBase64(orderAddrCellT.getSlice().getBytes());
    }

    return MsgAddressInt.deserialize(CellSlice.beginParse(orderAddrCell)).toAddress();
  }

  public MultiSigV2OrderData getOrderData(BigInteger orderSeqno) {
    Address orderAddress = getOrderAddress(orderSeqno);
    Address multiSigAddressCell;
    BigInteger orderSeqNo;
    BigInteger threshold;
    BigInteger sentForExecution;
    TonHashMap signers;
    BigInteger approvalsMask;
    BigInteger numberOfApprovals;
    BigInteger expirationDate;
    Cell order;
    if (nonNull(adnlLiteClient)) {
      RunMethodResult runMethodResult = adnlLiteClient.runMethod(orderAddress, "get_order_data");
      if (runMethodResult.getExitCode() == 0) {
        VmCellSlice slice = runMethodResult.getSliceByIndex(0);
        multiSigAddressCell =
            CellSlice.beginParse(slice.getCell()).skipBits(slice.getStBits()).loadAddress();
        //        multiSigAddress = runMethodResult.getSliceByIndex(0).getCell();
        orderSeqNo = runMethodResult.getIntByIndex(1);
        threshold = runMethodResult.getIntByIndex(2);
        sentForExecution = runMethodResult.getIntByIndex(3);
        Cell signersDictCell = runMethodResult.getCellByIndex(4);
        signers =
            CellSlice.beginParse(signersDictCell)
                .loadDict(
                    8,
                    k -> k.readUint(8),
                    v -> MsgAddressIntStd.deserialize(CellSlice.beginParse(v)));
        approvalsMask = runMethodResult.getIntByIndex(5);
        numberOfApprovals = runMethodResult.getIntByIndex(6);
        expirationDate = runMethodResult.getIntByIndex(7);
        order = runMethodResult.getCellByIndex(8);
      } else {
        throw new Error("Error retrieving order data. Exit code: " + runMethodResult.getExitCode());
      }

    } else {

      RunResult runResult = tonlib.runMethod(orderAddress, "get_order_data");
      TvmSlice multiSigAddressSlice = ((TvmStackEntrySlice) runResult.getStack().get(0)).getSlice();
      multiSigAddressCell =
          MsgAddressInt.deserialize(
                  CellSlice.beginParse(Cell.fromBocBase64(multiSigAddressSlice.getBytes())))
              .toAddress();
      orderSeqNo = ((TvmStackEntryNumber) runResult.getStack().get(1)).getNumber();
      threshold = ((TvmStackEntryNumber) runResult.getStack().get(2)).getNumber();
      sentForExecution = ((TvmStackEntryNumber) runResult.getStack().get(3)).getNumber();
      TvmCell signersDictCell = ((TvmStackEntryCell) runResult.getStack().get(4)).getCell();
      signers =
          CellSlice.beginParse(Cell.fromBocBase64(signersDictCell.getBytes()))
              .loadDict(
                  8,
                  k -> k.readUint(8),
                  v -> MsgAddressIntStd.deserialize(CellSlice.beginParse(v)));
      approvalsMask = ((TvmStackEntryNumber) runResult.getStack().get(5)).getNumber();
      numberOfApprovals = ((TvmStackEntryNumber) runResult.getStack().get(6)).getNumber();
      expirationDate = ((TvmStackEntryNumber) runResult.getStack().get(7)).getNumber();
      TvmCell orderT = ((TvmStackEntryCell) runResult.getStack().get(8)).getCell();
      order = Cell.fromBocBase64(orderT.getBytes());
    }
    return MultiSigV2OrderData.builder()
        .multiSigAddress(
            MsgAddressInt.deserialize(CellSlice.beginParse(multiSigAddressCell)).toAddress())
        .orderSeqno(orderSeqNo)
        .threshold(threshold.longValue())
        .sentForExecution(sentForExecution.longValue() == -1)
        .signers(fromSignersDict(signers))
        .approvals_mask(approvalsMask.longValue())
        .approvals_num(numberOfApprovals.longValue())
        .expirationDate(expirationDate.longValue())
        .order(order)
        .build();
  }

  public BigInteger getOrderEstimate(Cell order, long expirationDate) {
    if (nonNull(adnlLiteClient)) {
      RunMethodResult runMethodResult =
          adnlLiteClient.runMethod(
              getAddress(),
              "get_order_estimate",
              VmStackValueCell.builder().cell(Cell.fromBoc(order.toHex())).build(),
              VmStackValueInt.builder().value(BigInteger.valueOf(expirationDate)).build());
      return runMethodResult.getIntByIndex(0);
    }

    Deque<String> stackData = new ArrayDeque<>();
    stackData.offer("[cell, " + order.toHex() + "]");
    stackData.offer("[num, " + expirationDate + "]");
    RunResult runResult = tonlib.runMethod(getAddress(), "get_order_estimate", stackData);
    TvmStackEntryNumber estimatedGas = (TvmStackEntryNumber) runResult.getStack().get(0);
    return estimatedGas.getNumber();
  }

  public MultiSigV2Data getMultiSigData() {
    BigInteger nextOrderSeqno;
    BigInteger threshold;
    TonHashMap signers;
    TonHashMap proposers;

    if (nonNull(adnlLiteClient)) {
      RunMethodResult runMethodResult = adnlLiteClient.runMethod(getAddress(), "get_multisig_data");
      nextOrderSeqno = runMethodResult.getIntByIndex(0);
      threshold = runMethodResult.getIntByIndex(1);
      Cell cellSignersDict = runMethodResult.getCellByIndex(2);
      signers =
          CellSlice.beginParse(cellSignersDict)
              .loadDict(
                  8,
                  k -> k.readUint(8),
                  v -> MsgAddressIntStd.deserialize(CellSlice.beginParse(v)));
      Cell cellProposersDict = runMethodResult.getCellByIndex(3);
      proposers =
          CellSlice.beginParse(cellProposersDict)
              .loadDict(
                  8,
                  k -> k.readUint(8),
                  v -> MsgAddressIntStd.deserialize(CellSlice.beginParse(v)));
    } else {
      RunResult runResult = tonlib.runMethod(getAddress(), "get_multisig_data");
      nextOrderSeqno = ((TvmStackEntryNumber) runResult.getStack().get(0)).getNumber();

      threshold = ((TvmStackEntryNumber) runResult.getStack().get(1)).getNumber();

      TvmCell signersCell = ((TvmStackEntryCell) runResult.getStack().get(2)).getCell();

      signers =
          CellSlice.beginParse(Cell.fromBocBase64(signersCell.getBytes()))
              .loadDict(
                  8,
                  k -> k.readUint(8),
                  v -> MsgAddressIntStd.deserialize(CellSlice.beginParse(v)));

      TvmCell proposersCell = ((TvmStackEntryCell) runResult.getStack().get(3)).getCell();

      proposers =
          CellSlice.beginParse(Cell.fromBocBase64(proposersCell.getBytes()))
              .loadDict(
                  8,
                  k -> k.readUint(8),
                  v -> MsgAddressIntStd.deserialize(CellSlice.beginParse(v)));
    }
    return MultiSigV2Data.builder()
        .nextOrderSeqno(nextOrderSeqno)
        .threshold(threshold.longValue())
        .signers(fromSignersDict(signers))
        .proposers(fromProposersDict(proposers))
        .build();
  }

  private List<Address> fromSignersDict(TonHashMap signersDict) {
    List<Address> result = new ArrayList<>();
    for (Map.Entry<Object, Object> entry : signersDict.elements.entrySet()) {
      result.add(((MsgAddressIntStd) entry.getValue()).toAddress());
    }
    return result;
  }

  private List<Address> fromProposersDict(TonHashMap proposersDict) {
    List<Address> result = new ArrayList<>();
    for (Map.Entry<Object, Object> entry : proposersDict.elements.entrySet()) {
      result.add(((MsgAddressIntStd) entry.getValue()).toAddress());
    }
    return result;
  }

  public static Cell toSignersDict(List<Address> signers) {
    int dictKeySize = 8;
    TonHashMap dict = new TonHashMap(dictKeySize);
    long i = 0;
    for (Address signer : signers) {
      dict.elements.put(i++, MsgAddressIntStd.of(signer).toCell());
    }

    return dict.serialize(
        k -> CellBuilder.beginCell().storeUint((Long) k, dictKeySize).endCell().getBits(),
        v -> (Cell) v);
  }

  public static Cell toProposersDict(List<Address> proposers) {
    int dictKeySize = 8;
    TonHashMapE dict = new TonHashMapE(dictKeySize);
    long i = 0;
    for (Address proposer : proposers) {
      dict.elements.put(i++, MsgAddressIntStd.of(proposer).toCell());
    }

    return dict.serialize(
        k -> CellBuilder.beginCell().storeUint((Long) k, dictKeySize).endCell().getBits(),
        v -> (Cell) v);
  }
}
