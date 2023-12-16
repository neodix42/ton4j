package org.ton.java.tonlib;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.java.Log;
import org.ton.java.tonlib.client.TonIO;
import org.ton.java.tonlib.jna.TonlibJsonI;
import org.ton.java.tonlib.types.VerbosityLevel;
import org.ton.java.utils.Utils;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

@Log
@Getter
@Setter
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
    private TonIO tonIO;


    public void initTonlib() {
        try {
            String tonlibName = Utils.getTonLibName();

            if (isNull(this.pathToTonlibSharedLib)) {
                this.pathToTonlibSharedLib = tonlibName;
            }

            if (isNull(this.verbosityLevel)) {
                this.verbosityLevel = VerbosityLevel.FATAL;
            }

            if (isNull(this.keystorePath)) {
                this.keystorePath = ".";
            }

            if (this.receiveRetryTimes == 0) {
                this.receiveRetryTimes = 3;
            }

            if (this.receiveTimeout == 0) {
                this.receiveTimeout = 10.0;
            }

            String configData = null;
            if (isNull(this.pathToGlobalConfig) && isNull(this.configData)) {
                InputStream config;
                if (this.testnet) {
                    this.pathToGlobalConfig = "testnet-global.config.json (integrated resource)";
                    config = Tonlib.class.getClassLoader().getResourceAsStream("testnet-global.config.json");
                } else {
                    this.pathToGlobalConfig = "global-config.json (integrated resource)";
                    config = Tonlib.class.getClassLoader().getResourceAsStream("global-config.json");
                }
                configData = Utils.streamToString(config);

                if (nonNull(config)) {
                    config.close();
                }
            } else if (nonNull(this.pathToGlobalConfig) && isNull(this.configData)) {
                if (Files.exists(Paths.get(this.pathToGlobalConfig))) {
                    configData = new String(Files.readAllBytes(Paths.get(this.pathToGlobalConfig)));
                } else {
                    throw new RuntimeException("Global config is not found in path: " + this.pathToGlobalConfig);
                }
            } else if (isNull(this.pathToGlobalConfig)) {
                configData = this.configData;
            }
            this.configData = configData;

            System.out.printf("Java Tonlib configuration:\n" +
                            "Location: %s\n" +
                            "Verbosity level: %s (%s)\n" +
                            "Keystore in memory: %s\n" +
                            "Keystore path: %s\nPath to global config: %s\n" +
                            "Testnet: %s\n" +
                            "Receive timeout: %s seconds\n" +
                            "Receive retry times: %s%n\n",
                    "Raw configuration: %s%n",
                    this.pathToTonlibSharedLib, this.verbosityLevel, this.verbosityLevel.ordinal(),
                    this.keystoreInMemory, this.keystorePath, this.pathToGlobalConfig,
                    this.testnet, this.receiveTimeout, this.receiveRetryTimes, this.configData);
        } catch (Exception e) {
            throw new RuntimeException("Error creating tonlib instance: " + e.getMessage());
        }
        tonIO = new TonIO(pathToTonlibSharedLib, configData);
    }

}
