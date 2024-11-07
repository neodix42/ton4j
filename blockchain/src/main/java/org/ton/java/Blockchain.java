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
import org.ton.java.emulator.EmulateTransactionResult;
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
import org.ton.java.tlb.types.*;
import org.ton.java.tolk.TolkRunner;
import org.ton.java.tonlib.Tonlib;
import org.ton.java.tonlib.types.*;
import org.ton.java.utils.Utils;

@Slf4j
@Builder
public class Blockchain {

  private FuncRunner funcRunner;
  private FiftRunner fiftRunner;
  private TolkRunner tolkRunner;
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
  ShardAccount customEmulatorShardAccount;
  private static SmartContractCompiler smartContractCompiler;
  private static TxEmulator txEmulator;
  private static TvmEmulator tvmEmulator;
  private static StateInit stateInit;
  private static Cell codeCell;
  private static Cell dataCell;

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

        initializeTonlib();

        initializeSmartContractCompiler();

        compileSmartContract();

        initializeEmulators();

        printBlockchainInfo();

      } catch (Exception e) {
        e.printStackTrace();
        throw new Error("Error creating blockchain instance: " + e.getMessage());
      }
      return super.build();
    }

    private void initializeEmulators() {
      if (super.network == Network.EMULATOR) {
        tvmEmulator =
            TvmEmulator.builder()
                .codeBoc(codeCell.toBase64())
                .dataBoc(dataCell.toBase64())
                .verbosityLevel(super.tvmEmulatorVerbosityLevel)
                .build();

        txEmulator = TxEmulator.builder().verbosityLevel(super.txEmulatorVerbosityLevel).build();

        if (isNull(super.customEmulatorShardAccount)) {
          super.customEmulatorShardAccount =
              ShardAccount.builder()
                  .account(
                      Account.builder()
                          .isNone(false)
                          .address(MsgAddressIntStd.of(stateInit.getAddress()))
                          .storageInfo(
                              StorageInfo.builder()
                                  .storageUsed(
                                      StorageUsed.builder()
                                          .cellsUsed(BigInteger.ZERO)
                                          .bitsUsed(BigInteger.ZERO)
                                          .publicCellsUsed(BigInteger.ZERO)
                                          .build())
                                  .lastPaid(System.currentTimeMillis() / 1000)
                                  .duePayment(BigInteger.ZERO)
                                  .build())
                          .accountStorage(
                              AccountStorage.builder()
                                  .lastTransactionLt(BigInteger.ZERO)
                                  .balance(
                                      CurrencyCollection.builder()
                                          .coins(super.initialDeployTopUpAmount) // initial balance
                                          .build())
                                  .accountState(
                                      AccountStateActive.builder()
                                          .stateInit(
                                              StateInit.builder()
                                                  .code(codeCell)
                                                  .data(dataCell)
                                                  .build())
                                          .build())
                                  .build())
                          .build())
                  .lastTransHash(BigInteger.ZERO)
                  .lastTransLt(BigInteger.ZERO)
                  .build();
        }
      } else {
        super.contract.setTonlib(super.tonlib);
      }
    }

    private void compileSmartContract() {
      if (isNull(super.contract)) {
        if (nonNull(super.customContractAsResource)) {
          smartContractCompiler.setContractAsResource(super.customContractAsResource);
        } else if (nonNull(super.customContractPath)) {
          smartContractCompiler.setContractPath(super.customContractPath);
        } else {
          throw new Error(
              "Specify path to custom contract via customContractAsResource or customContractPath.");
        }

        codeCell = smartContractCompiler.compileToCell();
        if (isNull(super.customContractDataCell)) {
          throw new Error("Custom contract requires customContractDataCell to be specified.");
        }
        dataCell = super.customContractDataCell;
      } else {
        // no need to compile regular smart-contract
        codeCell = super.contract.createCodeCell();
        dataCell = super.contract.createDataCell();
      }
      stateInit = StateInit.builder().code(codeCell).data(dataCell).build();
    }

    private void initializeSmartContractCompiler() {
      if (isNull(super.funcRunner)) {
        super.funcRunner = FuncRunner.builder().build();
      }

      if (isNull(super.fiftRunner)) {
        super.fiftRunner = FiftRunner.builder().build();
      }

      if (isNull(super.tolkRunner)) {
        super.tolkRunner = TolkRunner.builder().build();
      }

      smartContractCompiler =
          SmartContractCompiler.builder()
              .fiftRunner(super.fiftRunner)
              .funcRunner(super.funcRunner)
              .tolkRunner(super.tolkRunner)
              .build();
    }

    private void printBlockchainInfo() {
      if (super.network == Network.EMULATOR) {

        log.info(
            "\nJava Blockchain configuration:\n"
                + "Target network: {}\n"
                + "Emulator location: {}, configType: {}, txVerbosity: {}, tvmVerbosity: {}\n"
                + "Emulator ShardAccount: balance {}, address: {}, lastPaid: {}, lastTransLt: {}\n"
                + "Func location: {}\n"
                + "Tolk location: {}\n"
                + "Fift location: {}, FIFTPATH={}\n"
                + "Contract: {}\n",
            super.network,
            Utils.detectAbsolutePath("emulator", true),
            txEmulator.getConfigType(),
            txEmulator.getVerbosityLevel(),
            tvmEmulator.getVerbosityLevel(),
            Utils.formatNanoValue(super.customEmulatorShardAccount.getBalance()),
            super.customEmulatorShardAccount.getAccount().getAddress().toAddress().toBounceable(),
            super.customEmulatorShardAccount.getAccount().getStorageInfo().getLastPaid(),
            super.customEmulatorShardAccount.getLastTransLt(),
            super.funcRunner.getFuncPath(),
            super.tolkRunner.getTolkPath(),
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
                + "Tolk location: {}\n"
                + "Fift location: {}, FIFTPATH={}\n"
                + "Contract: {}\n",
            super.network,
            super.tonlib.pathToTonlibSharedLib,
            super.tonlib.pathToGlobalConfig,
            super.funcRunner.getFuncPath(),
            super.tolkRunner.getTolkPath(),
            super.fiftRunner.getFiftPath(),
            super.fiftRunner.getLibsPath(),
            nonNull(super.contract)
                ? "standard contract " + super.contract.getName()
                : isNull(super.customContractPath)
                    ? "integrated resource " + super.customContractAsResource
                    : super.customContractPath);
      }
    }

    private void initializeTonlib() {
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
    }
  }

  public SendExternalResult sendExternal(Message message) {
    log.info("sending external message on {}", network);

    ExtMessageInfo tonlibResult = null;
    try {
      if (network != Network.EMULATOR) {
        tonlibResult = tonlib.sendRawMessage(message.toCell().toBase64());
        if (tonlibResult.getError().getCode() != 0) {
          throw new Error(
              "Cannot send external message on "
                  + network
                  + ". Error code "
                  + tonlibResult.getError().getCode());
        } else {
          log.info("successfully sent external message on {}", network);
        }
        return SendExternalResult.builder().tonlibResult(tonlibResult).build();
      } else { // emulator
        EmulateTransactionResult emulateTransactionResult =
            txEmulator.emulateTransaction(
                customEmulatorShardAccount.toCell().toBase64(), message.toCell().toBase64());
        if (emulateTransactionResult.isSuccess()) {
          customEmulatorShardAccount = emulateTransactionResult.getNewShardAccount();
          emulateTransactionResult.getTransaction().printTransactionFees(true, true);
          emulateTransactionResult.getTransaction().printAllMessages(true);
        } else {
          log.error("Cannot emulate transaction. Error " + emulateTransactionResult.getError());
        }
        return SendExternalResult.builder().emulatorResult(emulateTransactionResult).build();
      }

    } catch (Exception e) {
      e.printStackTrace();
      throw new Error("Cannot send external message on " + network + ". Error " + e.getMessage());
    }
  }

  public boolean deploy(int waitForDeploymentSeconds) {
    log.info("deploying on {}", network);
    try {

      if (nonNull(contract)) {
        deployRegularContract(contract, waitForDeploymentSeconds);
      } else { // deploy on emulator custom contract
        deployCustomContract(stateInit, waitForDeploymentSeconds);
      }
      log.info("deployed on {}", network);
      return true;
    } catch (Exception e) {
      log.error("Cannot deploy the contract on " + network + ". Error " + e.getMessage());
      e.printStackTrace();
      return false;
    }
  }

  public GetterResult runGetMethod(String methodName) {
    log.info("running GetMethod {} on {}", methodName, network);
    if (network == Network.EMULATOR) {
      return GetterResult.builder().emulatorResult(tvmEmulator.runGetMethod(methodName)).build();
    } else {
      Address address;
      if (nonNull(contract)) {
        address = contract.getAddress();
      } else {
        address = stateInit.getAddress();
      }
      return GetterResult.builder().tonlibResult(tonlib.runMethod(address, methodName)).build();
    }
  }

  public BigInteger runGetSeqNo() {
    log.info("running {} on {}", "seqno", network);
    if (network == Network.EMULATOR) {
      return tvmEmulator.runGetSeqNo();
    } else {
      Address address;
      if (nonNull(contract)) {
        address = contract.getAddress();
      } else {
        address = stateInit.getAddress();
      }
      RunResult result = tonlib.runMethod(address, "seqno");
      if (result.getExit_code() != 0) {
        if (network == Network.TESTNET) {
          throw new Error(
              "Cannot get seqno from contract "
                  + address.toBounceableTestnet()
                  + ", exitCode "
                  + result.getExit_code());
        } else {
          throw new Error(
              "Cannot get seqno from contract "
                  + address.toBounceable()
                  + ", exitCode "
                  + result.getExit_code());
        }
      }
      TvmStackEntryNumber seqno = (TvmStackEntryNumber) result.getStack().get(0);

      return seqno.getNumber();
    }
  }

  public String runGetPublicKey() {
    log.info("running {} on {}", "get_public_key", network);
    if (network == Network.EMULATOR) {
      return tvmEmulator.runGetPublicKey();
    } else {
      Address address;
      if (nonNull(contract)) {
        address = contract.getAddress();
      } else {
        address = stateInit.getAddress();
      }
      RunResult result = tonlib.runMethod(address, "get_public_key");
      if (result.getExit_code() != 0) {
        if (network == Network.TESTNET) {
          throw new Error(
              "Cannot get_public_key from contract "
                  + address.toBounceableTestnet()
                  + ", exitCode "
                  + result.getExit_code());
        } else {
          throw new Error(
              "Cannot get_public_key from contract "
                  + address.toBounceable()
                  + ", exitCode "
                  + result.getExit_code());
        }
      }
      TvmStackEntryNumber publicKeyNumber = (TvmStackEntryNumber) result.getStack().get(0);
      return publicKeyNumber.getNumber().toString(16);
    }
  }

  public BigInteger runGetSubWalletId() {
    log.info("running {} on {}", "get_subwallet_id", network);
    if (network == Network.EMULATOR) {
      return tvmEmulator.runGetSubWalletId();
    } else {
      Address address;
      if (nonNull(contract)) {
        address = contract.getAddress();
      } else {
        address = stateInit.getAddress();
      }
      RunResult result = tonlib.runMethod(address, "get_subwallet_id");
      if (result.getExit_code() != 0) {
        if (network == Network.TESTNET) {
          throw new Error(
              "Cannot get_subwallet_id from contract "
                  + address.toBounceableTestnet()
                  + ", exitCode "
                  + result.getExit_code());
        } else {
          throw new Error(
              "Cannot get_subwallet_id from contract "
                  + address.toBounceable()
                  + ", exitCode "
                  + result.getExit_code());
        }
      }
      TvmStackEntryNumber subWalletId = (TvmStackEntryNumber) result.getStack().get(0);

      return subWalletId.getNumber();
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

    log.info("myLocalTon faucet balance {}", faucetMyLocalTonWallet.getBalance());
    nonBounceableAddress = address.toNonBounceable();
    log.info(
        "topping up {} with {} toncoin from MyLocalTon Faucet",
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
          log.info("sending external message with deploy instructions...");
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
          log.info("sending external message with deploy instructions...");
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
          log.info("sending external message with deploy instructions...");
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
          log.info("sending external message with deploy instructions...");
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
    }
  }
}
