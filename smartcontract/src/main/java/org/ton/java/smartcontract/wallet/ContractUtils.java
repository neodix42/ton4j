package org.ton.java.smartcontract.wallet;

import java.math.BigInteger;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.ton.java.address.Address;
import org.ton.java.smartcontract.token.ft.JettonMinter;
import org.ton.java.smartcontract.token.ft.JettonWallet;
import org.ton.java.tonlib.Tonlib;
import org.ton.java.utils.Utils;

@Slf4j
public class ContractUtils {

  public static long getSeqno(Tonlib tonlib, Address address) {
    return tonlib.getSeqno(address);
  }

  public static boolean isDeployed(Tonlib tonlib, Address address) {
    return StringUtils.isNotEmpty(tonlib.getRawAccountState(address).getCode());
  }

  @Deprecated
  public static void waitForDeployment(Tonlib tonlib, Address address, int timeoutSeconds) {
    log.info("Waiting for deployment (up to {}s) - {}", timeoutSeconds, address.toRaw());
    int i = 0;
    do {
      if (++i * 2 >= timeoutSeconds) {
        throw new Error("Can't deploy contract within specified timeout.");
      }
      Utils.sleep(2);
    } while (!isDeployed(tonlib, address));
  }

  @Deprecated
  public static void waitForBalanceChange(Tonlib tonlib, Address address, int timeoutSeconds) {
    log.info("Waiting for balance change (up to {}s) - {}", timeoutSeconds, address.toRaw());
    BigInteger initialBalance = tonlib.getAccountBalance(address);
    int i = 0;
    do {
      if (++i * 2 >= timeoutSeconds) {
        throw new Error("Balance was not changed within specified timeout.");
      }
      Utils.sleep(2);
    } while (initialBalance.equals(tonlib.getAccountBalance(address)));
  }

  public static void waitForJettonBalanceChange(
      Tonlib tonlib, Address jettonMinter, Address address, int timeoutSeconds) {
    log.info("Waiting for jetton balance change (up to {}s) - {}", timeoutSeconds, address.toRaw());
    BigInteger initialBalance = getJettonBalance(tonlib, jettonMinter, address);
    int i = 0;
    do {
      if (++i * 2 >= timeoutSeconds) {
        throw new Error(
            "Balance of " + address.toRaw() + " was not changed within specified timeout.");
      }
      Utils.sleep(2);
    } while (initialBalance.equals(getJettonBalance(tonlib, jettonMinter, address)));
  }

  public static BigInteger getJettonBalance(
      Tonlib tonlib, Address jettonMinter, Address destinationAddress) {

    try {
      JettonMinter jettonMinterWallet =
          JettonMinter.builder().tonlib(tonlib).customAddress(jettonMinter).build();

      JettonWallet jettonWallet = jettonMinterWallet.getJettonWallet(destinationAddress);

      return jettonWallet.getBalance();
    } catch (Error e) {
      return new BigInteger("-1");
    }
  }
}
