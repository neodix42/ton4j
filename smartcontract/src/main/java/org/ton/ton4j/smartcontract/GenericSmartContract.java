package org.ton.ton4j.smartcontract;

import static java.util.Objects.isNull;

import com.iwebpp.crypto.TweetNaclFast;
import lombok.Builder;
import lombok.Data;
import org.ton.java.adnl.AdnlLiteClient;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellBuilder;
import org.ton.ton4j.smartcontract.wallet.Contract;
import org.ton.ton4j.tlb.*;
import org.ton.ton4j.toncenter.TonCenter;
import org.ton.ton4j.tonlib.Tonlib;
import org.ton.ton4j.tonlib.types.ExtMessageInfo;
import org.ton.ton4j.utils.Utils;

@Builder
@Data
public class GenericSmartContract implements Contract {

  TweetNaclFast.Signature.KeyPair keyPair;
  byte[] publicKey;

  String code;
  String data;
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
  public long getWorkchain() {
    return wc;
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
  public String getName() {
    return "GenericSmartContract";
  }

  @Override
  public Cell createCodeCell() {
    return CellBuilder.beginCell().fromBoc(code).endCell();
  }

  @Override
  public Cell createDataCell() {
    return CellBuilder.beginCell().fromBoc(data).endCell();
  }

  public static class GenericSmartContractBuilder {}

  public static GenericSmartContractBuilder builder() {
    return new CustomGenericSmartContractBuilder();
  }

  private static class CustomGenericSmartContractBuilder extends GenericSmartContractBuilder {
    @Override
    public GenericSmartContract build() {
      if (isNull(super.publicKey)) {
        if (isNull(super.keyPair)) {
          super.keyPair = Utils.generateSignatureKeyPair();
        }
      }
      return super.build();
    }
  }

  /**
   * Deploy with body
   *
   * @param deployMessageBody usually stands for internal message
   * @return ExtMessageInfo
   */
  public ExtMessageInfo deploy(Cell deployMessageBody) {
    Message msg = prepareDeployMsg(deployMessageBody);
    if (java.util.Objects.nonNull(tonCenterClient)) {
      return send(msg);
    }
    if (java.util.Objects.nonNull(adnlLiteClient)) {
      return send(msg);
    }
    return tonlib.sendRawMessage(msg.toCell().toBase64());
  }

  /**
   * Deploy with body without signing it.
   *
   * @param deployMessageBody usually stands for internal message
   * @return ExtMessageInfo
   */
  public ExtMessageInfo deployWithoutSignature(Cell deployMessageBody) {
    Message msg = prepareDeployMsgWithoutSignature(deployMessageBody);
    if (java.util.Objects.nonNull(tonCenterClient)) {
      return send(msg);
    }
    if (java.util.Objects.nonNull(adnlLiteClient)) {
      return send(msg);
    }
    return tonlib.sendRawMessage(msg.toCell().toBase64());
  }

  /**
   * Deploy without body
   *
   * @return ExtMessageInfo
   */
  public ExtMessageInfo deploy() {
    Message msg = prepareDeployMsgWithoutBody();
    if (java.util.Objects.nonNull(tonCenterClient)) {
      return send(msg);
    }
    if (java.util.Objects.nonNull(adnlLiteClient)) {
      return send(msg);
    }
    return tonlib.sendRawMessage(msg.toCell().toBase64());
  }

  public ExtMessageInfo deploy(Cell deployMessageBody, byte[] signedBody) {
    Message msg = prepareDeployMsg(deployMessageBody, signedBody);
    if (java.util.Objects.nonNull(tonCenterClient)) {
      return send(msg);
    }
    if (java.util.Objects.nonNull(adnlLiteClient)) {
      return send(msg);
    }
    return tonlib.sendRawMessage(msg.toCell().toBase64());
  }

  public Message prepareDeployMsgWithoutBody() {
    return Message.builder()
        .info(ExternalMessageInInfo.builder().dstAddr(getAddressIntStd()).build())
        .init(getStateInit())
        .build();
  }

  public Message prepareDeployMsg(Cell deployMessageBody, byte[] signedBodyHash) {

    return Message.builder()
        .info(ExternalMessageInInfo.builder().dstAddr(getAddressIntStd()).build())
        .init(getStateInit())
        .body(
            CellBuilder.beginCell()
                .storeBytes(signedBodyHash)
                .storeCell(deployMessageBody)
                .endCell())
        .build();
  }

  public Message prepareDeployMsg(Cell deployMessageBody) {

    return Message.builder()
        .info(ExternalMessageInInfo.builder().dstAddr(getAddressIntStd()).build())
        .init(getStateInit())
        .body(
            CellBuilder.beginCell()
                .storeBytes(
                    Utils.signData(
                        keyPair.getPublicKey(), keyPair.getSecretKey(), deployMessageBody.hash()))
                .storeCell(deployMessageBody)
                .endCell())
        .build();
  }

  public Message prepareDeployMsgWithoutSignature(Cell deployMessageBody) {

    return Message.builder()
        .info(ExternalMessageInInfo.builder().dstAddr(getAddressIntStd()).build())
        .init(getStateInit())
        .body(deployMessageBody)
        .build();
  }
}
