package org.ton.java.tonlib;

import com.jsoniter.output.JsonStream;
import com.sun.jna.Native;
import lombok.Builder;
import lombok.extern.java.Log;
import org.ton.java.tonlib.client.TonIO;
import org.ton.java.tonlib.jna.TonlibJsonI;
import org.ton.java.tonlib.queries.VerbosityLevelQuery;
import org.ton.java.tonlib.types.BlockIdExt;
import org.ton.java.tonlib.types.VerbosityLevel;
import org.ton.java.utils.Utils;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

@Log
@Builder
public class Tonlib {

    /**
     * If not specified then tonlib shared library must be located in:<br>
     * <ul>
     * <li><code>jna.library.path</code> User-customizable path</li>
     * <li><code>jna.platform.library.path</code> Platform-specific paths</li>
     * <li>On OSX, ~/Library/Frameworks, /Library/Frameworks, and /System/Library/Frameworks will be searched for a framework with a name corresponding to that requested. Absolute paths to frameworks are also accepted, either ending at the framework name (sans ".framework") or the full path to the framework shared library (e.g. CoreServices.framework/CoreServices).</li>
     * <li>Context class loader classpath. Deployed native libraries may be installed on the classpath under ${os-prefix}/LIBRARY_FILENAME, where ${os-prefix} is the OS/Arch prefix returned by Platform.getNativeLibraryResourcePrefix(). If bundled in a jar file, the resource will be extracted to jna.tmpdir for loading, and later removed.</li>
     * </ul>
     * <br>
     * Java Tonlib looking for following filenames in above locations:<br>
     * <ul>
     *     <li>tonlibjson.so and tonlibjson-arm.so</li>
     *     <li>tonlibjson.dll and tonlibjson-arm.dll</li>
     *     <li>tonlibjson.dylib and tonlibjson-arm.dylib</li>
     *  <ul>
     */
    private String pathToTonlibSharedLib;
    /**
     * if not specified then integrated global-config.json is used;
     * If not specified and testnet=true then integrated testnet-global.config.json is used;
     */
    private String pathToGlobalConfig;
    /**
     * Valid values are:<br>
     * 0 - FATAL<br>
     * 1 - ERROR<br>
     * 2 - WARNING<br>
     * 3 - INFO<br>
     * 4 - DEBUG<br>
     */
    private VerbosityLevel verbosityLevel;
    private boolean testnet;
    private boolean keystoreInMemory;
    private String keystorePath;
    private String configData;
    /**
     * Default value 3
     */
    private int receiveRetryTimes;
    /**
     * In seconds. Default value 10.0 seconds
     */
    private double receiveTimeout;
    private TonlibJsonI tonlibJson;
    private long tonlib;
    private boolean synced;
    private boolean crashedDuringInit;

    public static TonlibBuilder builder() {
        return new CustomTonlibBuilder();
    }

    private static class CustomTonlibBuilder extends TonlibBuilder {
        @Override
        public Tonlib build() {
            try {
                String tonlibName = Utils.getTonLibName();

                if (isNull(super.pathToTonlibSharedLib)) {
                    super.pathToTonlibSharedLib = tonlibName;
                }

                if (isNull(super.verbosityLevel)) {
                    super.verbosityLevel = VerbosityLevel.FATAL;
                }

                if (isNull(super.keystorePath)) {
                    super.keystorePath = ".";
                }

                if (super.receiveRetryTimes == 0) {
                    super.receiveRetryTimes = 3;
                }

                if (super.receiveTimeout == 0) {
                    super.receiveTimeout = 10.0;
                }

                super.synced = false;
                super.crashedDuringInit = false;

                String configData = null;
                if (isNull(super.pathToGlobalConfig) && isNull(super.configData)) {
                    InputStream config;
                    if (super.testnet) {
                        super.pathToGlobalConfig = "testnet-global.config.json (integrated resource)";
                        config = Tonlib.class.getClassLoader().getResourceAsStream("testnet-global.config.json");
                    } else {
                        super.pathToGlobalConfig = "global-config.json (integrated resource)";
                        config = Tonlib.class.getClassLoader().getResourceAsStream("global-config.json");
                    }
                    configData = Utils.streamToString(config);

                    if (nonNull(config)) {
                        config.close();
                    }
                } else if (nonNull(super.pathToGlobalConfig) && isNull(super.configData)) {
                    if (Files.exists(Paths.get(super.pathToGlobalConfig))) {
                        configData = new String(Files.readAllBytes(Paths.get(super.pathToGlobalConfig)));
                    } else {
                        throw new RuntimeException("Global config is not found in path: " + super.pathToGlobalConfig);
                    }
                } else if (isNull(super.pathToGlobalConfig)) {
                    configData = super.configData;
                }
                super.configData = configData;

                System.out.printf("Java Tonlib configuration:\n" +
                                "Location: %s\n" +
                                "Verbosity level: %s (%s)\n" +
                                "Keystore in memory: %s\n" +
                                "Keystore path: %s\nPath to global config: %s\n" +
                                "Testnet: %s\n" +
                                "Receive timeout: %s seconds\n" +
                                "Receive retry times: %s%n\n",
                        "Raw configuration: %s%n",
                        super.pathToTonlibSharedLib, super.verbosityLevel, super.verbosityLevel.ordinal(),
                        super.keystoreInMemory, super.keystorePath, super.pathToGlobalConfig,
                        super.testnet, super.receiveTimeout, super.receiveRetryTimes, super.configData);
            } catch (Exception e) {
                throw new RuntimeException("Error creating tonlib instance: " + e.getMessage());
            }
            return super.build();
        }
    }

    public void destroy() {
        tonlibJson.tonlib_client_json_destroy(tonlib);
    }

    private void initTonlib() {
        TonIO tonIO = new TonIO(pathToTonlibSharedLib, configData);
    }

}
