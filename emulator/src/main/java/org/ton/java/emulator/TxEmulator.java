package org.ton.java.emulator;

import com.sun.jna.Native;
import lombok.Builder;
import lombok.extern.java.Log;
import org.ton.java.utils.Utils;

import static java.util.Objects.isNull;

@Log
@Builder
public class TxEmulator {

    private String pathToEmulatorSharedLib;
    private final TxEmulatorI txEmulatorI;
    private final long txEmulator;

    private String configBoc;
    private int verbosityLevel;

    public static TxEmulatorBuilder builder() {
        return new CustomEmulatorBuilder();
    }

    private static class CustomEmulatorBuilder extends TxEmulatorBuilder {
        @Override
        public TxEmulator build() {
            String emulatorName = switch (Utils.getOS()) {
                case LINUX -> "libemulator-linux-x86-64.so";
                case LINUX_ARM -> "libemulator-linux-arm64.so";
                case WINDOWS -> "emulator.dll";
                case WINDOWS_ARM -> "emulator-arm.dll";
                case MAC -> "libemulator-mac-x86-64.dylib";
                case MAC_ARM64 -> "libemulator-mac-arm64.dylib";
                case UNKNOWN -> throw new Error("Operating system is not supported!");
            };

            if (isNull(super.pathToEmulatorSharedLib)) {
                super.pathToEmulatorSharedLib = emulatorName;
            }

            super.txEmulatorI = Native.load(super.pathToEmulatorSharedLib, TxEmulatorI.class);
            if (isNull(super.verbosityLevel)) {
                super.verbosityLevel = 2;
            }
            if (isNull(super.configBoc)) {
                throw new Error("Config is not set");
            }
            super.txEmulator = super.txEmulatorI.transaction_emulator_create(super.configBoc, super.verbosityLevel);

            if (super.txEmulator == 0) {
                throw new Error("Can't create emulator instance");
            }

            System.out.printf("""
                            Java TON Emulator configuration:
                            Location: %s
                            Verbosity level: %s""",
                    super.pathToEmulatorSharedLib,
                    super.verbosityLevel);
            return super.build();
        }
    }

    public void destroy() {
        txEmulatorI.transaction_emulator_destroy(txEmulator);
    }

    public void setVerbosityLevel(int verbosityLevel) {
        txEmulatorI.emulator_set_verbosity_level(txEmulator, verbosityLevel);
    }
}


