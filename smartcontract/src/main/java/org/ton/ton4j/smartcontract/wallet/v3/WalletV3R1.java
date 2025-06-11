package org.ton.ton4j.smartcontract.wallet.v3;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

import com.iwebpp.crypto.TweetNaclFast;
import java.time.Instant;
import lombok.Builder;
import lombok.Getter;
import org.ton.java.adnl.AdnlLiteClient;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellBuilder;
import org.ton.ton4j.smartcontract.types.WalletCodes;
import org.ton.ton4j.smartcontract.types.WalletV3Config;
import org.ton.ton4j.smartcontract.utils.MsgUtils;
import org.ton.ton4j.smartcontract.wallet.Contract;
import org.ton.ton4j.tlb.*;
import org.ton.ton4j.tonlib.Tonlib;
import org.ton.ton4j.tonlib.types.ExtMessageInfo;
import org.ton.ton4j.tonlib.types.RawTransaction;
import org.ton.ton4j.utils.Utils;

@Builder
@Getter
public class WalletV3R1 implements Contract {

  TweetNaclFast.Signature.KeyPair keyPair;
  long initialSeqno;
  long walletId;
  byte[] publicKey;

  public static class WalletV3R1Builder {}

  public static WalletV3R1Builder builder() {
    return new CustomWalletV3R1Builder();
  }

  private static class CustomWalletV3R1Builder extends WalletV3R1Builder {
    @Override
    public WalletV3R1 build() {
      if (isNull(super.publicKey)) {
        if (isNull(super.keyPair)) {
          super.keyPair = Utils.generateSignatureKeyPair();
        }
      }
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

  @Override
  public String getName() {
    return "V3R1";
  }

  @Override
  public Cell createCodeCell() {
    return CellBuilder.beginCell().fromBoc(WalletCodes.V3R1.getValue()).endCell();
  }

  @Override
  public Cell createDataCell() {
    return CellBuilder.beginCell()
        .storeUint(initialSeqno, 32)
        .storeUint(walletId, 32)
        .storeBytes(isNull(keyPair) ? publicKey : keyPair.getPublicKey())
        .endCell();
  }

  /**
   * Creates message payload with subwallet-id, valid-until and seqno, equivalent to:
   *
   * <pre>
   *     &lt;b subwallet-id 32 u, timestamp 32 i, seqno 32 u, ref order b&gt;
   * </pre>
   */
  public Cell createTransferBody(WalletV3Config config) {

    Cell order =
        MessageRelaxed.builder()
            .info(
                InternalMessageInfoRelaxed.builder()
                    .bounce(config.isBounce())
                    .dstAddr(
                        MsgAddressIntStd.builder()
                            .workchainId(config.getDestination().wc)
                            .address(config.getDestination().toBigInteger())
                            .build())
                    .value(
                        CurrencyCollection.builder()
                            .coins(config.getAmount())
                            .extraCurrencies(
                                convertExtraCurrenciesToHashMap(config.getExtraCurrencies()))
                            .build())
                    .build())
            .init(config.getStateInit())
            .body(
                (isNull(config.getBody()) && nonNull(config.getComment()))
                    ? CellBuilder.beginCell()
                        .storeUint(0, 32)
                        .storeString(config.getComment())
                        .endCell()
                    : config.getBody())
            .build()
            .toCell();

    return CellBuilder.beginCell()
        .storeUint(config.getWalletId(), 32)
        .storeUint(
            (config.getValidUntil() == 0)
                ? Instant.now().getEpochSecond() + 60
                : config.getValidUntil(),
            32)
        .storeUint(config.getSeqno(), 32)
        .storeUint(
            isNull(config.getSendMode()) // for backward compatibility
                ? ((config.getMode() == 0) ? 3 : config.getMode())
                : config.getSendMode().getValue(),
            8)
        .storeRef(order)
        .endCell();
  }

  public Cell createDeployMessage() {
    return CellBuilder.beginCell()
        .storeUint(walletId, 32) // wallet-id
        .storeInt(-1, 32)
        .storeUint(initialSeqno, 32) // seqno
        .endCell();
  }

  public ExtMessageInfo deploy() {
    return tonlib.sendRawMessage(prepareDeployMsg().toCell().toBase64());
  }

  public ExtMessageInfo deploy(byte[] signedBody) {
    return tonlib.sendRawMessage(prepareDeployMsg(signedBody).toCell().toBase64());
  }

  public Message prepareDeployMsg(byte[] signedBodyHash) {
    Cell body = createDeployMessage();
    return Message.builder()
        .info(ExternalMessageInInfo.builder().dstAddr(getAddressIntStd()).build())
        .init(getStateInit())
        .body(CellBuilder.beginCell().storeBytes(signedBodyHash).storeCell(body).endCell())
        .build();
  }

  public ExtMessageInfo send(WalletV3Config config, byte[] signedBodyHash) {
    return tonlib.sendRawMessage(prepareExternalMsg(config, signedBodyHash).toCell().toBase64());
  }

  public Message prepareExternalMsg(WalletV3Config config, byte[] signedBodyHash) {
    Cell body = createTransferBody(config);
    return MsgUtils.createExternalMessageWithSignedBody(signedBodyHash, getAddress(), null, body);
  }

  public Message prepareDeployMsg() {

    Cell body = createDeployMessage();

    return Message.builder()
        .info(ExternalMessageInInfo.builder().dstAddr(getAddressIntStd()).build())
        .init(getStateInit())
        .body(
            CellBuilder.beginCell()
                .storeBytes(
                    Utils.signData(keyPair.getPublicKey(), keyPair.getSecretKey(), body.hash()))
                .storeCell(body)
                .endCell())
        .build();
  }

  public ExtMessageInfo send(WalletV3Config config) {
    return tonlib.sendRawMessage(prepareExternalMsg(config).toCell().toBase64());
  }

  /**
   * Sends amount of nano toncoins to destination address and waits till message found among
   * account's transactions
   */
  public RawTransaction sendWithConfirmation(WalletV3Config config) {
    return tonlib.sendRawMessageWithConfirmation(
        prepareExternalMsg(config).toCell().toBase64(), getAddress());
  }

  public Message prepareExternalMsg(WalletV3Config config) {
    Cell body = createTransferBody(config);
    return MsgUtils.createExternalMessageWithSignedBody(keyPair, getAddress(), null, body);
  }

  public Cell createInternalSignedBody(WalletV3Config config) {
    Cell body = createTransferBody(config);
    byte[] signature = Utils.signData(keyPair.getPublicKey(), keyPair.getSecretKey(), body.hash());

    return CellBuilder.beginCell().storeCell(body).storeBytes(signature).endCell();
  }

  public Message prepareInternalMsg(WalletV3Config config) {
    Cell body = createInternalSignedBody(config);

    return Message.builder()
        .info(
            InternalMessageInfo.builder()
                .srcAddr(getAddressIntStd())
                .dstAddr(getAddressIntStd())
                .value(CurrencyCollection.builder().coins(config.getAmount()).build())
                .build())
        .body(body)
        .build();
  }

  public MessageRelaxed prepareInternalMsgRelaxed(WalletV3Config config) {
    Cell body = createInternalSignedBody(config);

    return MessageRelaxed.builder()
        .info(
            InternalMessageInfoRelaxed.builder()
                .dstAddr(getAddressIntStd())
                .value(CurrencyCollection.builder().coins(config.getAmount()).build())
                .build())
        .body(body)
        .build();
  }
}
