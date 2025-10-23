package org.ton.ton4j.smartcontract;

import static org.assertj.core.api.Assertions.assertThat;

import com.iwebpp.crypto.TweetNaclFast;
import java.math.BigInteger;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.ton.ton4j.address.Address;
import org.ton.ton4j.smartcontract.faucet.TestnetFaucet;
import org.ton.ton4j.smartcontract.faucet.TestnetJettonFaucet;
import org.ton.ton4j.smartcontract.token.ft.JettonMinter;
import org.ton.ton4j.smartcontract.token.ft.JettonWallet;
import org.ton.ton4j.smartcontract.token.nft.NftUtils;
import org.ton.ton4j.smartcontract.types.WalletV3Config;
import org.ton.ton4j.smartcontract.utils.MsgUtils;
import org.ton.ton4j.smartcontract.wallet.ContractUtils;
import org.ton.ton4j.smartcontract.wallet.v3.WalletV3R2;
import org.ton.ton4j.tonlib.Tonlib;
import org.ton.ton4j.tonlib.types.RawAccountState;
import org.ton.ton4j.utils.Utils;

/** Faucet for NEOJ jettons. */
@Slf4j
@RunWith(JUnit4.class)
public class TestJettonFaucet {

  static String tonlibPath = Utils.getTonlibGithubUrl();

  static String ADMIN_WALLET_PUBLIC_KEY =
      "d1d4515b2635b81de98d58f65502f2c242bb0e63615520341b83a12dd4d0f516";
  static String ADMIN_WALLET_SECRET_KEY =
      "be0bbb1725807ec0df984702a32a143864418400d797a48e267a120c3dc5f8d0d1d4515b2635b81de98d58f65502f2c242bb0e63615520341b83a12dd4d0f516";
  public static String ADMIN_WALLET_ADDRESS =
      "0:98972d1ab4b86f6be34ad03d64bb5e2cb369f0d7b5e53f13348664672b893010";
  public static String ADMIN_WALLET_BOUNCEABLE_ADDRESS =
      "EQCYly0atLhva-NK0D1ku14ss2nw17XlPxM0hmRnK4kwEO86";
  public static String FAUCET_MASTER_ADDRESS = "kQAN6TAGauShFKDQvZCwNb_EeTUIjQDwRZ9t6GOn4FBzfg9Y";

  @Test
  public void testJettonFaucetBalance() {
    Tonlib tonlib =
        Tonlib.builder().testnet(true).pathToTonlibSharedLib(tonlibPath).ignoreCache(false).build();

    TweetNaclFast.Signature.KeyPair keyPair =
        TweetNaclFast.Signature.keyPair_fromSeed(Utils.hexToSignedBytes(ADMIN_WALLET_SECRET_KEY));

    WalletV3R2 adminWallet =
        WalletV3R2.builder().tonlib(tonlib).walletId(42).keyPair(keyPair).build();

    JettonMinter jettonMinterWallet =
        JettonMinter.builder()
            .tonlib(tonlib)
            .customAddress(Address.of(FAUCET_MASTER_ADDRESS))
            .build();

    JettonWallet adminJettonWallet = jettonMinterWallet.getJettonWallet(adminWallet.getAddress());

    RawAccountState state = tonlib.getRawAccountState(Address.of(FAUCET_MASTER_ADDRESS));

    log.info("TEST FAUCET BALANCE IN TONCOINS {}", Utils.formatNanoValue(state.getBalance(), 2));
    log.info(
        "TEST FAUCET BALANCE TOTAL SUPPLY: {}",
        Utils.formatJettonValue(jettonMinterWallet.getTotalSupply(), 2, 2));
    log.info(
        "TEST FAUCET ADMIN BALANCE in TONCOINS: {}",
        Utils.formatNanoValue(adminWallet.getBalance()));
    log.info(
        "TEST FAUCET ADMIN BALANCE in JETTONS: {}",
        Utils.formatJettonValue(adminJettonWallet.getBalance(), 2, 2));
  }

  @Test
  public void createJettonFaucetAdminWallet() {
    WalletV3R2 contract = WalletV3R2.builder().walletId(42).build();

    assertThat(contract.getAddress()).isNotNull();
    log.info("Private key {}", Utils.bytesToHex(contract.getKeyPair().getSecretKey()));
    log.info("Public key {}", Utils.bytesToHex(contract.getKeyPair().getPublicKey()));
    log.info("Non-bounceable address (for init): {}", contract.getAddress().toNonBounceable());
    log.info("Bounceable address (for later access): {}", contract.getAddress().toBounceable());
    log.info("Raw address: {}", contract.getAddress().toRaw());
  }

  @Test
  public void deployJettonFaucetAdminWallet() throws InterruptedException {
    byte[] secretKey = Utils.hexToSignedBytes(ADMIN_WALLET_SECRET_KEY);
    TweetNaclFast.Signature.KeyPair keyPair = TweetNaclFast.Signature.keyPair_fromSeed(secretKey);

    Tonlib tonlib =
        Tonlib.builder().testnet(true).pathToTonlibSharedLib(tonlibPath).ignoreCache(false).build();

    WalletV3R2 adminWallet =
        WalletV3R2.builder().tonlib(tonlib).walletId(42).keyPair(keyPair).build();

    log.info("Private key {}", Utils.bytesToHex(keyPair.getSecretKey()));
    log.info("Public key {}", Utils.bytesToHex(keyPair.getPublicKey()));
    log.info("Non-bounceable address (for init): {}", adminWallet.getAddress().toNonBounceable());
    log.info("Bounceable address (for later access): {}", adminWallet.getAddress().toBounceable());
    log.info("Raw address: {}", adminWallet.getAddress().toRaw());

    BigInteger balance =
        TestnetFaucet.topUpContract(
            tonlib, Address.of(adminWallet.getAddress().toNonBounceable()), Utils.toNano(10));
    Utils.sleep(30, "topping up...");

    SendResponse sendResponse = adminWallet.deploy();
    assertThat(sendResponse.getCode()).isZero();
  }

  @Test
  public void deployJettonFaucetMinter() {

    Tonlib tonlib =
        Tonlib.builder().testnet(true).pathToTonlibSharedLib(tonlibPath).ignoreCache(false).build();

    byte[] secretKey = Utils.hexToSignedBytes(ADMIN_WALLET_SECRET_KEY);
    TweetNaclFast.Signature.KeyPair keyPair = TweetNaclFast.Signature.keyPair_fromSeed(secretKey);

    WalletV3R2 adminWallet =
        WalletV3R2.builder().tonlib(tonlib).keyPair(keyPair).walletId(42).build();
    log.info("adminWallet {}", adminWallet.getAddress().toBounceable());

    JettonMinter minter =
        JettonMinter.builder()
            .tonlib(tonlib)
            .adminAddress(adminWallet.getAddress())
            .content(
                NftUtils.createOffChainUriCell(
                    "https://raw.githubusercontent.com/neodix42/ton4j/main/1-media/neo-jetton.json"))
            .build();

    log.info("jetton minter address {}", minter.getAddress());

    WalletV3Config walletV3Config =
        WalletV3Config.builder()
            .walletId(42)
            .seqno(adminWallet.getSeqno())
            .destination(minter.getAddress())
            .amount(Utils.toNano(2))
            .stateInit(minter.getStateInit())
            .comment("deploy minter")
            .build();

    SendResponse sendResponse = adminWallet.send(walletV3Config);
    assertThat(sendResponse.getCode()).isZero();
  }

  @Test
  public void mintTestJettonsFaucet() {
    Tonlib tonlib =
        Tonlib.builder().testnet(true).pathToTonlibSharedLib(tonlibPath).ignoreCache(false).build();

    byte[] secretKey = Utils.hexToSignedBytes(ADMIN_WALLET_SECRET_KEY);
    TweetNaclFast.Signature.KeyPair keyPair = TweetNaclFast.Signature.keyPair_fromSeed(secretKey);

    WalletV3R2 adminWallet =
        WalletV3R2.builder().tonlib(tonlib).keyPair(keyPair).walletId(42).build();

    WalletV3Config walletV3Config =
        WalletV3Config.builder()
            .walletId(42)
            .seqno(adminWallet.getSeqno())
            .destination(Address.of(FAUCET_MASTER_ADDRESS))
            .amount(Utils.toNano(0.1))
            .body(
                JettonMinter.createMintBody(
                    0,
                    adminWallet.getAddress(),
                    Utils.toNano(0.1),
                    new BigInteger("10_000_000_000_000_000"),
                    null,
                    null,
                    BigInteger.ONE,
                    MsgUtils.createTextMessageBody("minting")))
            .build();

    SendResponse sendResponse = adminWallet.send(walletV3Config);
    assertThat(sendResponse.getCode()).isZero();
  }

  @Test
  public void topUpAnyContractWithNeoJettons() {
    Tonlib tonlib =
        Tonlib.builder().testnet(true).pathToTonlibSharedLib(tonlibPath).ignoreCache(false).build();
    BigInteger newBalance =
        TestnetJettonFaucet.topUpContractWithNeoj(
            tonlib,
            Address.of("0QCUqgn-Ix3kuzhPAkKKiqqXQazsJ98K3VSCi4QJ3ZTGC7O1"),
            BigInteger.valueOf(100));
    log.info("new balance " + Utils.formatNanoValue(newBalance));
  }

  @Test
  public void testJettonBalance() {

    Tonlib tonlib =
        Tonlib.builder().testnet(true).pathToTonlibSharedLib(tonlibPath).ignoreCache(false).build();

    log.info(
        "balance: {}",
        ContractUtils.getJettonBalance(
            tonlib,
            Address.of(FAUCET_MASTER_ADDRESS),
            Address.of("0:cf2d917d55ed2d9fde43b5a5b5512216c3a027661311bbd771c394892836b3a4")));
  }
}
