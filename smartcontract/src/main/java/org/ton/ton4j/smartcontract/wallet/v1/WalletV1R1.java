package org.ton.ton4j.smartcontract.wallet.v1;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

import com.iwebpp.crypto.TweetNaclFast;
import java.math.BigInteger;
import lombok.Builder;
import lombok.Getter;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellBuilder;
import org.ton.ton4j.smartcontract.types.WalletCodes;
import org.ton.ton4j.smartcontract.types.WalletV1R1Config;
import org.ton.ton4j.smartcontract.utils.MsgUtils;
import org.ton.ton4j.smartcontract.wallet.Contract;
import org.ton.ton4j.tlb.*;
import org.ton.ton4j.tonlib.Tonlib;
import org.ton.ton4j.tonlib.types.ExtMessageInfo;
import org.ton.ton4j.tonlib.types.RawTransaction;
import org.ton.ton4j.utils.Utils;

@Builder
@Getter
public class WalletV1R1 implements Contract {

  TweetNaclFast.Signature.KeyPair keyPair;
  long initialSeqno;
  byte[] publicKey;

  public static class WalletV1R1Builder {}

  public static WalletV1R1Builder builder() {
    return new CustomWalletV1R1Builder();
  }

  private static class CustomWalletV1R1Builder extends WalletV1R1Builder {
    @Override
    public WalletV1R1 build() {
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
    return "V1R1";
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
    return CellBuilder.beginCell().fromBoc(WalletCodes.V1R1.getValue()).endCell();
  }

  public Cell createDeployMessage() {
    return CellBuilder.beginCell().storeUint(initialSeqno, 32).endCell();
  }

  public Cell createTransferBody(WalletV1R1Config config) {
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
        .storeUint(BigInteger.valueOf(config.getSeqno()), 32)
        .storeUint(
            isNull(config.getSendMode()) // for backward compatibility
                ? ((config.getMode() == 0) ? 3 : config.getMode())
                : config.getSendMode().getValue(),
            8)
        .storeRef(order)
        .endCell();
  }

  /**
   * Sends amount of nano toncoins to destination address using specified seqno
   *
   * @param config WalletV1R1Config
   */
  public ExtMessageInfo send(WalletV1R1Config config) {
    return tonlib.sendRawMessage(prepareExternalMsg(config).toCell().toBase64());
  }

  /**
   * Sends amount of nano toncoins to destination address and waits till message found among
   * account's transactions
   */
  public RawTransaction sendWithConfirmation(WalletV1R1Config config) {
    return tonlib.sendRawMessageWithConfirmation(
        prepareExternalMsg(config).toCell().toBase64(), getAddress());
  }

  public Message prepareExternalMsg(WalletV1R1Config config) {
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

  public ExtMessageInfo send(WalletV1R1Config config, byte[] signedBodyHash) {
    return tonlib.sendRawMessage(prepareExternalMsg(config, signedBodyHash).toCell().toBase64());
  }

  public Message prepareExternalMsg(WalletV1R1Config config, byte[] signedBodyHash) {
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

  public Cell createInternalSignedBody(WalletV1R1Config config) {
    Cell body = createTransferBody(config);
    byte[] signature = Utils.signData(keyPair.getPublicKey(), keyPair.getSecretKey(), body.hash());

    return CellBuilder.beginCell().storeCell(body).storeBytes(signature).endCell();
  }

  public Message prepareInternalMsg(WalletV1R1Config config) {
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

  public MessageRelaxed prepareInternalMsgRelaxed(WalletV1R1Config config) {
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
