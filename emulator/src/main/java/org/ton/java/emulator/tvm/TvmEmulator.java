package org.ton.java.emulator.tvm;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.ToNumberPolicy;
import com.sun.jna.Native;
import java.math.BigInteger;
import java.util.Collections;
import java.util.List;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.CellSlice;
import org.ton.java.cell.TonHashMapE;
import org.ton.java.tlb.*;
import org.ton.java.utils.Utils;

@Slf4j
@Builder
@Getter
public class TvmEmulator {

  /**
   * If not specified then tries to find emulator in system folder, more info <a
   * href="https://github.com/ton-blockchain/packages">here</a>
   */
  public String pathToEmulatorSharedLib;

  private final TvmEmulatorI tvmEmulatorI;
  private final long tvmEmulator;

  private String codeBoc;
  private String dataBoc;
  private TvmVerbosityLevel verbosityLevel;
  private Boolean printEmulatorInfo;
  private List<Cell> libraries;
  private String extraCurrencies;

  public static class TvmEmulatorBuilder {}

  public static TvmEmulatorBuilder builder() {
    return new CustomTvmEmulatorBuilder();
  }

  private static class CustomTvmEmulatorBuilder extends TvmEmulatorBuilder {
    @Override
    public TvmEmulator build() {

      if (isNull(super.pathToEmulatorSharedLib)) {
        if ((Utils.getOS() == Utils.OS.WINDOWS) || (Utils.getOS() == Utils.OS.WINDOWS_ARM)) {
          super.pathToEmulatorSharedLib = Utils.detectAbsolutePath("emulator", true);
        } else {
          super.pathToEmulatorSharedLib = Utils.detectAbsolutePath("libemulator", true);
        }
      } else {
        super.pathToEmulatorSharedLib = Utils.getLocalOrDownload(super.pathToEmulatorSharedLib);
      }

      if (isNull(super.printEmulatorInfo)) {
        super.printEmulatorInfo = true;
      }

      super.tvmEmulatorI = Native.load(super.pathToEmulatorSharedLib, TvmEmulatorI.class);
      if (isNull(super.verbosityLevel)) {
        super.verbosityLevel = TvmVerbosityLevel.TRUNCATED;
      }

      Utils.disableNativeOutput(super.verbosityLevel.ordinal());

      if (isNull(super.codeBoc)) {
        throw new Error("codeBoc is not set");
      }
      if (isNull(super.dataBoc)) {
        throw new Error("dataBoc is not set");
      }
      super.tvmEmulator =
          super.tvmEmulatorI.tvm_emulator_create(
              super.codeBoc, super.dataBoc, super.verbosityLevel.ordinal());

      if (super.verbosityLevel.ordinal() > TvmVerbosityLevel.UNLIMITED.ordinal()) {
        super.tvmEmulatorI.tvm_emulator_set_debug_enabled(super.tvmEmulator, true);
      }

      if (nonNull(super.libraries)) {
        super.tvmEmulatorI.tvm_emulator_set_libraries(
            super.tvmEmulator, convertLibsToHashMap(super.libraries).toBase64());
      }

      if (nonNull(super.extraCurrencies)) {
        super.tvmEmulatorI.tvm_emulator_set_extra_currencies(
            super.tvmEmulator, super.extraCurrencies);
      }

      Utils.enableNativeOutput(super.verbosityLevel.ordinal());

      if (super.tvmEmulator == 0) {
        throw new Error("Can't create emulator instance");
      }

      if (super.printEmulatorInfo) {
        log.info(
            "\nTON TVM Emulator configuration:\n" + "Location: {}\n" + "Verbosity level: {}\n",
            super.pathToEmulatorSharedLib,
            super.verbosityLevel);
      }
      return super.build();
    }
  }

  public void destroy() {
    Utils.disableNativeOutput(verbosityLevel.ordinal());
    tvmEmulatorI.tvm_emulator_destroy(tvmEmulator);
    Utils.enableNativeOutput(verbosityLevel.ordinal());
  }

  /**
   * Set libs for emulation
   *
   * @param libsBoc Base64 encoded BoC serialized shared libraries dictionary (HashmapE 256 ^Cell).
   * @return true in case of success, false in case of error
   */
  public boolean setLibs(String libsBoc) {
    Utils.disableNativeOutput(verbosityLevel.ordinal());
    boolean result = tvmEmulatorI.tvm_emulator_set_libraries(tvmEmulator, libsBoc);
    Utils.enableNativeOutput(verbosityLevel.ordinal());
    return result;
  }

  /**
   *
   *
   * <pre>
   * Prepares the c7 tuple (virtual machine context) for a compute phase of a transaction.
   * C7 tlb-scheme FYI:
   * smc_info#076ef1ea
   *   actions:uint16
   *   msgs_sent:uint16
   *   unixtime:uint32
   *   block_lt:uint64
   *   trans_lt:uint64
   *   rand_seed:bits256
   *   balance_remaining:CurrencyCollection
   *   myself:MsgAddressInt
   *   global_config:(Maybe Cell) = SmartContractInfo;
   * </pre>
   *
   * <p>Set c7 parameters
   *
   * @param address Address of smart contract
   * @param unixTime Unix timestamp
   * @param balance Smart contract balance
   * @param randSeedHex Random seed as hex string of length 64
   * @param config Base64 encoded BoC serialized Config dictionary (Hashmap 32 ^Cell). Optional.
   * @return true in case of success, false in case of error
   */
  public boolean setC7(
      String address, long unixTime, long balance, String randSeedHex, String config) {
    Utils.disableNativeOutput(verbosityLevel.ordinal());
    boolean result =
        tvmEmulatorI.tvm_emulator_set_c7(
            tvmEmulator, address, unixTime, balance, randSeedHex, config);
    Utils.enableNativeOutput(verbosityLevel.ordinal());
    return result;
  }

  /**
   * Set tuple of previous blocks (13th element of c7)
   *
   * @param infoBoc Base64 encoded BoC serialized TVM tuple (VmStackValue).
   * @return true in case of success, false in case of error
   */
  public boolean setPrevBlockInfo(String infoBoc) {
    Utils.disableNativeOutput(verbosityLevel.ordinal());
    boolean result = tvmEmulatorI.tvm_emulator_set_prev_blocks_info(tvmEmulator, infoBoc);
    Utils.enableNativeOutput(verbosityLevel.ordinal());
    return result;
  }

  /**
   * Set TVM gas limit
   *
   * @param gasLimit Gas limit
   * @return true in case of success, false in case of error
   */
  public boolean setGasLimit(long gasLimit) {
    Utils.disableNativeOutput(verbosityLevel.ordinal());
    boolean result = tvmEmulatorI.tvm_emulator_set_gas_limit(tvmEmulator, gasLimit);
    Utils.enableNativeOutput(verbosityLevel.ordinal());
    return result;
  }

  /**
   * Enable or disable TVM debug primitives
   *
   * @param debugEnabled Whether debug primitives should be enabled or not
   * @return true in case of success, false in case of error
   */
  public boolean setDebugEnabled(boolean debugEnabled) {
    Utils.disableNativeOutput(verbosityLevel.ordinal());
    boolean result = tvmEmulatorI.tvm_emulator_set_debug_enabled(tvmEmulator, debugEnabled);
    Utils.enableNativeOutput(verbosityLevel.ordinal());
    return result;
  }

  /**
   * Run get method
   *
   * @param methodId Integer method id
   * @param stackBoc Base64 encoded BoC serialized stack (VmStack)
   * @return Json object with error: { "success": false, "error": "Error description" } Or success:
   *     { "success": true "vm_log": "...", "vm_exit_code": 0, "stack": "Base64 encoded BoC
   *     serialized stack (VmStack)", "missing_library": null, "gas_used": 1212 }
   */
  public GetMethodResult runGetMethod(int methodId, String stackBoc) {
    Utils.disableNativeOutput(verbosityLevel.ordinal());
    String result = tvmEmulatorI.tvm_emulator_run_get_method(tvmEmulator, methodId, stackBoc);
    Utils.enableNativeOutput(verbosityLevel.ordinal());
    Gson gson = new GsonBuilder().setObjectToNumberStrategy(ToNumberPolicy.BIG_DECIMAL).create();
    return gson.fromJson(result, GetMethodResult.class);
  }

  /**
   * Run get method with empty input stack
   *
   * @param methodId Integer method id
   * @return Json object with error: { "success": false, "error": "Error description" } Or success:
   *     { "success": true "vm_log": "...", "vm_exit_code": 0, "stack": "Base64 encoded BoC
   *     serialized stack (VmStack)", "missing_library": null, "gas_used": 1212 }
   */
  public GetMethodResult runGetMethod(int methodId) {
    Utils.disableNativeOutput(verbosityLevel.ordinal());
    String result =
        tvmEmulatorI.tvm_emulator_run_get_method(
            tvmEmulator,
            methodId,
            VmStack.builder()
                .depth(0)
                .stack(VmStackList.builder().tos(Collections.emptyList()).build())
                .build()
                .toCell()
                .toBase64());
    Utils.enableNativeOutput(verbosityLevel.ordinal());
    Gson gson = new GsonBuilder().setObjectToNumberStrategy(ToNumberPolicy.BIG_DECIMAL).create();
    return gson.fromJson(result, GetMethodResult.class);
  }

  public GetMethodResult runGetMethod(String methodName) {
    Utils.disableNativeOutput(verbosityLevel.ordinal());
    String result =
        tvmEmulatorI.tvm_emulator_run_get_method(
            tvmEmulator,
            Utils.calculateMethodId(methodName),
            VmStack.builder()
                .depth(0)
                .stack(VmStackList.builder().tos(Collections.emptyList()).build())
                .build()
                .toCell()
                .toBase64());
    Utils.enableNativeOutput(verbosityLevel.ordinal());
    Gson gson = new GsonBuilder().setObjectToNumberStrategy(ToNumberPolicy.BIG_DECIMAL).create();
    return gson.fromJson(result, GetMethodResult.class);
  }

  /**
   * Run get method
   *
   * @param methodName String method id
   * @param stackBoc Base64 encoded BoC serialized stack (VmStack)
   * @return Json object with error: { "success": false, "error": "Error description" } Or success:
   *     { "success": true "vm_log": "...", "vm_exit_code": 0, "stack": "Base64 encoded BoC
   *     serialized stack (VmStack)", "missing_library": null, "gas_used": 1212 }
   */
  public GetMethodResult runGetMethod(String methodName, String stackBoc) {
    Utils.disableNativeOutput(verbosityLevel.ordinal());
    String result =
        tvmEmulatorI.tvm_emulator_run_get_method(
            tvmEmulator, Utils.calculateMethodId(methodName), stackBoc);
    Utils.enableNativeOutput(verbosityLevel.ordinal());
    Gson gson = new GsonBuilder().setObjectToNumberStrategy(ToNumberPolicy.BIG_DECIMAL).create();
    return gson.fromJson(result, GetMethodResult.class);
  }

  public BigInteger runGetSeqNo() {
    GetMethodResult methodResult = runGetMethod(Utils.calculateMethodId("seqno"));
    if (methodResult.getVm_exit_code() != 0) {
      throw new Error("Cannot execute run method (seqno), Error:\n" + methodResult.getVm_log());
    }
    VmStack stack = methodResult.getStack();
    VmStackList vmStackList = stack.getStack();
    return VmStackValueTinyInt.deserialize(
            CellSlice.beginParse(vmStackList.getTos().get(0).toCell()))
        .getValue();
  }

  public BigInteger runGetMethodWithIntParam(String methodName, int param1) {
    GetMethodResult methodResult = runGetMethod(Utils.calculateMethodId(methodName));
    if (methodResult.getVm_exit_code() != 0) {
      throw new Error(
          "Cannot execute run method (" + methodName + "), Error:\n" + methodResult.getVm_log());
    }
    VmStack stack = methodResult.getStack();
    VmStackList vmStackList = stack.getStack();
    return VmStackValueTinyInt.deserialize(
            CellSlice.beginParse(vmStackList.getTos().get(0).toCell()))
        .getValue();
  }

  public BigInteger runGetSubWalletId() {
    GetMethodResult methodResult = runGetMethod(Utils.calculateMethodId("get_subwallet_id"));
    if (methodResult.getVm_exit_code() != 0) {
      throw new Error(
          "Cannot execute run method (get_subwallet_id), Error:\n" + methodResult.getVm_log());
    }
    VmStack stack = methodResult.getStack();
    VmStackList vmStackList = stack.getStack();
    return VmStackValueTinyInt.deserialize(
            CellSlice.beginParse(vmStackList.getTos().get(0).toCell()))
        .getValue();
  }

  public String runGetPublicKey() {
    GetMethodResult methodResult = runGetMethod(Utils.calculateMethodId("get_public_key"));
    if (methodResult.getVm_exit_code() != 0) {
      throw new Error(
          "Cannot execute run method (get_public_key), Error:\n" + methodResult.getVm_log());
    }
    VmStack stack = methodResult.getStack();
    int depth = stack.getDepth();
    VmStackList vmStackList = stack.getStack();
    BigInteger pubKey =
        VmStackValueInt.deserialize(CellSlice.beginParse(vmStackList.getTos().get(0).toCell()))
            .getValue();
    return pubKey.toString(16);
  }

  /**
   * Optimized version of "run get method" with all passed parameters in a single call
   *
   * @param len Length of params_boc buffer
   * @param paramsBoc BoC serialized parameters, scheme: code:^Cell data:^Cell stack:^VmStack
   *     params:^[c7:^VmStack libs:^Cell] method_id:(## 32)
   * @param gasLimit Gas limit
   * @return String with first 4 bytes defining length, and the rest BoC serialized result Scheme:
   *     result$_ exit_code:(## 32) gas_used:(## 32) stack:^VmStack
   */
  public String emulateRunMethod(int len, String paramsBoc, long gasLimit) {
    Utils.disableNativeOutput(verbosityLevel.ordinal());
    String result = tvmEmulatorI.tvm_emulator_emulate_run_method(len, paramsBoc, gasLimit);
    Utils.enableNativeOutput(verbosityLevel.ordinal());
    return result;
  }

  /**
   * Send external message
   *
   * @param messageBodyBoc Base64 encoded BoC serialized message body cell.
   * @return Json object with error: { "success": false, "error": "Error description" } Or success:
   *     { "success": true, "new_code": "Base64 boc decoded new code cell", "new_data": "Base64 boc
   *     decoded new data cell", "accepted": true, "vm_exit_code": 0, "vm_log": "...",
   *     "missing_library": null, "gas_used": 1212, "actions": "Base64 boc decoded actions cell of
   *     type (OutList n)" }
   */
  public SendExternalMessageResult sendExternalMessage(String messageBodyBoc) {
    Utils.disableNativeOutput(verbosityLevel.ordinal());
    String result = tvmEmulatorI.tvm_emulator_send_external_message(tvmEmulator, messageBodyBoc);
    Utils.enableNativeOutput(verbosityLevel.ordinal());
    Gson gson = new GsonBuilder().setObjectToNumberStrategy(ToNumberPolicy.BIG_DECIMAL).create();
    return gson.fromJson(result, SendExternalMessageResult.class);
  }

  /**
   * Send internal message
   *
   * @param messageBodyBoc Base64 encoded BoC serialized message body cell.
   * @param amount Amount of nanograms attached with internal message.
   * @return Json object with error: { "success": false, "error": "Error description" } Or success:
   *     { "success": true, "new_code": "Base64 boc decoded new code cell", "new_data": "Base64 boc
   *     decoded new data cell", "accepted": true, "vm_exit_code": 0, "vm_log": "...",
   *     "missing_library": null, "gas_used": 1212, "actions": "Base64 boc decoded actions cell of
   *     type (OutList n)" }
   */
  public SendInternalMessageResult sendInternalMessage(String messageBodyBoc, long amount) {
    Utils.disableNativeOutput(verbosityLevel.ordinal());
    String result =
        tvmEmulatorI.tvm_emulator_send_internal_message(tvmEmulator, messageBodyBoc, amount);
    Utils.enableNativeOutput(verbosityLevel.ordinal());
    Gson gson = new GsonBuilder().setObjectToNumberStrategy(ToNumberPolicy.BIG_DECIMAL).create();
    return gson.fromJson(result, SendInternalMessageResult.class);
  }

  /**
   * @param extraCurrencies in format currency_id1=balance1 currency_id2=balance2
   * @return true in case of success, false in case of error
   */
  public boolean setExtraCurrencies(String extraCurrencies) {
    Utils.disableNativeOutput(verbosityLevel.ordinal());
    boolean result = tvmEmulatorI.tvm_emulator_set_extra_currencies(tvmEmulator, extraCurrencies);
    Utils.enableNativeOutput(verbosityLevel.ordinal());
    return result;
  }

  private static Cell convertLibsToHashMap(List<Cell> libs) {

    TonHashMapE x = new TonHashMapE(256);

    for (Cell c : libs) {
      x.elements.put(c.getHash(), c);
    }
    return x.serialize(
        k -> CellBuilder.beginCell().storeBytes((byte[]) k, 256).endCell().getBits(),
        v -> CellBuilder.beginCell().storeRef(((Cell) v)).endCell());
  }
}
