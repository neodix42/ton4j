package org.ton.ton4j.smartcontract.integrationtests;

import com.iwebpp.crypto.TweetNaclFast;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.ton.ton4j.address.Address;
import org.ton.ton4j.adnl.AdnlLiteClient;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.mnemonic.Mnemonic;
import org.ton.ton4j.mnemonic.Pair;
import org.ton.ton4j.smartcontract.SendResponse;
import org.ton.ton4j.smartcontract.faucet.TestnetFaucet;
import org.ton.ton4j.smartcontract.highload.HighloadWalletV3S;
import org.ton.ton4j.smartcontract.types.HighloadQueryId;
import org.ton.ton4j.smartcontract.types.HighloadV3Config;
import org.ton.ton4j.smartcontract.wallet.v3.WalletV3R2;
import org.ton.ton4j.utils.Secp256k1KeyPair;
import org.ton.ton4j.utils.Utils;

import java.math.BigInteger;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@Slf4j
@RunWith(JUnit4.class)
public class TestDocuSamples {

  /**
   * Test used to show generation of mnemonic, its conversion to keypair and use of keypair with
   * ton4j wallets
   */
  @Test
  public void test_mnemonic_and_keypair() throws Exception {

    // generate random 24 words mnemonic without password protection
    String mnemonic = Mnemonic.generateString(24);
    log.info("mnemonic: {}", mnemonic);

    // use above mnemonic and generate keypair based on it
    Pair keyPair = Mnemonic.toKeyPair(mnemonic);
    log.info("public key[32 bytes]: {}", Utils.bytesToHex(keyPair.getPublicKey()));
    log.info("private key[32 bytes]: {}", Utils.bytesToHex(keyPair.getSecretKey()));

    // generate keypair for signatures, where private key is 64 bytes long (32 pubkey + 32 prvkey)
    TweetNaclFast.Signature.KeyPair keyPairSig =
        TweetNaclFast.Signature.keyPair_fromSeed(keyPair.getSecretKey());
    log.info("public key[32 bytes]: {}", Utils.bytesToHex(keyPairSig.getPublicKey()));
    log.info("private key[64 bytes]: {}", Utils.bytesToHex(keyPairSig.getSecretKey()));

    // if you don't need a mnemonic you can skip the above steps and get random signature keypair
    // using
    // Utils.generateSignatureKeyPair()
    TweetNaclFast.Signature.KeyPair keyPairQuick = Utils.generateSignatureKeyPair();
    log.info("public key[32 bytes]: {}", Utils.bytesToHex(keyPairQuick.getPublicKey()));
    log.info("private key[64 bytes]: {}", Utils.bytesToHex(keyPairQuick.getSecretKey()));
  }

  /** test used to show different wallet's addresses in testnet */
  @Test
  public void test_addresses_testnet() {

    TweetNaclFast.Signature.KeyPair keyPairSig = Utils.generateSignatureKeyPair();
    log.info("public key[32 bytes]: {}", Utils.bytesToHex(keyPairSig.getPublicKey()));
    log.info("private key[64 bytes]: {}", Utils.bytesToHex(keyPairSig.getSecretKey()));

    // easily use signature keypair in all ton4j wallets, in this case all payloads will be signed
    // automatically with a private key
    log.info("deploying walletA (V3R2) using ADNL client");
    WalletV3R2 wallet = WalletV3R2.builder().keyPair(keyPairSig).walletId(42).build();

    // once we assigned keypair to a wallet, we can get its address.
    log.info("wallet[rawAddress] {}", wallet.getAddress().toRaw());
    log.info("wallet[bounceableAddress] {}", wallet.getAddress().toBounceableTestnet());
    log.info("wallet[nonBounceableAddress] {}", wallet.getAddress().toNonBounceableTestnet());
  }

  /** test used to show different wallet's addresses in mainnet */
  @Test
  public void test_addresses_mainnet() {

    TweetNaclFast.Signature.KeyPair keyPairSig = Utils.generateSignatureKeyPair();
    log.info("public key[32 bytes]: {}", Utils.bytesToHex(keyPairSig.getPublicKey()));
    log.info("private key[64 bytes]: {}", Utils.bytesToHex(keyPairSig.getSecretKey()));

    // easily use signature keypair in all ton4j wallets, in this case all payloads will be signed
    // automatically with a private key
    log.info("deploying walletA (V3R2) using ADNL client");
    WalletV3R2 wallet = WalletV3R2.builder().keyPair(keyPairSig).walletId(42).build();

    // once we assigned keypair to a wallet, we can get its address.
    log.info("wallet[rawAddress] {}", wallet.getAddress().toRaw());
    log.info("wallet[bounceableAddress] {}", wallet.getAddress().toBounceable());
    log.info("wallet[nonBounceableAddress] {}", wallet.getAddress().toNonBounceable());
  }

  /**
   * test shows usage of keypair with WalletV3R2, ton4j faucet for quick top up of wallets and
   * deployment of a WalletV3R2.
   */
  @Test
  public void test_deploy_testnet() throws Exception {
    // initialize ADNL lite client for testnet
    AdnlLiteClient adnlLiteClient =
        AdnlLiteClient.builder().configUrl(Utils.getGlobalConfigUrlTestnetGithub()).build();

    TweetNaclFast.Signature.KeyPair keyPairSig = Utils.generateSignatureKeyPair();
    log.info("public key[32 bytes]: {}", Utils.bytesToHex(keyPairSig.getPublicKey()));
    log.info("private key[64 bytes]: {}", Utils.bytesToHex(keyPairSig.getSecretKey()));

    // easily use signature keypair in all ton4j wallets, in this case all payloads will be signed
    // automatically with a private key
    log.info("deploying wallet (V3R2) using ADNL client");
    WalletV3R2 wallet =
        WalletV3R2.builder()
            .adnlLiteClient(adnlLiteClient)
            .keyPair(keyPairSig)
            .walletId(42)
            .build();

    // once we assigned keypair to a wallet, we can get its address.
    log.info("wallet[rawAddress] {}", wallet.getAddress().toRaw());
    log.info("wallet[bounceableAddress] {}", wallet.getAddress().toBounceableTestnet());
    log.info("wallet[nonBounceableAddress] {}", wallet.getAddress().toNonBounceableTestnet());

    // before deploying any smart-contract in TON it must have funds on its address
    // in real life use other wallet to top up walletA using its nonBounceableAddress
    // in this example, we are using helper class TestnetFaucet and its topUpContract() method.

    BigInteger balanceWallet =
        TestnetFaucet.topUpContract(
            adnlLiteClient,
            Address.of(wallet.getAddress().toNonBounceableTestnet()),
            Utils.toNano(0.1));
    log.info("balanceWallet: {}", Utils.formatNanoValue(balanceWallet));

    // now we can deploy wallet
    wallet.deploy();
    wallet.waitForDeployment();
    log.info("wallet deployed");
  }

  /**
   * test shows usage of keypair with WalletV3R2, ton4j faucet for cannot be used in mainnet. Once
   * you got the wallet's address top it up externally and only then proceed with the wallet
   * deployment.
   */
  @Test
  public void test_deploy_mainet() throws Exception {
    // initialize ADNL lite client for testnet
    AdnlLiteClient adnlLiteClient =
        AdnlLiteClient.builder().configUrl(Utils.getGlobalConfigUrlTestnetGithub()).build();

    TweetNaclFast.Signature.KeyPair keyPairSig = Utils.generateSignatureKeyPair();
    log.info("public key[32 bytes]: {}", Utils.bytesToHex(keyPairSig.getPublicKey()));
    log.info("private key[64 bytes]: {}", Utils.bytesToHex(keyPairSig.getSecretKey()));

    // easily use signature keypair in all ton4j wallets, in this case all payloads will be signed
    // automatically with a private key
    log.info("deploying wallet (V3R2) using ADNL client");
    WalletV3R2 wallet =
        WalletV3R2.builder()
            .adnlLiteClient(adnlLiteClient)
            .keyPair(keyPairSig)
            .walletId(42)
            .build();

    // once we assigned keypair to a wallet, we can get its address.
    log.info("wallet[rawAddress] {}", wallet.getAddress().toRaw());
    log.info("wallet[bounceableAddress] {}", wallet.getAddress().toBounceableTestnet());
    log.info("wallet[nonBounceableAddress] {}", wallet.getAddress().toNonBounceableTestnet());

    // deploy wallet with an initial balance here
    Utils.sleep(10);
    log.info("balanceWallet: {}", Utils.formatNanoValue(wallet.getBalance()));

    // now we can deploy wallet
    wallet.deploy();
    wallet.waitForDeployment();
    log.info("wallet deployed");
  }

  /**
   * test shows usage of public key only with WalletV3R2, ton4j faucet for quick top up of wallets
   * and deployment of a WalletV3R2. The payload should be signed without exposing private key, e.g.
   * on HSM or other secure place.
   */
  @Test
  public void test_deploy_pubkey_only_testnet() throws Exception {
    // initialize ADNL lite client for testnet
    AdnlLiteClient adnlLiteClient =
        AdnlLiteClient.builder().configUrl(Utils.getGlobalConfigUrlTestnetGithub()).build();

    TweetNaclFast.Signature.KeyPair keyPairSig = Utils.generateSignatureKeyPair();
    log.info("public key[32 bytes]: {}", Utils.bytesToHex(keyPairSig.getPublicKey()));
    log.info("private key[64 bytes]: {}", Utils.bytesToHex(keyPairSig.getSecretKey()));

    // easily use signature keypair in all ton4j wallets, in this case all payloads will be signed
    // automatically with a private key
    log.info("deploying walletA (V3R2) using ADNL client");
    WalletV3R2 wallet =
        WalletV3R2.builder()
            .adnlLiteClient(adnlLiteClient)
            .publicKey(keyPairSig.getPublicKey()) // using pubkey only
            .walletId(42)
            .build();

    // once we assigned a pubkey to a wallet, we can get its address.
    log.info("wallet[rawAddress] {}", wallet.getAddress().toRaw());
    log.info("wallet[bounceableAddress] {}", wallet.getAddress().toBounceableTestnet());
    log.info("wallet[nonBounceableAddress] {}", wallet.getAddress().toNonBounceableTestnet());

    // before deploying any smart-contract in TON it must have funds on its address
    // in real life use other wallet to top up walletA using its nonBounceableAddress
    // in this example, we are using helper class TestnetFaucet and its topUpContract() method.

    BigInteger balanceWallet =
        TestnetFaucet.topUpContract(
            adnlLiteClient,
            Address.of(wallet.getAddress().toNonBounceableTestnet()),
            Utils.toNano(0.1));
    log.info("balanceWallet: {}", Utils.formatNanoValue(balanceWallet));

    // and sign the payload externally on HSM or in other secure place
    Cell deployBody = wallet.createDeployMessage();
    byte[] signature =
        Utils.signData(keyPairSig.getPublicKey(), keyPairSig.getSecretKey(), deployBody.hash());
    wallet.deploy(signature);
    wallet.waitForDeployment();
    log.info("wallet deployed");
  }

  /**
   * test show deployment of Highload Wallet V3S using Secp256k1 signature without exposing private
   * key
   */
  @Test
  public void test_deploy_highload_wallet_with_secp256k1_testnet() throws Exception {
    AdnlLiteClient adnlLiteClient =
        AdnlLiteClient.builder().configUrl(Utils.getGlobalConfigUrlTestnetGithub()).build();

    // ton4j also supports signatures of type secp256k1 in Highload Wallet V3S
    Secp256k1KeyPair keyPairSecp256k1 = Utils.generateSecp256k1SignatureKeyPair();

    HighloadWalletV3S wallet =
        HighloadWalletV3S.builder()
            .adnlLiteClient(adnlLiteClient)
            .publicKey(keyPairSecp256k1.getPublicKey()) // no private key is used
            .walletId(42)
            .build();

    log.info("wallet[rawAddress] {}", wallet.getAddress().toRaw());
    log.info("wallet[bounceableAddress] {}", wallet.getAddress().toBounceableTestnet());
    log.info("wallet[nonBounceableAddress] {}", wallet.getAddress().toNonBounceableTestnet());

    // top up wallet with initial balance
    BigInteger balanceWallet =
        TestnetFaucet.topUpContract(
            adnlLiteClient,
            Address.of(wallet.getAddress().toNonBounceableTestnet()),
            Utils.toNano(0.1));
    log.info("balanceWallet: {}", Utils.formatNanoValue(balanceWallet));

    HighloadV3Config config =
        HighloadV3Config.builder()
            .walletId(42)
            .queryId(HighloadQueryId.fromSeqno(0).getQueryId())
            .build();

    Cell deployBody = wallet.createDeployMessage(config);

    // sign deployBody elsewhere without exposing private key and come back with a signature
    byte[] signedDeployBody =
        Utils.signDataSecp256k1(
                deployBody.hash(),
                keyPairSecp256k1.getPrivateKey(),
                keyPairSecp256k1.getPublicKey())
            .getSignature();

    SendResponse sendResponse = wallet.deploy(config, signedDeployBody);
    assertThat(sendResponse.getCode()).isZero();

    wallet.waitForDeployment();
    log.info("wallet deployed");
  }
}
