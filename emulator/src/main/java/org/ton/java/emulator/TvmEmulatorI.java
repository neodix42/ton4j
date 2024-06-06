package org.ton.java.emulator;

import com.sun.jna.Library;

import java.math.BigInteger;

public interface TvmEmulatorI extends Library {

    /**
     * @param codeBoc        Base64 encoded BoC serialized of contract code
     * @param dataBoc        Base64 encoded BoC serialized of contract data
     * @param verbosityLevel Verbosity level of VM log.
     *                       0 - log truncated to last 256 characters.
     *                       1 - unlimited length log.
     *                       2 - for each command prints its cell hash and offset.
     *                       3 - for each command log prints all stack values.
     * @return Pointer to tvmEmulator or nullptr in case of error
     */
    long tvm_emulator_create(String codeBoc, String dataBoc, int verbosityLevel);

    /**
     * Destroy Destroy TVM emulator object
     *
     * @param tvmEmulator Pointer to TvmEmulator object
     */
    void tvm_emulator_destroy(long tvmEmulator);

    /**
     * Set libraries for TVM emulator
     *
     * @param tvmEmulator Pointer to TVM emulator
     * @param libsBoc     Base64 encoded BoC serialized libraries dictionary (HashmapE 256 ^Cell).
     * @return true in case of success, false in case of error
     */
    boolean tvm_emulator_set_libraries(long tvmEmulator, String libsBoc);

    /**
     * Set c7 parameters
     *
     * @param tvmEmulator Pointer to TVM emulator
     * @param address     Address of smart contract
     * @param unixTime    Unix timestamp
     * @param balance     Smart contract balance
     * @param randSeedHex Random seed as hex string of length 64
     * @param config      Base64 encoded BoC serialized Config dictionary (Hashmap 32 ^Cell). Optional.
     * @return true in case of success, false in case of error
     */
    boolean tvm_emulator_set_c7(long tvmEmulator, String address, long unixTime, long balance, String randSeedHex, String config);

    /**
     * Set tuple of previous blocks (13th element of c7)
     *
     * @param tvmEmulator Pointer to TVM emulator
     * @param infoBoc     Base64 encoded BoC serialized TVM tuple (VmStackValue).
     * @return true in case of success, false in case of error
     */
    boolean tvm_emulator_set_prev_blocks_info(long tvmEmulator, String infoBoc);

    /**
     * Set TVM gas limit
     *
     * @param tvmEmulator Pointer to TVM emulator
     * @param gasLimit    Gas limit
     * @return true in case of success, false in case of error
     */
    boolean tvm_emulator_set_gas_limit(long tvmEmulator, long gasLimit);

    /**
     * Enable or disable TVM debug primitives
     *
     * @param tvmEmulator  Pointer to TVM emulator
     * @param debugEnabled Whether debug primitives should be enabled or not
     * @return true in case of success, false in case of error
     */
    boolean tvm_emulator_set_debug_enabled(long tvmEmulator, boolean debugEnabled);

    /**
     * Run get method
     *
     * @param tvmEmulator Pointer to TVM emulator
     * @param methodId    Integer method id
     * @param stackBoc    Base64 encoded BoC serialized stack (VmStack)
     * @return Json object with error:
     * {
     * "success": false,
     * "error": "Error description"
     * }
     * Or success:
     * {
     * "success": true
     * "vm_log": "...",
     * "vm_exit_code": 0,
     * "stack": "Base64 encoded BoC serialized stack (VmStack)",
     * "missing_library": null,
     * "gas_used": 1212
     * }
     */
    String tvm_emulator_run_get_method(long tvmEmulator, int methodId, String stackBoc);

    /**
     * Optimized version of "run get method" with all passed parameters in a single call
     *
     * @param len       Length of params_boc buffer
     * @param paramsBoc BoC serialized parameters, scheme: request$_ code:^Cell data:^Cell stack:^VmStack params:^[c7:^VmStack libs:^Cell] method_id:(## 32)
     * @param gasLimit  Gas limit
     * @return Char* with first 4 bytes defining length, and the rest BoC serialized result
     * Scheme: result$_ exit_code:(## 32) gas_used:(## 32) stack:^VmStack
     */
    String tvm_emulator_emulate_run_method(long len, String paramsBoc, long gasLimit);


    /**
     * Send external message
     *
     * @param tvmEmulator    Pointer to TVM emulator
     * @param messageBodyBoc Base64 encoded BoC serialized message body cell.
     * @return Json object with error:
     * {
     * "success": false,
     * "error": "Error description"
     * }
     * Or success:
     * {
     * "success": true,
     * "new_code": "Base64 boc decoded new code cell",
     * "new_data": "Base64 boc decoded new data cell",
     * "accepted": true,
     * "vm_exit_code": 0,
     * "vm_log": "...",
     * "missing_library": null,
     * "gas_used": 1212,
     * "actions": "Base64 boc decoded actions cell of type (OutList n)"
     * }
     */
    String tvm_emulator_send_external_message(long tvmEmulator, String messageBodyBoc);

    /**
     * Send internal message
     *
     * @param tvmEmulator    Pointer to TVM emulator
     * @param messageBodyBoc Base64 encoded BoC serialized message body cell.
     * @param amount         Amount of nanograms attached with internal message.
     * @return Json object with error:
     * {
     * "success": false,
     * "error": "Error description"
     * }
     * Or success:
     * {
     * "success": true,
     * "new_code": "Base64 boc decoded new code cell",
     * "new_data": "Base64 boc decoded new data cell",
     * "accepted": true,
     * "vm_exit_code": 0,
     * "vm_log": "...",
     * "missing_library": null,
     * "gas_used": 1212,
     * "actions": "Base64 boc decoded actions cell of type (OutList n)"
     * }
     */
    String tvm_emulator_send_internal_message(long tvmEmulator, String messageBodyBoc, BigInteger amount);
}
