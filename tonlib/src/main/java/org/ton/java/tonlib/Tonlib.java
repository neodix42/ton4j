package org.ton.java.tonlib;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.ToNumberPolicy;
import com.sun.jna.*;
import java.io.File;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.TimeUnit;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.ton.java.address.Address;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.CellSlice;
import org.ton.java.tlb.*;
import org.ton.java.tonlib.queries.*;
import org.ton.java.tonlib.types.*;
import org.ton.java.tonlib.types.BlockIdExt;
import org.ton.java.tonlib.types.globalconfig.*;
import org.ton.java.utils.Utils;

@Slf4j
@Builder
public class Tonlib {

  /**
   * If not specified then tries to find tonlib in system folder, more info <a
   * href="https://github.com/ton-blockchain/packages">here</a>
   */
  public String pathToTonlibSharedLib;

  /**
   * if not specified and globalConfigAsString is null then integrated global-config.json is used;
   *
   * <p>if not specified and globalConfigAsString is filled then globalConfigAsString is used;
   *
   * <p>If not specified and testnet=true then integrated testnet-global.config.json is used;
   */
  public String pathToGlobalConfig;

  /**
   * if not specified and pathToGlobalConfig is null then integrated global-config.json is used;
   *
   * <p>if not specified and pathToGlobalConfig is filled then pathToGlobalConfig is used;
   */
  private String globalConfigAsString;

  private TonGlobalConfig globalConfigAsObject;

  /**
   *
   *
   * <pre>
   * Valid values are:
   * 0 - FATAL
   * 1 - ERROR
   * 2 - WARNING
   * 3 - INFO
   * 4 - DEBUG
   * </pre>
   */
  private VerbosityLevel verbosityLevel;

  private Boolean ignoreCache;

  /** Ignored if pathToGlobalConfig is not null. */
  private boolean testnet;

  private boolean keystoreInMemory;

  private String keystorePath;

  private Integer liteServerIndex;
  private Boolean usingAllLiteServers;

  /** Do not use! Reserved for internal usage. */
  public static TonGlobalConfig originalGlobalConfigInternal;

  /** Do not use! Reserved for internal usage. */
  public static String originalGlobalConfigStr;

  /** Default value 5 */
  private int receiveRetryTimes;

  /** In seconds. Default value 10.0 seconds */
  private double receiveTimeout;

  private static TonlibJsonI tonlibJson;

  private Boolean printInfo;

  private static final Gson gson =
      new GsonBuilder().setObjectToNumberStrategy(ToNumberPolicy.BIG_DECIMAL).create();

  private static Pointer tonlib;

  public static class TonlibBuilder {}

  public static TonlibBuilder builder() {
    return new CustomTonlibBuilder();
  }

  private static class CustomTonlibBuilder extends TonlibBuilder {
    @Override
    public Tonlib build() {

      try {

        if (isNull(super.printInfo)) {
          super.printInfo = true;
        }

        if (isNull(super.pathToTonlibSharedLib)) {
          if ((Utils.getOS() == Utils.OS.WINDOWS) || (Utils.getOS() == Utils.OS.WINDOWS_ARM)) {
            super.pathToTonlibSharedLib = Utils.detectAbsolutePath("tonlibjson", true);
          } else {
            super.pathToTonlibSharedLib = Utils.detectAbsolutePath("libtonlibjson", true);
          }

          if ((nonNull(super.pathToTonlibSharedLib)
                  && (super.pathToTonlibSharedLib.contains("INFO:")))
              || isNull(super.pathToTonlibSharedLib)) {
            throw new Error(
                "tonlibjson shared library not found.\nYou can specify full path via Tonlib.builder().pathToTonlibSharedLib(Utils.getTonlibGithubUrl()).");
          }
        } else {
          super.pathToTonlibSharedLib = Utils.getLocalOrDownload(super.pathToTonlibSharedLib);
        }

        if (isNull(super.verbosityLevel)) {
          super.verbosityLevel = VerbosityLevel.FATAL;
        }

        if (isNull(super.keystorePath)) {
          super.keystorePath = ".";
        }

        if (isNull(super.liteServerIndex)) {
          super.liteServerIndex = -1;
        }

        super.keystorePath = super.keystorePath.replace("\\", "/");

        if (super.receiveRetryTimes == 0) {
          super.receiveRetryTimes = 5;
        }

        if (super.receiveTimeout == 0) {
          super.receiveTimeout = 10.0;
        }

        if (isNull(super.ignoreCache)) {
          super.ignoreCache = false;
        }

        if (nonNull(super.globalConfigAsString)) {
          originalGlobalConfigStr = super.globalConfigAsString;
        } else {
          if (isNull(super.globalConfigAsObject)) {
            super.pathToGlobalConfig = resolvePathToGlobalConfig();
            originalGlobalConfigStr =
                FileUtils.readFileToString(
                    new File(super.pathToGlobalConfig), StandardCharsets.UTF_8);
          }
        }

        TonGlobalConfig globalConfigCurrent =
            nonNull(super.globalConfigAsObject)
                ? super.globalConfigAsObject
                : gson.fromJson(originalGlobalConfigStr, TonGlobalConfig.class);

        originalGlobalConfigInternal =
            nonNull(super.globalConfigAsObject)
                ? super.globalConfigAsObject
                : gson.fromJson(originalGlobalConfigStr, TonGlobalConfig.class);

        if (super.liteServerIndex != -1) {
          super.usingAllLiteServers = false;
          if (super.liteServerIndex > globalConfigCurrent.getLiteservers().length - 1) {
            throw new RuntimeException(
                "Specified lite-server index is greater than total number of lite-servers in config.");
          }
        } else {
          super.liteServerIndex = 0;
          super.usingAllLiteServers = true;
        }

        // always construct global config with one lite-server
        // pick the first one if user hasn't specified any specific lite-server
        // in case of error, the second lite-server from the original list of lite-servers will be
        // picked
        LiteServers[] liteServers = originalGlobalConfigInternal.getLiteservers();
        LiteServers[] newLiteServers = new LiteServers[1];
        newLiteServers[0] = liteServers[super.liteServerIndex];
        globalConfigCurrent.setLiteservers(newLiteServers);

        //        TonlibJsonI rawLibrary = Native.load(super.pathToTonlibSharedLib,
        // TonlibJsonI.class);
        //        super.tonlibJson = (TonlibJsonI) Native.synchronizedLibrary(rawLibrary);
        tonlibJson = Native.load(super.pathToTonlibSharedLib, TonlibJsonI.class);
        Utils.disableNativeOutput(super.verbosityLevel.ordinal());
        tonlib = tonlibJson.tonlib_client_json_create();
        tonlibJson.tonlib_client_set_verbosity_level(super.verbosityLevel.ordinal());
        Utils.enableNativeOutput(super.verbosityLevel.ordinal());

        if (super.printInfo) {
          log.info(
              String.format(
                  "Java Tonlib configuration:\n"
                      + "Location: %s\n"
                      + "Verbosity level: %s (%s)\n"
                      + "Keystore in memory: %s\n"
                      + "Keystore path: %s\n"
                      + "Path to global config: %s\n"
                      + "Global config as string: %s\n"
                      + "lite-servers found: %s\n"
                      + "dht-nodes found: %s\n"
                      + "init-block seqno: %s\n"
                      + "%s\n"
                      + "Ignore cache: %s\n"
                      + "Testnet: %s\n"
                      + "Receive timeout: %s seconds\n"
                      + "Receive retry times: %s%n",
                  super.pathToTonlibSharedLib,
                  super.verbosityLevel,
                  super.verbosityLevel.ordinal(),
                  super.keystoreInMemory,
                  super.keystorePath,
                  super.pathToGlobalConfig,
                  (nonNull(super.globalConfigAsString) && super.globalConfigAsString.length() > 33)
                      ? super.globalConfigAsString.substring(0, 33)
                      : "",
                  originalGlobalConfigInternal.getLiteservers().length,
                  globalConfigCurrent.getDht().getStatic_nodes().getNodes().length,
                  globalConfigCurrent.getValidator().getInit_block().getSeqno(),
                  (super.usingAllLiteServers)
                      ? "using lite-servers: all"
                      : "using lite-server at index: "
                          + super.liteServerIndex
                          + " ("
                          + Utils.int2ip(globalConfigCurrent.getLiteservers()[0].getIp())
                          + ")",
                  super.ignoreCache,
                  super.testnet,
                  super.receiveTimeout,
                  super.receiveRetryTimes));
        }

        initTonlibConfig(globalConfigCurrent);

        if (super.usingAllLiteServers) {
          if (super.printInfo) {
            log.info(
                "Using lite-server at index: "
                    + (super.liteServerIndex)
                    + " ("
                    + Utils.int2ip(globalConfigCurrent.getLiteservers()[0].getIp())
                    + ")");
          }
        }

      } catch (Throwable e) {
        e.printStackTrace();
        throw new RuntimeException("Error creating tonlib instance: " + e.getMessage());
      }
      return super.build();
    }

    private String resolvePathToGlobalConfig() {
      String globalConfigPath;

      if (isNull(super.pathToGlobalConfig)) {
        if (super.testnet) {
          globalConfigPath = Utils.getLocalOrDownload(Utils.getGlobalConfigUrlTestnet());
        } else {
          globalConfigPath = Utils.getLocalOrDownload(Utils.getGlobalConfigUrlMainnet());
        }
      } else {
        globalConfigPath = Utils.getLocalOrDownload(super.pathToGlobalConfig);
      }

      if (!Files.exists(Paths.get(globalConfigPath))) {
        throw new RuntimeException(
            "Global config is not found in path: " + super.pathToGlobalConfig);
      }
      return globalConfigPath;
    }

    private void initTonlibConfig(TonGlobalConfig tonGlobalConfig) {
      TonlibSetup tonlibSetup =
          TonlibSetup.builder()
              .type("init")
              .options(
                  TonlibOptions.builder()
                      .type("options")
                      .config(
                          TonlibConfig.builder()
                              .type("config")
                              .config(gson.toJson(tonGlobalConfig))
                              .use_callbacks_for_network(false)
                              .blockchain_name("")
                              .ignore_cache(super.ignoreCache)
                              .build())
                      .keystore_type(
                          super.keystoreInMemory
                              ? KeyStoreTypeMemory.builder().type("keyStoreTypeInMemory").build()
                              : KeyStoreTypeDirectory.builder()
                                  .type("keyStoreTypeDirectory")
                                  .directory(
                                      super.keystorePath.equals(".") ? "." : super.keystorePath)
                                  .build())
                      .build())
              .build();

      Utils.disableNativeOutput(super.verbosityLevel.ordinal());
      tonlibJson.tonlib_client_json_send(tonlib, gson.toJson(tonlibSetup));
      tonlibJson.tonlib_client_json_receive(tonlib, super.receiveTimeout);
      Utils.enableNativeOutput(super.verbosityLevel.ordinal());
    }
  }

  private void reinitTonlibConfig(TonGlobalConfig tonGlobalConfig) {

    // recreate tonlib instance
    // tonlibJson.tonlib_client_json_destroy(tonlib);
    destroy();

    //    TonlibJsonI rawLibrary = Native.load(pathToTonlibSharedLib, TonlibJsonI.class);
    //    tonlibJson = (TonlibJsonI) Native.synchronizedLibrary(rawLibrary);
    tonlibJson = Native.load(pathToTonlibSharedLib, TonlibJsonI.class);
    Utils.disableNativeOutput(verbosityLevel.ordinal());
    tonlib = tonlibJson.tonlib_client_json_create();

    tonlibJson.tonlib_client_set_verbosity_level(verbosityLevel.ordinal());

    TonlibSetup tonlibSetup =
        TonlibSetup.builder()
            .type("init")
            .options(
                TonlibOptions.builder()
                    .type("options")
                    .config(
                        TonlibConfig.builder()
                            .type("config")
                            .config(gson.toJson(tonGlobalConfig))
                            .use_callbacks_for_network(false)
                            .blockchain_name("")
                            .ignore_cache(ignoreCache)
                            .build())
                    .keystore_type(
                        keystoreInMemory
                            ? KeyStoreTypeMemory.builder().type("keyStoreTypeInMemory").build()
                            : KeyStoreTypeDirectory.builder()
                                .type("keyStoreTypeDirectory")
                                .directory(keystorePath.equals(".") ? "." : keystorePath)
                                .build())
                    .build())
            .build();

    tonlibJson.tonlib_client_json_send(tonlib, gson.toJson(tonlibSetup));
    tonlibJson.tonlib_client_json_receive(tonlib, receiveTimeout);

    Utils.enableNativeOutput(verbosityLevel.ordinal());
  }

  public void destroy() {
    Utils.disableNativeOutput(verbosityLevel.ordinal());
    tonlibJson.tonlib_client_json_destroy(tonlib);
    Utils.enableNativeOutput(verbosityLevel.ordinal());
  }

  private String receive() {
    String result = null;
    int retry = 0;
    while (isNull(result)) {
      //      if (retry > 0) {
      //        log.info("retry " + retry);
      //      }
      if (++retry > receiveRetryTimes) {
        throw new Error(
            "Error in tonlib.receive(), "
                + receiveRetryTimes
                + " times was not able retrieve result from lite-server.");
      }
      result = tonlibJson.tonlib_client_json_receive(tonlib, receiveTimeout);
    }
    return result;
  }

  private String syncAndRead(String query) {
    String response = null;
    try {
      Utils.disableNativeOutput(verbosityLevel.ordinal());
      tonlibJson.tonlib_client_json_send(tonlib, query);
      // {"@type":"blocks.lookupBlock","mode":1,"id":{"@type":"ton.blockId","workchain":-1,"shard":-9223372036854775808,"seqno":42555714},"lt":0,"utime":0,"@extra":"94e27988-cf7a-4205-9f89-35dab8037c16"}
      TimeUnit.MILLISECONDS.sleep(200);
      response = receive();
      Utils.enableNativeOutput(verbosityLevel.ordinal());
      int retry = 0;
      outterloop:
      do {
        do {

          if (response.contains("error")) {
            log.info(response);

            if (++retry > receiveRetryTimes) {
              throw new Error(
                  "Error in tonlib.receive(), "
                      + receiveRetryTimes
                      + " times was not able retrieve result from lite-server.");
            }

            if (response.contains("Failed to unpack account state")) {
              log.info(
                  "You are trying to deploy a contract on address that does not have toncoins.");
              break outterloop;
            }

            if (usingAllLiteServers) {
              if (retry < originalGlobalConfigInternal.getLiteservers().length) {
                // try next lite-server from the list
                TonGlobalConfig globalConfigCurrent =
                    gson.fromJson(originalGlobalConfigStr, TonGlobalConfig.class);
                LiteServers[] liteServers = originalGlobalConfigInternal.getLiteservers();
                LiteServers[] newLiteServers = new LiteServers[1];
                newLiteServers[0] =
                    liteServers[retry % originalGlobalConfigInternal.getLiteservers().length];
                globalConfigCurrent.setLiteservers(newLiteServers);

                log.info(
                    "Trying next lite-server at index: "
                        + (retry % originalGlobalConfigInternal.getLiteservers().length)
                        + " ("
                        + Utils.int2ip(globalConfigCurrent.getLiteservers()[0].getIp())
                        + ")");

                reinitTonlibConfig(globalConfigCurrent);
                // repeat request
                Utils.disableNativeOutput(verbosityLevel.ordinal());
                tonlibJson.tonlib_client_json_send(tonlib, query);
                Utils.enableNativeOutput(verbosityLevel.ordinal());
              }
            }
          } else if (response.contains("\"@type\":\"ok\"")) {
            String queryExtraId = StringUtils.substringBetween(query, "@extra\":\"", "\"}");
            String responseExtraId = StringUtils.substringBetween(response, "@extra\":\"", "\"}");
            if (queryExtraId.equals(responseExtraId)) {
              break outterloop;
            }
          } else if (response.contains("\"@extra\"")) {
            break outterloop;
          }

          if (response.contains(" : duplicate message\"")) {
            break outterloop;
          }
          Utils.disableNativeOutput(verbosityLevel.ordinal());
          TimeUnit.MILLISECONDS.sleep(200);
          response = receive();
          Utils.enableNativeOutput(verbosityLevel.ordinal());
          UpdateSyncState sync = gson.fromJson(response, UpdateSyncState.class);
          if (nonNull(sync)
              && nonNull(sync.getSync_state())
              && sync.getType().equals("updateSyncState")
              && !response.contains("syncStateDone")) {
            double pct = 0.0;
            if (sync.getSync_state().getTo_seqno() != 0) {
              pct =
                  (sync.getSync_state().getCurrent_seqno() * 100)
                      / (double) sync.getSync_state().getTo_seqno();
            }
            if (pct < 99.5) {
              log.info("Synchronized: " + String.format("%.2f%%", pct));
            }
          }
          if (isNull(response)) {
            throw new RuntimeException("Error in waitForSyncDone(), response is null.");
          }

        } while (response.contains("error") || response.contains("syncStateInProgress"));

        if (response.contains("syncStateDone")) {
          response = receive();
        }
        if (response.contains("error")) {
          log.info(response);

          if (++retry > receiveRetryTimes) {
            throw new Error(
                "Error in tonlib.receive(), "
                    + receiveRetryTimes
                    + " times was not able retrieve result from lite-server.");
          }

          Utils.disableNativeOutput(verbosityLevel.ordinal());
          tonlibJson.tonlib_client_json_send(tonlib, query);
          Utils.enableNativeOutput(verbosityLevel.ordinal());
        }
      } while (response.contains("error") || response.contains("syncStateInProgress"));

      return response;

    } catch (Exception e) {
      log.info(e.getMessage());
      return response;
    }
  }

  /**
   * Get BlockIdExt by parameters.
   *
   * @param seqno long, can be zero if unknown
   * @param workchain long, -1 or 0
   * @param shard long
   * @param lt long
   * @param utime long
   * @return BlockIdExt
   */
  public BlockIdExt lookupBlock(long seqno, long workchain, long shard, long lt, long utime) {
    synchronized (gson) {
      int mode = 0;
      if (seqno != 0) {
        mode += 1;
      }
      if (lt != 0) {
        mode += 2;
      }
      if (utime != 0) {
        mode += 4;
      }
      LookupBlockQuery lookupBlockQuery =
          LookupBlockQuery.builder()
              .mode(mode)
              .id(BlockId.builder().seqno(seqno).workchain(workchain).shard(shard).build())
              .lt(lt)
              .utime(utime)
              .build();

      String result = syncAndRead(gson.toJson(lookupBlockQuery));
      return gson.fromJson(result, BlockIdExt.class);
    }
  }

  public BlockIdExt lookupBlock(long seqno, long workchain, long shard, long lt) {
    return lookupBlock(seqno, workchain, shard, lt, 0);
  }

  public MasterChainInfo getLast() {
    synchronized (gson) {
      GetLastQuery getLastQuery = GetLastQuery.builder().build();

      String result = syncAndRead(gson.toJson(getLastQuery));
      return gson.fromJson(result, MasterChainInfo.class);
    }
  }

  public LiteServerVersion getLiteServerVersion() {
    synchronized (gson) {
      GetLiteServerInfoQuery getLiteServerQuery = GetLiteServerInfoQuery.builder().build();

      String result = syncAndRead(gson.toJson(getLiteServerQuery));
      return gson.fromJson(result, LiteServerVersion.class);
    }
  }

  public MasterChainInfo getMasterChainInfo() {
    return getLast();
  }

  public Shards getShards(BlockIdExt id) {
    synchronized (gson) {
      GetShardsQuery getShardsQuery = GetShardsQuery.builder().id(id).build();

      String result = syncAndRead(gson.toJson(getShardsQuery));
      return gson.fromJson(result, Shards.class);
    }
  }

  public Shards getShards(long seqno, long lt, long unixtime) {
    if ((seqno <= 0) && (lt <= 0) && (unixtime <= 0)) {
      throw new Error("Seqno, LT or unixtime should be defined");
    }

    long wc = -1;
    long shard = -9223372036854775808L;

    BlockIdExt fullblock = lookupBlock(seqno, wc, shard, lt, unixtime);

    synchronized (gson) {
      GetShardsQuery getShardsQuery = GetShardsQuery.builder().id(fullblock).build();

      String result = syncAndRead(gson.toJson(getShardsQuery));
      return gson.fromJson(result, Shards.class);
    }
  }

  public Key createNewKey() {
    synchronized (gson) {
      NewKeyQuery newKeyQuery = NewKeyQuery.builder().build();

      String result = syncAndRead(gson.toJson(newKeyQuery));
      return gson.fromJson(result, Key.class);
    }
  }

  public Data encrypt(String data, String secret) {
    synchronized (gson) {
      EncryptQuery encryptQuery =
          EncryptQuery.builder().decrypted_data(data).secret(secret).build();

      String result = syncAndRead(gson.toJson(encryptQuery));
      return gson.fromJson(result, Data.class);
    }
  }

  public Data decrypt(String data, String secret) {
    synchronized (gson) {
      DecryptQuery decryptQuery =
          DecryptQuery.builder().encrypted_data(data).secret(secret).build();

      String result = syncAndRead(gson.toJson(decryptQuery));
      return gson.fromJson(result, Data.class);
    }
  }

  public BlockHeader getBlockHeader(BlockIdExt fullblock) {
    synchronized (gson) {
      BlockHeaderQuery blockHeaderQuery = BlockHeaderQuery.builder().id(fullblock).build();

      String result = syncAndRead(gson.toJson(blockHeaderQuery));
      return gson.fromJson(result, BlockHeader.class);
    }
  }

  // @formatter:off

  /**
   * @param address String
   * @param fromTxLt BigInteger
   * @param fromTxHash String in base64 format
   * @return RawTransactions
   */
  public RawTransactions getRawTransactions(
      String address, BigInteger fromTxLt, String fromTxHash) {

    if (isNull(fromTxLt) || isNull(fromTxHash)) {
      RawAccountState fullAccountState =
          getRawAccountState(AccountAddressOnly.builder().account_address(address).build());
      fromTxLt = fullAccountState.getLast_transaction_id().getLt();
      fromTxHash = fullAccountState.getLast_transaction_id().getHash();
    }

    synchronized (gson) {
      GetRawTransactionsQuery getRawTransactionsQuery =
          GetRawTransactionsQuery.builder()
              .account_address(AccountAddressOnly.builder().account_address(address).build())
              .from_transaction_id(
                  LastTransactionId.builder().lt(fromTxLt).hash(fromTxHash).build())
              .build();

      String result = syncAndRead(gson.toJson(getRawTransactionsQuery));
      return gson.fromJson(result, RawTransactions.class);
    }
  }

  /**
   * @param address String
   * @param fromTxLt BigInteger
   * @param fromTxHash String in base64 format
   * @return RawTransactions
   */
  public RawTransactions getRawTransactionsV2(
      String address, BigInteger fromTxLt, String fromTxHash, int count, boolean tryDecodeMessage) {

    if (isNull(fromTxLt) || isNull(fromTxHash)) {
      RawAccountState fullAccountState =
          getRawAccountState(AccountAddressOnly.builder().account_address(address).build());
      fromTxLt = fullAccountState.getLast_transaction_id().getLt();
      fromTxHash = fullAccountState.getLast_transaction_id().getHash();
    }

    synchronized (gson) {
      GetRawTransactionsV2Query getRawTransactionsQuery =
          GetRawTransactionsV2Query.builder()
              .account_address(AccountAddressOnly.builder().account_address(address).build())
              .from_transaction_id(
                  LastTransactionId.builder().lt(fromTxLt).hash(fromTxHash).build())
              .count(count)
              .try_decode_message(tryDecodeMessage)
              .build();

      String result = syncAndRead(gson.toJson(getRawTransactionsQuery));
      return gson.fromJson(result, RawTransactions.class);
    }
  }

  /**
   * Similar to getRawTransactions but limits the number of txs
   *
   * @param address String
   * @param fromTxLt BigInteger
   * @param fromTxHash String in base64 format
   * @param limit int
   * @return RawTransactions
   */
  public RawTransactions getRawTransactions(
      String address, BigInteger fromTxLt, String fromTxHash, int limit) {

    if (isNull(fromTxLt) || isNull(fromTxHash)) {
      RawAccountState rawAccountState =
          getRawAccountState(AccountAddressOnly.builder().account_address(address).build());
      fromTxLt = rawAccountState.getLast_transaction_id().getLt();
      fromTxHash = rawAccountState.getLast_transaction_id().getHash();
    }
    synchronized (gson) {
      GetRawTransactionsQuery getRawTransactionsQuery =
          GetRawTransactionsQuery.builder()
              .account_address(AccountAddressOnly.builder().account_address(address).build())
              .from_transaction_id(
                  LastTransactionId.builder().lt(fromTxLt).hash(fromTxHash).build())
              .build();

      String result = syncAndRead(gson.toJson(getRawTransactionsQuery));

      RawTransactions rawTransactions = gson.fromJson(result, RawTransactions.class);

      if (isNull(rawTransactions.getTransactions())) {
        throw new Error("lite-server cannot return any transactions");
      }

      if (limit > rawTransactions.getTransactions().size()) {
        limit = rawTransactions.getTransactions().size();
      }

      return RawTransactions.builder()
          .previous_transaction_id(rawTransactions.getPrevious_transaction_id())
          .transactions(rawTransactions.getTransactions().subList(0, limit))
          .build();
    }
  }

  /**
   * @param address String
   * @param fromTxLt BigInteger
   * @param fromTxHash String in base64 format
   * @param historyLimit int
   * @return RawTransactions
   */
  public RawTransactions getAllRawTransactions(
      String address, BigInteger fromTxLt, String fromTxHash, int historyLimit) {

    RawTransactions rawTransactions = getRawTransactions(address, fromTxLt, fromTxHash);

    if (isNull(rawTransactions.getTransactions())) {
      throw new Error("lite-server cannot return any transactions");
    }
    List<RawTransaction> transactions = new ArrayList<>(rawTransactions.getTransactions());

    while (rawTransactions.getPrevious_transaction_id().getLt().compareTo(BigInteger.ZERO) != 0) {
      rawTransactions =
          getRawTransactions(
              address,
              rawTransactions.getPrevious_transaction_id().getLt(),
              rawTransactions.getPrevious_transaction_id().getHash());
      if (isNull(rawTransactions.getTransactions()) && !transactions.isEmpty()) {
        return RawTransactions.builder().transactions(transactions).build();
      }
      transactions.addAll(rawTransactions.getTransactions());
      if (transactions.size() > historyLimit) {
        return RawTransactions.builder()
            .transactions(transactions.subList(0, historyLimit))
            .build();
      }
    }

    if (historyLimit > transactions.size()) {
      return RawTransactions.builder().transactions(transactions).build();
    } else {
      return RawTransactions.builder().transactions(transactions.subList(0, historyLimit)).build();
    }
  }

  public BlockTransactions getBlockTransactions(
      BlockIdExt fullblock, long count, long afterLt, String afterHash) {
    AccountTransactionId afterTx =
        AccountTransactionId.builder().account(afterHash).lt(afterLt).build();

    return getBlockTransactions(fullblock, count, afterTx);
  }

  public BlockTransactions getBlockTransactions(BlockIdExt fullblock, long count) {
    return getBlockTransactions(fullblock, count, null);
  }

  public BlockTransactions getBlockTransactions(
      BlockIdExt fullblock, long count, AccountTransactionId afterTx) {
    synchronized (gson) {
      int mode = 7;
      if (nonNull(afterTx)) {
        mode = 7 + 128;
      }

      if (isNull(afterTx)) {
        afterTx =
            AccountTransactionId.builder()
                .account("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=")
                .lt(0)
                .build();
      }

      GetBlockTransactionsQuery getBlockTransactionsQuery =
          GetBlockTransactionsQuery.builder()
              .id(fullblock)
              .mode(mode)
              .count(count)
              .after(afterTx)
              .build();

      String result = syncAndRead(gson.toJson(getBlockTransactionsQuery));

      return gson.fromJson(result, BlockTransactions.class);
    }
  }

  /**
   * @param fullblock - workchain, shard, seqno, root-hash, file-hash
   * @param count - limit result
   * @param afterTx - filter out Tx before this one
   * @return Map &lt;String, RawTransactions&gt;
   */
  public Map<String, RawTransactions> getAllBlockTransactions(
      BlockIdExt fullblock, long count, AccountTransactionId afterTx) {
    Map<String, RawTransactions> totalTxs = new HashMap<>();
    BlockTransactions blockTransactions = getBlockTransactions(fullblock, count, afterTx);
    for (ShortTxId tx : blockTransactions.getTransactions()) {
      String addressHex = Utils.base64ToHexString(tx.getAccount());
      String address = Address.of(fullblock.getWorkchain() + ":" + addressHex).toString(false);
      RawTransactions rawTransactions =
          getRawTransactions(address, BigInteger.valueOf(tx.getLt()), tx.getHash());
      totalTxs.put(address + "|" + tx.getLt(), rawTransactions);
    }
    return totalTxs;
  }

  /**
   * Returns RawAccountState that always contains code and data
   *
   * @param address AccountAddressOnly
   * @return RawAccountState
   */
  public RawAccountState getRawAccountState(AccountAddressOnly address) {
    synchronized (gson) {
      GetRawAccountStateQueryOnly getAccountStateQuery =
          GetRawAccountStateQueryOnly.builder().account_address(address).build();

      String result = syncAndRead(gson.toJson(getAccountStateQuery));
      return gson.fromJson(result, RawAccountState.class);
    }
  }

  /**
   * Returns status of an address, code and data
   *
   * @param address Address
   * @return account state RawAccountState
   */
  public RawAccountState getRawAccountState(Address address) {
    synchronized (gson) {
      AccountAddressOnly accountAddressOnly =
          AccountAddressOnly.builder().account_address(address.toString(false)).build();

      GetRawAccountStateQueryOnly getAccountStateQuery =
          GetRawAccountStateQueryOnly.builder().account_address(accountAddressOnly).build();

      String result = syncAndRead(gson.toJson(getAccountStateQuery));
      return gson.fromJson(result, RawAccountState.class);
    }
  }

  public RawAccountState getRawAccountState(Address address, BlockIdExt blockId) {
    synchronized (gson) {
      if (StringUtils.isEmpty(blockId.getRoot_hash())) { // retrieve hashes
        blockId = lookupBlock(blockId.getSeqno(), blockId.getWorkchain(), blockId.getShard(), 0);
        if (StringUtils.isEmpty(blockId.getRoot_hash())) {
          throw new Error(
              "Cannot lookup block for hashes by seqno. Probably block not in db. Try to specify block's root and file hashes manually in base64 format.");
        }
        log.info("got hashes " + blockId);
      }

      AccountAddressOnly accountAddressOnly =
          AccountAddressOnly.builder().account_address(address.toString(false)).build();

      GetRawAccountStateQueryOnly getAccountStateQuery =
          GetRawAccountStateQueryOnly.builder().account_address(accountAddressOnly).build();

      RawGetAccountStateOnlyWithBlockQuery rawGetAccountStateOnlyWithBlockQuery =
          RawGetAccountStateOnlyWithBlockQuery.builder()
              .id(blockId)
              .function(getAccountStateQuery)
              .build();

      String result = syncAndRead(gson.toJson(rawGetAccountStateOnlyWithBlockQuery));

      if ((isNull(result)) || (result.contains("@type") && result.contains("error"))) {
        throw new Error("Cannot getRawAccountState, error" + result);
      }

      return gson.fromJson(result, RawAccountState.class);
    }
  }

  /**
   * Returns status of an address
   *
   * @param address Address
   * @return String, uninitialized, frozen or active
   */
  public String getRawAccountStatus(Address address) {
    synchronized (gson) {
      AccountAddressOnly accountAddressOnly =
          AccountAddressOnly.builder().account_address(address.toString(false)).build();

      GetRawAccountStateQueryOnly getAccountStateQuery =
          GetRawAccountStateQueryOnly.builder().account_address(accountAddressOnly).build();

      String result = syncAndRead(gson.toJson(getAccountStateQuery));

      RawAccountState state = gson.fromJson(result, RawAccountState.class);

      if (StringUtils.isEmpty(state.getCode())) {
        if (StringUtils.isEmpty(state.getFrozen_hash())) {
          return "uninitialized";
        } else {
          return "frozen";
        }
      } else {
        return "active";
      }
    }
  }

  /**
   * With comparison to getRawAccountState returns wallet_id and seqno, not necessarily returns code
   * and data
   *
   * @param address AccountAddressOnly
   * @return FullAccountState
   */
  public FullAccountState getAccountState(AccountAddressOnly address) {
    synchronized (gson) {
      GetAccountStateQueryOnly getAccountStateQuery =
          GetAccountStateQueryOnly.builder().account_address(address).build();

      String result = syncAndRead(gson.toJson(getAccountStateQuery));
      return gson.fromJson(result, FullAccountState.class);
    }
  }

  /**
   * With comparison to getRawAccountState returns wallet_id and seqno, not necessarily returns code
   * and data
   *
   * @param address Address
   * @return FullAccountState
   */
  public FullAccountState getAccountState(Address address) {
    synchronized (gson) {
      AccountAddressOnly accountAddressOnly =
          AccountAddressOnly.builder().account_address(address.toString(false)).build();

      GetAccountStateQueryOnly getAccountStateQuery =
          GetAccountStateQueryOnly.builder().account_address(accountAddressOnly).build();

      String result = syncAndRead(gson.toJson(getAccountStateQuery));
      return gson.fromJson(result, FullAccountState.class);
    }
  }

  public FullAccountState getAccountState(Address address, BlockIdExt blockId) {
    synchronized (gson) {
      if (StringUtils.isEmpty(blockId.getRoot_hash())) { // retrieve hashes
        blockId = lookupBlock(blockId.getSeqno(), blockId.getWorkchain(), blockId.getShard(), 0);
        if (StringUtils.isEmpty(blockId.getRoot_hash())) {
          throw new Error(
              "Cannot lookup block for hashes by seqno. Probably block not in db. Try to specify block's root and file hashes manually in base64 format.");
        }
        log.info("got hashes " + blockId);
      }

      AccountAddressOnly accountAddressOnly =
          AccountAddressOnly.builder().account_address(address.toString(false)).build();

      GetAccountStateQueryOnly getAccountStateQuery =
          GetAccountStateQueryOnly.builder().account_address(accountAddressOnly).build();

      GetAccountStateOnlyWithBlockQuery getAccountStateOnlyWithBlockQuery =
          GetAccountStateOnlyWithBlockQuery.builder()
              .id(blockId)
              .function(getAccountStateQuery)
              .build();

      String result = syncAndRead(gson.toJson(getAccountStateOnlyWithBlockQuery));

      if ((isNull(result)) || (result.contains("@type") && result.contains("error"))) {
        throw new Error("Cannot getAccountState, error" + result);
      }

      return gson.fromJson(result, FullAccountState.class);
    }
  }

  /**
   * Returns account status by address.
   *
   * @param address Address
   * @return String - uninitialized, frozen or active.
   */
  public String getAccountStatus(Address address) {
    RawAccountState state = getRawAccountState(address);
    if (nonNull(state) && StringUtils.isEmpty(state.getCode())) {
      if (StringUtils.isEmpty(state.getFrozen_hash())) {
        return "uninitialized";
      } else {
        return "frozen";
      }
    } else {
      return "active";
    }
  }

  /**
   * Returns account status by address.
   *
   * @param address Address
   * @return BigInteger returned balance
   */
  public BigInteger getAccountBalance(Address address) {
    String balance = getRawAccountState(address).getBalance();
    if (balance.equals("-1")) {
      return BigInteger.ZERO;
    }
    return new BigInteger(balance);
  }

  /**
   * Returns account status by address and blockId.
   *
   * @param address Address
   * @return String - uninitialized, frozen or active.
   */
  public String getAccountStatus(Address address, BlockIdExt blockId) {
    RawAccountState state = getRawAccountState(address, blockId);
    if (nonNull(state) && StringUtils.isEmpty(state.getCode())) {
      if (StringUtils.isEmpty(state.getFrozen_hash())) {
        return "uninitialized";
      } else {
        return "frozen";
      }
    } else {
      return "active";
    }
  }

  public String getAccountBalance(Address address, BlockIdExt blockId) {
    return getRawAccountState(address, blockId).getBalance();
  }

  public Cell getConfigAll(int mode) {
    synchronized (gson) {
      GetConfigAllQuery configParamQuery = GetConfigAllQuery.builder().mode(mode).build();

      String result = syncAndRead(gson.toJson(configParamQuery));
      ConfigInfo ci = gson.fromJson(result, ConfigInfo.class);
      return CellBuilder.beginCell()
          .fromBoc(Utils.base64ToBytes(ci.getConfig().getBytes()))
          .endCell();
    }
  }

  public Cell getConfigParam(BlockIdExt id, long param) {
    synchronized (gson) {
      GetConfigParamQuery configParamQuery =
          GetConfigParamQuery.builder().id(id).param(param).build();

      String result = syncAndRead(gson.toJson(configParamQuery));
      ConfigInfo ci = gson.fromJson(result, ConfigInfo.class);
      return CellBuilder.beginCell()
          .fromBoc(Utils.base64ToBytes(ci.getConfig().getBytes()))
          .endCell();
    }
  }

  public ConfigParams0 getConfigParam0() {
    return ConfigParams0.deserialize(CellSlice.beginParse(getConfigParam(getLast().getLast(), 0)));
  }

  public ConfigParams1 getConfigParam1() {
    return ConfigParams1.deserialize(CellSlice.beginParse(getConfigParam(getLast().getLast(), 1)));
  }

  public ConfigParams2 getConfigParam2() {
    return ConfigParams2.deserialize(CellSlice.beginParse(getConfigParam(getLast().getLast(), 2)));
  }

  public ConfigParams3 getConfigParam3() {
    return ConfigParams3.deserialize(CellSlice.beginParse(getConfigParam(getLast().getLast(), 3)));
  }

  public ConfigParams4 getConfigParam4() {
    return ConfigParams4.deserialize(CellSlice.beginParse(getConfigParam(getLast().getLast(), 4)));
  }

  public ConfigParams5 getConfigParam5() {
    return ConfigParams5.deserialize(CellSlice.beginParse(getConfigParam(getLast().getLast(), 5)));
  }

  public ConfigParams6 getConfigParam6() {
    return ConfigParams6.deserialize(CellSlice.beginParse(getConfigParam(getLast().getLast(), 6)));
  }

  public ConfigParams8 getConfigParam8() {
    return ConfigParams8.deserialize(CellSlice.beginParse(getConfigParam(getLast().getLast(), 8)));
  }

  public long loadContract(AccountAddressOnly address) {
    synchronized (gson) {
      LoadContractQuery loadContractQuery =
          LoadContractQuery.builder().account_address(address).build();

      String result = syncAndRead(gson.toJson(loadContractQuery));

      return gson.fromJson(result, LoadContract.class).getId();
    }
  }

  /** loads contract by seqno within master chain and shard -9223372036854775808 */
  public long loadContract(AccountAddressOnly address, long seqno) {
    synchronized (gson) {
      BlockIdExt fullBlock;
      if (seqno != 0) {
        fullBlock = lookupBlock(seqno, -1, -9223372036854775808L, 0);
      } else {
        fullBlock = getMasterChainInfo().getLast();
      }

      LoadContractQuery loadContractQuery =
          LoadContractQuery.builder().account_address(address).build();

      LoadContractWithBlockQuery loadContractWithBlockQuery =
          LoadContractWithBlockQuery.builder().id(fullBlock).function(loadContractQuery).build();

      String result = syncAndRead(gson.toJson(loadContractWithBlockQuery));

      return gson.fromJson(result, LoadContract.class).getId();
    }
  }

  /**
   * load contract by blockId
   *
   * @param address contract's address
   * @param blockId BlockIdExt
   * @return contract's id
   */
  public long loadContract(AccountAddressOnly address, BlockIdExt blockId) {
    synchronized (gson) {
      if (StringUtils.isEmpty(blockId.getRoot_hash())) {
        blockId = lookupBlock(blockId.getSeqno(), blockId.getWorkchain(), blockId.getShard(), 0);
      }

      LoadContractQuery loadContractQuery =
          LoadContractQuery.builder().account_address(address).build();

      LoadContractWithBlockQuery loadContractWithBlockQuery =
          LoadContractWithBlockQuery.builder().id(blockId).function(loadContractQuery).build();

      String result = syncAndRead(gson.toJson(loadContractWithBlockQuery));

      return gson.fromJson(result, LoadContract.class).getId();
    }
  }

  public RunResult runMethod(Address contractAddress, String methodName) {
    long contractId =
        loadContract(
            AccountAddressOnly.builder().account_address(contractAddress.toString(false)).build());
    if (contractId == -1) {
      System.err.println(
          "cannot load contract "
              + AccountAddressOnly.builder().account_address(contractAddress.toString(false)));
      return null;
    } else {
      return runMethod(contractId, methodName, null);
    }
  }

  /** executes runMethod at specified blockId */
  public RunResult runMethod(Address contractAddress, String methodName, BlockIdExt blockId) {
    long contractId =
        loadContract(
            AccountAddressOnly.builder().account_address(contractAddress.toString(false)).build(),
            blockId);
    if (contractId == -1) {
      System.err.println(
          "cannot load contract "
              + AccountAddressOnly.builder().account_address(contractAddress.toString(false)));
      return null;
    } else {
      return runMethod(contractId, methodName, null);
    }
  }

  /** executes runMethod by seqno within master chain and shard -9223372036854775808 */
  public RunResult runMethod(Address contractAddress, String methodName, long seqno) {
    long contractId =
        loadContract(
            AccountAddressOnly.builder().account_address(contractAddress.toString(false)).build(),
            seqno);
    if (contractId == -1) {
      System.err.println(
          "cannot load contract "
              + AccountAddressOnly.builder().account_address(contractAddress.toString(false)));
      return null;
    } else {
      return runMethod(contractId, methodName, null);
    }
  }

  public RunResult runMethod(Address contractAddress, String methodName, Deque<String> stackData) {
    synchronized (gson) {
      long contractId =
          loadContract(
              AccountAddressOnly.builder()
                  .account_address(contractAddress.toString(false))
                  .build());
      if (contractId == -1) {
        System.err.println(
            "cannot load contract "
                + AccountAddressOnly.builder().account_address(contractAddress.toString(false)));
        return null;
      } else {
        return runMethod(contractId, methodName, stackData);
      }
    }
  }

  public RunResult runMethod(Address contractAddress, long methodId) {
    long contractId =
        loadContract(
            AccountAddressOnly.builder().account_address(contractAddress.toString(false)).build());
    if (contractId == -1) {
      System.err.println(
          "cannot load contract "
              + AccountAddressOnly.builder().account_address(contractAddress.toString(false)));
      return null;
    } else {
      return runMethod(contractId, methodId, null);
    }
  }

  public RunResult runMethod(Address contractAddress, long methodId, Deque<String> stackData) {
    synchronized (gson) {
      long contractId =
          loadContract(
              AccountAddressOnly.builder()
                  .account_address(contractAddress.toString(false))
                  .build());
      if (contractId == -1) {
        System.err.println(
            "cannot load contract "
                + AccountAddressOnly.builder().account_address(contractAddress.toString(false)));
        return null;
      } else {
        return runMethod(contractId, methodId, stackData);
      }
    }
  }

  public RunResult runMethod(long contractId, String methodName, Deque<String> stackData) {
    synchronized (gson) {
      Deque<TvmStackEntry> stack = null;
      if (nonNull(stackData)) {
        stack = ParseRunResult.renderTvmStack(stackData);
      }

      RunMethodStrQuery runMethodQuery =
          RunMethodStrQuery.builder()
              .id(contractId)
              .method(MethodString.builder().name(methodName).build())
              .stack(stack)
              .build();

      String result = syncAndRead(gson.toJson(runMethodQuery));

      return new RunResultParser().parse(result);
    }
  }

  public RunResult runMethod(long contractId, long methodId, Deque<String> stackData) {
    synchronized (gson) {
      Deque<TvmStackEntry> stack = null;
      if (nonNull(stackData)) {
        stack = ParseRunResult.renderTvmStack(stackData);
      }

      RunMethodIntQuery runMethodQuery =
          RunMethodIntQuery.builder()
              .id(contractId)
              .method(MethodNumber.builder().number(methodId).build())
              .stack(stack)
              .build();

      String result = syncAndRead(gson.toJson(runMethodQuery));

      return new RunResultParser().parse(result);
    }
  }

  /**
   * Generic method to call seqno method of a contract. There is no check if seqno method exists.
   *
   * @param address Address
   * @return long
   */
  public long getSeqno(Address address) {
    RunResult result = runMethod(address, "seqno");
    if (result.getExit_code() != 0) {
      throw new Error(
          "can't get result by executing seqno method, exit code " + result.getExit_code());
    }

    TvmStackEntryNumber seqno = (TvmStackEntryNumber) result.getStack().get(0);

    return seqno.getNumber().longValue();
  }

  public BigInteger getPublicKey(Address address) {
    RunResult result = runMethod(address, "get_public_key");
    if (result.getExit_code() != 0) {
      throw new Error(
          "can't get result by executing get_public_key method, exit code "
              + result.getExit_code());
    }

    TvmStackEntryNumber pubKey = (TvmStackEntryNumber) result.getStack().get(0);

    return pubKey.getNumber();
  }

  /**
   * Generic method to call get_subwallet_id method of a contract. There is no check if
   * get_subwallet_id method exists.
   *
   * @param address Address
   * @return long
   */
  public long getSubWalletId(Address address) {
    RunResult result = runMethod(address, "get_subwallet_id");
    if (result.getExit_code() != 0) {
      throw new Error("method get_subwallet_id returned an exit code " + result.getExit_code());
    }

    TvmStackEntryNumber seqno = (TvmStackEntryNumber) result.getStack().get(0);

    return seqno.getNumber().longValue();
  }

  /**
   * Sends raw message, bag of cells encoded in base64
   *
   * @param serializedBoc - base64 encoded BoC
   * @return ExtMessageInfo In case of error might contain error code and message inside
   */
  public ExtMessageInfo sendRawMessage(String serializedBoc) {
    synchronized (gson) {
      SendRawMessageQuery sendMessageQuery =
          SendRawMessageQuery.builder().body(serializedBoc).build();

      String result = syncAndRead(gson.toJson(sendMessageQuery));

      if ((isNull(result)) || (result.contains("@type") && result.contains("error"))) {
        TonlibError error = gson.fromJson(result, TonlibError.class);
        return ExtMessageInfo.builder().error(error).build();
      } else {
        ExtMessageInfo extMessageInfo = gson.fromJson(result, ExtMessageInfo.class);
        extMessageInfo.setError(TonlibError.builder().code(0).build());
        return extMessageInfo;
      }
    }
  }

  /**
   * Sends raw message, bag of cells encoded in base64, with deliver confirmation. After the message
   * has been sent to the network this method looks up specified account transactions and returns
   * true if message was found among them. Timeout 60 seconds.
   *
   * @param serializedBoc - base64 encoded BoC
   * @return RawTransaction in case of success, null if message not found within a timeout and
   *     throws Error in case of failure.
   */
  public RawTransaction sendRawMessageWithConfirmation(String serializedBoc, Address account) {
    synchronized (gson) {
      SendRawMessageQuery sendMessageQuery =
          SendRawMessageQuery.builder().body(serializedBoc).build();

      String result = syncAndRead(gson.toJson(sendMessageQuery));

      if ((isNull(result)) || (result.contains("@type") && result.contains("error"))) {
        TonlibError error = gson.fromJson(result, TonlibError.class);
        throw new Error("Cannot send message. Error " + error.toString());
      } else {
        ExtMessageInfo extMessageInfo = gson.fromJson(result, ExtMessageInfo.class);
        extMessageInfo.setError(TonlibError.builder().code(0).build());
        log.info(
            "Message has been successfully sent. Waiting for delivery of message with hash {}",
            extMessageInfo.getHash());
        RawTransactions rawTransactions = null;
        for (int i = 0; i < 12; i++) {
          rawTransactions = getRawTransactions(account.toRaw(), null, null);
          for (RawTransaction tx : rawTransactions.getTransactions()) {
            if (nonNull(tx.getIn_msg())
                && tx.getIn_msg().getHash().equals(extMessageInfo.getHash())) {
              log.info("Message has been delivered.");
              return tx;
            }
          }
          Utils.sleep(5);
        }
        return null;
      }
    }
  }

  public QueryFees estimateFees(
      String destinationAddress,
      String body,
      String initCode,
      String initData,
      boolean ignoreChksig) {
    QueryInfo queryInfo = createQuery(destinationAddress, body, initCode, initData);

    synchronized (gson) {
      EstimateFeesQuery estimateFeesQuery =
          EstimateFeesQuery.builder()
              .queryId(queryInfo.getId())
              .ignore_chksig(ignoreChksig)
              .build();

      String result = syncAndRead(gson.toJson(estimateFeesQuery));

      return gson.fromJson(result, QueryFees.class);
    }
  }

  public QueryFees estimateFees(String destinationAddress, String body) {
    return estimateFees(destinationAddress, body, null, null, true);
  }

  /**
   * Creates query with body, init-code and init-data to be sent to the destination address
   *
   * @param initCode - base64 encoded boc
   * @param initData - base64 encoded boc
   * @param body - base64 encoded boc
   * @param destinationAddress - friendly or unfriendly address
   * @return QueryInfo
   */
  public QueryInfo createQuery(
      String destinationAddress, String body, String initCode, String initData) {
    synchronized (gson) {
      CreateQuery createQuery =
          CreateQuery.builder()
              .init_code(initCode)
              .init_data(initData)
              .body(body)
              .destination(Destination.builder().account_address(destinationAddress).build())
              .build();

      String result = syncAndRead(gson.toJson(createQuery));

      if (result.contains("@type") && result.contains("error")) {
        return QueryInfo.builder().id(-1).build();
      } else {
        return gson.fromJson(result, QueryInfo.class);
      }
    }
  }

  /**
   * Sends/Uploads query to the destination address
   *
   * @param queryInfo - result of createQuery()
   * @return true if query was sent without errors
   */
  public boolean sendQuery(QueryInfo queryInfo) {
    synchronized (gson) {
      SendQuery createQuery = SendQuery.builder().id(queryInfo.getId()).build();

      String result = syncAndRead(gson.toJson(createQuery));

      if (isNull(result)) {
        return false;
      }

      if (result.contains("@type") && result.contains("error")) {
        return false;
      } else {
        try {
          Ok ok = gson.fromJson(result, Ok.class);
          log.info(ok.toString());
          return true;
        } catch (Exception e) {
          return false;
        }
      }
    }
  }

  public boolean createAndSendQuery(
      String destinationAddress, String body, String initCode, String initData) {
    QueryInfo queryInfo = createQuery(destinationAddress, body, initCode, initData);
    return sendQuery(queryInfo);
  }

  /**
   * very close to createAndSendQuery, but StateInit should be generated outside
   *
   * @param destinationAddress - friendly or unfriendly wallet address
   * @param body - serialized base64 encoded body
   * @param initialAccountState - serialized base64 initial account state
   * @return true if query was sent without errors
   */
  public boolean createAndSendMessage(
      String destinationAddress, String body, String initialAccountState) {
    synchronized (gson) {
      CreateAndSendRawMessageQuery createAndSendRawMessageQuery =
          CreateAndSendRawMessageQuery.builder()
              .destination(AccountAddressOnly.builder().account_address(destinationAddress).build())
              .initial_account_state(initialAccountState)
              .data(body)
              .build();

      String result = syncAndRead(gson.toJson(createAndSendRawMessageQuery));

      if (isNull(result)) {
        return false;
      }
      if (result.contains("@type") && result.contains("error")) {
        return false;
      } else {
        try {
          Ok ok = gson.fromJson(result, Ok.class);
          log.info(ok.toString());
          return true;
        } catch (Exception e) {
          return false;
        }
      }
    }
  }

  public RawTransaction tryLocateTxByIncomingMessage(
      Address source, Address destination, long creationLt) {
    Shards shards = getShards(0, creationLt, 0);
    for (BlockIdExt shardData : shards.getShards()) {
      for (int b = 0; b < 3; b++) {
        BlockIdExt block =
            lookupBlock(
                0, shardData.getWorkchain(), shardData.getShard(), creationLt + b * 1000000L);
        BlockTransactions txs = getBlockTransactions(block, 40);

        Pair<String, Long> candidate = null;
        int count = 0;

        for (ShortTxId tx : txs.getTransactions()) {
          if (tx.getAccount().equals(Utils.bytesToBase64(destination.hashPart))) {
            count++;
            if (isNull(candidate) || (candidate.getRight() < tx.getLt())) {
              candidate = Pair.of(tx.getHash(), tx.getLt());
            }
          }
        }

        if (nonNull(candidate)) {
          RawTransactions transactions =
              getRawTransactions(
                  destination.toString(false),
                  BigInteger.valueOf(candidate.getRight()),
                  candidate.getLeft(),
                  Math.max(count, 10));

          for (RawTransaction tx : transactions.getTransactions()) {
            RawMessage in_msg = tx.getIn_msg();
            String txSource = in_msg.getSource().getAccount_address();
            if (StringUtils.isNoneEmpty(txSource)
                && (Address.of(txSource).toString(false).equals(source.toString(false)))) {
              if (in_msg.getCreated_lt() == creationLt) {
                return tx;
              }
            }
          }
        }
      }
    }
    throw new Error("Transaction not found");
  }

  /**
   * PR pending https://github.com/ton-blockchain/ton/pull/1379 todo
   *
   * @param address String
   * @param msgHashBase64 msgHexHash
   * @return RawTransaction
   */
  private RawTransaction getTxByMessageHash(String address, String msgHashBase64) {
    RawTransactions rawTransactions = getRawTransactions(address, null, null);
    for (RawTransaction tx : rawTransactions.getTransactions()) {
      if (nonNull(tx.getIn_msg())) {
        if (tx.getIn_msg().getHash().equals(msgHashBase64)) {
          return tx;
        }
      }
    }
    return null;
  }

  public RawTransaction getRawTransaction(byte workchain, ShortTxId tx) {
    String addressHex = Utils.base64ToHexString(tx.getAccount());
    String address = Address.of(workchain + ":" + addressHex).toString(false);
    synchronized (gson) {
      GetRawTransactionsV2Query getRawTransactionsQuery =
          GetRawTransactionsV2Query.builder()
              .account_address(AccountAddressOnly.builder().account_address(address).build())
              .from_transaction_id(
                  LastTransactionId.builder()
                      .lt(BigInteger.valueOf(tx.getLt()))
                      .hash(tx.getHash())
                      .build())
              .count(1)
              .try_decode_message(false)
              .build();

      Utils.disableNativeOutput(verbosityLevel.ordinal());
      String result = syncAndRead(gson.toJson(getRawTransactionsQuery));
      Utils.enableNativeOutput(verbosityLevel.ordinal());
      RawTransactions res = gson.fromJson(result, RawTransactions.class);
      List<RawTransaction> t = res.getTransactions();
      if (t.size() >= 1) {
        return t.get(0);
      } else {
        return RawTransaction.builder().build();
      }
    }
  }

  public RawTransaction tryLocateTxByOutcomingMessage(
      Address source, Address destination, long creationLt) {
    Shards shards = getShards(0, creationLt, 0);
    for (BlockIdExt shardData : shards.getShards()) {
      BlockIdExt block = lookupBlock(0, shardData.getWorkchain(), shardData.getShard(), creationLt);
      BlockTransactions txs = getBlockTransactions(block, 40);

      Pair<String, Long> candidate = null;
      int count = 0;

      for (ShortTxId tx : txs.getTransactions()) {
        if (tx.getAccount().equals(Utils.bytesToBase64(source.hashPart))) {
          count++;
          if (isNull(candidate) || (candidate.getRight() < tx.getLt())) {
            candidate = Pair.of(tx.getHash(), tx.getLt());
          }
        }
      }

      if (nonNull(candidate)) {
        RawTransactions transactions =
            getRawTransactions(
                source.toString(false),
                BigInteger.valueOf(candidate.getRight()),
                candidate.getLeft(),
                Math.max(count, 10));

        for (RawTransaction tx : transactions.getTransactions()) {
          for (RawMessage out_msg : tx.getOut_msgs()) {
            String txDestination = out_msg.getDestination().getAccount_address();
            if (StringUtils.isNoneEmpty(txDestination)
                && (Address.of(txDestination)
                    .toString(false)
                    .equals(destination.toString(false)))) {
              if (out_msg.getCreated_lt() == creationLt) {
                return tx;
              }
            }
          }
        }
      }
    }
    throw new Error("Transaction not found");
  }

  // taken from PR by Vitaly Valtman
  public DnsResolved dnsResolve(String name, AccountAddressOnly addr) {
    if (addr == null) {
      addr =
          AccountAddressOnly.builder()
              .account_address(
                  "-1:E56754F83426F69B09267BD876AC97C44821345B7E266BD956A7BFBFB98DF35C")
              .build();
    }
    byte[] category = new byte[32];
    Arrays.fill(category, (byte) 0);
    DnsResolveQuery query =
        DnsResolveQuery.builder()
            .account_address(addr)
            .name(name)
            .category(Utils.bytesToBase64(category))
            .ttl(1)
            .build();

    String result = syncAndRead(gson.toJson(query));
    return gson.fromJson(result, DnsResolved.class);
  }

  /**
   * @param librariesHashes list of base64-encoded libraries hashes
   * @return RunResult
   */
  public SmcLibraryResult getLibraries(List<String> librariesHashes) {
    synchronized (gson) {
      GetLibrariesQuery getLibrariesQuery =
          GetLibrariesQuery.builder().library_list(librariesHashes).build();

      String result = syncAndRead(gson.toJson(getLibrariesQuery));
      return new LibraryResultParser().parse(result);
    }
  }

  /**
   * @param librariesHashes list of SmcLibraryQueryExt
   * @return SmcLibraryResult
   */
  public SmcLibraryResult getLibrariesExt(List<SmcLibraryQueryExt> librariesHashes) {
    synchronized (gson) {
      GetLibrariesExtQuery getLibrariesQuery =
          GetLibrariesExtQuery.builder().list(librariesHashes).build();

      String result = syncAndRead(gson.toJson(getLibrariesQuery));
      return new LibraryResultParser().parse(result);
    }
  }

  public boolean isDeployed(Address address) {
    return StringUtils.isNotEmpty(this.getRawAccountState(address).getCode());
  }

  public void waitForDeployment(Address address, int timeoutSeconds) {
    log.info(
        "Waiting for deployment (up to {}s) - {} ({})",
        timeoutSeconds,
        testnet ? address.toBounceableTestnet() : address.toBounceable(),
        address.toRaw());
    int i = 0;
    do {
      if (++i * 2 >= timeoutSeconds) {
        throw new Error("Can't deploy contract within specified timeout.");
      }
      Utils.sleep(2);
    } while (!isDeployed(address));
  }

  public void waitForBalanceChange(Address address, int timeoutSeconds) {
    log.info(
        "Waiting for balance change (up to {}s) - {} ({})",
        timeoutSeconds,
        testnet ? address.toBounceableTestnet() : address.toBounceable(),
        address.toRaw());
    BigInteger initialBalance = getAccountBalance(address);
    int i = 0;
    do {
      if (++i * 2 >= timeoutSeconds) {
        throw new Error(
            "Balance of " + address.toRaw() + "was not changed within specified timeout.");
      }
      Utils.sleep(2);
    } while (initialBalance.equals(getAccountBalance(address)));
  }

  public boolean isTestnet() {
    return testnet;
  }

  public void updateInitBlock() {
    updateInitBlock(pathToGlobalConfig);
  }

  public void updateInitBlock(String pathToGlobalConfig) {

    try {
      if (Files.exists(Path.of(pathToGlobalConfig))) {
        MasterChainInfo masterChainInfo = getLast();
        BlockHeader blockHeader = getBlockHeader(masterChainInfo.getLast());

        BlockIdExt blockIdExt =
            lookupBlock(blockHeader.getPrev_key_block_seqno(), -1, -9223372036854775808L, 0);
        //        log.info(
        //            "seqno {}, fileHash {}, rootHash {}",
        //            blockIdExt.getSeqno(),
        //            blockIdExt.getFile_hash(),
        //            blockIdExt.getRoot_hash());

        String content =
            FileUtils.readFileToString(new File(pathToGlobalConfig), StandardCharsets.UTF_8);

        TonGlobalConfig tonGlobalConfig = gson.fromJson(content, TonGlobalConfig.class);

        tonGlobalConfig.getValidator().getInit_block().setSeqno(blockIdExt.getSeqno());
        tonGlobalConfig.getValidator().getInit_block().setShard(blockIdExt.getShard());
        tonGlobalConfig.getValidator().getInit_block().setWorkchain(blockIdExt.getWorkchain());
        tonGlobalConfig.getValidator().getInit_block().setFile_hash(blockIdExt.getFile_hash());
        tonGlobalConfig.getValidator().getInit_block().setRoot_hash(blockIdExt.getRoot_hash());
        Gson gs = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
        FileUtils.writeStringToFile(
            new File(pathToGlobalConfig), gs.toJson(tonGlobalConfig), Charset.defaultCharset());
        log.info("init-block updated");
      }
    } catch (Exception e) {
      log.error("cannot update init-block in " + pathToGlobalConfig);
    }
  }
}
