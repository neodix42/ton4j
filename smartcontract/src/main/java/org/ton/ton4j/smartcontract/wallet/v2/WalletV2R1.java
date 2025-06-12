package org.ton.ton4j.smartcontract.wallet.v2;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

import com.iwebpp.crypto.TweetNaclFast;
import java.math.BigInteger;
import java.time.Instant;
import lombok.Builder;
import lombok.Getter;
import org.ton.java.adnl.AdnlLiteClient;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellBuilder;
import org.ton.ton4j.smartcontract.types.WalletCodes;
import org.ton.ton4j.smartcontract.types.WalletV2R1Config;
import org.ton.ton4j.smartcontract.utils.MsgUtils;
import org.ton.ton4j.smartcontract.wallet.Contract;
import org.ton.ton4j.tlb.*;
import org.ton.ton4j.tonlib.Tonlib;
import org.ton.ton4j.tonlib.types.ExtMessageInfo;
import org.ton.ton4j.tonlib.types.RawTransaction;
import org.ton.ton4j.utils.Utils;

@Builder
@Getter
public class WalletV2R1 implements Contract {

  public TweetNaclFast.Signature.KeyPair keyPair;
  long initialSeqno;
  byte[] publicKey;

  public static class WalletV2R1Builder {}

  public static WalletV2R1Builder builder() {
    return new CustomWalletV2R1Builder();
  }

  private static class CustomWalletV2R1Builder extends WalletV2R1Builder {
    @Override
    public WalletV2R1 build() {
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
    return "V2R1";
  }

  @Override
  public Cell createCodeCell() {
    return CellBuilder.beginCell().fromBoc(WalletCodes.V2R1.getValue()).endCell();
  }

  @Override
  public Cell createDataCell() {
    return CellBuilder.beginCell()
        .storeUint(initialSeqno, 32)
        .storeBytes(isNull(keyPair) ? publicKey : keyPair.getPublicKey())
        .endCell();
  }

  public Cell createDeployMessage() {
    return CellBuilder.beginCell().storeUint(0, 32).storeInt(-1, 32).endCell();
  }

  /** Creates message payload with seqno and validUntil fields */
  public Cell createTransferBody(WalletV2R1Config config) {

    CellBuilder message = CellBuilder.beginCell();
    message.storeUint(BigInteger.valueOf(config.getSeqno()), 32);

    message.storeUint(
        (config.getValidUntil() == 0)
            ? Instant.now().getEpochSecond() + 60
            : config.getValidUntil(),
        32);

    if (nonNull(config.getDestination1())) {
      MessageRelaxed order =
          MsgUtils.createInternalMessageRelaxed(
              config.getDestination1(),
              config.getAmount1(),
              config.getExtraCurrencies1(),
              config.getStateInit(),
              config.getBody(),
              config.getBounce());
      message.storeUint(
          isNull(config.getSendMode()) // for backward compatibility
              ? ((config.getMode() == 0) ? 3 : config.getMode())
              : config.getSendMode().getValue(),
          8);
      message.storeRef(order.toCell());
    }
    if (nonNull(config.getDestination2())) {
      MessageRelaxed order =
          MsgUtils.createInternalMessageRelaxed(
              config.getDestination2(),
              config.getAmount2(),
              config.getExtraCurrencies2(),
              config.getStateInit(),
              config.getBody(),
              config.getBounce());
      message.storeUint(
          isNull(config.getSendMode()) // for backward compatibility
              ? ((config.getMode() == 0) ? 3 : config.getMode())
              : config.getSendMode().getValue(),
          8);
      message.storeRef(order.toCell());
    }
    if (nonNull(config.getDestination3())) {
      MessageRelaxed order =
          MsgUtils.createInternalMessageRelaxed(
              config.getDestination3(),
              config.getAmount3(),
              config.getExtraCurrencies3(),
              config.getStateInit(),
              config.getBody(),
              config.getBounce());
      message.storeUint(
          isNull(config.getSendMode()) // for backward compatibility
              ? ((config.getMode() == 0) ? 3 : config.getMode())
              : config.getSendMode().getValue(),
          8);
      message.storeRef(order.toCell());
    }
    if (nonNull(config.getDestination4())) {
      MessageRelaxed order =
          MsgUtils.createInternalMessageRelaxed(
              config.getDestination4(),
              config.getAmount3(),
              config.getExtraCurrencies4(),
              config.getStateInit(),
              config.getBody(),
              config.getBounce());
      message.storeUint(
          isNull(config.getSendMode()) // for backward compatibility
              ? ((config.getMode() == 0) ? 3 : config.getMode())
              : config.getSendMode().getValue(),
          8);
      message.storeRef(order.toCell());
    }

    return message.endCell();
  }

  public ExtMessageInfo send(WalletV2R1Config config) {
    if (nonNull(adnlLiteClient)) {
      return send(prepareExternalMsg(config));
    }
    return tonlib.sendRawMessage(prepareExternalMsg(config).toCell().toBase64());
  }

  /**
   * Sends amount of nano toncoins to destination address and waits till message found among
   * account's transactions
   */
  public RawTransaction sendWithConfirmation(WalletV2R1Config config) throws Exception {
    if (nonNull(adnlLiteClient)) {
      adnlLiteClient.sendRawMessageWithConfirmation(prepareExternalMsg(config), getAddress());
    }
    return tonlib.sendRawMessageWithConfirmation(
        prepareExternalMsg(config).toCell().toBase64(), getAddress());
  }

  public Message prepareExternalMsg(WalletV2R1Config config) {
    Cell body = createTransferBody(config);
    return MsgUtils.createExternalMessageWithSignedBody(keyPair, getAddress(), null, body);
  }

  public ExtMessageInfo deploy() {
    if (nonNull(adnlLiteClient)) {
      return send(prepareDeployMsg());
    }
    return tonlib.sendRawMessage(prepareDeployMsg().toCell().toBase64());
  }

  public ExtMessageInfo deploy(byte[] signedBody) {
    if (nonNull(adnlLiteClient)) {
      return send(prepareDeployMsg(signedBody));
    }
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

  public ExtMessageInfo send(WalletV2R1Config config, byte[] signedBodyHash) {
    if (nonNull(adnlLiteClient)) {
      return send(prepareExternalMsg(config, signedBodyHash));
    }
    return tonlib.sendRawMessage(prepareExternalMsg(config, signedBodyHash).toCell().toBase64());
  }

  public Message prepareExternalMsg(WalletV2R1Config config, byte[] signedBodyHash) {
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

  public Cell createInternalSignedBody(WalletV2R1Config config) {
    Cell body = createTransferBody(config);
    byte[] signature = Utils.signData(keyPair.getPublicKey(), keyPair.getSecretKey(), body.hash());

    return CellBuilder.beginCell().storeCell(body).storeBytes(signature).endCell();
  }

  public Message prepareInternalMsg(WalletV2R1Config config) {
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

  public MessageRelaxed prepareInternalMsgRelaxed(WalletV2R1Config config) {
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
