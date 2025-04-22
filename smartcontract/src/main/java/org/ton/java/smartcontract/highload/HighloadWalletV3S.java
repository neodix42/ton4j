package org.ton.java.smartcontract.highload;

import static java.util.Objects.isNull;
import static org.ton.java.utils.Utils.HIGH_S;

import java.math.BigInteger;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.Builder;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.bouncycastle.util.BigIntegers;
import org.ton.java.address.Address;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.smartcontract.types.*;
import org.ton.java.smartcontract.types.Destination;
import org.ton.java.smartcontract.wallet.Contract;
import org.ton.java.tlb.*;
import org.ton.java.tonlib.Tonlib;
import org.ton.java.tonlib.types.*;
import org.ton.java.utils.Secp256k1KeyPair;
import org.ton.java.utils.SignatureWithRecovery;
import org.ton.java.utils.Utils;

/**
 * New version of Highload Wallet V3 where signature verification based on ed25519 replaced by
 * secp256k1.
 */
@Builder
@Getter
public class HighloadWalletV3S implements Contract {

  Secp256k1KeyPair keyPair;
  long walletId;
  long timeout;

  /**
   * interface to <a
   * href="https://github.com/ton-blockchain/highload-wallet-contract-v3/blob/main/contracts/highload-wallet-v3.func">highload-v3s
   * smart-contract</a>
   *
   * <p>Options - mandatory - highloadQueryId, walletId, publicKey
   */
  public static class HighloadWalletV3SBuilder {}

  public static HighloadWalletV3SBuilder builder() {
    return new CustomHighloadWalletV3SBuilder();
  }

  private static class CustomHighloadWalletV3SBuilder extends HighloadWalletV3SBuilder {
    @Override
    public HighloadWalletV3S build() {
      if (isNull(super.keyPair)) {
        super.keyPair = Utils.generateSecp256k1SignatureKeyPair();
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
    return "highload-v3s";
  }

  /**
   * Returns public_key of wallet with its parity bit.
   *
   * @param tonlib Tonlib instance
   * @return Pair, where left is parity, right - public key in hex format
   */
  public Pair<Long, String> getPublicKey(Tonlib tonlib) {

    Address myAddress = this.getAddress();
    RunResult result = tonlib.runMethod(myAddress, "get_public_key");

    if (result.getExit_code() != 0) {
      throw new Error("method get_public_key, returned an exit code " + result.getExit_code());
    }

    TvmStackEntryNumber parityNumber = (TvmStackEntryNumber) result.getStack().get(0);
    TvmStackEntryNumber publicKeyNumber = (TvmStackEntryNumber) result.getStack().get(0);
    // return publicKeyNumber.getNumber().toString(16);

    return Pair.of(parityNumber.getNumber().longValue(), publicKeyNumber.getNumber().toString(16));
  }

  /**
   * Calls get_subwallet_id method of a contract.
   *
   * @param address Address
   * @return long
   */
  public long getSubWalletId(Tonlib tonlib, Address address) {
    RunResult result = tonlib.runMethod(address, "get_subwallet_id");
    if (result.getExit_code() != 0) {
      throw new Error("method get_subwallet_id returned an exit code " + result.getExit_code());
    }

    TvmStackEntryNumber walletId = (TvmStackEntryNumber) result.getStack().get(0);
    return walletId.getNumber().longValue();
  }

  /**
   * Calls get_last_clean_time method of a contract.
   *
   * @param address Address
   * @return long
   */
  public long getLastCleanTime(Tonlib tonlib, Address address) {
    RunResult result = tonlib.runMethod(address, "get_last_clean_time");
    if (result.getExit_code() != 0) {
      throw new Error("method get_last_clean_time returned an exit code " + result.getExit_code());
    }

    TvmStackEntryNumber lastCleanTime = (TvmStackEntryNumber) result.getStack().get(0);
    return lastCleanTime.getNumber().longValue();
  }

  /**
   * Calls get_timeout method of a contract.
   *
   * @param address Address
   * @return long
   */
  public long get_timeout(Tonlib tonlib, Address address) {
    RunResult result = tonlib.runMethod(address, "get_timeout");
    if (result.getExit_code() != 0) {
      throw new Error("method get_timeout returned an exit code " + result.getExit_code());
    }

    TvmStackEntryNumber timeout = (TvmStackEntryNumber) result.getStack().get(0);
    return timeout.getNumber().longValue();
  }

  /**
   *
   *
   * <pre>
   * storage$_
   *  initial contract storage
   *  parity:uint1
   *  public_key:bits256
   *  subwallet_id:uint32
   *  old_queries:(HashmapE 14 ^Cell)
   *  queries:(HashmapE 14 ^Cell)
   *  last_clean_time:uint64
   *  timeout:uint22 = Storage;
   * </pre>
   *
   * @return Cell
   */
  @Override
  public Cell createDataCell() {
    return CellBuilder.beginCell()
        .storeUint(keyPair.getPublicKey()[0] % 2, 1)
        .storeBytes(Utils.slice(keyPair.getPublicKey(), 1, 32))
        .storeUint(walletId, 32)
        .storeBit(false) // old queries
        .storeBit(false) // queries
        .storeUint(0, 64) // last clean time
        .storeUint((timeout == 0) ? 5 * 60 : timeout, 22) // time out
        .endCell();
  }

  @Override
  public Cell createCodeCell() {
    return CellBuilder.beginCell().fromBoc(WalletCodes.highloadV3S.getValue()).endCell();
  }

  /**
   *
   *
   * <pre>
   * _ {n:#}  subwallet_id:uint32
   * message_to_send:^Cell
   * send_mode:uint8
   * query_id:QueryId
   * created_at:uint64
   * timeout:uint22 = MsgInner;
   * </pre>
   *
   * @return Cell
   */
  public Cell createTransferMessage(HighloadV3Config highloadConfig) {
    return CellBuilder.beginCell()
        .storeUint(highloadConfig.getWalletId(), 32)
        .storeRef(highloadConfig.getBody())
        .storeUint((highloadConfig.getMode() == 0) ? 3 : highloadConfig.getMode(), 8)
        .storeUint(highloadConfig.getQueryId(), 23)
        .storeUint(
            (highloadConfig.getCreatedAt() == 0)
                ? Instant.now().getEpochSecond() - 60
                : highloadConfig.getCreatedAt(),
            64)
        .storeUint((highloadConfig.getTimeOut() == 0) ? 5 * 60 : highloadConfig.getTimeOut(), 22)
        .endCell();
  }

  /**
   * @param highloadConfig HighloadV3Config
   */
  public ExtMessageInfo send(HighloadV3Config highloadConfig) {
    Address ownAddress = getAddress();

    Cell body = createTransferMessage(highloadConfig);

    SignatureWithRecovery signature =
        Utils.signDataSecp256k1(body.hash(), keyPair.getPrivateKey(), keyPair.getPublicKey());

    System.out.println(
        "recovered pub key send "
            + Utils.bytesToHex(
                Utils.recoverPublicKey(
                    signature.getR(), signature.getS(), signature.getV(), body.hash())));

    BigInteger s = BigIntegers.fromUnsignedByteArray(signature.getS());
    BigInteger highS = BigIntegers.fromUnsignedByteArray(HIGH_S);
    // second time
    if (s.compareTo(highS) >= 0) {
      System.out.println("NOT OK HIGH_S");
    }

    Message externalMessage =
        Message.builder()
            .info(
                ExternalMessageInInfo.builder()
                    .dstAddr(
                        MsgAddressIntStd.builder()
                            .workchainId(ownAddress.wc)
                            .address(ownAddress.toBigInteger())
                            .build())
                    .build())
            .body(
                CellBuilder.beginCell()
                    .storeBit((signature.getV()[0] & 1) == 1)
                    .storeBytes(signature.getR())
                    .storeBytes(signature.getS())
                    .storeRef(body)
                    .endCell())
            .build();

    return tonlib.sendRawMessage(externalMessage.toCell().toBase64());
  }

  /**
   * Sends amount of nano toncoins to destination address and waits till message found among
   * account's transactions
   */
  public RawTransaction sendWithConfirmation(HighloadV3Config highloadConfig) {
    Address ownAddress = getAddress();

    Cell body = createTransferMessage(highloadConfig);

    SignatureWithRecovery signature =
        Utils.signDataSecp256k1(body.hash(), keyPair.getPrivateKey(), keyPair.getPublicKey());

    Message externalMessage =
        Message.builder()
            .info(
                ExternalMessageInInfo.builder()
                    .dstAddr(
                        MsgAddressIntStd.builder()
                            .workchainId(ownAddress.wc)
                            .address(ownAddress.toBigInteger())
                            .build())
                    .build())
            .body(
                CellBuilder.beginCell()
                    .storeBit((signature.getV()[0] & 1) == 1)
                    .storeBytes(signature.getR())
                    .storeBytes(signature.getS())
                    .storeRef(body)
                    .storeRef(body)
                    .endCell())
            .build();
    return tonlib.sendRawMessageWithConfirmation(externalMessage.toCell().toBase64(), getAddress());
  }

  public ExtMessageInfo deploy(HighloadV3Config highloadConfig) {
    Address ownAddress = getAddress();

    if (isNull(highloadConfig.getBody())) {
      // dummy deploy msg
      highloadConfig.setBody(
          MessageRelaxed.builder()
              .info(
                  InternalMessageInfoRelaxed.builder()
                      .dstAddr(
                          MsgAddressIntStd.builder()
                              .workchainId(ownAddress.wc)
                              .address(ownAddress.toBigInteger())
                              .build())
                      .createdAt(
                          (highloadConfig.getCreatedAt() == 0)
                              ? Instant.now().getEpochSecond() - 60
                              : highloadConfig.getCreatedAt())
                      .build())
              .build()
              .toCell());
    }

    Cell innerMsg = createTransferMessage(highloadConfig);

    SignatureWithRecovery signature =
        Utils.signDataSecp256k1(innerMsg.hash(), keyPair.getPrivateKey(), keyPair.getPublicKey());

    Message externalMessage =
        Message.builder()
            .info(
                ExternalMessageInInfo.builder()
                    .dstAddr(
                        MsgAddressIntStd.builder()
                            .workchainId(ownAddress.wc)
                            .address(ownAddress.toBigInteger())
                            .build())
                    .build())
            .init(getStateInit())
            .body(
                CellBuilder.beginCell()
                    .storeBit((signature.getV()[0] & 1) == 1)
                    .storeBytes(signature.getR())
                    .storeBytes(signature.getS())
                    .storeRef(innerMsg)
                    .endCell())
            .build();
    return tonlib.sendRawMessage(externalMessage.toCell().toBase64());
  }

  public Cell createSingleTransfer(
      Address destAddress, BigInteger amount, Boolean bounce, StateInit stateInit, Cell body) {

    SignatureWithRecovery signature =
        Utils.signDataSecp256k1(body.hash(), keyPair.getPrivateKey(), keyPair.getPublicKey());

    return MessageRelaxed.builder()
        .info(
            InternalMessageInfoRelaxed.builder()
                .bounce(bounce)
                .srcAddr(MsgAddressExtNone.builder().build())
                .dstAddr(
                    MsgAddressIntStd.builder()
                        .workchainId(destAddress.wc)
                        .address(destAddress.toBigInteger())
                        .build())
                .value(CurrencyCollection.builder().coins(amount).build())
                .build())
        .init(stateInit)
        .body(
            CellBuilder.beginCell()
                .storeBit((signature.getV()[0] & 1) == 1)
                .storeBytes(signature.getR())
                .storeBytes(signature.getS())
                .storeRef(body)
                .endCell())
        .build()
        .toCell();
  }

  public Cell createSingleTransfer(
      Address destAddress,
      BigInteger amount,
      List<ExtraCurrency> extraCurrencies,
      Boolean bounce,
      StateInit stateInit,
      Cell body) {

    SignatureWithRecovery signature =
        Utils.signDataSecp256k1(body.hash(), keyPair.getPrivateKey(), keyPair.getPublicKey());

    return MessageRelaxed.builder()
        .info(
            InternalMessageInfoRelaxed.builder()
                .bounce(bounce)
                .srcAddr(MsgAddressExtNone.builder().build())
                .dstAddr(
                    MsgAddressIntStd.builder()
                        .workchainId(destAddress.wc)
                        .address(destAddress.toBigInteger())
                        .build())
                .value(
                    CurrencyCollection.builder()
                        .coins(amount)
                        .extraCurrencies(convertExtraCurrenciesToHashMap(extraCurrencies))
                        .build())
                .build())
        .init(stateInit)
        .body(
            CellBuilder.beginCell()
                .storeBit((signature.getV()[0] & 1) == 1)
                .storeBytes(signature.getR())
                .storeBytes(signature.getS())
                .storeRef(body)
                .endCell())
        .build()
        .toCell();
  }

  public Cell createBulkTransfer(BigInteger totalAmount, Cell bulkMessages) {
    Address ownAddress = getAddress();
    return MessageRelaxed.builder()
        .info(
            InternalMessageInfoRelaxed.builder()
                .dstAddr(
                    MsgAddressIntStd.builder()
                        .workchainId(ownAddress.wc)
                        .address(ownAddress.toBigInteger())
                        .build())
                .value(CurrencyCollection.builder().coins(totalAmount).build())
                .build())
        .body(bulkMessages)
        .build()
        .toCell();
  }

  public Cell createBulkTransfer(List<Destination> recipients, BigInteger queryId) {

    if (recipients.size() > 1000) {
      throw new IllegalArgumentException("Maximum number of recipients should be less than 1000");
    }

    BigInteger totalAmount = BigInteger.ZERO;

    for (Destination destination : recipients) {
      totalAmount =
          totalAmount.add(
              isNull(destination.getAmount()) ? BigInteger.ZERO : destination.getAmount());
    }

    List<Destination> tmpRecipients = new ArrayList<>(recipients);
    Cell chunk1, chunk2, chunk3, chunk4;

    chunk1 = addChunk(tmpRecipients.subList(0, Math.min(tmpRecipients.size(), 250)), queryId, null);
    tmpRecipients.subList(0, Math.min(tmpRecipients.size(), 250)).clear();
    if (tmpRecipients.isEmpty()) {
      return MessageRelaxed.builder()
          .info(
              InternalMessageInfoRelaxed.builder()
                  .dstAddr(getAddressIntStd())
                  .value(CurrencyCollection.builder().coins(totalAmount).build())
                  .build())
          .body(chunk1)
          .build()
          .toCell();
    } else {
      chunk2 =
          addChunk(tmpRecipients.subList(0, Math.min(tmpRecipients.size(), 250)), queryId, chunk1);
      tmpRecipients.subList(0, Math.min(tmpRecipients.size(), 250)).clear();
    }

    if (tmpRecipients.isEmpty()) {
      return MessageRelaxed.builder()
          .info(
              InternalMessageInfoRelaxed.builder()
                  .dstAddr(getAddressIntStd())
                  .value(
                      CurrencyCollection.builder()
                          .coins(
                              totalAmount.add(BigInteger.valueOf(Utils.toNano(0.01).longValue())))
                          .build())
                  .build())
          .body(chunk2)
          .build()
          .toCell();
    } else {
      chunk3 =
          addChunk(tmpRecipients.subList(0, Math.min(tmpRecipients.size(), 250)), queryId, chunk2);
      tmpRecipients.subList(0, Math.min(tmpRecipients.size(), 250)).clear();
    }

    if (tmpRecipients.isEmpty()) {
      return MessageRelaxed.builder()
          .info(
              InternalMessageInfoRelaxed.builder()
                  .dstAddr(getAddressIntStd())
                  .value(
                      CurrencyCollection.builder()
                          .coins(totalAmount.add(Utils.toNano(0.02)))
                          .build())
                  .build())
          .body(chunk3)
          .build()
          .toCell();
    } else {
      chunk4 =
          addChunk(tmpRecipients.subList(0, Math.min(tmpRecipients.size(), 250)), queryId, chunk3);
      tmpRecipients.subList(0, Math.min(tmpRecipients.size(), 250)).clear();
    }

    return MessageRelaxed.builder()
        .info(
            InternalMessageInfoRelaxed.builder()
                .dstAddr(getAddressIntStd())
                .value(
                    CurrencyCollection.builder().coins(totalAmount.add(Utils.toNano(0.03))).build())
                .build())
        .body(chunk4)
        .build()
        .toCell();
  }

  private Cell addChunk(List<Destination> destinations, BigInteger queryId, Cell enclosedMessages) {
    List<OutAction> outActions = new ArrayList<>();

    if (isNull(enclosedMessages)) {
      for (Destination destination : destinations) {
        outActions.add(convertDestinationToOutAction(destination, null));
      }
    } else {
      for (int i = 0; i <= destinations.size() - 1; i++) {
        outActions.add(convertDestinationToOutAction(destinations.get(i), null));
      }
      outActions.add(
          convertDestinationToOutAction(
              destinations.get(destinations.size() - 1), enclosedMessages));
    }

    return HighloadV3InternalMessageBody.builder()
        .queryId(queryId)
        .actions(OutList.builder().actions(outActions).build())
        .build()
        .toCell();
  }

  private OutAction convertDestinationToOutAction(Destination destination, Cell enclosedMessages) {
    Address dstAddress = Address.of(destination.getAddress());

    if (isNull(enclosedMessages)) {
      return ActionSendMsg.builder()
          .mode((destination.getMode() == 0) ? 3 : destination.getMode())
          .outMsg(
              MessageRelaxed.builder()
                  .info(
                      InternalMessageInfoRelaxed.builder()
                          .bounce(destination.isBounce())
                          .dstAddr(
                              MsgAddressIntStd.builder()
                                  .workchainId(dstAddress.wc)
                                  .address(dstAddress.toBigInteger())
                                  .build())
                          .value(
                              CurrencyCollection.builder()
                                  .coins(destination.getAmount())
                                  .extraCurrencies(
                                      convertExtraCurrenciesToHashMap(
                                          destination.getExtraCurrencies()))
                                  .build())
                          .build())
                  // .init() is not supported
                  .body(
                      (isNull(destination.getBody())
                              && StringUtils.isNotEmpty(destination.getComment()))
                          ? CellBuilder.beginCell()
                              .storeUint(0, 32) // 0 opcode means we have a comment
                              .storeString(destination.getComment())
                              .endCell()
                          : destination.getBody())
                  .build())
          .build();
    } else {
      return ActionSendMsg.builder()
          .mode(3)
          .outMsg(
              MessageRelaxed.builder()
                  .info(
                      InternalMessageInfoRelaxed.builder()
                          .dstAddr(getAddressIntStd()) // self needs value, connecting message
                          .value(CurrencyCollection.builder().coins(Utils.toNano(0.01)).build())
                          .build())
                  .body(enclosedMessages)
                  .build())
          .build();
    }
  }
}
