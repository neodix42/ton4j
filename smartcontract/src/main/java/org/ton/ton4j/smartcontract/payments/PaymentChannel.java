package org.ton.ton4j.smartcontract.payments;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.ton.ton4j.smartcontract.payments.PaymentsUtils.*;

import com.iwebpp.crypto.TweetNaclFast;
import java.math.BigInteger;
import java.util.List;
import lombok.Builder;
import lombok.Getter;
import org.ton.ton4j.adnl.AdnlLiteClient;
import org.ton.ton4j.address.Address;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellBuilder;
import org.ton.ton4j.mnemonic.Ed25519;
import org.ton.ton4j.smartcontract.token.nft.NftUtils;
import org.ton.ton4j.smartcontract.types.ChannelConfig;
import org.ton.ton4j.smartcontract.types.ChannelData;
import org.ton.ton4j.smartcontract.types.ChannelState;
import org.ton.ton4j.smartcontract.types.WalletCodes;
import org.ton.ton4j.smartcontract.wallet.Contract;
import org.ton.ton4j.tl.liteserver.responses.RunMethodResult;
import org.ton.ton4j.tlb.VmStackValue;
import org.ton.ton4j.tlb.VmTuple;
import org.ton.ton4j.toncenter.TonCenter;
import org.ton.ton4j.tonlib.Tonlib;
import org.ton.ton4j.tonlib.types.*;
import org.ton.ton4j.utils.Utils;

@Builder
@Getter
public class PaymentChannel implements Contract {

  public static final long STATE_UNINITED = 0;
  public static final long STATE_OPEN = 1;
  public static final long STATE_CLOSURE_STARTED = 2;
  public static final long STATE_SETTLING_CONDITIONALS = 3;
  public static final long STATE_AWAITING_FINALIZATION = 4;

  boolean isA;
  BigInteger channelId;
  TweetNaclFast.Signature.KeyPair myKeyPair;
  byte[] hisPublicKey;
  byte[] publicKeyA;
  byte[] publicKeyB;
  BigInteger initBalanceA;
  BigInteger initBalanceB;
  Address addressA;
  Address addressB;

  BigInteger excessFee;

  ChannelConfig channelConfig;
  ClosingConfig closingChannelConfig;

  /**
   *
   *
   * <pre>
   * <a href="https://github.com/ton-blockchain/payment-channels">Payment Channels</a>
   * <p>
   * isA: boolean,
   * channelId: BigInteger,
   * myKeyPair: nacl.SignKeyPair,
   * hisPublicKey: byte[],
   * initBalanceA: BigInteger,
   * initBalanceB: BigInteger,
   * addressA: Address,
   * addressB: Address,
   * closingConfig (optional):
   * {
   * quarantineDuration: long,
   * misbehaviorFine: BigInteger,
   * conditionalCloseDuration: long
   * },
   * excessFee?: BigInteger
   * </pre>
   */
  public static class PaymentChannelBuilder {}

  public static PaymentChannelBuilder builder() {
    return new CustomPaymentChannelBuilder();
  }

  private static class CustomPaymentChannelBuilder extends PaymentChannelBuilder {
    @Override
    public PaymentChannel build() {
      super.publicKeyA = super.isA ? super.myKeyPair.getPublicKey() : super.hisPublicKey;
      super.publicKeyB = !super.isA ? super.myKeyPair.getPublicKey() : super.hisPublicKey;
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

  public String getName() {
    return "payments";
  }

  @Override
  public Cell createDataCell() {
    CellBuilder cell = CellBuilder.beginCell();
    cell.storeBit(false); // inited
    cell.storeCoins(BigInteger.ZERO); // balance_A
    cell.storeCoins(BigInteger.ZERO); // balance_B
    cell.storeBytes(publicKeyA);
    cell.storeBytes(publicKeyB);
    cell.storeUint(channelConfig.getChannelId(), 128); // channel_id

    CellBuilder closingConfig = CellBuilder.beginCell();
    if (nonNull(closingChannelConfig)) {
      closingConfig.storeUint(
          closingChannelConfig.getQuarantineDuration(), 32); // quarantine_duration
      closingConfig.storeCoins(
          isNull(closingChannelConfig.getMisbehaviorFine())
              ? BigInteger.ZERO
              : closingChannelConfig.getMisbehaviorFine()); // misbehavior_fine
      closingConfig.storeUint(
          closingChannelConfig.getConditionalCloseDuration(), 32); // conditional_close_duration
    } else {
      closingConfig.storeUint(0, 32); // quarantine_duration
      closingConfig.storeCoins(BigInteger.ZERO); // misbehavior_fine
      closingConfig.storeUint(0, 32); // conditional_close_duration
    }
    cell.storeRef(closingConfig.endCell());

    cell.storeUint(0, 32); // committed_seqno_A
    cell.storeUint(0, 32); // committed_seqno_B
    cell.storeBit(false); // quarantine ref

    CellBuilder paymentConfig =
        CellBuilder.beginCell()
            .storeCoins(isNull(excessFee) ? BigInteger.ZERO : excessFee) // excess_fee
            .storeAddress(channelConfig.getAddressA()) // addr_A
            .storeAddress(channelConfig.getAddressB()); // addr_B

    cell.storeRef(paymentConfig.endCell());

    return cell.endCell();
  }

  @Override
  public Cell createCodeCell() {
    return CellBuilder.beginCell().fromBoc(WalletCodes.payments.getValue()).endCell();
  }

  public Signature createCooperativeCommit(
      byte[] hisSignature, BigInteger seqnoA, BigInteger seqnoB) {
    if (hisSignature.length != 0) {
      hisSignature = new byte[512 / 8];
    }
    return createTwoSignature(
        op_cooperative_close,
        hisSignature,
        PaymentsUtils.createCooperativeCommitBody(channelConfig.getChannelId(), seqnoA, seqnoB));
  }

  public Signature createCooperativeCloseChannel(byte[] hisSignature, ChannelState channelState) {
    if (isNull(hisSignature)) {
      hisSignature = new byte[512 / 8];
    }
    return createTwoSignature(
        op_cooperative_close,
        hisSignature,
        PaymentsUtils.createCooperativeCloseChannelBody(
            channelConfig.getChannelId(),
            channelState.getBalanceA(),
            channelState.getBalanceB(),
            channelState.getSeqnoA(),
            channelState.getSeqnoB()));
  }

  public Signature createStartUncooperativeClose(
      Cell signedSemiChannelStateA, Cell signedSemiChannelStateB) {
    return createOneSignature(
        op_start_uncooperative_close,
        createStartUncooperativeCloseBody(
            channelConfig.getChannelId(), signedSemiChannelStateA, signedSemiChannelStateB));
  }

  public Signature createChallengeQuarantinedState(
      Cell signedSemiChannelStateA, Cell signedSemiChannelStateB) {
    return createOneSignature(
        op_challenge_quarantined_state,
        createChallengeQuarantinedStateBody(
            channelConfig.getChannelId(), signedSemiChannelStateA, signedSemiChannelStateB));
  }

  public Signature createSettleConditionals(Cell conditionalsToSettle) {
    return createOneSignature(
        op_settle_conditionals,
        createSettleConditionalsBody(channelConfig.getChannelId(), conditionalsToSettle));
  }

  public Cell createInitChannel(BigInteger balanceA, BigInteger balanceB) {
    return createOneSignature(
            op_init_channel,
            PaymentsUtils.createInitChannelBody(channelConfig.getChannelId(), balanceA, balanceB))
        .getCell();
  }

  public Signature createOneSignature(long op, Cell cellForSigning) {
    byte[] signature =
        new TweetNaclFast.Signature(myKeyPair.getPublicKey(), myKeyPair.getSecretKey())
            .detached(cellForSigning.hash());

    Cell cell = PaymentsUtils.createOneSignature(op, isA, signature, cellForSigning);

    return Signature.builder().cell(cell).signature(signature).build();
  }

  public Signature createTwoSignature(long op, byte[] hisSignature, Cell cellForSigning) {
    byte[] signature =
        new TweetNaclFast.Signature(myKeyPair.getPublicKey(), myKeyPair.getSecretKey())
            .detached(cellForSigning.hash());

    byte[] signatureA = isA ? signature : hisSignature;
    byte[] signatureB = !isA ? signature : hisSignature;

    Cell cell = PaymentsUtils.createTwoSignature(op, signatureA, signatureB, cellForSigning);

    return Signature.builder().cell(cell).signature(signature).build();
  }

  public Signature createSignedSemiChannelState(
      BigInteger mySeqNo, BigInteger mySentCoins, BigInteger hisSeqno, BigInteger hisSentCoins) {
    Cell state =
        createSemiChannelState(
            channelConfig.getChannelId(),
            createSemiChannelBody(mySeqNo, mySentCoins, null),
            isNull(hisSeqno) ? null : createSemiChannelBody(hisSeqno, hisSentCoins, null));

    byte[] signature =
        new TweetNaclFast.Signature(myKeyPair.getPublicKey(), myKeyPair.getSecretKey())
            .detached(state.hash());
    Cell cell = PaymentsUtils.createSignedSemiChannelState(signature, state);

    return Signature.builder().signature(signature).cell(cell).build();
  }

  public byte[] signState(ChannelState channelState) {
    BigInteger mySeqno = isA ? channelState.getSeqnoA() : channelState.getSeqnoB();
    BigInteger hisSeqno = !isA ? channelState.getSeqnoA() : channelState.getSeqnoB();

    BigInteger sentCoinsA =
        channelConfig.getInitBalanceA().compareTo(channelState.getBalanceA()) > 0
            ? channelConfig.getInitBalanceA().subtract(channelState.getBalanceA())
            : BigInteger.ZERO;
    BigInteger sentCoinsB =
        channelConfig.getInitBalanceB().compareTo(channelState.getBalanceB()) > 0
            ? channelConfig.getInitBalanceB().subtract(channelState.getBalanceB())
            : BigInteger.ZERO;

    BigInteger mySentCoins = isA ? sentCoinsA : sentCoinsB;
    BigInteger hisSentCoins = !isA ? sentCoinsA : sentCoinsB;

    Signature s = createSignedSemiChannelState(mySeqno, mySentCoins, hisSeqno, hisSentCoins);
    return s.signature;
  }

  public boolean verifyState(ChannelState channelState, byte[] hisSignature) {
    BigInteger mySeqno = !isA ? channelState.getSeqnoA() : channelState.getSeqnoB();
    BigInteger hisSeqno = isA ? channelState.getSeqnoA() : channelState.getSeqnoB();

    BigInteger sentCoinsA =
        channelConfig.getInitBalanceA().compareTo(channelState.getBalanceA()) > 0
            ? channelConfig.getInitBalanceA().subtract(channelState.getBalanceA())
            : BigInteger.ZERO;
    BigInteger sentCoinsB =
        channelConfig.getInitBalanceB().compareTo(channelState.getBalanceB()) > 0
            ? channelConfig.getInitBalanceB().subtract(channelState.getBalanceB())
            : BigInteger.ZERO;

    BigInteger mySentCoins = !isA ? sentCoinsA : sentCoinsB;
    BigInteger hisSentCoins = isA ? sentCoinsA : sentCoinsB;

    Cell state =
        createSemiChannelState(
            channelConfig.getChannelId(),
            createSemiChannelBody(mySeqno, mySentCoins, null),
            isNull(hisSeqno) ? null : createSemiChannelBody(hisSeqno, hisSentCoins, null));

    return Ed25519.verify(isA ? publicKeyB : publicKeyA, state.hash(), hisSignature);
  }

  public byte[] signClose(ChannelState channelState) {
    Signature s = createCooperativeCloseChannel(null, channelState);
    return s.signature;
  }

  public boolean verifyClose(ChannelState channelState, byte[] hisSignature) {
    Cell cell =
        PaymentsUtils.createCooperativeCloseChannelBody(
            channelConfig.getChannelId(),
            channelState.getBalanceA(),
            channelState.getBalanceB(),
            channelState.getSeqnoA(),
            channelState.getSeqnoB());
    return Ed25519.verify(isA ? publicKeyB : publicKeyA, cell.hash(), hisSignature);
  }

  public Cell createFinishUncooperativeClose() {
    return PaymentsUtils.createFinishUncooperativeClose();
  }

  public BigInteger getChannelState() {
    if (nonNull(adnlLiteClient)) {
      RunMethodResult runMethodResult = adnlLiteClient.runMethod(getAddress(), "get_channel_state");
      return runMethodResult.getIntByIndex(0);
    } else {
      RunResult result = tonlib.runMethod(getAddress(), "get_channel_state");

      if (result.getExit_code() != 0) {
        throw new Error("method get_channel_state, returned an exit code " + result.getExit_code());
      }

      TvmStackEntryNumber addr = (TvmStackEntryNumber) result.getStack().get(0);
      return addr.getNumber();
    }
  }

  public ChannelData getData() {
    if (nonNull(adnlLiteClient)) {
      RunMethodResult runMethodResult = adnlLiteClient.runMethod(getAddress(), "get_channel_data");

      BigInteger stateNumber = runMethodResult.getIntByIndex(0);

      VmTuple balanceTuple = runMethodResult.getTupleByIndex(1);
      BigInteger balanceA = balanceTuple.getIntByIndex(0);
      BigInteger balanceB = balanceTuple.getIntByIndex(1);

      VmTuple keyTuple = runMethodResult.getTupleByIndex(2);
      BigInteger publicKeyA = keyTuple.getIntByIndex(0);
      BigInteger publicKeyB = keyTuple.getIntByIndex(1);

      BigInteger channelIdNumber = runMethodResult.getIntByIndex(3);

      VmTuple closureConfigTuple = runMethodResult.getTupleByIndex(4);
      BigInteger quarantineDuration = closureConfigTuple.getIntByIndex(0);
      BigInteger misbehaviourFine = closureConfigTuple.getIntByIndex(1);
      BigInteger conditionalCloseDuration = closureConfigTuple.getIntByIndex(2);

      VmTuple commitedSeqnoTuple = runMethodResult.getTupleByIndex(5);
      BigInteger seqnoA = commitedSeqnoTuple.getIntByIndex(0);
      BigInteger seqnoB = commitedSeqnoTuple.getIntByIndex(1);

      Cell quarantine = null;
      List<VmStackValue> quarantineList = runMethodResult.getListByIndex(6);
      for (VmStackValue o : quarantineList) {
        TvmStackEntryCell t = (TvmStackEntryCell) o;
        quarantine = CellBuilder.beginCell().fromBoc(t.getCell().getBytes()).endCell();
      }

      VmTuple trippleTuple = runMethodResult.getTupleByIndex(7);
      BigInteger excessFee = trippleTuple.getIntByIndex(0);
      Cell addressCellA = trippleTuple.getCellByIndex(1);
      Address addressA = NftUtils.parseAddress(addressCellA);
      Cell addressCellB = trippleTuple.getCellByIndex(2);
      Address addressB = NftUtils.parseAddress(addressCellB);

      return ChannelData.builder()
          .state(stateNumber.longValue())
          .balanceA(balanceA)
          .balanceB(balanceB)
          .publicKeyA(publicKeyA.toByteArray())
          .publicKeyB(publicKeyB.toByteArray())
          .channelId(channelIdNumber)
          .quarantineDuration(quarantineDuration.longValue())
          .misbehaviorFine(misbehaviourFine)
          .conditionalCloseDuration(conditionalCloseDuration.longValue())
          .seqnoA(seqnoA)
          .seqnoB(seqnoB)
          .quarantine(quarantine)
          .excessFee(excessFee)
          .addressA(addressA)
          .addressB(addressB)
          .build();
    } else {
      RunResult result = tonlib.runMethod(getAddress(), "get_channel_data");

      if (result.getExit_code() != 0) {
        throw new Error("method get_channel_data, returned an exit code " + result.getExit_code());
      }

      TvmStackEntryNumber stateNumber = (TvmStackEntryNumber) result.getStack().get(0);

      TvmStackEntryTuple balanceTuple = (TvmStackEntryTuple) result.getStack().get(1);
      TvmStackEntryNumber balanceA =
          (TvmStackEntryNumber) balanceTuple.getTuple().getElements().get(0);
      TvmStackEntryNumber balanceB =
          (TvmStackEntryNumber) balanceTuple.getTuple().getElements().get(1);

      TvmStackEntryTuple keyTuple = (TvmStackEntryTuple) result.getStack().get(2);
      TvmStackEntryNumber publicKeyA =
          (TvmStackEntryNumber) keyTuple.getTuple().getElements().get(0);
      TvmStackEntryNumber publicKeyB =
          (TvmStackEntryNumber) keyTuple.getTuple().getElements().get(1);

      TvmStackEntryNumber channelIdNumber = (TvmStackEntryNumber) result.getStack().get(3);

      TvmStackEntryTuple closureConfigTuple = (TvmStackEntryTuple) result.getStack().get(4);
      TvmStackEntryNumber quarantineDuration =
          (TvmStackEntryNumber) closureConfigTuple.getTuple().getElements().get(0);
      TvmStackEntryNumber misbehaviourFine =
          (TvmStackEntryNumber) closureConfigTuple.getTuple().getElements().get(1);
      TvmStackEntryNumber conditionalCloseDuration =
          (TvmStackEntryNumber) closureConfigTuple.getTuple().getElements().get(2);

      TvmStackEntryTuple commitedSeqnoTuple = (TvmStackEntryTuple) result.getStack().get(5);
      TvmStackEntryNumber seqnoA =
          (TvmStackEntryNumber) commitedSeqnoTuple.getTuple().getElements().get(0);
      TvmStackEntryNumber seqnoB =
          (TvmStackEntryNumber) commitedSeqnoTuple.getTuple().getElements().get(1);

      Cell quarantine = null;
      TvmStackEntryList quarantineList = (TvmStackEntryList) result.getStack().get(6);
      for (Object o : quarantineList.getList().getElements()) {
        TvmStackEntryCell t = (TvmStackEntryCell) o;
        quarantine = CellBuilder.beginCell().fromBoc(t.getCell().getBytes()).endCell();
      }

      TvmStackEntryTuple trippleTuple = (TvmStackEntryTuple) result.getStack().get(7);
      TvmStackEntryNumber excessFee =
          (TvmStackEntryNumber) trippleTuple.getTuple().getElements().get(0);

      TvmStackEntrySlice addressACell =
          (TvmStackEntrySlice) trippleTuple.getTuple().getElements().get(1);

      Address addressA =
          NftUtils.parseAddress(
              CellBuilder.beginCell()
                  .fromBoc(Utils.base64ToBytes(addressACell.getSlice().getBytes()))
                  .endCell());

      TvmStackEntrySlice AddressBCell =
          (TvmStackEntrySlice) trippleTuple.getTuple().getElements().get(2);

      Address addressB =
          NftUtils.parseAddress(
              CellBuilder.beginCell()
                  .fromBoc(Utils.base64ToBytes(AddressBCell.getSlice().getBytes()))
                  .endCell());

      return ChannelData.builder()
          .state(stateNumber.getNumber().longValue())
          .balanceA(balanceA.getNumber())
          .balanceB(balanceB.getNumber())
          .publicKeyA(publicKeyA.getNumber().toByteArray())
          .publicKeyB(publicKeyB.getNumber().toByteArray())
          .channelId(channelIdNumber.getNumber())
          .quarantineDuration(quarantineDuration.getNumber().longValue())
          .misbehaviorFine(misbehaviourFine.getNumber())
          .conditionalCloseDuration(conditionalCloseDuration.getNumber().longValue())
          .seqnoA(seqnoA.getNumber())
          .seqnoB(seqnoB.getNumber())
          .quarantine(quarantine)
          .excessFee(excessFee.getNumber())
          .addressA(addressA)
          .addressB(addressB)
          .build();
    }
  }
}
