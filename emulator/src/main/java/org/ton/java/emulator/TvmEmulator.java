package org.ton.java.emulator;

import com.sun.jna.Native;
import lombok.Builder;
import lombok.extern.java.Log;
import org.ton.java.utils.Utils;

import static java.util.Objects.isNull;

@Log
@Builder
public class TvmEmulator {

    /**
     * If not specified then emulator shared library must be located in:<br>
     * <ul>
     * <li><code>jna.library.path</code> User-customizable path</li>
     * <li><code>jna.platform.library.path</code> Platform-specific paths</li>
     * <li>On OSX, ~/Library/Frameworks, /Library/Frameworks, and /System/Library/Frameworks will be searched for a framework with a name corresponding to that requested. Absolute paths to frameworks are also accepted, either ending at the framework name (sans ".framework") or the full path to the framework shared library (e.g. CoreServices.framework/CoreServices).</li>
     * <li>Context class loader classpath. Deployed native libraries may be installed on the classpath under ${os-prefix}/LIBRARY_FILENAME, where ${os-prefix} is the OS/Arch prefix returned by Platform.getNativeLibraryResourcePrefix(). If bundled in a jar file, the resource will be extracted to jna.tmpdir for loading, and later removed.</li>
     * </ul>
     * <br>
     * Java Tonlib looking for following filenames in above locations:<br>
     * <ul>
     *     <li>libemulator-linux-x86-64.so and libemulator-linux-arm64.so</li>
     *     <li>emulator.dll and emulator-arm.dll</li>
     *     <li>libemulator-mac-x86-64.dylib and libemulator-mac-arm64.dylib</li>
     *  <ul>
     */
    private String pathToEmulatorSharedLib;
    private final TvmEmulatorI tvmEmulatorI;
    private final long tvmEmulator;

    private String codeBoc;
    private String dataBoc;
    private Integer verbosityLevel;

    public static class TvmEmulatorBuilder {
    }

    public static TvmEmulatorBuilder builder() {
        return new CustomTvmEmulatorBuilder();
    }

    private static class CustomTvmEmulatorBuilder extends TvmEmulatorBuilder {
        @Override
        public TvmEmulator build() {
            String emulatorName;
            Utils.OS os = Utils.getOS();
            switch (os) {
                case LINUX:
                    emulatorName = "libemulator-linux-x86-64.so";
                    break;
                case LINUX_ARM:
                    emulatorName = "libemulator-linux-arm64.so";
                    break;
                case WINDOWS:
                    emulatorName = "emulator.dll";
                    break;
                case WINDOWS_ARM:
                    emulatorName = "emulator-arm.dll";
                    break;
                case MAC:
                    emulatorName = "libemulator-mac-x86-64.dylib";
                    break;
                case MAC_ARM64:
                    emulatorName = "libemulator-mac-arm64.dylib";
                    break;
                case UNKNOWN:
                    throw new Error("Operating system is not supported!");
                default:
                    throw new IllegalArgumentException("Unknown operating system: " + os);
            }

            if (isNull(super.pathToEmulatorSharedLib)) {
                super.pathToEmulatorSharedLib = emulatorName;
            }

            super.tvmEmulatorI = Native.load(super.pathToEmulatorSharedLib, TvmEmulatorI.class);
            if (isNull(super.verbosityLevel)) {
                super.verbosityLevel = 3;
            }
            if (isNull(super.codeBoc)) {
                throw new Error("codeBoc is not set");
            }
            if (isNull(super.dataBoc)) {
                throw new Error("dataBoc is not set");
            }
            super.tvmEmulator = super.tvmEmulatorI.tvm_emulator_create(super.codeBoc, super.dataBoc, super.verbosityLevel);

            if (super.tvmEmulator == 0) {
                throw new Error("Can't create emulator instance");
            }

            System.out.printf("Java TON TVM Emulator configuration:\n" +
                            "Location: %s\n" +
                            "Verbosity level: %s\n",
                    super.pathToEmulatorSharedLib,
                    super.verbosityLevel);
            return super.build();
        }
    }

    public void destroy() {
        tvmEmulatorI.tvm_emulator_destroy(tvmEmulator);
    }

    /**
     * Set libs for emulation
     *
     * @param libsBoc Base64 encoded BoC serialized shared libraries dictionary (HashmapE 256 ^Cell).
     * @return true in case of success, false in case of error
     */
    public boolean setLibs(String libsBoc) {
        return tvmEmulatorI.tvm_emulator_set_libraries(tvmEmulator, libsBoc);
    }

    /**
     * C7 tlb-scheme:
     * <p>
     * smc_info#076ef1ea
     * actions:uint16
     * msgs_sent:uint16
     * unixtime:uint32
     * block_lt:uint64
     * trans_lt:uint64
     * rand_seed:bits256
     * balance_remaining:CurrencyCollection
     * myself:MsgAddressInt
     * global_config:(Maybe Cell) = SmartContractInfo;
     * <p>
     * Set c7 parameters
     *
     * @param address     Address of smart contract
     * @param unixTime    Unix timestamp
     * @param balance     Smart contract balance
     * @param randSeedHex Random seed as hex string of length 64
     * @param config      Base64 encoded BoC serialized Config dictionary (Hashmap 32 ^Cell). Optional.
     * @return true in case of success, false in case of error
     */
    public boolean setC7(String address, long unixTime, long balance, String randSeedHex, String config) {
        return tvmEmulatorI.tvm_emulator_set_c7(tvmEmulator, address, unixTime, balance, randSeedHex, config);
    }

    /**
     * Set tuple of previous blocks (13th element of c7)
     *
     * @param infoBoc Base64 encoded BoC serialized TVM tuple (VmStackValue).
     * @return true in case of success, false in case of error
     */
    public boolean setPrevBlockInfo(String infoBoc) {
        return tvmEmulatorI.tvm_emulator_set_prev_blocks_info(tvmEmulator, infoBoc);
    }

    /**
     * Set TVM gas limit
     *
     * @param gasLimit Gas limit
     * @return true in case of success, false in case of error
     */
    public boolean setGasLimit(long gasLimit) {
        return tvmEmulatorI.tvm_emulator_set_gas_limit(tvmEmulator, gasLimit);
    }

    /**
     * Enable or disable TVM debug primitives
     *
     * @param debugEnabled Whether debug primitives should be enabled or not
     * @return true in case of success, false in case of error
     */
    public boolean setDebugEnabled(boolean debugEnabled) {
        return tvmEmulatorI.tvm_emulator_set_debug_enabled(tvmEmulator, debugEnabled);
    }


    /**
     * Run get method
     *
     * @param methodId Integer method id
     * @param stackBoc Base64 encoded BoC serialized stack (VmStack)
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
    public String runGetMethod(int methodId, String stackBoc) {
        return tvmEmulatorI.tvm_emulator_run_get_method(tvmEmulator, methodId, stackBoc);
    }

    /**
     * Optimized version of "run get method" with all passed parameters in a single call
     *
     * @param len       Length of params_boc buffer
     * @param paramsBoc BoC serialized parameters, scheme:
     *                  request$_
     *                  code:^Cell data:^Cell stack:^VmStack params:^[c7:^VmStack libs:^Cell]
     *                  method_id:(## 32)
     * @param gasLimit  Gas limit
     * @return String with first 4 bytes defining length, and the rest BoC serialized result
     * Scheme: result$_ exit_code:(## 32) gas_used:(## 32) stack:^VmStack
     */
    public String emulateRunMethod(int len, String paramsBoc, long gasLimit) {
        return tvmEmulatorI.tvm_emulator_emulate_run_method(len, paramsBoc, gasLimit);
    }

    /**
     * Send external message
     *
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
    public String sendExternalMessage(String messageBodyBoc) {
        return tvmEmulatorI.tvm_emulator_send_external_message(tvmEmulator, messageBodyBoc);
    }

    /**
     * Send internal message
     *
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
    public String sendInternalMessage(String messageBodyBoc, long amount) {
        return tvmEmulatorI.tvm_emulator_send_internal_message(tvmEmulator, messageBodyBoc, amount);
    }

}


