package org.ton.java.smartcontract.integrationtests;

import static java.util.Objects.isNull;

import com.iwebpp.crypto.TweetNaclFast;
import java.math.BigInteger;
import java.time.Instant;
import lombok.Builder;
import lombok.Getter;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.smartcontract.types.CustomContractConfig;
import org.ton.java.smartcontract.wallet.Contract;
import org.ton.java.tlb.*;
import org.ton.java.tonlib.Tonlib;
import org.ton.java.tonlib.types.ExtMessageInfo;
import org.ton.java.utils.Utils;

@Builder
@Getter
public class ExampleContract implements Contract {

  TweetNaclFast.Signature.KeyPair keyPair;
  long initialSeqno;
  long initialExtraField;

  public static class ExampleContractBuilder {}

  public static ExampleContractBuilder builder() {
    return new CustomExampleContractBuilder();
  }

  private static class CustomExampleContractBuilder extends ExampleContractBuilder {
    @Override
    public ExampleContract build() {
      if (isNull(super.keyPair)) {
        super.keyPair = Utils.generateSignatureKeyPair();
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
    return "exampleContract";
  }

  @Override
  public Cell createDataCell() {
    return CellBuilder.beginCell()
        .storeUint(initialSeqno, 32) // seqno
        .storeBytes(keyPair.getPublicKey()) // 256 bits
        .storeUint(initialExtraField, 64) // stored_x_data
        .endCell();
  }

  @Override
  public Cell createCodeCell() {
    return CellBuilder.beginCell()
        .fromBoc(
            "B5EE9C7241010C0100B2000114FF00F4A413F4BCF2C80B01020120020302014804050094F28308D71820D31FD31FD33F02F823BBF263ED44D0D31FD3FFD33F305152BAF2A105F901541065F910F2A2F800019320D74A96D307D402FB00E8D103A4C8CB1F12CBFFCB3FCB3FC9ED540004D03002012006070201200809001DBDC3676A268698F98E9FF98EB859FC0017BB39CED44D0D31F31D70BFF80202710A0B0022AA77ED44D0D31F31D3FF31D33F31D70B3F0010A897ED44D0D70B1F56A9826C")
        .endCell();
  }

  public Cell createTransferBody(CustomContractConfig config) {

    Cell order =
        Message.builder()
            .info(
                InternalMessageInfo.builder()
                    .dstAddr(
                        MsgAddressIntStd.builder()
                            .workchainId(config.getDestination().wc)
                            .address(config.getDestination().toBigInteger())
                            .build())
                    .value(CurrencyCollection.builder().coins(config.getAmount()).build())
                    .build())
            .body(
                CellBuilder.beginCell().storeUint(0, 32).storeString(config.getComment()).endCell())
            .build()
            .toCell();

    CellBuilder message = CellBuilder.beginCell();

    message.storeUint(BigInteger.valueOf(config.getSeqno()), 32); // seqno
    message.storeUint(
        (config.getValidUntil() == 0)
            ? Instant.now().getEpochSecond() + 60
            : config.getValidUntil(),
        32);
    message.storeUint(BigInteger.valueOf(config.getExtraField()), 64); // extraField
    message.storeUint((config.getMode() == 0) ? 3 : config.getMode() & 0xff, 8);
    message.storeRef(order);
    return message.endCell();
  }

  public Cell createDeployMessage() {
    return CellBuilder.beginCell()
        .storeUint(initialSeqno, 32) // seqno
        .storeInt(-1, 32)
        .storeUint(initialExtraField, 64) // extra field
        .endCell();
  }

  public ExtMessageInfo deploy() {
    Cell body = createDeployMessage();

    Message externalMessage =
        Message.builder()
            .info(ExternalMessageInInfo.builder().dstAddr(getAddressIntStd()).build())
            .init(getStateInit())
            .body(
                CellBuilder.beginCell()
                    .storeBytes(
                        Utils.signData(keyPair.getPublicKey(), keyPair.getSecretKey(), body.hash()))
                    .storeCell(body)
                    .endCell())
            .build();

    return tonlib.sendRawMessage(externalMessage.toCell().toBase64());
  }

  public ExtMessageInfo send(CustomContractConfig config) {
    Cell body = createTransferBody(config);
    Message externalMessage =
        Message.builder()
            .info(ExternalMessageInInfo.builder().dstAddr(getAddressIntStd()).build())
            .body(
                CellBuilder.beginCell()
                    .storeBytes(
                        Utils.signData(keyPair.getPublicKey(), keyPair.getSecretKey(), body.hash()))
                    .storeCell(body)
                    .endCell())
            .build();

    return tonlib.sendRawMessage(externalMessage.toCell().toBase64());
  }
}
