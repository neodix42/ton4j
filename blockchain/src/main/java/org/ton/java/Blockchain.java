package org.ton.java;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

import com.iwebpp.crypto.TweetNaclFast;
import java.math.BigInteger;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.ton.java.address.Address;
import org.ton.java.cell.Cell;
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
import org.ton.java.smartcontract.utils.MsgUtils;
import org.ton.java.smartcontract.wallet.Contract;
import org.ton.java.smartcontract.wallet.v3.WalletV3R2;
import org.ton.java.tlb.types.Message;
import org.ton.java.tlb.types.StateInit;
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
  TxVerbosityLevel txEmulatorVerbosityLevel;
  TvmVerbosityLevel tvmEmulatorVerbosityLevel;
  Contract contract;
  String myLocalTonInstallationPath;
  String customContractPath;
  String customContractAsResource;
  Cell customContractDataCell;
  Cell customContractBodyCell;
  String customGlobalConfigPath;
  String customEmulatorPath;
  private static SmartContractCompiler smartContractCompiler;
  private static TxEmulator txEmulator;
  private static TvmEmulator tvmEmulator;
  private static StateInit stateInit;

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

        // initiate tonlib
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

        // compile contract with Contract interface
        if (nonNull(super.contract)) {
          tvmEmulator =
              TvmEmulator.builder()
                  .codeBoc(super.contract.createCodeCell().toBase64())
                  .dataBoc(super.contract.createDataCell().toBase64())
                  .verbosityLevel(super.tvmEmulatorVerbosityLevel)
                  .build();
          stateInit =
              StateInit.builder()
                  .code(super.contract.createCodeCell())
                  .data(super.contract.createDataCell())
                  .build();

          txEmulator = TxEmulator.builder().verbosityLevel(super.txEmulatorVerbosityLevel).build();
        } else {
          // compile custom contract
          if (nonNull(super.customContractAsResource)) {
            smartContractCompiler.setContractAsResource(super.customContractAsResource);
          } else if (nonNull(super.customContractPath)) {
            smartContractCompiler.setContractPath(super.customContractPath);
          } else {
            throw new Error(
                "Specify path to custom contract via customContractAsResource or customContractPath.");
          }

          Cell codeCell = smartContractCompiler.compileToCell();
          if (isNull(super.customContractDataCell)) {
            throw new Error("Custom contract requires customContractDataCell to be specified.");
          }
          tvmEmulator =
              TvmEmulator.builder()
                  .codeBoc(codeCell.toBase64())
                  .dataBoc(super.customContractDataCell.toBase64())
                  .verbosityLevel(super.tvmEmulatorVerbosityLevel)
                  .build();

          stateInit = StateInit.builder().code(codeCell).data(super.customContractDataCell).build();

          txEmulator = TxEmulator.builder().verbosityLevel(super.txEmulatorVerbosityLevel).build();
        }
        if (super.network == Network.EMULATOR) {

          log.info(
              "\nJava Blockchain configuration:\n"
                  + "Target network: {}\n"
                  + "Emulator location: {}, configType: {}, txVerbosity: {}, tvmVerbosity: {}\n"
                  + "Func location: {}\n"
                  + "Fift location: {}, FIFTPATH={}\n"
                  + "Contract: {}\n",
              super.network,
              Utils.detectAbsolutePath("emulator", true),
              txEmulator.getConfigType(),
              txEmulator.getVerbosityLevel(),
              tvmEmulator.getVerbosityLevel(),
              super.funcRunner.getFuncPath(),
              super.fiftRunner.getFiftPath(),
              super.fiftRunner.getLibsPath(),
              nonNull(super.contract)
                  ? "standard contract " + super.contract.getName()
                  : isNull(super.customContractPath)
                      ? "integrated resource " + super.customContractAsResource
                      : super.customContractPath);
        } else {
          log.info(
              "\nBlockchain configuration:\n"
                  + "Target network: {}\n"
                  + "Emulator not used\n"
                  + "Tonlib location: {}\n"
                  + "Tonlib global config: {}\n"
                  + "Func location: {}\n"
                  + "Fift location: {}, FIFTPATH={}\n"
                  + "Contract: {}\n",
              super.network,
              super.tonlib.pathToTonlibSharedLib,
              super.tonlib.pathToGlobalConfig,
              super.funcRunner.getFuncPath(),
              super.fiftRunner.getFiftPath(),
              super.fiftRunner.getLibsPath(),
              nonNull(super.contract)
                  ? "standard contract " + super.contract.getName()
                  : isNull(super.customContractPath)
                      ? "integrated resource " + super.customContractAsResource
                      : super.customContractPath);
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

      if (nonNull(contract)) {
        deployRegularContract(contract, waitForDeploymentSeconds);
      } else { // deploy on emulator custom contract
        deployCustomContract(stateInit, waitForDeploymentSeconds);
      }
      log.info("deployed on {}", network);
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

  private void deployRegularContract(Contract contract, int waitForDeploymentSeconds)
      throws InterruptedException {
    Address address = contract.getAddress();
    log.info("contract address {}", address.toRaw());
    //    contract.getTonlib();
    if (network != Network.EMULATOR) {
      ExtMessageInfo result;

      if (waitForDeploymentSeconds != 0) {
        String nonBounceableAddress = address.toNonBounceableTestnet();
        if (network == Network.MAINNET) {

          log.info(
              "waiting {}s for toncoins to be deposited to address {}",
              waitForDeploymentSeconds,
              nonBounceableAddress);
          tonlib.waitForBalanceChange(address, waitForDeploymentSeconds);
          log.info("now sending external message with deploy instructions...");
          Message msg = contract.prepareDeployMsg();
          result = tonlib.sendRawMessage(msg.toCell().toBase64());
          assert result.getError().getCode() != 0;
          tonlib.waitForDeployment(address, waitForDeploymentSeconds);
          log.info("{} deployed at address {}", contract.getName(), nonBounceableAddress);
        } else if (network == Network.TESTNET) {

          log.info(
              "topping up {} with {} toncoin from TestnetFaucet",
              nonBounceableAddress,
              Utils.formatNanoValue(initialDeployTopUpAmount));
          BigInteger newBalance =
              TestnetFaucet.topUpContract(tonlib, contract.getAddress(), initialDeployTopUpAmount);
          log.info("topped up successfully, new balance {}", Utils.formatNanoValue(newBalance));
          log.info("now sending external message with deploy instructions...");
          Message msg = contract.prepareDeployMsg();
          result = tonlib.sendRawMessage(msg.toCell().toBase64());
          if (result.getError().getCode() != 0) {
            throw new Error(
                "Cannot send external message. Error: " + result.getError().getMessage());
          }

          tonlib.waitForDeployment(address, waitForDeploymentSeconds);
          log.info("{} deployed at address {}", contract.getName(), nonBounceableAddress);
        } else { // myLocalTon

          // top up first
          BigInteger newBalance = topUpFromMyLocalTonFaucet(address);
          log.info("topped up successfully, new balance {}", Utils.formatNanoValue(newBalance));
          // deploy smc
          Message msg = contract.prepareDeployMsg();
          result = tonlib.sendRawMessage(msg.toCell().toBase64());
          if (result.getError().getCode() != 0) {
            throw new Error(
                "Cannot send external message. Error: " + result.getError().getMessage());
          }

          tonlib.waitForDeployment(address, waitForDeploymentSeconds);
          log.info("{} deployed at address {}", contract.getName(), nonBounceableAddress);
        }
      }
    } else {
      log.info("emulator regular");
    }
  }

  /** custom contract does not have conventional deploy methods */
  private void deployCustomContract(StateInit stateInit, int waitForDeploymentSeconds)
      throws InterruptedException {
    String contractName =
        isNull(customContractAsResource) ? customContractPath : customContractAsResource;
    Address address = stateInit.getAddress();
    log.info("contract address {}", address.toRaw());
    if (network != Network.EMULATOR) {
      ExtMessageInfo result;

      if (waitForDeploymentSeconds != 0) {
        String nonBounceableAddress = address.toNonBounceableTestnet();
        if (network == Network.MAINNET) {

          log.info(
              "waiting {}s for toncoins to be deposited to address {}",
              waitForDeploymentSeconds,
              nonBounceableAddress);
          tonlib.waitForBalanceChange(address, waitForDeploymentSeconds);
          log.info("now sending external message with deploy instructions...");
          Message msg =
              MsgUtils.createExternalMessage(
                  address,
                  stateInit,
                  isNull(customContractBodyCell) ? stateInit.getData() : customContractDataCell);
          result = tonlib.sendRawMessage(msg.toCell().toBase64());
          assert result.getError().getCode() != 0;
          tonlib.waitForDeployment(address, waitForDeploymentSeconds);
          log.info("{} deployed at address {}", contractName, nonBounceableAddress);
        } else if (network == Network.TESTNET) {

          log.info(
              "topping up {} with {} toncoin from TestnetFaucet",
              nonBounceableAddress,
              Utils.formatNanoValue(initialDeployTopUpAmount));
          BigInteger newBalance =
              TestnetFaucet.topUpContract(tonlib, address, initialDeployTopUpAmount);
          log.info("topped up successfully, new balance {}", Utils.formatNanoValue(newBalance));
          log.info("now sending external message with deploy instructions...");
          Message msg =
              MsgUtils.createExternalMessage(
                  address,
                  stateInit,
                  isNull(customContractBodyCell) ? stateInit.getData() : customContractDataCell);
          result = tonlib.sendRawMessage(msg.toCell().toBase64());
          if (result.getError().getCode() != 0) {
            throw new Error(
                "Cannot send external message. Error: " + result.getError().getMessage());
          }

          tonlib.waitForDeployment(address, waitForDeploymentSeconds);
          log.info("{} deployed at address {}", contractName, nonBounceableAddress);
        } else { // myLocalTon

          // top up first
          BigInteger newBalance = topUpFromMyLocalTonFaucet(address);
          log.info("topped up successfully, new balance {}", Utils.formatNanoValue(newBalance));
          // deploy smc
          Message msg =
              MsgUtils.createExternalMessage(
                  address,
                  stateInit,
                  isNull(customContractBodyCell) ? stateInit.getData() : customContractDataCell);
          result = tonlib.sendRawMessage(msg.toCell().toBase64());
          if (result.getError().getCode() != 0) {
            throw new Error(
                "Cannot send external message. Error: " + result.getError().getMessage());
          }

          tonlib.waitForDeployment(address, waitForDeploymentSeconds);
          log.info("{} deployed at address {}", contractName, nonBounceableAddress);
        }
      }

    } else {
      log.info("emulator regular");
    }
  }
}
