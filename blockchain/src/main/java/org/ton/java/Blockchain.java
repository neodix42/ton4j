package org.ton.java;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

import com.iwebpp.crypto.TweetNaclFast;
import java.math.BigInteger;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.ton.java.address.Address;
import org.ton.java.emulator.tvm.TvmEmulator;
import org.ton.java.emulator.tvm.TvmVerbosityLevel;
import org.ton.java.emulator.tx.TxEmulator;
import org.ton.java.emulator.tx.TxEmulatorConfig;
import org.ton.java.emulator.tx.TxVerbosityLevel;
import org.ton.java.fift.FiftRunner;
import org.ton.java.func.FuncRunner;
import org.ton.java.smartcontract.SmartContractCompiler;
import org.ton.java.smartcontract.faucet.TestnetFaucet;
import org.ton.java.smartcontract.types.WalletV3Config;
import org.ton.java.smartcontract.wallet.Contract;
import org.ton.java.smartcontract.wallet.v3.WalletV3R2;
import org.ton.java.tlb.types.Message;
import org.ton.java.tonlib.Tonlib;
import org.ton.java.tonlib.types.*;
import org.ton.java.utils.Utils;

@Slf4j
@Builder
public class Blockchain {

  private FuncRunner funcRunner;
  private FiftRunner fiftRunner;
  private Tonlib tonlib;
  private Network network;
  VerbosityLevel tonlibVerbosityLevel;
  TxEmulatorConfig txEmulatorConfig;
  TxVerbosityLevel txVerbosityLevel;
  TvmVerbosityLevel tvmVerbosityLevel;
  Contract standardContract;
  String myLocalTonInstallationPath;
  String customContractPath;
  String customGlobalConfigPath;
  String customContractAsResource;
  String customEmulatorPath;
  private static SmartContractCompiler smartContractCompiler;
  private static TxEmulator txEmulator;
  private static TvmEmulator tvmEmulator;

  /** default 0.1 toncoin */
  BigInteger initialDeployTopUpAmount;

  public static class BlockchainBuilder {}

  public static BlockchainBuilder builder() {
    return new CustomBlockchainBuilder();
  }

  private static class CustomBlockchainBuilder extends BlockchainBuilder {
    @Override
    public Blockchain build() {
      try {

        if (isNull(super.initialDeployTopUpAmount)) {
          super.initialDeployTopUpAmount = Utils.toNano(0.1);
        }

        if (super.network != Network.EMULATOR) {
          if (isNull(super.tonlib)) {
            if (StringUtils.isNotEmpty(super.customGlobalConfigPath)) {
              super.tonlib =
                  Tonlib.builder()
                      .ignoreCache(false)
                      .pathToGlobalConfig(super.customGlobalConfigPath)
                      .build();
            } else if (super.network == Network.MAINNET) {
              super.tonlib =
                  Tonlib.builder()
                      .testnet(false)
                      .ignoreCache(false)
                      .verbosityLevel(
                          nonNull(super.tonlibVerbosityLevel)
                              ? super.tonlibVerbosityLevel
                              : VerbosityLevel.INFO)
                      .build();
            } else if (super.network == Network.TESTNET) {
              super.tonlib =
                  Tonlib.builder()
                      .testnet(true)
                      .ignoreCache(false)
                      .verbosityLevel(
                          nonNull(super.tonlibVerbosityLevel)
                              ? super.tonlibVerbosityLevel
                              : VerbosityLevel.INFO)
                      .build();

            } else { // MyLocalTon
              if (StringUtils.isNotEmpty(super.myLocalTonInstallationPath)) {
                super.tonlib =
                    Tonlib.builder()
                        .ignoreCache(false)
                        .verbosityLevel(
                            nonNull(super.tonlibVerbosityLevel)
                                ? super.tonlibVerbosityLevel
                                : VerbosityLevel.INFO)
                        .pathToGlobalConfig(
                            super.myLocalTonInstallationPath
                                + "/genesis/db/my-ton-global.config.json")
                        .build();
              } else {
                throw new Error(
                    "When using MyLocalTon network myLocalTonInstallationPath must bet set.");
              }
            }
          }
        }

        if (isNull(super.funcRunner)) {
          super.funcRunner = FuncRunner.builder().build();
        }

        if (isNull(super.fiftRunner)) {
          super.fiftRunner = FiftRunner.builder().build();
        }

        smartContractCompiler =
            SmartContractCompiler.builder()
                .fiftRunner(super.fiftRunner)
                .funcRunner(super.funcRunner)
                .build();

        if (super.network == Network.EMULATOR) {
          log.info(
              String.format(
                  "\nJava Blockchain configuration:\n"
                      + "Target network: %s\n"
                      + "Emulator location: %s, configType: %s, txVerbosity: %s, tvmVerbosity: %s\n"
                      //                      + "Tonlib location: %s\n"
                      //                      + "Tonlib global config: %s\n"
                      + "Func location: %s\n"
                      + "Fift location: %s, FIFTPATH=%s\n"
                      + "Contract: %s\n",
                  super.network,
                  Utils.detectAbsolutePath("emulator", true),
                  super.txEmulatorConfig,
                  super.txVerbosityLevel,
                  super.tvmVerbosityLevel,
                  //                  super.tonlib.pathToTonlibSharedLib,
                  //                  super.tonlib.pathToGlobalConfig,
                  super.funcRunner.funcExecutablePath,
                  super.fiftRunner.fiftExecutablePath,
                  super.fiftRunner.getLibsPath(),
                  nonNull(super.standardContract)
                      ? "standard contract " + super.standardContract.getName()
                      : isNull(super.customContractPath)
                          ? "integrated resource: " + super.customContractAsResource
                          : super.customContractPath));
        } else {
          log.info(
              String.format(
                  "Java Blockchain configuration:\n"
                      + "Target network: %s\n"
                      + "Emulator not used\n"
                      + "Tonlib location: %s\n"
                      + "Tonlib global config: %s\n"
                      + "Func location: %s\n"
                      + "Fift location: %s, FIFTPATH=%s\n"
                      + "Contract: %s\n",
                  super.network,
                  super.tonlib.pathToTonlibSharedLib,
                  super.tonlib.pathToGlobalConfig,
                  super.funcRunner.getFuncPath(),
                  super.fiftRunner.getFiftPath(),
                  super.fiftRunner.getLibsPath(),
                  nonNull(super.standardContract)
                      ? "standard contract " + super.standardContract.getName()
                      : isNull(super.customContractPath)
                          ? "integrated resource at " + super.customContractAsResource
                          : super.customContractPath));
        }
      } catch (Exception e) {
        e.printStackTrace();
        throw new Error("Error creating blockchain instance: " + e.getMessage());
      }
      return super.build();
    }
  }

  public void deploy(int waitForDeploymentSeconds) {
    log.info("deploying on {}", network);
    try {
      Address address = standardContract.getAddress();
      log.info("contract address {}", address.toRaw());

      String nonBounceableAddress;
      if (nonNull(standardContract)) {
        standardContract.getTonlib();
        if (network == Network.MAINNET
            || network == Network.TESTNET
            || network == Network.MY_LOCAL_TON) {
          ExtMessageInfo result;

          if (waitForDeploymentSeconds != 0) {
            nonBounceableAddress = address.toNonBounceableTestnet();
            if (network == Network.MAINNET) {

              log.info(
                  "waiting {}s for toncoins to be deposited to address {}",
                  waitForDeploymentSeconds,
                  nonBounceableAddress);
              tonlib.waitForBalanceChange(address, waitForDeploymentSeconds);
              log.info("now sending external message with deploy instructions...");
              Message msg = standardContract.prepareDeployMsg();
              result = tonlib.sendRawMessage(msg.toCell().toBase64());
              assert result.getError().getCode() != 0;
              tonlib.waitForDeployment(address, waitForDeploymentSeconds);
              log.info(
                  "{} deployed at address {}", standardContract.getName(), nonBounceableAddress);
            } else if (network == Network.TESTNET) {

              log.info(
                  "topping up {} with {} toncoin from TestnetFaucet",
                  nonBounceableAddress,
                  Utils.formatNanoValue(initialDeployTopUpAmount));
              BigInteger newBalance =
                  TestnetFaucet.topUpContract(
                      tonlib, standardContract.getAddress(), initialDeployTopUpAmount);
              log.info("topped up successfully, new balance {}", Utils.formatNanoValue(newBalance));
              log.info("now sending external message with deploy instructions...");
              Message msg = standardContract.prepareDeployMsg();
              result = tonlib.sendRawMessage(msg.toCell().toBase64());
              if (result.getError().getCode() != 0) {
                throw new Error(
                    "Cannot send external message. Error: " + result.getError().getMessage());
              }

              tonlib.waitForDeployment(address, waitForDeploymentSeconds);
              log.info(
                  "{} deployed at address {}", standardContract.getName(), nonBounceableAddress);
            } else { // myLocalTon

              // top up first
              BigInteger newBalance = topUpFromMyLocalTonFaucet(address);
              log.info("topped up successfully, new balance {}", Utils.formatNanoValue(newBalance));
              // deploy smc
              Message msg = standardContract.prepareDeployMsg();
              result = tonlib.sendRawMessage(msg.toCell().toBase64());
              if (result.getError().getCode() != 0) {
                throw new Error(
                    "Cannot send external message. Error: " + result.getError().getMessage());
              }

              tonlib.waitForDeployment(address, waitForDeploymentSeconds);
              log.info(
                  "{} deployed at address {}", standardContract.getName(), nonBounceableAddress);
            }
          }
        } else {
          tvmEmulator =
              TvmEmulator.builder()
                  .codeBoc(standardContract.createCodeCell().toBase64())
                  .dataBoc(standardContract.createDataCell().toBase64())
                  .verbosityLevel(tvmVerbosityLevel)
                  .build();

          txEmulator = TxEmulator.builder().verbosityLevel(txVerbosityLevel).build();
          log.info("deployed at emulators");
        }
      } else { // deploy customer contract
        // todo
        log.info("deploying custom contract");
      }
    } catch (Exception e) {
      e.printStackTrace();
      //      throw new Error("Cannot deploy the contract on " + network + ". Error " +
      // e.getMessage());
    }
  }

  private BigInteger topUpFromMyLocalTonFaucet(Address address) {
    ExtMessageInfo result;
    String nonBounceableAddress;
    WalletV3R2 faucetMyLocalTonWallet =
        WalletV3R2.builder()
            .tonlib(tonlib)
            .walletId(42)
            .keyPair(
                TweetNaclFast.Signature.keyPair_fromSeed(
                    Utils.hexToSignedBytes(
                        "44e67357b8e3333b617eb62f759890c95a6bb3cc95557ba60b80b8619f8b7c9d")))
            .build();
    log.info("faucetMyLocalTonWallet address {}", faucetMyLocalTonWallet.getAddress().toRaw());

    log.info("mlt faucet balance {}", faucetMyLocalTonWallet.getBalance());
    nonBounceableAddress = address.toNonBounceable();
    log.info(
        "topping up {} with {} toncoin from TestnetFaucet",
        nonBounceableAddress,
        Utils.formatNanoValue(initialDeployTopUpAmount));

    WalletV3Config walletV3Config =
        WalletV3Config.builder()
            .bounce(false)
            .walletId(42)
            .seqno(tonlib.getSeqno(faucetMyLocalTonWallet.getAddress()))
            .destination(address)
            .amount(initialDeployTopUpAmount)
            .comment("top-up from ton4j faucet")
            .build();

    result = faucetMyLocalTonWallet.send(walletV3Config);

    if (result.getError().getCode() != 0) {
      throw new Error("Cannot send external message. Error: " + result.getError().getMessage());
    }

    tonlib.waitForBalanceChange(address, 20);
    return tonlib.getAccountBalance(address);
  }
}
