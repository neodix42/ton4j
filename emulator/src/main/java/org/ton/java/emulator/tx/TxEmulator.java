package org.ton.java.emulator.tx;

import static java.util.Objects.isNull;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.ToNumberPolicy;
import com.sun.jna.Native;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.WinNT;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.ton.java.cell.Cell;
import org.ton.java.emulator.EmulateTransactionResult;
import org.ton.java.tlb.types.*;
import org.ton.java.utils.Utils;

/**
 * If not specified then tries to find emulator in system folder, more info <a
 * href="https://github.com/ton-blockchain/packages">here</a>
 */
@Slf4j
@Builder
@Getter
public class TxEmulator {

  public String pathToEmulatorSharedLib;
  private final TxEmulatorI txEmulatorI;
  private final long txEmulator;
  private TxEmulatorConfig configType;
  private String customConfig;
  private TxVerbosityLevel verbosityLevel;
  private Boolean printEmulatorInfo;

  public static class TxEmulatorBuilder {}

  public static TxEmulatorBuilder builder() {
    return new CustomEmulatorBuilder();
  }

  private static class CustomEmulatorBuilder extends TxEmulatorBuilder {
    @Override
    public TxEmulator build() {
      try {

        if (isNull(super.pathToEmulatorSharedLib)) {
          if ((Utils.getOS() == Utils.OS.WINDOWS) || (Utils.getOS() == Utils.OS.WINDOWS_ARM)) {
            super.pathToEmulatorSharedLib = Utils.detectAbsolutePath("emulator", true);
          } else {
            super.pathToEmulatorSharedLib = Utils.detectAbsolutePath("libemulator", true);
          }
        }

        if (isNull(super.printEmulatorInfo)) {
          super.printEmulatorInfo = true;
        }

        super.txEmulatorI = Native.load(super.pathToEmulatorSharedLib, TxEmulatorI.class);

        if (isNull(super.verbosityLevel)) {
          super.verbosityLevel = TxVerbosityLevel.TRUNCATED;
        }

        redirectNativeOutput();

        if (isNull(super.configType)) {
          super.configType = TxEmulatorConfig.MAINNET;
        }

        String configBoc = "";
        switch (super.configType) {
          case MAINNET:
            {
              configBoc =
                  IOUtils.toString(
                      Objects.requireNonNull(
                          TxEmulator.class.getResourceAsStream("/config-all-mainnet.txt")),
                      StandardCharsets.UTF_8);
              break;
            }
          case TESTNET:
            {
              configBoc =
                  IOUtils.toString(
                      Objects.requireNonNull(
                          TxEmulator.class.getResourceAsStream("/config-all-testnet.txt")),
                      StandardCharsets.UTF_8);
              break;
            }
          case CUSTOM:
            {
              configBoc = super.customConfig;
              break;
            }
        }

        super.txEmulator =
            super.txEmulatorI.transaction_emulator_create(
                configBoc, super.verbosityLevel.ordinal());

        super.txEmulatorI.emulator_set_verbosity_level(
            super.txEmulator, super.verbosityLevel.ordinal());

        if (super.verbosityLevel == TxVerbosityLevel.WITH_ALL_STACK_VALUES) {
          super.txEmulatorI.transaction_emulator_set_debug_enabled(super.txEmulator, true);
        }

        if (super.txEmulator == 0) {
          throw new Error("Can't create tx emulator instance");
        }

        if (super.printEmulatorInfo) {

          log.info(
              "\nTON Tx Emulator configuration:\n"
                  + "Location: {}\n"
                  + "Config: {}\n"
                  + "Verbosity level: {}",
              super.pathToEmulatorSharedLib,
              super.configType,
              super.verbosityLevel);
        }
        return super.build();
      } catch (Exception e) {
        throw new Error("Error creating tx emulator instance: " + e.getMessage());
      }
    }
  }

  private static void redirectNativeOutput() {

    if ((Utils.getOS() == Utils.OS.WINDOWS) || (Utils.getOS() == Utils.OS.WINDOWS_ARM)) {
      // Redirect native output on Windows
      WinNT.HANDLE originalOut = Kernel32.INSTANCE.GetStdHandle(Kernel32.STD_OUTPUT_HANDLE);
      WinNT.HANDLE originalErr = Kernel32.INSTANCE.GetStdHandle(Kernel32.STD_ERROR_HANDLE);

      try (FileOutputStream nulStream = new FileOutputStream("NUL")) {
        WinNT.HANDLE hNul =
            Kernel32.INSTANCE.CreateFile(
                "NUL",
                Kernel32.GENERIC_WRITE,
                Kernel32.FILE_SHARE_WRITE,
                null,
                Kernel32.OPEN_EXISTING,
                0,
                null);

        // Redirect stdout and stderr to NUL
        Kernel32.INSTANCE.SetStdHandle(Kernel32.STD_OUTPUT_HANDLE, hNul);
        Kernel32.INSTANCE.SetStdHandle(Kernel32.STD_ERROR_HANDLE, hNul);

        // Close the handle to NUL
        Kernel32.INSTANCE.CloseHandle(hNul);
      } catch (IOException e) {
        throw new RuntimeException(e);
      } finally {
        // Restore original stdout and stderr
        Kernel32.INSTANCE.SetStdHandle(Kernel32.STD_OUTPUT_HANDLE, originalOut);
        Kernel32.INSTANCE.SetStdHandle(Kernel32.STD_ERROR_HANDLE, originalErr);
      }
    } else if ((Utils.getOS() == Utils.OS.LINUX) || (Utils.getOS() == Utils.OS.LINUX_ARM)) {
      // asdf
    } else if ((Utils.getOS() == Utils.OS.MAC) || (Utils.getOS() == Utils.OS.MAC_ARM64)) {
      // asdf
    }
  }

  public void destroy() {
    txEmulatorI.transaction_emulator_destroy(txEmulator);
  }

  /**
   * Emulate transaction
   *
   * @param shardAccountBoc Base64 encoded BoC serialized ShardAccount
   * @param messageBoc Base64 encoded BoC serialized inbound Message (internal or external)
   * @return Json object with error: { "success": false, "error": "Error description",
   *     "external_not_accepted": false, // and optional fields "vm_exit_code", "vm_log",
   *     "elapsed_time" in case external message was not accepted. } Or success: { "success": true,
   *     "transaction": "Base64 encoded Transaction boc", "shard_account": "Base64 encoded new
   *     ShardAccount boc", "vm_log": "execute DUP...", "actions": "Base64 encoded compute phase
   *     actions boc (OutList n)", "elapsed_time": 0.02 }
   */
  public EmulateTransactionResult emulateTransaction(String shardAccountBoc, String messageBoc) {
    String result =
        txEmulatorI.transaction_emulator_emulate_transaction(
            txEmulator, shardAccountBoc, messageBoc);
    Gson gson = new GsonBuilder().setObjectToNumberStrategy(ToNumberPolicy.BIG_DECIMAL).create();
    return gson.fromJson(result, EmulateTransactionResult.class);
  }

  /**
   * Emulate transaction
   *
   * @param code code cell of a contract
   * @param data data cell of a contract
   * @param initialBalance Initial balance in nanacoins
   * @param messageBoc Base64 encoded BoC serialized inbound Message (internal or external)
   * @return Json object with error: { "success": false, "error": "Error description",
   *     "external_not_accepted": false, // and optional fields "vm_exit_code", "vm_log",
   *     "elapsed_time" in case external message was not accepted. } Or success: { "success": true,
   *     "transaction": "Base64 encoded Transaction boc", "shard_account": "Base64 encoded new
   *     ShardAccount boc", "vm_log": "execute DUP...", "actions": "Base64 encoded compute phase
   *     actions boc (OutList n)", "elapsed_time": 0.02 }
   */
  public EmulateTransactionResult emulateTransaction(
      Cell code, Cell data, BigInteger initialBalance, String messageBoc) {

    StateInit stateInit = StateInit.builder().code(code).data(data).build();

    ShardAccount shardAccount =
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
                            .balance(CurrencyCollection.builder().coins(initialBalance).build())
                            .accountState(AccountStateActive.builder().stateInit(stateInit).build())
                            .build())
                    .build())
            .lastTransHash(BigInteger.ZERO)
            .lastTransLt(BigInteger.ZERO)
            .build();

    String shardAccountBocBase64 = shardAccount.toCell().toBase64();

    String result =
        txEmulatorI.transaction_emulator_emulate_transaction(
            txEmulator, shardAccountBocBase64, messageBoc);
    Gson gson = new GsonBuilder().setObjectToNumberStrategy(ToNumberPolicy.BIG_DECIMAL).create();
    return gson.fromJson(result, EmulateTransactionResult.class);
  }

  /**
   * Set global verbosity level of the library
   *
   * @param verbosityLevel New verbosity level (0 - never, 1 - error, 2 - warning, 3 - info, 4 -
   *     debug)
   */
  public void setVerbosityLevel(int verbosityLevel) {
    txEmulatorI.emulator_set_verbosity_level(txEmulator, verbosityLevel);
  }

  /**
   * Enable or disable TVM debug primitives
   *
   * @param debugEnabled Whether debug primitives should be enabled or not
   * @return true in case of success, false in case of error
   */
  public boolean setDebugEnabled(boolean debugEnabled) {
    return txEmulatorI.transaction_emulator_set_debug_enabled(txEmulator, debugEnabled);
  }

  /**
   * Set libs for emulation
   *
   * @param libsBoc Base64 encoded BoC serialized shared libraries dictionary (HashmapE 256 ^Cell).
   * @return true in case of success, false in case of error
   */
  public boolean setLibs(String libsBoc) {
    return txEmulatorI.transaction_emulator_set_libs(txEmulator, libsBoc);
  }

  /**
   * Set tuple of previous blocks (13th element of c7)
   *
   * @param infoBoc Base64 encoded BoC serialized TVM tuple (VmStackValue).
   * @return true in case of success, false in case of error
   */
  public boolean setPrevBlockInfo(String infoBoc) {
    return txEmulatorI.transaction_emulator_set_prev_blocks_info(txEmulator, infoBoc);
  }

  /**
   * Set rand seed for emulation
   *
   * @param randSeedHex Hex string of length 64
   * @return true in case of success, false in case of error
   */
  public boolean setRandSeed(String randSeedHex) {
    return txEmulatorI.transaction_emulator_set_rand_seed(txEmulator, randSeedHex);
  }

  /**
   * Set unixtime for emulation
   *
   * @param utime Unix timestamp
   * @return true in case of success, false in case of error
   */
  public boolean setUnixTime(long utime) {
    return txEmulatorI.transaction_emulator_set_unixtime(txEmulator, utime);
  }

  /**
   * Set config for emulation
   *
   * @param configBoc Base64 encoded BoC serialized Config dictionary (Hashmap 32 ^Cell)
   * @return true in case of success, false in case of error
   */
  public boolean setConfig(String configBoc) {
    return txEmulatorI.transaction_emulator_set_config(txEmulator, configBoc);
  }

  /**
   * Creates Config object from base64 encoded BoC
   *
   * @param configBoc Base64 encoded BoC serialized Config dictionary (Hashmap 32 ^Cell)
   * @return Pointer to Config object or nullptr in case of error
   */
  public long createConfig(String configBoc) {
    return txEmulatorI.emulator_config_create(configBoc);
  }

  /**
   * Destroy Config object
   *
   * @param config Pointer to Config object
   */
  public void destroyConfig(long config) {
    txEmulatorI.emulator_config_destroy(config);
  }

  /**
   * Emulate tick-tock transaction
   *
   * @param shardAccountBoc Base64 encoded BoC serialized ShardAccount of special account
   * @param isTock True for tock transactions, false for tick
   * @return Json object with error: { "success": false, "error": "Error description",
   *     "external_not_accepted": false } Or success: { "success": true, "transaction": "Base64
   *     encoded Transaction boc", "shard_account": "Base64 encoded new ShardAccount boc", "vm_log":
   *     "execute DUP...", "actions": "Base64 encoded compute phase actions boc (OutList n)",
   *     "elapsed_time": 0.02 }
   */
  public EmulateTransactionResult emulateTickTockTransaction(
      String shardAccountBoc, boolean isTock) {
    String result =
        txEmulatorI.transaction_emulator_emulate_tick_tock_transaction(
            txEmulator, shardAccountBoc, isTock);
    Gson gson = new GsonBuilder().setObjectToNumberStrategy(ToNumberPolicy.BIG_DECIMAL).create();
    return gson.fromJson(result, EmulateTransactionResult.class);
  }

  /**
   * Set lt for emulation
   *
   * @param lt Logical time
   * @return true in case of success, false in case of error
   */
  public boolean setEmulatorLt(long lt) {
    return txEmulatorI.transaction_emulator_set_lt(txEmulator, lt);
  }

  /**
   * Set ignore_chksig flag for emulation
   *
   * @param ignoreChksig Whether emulation should always succeed on CHKSIG operation
   * @return true in case of success, false in case of error
   */
  public boolean setIgnoreCheckSignature(boolean ignoreChksig) {
    return txEmulatorI.transaction_emulator_set_ignore_chksig(txEmulator, ignoreChksig);
  }
}
