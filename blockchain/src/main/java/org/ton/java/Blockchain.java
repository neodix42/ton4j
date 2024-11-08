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

        if (super.network != Network.EMULATOR) {
          printBlockchainInfo();
        }

        compileSmartContract();

        if (super.network == Network.EMULATOR) {
          initializeEmulators();
        }

        if (super.network == Network.EMULATOR) {
          printBlockchainInfo();
        }

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
                .printEmulatorInfo(false)
                .build();

        txEmulator =
            TxEmulator.builder()
                .verbosityLevel(super.txEmulatorVerbosityLevel)
                .printEmulatorInfo(false)
                .build();

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
        super.funcRunner = FuncRunner.builder().printInfo(false).build();
      }

      if (isNull(super.fiftRunner)) {
        super.fiftRunner = FiftRunner.builder().printInfo(false).build();
      }

      if (isNull(super.tolkRunner)) {
        super.tolkRunner = TolkRunner.builder().printInfo(false).build();
      }

      smartContractCompiler =
          SmartContractCompiler.builder()
              .fiftRunner(super.fiftRunner)
              .funcRunner(super.funcRunner)
              .tolkRunner(super.tolkRunner)
              .printFiftAsmOutput(false)
              .printInfo(false)
              .build();
    }

    private void printBlockchainInfo() {
      if (super.network == Network.EMULATOR) {

        System.out.printf(
            "Java Blockchain configuration:\n"
                + "Target network: %s\n"
                + "Emulator location: %s, configType: %s, txVerbosity: %s, tvmVerbosity: %s\n"
                + "Emulator ShardAccount: balance %s, address: %s, lastPaid: %s, lastTransLt: %s\n"
                + "Func location: %s\n"
                + "Tolk location: %s\n"
                + "Fift location: %s, FIFTPATH=%s\n"
                + "Contract: %s\n\n",
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
        System.out.printf(
            "\nBlockchain configuration:\n"
                + "Target network: %s\n"
                + "Emulator not used\n"
                + "Tonlib location: %s\n"
                + "Tonlib global config: %s\n"
                + "Func location: %s\n"
                + "Tolk location: %s\n"
                + "Fift location: %s, FIFTPATH=%s\n"
                + "Contract: %s\n\n",
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
                    .printInfo(false)
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
                    .printInfo(false)
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
                    .printInfo(false)
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
                      .printInfo(false)
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
    System.out.printf("sending external message on %s\n", network);

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
          System.out.printf("successfully sent external message on %s\n", network);
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
    System.out.printf("deploying on %s\n", network);
    try {

      if (nonNull(contract)) {
        deployRegularContract(contract, waitForDeploymentSeconds);
      } else { // deploy on emulator custom contract
        deployCustomContract(stateInit, waitForDeploymentSeconds);
      }
      System.out.printf("deployed on %s\n", network);
      return true;
    } catch (Exception e) {
      log.error("Cannot deploy the contract on " + network + ". Error " + e.getMessage());
      e.printStackTrace();
      return false;
    }
  }

  public GetterResult runGetMethod(String methodName) {
    System.out.printf("running GetMethod %s on %s\n", methodName, network);
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
    System.out.printf("running %s on %s\n", "seqno", network);
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
    System.out.printf("running %s on %s\n", "get_public_key", network);
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
    System.out.printf("running %s on %s\n", "get_subwallet_id", network);
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
    System.out.printf(
        "faucetMyLocalTonWallet address %s\n", faucetMyLocalTonWallet.getAddress().toRaw());

    System.out.printf("myLocalTon faucet balance %s\n", faucetMyLocalTonWallet.getBalance());
    nonBounceableAddress = address.toNonBounceable();
    System.out.printf(
        "topping up %s with %s toncoin from MyLocalTon Faucet\n",
        nonBounceableAddress, Utils.formatNanoValue(initialDeployTopUpAmount));

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
    System.out.printf("contract address %s\n", address.toRaw());
    //    contract.getTonlib();
    if (network != Network.EMULATOR) {
      ExtMessageInfo result;

      if (waitForDeploymentSeconds != 0) {
        String nonBounceableAddress = address.toNonBounceableTestnet();
        if (network == Network.MAINNET) {

          System.out.printf(
              "waiting %ss for toncoins to be deposited to address %s\n",
              waitForDeploymentSeconds, nonBounceableAddress);
          tonlib.waitForBalanceChange(address, waitForDeploymentSeconds);
          System.out.println("sending external message with deploy instructions...");
          Message msg = contract.prepareDeployMsg();
          result = tonlib.sendRawMessage(msg.toCell().toBase64());
          assert result.getError().getCode() != 0;
          tonlib.waitForDeployment(address, waitForDeploymentSeconds);
          System.out.printf(
              "%s deployed at address %s\n", contract.getName(), nonBounceableAddress);
        } else if (network == Network.TESTNET) {

          System.out.printf(
              "topping up %s with %s toncoin from TestnetFaucet\n",
              nonBounceableAddress, Utils.formatNanoValue(initialDeployTopUpAmount));
          BigInteger newBalance =
              TestnetFaucet.topUpContract(tonlib, contract.getAddress(), initialDeployTopUpAmount);
          System.out.printf(
              "topped up successfully, new balance %s\n", Utils.formatNanoValue(newBalance));
          System.out.println("sending external message with deploy instructions...");
          Message msg = contract.prepareDeployMsg();
          result = tonlib.sendRawMessage(msg.toCell().toBase64());
          if (result.getError().getCode() != 0) {
            throw new Error(
                "Cannot send external message. Error: " + result.getError().getMessage());
          }

          tonlib.waitForDeployment(address, waitForDeploymentSeconds);
          System.out.printf(
              "%s deployed at address %s\n", contract.getName(), nonBounceableAddress);
        } else { // myLocalTon

          // top up first
          BigInteger newBalance = topUpFromMyLocalTonFaucet(address);
          System.out.printf(
              "topped up successfully, new balance %s\n", Utils.formatNanoValue(newBalance));
          // deploy smc
          Message msg = contract.prepareDeployMsg();
          result = tonlib.sendRawMessage(msg.toCell().toBase64());
          if (result.getError().getCode() != 0) {
            throw new Error(
                "Cannot send external message. Error: " + result.getError().getMessage());
          }

          tonlib.waitForDeployment(address, waitForDeploymentSeconds);
          System.out.printf(
              "%s deployed at address %s\n", contract.getName(), nonBounceableAddress);
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
    System.out.printf("contract address %s\n", address.toRaw());
    if (network != Network.EMULATOR) {
      ExtMessageInfo result;

      if (waitForDeploymentSeconds != 0) {
        String nonBounceableAddress = address.toNonBounceableTestnet();
        if (network == Network.MAINNET) {

          System.out.printf(
              "waiting %ss for toncoins to be deposited to address %s\n",
              waitForDeploymentSeconds, nonBounceableAddress);
          tonlib.waitForBalanceChange(address, waitForDeploymentSeconds);
          System.out.println("sending external message with deploy instructions...");
          Message msg =
              MsgUtils.createExternalMessage(
                  address,
                  stateInit,
                  isNull(customContractBodyCell) ? stateInit.getData() : customContractDataCell);
          result = tonlib.sendRawMessage(msg.toCell().toBase64());
          assert result.getError().getCode() != 0;
          tonlib.waitForDeployment(address, waitForDeploymentSeconds);
          System.out.printf("%s deployed at address %s", contractName, nonBounceableAddress);
        } else if (network == Network.TESTNET) {

          System.out.printf(
              "topping up %s with %s toncoin from TestnetFaucet\n",
              nonBounceableAddress, Utils.formatNanoValue(initialDeployTopUpAmount));
          BigInteger newBalance =
              TestnetFaucet.topUpContract(tonlib, address, initialDeployTopUpAmount);
          System.out.printf(
              "topped up successfully, new balance %s", Utils.formatNanoValue(newBalance));
          System.out.println("sending external message with deploy instructions...");
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
          System.out.printf("%s deployed at address %s\n", contractName, nonBounceableAddress);
        } else { // myLocalTon

          // top up first
          BigInteger newBalance = topUpFromMyLocalTonFaucet(address);
          System.out.printf(
              "topped up successfully, new balance %s\n", Utils.formatNanoValue(newBalance));
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
          System.out.printf("%s deployed at address %s\n", contractName, nonBounceableAddress);
        }
      }
    }
  }
}
