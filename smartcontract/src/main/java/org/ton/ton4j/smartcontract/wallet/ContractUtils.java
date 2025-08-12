package org.ton.ton4j.smartcontract.wallet;

import java.math.BigInteger;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.ton.ton4j.adnl.AdnlLiteClient;
import org.ton.ton4j.address.Address;
import org.ton.ton4j.smartcontract.token.ft.JettonMinter;
import org.ton.ton4j.smartcontract.token.ft.JettonWallet;
import org.ton.ton4j.toncenter.TonCenter;
import org.ton.ton4j.tonlib.Tonlib;
import org.ton.ton4j.utils.Utils;

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
      System.out.println("tonlib - jettonWallet " + jettonWallet.getAddress().toRaw());

      return jettonWallet.getBalance();
    } catch (Error e) {
      return new BigInteger("-1");
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public static void waitForJettonBalanceChange(
      AdnlLiteClient adnlLiteClient, Address jettonMinter, Address address, int timeoutSeconds) {
    log.info("Waiting for jetton balance change (up to {}s) - {}", timeoutSeconds, address.toRaw());
    BigInteger initialBalance = getJettonBalance(adnlLiteClient, jettonMinter, address);
    int i = 0;
    do {
      if (++i * 2 >= timeoutSeconds) {
        throw new Error(
            "Balance of " + address.toRaw() + " was not changed within specified timeout.");
      }
      Utils.sleep(2);
    } while (initialBalance.equals(getJettonBalance(adnlLiteClient, jettonMinter, address)));
  }

  public static BigInteger getJettonBalance(
      AdnlLiteClient adnlLiteClient, Address jettonMinter, Address destinationAddress) {

    try {
      JettonMinter jettonMinterWallet =
          JettonMinter.builder().adnlLiteClient(adnlLiteClient).customAddress(jettonMinter).build();

      JettonWallet jettonWallet = jettonMinterWallet.getJettonWallet(destinationAddress);
      System.out.println("adnl - jettonWallet " + jettonWallet.getAddress().toRaw());
      return jettonWallet.getBalance();
    } catch (Error e) {
      return new BigInteger("-1");
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public static void waitForJettonBalanceChange(
      TonCenter tonCenterClient, Address jettonMinter, Address address, int timeoutSeconds) {
    log.info("Waiting for jetton balance change (up to {}s) - {}", timeoutSeconds, address.toRaw());
    BigInteger initialBalance = getJettonBalance(tonCenterClient, jettonMinter, address);
    int i = 0;
    do {
      if (++i * 2 >= timeoutSeconds) {
        throw new Error(
            "Balance of " + address.toRaw() + " was not changed within specified timeout.");
      }
      Utils.sleep(2);
    } while (initialBalance.equals(getJettonBalance(tonCenterClient, jettonMinter, address)));
  }

  public static BigInteger getJettonBalance(
      TonCenter tonCenterClient, Address jettonMinter, Address destinationAddress) {

    try {
      JettonMinter jettonMinterWallet =
          JettonMinter.builder().tonCenterClient(tonCenterClient).customAddress(jettonMinter).build();

      JettonWallet jettonWallet = jettonMinterWallet.getJettonWallet(destinationAddress);
      System.out.println("toncenter - jettonWallet " + jettonWallet.getAddress().toRaw());
      return jettonWallet.getBalance();
    } catch (Error e) {
      return new BigInteger("-1");
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
