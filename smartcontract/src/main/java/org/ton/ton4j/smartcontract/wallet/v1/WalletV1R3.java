package org.ton.ton4j.smartcontract.wallet.v1;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

import com.iwebpp.crypto.TweetNaclFast;
import java.math.BigInteger;
import lombok.Builder;
import lombok.Getter;
import org.ton.ton4j.adnl.AdnlLiteClient;
import org.ton.ton4j.address.Address;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellBuilder;
import org.ton.ton4j.smartcontract.types.WalletCodes;
import org.ton.ton4j.smartcontract.types.WalletV1R3Config;
import org.ton.ton4j.smartcontract.utils.MsgUtils;
import org.ton.ton4j.smartcontract.wallet.Contract;
import org.ton.ton4j.tlb.*;
import org.ton.ton4j.toncenter.TonCenter;
import org.ton.ton4j.tonlib.Tonlib;
import org.ton.ton4j.tonlib.types.*;
import org.ton.ton4j.utils.Utils;

@Builder
@Getter
public class WalletV1R3 implements Contract {

  TweetNaclFast.Signature.KeyPair keyPair;
  long initialSeqno;
  byte[] publicKey;

  public static class WalletV1R3Builder {}

  public static WalletV1R3Builder builder() {
    return new CustomWalletV1R3Builder();
  }

  private static class CustomWalletV1R3Builder extends WalletV1R3Builder {
    @Override
    public WalletV1R3 build() {
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

  @Override
  public String getName() {
    return "V1R3";
  }

  public String getPublicKey() {
    if (nonNull(tonCenterClient)) {
      try {
        return Utils.bytesToHex(Utils.to32ByteArray(tonCenterClient.getPublicKey(getAddress().toBounceable())));
      } catch (Exception e) {
        throw new Error(e);
      }
    }
    if (nonNull(adnlLiteClient)) {
      return Utils.bytesToHex(Utils.to32ByteArray(adnlLiteClient.getPublicKey(getAddress())));
    }

    Address myAddress = getAddress();
    RunResult result = tonlib.runMethod(myAddress, "get_public_key");

    if (result.getExit_code() != 0) {
      throw new Error("method get_public_key, returned an exit code " + result.getExit_code());
    }

    TvmStackEntryNumber publicKeyNumber = (TvmStackEntryNumber) result.getStack().get(0);
    return publicKeyNumber.getNumber().toString(16);
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
    return CellBuilder.beginCell().fromBoc(WalletCodes.V1R3.getValue()).endCell();
  }

  public Cell createDeployMessage() {
    return CellBuilder.beginCell().storeUint(0, 32).endCell();
  }

  public Cell createTransferBody(WalletV1R3Config config) {
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
   * @param config WalletV1R2Config
   */
  public ExtMessageInfo send(WalletV1R3Config config) {
    if (nonNull(tonCenterClient)) {
      return send(prepareExternalMsg(config));
    }
    if (nonNull(adnlLiteClient)) {
      return send(prepareExternalMsg(config));
    }
    return tonlib.sendRawMessage(prepareExternalMsg(config).toCell().toBase64());
  }

  /**
   * Sends amount of nano toncoins to destination address and waits till message found among
   * account's transactions
   */
  public RawTransaction sendWithConfirmation(WalletV1R3Config config) throws Exception {
    if (nonNull(adnlLiteClient)) {
      adnlLiteClient.sendRawMessageWithConfirmation(prepareExternalMsg(config), getAddress());
      return null;
    } else {
      return tonlib.sendRawMessageWithConfirmation(
          prepareExternalMsg(config).toCell().toBase64(), getAddress());
    }
  }

  public Message prepareExternalMsg(WalletV1R3Config config) {
    Cell body = createTransferBody(config);
    return MsgUtils.createExternalMessageWithSignedBody(keyPair, getAddress(), null, body);
  }

  public ExtMessageInfo deploy() {
    if (nonNull(tonCenterClient)) {
      return send(prepareDeployMsg());
    }
    if (nonNull(adnlLiteClient)) {
      return send(prepareDeployMsg());
    }
    return tonlib.sendRawMessage(prepareDeployMsg().toCell().toBase64());
  }

  public ExtMessageInfo deploy(byte[] signedBody) {
    if (nonNull(tonCenterClient)) {
      return send(prepareDeployMsg(signedBody));
    }
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

  public ExtMessageInfo send(WalletV1R3Config config, byte[] signedBodyHash) {
    if (nonNull(tonCenterClient)) {
      return send(prepareExternalMsg(config, signedBodyHash));
    }
    if (nonNull(adnlLiteClient)) {
      return send(prepareExternalMsg(config, signedBodyHash));
    }
    return tonlib.sendRawMessage(prepareExternalMsg(config, signedBodyHash).toCell().toBase64());
  }

  public Message prepareExternalMsg(WalletV1R3Config config, byte[] signedBodyHash) {
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

  public Cell createInternalSignedBody(WalletV1R3Config config) {
    Cell body = createTransferBody(config);
    byte[] signature = Utils.signData(keyPair.getPublicKey(), keyPair.getSecretKey(), body.hash());

    return CellBuilder.beginCell().storeCell(body).storeBytes(signature).endCell();
  }

  public Message prepareInternalMsg(WalletV1R3Config config) {
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

  public MessageRelaxed prepareInternalMsgRelaxed(WalletV1R3Config config) {
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
