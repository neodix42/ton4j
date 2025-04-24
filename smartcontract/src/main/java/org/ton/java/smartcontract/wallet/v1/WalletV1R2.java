package org.ton.java.smartcontract.wallet.v1;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

import com.iwebpp.crypto.TweetNaclFast;
import java.math.BigInteger;
import lombok.Builder;
import lombok.Getter;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.smartcontract.types.WalletCodes;
import org.ton.java.smartcontract.types.WalletV1R2Config;
import org.ton.java.smartcontract.utils.MsgUtils;
import org.ton.java.smartcontract.wallet.Contract;
import org.ton.java.tlb.*;
import org.ton.java.tonlib.Tonlib;
import org.ton.java.tonlib.types.ExtMessageInfo;
import org.ton.java.tonlib.types.RawTransaction;
import org.ton.java.utils.Utils;

@Builder
@Getter
public class WalletV1R2 implements Contract {

  TweetNaclFast.Signature.KeyPair keyPair;
  long initialSeqno;
  byte[] publicKey;

  public static class WalletV1R2Builder {}

  public static WalletV1R2Builder builder() {
    return new CustomWalletV1R2Builder();
  }

  private static class CustomWalletV1R2Builder extends WalletV1R2Builder {
    @Override
    public WalletV1R2 build() {
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
    return "V1R2";
  }

  @Override
  public Cell createDataCell() {
    return CellBuilder.beginCell()
        .storeUint(initialSeqno, 32) // seqno
        .storeBytes(isNull(keyPair) ? publicKey : keyPair.getPublicKey())
        .endCell();
  }

  @Override
  public Cell createCodeCell() {
    return CellBuilder.beginCell().fromBoc(WalletCodes.V1R2.getValue()).endCell();
  }

  public Cell createDeployMessage() {
    return CellBuilder.beginCell().storeUint(initialSeqno, 32).endCell();
  }

  public Cell createTransferBody(WalletV1R2Config config) {
    Cell order =
        MessageRelaxed.builder()
            .info(
                InternalMessageInfoRelaxed.builder()
                    .bounce(config.getBounce())
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
        .storeUint(BigInteger.valueOf(config.getSeqno()), 32)
        .storeUint((config.getMode() == 0) ? 3 : config.getMode(), 8)
        .storeRef(order)
        .endCell();
  }

  /**
   * Sends amount of nano toncoins to destination address using specified seqno
   *
   * @param config WalletV1R2Config
   */
  public ExtMessageInfo send(WalletV1R2Config config) {

    return tonlib.sendRawMessage(prepareExternalMsg(config).toCell().toBase64());
  }

  /**
   * Sends amount of nano toncoins to destination address and waits till message found among
   * account's transactions
   */
  public RawTransaction sendWithConfirmation(WalletV1R2Config config) {
    return tonlib.sendRawMessageWithConfirmation(
        prepareExternalMsg(config).toCell().toBase64(), getAddress());
  }

  public Message prepareExternalMsg(WalletV1R2Config config) {
    Cell body = createTransferBody(config);
    return MsgUtils.createExternalMessageWithSignedBody(keyPair, getAddress(), null, body);
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

  public ExtMessageInfo send(WalletV1R2Config config, byte[] signedBodyHash) {
    return tonlib.sendRawMessage(prepareExternalMsg(config, signedBodyHash).toCell().toBase64());
  }

  public Message prepareExternalMsg(WalletV1R2Config config, byte[] signedBodyHash) {
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

  public Cell createInternalSignedBody(WalletV1R2Config config) {
    Cell body = createTransferBody(config);
    byte[] signature = Utils.signData(keyPair.getPublicKey(), keyPair.getSecretKey(), body.hash());

    return CellBuilder.beginCell().storeCell(body).storeBytes(signature).endCell();
  }

  public Message prepareInternalMsg(WalletV1R2Config config) {
    Cell body = createInternalSignedBody(config);

    return Message.builder()
        .info(
            InternalMessageInfo.builder()
                .srcAddr(getAddressIntStd())
                .dstAddr(getAddressIntStd())
                .value(
                    CurrencyCollection.builder()
                        .coins(config.getAmount())
                        .extraCurrencies(
                            convertExtraCurrenciesToHashMap(config.getExtraCurrencies()))
                        .build())
                .build())
        .body(body)
        .build();
  }

  public MessageRelaxed prepareInternalMsgRelaxed(WalletV1R2Config config) {
    Cell body = createInternalSignedBody(config);

    return MessageRelaxed.builder()
        .info(
            InternalMessageInfoRelaxed.builder()
                .dstAddr(getAddressIntStd())
                .value(
                    CurrencyCollection.builder()
                        .coins(config.getAmount())
                        .extraCurrencies(
                            convertExtraCurrenciesToHashMap(config.getExtraCurrencies()))
                        .build())
                .build())
        .body(body)
        .build();
  }
}
