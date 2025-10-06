package org.ton.ton4j.tonlib;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.ToNumberPolicy;
import com.sun.jna.*;
import java.io.File;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.ton.ton4j.address.Address;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellBuilder;
import org.ton.ton4j.cell.CellSlice;
import org.ton.ton4j.cell.TonHashMapE;
import org.ton.ton4j.tlb.*;
import org.ton.ton4j.tlb.print.MessagePrintInfo;
import org.ton.ton4j.tlb.print.TransactionPrintInfo;
import org.ton.ton4j.tonlib.queries.*;
import org.ton.ton4j.tonlib.types.*;
import org.ton.ton4j.tonlib.types.BlockHeader;
import org.ton.ton4j.tonlib.types.BlockId;
import org.ton.ton4j.tonlib.types.BlockIdExt;
import org.ton.ton4j.tonlib.types.globalconfig.*;
import org.ton.ton4j.utils.Utils;

@Slf4j
@Builder
public class Tonlib {

  public static final Address ELECTION_ADDRESS =
      Address.of("-1:3333333333333333333333333333333333333333333333333333333333333333");

  /**
   * If not specified then tries to find tonlib in system folder, more info <a
   * href="https://github.com/ton-blockchain/packages">here</a>
   */
  public String pathToTonlibSharedLib;

  /** if not specified and globalConfigAsString is filled then globalConfigAsString is used; */
  public String pathToGlobalConfig;

  /** if not specified and pathToGlobalConfig is filled then pathToGlobalConfig is used; */
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

  private Map<String, String> sent;
  private Map<String, String> received;

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

        super.sent = new ConcurrentHashMap<>();
        super.received = new ConcurrentHashMap<>();

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
        tonlib = tonlibJson.tonlib_client_json_create();
        tonlibJson.tonlib_client_set_verbosity_level(super.verbosityLevel.ordinal());

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
                  isNull(super.pathToGlobalConfig) ? "not specified" : super.pathToGlobalConfig,
                  (nonNull(super.globalConfigAsString) && super.globalConfigAsString.length() > 33)
                      ? "yes"
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
          try {
            globalConfigPath = Utils.getLocalOrDownload(Utils.getGlobalConfigUrlTestnet());
          } catch (Error e) {
            globalConfigPath = Utils.getLocalOrDownload(Utils.getGlobalConfigUrlTestnetGithub());
          }
        } else {
          try {
            globalConfigPath = Utils.getLocalOrDownload(Utils.getGlobalConfigUrlMainnet());
          } catch (Error e) {
            globalConfigPath = Utils.getLocalOrDownload(Utils.getGlobalConfigUrlMainnetGithub());
          }
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

      tonlibJson.tonlib_client_json_send(tonlib, gson.toJson(tonlibSetup));
      tonlibJson.tonlib_client_json_receive(tonlib, super.receiveTimeout);
    }
  }

  private void reinitTonlibConfig(TonGlobalConfig tonGlobalConfig) {

    // recreate tonlib instance
    // tonlibJson.tonlib_client_json_destroy(tonlib);
    destroy();

    //    TonlibJsonI rawLibrary = Native.load(pathToTonlibSharedLib, TonlibJsonI.class);
    //    tonlibJson = (TonlibJsonI) Native.synchronizedLibrary(rawLibrary);
    tonlibJson = Native.load(pathToTonlibSharedLib, TonlibJsonI.class);
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
  }

  public void destroy() {
    tonlibJson.tonlib_client_json_destroy(tonlib);
  }

  private String receive(String queryExtraId) {
    String result;
    do {
      result = tonlibJson.tonlib_client_json_receive(tonlib, receiveTimeout);
      if (isNull(result)) {
        return "";
      }

      String responseExtraId = extractExtra(result);
      if (StringUtils.isNotEmpty(responseExtraId)) {
        received.put(responseExtraId, "");
      }

      if (result.contains("updateSyncState")
          || result.contains("syncStateDone")
          || result.contains("syncStateInProgress")) {
        return result;
      }

      if (received.containsKey(queryExtraId)) {
        sent.remove(queryExtraId);
        received.remove(queryExtraId);
        return result;
      }
    } while (!received.containsKey(queryExtraId));
    return result;
  }

  private String extractExtra(String query) {
    if (StringUtils.countMatches(query, "@extra") > 1) {
      String q = query.replaceFirst("@extra", "");
      return StringUtils.substringBetween(q, "@extra\":\"", "\"}");
    } else {
      return StringUtils.substringBetween(query, "@extra\":\"", "\"}");
    }
  }

  private synchronized String syncAndRead(String query) {
    String response = "";

    String queryExtraId = extractExtra(query);
    if (StringUtils.isNotEmpty(queryExtraId)) {
      sent.put(queryExtraId, "");
    }
    int retry = 0;
    do {

      if (response.contains("syncStateInProgress") || response.contains("syncStateDone")) {
        response = receive(queryExtraId);
      } else {
        tonlibJson.tonlib_client_json_send(tonlib, query);
        response = receive(queryExtraId);
      }

      if (response.contains(" : duplicate message\"")
          || response.contains(": not in db\"")
          || response.contains("Failed to unpack account state")
          || response.contains(": cannot apply external message to current state")) {
        return response;
      }

      if (StringUtils.isEmpty(response) || response.contains("\"@type\":\"error\"")) {
        if (!StringUtils.isEmpty(response)) {
          log.info(response);
        }

        if (++retry > receiveRetryTimes) {
          throw new Error(
              "Error in tonlib.receive(), "
                  + receiveRetryTimes
                  + " times was not able retrieve result from lite-server.");
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
          }
        }
      } else if (response.contains("\"@type\":\"ok\"")) {
        queryExtraId = extractExtra(query);
        String responseExtraId = extractExtra(response);
        if (queryExtraId.equals(responseExtraId)) {
          return response;
        }
      } else if (!response.contains("syncStateDone") && !response.contains("syncStateInProgress")) {
        return response;
      }
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
          if (pct < 99.5) {
            log.info("Synchronizing: " + String.format("%.2f%%", pct));
          }
        }
      }

      Utils.sleepMs(20);

    } while (StringUtils.isEmpty(response)
        || response.contains("\"@type\":\"error\"")
        || response.contains("syncStateInProgress")
        || response.contains("syncStateDone"));

    return response;
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

  public BlockIdExt lookupBlock(long seqno, long workchain, long shard, long lt) {
    return lookupBlock(seqno, workchain, shard, lt, 0);
  }

  public MasterChainInfo getLast() {

    GetLastQuery getLastQuery = GetLastQuery.builder().build();

    String result = syncAndRead(gson.toJson(getLastQuery));
    return gson.fromJson(result, MasterChainInfo.class);
  }

  public LiteServerVersion getLiteServerVersion() {

    GetLiteServerInfoQuery getLiteServerQuery = GetLiteServerInfoQuery.builder().build();

    String result = syncAndRead(gson.toJson(getLiteServerQuery));
    return gson.fromJson(result, LiteServerVersion.class);
  }

  public MasterChainInfo getMasterChainInfo() {
    return getLast();
  }

  public Shards getShards(BlockIdExt id) {

    GetShardsQuery getShardsQuery = GetShardsQuery.builder().id(id).build();

    String result = syncAndRead(gson.toJson(getShardsQuery));
    return gson.fromJson(result, Shards.class);
  }

  public Shards getShards(long seqno, long lt, long unixtime) {
    if ((seqno <= 0) && (lt <= 0) && (unixtime <= 0)) {
      throw new Error("Seqno, LT or unixtime should be defined");
    }

    long wc = -1;
    long shard = -9223372036854775808L;

    BlockIdExt fullblock = lookupBlock(seqno, wc, shard, lt, unixtime);

    GetShardsQuery getShardsQuery = GetShardsQuery.builder().id(fullblock).build();

    String result = syncAndRead(gson.toJson(getShardsQuery));
    return gson.fromJson(result, Shards.class);
  }

  public Key createNewKey() {

    NewKeyQuery newKeyQuery = NewKeyQuery.builder().build();

    String result = syncAndRead(gson.toJson(newKeyQuery));
    return gson.fromJson(result, Key.class);
  }

  public Data encrypt(String data, String secret) {

    EncryptQuery encryptQuery = EncryptQuery.builder().decrypted_data(data).secret(secret).build();

    String result = syncAndRead(gson.toJson(encryptQuery));
    return gson.fromJson(result, Data.class);
  }

  public Data decrypt(String data, String secret) {

    DecryptQuery decryptQuery = DecryptQuery.builder().encrypted_data(data).secret(secret).build();

    String result = syncAndRead(gson.toJson(decryptQuery));
    return gson.fromJson(result, Data.class);
  }

  public BlockHeader getBlockHeader(BlockIdExt fullblock) {

    BlockHeaderQuery blockHeaderQuery = BlockHeaderQuery.builder().id(fullblock).build();

    String result = syncAndRead(gson.toJson(blockHeaderQuery));
    return gson.fromJson(result, BlockHeader.class);
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

    GetRawTransactionsQuery getRawTransactionsQuery =
        GetRawTransactionsQuery.builder()
            .account_address(AccountAddressOnly.builder().account_address(address).build())
            .from_transaction_id(LastTransactionId.builder().lt(fromTxLt).hash(fromTxHash).build())
            .build();

    String result = syncAndRead(gson.toJson(getRawTransactionsQuery));
    return gson.fromJson(result, RawTransactions.class);
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

    GetRawTransactionsV2Query getRawTransactionsQuery =
        GetRawTransactionsV2Query.builder()
            .account_address(AccountAddressOnly.builder().account_address(address).build())
            .from_transaction_id(LastTransactionId.builder().lt(fromTxLt).hash(fromTxHash).build())
            .count(count)
            .try_decode_message(tryDecodeMessage)
            .build();

    String result = syncAndRead(gson.toJson(getRawTransactionsQuery));
    return gson.fromJson(result, RawTransactions.class);
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

    GetRawTransactionsQuery getRawTransactionsQuery =
        GetRawTransactionsQuery.builder()
            .account_address(AccountAddressOnly.builder().account_address(address).build())
            .from_transaction_id(LastTransactionId.builder().lt(fromTxLt).hash(fromTxHash).build())
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

  public BlockTransactionsExt getBlockTransactionsExt(
      BlockIdExt fullblock, long count, AccountTransactionId afterTx) {

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

    GetBlockTransactionsExtQuery getBlockTransactionsExtQuery =
        GetBlockTransactionsExtQuery.builder()
            .id(fullblock)
            .mode(mode)
            .count(count)
            .after(afterTx)
            .build();

    String result = syncAndRead(gson.toJson(getBlockTransactionsExtQuery));

    return gson.fromJson(result, BlockTransactionsExt.class);
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

  public void printAccountMessages(Address account) {
    printAccountMessages(account, 20);
  }

  /** prints messages of account's last historyLimit transactions */
  public void printAccountMessages(Address account, int historyLimit) {
    try {
      boolean first = true;

      List<RawTransaction> response =
          getRawTransactionsV2(account.toBounceable(), null, null, historyLimit, false)
              .getTransactions();
      List<Transaction> result = new ArrayList<>();
      for (RawTransaction tx : response) {
        result.add(Transaction.deserialize(CellSlice.beginParse(Cell.fromBocBase64(tx.getData()))));
      }

      for (Transaction tx : result) {
        TransactionPrintInfo.printAllMessages(tx, first, false);
        first = false;
      }
      MessagePrintInfo.printMessageInfoFooter();

    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /** prints messages of account's last 20 transactions */
  public void printAccountTransactions(Address account) {
    printAccountTransactions(account, 20, false);
  }

  /** prints messages of account's last historyLimit transactions */
  public void printAccountTransactions(Address account, int historyLimit) {
    printAccountTransactions(account, historyLimit, false);
  }

  /** prints messages of account's last historyLimit transactions with messages optionally */
  public void printAccountTransactions(Address account, int historyLimit, boolean withMessages) {
    try {
      boolean first = true;

      List<RawTransaction> response =
          getRawTransactionsV2(account.toBounceable(), null, null, historyLimit, false)
              .getTransactions();
      List<Transaction> result = new ArrayList<>();
      for (RawTransaction tx : response) {
        result.add(Transaction.deserialize(CellSlice.beginParse(Cell.fromBocBase64(tx.getData()))));
      }

      TransactionPrintInfo.printTxHeader();
      for (Transaction tx : result) {
        TransactionPrintInfo.printTransactionInfo(tx);
        if (withMessages) {
          TransactionPrintInfo.printAllMessages(tx, first, true);
        }
      }
      TransactionPrintInfo.printTxFooter();

    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Returns RawAccountState that always contains code and data
   *
   * @param address AccountAddressOnly
   * @return RawAccountState
   */
  public RawAccountState getRawAccountState(AccountAddressOnly address) {

    GetRawAccountStateQueryOnly getAccountStateQuery =
        GetRawAccountStateQueryOnly.builder().account_address(address).build();

    String result = syncAndRead(gson.toJson(getAccountStateQuery));
    return gson.fromJson(result, RawAccountState.class);
  }

  /**
   * Returns status of an address, code and data
   *
   * @param address Address
   * @return account state RawAccountState
   */
  public RawAccountState getRawAccountState(Address address) {

    AccountAddressOnly accountAddressOnly =
        AccountAddressOnly.builder().account_address(address.toString(false)).build();

    GetRawAccountStateQueryOnly getAccountStateQuery =
        GetRawAccountStateQueryOnly.builder().account_address(accountAddressOnly).build();

    String result = syncAndRead(gson.toJson(getAccountStateQuery));
    return gson.fromJson(result, RawAccountState.class);
  }

  public RawAccountState getRawAccountState(Address address, BlockIdExt blockId) {

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

  /**
   * Returns status of an address
   *
   * @param address Address
   * @return String, uninitialized, frozen or active
   */
  public String getRawAccountStatus(Address address) {

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

  /**
   * With comparison to getRawAccountState returns wallet_id and seqno, not necessarily returns code
   * and data
   *
   * @param address AccountAddressOnly
   * @return FullAccountState
   */
  public FullAccountState getAccountState(AccountAddressOnly address) {

    GetAccountStateQueryOnly getAccountStateQuery =
        GetAccountStateQueryOnly.builder().account_address(address).build();

    String result = syncAndRead(gson.toJson(getAccountStateQuery));
    return gson.fromJson(result, FullAccountState.class);
  }

  /**
   * With comparison to getRawAccountState returns wallet_id and seqno, not necessarily returns code
   * and data
   *
   * @param address Address
   * @return FullAccountState
   */
  public FullAccountState getAccountState(Address address) {

    AccountAddressOnly accountAddressOnly =
        AccountAddressOnly.builder().account_address(address.toString(false)).build();

    GetAccountStateQueryOnly getAccountStateQuery =
        GetAccountStateQueryOnly.builder().account_address(accountAddressOnly).build();

    String result = syncAndRead(gson.toJson(getAccountStateQuery));
    return gson.fromJson(result, FullAccountState.class);
  }

  public FullAccountState getAccountState(Address address, BlockIdExt blockId) {

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

    if (isNull(balance) || balance.equals("-1")) {
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

    GetConfigAllQuery configParamQuery = GetConfigAllQuery.builder().mode(mode).build();

    String result = syncAndRead(gson.toJson(configParamQuery));
    ConfigInfo ci = gson.fromJson(result, ConfigInfo.class);
    return CellBuilder.beginCell()
        .fromBoc(Utils.base64ToBytes(ci.getConfig().getBytes()))
        .endCell();
  }

  public Cell getConfigParam(BlockIdExt id, long param) {

    GetConfigParamQuery configParamQuery =
        GetConfigParamQuery.builder().id(id).param(param).build();

    String result = syncAndRead(gson.toJson(configParamQuery));
    ConfigInfo ci = gson.fromJson(result, ConfigInfo.class);
    return CellBuilder.beginCell()
        .fromBoc(Utils.base64ToBytes(ci.getConfig().getBytes()))
        .endCell();
  }

  /** config address */
  public ConfigParams0 getConfigParam0() {
    return ConfigParams0.deserialize(CellSlice.beginParse(getConfigParam(getLast().getLast(), 0)));
  }

  /** elector address */
  public ConfigParams1 getConfigParam1() {
    return ConfigParams1.deserialize(CellSlice.beginParse(getConfigParam(getLast().getLast(), 1)));
  }

  /** minter address */
  public ConfigParams2 getConfigParam2() {
    return ConfigParams2.deserialize(CellSlice.beginParse(getConfigParam(getLast().getLast(), 2)));
  }

  /** fee collector address */
  public ConfigParams3 getConfigParam3() {
    try {
      return ConfigParams3.deserialize(
          CellSlice.beginParse(getConfigParam(getLast().getLast(), 3)));
    } catch (Error e) {
      log.error("Error getting config params 3, use config 1");
      return ConfigParams3.builder().feeCollectorAddr(BigInteger.ONE.negate()).build();
    }
  }

  /** dns root address */
  public ConfigParams4 getConfigParam4() {
    return ConfigParams4.deserialize(CellSlice.beginParse(getConfigParam(getLast().getLast(), 4)));
  }

  /** burning_config */
  public ConfigParams5 getConfigParam5() {
    return ConfigParams5.deserialize(CellSlice.beginParse(getConfigParam(getLast().getLast(), 5)));
  }

  /** mint_new_price:Grams mint_add_price:Grams */
  public ConfigParams6 getConfigParam6() {
    return ConfigParams6.deserialize(CellSlice.beginParse(getConfigParam(getLast().getLast(), 6)));
  }

  /**
   * capabilities#c4 version:uint32 capabilities:uint64 = GlobalVersion; _ GlobalVersion =
   * ConfigParam 8;
   */
  public ConfigParams8 getConfigParam8() {
    return ConfigParams8.deserialize(CellSlice.beginParse(getConfigParam(getLast().getLast(), 8)));
  }

  /** mandatory_params */
  public ConfigParams9 getConfigParam9() {
    return ConfigParams9.deserialize(CellSlice.beginParse(getConfigParam(getLast().getLast(), 9)));
  }

  /** critical_params */
  public ConfigParams10 getConfigParam10() {
    return ConfigParams10.deserialize(
        CellSlice.beginParse(getConfigParam(getLast().getLast(), 10)));
  }

  /**
   * cfg_vote_setup#91 normal_params:^ConfigProposalSetup critical_params:^ConfigProposalSetup =
   * ConfigVotingSetup; _ ConfigVotingSetup = ConfigParam 11;
   */
  public ConfigParams11 getConfigParam11() {
    return ConfigParams11.deserialize(
        CellSlice.beginParse(getConfigParam(getLast().getLast(), 11)));
  }

  /** workchains */
  public ConfigParams12 getConfigParam12() {
    return ConfigParams12.deserialize(
        CellSlice.beginParse(getConfigParam(getLast().getLast(), 12)));
  }

  /** ComplaintPricing */
  public ConfigParams13 getConfigParam13() {
    return ConfigParams13.deserialize(
        CellSlice.beginParse(getConfigParam(getLast().getLast(), 13)));
  }

  /** BlockCreateFees */
  public ConfigParams14 getConfigParam14() {
    return ConfigParams14.deserialize(
        CellSlice.beginParse(getConfigParam(getLast().getLast(), 14)));
  }

  /** election timing */
  public ConfigParams15 getConfigParam15() {
    return ConfigParams15.deserialize(
        CellSlice.beginParse(getConfigParam(getLast().getLast(), 15)));
  }

  /** max min validators */
  public ConfigParams16 getConfigParam16() {
    return ConfigParams16.deserialize(
        CellSlice.beginParse(getConfigParam(getLast().getLast(), 16)));
  }

  /** max min stake */
  public ConfigParams17 getConfigParam17() {
    return ConfigParams17.deserialize(
        CellSlice.beginParse(getConfigParam(getLast().getLast(), 17)));
  }

  /** storage prices */
  public ConfigParams18 getConfigParam18() {
    return ConfigParams18.deserialize(
        CellSlice.beginParse(getConfigParam(getLast().getLast(), 18)));
  }

  /** global id */
  public ConfigParams19 getConfigParam19() {
    return ConfigParams19.deserialize(
        CellSlice.beginParse(getConfigParam(getLast().getLast(), 19)));
  }

  /** GasLimitsPrices masterchain */
  public ConfigParams20 getConfigParam20() {
    return ConfigParams20.deserialize(
        CellSlice.beginParse(getConfigParam(getLast().getLast(), 20)));
  }

  /** GasLimitsPrices workchains */
  public ConfigParams21 getConfigParam21() {
    return ConfigParams21.deserialize(
        CellSlice.beginParse(getConfigParam(getLast().getLast(), 21)));
  }

  /** BlockLimits masterchain */
  public ConfigParams22 getConfigParam22() {
    return ConfigParams22.deserialize(
        CellSlice.beginParse(getConfigParam(getLast().getLast(), 22)));
  }

  /** BlockLimits workchains */
  public ConfigParams23 getConfigParam23() {
    return ConfigParams23.deserialize(
        CellSlice.beginParse(getConfigParam(getLast().getLast(), 23)));
  }

  /** MsgForwardPrices masterchain */
  public ConfigParams24 getConfigParam24() {
    return ConfigParams24.deserialize(
        CellSlice.beginParse(getConfigParam(getLast().getLast(), 24)));
  }

  /** MsgForwardPrices */
  public ConfigParams25 getConfigParam25() {
    return ConfigParams25.deserialize(
        CellSlice.beginParse(getConfigParam(getLast().getLast(), 25)));
  }

  /** CatchainConfig */
  public ConfigParams28 getConfigParam28() {
    return ConfigParams28.deserialize(
        CellSlice.beginParse(getConfigParam(getLast().getLast(), 28)));
  }

  /** ConsensusConfig */
  public ConfigParams29 getConfigParam29() {
    return ConfigParams29.deserialize(
        CellSlice.beginParse(getConfigParam(getLast().getLast(), 29)));
  }

  /** fundamental_smc_addr */
  public ConfigParams31 getConfigParam31() {
    return ConfigParams31.deserialize(
        CellSlice.beginParse(getConfigParam(getLast().getLast(), 31)));
  }

  /** prev_validators */
  public ConfigParams32 getConfigParam32() {
    try {
      return ConfigParams32.deserialize(
          CellSlice.beginParse(getConfigParam(getLast().getLast(), 32)));
    } catch (Error e) {
      return ConfigParams32.builder().prevValidatorSet(Validators.builder().build()).build();
    }
  }

  /** prev_temp_validators */
  public ConfigParams33 getConfigParam33() {
    try {
      return ConfigParams33.deserialize(
          CellSlice.beginParse(getConfigParam(getLast().getLast(), 33)));
    } catch (Error e) {
      return ConfigParams33.builder().prevTempValidatorSet(Validators.builder().build()).build();
    }
  }

  /** cur_validators */
  public ConfigParams34 getConfigParam34() {
    try {
      return ConfigParams34.deserialize(
          CellSlice.beginParse(getConfigParam(getLast().getLast(), 34)));
    } catch (Error e) {
      return ConfigParams34.builder().currValidatorSet(Validators.builder().build()).build();
    }
  }

  /** cur_temp_validators */
  public ConfigParams35 getConfigParam35() {
    try {
      return ConfigParams35.deserialize(
          CellSlice.beginParse(getConfigParam(getLast().getLast(), 35)));
    } catch (Error e) {
      return ConfigParams35.builder().currTempValidatorSet(Validators.builder().build()).build();
    }
  }

  /** next_validators */
  public ConfigParams36 getConfigParam36() {
    try {
      return ConfigParams36.deserialize(
          CellSlice.beginParse(getConfigParam(getLast().getLast(), 36)));
    } catch (Error e) {
      return ConfigParams36.builder().nextValidatorSet(Validators.builder().build()).build();
    }
  }

  /** next_temp_validators */
  public ConfigParams37 getConfigParam37() {
    try {
      return ConfigParams37.deserialize(
          CellSlice.beginParse(getConfigParam(getLast().getLast(), 37)));
    } catch (Error e) {
      return ConfigParams37.builder().nextTempValidatorSet(Validators.builder().build()).build();
    }
  }

  /** ValidatorSignedTempKey */
  public ConfigParams39 getConfigParam39() {
    try {
      return ConfigParams39.deserialize(
          CellSlice.beginParse(getConfigParam(getLast().getLast(), 39)));
    } catch (Error e) {
      return ConfigParams39.builder().validatorSignedTemp(new TonHashMapE(0)).build();
    }
  }

  /** MisbehaviourPunishmentConfig */
  public ConfigParams40 getConfigParam40() {
    try {
      return ConfigParams40.deserialize(
          CellSlice.beginParse(getConfigParam(getLast().getLast(), 40)));
    } catch (Error e) {
      log.error("Error getting config params 40");
      return ConfigParams40.builder().build();
    }
  }

  /** SuspendedAddressList */
  public ConfigParams44 getConfigParam44() {
    return ConfigParams44.deserialize(
        CellSlice.beginParse(getConfigParam(getLast().getLast(), 44)));
  }

  /** PrecompiledContractsConfig */
  public ConfigParams45 getConfigParam45() {
    return ConfigParams45.deserialize(
        CellSlice.beginParse(getConfigParam(getLast().getLast(), 45)));
  }

  /** Ethereum bridges */
  public ConfigParams71 getConfigParam71() {
    return ConfigParams71.deserialize(
        CellSlice.beginParse(getConfigParam(getLast().getLast(), 71)));
  }

  /** Binance Smart Chain bridges */
  public ConfigParams72 getConfigParam72() {
    return ConfigParams72.deserialize(
        CellSlice.beginParse(getConfigParam(getLast().getLast(), 72)));
  }

  /** Polygon bridges */
  public ConfigParams73 getConfigParam73() {
    try {
      return ConfigParams73.deserialize(
          CellSlice.beginParse(getConfigParam(getLast().getLast(), 73)));
    } catch (Error e) {
      return ConfigParams73.builder().polygonBridge(OracleBridgeParams.builder().build()).build();
    }
  }

  /** ETH-&gt;TON token bridges */
  public ConfigParams79 getConfigParam79() {
    return ConfigParams79.deserialize(
        CellSlice.beginParse(getConfigParam(getLast().getLast(), 79)));
  }

  /** BNB-&gt;TON token bridges */
  public ConfigParams81 getConfigParam81() {
    return ConfigParams81.deserialize(
        CellSlice.beginParse(getConfigParam(getLast().getLast(), 81)));
  }

  /** Polygon-&gt;TON token bridges */
  public ConfigParams82 getConfigParam82() {
    try {
      return ConfigParams82.deserialize(
          CellSlice.beginParse(getConfigParam(getLast().getLast(), 82)));
    } catch (Error e) {
      return ConfigParams82.builder()
          .polygonTonTokenBridge(JettonBridgeParamsV1.builder().build())
          .build();
    }
  }

  public long loadContract(AccountAddressOnly address) {

    LoadContractQuery loadContractQuery =
        LoadContractQuery.builder().account_address(address).build();

    String result = syncAndRead(gson.toJson(loadContractQuery));

    return gson.fromJson(result, LoadContract.class).getId();
  }

  /** loads contract by seqno within master chain and shard -9223372036854775808 */
  public long loadContract(AccountAddressOnly address, long seqno) {

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

  /**
   * load contract by blockId
   *
   * @param address contract's address
   * @param blockId BlockIdExt
   * @return contract's id
   */
  public long loadContract(AccountAddressOnly address, BlockIdExt blockId) {

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

    long contractId =
        loadContract(
            AccountAddressOnly.builder().account_address(contractAddress.toString(false)).build());
    if (contractId == -1) {
      System.err.println(
          "cannot load contract "
              + AccountAddressOnly.builder().account_address(contractAddress.toString(false)));
      return null;
    } else {
      return runMethod(contractId, methodName, stackData);
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

    long contractId =
        loadContract(
            AccountAddressOnly.builder().account_address(contractAddress.toString(false)).build());
    if (contractId == -1) {
      System.err.println(
          "cannot load contract "
              + AccountAddressOnly.builder().account_address(contractAddress.toString(false)));
      return null;
    } else {
      return runMethod(contractId, methodId, stackData);
    }
  }

  public RunResult runMethod(long contractId, String methodName, Deque<String> stackData) {

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

  public RunResult runMethod(long contractId, long methodId, Deque<String> stackData) {

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

  /**
   * Sends raw message (bag of cells encoded in base64) without waiting for response
   *
   * @param serializedBoc - base64 encoded BoC
   */
  public void sendRawMessageOnly(String serializedBoc) {

    SendRawMessageQuery sendMessageQuery =
        SendRawMessageQuery.builder().body(serializedBoc).build();

    String query = gson.toJson(sendMessageQuery);
    String queryExtraId = StringUtils.substringBetween(query, "@extra\":\"", "\"}");
    if (StringUtils.isNotEmpty(queryExtraId)) {
      sent.put(queryExtraId, "");
    }

    tonlibJson.tonlib_client_json_send(tonlib, query);
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

  public QueryFees estimateFees(
      String destinationAddress,
      String body,
      String initCode,
      String initData,
      boolean ignoreChksig) {
    QueryInfo queryInfo = createQuery(destinationAddress, body, initCode, initData);

    EstimateFeesQuery estimateFeesQuery =
        EstimateFeesQuery.builder().queryId(queryInfo.getId()).ignore_chksig(ignoreChksig).build();

    String result = syncAndRead(gson.toJson(estimateFeesQuery));

    return gson.fromJson(result, QueryFees.class);
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

  /**
   * Sends/Uploads query to the destination address
   *
   * @param queryInfo - result of createQuery()
   * @return true if query was sent without errors
   */
  public boolean sendQuery(QueryInfo queryInfo) {

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

    String result = syncAndRead(gson.toJson(getRawTransactionsQuery));
    RawTransactions res = gson.fromJson(result, RawTransactions.class);
    List<RawTransaction> t = res.getTransactions();
    if (t.size() >= 1) {
      return t.get(0);
    } else {
      return RawTransaction.builder().build();
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

    GetLibrariesQuery getLibrariesQuery =
        GetLibrariesQuery.builder().library_list(librariesHashes).build();

    String result = syncAndRead(gson.toJson(getLibrariesQuery));
    return new LibraryResultParser().parse(result);
  }

  /**
   * @param librariesHashes list of SmcLibraryQueryExt
   * @return SmcLibraryResult
   */
  public SmcLibraryResult getLibrariesExt(List<SmcLibraryQueryExt> librariesHashes) {

    GetLibrariesExtQuery getLibrariesQuery =
        GetLibrariesExtQuery.builder().list(librariesHashes).build();

    String result = syncAndRead(gson.toJson(getLibrariesQuery));
    return new LibraryResultParser().parse(result);
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

  public void waitForBalanceChangeWithTolerance(
      Address address, int timeoutSeconds, BigInteger tolerateNanoCoins) {

    BigInteger initialBalance = getAccountBalance(address);
    long diff;
    int i = 0;
    do {
      if (++i * 2 >= timeoutSeconds) {
        throw new Error(
            "Balance was not changed by +/- "
                + Utils.formatNanoValue(tolerateNanoCoins)
                + " within specified timeout.");
      }
      Utils.sleep(2);
      BigInteger currentBalance = getAccountBalance(address);

      diff =
          Math.max(currentBalance.longValue(), initialBalance.longValue())
              - Math.min(currentBalance.longValue(), initialBalance.longValue());
    } while (diff < tolerateNanoCoins.longValue());
  }

  public boolean isTestnet() {
    return testnet;
  }

  public void updateInitBlock() {
    updateInitBlock(pathToGlobalConfig);
  }

  public void updateInitBlock(String pathToGlobalConfig) {

    try {
      if (Files.exists(new File(pathToGlobalConfig).toPath())) {
        MasterChainInfo masterChainInfo = getLast();
        BlockHeader blockHeader = getBlockHeader(masterChainInfo.getLast());

        BlockIdExt blockIdExt =
            lookupBlock(blockHeader.getPrev_key_block_seqno(), -1, -9223372036854775808L, 0);

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

  /**
   * returns TPS (transactions per second) all of shardchains within specified time frame
   *
   * @return TPS
   */
  public long getTps(long periodInMinutes) {
    log.info("calculating tps...");
    LinkedList<Long> totalInRange = new LinkedList<>();
    MasterChainInfo masterChainInfo = getLast();
    BlockIdExt last0 = masterChainInfo.getLast();
    long delta;
    long i = 0;
    do {
      BlockIdExt last =
          lookupBlock(last0.getSeqno() - i++, last0.getWorkchain(), last0.getShard(), 0, 0);
      Shards shards = getShards(last.getSeqno(), 0, 0);
      shards.getShards().add(last);
      for (BlockIdExt shard : shards.getShards()) {
        BlockTransactionsExt txs = getBlockTransactionsExt(shard, 10000, null);
        for (RawTransaction tx : txs.getTransactions()) {
          Transaction txi =
              Transaction.deserialize(CellSlice.beginParse(Cell.fromBocBase64(tx.getData())));
          totalInRange.add(txi.getNow());
        }
      }
      Collections.sort(totalInRange);
      delta = totalInRange.getLast() - totalInRange.getFirst();
      delta = (delta == 0) ? 1 : delta;

    } while (delta < periodInMinutes * 60);
    double tps = (double) totalInRange.size() / delta;
    return new BigDecimal(tps).setScale(0, RoundingMode.HALF_UP).toBigInteger().longValue();
  }

  /**
   * quickly gets TPS using latest block data
   *
   * @return TPS
   */
  public long getTpsOneBlock() {
    log.info("calculating tps...");
    LinkedList<Long> totalInRange = new LinkedList<>();
    MasterChainInfo masterChainInfo = getLast();
    BlockIdExt last0 = masterChainInfo.getLast();
    long delta;

    BlockIdExt last = lookupBlock(last0.getSeqno(), last0.getWorkchain(), last0.getShard(), 0, 0);
    Shards shards = getShards(last.getSeqno(), 0, 0);
    shards.getShards().add(last);
    for (BlockIdExt shard : shards.getShards()) {
      BlockTransactionsExt txs = getBlockTransactionsExt(shard, 10000, null);
      for (RawTransaction tx : txs.getTransactions()) {
        Transaction txi =
            Transaction.deserialize(CellSlice.beginParse(Cell.fromBocBase64(tx.getData())));
        totalInRange.add(txi.getNow());
      }
    }
    Collections.sort(totalInRange);
    delta = totalInRange.getLast() - totalInRange.getFirst();
    delta = (delta == 0) ? 1 : delta;

    double tps = (double) totalInRange.size() / delta;
    return new BigDecimal(tps).setScale(0, RoundingMode.HALF_UP).toBigInteger().longValue();
  }

  public List<Participant> getElectionParticipants() {
    List<Participant> participants = new ArrayList<>();
    RunResult result = runMethod(ELECTION_ADDRESS, "participant_list");
    TvmStackEntryList listResult = (TvmStackEntryList) result.getStack().get(0);
    for (Object o : listResult.getList().getElements()) {
      TvmStackEntryTuple t = (TvmStackEntryTuple) o;
      TvmTuple tuple = t.getTuple();
      TvmStackEntryNumber addr = (TvmStackEntryNumber) tuple.getElements().get(0);
      TvmStackEntryNumber stake = (TvmStackEntryNumber) tuple.getElements().get(1);
      participants.add(
          Participant.builder().address(addr.getNumber()).stake(stake.getNumber()).build());
    }
    return participants;
  }

  public BigInteger getElectionId() {
    RunResult result = runMethod(ELECTION_ADDRESS, "active_election_id");
    TvmStackEntryNumber electionId = (TvmStackEntryNumber) result.getStack().get(0);
    return electionId.getNumber();
  }

  public BigInteger getReturnedStake(String validatorWalletHex) {
    Deque<String> params = new ArrayDeque<>();
    params.offer("[num," + new BigInteger(validatorWalletHex.toLowerCase(), 16) + "]");
    RunResult result = runMethod(ELECTION_ADDRESS, "compute_returned_stake", params);
    TvmStackEntryNumber stake = (TvmStackEntryNumber) result.getStack().get(0);
    return stake.getNumber();
  }
}
