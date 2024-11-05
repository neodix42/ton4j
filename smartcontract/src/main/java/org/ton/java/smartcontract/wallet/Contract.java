package org.ton.java.smartcontract.wallet;

import java.math.BigInteger;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.ton.java.address.Address;
import org.ton.java.cell.Cell;
import org.ton.java.smartcontract.types.WalletConfig;
import org.ton.java.smartcontract.wallet.v1.WalletV1R1;
import org.ton.java.tlb.types.Message;
import org.ton.java.tlb.types.MsgAddressIntStd;
import org.ton.java.tlb.types.StateInit;
import org.ton.java.tonlib.Tonlib;
import org.ton.java.tonlib.types.RawTransaction;
import org.ton.java.utils.Utils;

/** Interface for all smart contract objects in ton4j. */
public interface Contract {

  Tonlib getTonlib();

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

    return getTonlib().getSeqno(getAddress());
  }

  default boolean isDeployed() {
    return StringUtils.isNotEmpty(getTonlib().getRawAccountState(getAddress()).getCode());
  }

  default void waitForDeployment(int timeoutSeconds) {
    System.out.println("waiting for deployment (up to " + timeoutSeconds + "s)");
    int i = 0;
    do {
      if (++i * 2 >= timeoutSeconds) {
        throw new Error("Can't deploy contract within specified timeout.");
      }
      Utils.sleep(2);
    } while (!isDeployed());
  }

  default void waitForBalanceChange(int timeoutSeconds) {
    System.out.println("waiting for balance change (up to " + timeoutSeconds + "s)");
    BigInteger initialBalance = getBalance();
    int i = 0;
    do {
      if (++i * 2 >= timeoutSeconds) {
        throw new Error("Balance was not changed within specified timeout.");
      }
      Utils.sleep(2);
    } while (initialBalance.equals(getBalance()));
  }

  default BigInteger getBalance() {
    return new BigInteger(getTonlib().getAccountState(getAddress()).getBalance());
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
}
