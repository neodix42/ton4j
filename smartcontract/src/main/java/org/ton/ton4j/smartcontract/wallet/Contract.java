package org.ton.ton4j.smartcontract.wallet;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

import java.math.BigInteger;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.ton.java.adnl.AdnlLiteClient;
import org.ton.ton4j.address.Address;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.TonHashMapE;
import org.ton.ton4j.smartcontract.types.WalletConfig;
import org.ton.ton4j.smartcontract.wallet.v1.WalletV1R1;
import org.ton.ton4j.tl.liteserver.responses.SendMsgStatus;
import org.ton.ton4j.tlb.*;
import org.ton.ton4j.tonlib.Tonlib;
import org.ton.ton4j.tonlib.types.*;
import org.ton.ton4j.utils.Utils;

/** Interface for all smart contract objects in ton4j. */
public interface Contract {

  Tonlib getTonlib();

  AdnlLiteClient getAdnlLiteClient();

  /**
   * Used for late tonlib assignment
   *
   * @param pTonlib Tonlib instance
   */
  void setTonlib(Tonlib pTonlib);

  void setAdnlLiteClient(AdnlLiteClient pAdnlClient);

  long getWorkchain();

  String getName();

  default Address getAddress() {
    return StateInit.builder()
        .code(createCodeCell())
        .data(createDataCell())
        .build()
        .getAddress(getWorkchain());
  }

  default Address getAddress(byte workchain) {
    return getStateInit().getAddress(workchain);
  }

  default MsgAddressIntStd getAddressIntStd() {
    Address ownAddress = getStateInit().getAddress(getWorkchain());
    return MsgAddressIntStd.builder()
        .workchainId(ownAddress.wc)
        .address(ownAddress.toBigInteger())
        .build();
  }

  default MsgAddressIntStd getAddressIntStd(int workchain) {
    Address ownAddress = getStateInit().getAddress();
    return MsgAddressIntStd.builder()
        .workchainId((byte) workchain)
        .address(ownAddress.toBigInteger())
        .build();
  }

  /**
   * @return Cell containing contact code
   */
  Cell createCodeCell();

  /**
   * Method to override
   *
   * @return {Cell} cell contains contract data
   */
  Cell createDataCell();

  default Cell createLibraryCell() {
    return null;
  }

  /**
   * Message StateInit consists of initial contract code, data and address in a blockchain
   *
   * @return StateInit
   */
  default StateInit getStateInit() {
    return StateInit.builder()
        .code(createCodeCell())
        .data(createDataCell())
        .lib(createLibraryCell())
        .build();
  }

  default long getSeqno() {

    if (this instanceof WalletV1R1) {
      throw new Error("Wallet V1R1 does not have seqno method");
    }
    if (nonNull(getAdnlLiteClient())) {
      try {
        return getAdnlLiteClient().getSeqno(getAddress());
      } catch (Exception e) {
        throw new Error(e);
      }
    }

    return getTonlib().getSeqno(getAddress());
  }

  default boolean isDeployed() {
    if (nonNull(getAdnlLiteClient())) {
      try {
        return (getAdnlLiteClient().getAccount(getAddress()).getAccountStorage().getAccountState()
            instanceof AccountStateActive);
      } catch (Exception e) {
        return false;
      }
    }
    return StringUtils.isNotEmpty(getTonlib().getRawAccountState(getAddress()).getCode());
  }

  default ExtMessageInfo send(Message externalMessage) {
    if (nonNull(getAdnlLiteClient())) {

      SendMsgStatus sendMsgStatus =
          getAdnlLiteClient()
              .sendMessage(
                  externalMessage); // raw boc // prepareExternalMsg(config).toCell().toBoc()
      if (StringUtils.isEmpty(sendMsgStatus.getResponseMessage())) {
        return ExtMessageInfo.builder()
            .error(
                TonlibError.builder()
                    .code(0)
                    .build()) // compatibility, if user checks tonlib error when using adnl client
            .adnlLiteClientError(
                AdnlLiteClientError.builder().code(sendMsgStatus.getStatus()).build())
            .build();
      } else {
        return ExtMessageInfo.builder()
            .error(
                TonlibError.builder()
                    .code(
                        sendMsgStatus.getResponseCode() == 0 ? 1 : sendMsgStatus.getResponseCode())
                    .build()) // if user checks tonlib error when using adnl client
            .adnlLiteClientError(
                AdnlLiteClientError.builder()
                    .code(
                        sendMsgStatus.getResponseCode() == 0 ? 1 : sendMsgStatus.getResponseCode())
                    .message(sendMsgStatus.getResponseMessage())
                    .build())
            .build();
      }
    }
    return getTonlib().sendRawMessage(externalMessage.toCell().toBase64()); // base64
  }

  default void sendWithConfirmation(Message externalMessage) throws Exception {
    if (nonNull(getAdnlLiteClient())) {
      getAdnlLiteClient().sendRawMessageWithConfirmation(externalMessage, getAddress());
    } else {
      getTonlib().sendRawMessageWithConfirmation(externalMessage.toCell().toBase64(), getAddress());
    }
  }

  /** Checks every 2 seconds for 60 seconds if account state was deployed at address */
  default void waitForDeployment() {
    waitForDeployment(60);
  }

  /** Checks every 2 seconds for timeoutSeconds if account state was deployed at address */
  default void waitForDeployment(int timeoutSeconds) {
    int i = 0;
    do {
      if (++i * 2 >= timeoutSeconds) {
        throw new Error("Can't deploy contract within specified timeout.");
      }
      Utils.sleep(2);
    } while (!isDeployed());
  }

  /** Checks every 2 seconds for 60 if account balance was changed */
  default void waitForBalanceChange() {
    waitForBalanceChange(60);
  }

  /**
   * Checks every 2 seconds for timeoutSeconds if account balance was changed. Notice, storage fee
   * changes often by 1 nanocoin with few seconds, if you need to tolerate that consider using
   * waitForBalanceChangeWithTolerance().
   */
  default void waitForBalanceChange(int timeoutSeconds) {
    BigInteger initialBalance = getBalance();
    int i = 0;
    do {
      if (++i * 2 >= timeoutSeconds) {
        throw new Error("Balance was not changed within specified timeout.");
      }
      Utils.sleep(2);
    } while (initialBalance.equals(getBalance()));
  }

  /**
   * returns if balance has changed by +/- tolerateNanoCoins within timeoutSeconds, otherwise throws
   * an error.
   *
   * @param timeoutSeconds timeout in seconds
   * @param tolerateNanoCoins tolerate value
   */
  default void waitForBalanceChangeWithTolerance(int timeoutSeconds, BigInteger tolerateNanoCoins) {

    BigInteger initialBalance = getBalance();
    long diff;
    int i = 0;
    do {
      if (++i * 2 >= timeoutSeconds) {
        throw new Error("Balance was not changed within specified timeout.");
      }
      Utils.sleep(2);
      BigInteger currentBalance = getBalance();

      diff =
          Math.max(currentBalance.longValue(), initialBalance.longValue())
              - Math.min(currentBalance.longValue(), initialBalance.longValue());
    } while (diff < tolerateNanoCoins.longValue());
  }

  default BigInteger getBalance() {
    if (nonNull(getAdnlLiteClient())) {
      try {
        return getAdnlLiteClient().getBalance(getAddress());
      } catch (Exception e) {
        throw new Error(e);
      }
    }
    return new BigInteger(getTonlib().getRawAccountState(getAddress()).getBalance());
  }

  default List<RawTransaction> getTransactions(int historyLimit) {
    return getTonlib()
        .getAllRawTransactions(getAddress().toBounceable(), BigInteger.ZERO, null, historyLimit)
        .getTransactions();
  }

  default List<RawTransaction> getTransactions() {
    return getTonlib()
        .getAllRawTransactions(getAddress().toBounceable(), BigInteger.ZERO, null, 20)
        .getTransactions();
  }

  default List<Transaction> getTransactionsTlb() {
    if (isNull(getAdnlLiteClient())) {
      throw new Error("ADNL lite client not initialized");
    }
    try {
      return getAdnlLiteClient().getTransactions(getAddress(), 0, null, 20).getTransactionsParsed();
    } catch (Exception e) {
      throw new Error(e);
    }
  }

  default List<Transaction> getTransactionsTlb(int lt, int historyLimit) {
    if (isNull(getAdnlLiteClient())) {
      throw new Error("ADNL lite client not initialized");
    }
    try {
      return getTransactionsTlb(lt, null, historyLimit);
    } catch (Exception e) {
      throw new Error(e);
    }
  }

  default List<Transaction> getTransactionsTlb(int lt, byte[] hash, int historyLimit) {
    if (isNull(getAdnlLiteClient())) {
      throw new Error("ADNL lite client not initialized");
    }
    try {
      return getAdnlLiteClient()
          .getTransactions(getAddress(), lt, hash, historyLimit)
          .getTransactionsParsed();
    } catch (Exception e) {
      throw new Error(e);
    }
  }

  default List<Transaction> getTransactionsTlb(int historyLimit) {
    if (isNull(getAdnlLiteClient())) {
      throw new Error("ADNL lite client not initialized");
    }
    try {
      return getTransactionsTlb(0, new byte[32], historyLimit);
    } catch (Exception e) {
      throw new Error(e);
    }
  }

  default Message prepareDeployMsg() {
    throw new Error("not implemented");
  }

  default Message prepareExternalMsg(WalletConfig config) {
    throw new Error("not implemented");
  }

  default BigInteger getGasFees() {
    switch (getName()) {
      case "V1R1":
        return BigInteger.valueOf(40000); // 0.00004 toncoins
      case "V1R2":
        return BigInteger.valueOf(40000);
      case "V1R3":
        return BigInteger.valueOf(40000);
      case "V2R1":
        return BigInteger.valueOf(40000);
      case "V2R2":
        return BigInteger.valueOf(40000);
      case "V3R1":
        return BigInteger.valueOf(40000);
      case "V3R2":
        return BigInteger.valueOf(40000);
      case "V4R2":
        return BigInteger.valueOf(310000);
      default:
        throw new Error("Unknown wallet version");
    }
  }

  default TonHashMapE convertExtraCurrenciesToHashMap(List<ExtraCurrency> extraCurrencies) {

    if (isNull(extraCurrencies)) {
      return null;
    }
    TonHashMapE x = new TonHashMapE(32);

    for (ExtraCurrency ec : extraCurrencies) {
      x.elements.put(ec.getId(), ec.getAmount());
    }
    return x;
  }

  default RawTransaction waitForExtraCurrency(long extraCurrencyId) {
    return waitForExtraCurrency(extraCurrencyId, null, null, 10);
  }

  default RawTransaction waitForExtraCurrency(
      long extraCurrencyId, BigInteger fromTxLt, String fromTxHash) {
    return waitForExtraCurrency(extraCurrencyId, fromTxLt, fromTxHash, 10);
  }

  /**
   * @param extraCurrencyId custom specified extra-currency id
   * @param attempts number of attempts to wait for tx with EC, an attempt runs every 5 sec.
   * @return RawTransaction if found
   */
  default RawTransaction waitForExtraCurrency(
      long extraCurrencyId, BigInteger fromTxLt, String fromTxHash, int attempts) {
    // todo
    for (int i = 0; i < attempts; i++) {
      RawTransactions txs =
          getTonlib().getRawTransactions(getAddress().toRaw(), fromTxLt, fromTxHash);
      for (RawTransaction tx : txs.getTransactions()) {
        for (ExtraCurrency ec : tx.getIn_msg().getExtra_currencies()) {
          if (ec.getId() == extraCurrencyId) {
            Transaction tlbTransaction = tx.getTransactionAsTlb();
            if (tlbTransaction.getDescription() instanceof TransactionDescriptionOrdinary) {
              BouncePhase bouncePhase = tlbTransaction.getOrdinaryTransaction().getBouncePhase();
              if (isNull(bouncePhase)) {
                return tx;
              } else {
                if (bouncePhase instanceof BouncePhaseOk) {
                  throw new Error("extra-currency transaction has bounced");
                } else {
                  return tx;
                }
              }
            } else {
              return tx;
            }
          }
        }
      }
      Utils.sleep(5);
    }
    throw new Error("time out waiting for extra-currency with id " + extraCurrencyId);
  }
}
