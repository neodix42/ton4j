package org.ton.java.smartcontract.wallet.v3;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

import com.iwebpp.crypto.TweetNaclFast;
import java.time.Instant;
import lombok.Builder;
import lombok.Getter;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.smartcontract.types.WalletCodes;
import org.ton.java.smartcontract.types.WalletV3Config;
import org.ton.java.smartcontract.utils.MsgUtils;
import org.ton.java.smartcontract.wallet.Contract;
import org.ton.java.tlb.types.*;
import org.ton.java.tonlib.Tonlib;
import org.ton.java.tonlib.types.ExtMessageInfo;
import org.ton.java.tonlib.types.RunResult;
import org.ton.java.tonlib.types.TvmStackEntryNumber;
import org.ton.java.utils.Utils;

@Builder
@Getter
public class WalletV3R2 implements Contract {

  TweetNaclFast.Signature.KeyPair keyPair;
  long initialSeqno;
  long walletId;

  public static class WalletV3R2Builder {}

  public static WalletV3R2Builder builder() {
    return new CustomWalletV3R2Builder();
  }

  private static class CustomWalletV3R2Builder extends WalletV3R2Builder {
    @Override
    public WalletV3R2 build() {
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
    return "V3R2";
  }

  @Override
  public Cell createCodeCell() {
    return CellBuilder.beginCell().fromBoc(WalletCodes.V3R2.getValue()).endCell();
  }

  @Override
  public Cell createDataCell() {
    return CellBuilder.beginCell()
        .storeUint(initialSeqno, 32)
        .storeUint(walletId, 32)
        .storeBytes(keyPair.getPublicKey())
        .endCell();
  }

  public String getPublicKey() {

    RunResult result = tonlib.runMethod(getAddress(), "get_public_key");

    if (result.getExit_code() != 0) {
      throw new Error("method get_public_key, returned an exit code " + result.getExit_code());
    }

    TvmStackEntryNumber publicKeyNumber = (TvmStackEntryNumber) result.getStack().get(0);
    return publicKeyNumber.getNumber().toString(16);
  }

  /**
   * Creates message payload with subwallet-id, valid-until and seqno, equivalent to: <b
   * subwallet-id 32 u, timestamp 32 i, seqno 32 u, b> // signing message
   */
  public Cell createTransferBody(WalletV3Config config) {

    Cell order =
        Message.builder()
            .info(
                InternalMessageInfo.builder()
                    .bounce(config.isBounce())
                    .dstAddr(
                        MsgAddressIntStd.builder()
                            .workchainId(config.getDestination().wc)
                            .address(config.getDestination().toBigInteger())
                            .build())
                    .value(CurrencyCollection.builder().coins(config.getAmount()).build())
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
        .storeUint((config.getMode() == 0) ? 3 : config.getMode(), 8)
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
}
