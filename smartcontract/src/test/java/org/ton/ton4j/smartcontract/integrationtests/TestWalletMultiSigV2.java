package org.ton.ton4j.smartcontract.integrationtests;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigInteger;
import java.util.*;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.ton.ton4j.address.Address;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.smartcontract.SendMode;
import org.ton.ton4j.smartcontract.faucet.TestnetFaucet;
import org.ton.ton4j.smartcontract.multisig.MultiSigWalletV2;
import org.ton.ton4j.smartcontract.types.*;
import org.ton.ton4j.smartcontract.utils.MsgUtils;
import org.ton.ton4j.smartcontract.wallet.v3.WalletV3R2;
import org.ton.ton4j.tonlib.types.ExtMessageInfo;
import org.ton.ton4j.utils.Utils;

@Slf4j
@RunWith(JUnit4.class)
public class TestWalletMultiSigV2 extends CommonTest {

  Address dummyRecipient1 = Address.of(Utils.generateRandomAddress(0));
  Address dummyRecipient2 = Address.of(Utils.generateRandomAddress(0));

  /**
   *
   *
   * <pre>
   * - deploys admin wallet v3r2;
   * - from admin wallet sends internal msg to deploy multisig with specific params;
   * - from admin wallet sends internal msg with order that contains two actions (transfers) to two recipients;
   * - order-contract will be deployed;
   * - sending approval internal message from signer2 wallet to order-contract;
   * - order contract send execute msg back to multisig and multisig executed the order (two transfers)
   * - as a result two recipients should receive 0.025 and 0.026 tons.
   * </pre>
   */
  @Test
  public void testMultiSigV2DeploymentAnd2out3Approvals() throws InterruptedException {

    WalletV3R2 deployer =
        WalletV3R2.builder()
            .tonlib(tonlib)
            .keyPair(Utils.generateSignatureKeyPair())
            .walletId(42)
            .build();
    WalletV3R2 signer2 =
        WalletV3R2.builder()
            .tonlib(tonlib)
            .keyPair(Utils.generateSignatureKeyPair())
            .walletId(42)
            .build();
    WalletV3R2 signer3 =
        WalletV3R2.builder()
            .tonlib(tonlib)
            .keyPair(Utils.generateSignatureKeyPair())
            .walletId(42)
            .build();

    log.info("deployer {}", deployer.getAddress().toRaw());
    log.info("signer2 {}", signer2.getAddress().toRaw());
    log.info("signer3 {}", signer3.getAddress().toRaw());
    log.info("recipient1 {}", dummyRecipient1.toRaw());
    log.info("recipient2 {}", dummyRecipient2.toRaw());

    topUpAndDeploy(deployer);
    topUpAndDeploy(signer2);
    log.info("deployer seqno {}", deployer.getSeqno());
    log.info("signer2 seqno {}", signer2.getSeqno());
    //    topUpAndDeploy(signer3);

    MultiSigWalletV2 multiSigWalletV2 =
        MultiSigWalletV2.builder()
            .tonlib(tonlib)
            .config(
                MultiSigV2Config.builder()
                    .allowArbitraryOrderSeqno(false)
                    .nextOrderSeqno(BigInteger.ZERO)
                    .threshold(2)
                    .numberOfSigners(3)
                    .signers(
                        Arrays.asList(
                            deployer.getAddress(), signer2.getAddress(), signer3.getAddress()))
                    .proposers(Collections.emptyList())
                    .build())
            .build();

    log.info("multisig address {}", multiSigWalletV2.getAddress().toBounceable());

    // deploy multisig from admin wallet on testnet
    WalletV3Config config =
        WalletV3Config.builder()
            .walletId(42)
            .seqno(1)
            .destination(multiSigWalletV2.getAddress())
            .amount(Utils.toNano(0.2))
            .stateInit(multiSigWalletV2.getStateInit())
            .sendMode(SendMode.PAY_GAS_SEPARATELY_AND_IGNORE_ERRORS)
            .build();
    deployer.send(config);
    deployer.waitForDeployment(30);
    Utils.sleep(10, "pause");

    // send external msg to admin wallet that sends internal msg to multisig with body to create
    // order-contract

    Cell orderBody =
        MultiSigWalletV2.createOrder(
            Arrays.asList(
                MultiSigWalletV2.createSendMessageAction(
                    1,
                    MsgUtils.createInternalMessageRelaxed(
                            dummyRecipient1, Utils.toNano(0.025), null, null, null, false)
                        .toCell()),
                MultiSigWalletV2.createSendMessageAction(
                    1,
                    MsgUtils.createInternalMessageRelaxed(
                            dummyRecipient2, Utils.toNano(0.026), null, null, null, false)
                        .toCell())));
    config =
        WalletV3Config.builder()
            .walletId(42)
            .seqno(2)
            .destination(multiSigWalletV2.getAddress())
            .amount(Utils.toNano(0.1))
            .body(
                MultiSigWalletV2.newOrder(
                    0, BigInteger.ZERO, true, 0, Utils.now() + 3600, orderBody))
            .build();

    deployer.send(config);
    deployer.waitForBalanceChange();

    Utils.sleep(15);

    Address orderAddress = multiSigWalletV2.getOrderAddress(BigInteger.ZERO);
    log.info("orderAddress {}", orderAddress);

    log.info(
        "orderData when once approved {}", multiSigWalletV2.getOrderData(BigInteger.valueOf(0)));

    log.info(
        "getOrderEstimate {}", multiSigWalletV2.getOrderEstimate(orderBody, Utils.now() + 3600));

    log.info("sending approve from signer2 to order address");
    config =
        WalletV3Config.builder()
            .walletId(42)
            .seqno(1)
            .destination(orderAddress)
            .amount(Utils.toNano(0.05))
            .body(MultiSigWalletV2.approve(0, 1))
            .build();
    signer2.send(config);
    signer2.waitForBalanceChange();

    Utils.sleep(20);

    log.info(
        "orderData when twice approved {}", multiSigWalletV2.getOrderData(BigInteger.valueOf(0)));

    BigInteger balanceRecipient1 = tonlib.getAccountBalance(dummyRecipient1);
    BigInteger balanceRecipient2 = tonlib.getAccountBalance(dummyRecipient2);

    assertThat(balanceRecipient1).isGreaterThan(BigInteger.ZERO);
    assertThat(balanceRecipient2).isGreaterThan(BigInteger.ZERO);
  }

  /** same as above, but requires 3 out 3 approvals */
  @Test
  public void testMultiSigV2DeploymentAnd3out3Approvals() throws InterruptedException {

    WalletV3R2 deployer =
        WalletV3R2.builder()
            .tonlib(tonlib)
            .keyPair(Utils.generateSignatureKeyPair())
            .walletId(42)
            .build();
    WalletV3R2 signer2 =
        WalletV3R2.builder()
            .tonlib(tonlib)
            .keyPair(Utils.generateSignatureKeyPair())
            .walletId(42)
            .build();
    WalletV3R2 signer3 =
        WalletV3R2.builder()
            .tonlib(tonlib)
            .keyPair(Utils.generateSignatureKeyPair())
            .walletId(42)
            .build();

    log.info("deployer {}", deployer.getAddress().toRaw());
    log.info("signer2 {}", signer2.getAddress().toRaw());
    log.info("signer3 {}", signer3.getAddress().toRaw());
    log.info("recipient1 {}", dummyRecipient1.toRaw());
    log.info("recipient2 {}", dummyRecipient2.toRaw());

    topUpAndDeploy(deployer);
    topUpAndDeploy(signer2);
    topUpAndDeploy(signer3);
    log.info("deployer seqno {}", deployer.getSeqno());
    log.info("signer2 seqno {}", signer2.getSeqno());
    log.info("signer3 seqno {}", signer2.getSeqno());

    MultiSigWalletV2 multiSigWalletV2 =
        MultiSigWalletV2.builder()
            .tonlib(tonlib)
            .config(
                MultiSigV2Config.builder()
                    .allowArbitraryOrderSeqno(false)
                    .nextOrderSeqno(BigInteger.ZERO)
                    .threshold(3)
                    .numberOfSigners(3)
                    .signers(
                        Arrays.asList(
                            deployer.getAddress(), signer2.getAddress(), signer3.getAddress()))
                    .proposers(Collections.emptyList())
                    .build())
            .build();

    log.info("multisig address {}", multiSigWalletV2.getAddress().toBounceable());

    // deploy multisig from admin wallet on testnet
    WalletV3Config config =
        WalletV3Config.builder()
            .walletId(42)
            .seqno(1)
            .destination(multiSigWalletV2.getAddress())
            .amount(Utils.toNano(0.2))
            .stateInit(multiSigWalletV2.getStateInit())
            .sendMode(SendMode.PAY_GAS_SEPARATELY_AND_IGNORE_ERRORS)
            .build();
    deployer.send(config);
    deployer.waitForDeployment();
    Utils.sleep(10);

    // send external msg to admin wallet that sends internal msg to multisig with body to create
    // order-contract
    config =
        WalletV3Config.builder()
            .walletId(42)
            .seqno(2)
            .destination(multiSigWalletV2.getAddress())
            .amount(Utils.toNano(0.1))
            .body(
                MultiSigWalletV2.newOrder(
                    0,
                    BigInteger.ZERO,
                    true,
                    0,
                    Utils.now() + 3600,
                    MultiSigWalletV2.createOrder(
                        Arrays.asList(
                            MultiSigWalletV2.createSendMessageAction(
                                1,
                                MsgUtils.createInternalMessageRelaxed(
                                        dummyRecipient1,
                                        Utils.toNano(0.025),
                                        null,
                                        null,
                                        null,
                                        false)
                                    .toCell()),
                            MultiSigWalletV2.createSendMessageAction(
                                1,
                                MsgUtils.createInternalMessageRelaxed(
                                        dummyRecipient2,
                                        Utils.toNano(0.026),
                                        null,
                                        null,
                                        null,
                                        false)
                                    .toCell())))))
            .build();

    deployer.send(config);
    deployer.waitForBalanceChange();

    Utils.sleep(15);

    Address orderAddress = multiSigWalletV2.getOrderAddress(BigInteger.ZERO);
    log.info("orderAddress {}", orderAddress);

    log.info("sending approve from signer2 to order address");
    config =
        WalletV3Config.builder()
            .walletId(42)
            .seqno(1)
            .destination(orderAddress)
            .amount(Utils.toNano(0.05))
            .body(MultiSigWalletV2.approve(0, 1))
            .build();
    signer2.send(config);
    signer2.waitForBalanceChange();

    Utils.sleep(45);

    BigInteger balanceRecipient1 = tonlib.getAccountBalance(dummyRecipient1);
    BigInteger balanceRecipient2 = tonlib.getAccountBalance(dummyRecipient2);

    // should not be executed since requires 3 out of 3
    assertThat(balanceRecipient1).isEqualTo(BigInteger.ZERO);
    assertThat(balanceRecipient2).isEqualTo(BigInteger.ZERO);

    log.info("sending approve from signer3 to order address");
    config =
        WalletV3Config.builder()
            .walletId(42)
            .seqno(1)
            .destination(orderAddress)
            .amount(Utils.toNano(0.05))
            .body(MultiSigWalletV2.approve(0, 2))
            .build();
    signer3.send(config);
    signer3.waitForBalanceChange();

    Utils.sleep(60);

    balanceRecipient1 = tonlib.getAccountBalance(dummyRecipient1);
    balanceRecipient2 = tonlib.getAccountBalance(dummyRecipient2);

    assertThat(balanceRecipient1).isGreaterThan(BigInteger.ZERO);
    assertThat(balanceRecipient2).isGreaterThan(BigInteger.ZERO);
  }

  /**
   *
   *
   * <pre>
   * - deploys admin wallet v3r2;
   * - from admin wallet sends internal msg to deploy multisig with specific params;
   * - from proposer wallet sends internal msg with order that contains two actions (transfers) to one recipient;
   * - order-contract will be deployed;
   * - sending approval internal message from signer2 wallet to order-contract;
   * - order contract send execute msg back to multisig and multisig executed the order (two transfers)
   * - as a result two recipients should receive 0.025 and 0.026 tons.
   * </pre>
   */
  @Test
  public void testMultiSigV2ProposerProposesOrder() throws InterruptedException {

    WalletV3R2 deployer =
        WalletV3R2.builder()
            .tonlib(tonlib)
            .keyPair(Utils.generateSignatureKeyPair())
            .walletId(42)
            .build();
    WalletV3R2 signer2 =
        WalletV3R2.builder()
            .tonlib(tonlib)
            .keyPair(Utils.generateSignatureKeyPair())
            .walletId(42)
            .build();
    WalletV3R2 signer3 =
        WalletV3R2.builder()
            .tonlib(tonlib)
            .keyPair(Utils.generateSignatureKeyPair())
            .walletId(42)
            .build();

    WalletV3R2 proposer =
        WalletV3R2.builder()
            .tonlib(tonlib)
            .keyPair(Utils.generateSignatureKeyPair())
            .walletId(42)
            .build();

    log.info("deployer {}", deployer.getAddress().toRaw());
    log.info("signer2 {}", signer2.getAddress().toRaw());
    log.info("signer3 {}", signer2.getAddress().toRaw());
    log.info("proposer {}", signer3.getAddress().toRaw());
    log.info("recipient1 {}", dummyRecipient1.toRaw());

    topUpAndDeploy(deployer);
    topUpAndDeploy(signer2);
    topUpAndDeploy(signer3);
    topUpAndDeploy(proposer);

    log.info("deployer seqno {}", deployer.getSeqno());
    log.info("signer2 seqno {}", signer2.getSeqno());
    log.info("signer4 seqno {}", signer3.getSeqno());
    log.info("proposer seqno {}", proposer.getSeqno());

    MultiSigWalletV2 multiSigWalletV2 =
        MultiSigWalletV2.builder()
            .tonlib(tonlib)
            .config(
                MultiSigV2Config.builder()
                    .allowArbitraryOrderSeqno(false)
                    .nextOrderSeqno(BigInteger.ZERO)
                    .threshold(2)
                    .numberOfSigners(3)
                    .signers(
                        Arrays.asList(
                            deployer.getAddress(), signer2.getAddress(), signer3.getAddress()))
                    .proposers(Collections.singletonList(proposer.getAddress()))
                    .build())
            .build();

    log.info("multisig address {}", multiSigWalletV2.getAddress().toBounceable());

    // deploy multisig from admin wallet on testnet
    WalletV3Config config =
        WalletV3Config.builder()
            .walletId(42)
            .seqno(1)
            .destination(multiSigWalletV2.getAddress())
            .amount(Utils.toNano(0.2))
            .stateInit(multiSigWalletV2.getStateInit())
            .sendMode(SendMode.PAY_GAS_SEPARATELY_AND_IGNORE_ERRORS)
            .build();
    deployer.send(config);
    deployer.waitForDeployment(30);
    Utils.sleep(15);

    // send external msg to PROPOSER wallet that sends internal msg to multisig with body to create
    // order-contract

    Cell orderBody =
        MultiSigWalletV2.createOrder(
            Collections.singletonList(
                MultiSigWalletV2.createSendMessageAction(
                    1,
                    MsgUtils.createInternalMessageRelaxed(
                            dummyRecipient1, Utils.toNano(0.025), null, null, null, false)
                        .toCell())));
    config =
        WalletV3Config.builder()
            .walletId(42)
            .seqno(1)
            .destination(multiSigWalletV2.getAddress())
            .amount(Utils.toNano(0.1))
            .body(
                MultiSigWalletV2.newOrder(
                    0, BigInteger.ZERO, false, 0, Utils.now() + 3600, orderBody))
            .build();

    proposer.send(config);
    proposer.waitForBalanceChange();

    Utils.sleep(15);

    Address orderAddress = multiSigWalletV2.getOrderAddress(BigInteger.ZERO);
    log.info("orderAddress {}", orderAddress);

    log.info(
        "orderData when not approved {}", multiSigWalletV2.getOrderData(BigInteger.valueOf(0)));

    log.info(
        "getOrderEstimate {}", multiSigWalletV2.getOrderEstimate(orderBody, Utils.now() + 3600));

    log.info("sending approve from signer2 to order address");
    config =
        WalletV3Config.builder()
            .walletId(42)
            .seqno(1)
            .destination(orderAddress)
            .amount(Utils.toNano(0.05))
            .body(MultiSigWalletV2.approve(0, 1))
            .build();
    signer2.send(config);
    signer2.waitForBalanceChange();

    Utils.sleep(20);

    BigInteger balanceRecipient1 = tonlib.getAccountBalance(dummyRecipient1);
    BigInteger balanceRecipient2 = tonlib.getAccountBalance(dummyRecipient2);

    assertThat(balanceRecipient1).isEqualTo(BigInteger.ZERO);
    assertThat(balanceRecipient2).isEqualTo(BigInteger.ZERO);

    log.info(
        "orderData when once approved {}", multiSigWalletV2.getOrderData(BigInteger.valueOf(0)));

    log.info(
        "getOrderEstimate {}", multiSigWalletV2.getOrderEstimate(orderBody, Utils.now() + 3600));

    log.info("sending approve from signer3 to order address");
    config =
        WalletV3Config.builder()
            .walletId(42)
            .seqno(1)
            .destination(orderAddress)
            .amount(Utils.toNano(0.05))
            .body(MultiSigWalletV2.approve(0, 2))
            .build();
    signer3.send(config);
    signer3.waitForBalanceChange();

    Utils.sleep(60);

    log.info(
        "orderData when twice approved {}", multiSigWalletV2.getOrderData(BigInteger.valueOf(0)));

    balanceRecipient1 = tonlib.getAccountBalance(dummyRecipient1);

    assertThat(balanceRecipient1).isGreaterThan(BigInteger.ZERO);
  }

  /**
   *
   *
   * <pre>
   * - deploys admin wallet v3r2;
   * - from admin wallet sends internal msg to deploy multisig with specific params;
   * - from admin wallet sends internal msg to multisig to change its parameters (new threshold=4, 4 signers and 2 proposers)
   * </pre>
   */
  @Test
  public void testMultiSigV2UpdateParams() throws InterruptedException {

    WalletV3R2 deployer =
        WalletV3R2.builder()
            .tonlib(tonlib)
            .keyPair(Utils.generateSignatureKeyPair())
            .walletId(42)
            .build();
    WalletV3R2 signer2 =
        WalletV3R2.builder()
            .tonlib(tonlib)
            .keyPair(Utils.generateSignatureKeyPair())
            .walletId(42)
            .build();
    WalletV3R2 signer3 =
        WalletV3R2.builder()
            .tonlib(tonlib)
            .keyPair(Utils.generateSignatureKeyPair())
            .walletId(42)
            .build();
    WalletV3R2 signer4 =
        WalletV3R2.builder()
            .tonlib(tonlib)
            .keyPair(Utils.generateSignatureKeyPair())
            .walletId(42)
            .build();

    WalletV3R2 proposer1 =
        WalletV3R2.builder()
            .tonlib(tonlib)
            .keyPair(Utils.generateSignatureKeyPair())
            .walletId(42)
            .build();

    WalletV3R2 proposer2 =
        WalletV3R2.builder()
            .tonlib(tonlib)
            .keyPair(Utils.generateSignatureKeyPair())
            .walletId(42)
            .build();

    log.info("deployer {}", deployer.getAddress().toRaw());
    log.info("signer2 {}", signer2.getAddress().toRaw());
    log.info("signer3 {}", signer3.getAddress().toRaw());
    log.info("signer4 {}", signer4.getAddress().toRaw());
    log.info("proposer1 {}", proposer1.getAddress().toRaw());
    log.info("recipient1 {}", dummyRecipient1.toRaw());
    log.info("recipient2 {}", dummyRecipient2.toRaw());

    topUpAndDeploy(deployer);
    topUpAndDeploy(signer2);
    log.info("deployer seqno {}", deployer.getSeqno());
    log.info("signer2 seqno {}", signer2.getSeqno());

    MultiSigWalletV2 multiSigWalletV2 =
        MultiSigWalletV2.builder()
            .tonlib(tonlib)
            .config(
                MultiSigV2Config.builder()
                    .allowArbitraryOrderSeqno(false)
                    .nextOrderSeqno(BigInteger.ZERO)
                    .threshold(2)
                    .numberOfSigners(3)
                    .signers(
                        Arrays.asList(
                            deployer.getAddress(), signer2.getAddress(), signer3.getAddress()))
                    .proposers(Collections.singletonList(proposer1.getAddress()))
                    .build())
            .build();

    log.info("multisig address {}", multiSigWalletV2.getAddress().toBounceable());

    // deploy multisig from admin wallet on testnet
    WalletV3Config config =
        WalletV3Config.builder()
            .walletId(42)
            .seqno(1)
            .destination(multiSigWalletV2.getAddress())
            .amount(Utils.toNano(0.2))
            .stateInit(multiSigWalletV2.getStateInit())
            .sendMode(SendMode.PAY_GAS_SEPARATELY_AND_IGNORE_ERRORS)
            .build();
    deployer.send(config);
    deployer.waitForDeployment(30);
    Utils.sleep(15);

    MultiSigV2Data data = multiSigWalletV2.getMultiSigData();
    log.info("multiSig data before {}", data);

    // send external msg to admin wallet that sends internal msg to multisig to change its
    // parameters (new threshold=4, new 4 signers and one proposer)

    Cell orderBody =
        MultiSigWalletV2.createOrder(
            Collections.singletonList(
                MultiSigWalletV2.updateMultiSigParam(
                    4,
                    Arrays.asList(
                        deployer.getAddress(),
                        signer2.getAddress(),
                        signer3.getAddress(),
                        signer4.getAddress()),
                    Arrays.asList(proposer1.getAddress(), proposer2.getAddress()))));
    config =
        WalletV3Config.builder()
            .walletId(42)
            .seqno(2)
            .destination(multiSigWalletV2.getAddress())
            .amount(Utils.toNano(0.1))
            .body(
                MultiSigWalletV2.newOrder(
                    0, BigInteger.ZERO, true, 0, Utils.now() + 3600, orderBody))
            .build();

    deployer.send(config);
    deployer.waitForBalanceChange();

    Utils.sleep(15);

    Address orderAddress = multiSigWalletV2.getOrderAddress(BigInteger.ZERO);
    log.info("orderAddress {}", orderAddress);

    log.info("sending approve from signer2 to order address");
    config =
        WalletV3Config.builder()
            .walletId(42)
            .seqno(1)
            .destination(orderAddress)
            .amount(Utils.toNano(0.05))
            .body(MultiSigWalletV2.approve(0, 1))
            .build();
    signer2.send(config);
    signer2.waitForBalanceChange();

    Utils.sleep(15);

    data = multiSigWalletV2.getMultiSigData();
    log.info("multiSig data after {}", data);
    assertThat(data.getThreshold()).isEqualTo(4);
    assertThat(data.getSigners().size()).isEqualTo(4);
    assertThat(data.getProposers().size()).isEqualTo(2);

    log.info("orderData {}", multiSigWalletV2.getOrderData(BigInteger.valueOf(0)));

    log.info(
        "getOrderEstimate for 10 years {}",
        multiSigWalletV2.getOrderEstimate(orderBody, Utils.now() + 60 * 60 * 24 * 365 * 10));
  }

  private void topUpAndDeploy(WalletV3R2 wallet) throws InterruptedException {

    String nonBounceableAddress = wallet.getAddress().toNonBounceable();

    // top up new wallet using test-faucet-wallet
    BigInteger balance =
        TestnetFaucet.topUpContract(tonlib, Address.of(nonBounceableAddress), Utils.toNano(1));
    log.info(
        "new wallet (id={} {}) balance: {}",
        wallet.getWalletId(),
        wallet.getName(),
        Utils.formatNanoValue(balance));

    ExtMessageInfo extMessageInfo = wallet.deploy();
    assertThat(extMessageInfo.getError().getCode()).isZero();

    wallet.waitForDeployment();
    log.info("deployed {} {}", wallet.getWalletId(), wallet.getName());
  }
}
