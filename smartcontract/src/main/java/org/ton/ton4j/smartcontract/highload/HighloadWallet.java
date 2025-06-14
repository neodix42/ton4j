package org.ton.ton4j.smartcontract.highload;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

import com.iwebpp.crypto.TweetNaclFast;
import java.math.BigInteger;
import lombok.Builder;
import lombok.Getter;
import org.ton.java.adnl.AdnlLiteClient;
import org.ton.ton4j.address.Address;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellBuilder;
import org.ton.ton4j.cell.TonHashMap;
import org.ton.ton4j.smartcontract.types.Destination;
import org.ton.ton4j.smartcontract.types.HighloadConfig;
import org.ton.ton4j.smartcontract.types.WalletCodes;
import org.ton.ton4j.smartcontract.utils.MsgUtils;
import org.ton.ton4j.smartcontract.wallet.Contract;
import org.ton.ton4j.tlb.*;
import org.ton.ton4j.tonlib.Tonlib;
import org.ton.ton4j.tonlib.types.ExtMessageInfo;
import org.ton.ton4j.utils.Utils;

/**
 * @deprecated - will be removed in future releases, please switch to Highload Wallet v3
 */
@Builder
@Getter
@Deprecated
public class HighloadWallet implements Contract {

  // https://github.com/ton-blockchain/ton/blob/master/crypto/smartcont/highload-wallet-v2-code.fc
  TweetNaclFast.Signature.KeyPair keyPair;
  long walletId;
  byte[] publicKey;
  BigInteger queryId;

  public static class HighloadWalletBuilder {}

  public static HighloadWalletBuilder builder() {
    return new CustomHighloadWalletBuilder();
  }

  private static class CustomHighloadWalletBuilder extends HighloadWalletBuilder {
    @Override
    public HighloadWallet build() {
      if (isNull(super.keyPair)) {
        super.keyPair = Utils.generateSignatureKeyPair();
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

  /**
   * interface to <a
   * href="https://github.com/ton-blockchain/ton/blob/master/crypto/smartcont/highload-wallet-v2-code.fc">highload
   * smart-contract</a>
   */
  //    public HighloadWallet(Options options) {
  //        options.code =
  // CellBuilder.beginCell().fromBoc(WalletCodes.highload.getValue()).endCell();
  //    }
  @Override
  public String getName() {
    return "highload-v2";
  }

  /**
   * initial contract storage
   *
   * @return Cell
   */
  @Override
  public Cell createDataCell() {
    CellBuilder cell = CellBuilder.beginCell();
    cell.storeUint(walletId, 32); // wallet id
    cell.storeUint(BigInteger.ZERO, 64); // last_cleaned
    cell.storeBytes(isNull(keyPair) ? publicKey : keyPair.getPublicKey()); // 256 bits
    cell.storeBit(false); // initial storage has old_queries dict empty
    return cell.endCell();
  }

  @Override
  public Cell createCodeCell() {
    return CellBuilder.beginCell().fromBoc(WalletCodes.highload.getValue()).endCell();
  }

  public Cell createDeployMessage() {
    return CellBuilder.beginCell()
        .storeUint(walletId, 32)
        .storeUint(queryId, 64) // query id
        .storeBit(false)
        .endCell();
  }

  public Cell createTransferBody(HighloadConfig config) {
    CellBuilder body = CellBuilder.beginCell();
    body.storeUint(config.getWalletId(), 32);
    body.storeUint(config.getQueryId(), 64);
    if (nonNull(config.getDestinations())) {
      body.storeBit(true);
      body.storeRef(createDict(config));
    } else {
      body.storeBit(false);
    }
    return body.endCell();
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

  //    public Cell createSigningMessageInternal(HighloadConfig highloadConfig) {
  //        CellBuilder message = CellBuilder.beginCell();
  //        message.storeUint(BigInteger.valueOf(getOptions().walletId), 32);
  //        message.storeUint(highloadConfig.getQueryId(), 64);
  //        message.storeBit(true);
  //        message.storeRef(createDict(highloadConfig));
  //        return message.endCell();
  //    }

  public String getPublicKey() throws Exception {
    if (nonNull(adnlLiteClient)) {
      return Utils.bytesToHex(Utils.to32ByteArray(adnlLiteClient.getPublicKey(getAddress())));
    }
    return Utils.bytesToHex(Utils.to32ByteArray(tonlib.getPublicKey(getAddress())));
  }

  /**
   * Sends to up to 84 destinations
   *
   * @param highloadConfig HighloadConfig
   */
  public ExtMessageInfo send(HighloadConfig highloadConfig) {
    Cell body = createTransferBody(highloadConfig);

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
    if (nonNull(adnlLiteClient)) {
      return send(externalMessage);
    }
    return tonlib.sendRawMessage(externalMessage.toCell().toBase64());
  }

  private Cell createDict(HighloadConfig config) {
    int dictKeySize = 16;
    TonHashMap dictDestinations = new TonHashMap(dictKeySize);

    long i = 0; // key, index 16bit
    for (Destination destination : config.getDestinations()) {

      Cell order;
      if (destination.isBounce()) {
        if (isNull(destination.getBody()) && nonNull(destination.getComment())) {
          order =
              MsgUtils.createInternalMessage(
                      getAddress(),
                      Address.of(destination.getAddress()),
                      destination.getAmount(),
                      destination.getExtraCurrencies(),
                      null,
                      CellBuilder.beginCell()
                          .storeUint(0, 32)
                          .storeString(destination.getComment())
                          .endCell(),
                      true)
                  .toCell();
        } else if (nonNull(destination.getBody())) {
          order =
              MsgUtils.createInternalMessage(
                      getAddress(),
                      Address.of(destination.getAddress()),
                      destination.getAmount(),
                      destination.getExtraCurrencies(),
                      null,
                      destination.getBody(),
                      true)
                  .toCell();
        } else {
          order =
              MsgUtils.createInternalMessage(
                      getAddress(),
                      Address.of(destination.getAddress()),
                      destination.getAmount(),
                      destination.getExtraCurrencies(),
                      null,
                      null,
                      true)
                  .toCell();
        }
      } else {
        if (isNull(destination.getBody()) && nonNull(destination.getComment())) {
          order =
              MsgUtils.createInternalMessage(
                      getAddress(),
                      Address.of(destination.getAddress()),
                      destination.getAmount(),
                      destination.getExtraCurrencies(),
                      null,
                      CellBuilder.beginCell()
                          .storeUint(0, 32)
                          .storeString(destination.getComment())
                          .endCell(),
                      false)
                  .toCell();
        } else if (nonNull(destination.getBody())) {
          order =
              MsgUtils.createInternalMessage(
                      getAddress(),
                      Address.of(destination.getAddress()),
                      destination.getAmount(),
                      destination.getExtraCurrencies(),
                      null,
                      destination.getBody(),
                      false)
                  .toCell();
        } else {
          order =
              MsgUtils.createInternalMessage(
                      getAddress(),
                      Address.of(destination.getAddress()),
                      destination.getAmount(),
                      destination.getExtraCurrencies(),
                      null,
                      null,
                      false)
                  .toCell();
        }
      }

      CellBuilder p =
          CellBuilder.beginCell()
              .storeUint(
                  isNull(destination.getSendMode())
                      ? ((destination.getMode() == 0) ? 3 : destination.getMode())
                      : destination.getSendMode().getValue(),
                  8)
              .storeRef(order);

      dictDestinations.elements.put(i++, p.endCell());
    }

    Cell cellDict =
        dictDestinations.serialize(
            k -> CellBuilder.beginCell().storeUint((Long) k, dictKeySize).endCell().getBits(),
            v -> (Cell) v);

    return cellDict;
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
    if (nonNull(adnlLiteClient)) {
      return send(externalMessage);
    }
    return tonlib.sendRawMessage(externalMessage.toCell().toBase64());
  }
}
