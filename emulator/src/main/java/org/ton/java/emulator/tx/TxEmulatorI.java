package org.ton.java.emulator.tx;

import com.sun.jna.Library;

public interface TxEmulatorI extends Library {

  /**
   * Creates Config object from base64 encoded BoC
   * @param configParamBoc Base64 encoded BoC serialized Config dictionary (Hashmap 32 ^Cell)
   * @return Pointer to Config object or nullptr in case of error
   */
  long emulator_config_create(String configParamBoc);

  /**
   * Destroy Config object
   * @param config Pointer to Config object
   */
  void emulator_config_destroy(long config);

  /**
   * Get git commit hash and date of the library
   */
  String emulator_version();

  /**
   * @param configParamBoc Base64 encoded BoC serialized Config dictionary (Hashmap 32 ^Cell)
   * @param verbosityLevel Verbosity level of VM log. 0 - log truncated to last 256 characters. 1 -
   *     unlimited length log. 2 - for each command prints its cell hash and offset. 3 - for each
   *     command log prints all stack values.
   * @return Pointer to txEmulator or nullptr in case of error
   */
  long transaction_emulator_create(String configParamBoc, int verbosityLevel);

  /**
   * Destroy TransactionEmulator object
   *
   * @param txEmulator Pointer to TransactionEmulator object
   */
  void transaction_emulator_destroy(long txEmulator);

  /**
   * Emulate transaction
   *
   * @param txEmulator Pointer to TransactionEmulator object
   * @param shardAccountBoc Base64 encoded BoC serialized ShardAccount
   * @param messageBoc Base64 encoded BoC serialized inbound Message (internal or external)
   * @return Json object with error: { "success": false, "error": "Error description",
   *     "external_not_accepted": false, // and optional fields "vm_exit_code", "vm_log",
   *     "elapsed_time" in case external message was not accepted. } Or success: { "success": true,
   *     "transaction": "Base64 encoded Transaction boc", "shard_account": "Base64 encoded new
   *     ShardAccount boc", "vm_log": "execute DUP...", "actions": "Base64 encoded compute phase
   *     actions boc (OutList n)", "elapsed_time": 0.02 }
   */
  String transaction_emulator_emulate_transaction(
      long txEmulator, String shardAccountBoc, String messageBoc);

  /**
   * Set unixtime for emulation
   *
   * @param txEmulator Pointer to TransactionEmulator object
   * @param unixtime Unix timestamp
   * @return true in case of success, false in case of error
   */
  boolean transaction_emulator_set_unixtime(long txEmulator, long unixtime);

  /**
   * Set rand seed for emulation
   *
   * @param txEmulator Pointer to TransactionEmulator object
   * @param randSeedHex Hex string of length 64
   * @return true in case of success, false in case of error
   */
  boolean transaction_emulator_set_rand_seed(long txEmulator, String randSeedHex);

  /**
   * Set config for emulation
   *
   * @param txEmulator Pointer to TransactionEmulator object
   * @param configBoc Base64 encoded BoC serialized Config dictionary (Hashmap 32 ^Cell)
   * @return true in case of success, false in case of error
   */
  boolean transaction_emulator_set_config(long txEmulator, String configBoc);

  /**
   * Set libs for emulation
   *
   * @param txEmulator Pointer to TransactionEmulator object
   * @param libsBoc Base64 encoded BoC serialized shared libraries dictionary (HashmapE 256 ^Cell).
   * @return true in case of success, false in case of error
   */
  boolean transaction_emulator_set_libs(long txEmulator, String libsBoc);

  /**
   * Enable or disable TVM debug primitives
   *
   * @param txEmulator Pointer to TransactionEmulator object
   * @param debugEnabled Whether debug primitives should be enabled or not
   * @return true in case of success, false in case of error
   */
  boolean transaction_emulator_set_debug_enabled(long txEmulator, boolean debugEnabled);

  /**
   * Set tuple of previous blocks (13th element of c7)
   *
   * @param txEmulator Pointer to TransactionEmulator object
   * @param infoBoc Base64 encoded BoC serialized TVM tuple (VmStackValue).
   * @return true in case of success, false in case of error
   */
  boolean transaction_emulator_set_prev_blocks_info(long txEmulator, String infoBoc);

  /**
   * Emulate tick-tock transaction
   *
   * @param txEmulator Pointer to TransactionEmulator object
   * @param shardAccountBoc Base64 encoded BoC serialized ShardAccount of special account
   * @param isTock True for tock transactions, false for tick
   * @return Json object with error: { "success": false, "error": "Error description",
   *     "external_not_accepted": false } Or success: { "success": true, "transaction": "Base64
   *     encoded Transaction boc", "shard_account": "Base64 encoded new ShardAccount boc", "vm_log":
   *     "execute DUP...", "actions": "Base64 encoded compute phase actions boc (OutList n)",
   *     "elapsed_time": 0.02 }
   */
  String transaction_emulator_emulate_tick_tock_transaction(
      long txEmulator, String shardAccountBoc, boolean isTock);

  /**
   * Set global verbosity level of the library
   *
   * @param verbosityLevel New verbosity level (0 - never, 1 - error, 2 - warning, 3 - info, 4 -
   *     debug)
   */
  void emulator_set_verbosity_level(long txEmulator, int verbosityLevel);

  /**
   * Set lt for emulation
   *
   * @param txEmulator Pointer to TransactionEmulator object
   * @param lt Logical time
   * @return true in case of success, false in case of error
   */
  boolean transaction_emulator_set_lt(long txEmulator, long lt);

  /**
   * Set ignore_chksig flag for emulation
   *
   * @param txEmulator Pointer to TransactionEmulator object
   * @param ignoreChksig Whether emulation should always succeed on CHKSIG operation
   * @return true in case of success, false in case of error
   */
  boolean transaction_emulator_set_ignore_chksig(long txEmulator, boolean ignoreChksig);
}
