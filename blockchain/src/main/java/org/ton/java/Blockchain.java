package org.ton.java;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

import java.math.BigInteger;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
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
import org.ton.java.smartcontract.wallet.Contract;
import org.ton.java.tlb.types.Message;
import org.ton.java.tonlib.Tonlib;
import org.ton.java.tonlib.types.ExtMessageInfo;
import org.ton.java.utils.Utils;

@Slf4j
@Builder
public class Blockchain {

  private FuncRunner funcRunner;
  private FiftRunner fiftRunner;
  private Tonlib tonlib;
  private Network network;
  TxEmulatorConfig txEmulatorConfig;
  TxVerbosityLevel txVerbosityLevel;
  TvmVerbosityLevel tvmVerbosityLevel;
  Contract standardContract;
  String customContractPath;
  String customContractAsResource;
  private static SmartContractCompiler smartContractCompiler;
  private static TxEmulator txEmulator;
  private static TvmEmulator tvmEmulator;

  public static class BlockchainBuilder {}

  public static BlockchainBuilder builder() {
    return new CustomBlockchainBuilder();
  }

  private static class CustomBlockchainBuilder extends BlockchainBuilder {
    @Override
    public Blockchain build() {
      try {

        smartContractCompiler =
            SmartContractCompiler.builder()
                .fiftRunner(super.fiftRunner)
                .funcRunner(super.funcRunner)
                .build();

        if (isNull(super.tonlib)) {
          if (super.network == Network.MAINNET) {
            super.tonlib = Tonlib.builder().testnet(false).ignoreCache(false).build();
          } else if (super.network == Network.TESTNET) {
            super.tonlib = Tonlib.builder().testnet(true).ignoreCache(false).build();
          }
        }

        if (super.network == Network.EMULATOR) {
          tvmEmulator =
              TvmEmulator.builder()
                  .codeBoc("a")
                  .dataBoc("a")
                  .verbosityLevel(super.tvmVerbosityLevel)
                  .build();

          txEmulator = TxEmulator.builder().verbosityLevel(super.txVerbosityLevel).build();
        }
        if (isNull(super.funcRunner)) {
          super.funcRunner = FuncRunner.builder().build();
        }

        if (isNull(super.fiftRunner)) {
          super.fiftRunner = FiftRunner.builder().build();
        }

        if (super.network == Network.EMULATOR) {
          log.info(
              String.format(
                  "\nJava Blockchain configuration:\n"
                      + "Target network: %s\n"
                      + "Emulator location: %s, configType: %s, txVerbosity: %s, tvmVerbosity: %s\n"
                      + "Tonlib location: %s\n"
                      + "Tonlib global config: %s\n"
                      + "Func location: %s\n"
                      + "Fift location: %s, FIFTPATH=%s\n"
                      + "Contract: %s\n",
                  super.network,
                  txEmulator.pathToEmulatorSharedLib,
                  super.txEmulatorConfig,
                  super.txVerbosityLevel,
                  super.tvmVerbosityLevel,
                  super.tonlib.pathToTonlibSharedLib,
                  super.tonlib.pathToGlobalConfig,
                  super.funcRunner.funcExecutablePath,
                  super.fiftRunner.fiftExecutablePath,
                  super.fiftRunner.getLibsPath(),
                  nonNull(super.standardContract)
                      ? "standard contract"
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
        throw new RuntimeException("Error creating blockchain instance: " + e.getMessage());
      }
      return super.build();
    }
  }

  public void deploy(int waitForDeploymentSeconds) {
    log.info("deploying on {}", network);
    try {
      Address address = standardContract.getAddress();
      String nonBounceableAddress;
      if (nonNull(standardContract)) {
        standardContract.getTonlib();
        if (network == Network.MAINNET || network == Network.TESTNET) {
          ExtMessageInfo result;

          if (waitForDeploymentSeconds != 0) {
            if (network == Network.MAINNET) {

              nonBounceableAddress = address.toNonBounceable();
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
            } else {
              nonBounceableAddress = address.toNonBounceableTestnet();
              log.info("topping up {} with 0.1 toncoin from TestnetFaucet", nonBounceableAddress);
              BigInteger newBalance =
                  TestnetFaucet.topUpContract(
                      tonlib, Address.of(standardContract.getAddress()), Utils.toNano(0.1));
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
        }
      }
    } catch (Exception e) {
      throw new Error("Cannot deploy the contract on " + network + ". Error " + e.getMessage());
    }
  }
}
