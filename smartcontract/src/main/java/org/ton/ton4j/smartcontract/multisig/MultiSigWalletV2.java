package org.ton.ton4j.smartcontract.multisig;

import static java.util.Objects.isNull;

import java.math.BigInteger;
import java.util.*;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.ton.ton4j.address.Address;
import org.ton.ton4j.smartcontract.types.*;
import org.ton.ton4j.smartcontract.wallet.Contract;
import org.ton.ton4j.tlb.*;
import org.ton.ton4j.tonlib.Tonlib;
import org.ton.ton4j.tonlib.types.*;
import org.ton.ton4j.cell.*;

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
    Deque<String> stackData = new ArrayDeque<>();
    stackData.offer("[num, " + orderSeqno + "]");
    RunResult runResult = tonlib.runMethod(getAddress(), "get_order_address", stackData);
    TvmStackEntrySlice orderAddrCell = (TvmStackEntrySlice) runResult.getStack().get(0);
    return MsgAddressInt.deserialize(
            CellSlice.beginParse(Cell.fromBocBase64(orderAddrCell.getSlice().getBytes())))
        .toAddress();
  }

  public MultiSigV2OrderData getOrderData(BigInteger orderSeqno) {
    Address orderAddress = getOrderAddress(orderSeqno);
    RunResult runResult = tonlib.runMethod(orderAddress, "get_order_data");
    TvmStackEntrySlice multiSigAddressCell = (TvmStackEntrySlice) runResult.getStack().get(0);
    TvmStackEntryNumber orderSeqNo = (TvmStackEntryNumber) runResult.getStack().get(1);
    TvmStackEntryNumber threshold = (TvmStackEntryNumber) runResult.getStack().get(2);
    TvmStackEntryNumber sentForExecution = (TvmStackEntryNumber) runResult.getStack().get(3);
    TvmStackEntryCell signersCell = (TvmStackEntryCell) runResult.getStack().get(4);
    TonHashMap signers =
        CellSlice.beginParse(Cell.fromBocBase64(signersCell.getCell().getBytes()))
            .loadDict(
                8, k -> k.readUint(8), v -> MsgAddressIntStd.deserialize(CellSlice.beginParse(v)));
    TvmStackEntryNumber approvalsMask = (TvmStackEntryNumber) runResult.getStack().get(5);
    TvmStackEntryNumber numberOfApprovals = (TvmStackEntryNumber) runResult.getStack().get(6);
    TvmStackEntryNumber expirationDate = (TvmStackEntryNumber) runResult.getStack().get(7);
    TvmStackEntryCell order = (TvmStackEntryCell) runResult.getStack().get(8);

    return MultiSigV2OrderData.builder()
        .multiSigAddress(
            MsgAddressInt.deserialize(
                    CellSlice.beginParse(
                        Cell.fromBocBase64(multiSigAddressCell.getSlice().getBytes())))
                .toAddress())
        .orderSeqno(orderSeqNo.getNumber())
        .threshold(threshold.getNumber().longValue())
        .sentForExecution(sentForExecution.getNumber().longValue() == -1)
        .signers(fromSignersDict(signers))
        .approvals_mask(approvalsMask.getNumber().longValue())
        .approvals_num(numberOfApprovals.getNumber().longValue())
        .expirationDate(expirationDate.getNumber().longValue())
        .order(Cell.fromBocBase64(order.getCell().getBytes()))
        .build();
  }

  public BigInteger getOrderEstimate(Cell order, long expirationDate) {
    Deque<String> stackData = new ArrayDeque<>();
    stackData.offer("[cell, " + order.toHex() + "]");
    stackData.offer("[num, " + expirationDate + "]");
    RunResult runResult = tonlib.runMethod(getAddress(), "get_order_estimate", stackData);
    TvmStackEntryNumber estimatedGas = (TvmStackEntryNumber) runResult.getStack().get(0);
    return estimatedGas.getNumber();
  }

  public MultiSigV2Data getMultiSigData() {
    RunResult runResult = tonlib.runMethod(getAddress(), "get_multisig_data");
    TvmStackEntryNumber nextOrderSeqno = (TvmStackEntryNumber) runResult.getStack().get(0);

    TvmStackEntryNumber threshold = (TvmStackEntryNumber) runResult.getStack().get(1);

    TvmStackEntryCell signersCell = (TvmStackEntryCell) runResult.getStack().get(2);

    TonHashMap signers =
        CellSlice.beginParse(Cell.fromBocBase64(signersCell.getCell().getBytes()))
            .loadDict(
                8, k -> k.readUint(8), v -> MsgAddressIntStd.deserialize(CellSlice.beginParse(v)));

    TvmStackEntryCell proposersCell = (TvmStackEntryCell) runResult.getStack().get(3);

    TonHashMap proposers =
        CellSlice.beginParse(Cell.fromBocBase64(proposersCell.getCell().getBytes()))
            .loadDict(
                8, k -> k.readUint(8), v -> MsgAddressIntStd.deserialize(CellSlice.beginParse(v)));

    return MultiSigV2Data.builder()
        .nextOrderSeqno(nextOrderSeqno.getNumber())
        .threshold(threshold.getNumber().longValue())
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
