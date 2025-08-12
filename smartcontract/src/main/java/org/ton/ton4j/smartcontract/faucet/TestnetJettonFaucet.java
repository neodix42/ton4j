package org.ton.ton4j.smartcontract.faucet;

import com.iwebpp.crypto.TweetNaclFast;
import java.math.BigInteger;
import org.ton.ton4j.adnl.AdnlLiteClient;
import org.ton.ton4j.address.Address;
import org.ton.ton4j.smartcontract.token.ft.JettonMinter;
import org.ton.ton4j.smartcontract.token.ft.JettonWallet;
import org.ton.ton4j.smartcontract.types.WalletV3Config;
import org.ton.ton4j.smartcontract.utils.MsgUtils;
import org.ton.ton4j.smartcontract.wallet.ContractUtils;
import org.ton.ton4j.smartcontract.wallet.v3.WalletV3R2;
import org.ton.ton4j.toncenter.TonCenter;
import org.ton.ton4j.tonlib.Tonlib;
import org.ton.ton4j.tonlib.types.ExtMessageInfo;
import org.ton.ton4j.utils.Utils;

/** Faucet for NEOJ jettons. */
public class TestnetJettonFaucet {

  public static String ADMIN_WALLET_PUBLIC_KEY =
      "d1d4515b2635b81de98d58f65502f2c242bb0e63615520341b83a12dd4d0f516";
  static String ADMIN_WALLET_SECRET_KEY =
      "be0bbb1725807ec0df984702a32a143864418400d797a48e267a120c3dc5f8d0d1d4515b2635b81de98d58f65502f2c242bb0e63615520341b83a12dd4d0f516";
  public static String ADMIN_WALLET_ADDRESS =
      "0:98972d1ab4b86f6be34ad03d64bb5e2cb369f0d7b5e53f13348664672b893010";
  public static String ADMIN_WALLET_BOUNCEABLE_ADDRESS =
      "EQCYly0atLhva-NK0D1ku14ss2nw17XlPxM0hmRnK4kwEO86";
  public static String FAUCET_MASTER_ADDRESS = "kQAN6TAGauShFKDQvZCwNb_EeTUIjQDwRZ9t6GOn4FBzfg9Y";

  public static BigInteger topUpContractWithNeoj(
      Tonlib tonlib, Address destinationAddress, BigInteger jettonsAmount) {

    if (jettonsAmount.compareTo(Utils.toNano(100)) > 0) {
      throw new Error(
          "Too many NEOJ jettons requested from the TestnetJettonFaucet, maximum amount per request is 100.");
    }

    TweetNaclFast.Signature.KeyPair keyPair =
        TweetNaclFast.Signature.keyPair_fromSeed(Utils.hexToSignedBytes(ADMIN_WALLET_SECRET_KEY));

    WalletV3R2 adminWallet =
        WalletV3R2.builder().tonlib(tonlib).walletId(42).keyPair(keyPair).build();

    JettonMinter jettonMinterWallet =
        JettonMinter.builder()
            .tonlib(tonlib)
            .customAddress(Address.of(FAUCET_MASTER_ADDRESS))
            .build();

    System.out.println("tonlib - adminWallet " + adminWallet.getAddress().toRaw());
    JettonWallet adminJettonWallet = jettonMinterWallet.getJettonWallet(adminWallet.getAddress());
    System.out.println("tonlib - adminJettonWallet " + adminJettonWallet.getAddress().toRaw());

    WalletV3Config walletV3Config =
        WalletV3Config.builder()
            .walletId(42)
            .seqno(adminWallet.getSeqno())
            .destination(adminJettonWallet.getAddress())
            .amount(Utils.toNano(0.06))
            .body(
                JettonWallet.createTransferBody(
                    0,
                    jettonsAmount,
                    destinationAddress, // recipient
                    adminWallet.getAddress(), // response address
                    null, // custom payload
                    BigInteger.ONE, // forward amount
                    MsgUtils.createTextMessageBody(
                        "jetton top up from ton4j faucet") // forward payload
                    ))
            .build();
    ExtMessageInfo extMessageInfo = adminWallet.send(walletV3Config);

    if (extMessageInfo.getError().getCode() != 0) {
      throw new Error(extMessageInfo.getError().getMessage());
    }

    ContractUtils.waitForJettonBalanceChange(
        tonlib, Address.of(FAUCET_MASTER_ADDRESS), adminWallet.getAddress(), 60);
    Utils.sleep(10);
    return ContractUtils.getJettonBalance(
        tonlib, Address.of(FAUCET_MASTER_ADDRESS), destinationAddress);
  }

  public static BigInteger topUpContractWithNeoj(
      AdnlLiteClient adnlLiteClient, Address destinationAddress, BigInteger jettonsAmount) {

    if (jettonsAmount.compareTo(Utils.toNano(100)) > 0) {
      throw new Error(
          "Too many NEOJ jettons requested from the TestnetJettonFaucet, maximum amount per request is 100.");
    }

    TweetNaclFast.Signature.KeyPair keyPair =
        TweetNaclFast.Signature.keyPair_fromSeed(Utils.hexToSignedBytes(ADMIN_WALLET_SECRET_KEY));

    WalletV3R2 adminWallet =
        WalletV3R2.builder().adnlLiteClient(adnlLiteClient).walletId(42).keyPair(keyPair).build();

    JettonMinter jettonMinterWallet =
        JettonMinter.builder()
            .adnlLiteClient(adnlLiteClient)
            .customAddress(Address.of(FAUCET_MASTER_ADDRESS))
            .build();

    System.out.println("adnl - adminWallet " + adminWallet.getAddress().toRaw());
    JettonWallet adminJettonWallet = jettonMinterWallet.getJettonWallet(adminWallet.getAddress());
    System.out.println("adnl - adminJettonWallet " + adminJettonWallet.getAddress().toRaw());

    WalletV3Config walletV3Config =
        WalletV3Config.builder()
            .walletId(42)
            .seqno(adminWallet.getSeqno())
            .destination(adminJettonWallet.getAddress())
            .amount(Utils.toNano(0.06))
            .body(
                JettonWallet.createTransferBody(
                    0,
                    jettonsAmount,
                    destinationAddress, // recipient
                    adminWallet.getAddress(), // response address
                    null, // custom payload
                    BigInteger.ONE, // forward amount
                    MsgUtils.createTextMessageBody(
                        "jetton top up from ton4j faucet") // forward payload
                    ))
            .build();
    ExtMessageInfo extMessageInfo = adminWallet.send(walletV3Config);

    if (extMessageInfo.getError().getCode() != 0) {
      throw new Error(extMessageInfo.getError().getMessage());
    }

    ContractUtils.waitForJettonBalanceChange(
        adnlLiteClient, Address.of(FAUCET_MASTER_ADDRESS), adminWallet.getAddress(), 60);
    Utils.sleep(10);
    return ContractUtils.getJettonBalance(
        adnlLiteClient, Address.of(FAUCET_MASTER_ADDRESS), destinationAddress);
  }

  public static BigInteger topUpContractWithNeoj(
      TonCenter tonCenterClient,
      Address destinationAddress,
      BigInteger jettonsAmount,
      boolean avoidRateLimit) {

    if (jettonsAmount.compareTo(Utils.toNano(100)) > 0) {
      throw new Error(
          "Too many NEOJ jettons requested from the TestnetJettonFaucet, maximum amount per request is 100.");
    }

    TweetNaclFast.Signature.KeyPair keyPair =
        TweetNaclFast.Signature.keyPair_fromSeed(Utils.hexToSignedBytes(ADMIN_WALLET_SECRET_KEY));

    WalletV3R2 adminWallet =
        WalletV3R2.builder().tonCenterClient(tonCenterClient).walletId(42).keyPair(keyPair).build();

    JettonMinter jettonMinterWallet =
        JettonMinter.builder()
            .tonCenterClient(tonCenterClient)
            .customAddress(Address.of(FAUCET_MASTER_ADDRESS))
            .build();

    System.out.println("toncenter - adminWallet " + adminWallet.getAddress().toRaw());
    JettonWallet adminJettonWallet = jettonMinterWallet.getJettonWallet(adminWallet.getAddress());
    System.out.println("toncenter - adminJettonWallet " + adminJettonWallet.getAddress().toRaw());

    WalletV3Config walletV3Config =
        WalletV3Config.builder()
            .walletId(42)
            .seqno(adminWallet.getSeqno())
            .destination(adminJettonWallet.getAddress())
            .amount(Utils.toNano(0.06))
            .body(
                JettonWallet.createTransferBody(
                    0,
                    jettonsAmount,
                    destinationAddress, // recipient
                    adminWallet.getAddress(), // response address
                    null, // custom payload
                    BigInteger.ONE, // forward amount
                    MsgUtils.createTextMessageBody(
                        "jetton top up from ton4j faucet") // forward payload
                    ))
            .build();
    ExtMessageInfo extMessageInfo = adminWallet.send(walletV3Config);

    if (extMessageInfo.getError().getCode() != 0) {
      throw new Error(extMessageInfo.getTonCenterError().getMessage());
    }

    // Wait for jetton balance change
    try {
      BigInteger initialBalance =
          ContractUtils.getJettonBalance(
              tonCenterClient, Address.of(FAUCET_MASTER_ADDRESS), adminWallet.getAddress());
      int timeoutSeconds = 60;
      int i = 0;
      BigInteger currentBalance;
      do {
        if (++i * 2 >= timeoutSeconds) {
          throw new Error("Jetton balance was not changed within specified timeout.");
        }
        Utils.sleep(2);
        currentBalance =
            ContractUtils.getJettonBalance(
                tonCenterClient, Address.of(FAUCET_MASTER_ADDRESS), adminWallet.getAddress());
      } while (initialBalance.equals(currentBalance));
    } catch (Exception e) {
      throw new Error("Error waiting for jetton balance change: " + e.getMessage());
    }

    Utils.sleep(10);
    return ContractUtils.getJettonBalance(
        tonCenterClient, Address.of(FAUCET_MASTER_ADDRESS), destinationAddress);
  }
}
