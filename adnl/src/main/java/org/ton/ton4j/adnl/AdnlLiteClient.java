package org.ton.ton4j.adnl;

import static java.util.Objects.nonNull;

import java.io.File;
import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.ton.ton4j.adnl.globalconfig.LiteServers;
import org.ton.ton4j.adnl.globalconfig.TonGlobalConfig;
import org.ton.ton4j.address.Address;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellSlice;
import org.ton.ton4j.cell.TonHashMapE;
import org.ton.ton4j.tl.liteserver.queries.*;
import org.ton.ton4j.tl.liteserver.responses.*;
import org.ton.ton4j.tl.liteserver.responses.AccountState;
import org.ton.ton4j.tl.liteserver.responses.AllShardsInfo;
import org.ton.ton4j.tl.liteserver.responses.BlockData;
import org.ton.ton4j.tl.liteserver.responses.BlockHeader;
import org.ton.ton4j.tl.liteserver.responses.BlockIdExt;
import org.ton.ton4j.tlb.*;
import org.ton.ton4j.tlb.JettonBridgeParamsV1;
import org.ton.ton4j.tlb.OracleBridgeParams;
import org.ton.ton4j.tlb.Validators;
import org.ton.ton4j.utils.Utils;

/**
 * Native ADNL Lite Client implementation for TON blockchain Uses TCP transport to communicate with
 * lite-servers
 */
@Slf4j
public class AdnlLiteClient {

  public static final Address ELECTION_ADDRESS =
      Address.of("-1:3333333333333333333333333333333333333333333333333333333333333333");

  /**
   * Create a new builder
   *
   * @return Builder
   */
  public static Builder builder() {
    return new Builder();
  }

  private final AdnlTcpTransport transport;
  private final ScheduledExecutorService pingScheduler;
  private volatile boolean connected = false;
  private final TonGlobalConfig globalConfig;
  private final int liteServerIndex;
  private final boolean useServerRotation;
  private final int maxRetries;
  private final int queryTimeout;
  private final AtomicInteger currentServerIndex = new AtomicInteger(0);

  private AdnlLiteClient(Builder builder) {
    this.transport =
        builder.client != null ? new AdnlTcpTransport(builder.client) : new AdnlTcpTransport();
    this.pingScheduler = Executors.newSingleThreadScheduledExecutor();
    this.globalConfig = builder.globalConfig;
    this.liteServerIndex = builder.liteServerIndex;
    this.useServerRotation = builder.useServerRotation;
    this.maxRetries = builder.maxRetries;
    this.queryTimeout = builder.queryTimeout;

    if (this.liteServerIndex >= 0) {
      this.currentServerIndex.set(this.liteServerIndex);
    }
  }

  /**
   * Connect to liteserver
   *
   * @param host Server host
   * @param port Server port
   * @param serverPublicKeyBase64 Server's Ed25519 public key (base64 encoded)
   * @throws Exception if connection fails
   */
  private void connect(String host, int port, String serverPublicKeyBase64) throws Exception {
    byte[] serverPublicKey = Base64.getDecoder().decode(serverPublicKeyBase64);
    transport.connect(host, port, serverPublicKey);
    connected = true;

    // Start ping scheduler (every 5 seconds as per specification)
    startPingScheduler();

    log.info("Connected to lite-server " + host + ":" + port);
  }

  /**
   * Connect to a lite server from the global config
   *
   * @param liteServer The lite server to connect to
   * @throws Exception if connection fails
   */
  private void connect(LiteServers liteServer) throws Exception {
    connect(
        Utils.int2ip(liteServer.getIp()), (int) liteServer.getPort(), liteServer.getId().getKey());
  }

  /**
   * Connect to a lite server from the global config using the current server index
   *
   * @throws Exception if connection fails
   */
  private void connect() throws Exception {
    if (globalConfig == null
        || globalConfig.getLiteservers() == null
        || globalConfig.getLiteservers().length == 0) {
      throw new IllegalStateException("No lite servers available in global config");
    }

    int serverIndex = currentServerIndex.get();
    if (serverIndex >= globalConfig.getLiteservers().length) {
      serverIndex = 0;
      currentServerIndex.set(0);
    }

    connect(globalConfig.getLiteservers()[serverIndex]);
  }

  /**
   * Connect to a lite server with retry mechanism If connection fails, it will try to connect to
   * another lite server from the config
   *
   * @throws Exception if all connection attempts fail
   */
  private void connectWithRetry() throws Exception {
    Exception lastException = null;
    int retries = 0;

    while (retries < maxRetries) {
      try {
        connect();
        return;
      } catch (Exception e) {
        lastException = e;
        log.warn("Connection failed (attempt {}/{}): {}", retries + 1, maxRetries, e.getMessage());

        if (!useServerRotation
            || globalConfig == null
            || globalConfig.getLiteservers() == null
            || globalConfig.getLiteservers().length <= 1
            || liteServerIndex >= 0) {
          // No server rotation or only one server available or fixed server index
          retries++;
          continue;
        }

        // Try next server
        int nextIndex = (currentServerIndex.get() + 1) % globalConfig.getLiteservers().length;
        currentServerIndex.set(nextIndex);
        log.info("Trying next lite-server at index: {}", nextIndex);

        retries++;
      }
    }

    throw new Exception(
        "Failed to connect after "
            + maxRetries
            + " attempts. Last error: "
            + (lastException != null ? lastException.getMessage() : "unknown error"));
  }

  /** Start ping scheduler to maintain connection */
  private void startPingScheduler() {
    pingScheduler.scheduleAtFixedRate(
        () -> {
          try {
            if (connected && transport.isConnected()) {
              transport.ping().get(5, TimeUnit.SECONDS);
              //              log.info("Ping successful");
            }
          } catch (Exception e) {
            log.warn("Adnl tcp.Ping failed: ", e);
            // Connection might be lost, could implement reconnection logic here
          }
        },
        5,
        5,
        TimeUnit.SECONDS);
  }

  /**
   * Get masterchain info
   *
   * @return MasterchainInfo
   * @throws Exception if query fails
   */
  public MasterchainInfo getMasterchainInfo() throws Exception {
    return executeWithRetry(
        () -> {
          if (!connected || !transport.isConnected()) {
            throw new IllegalStateException("Not connected to lite-server");
          }
          byte[] queryBytes = LiteServerQuery.pack(MasterchainInfoQuery.builder().build());

          LiteServerAnswer response;
          response = transport.query(queryBytes).get(queryTimeout, TimeUnit.SECONDS);
          try {
            return (MasterchainInfo) response;
          } catch (Exception e) {
            if (response instanceof LiteServerError) {
              throw new Exception(((LiteServerError) response).getMessage());
            }
            throw e;
          }
        });
  }

  public MasterchainInfoExt getMasterchainInfoExt(int mode) throws Exception {
    return executeWithRetry(
        () -> {
          if (!connected || !transport.isConnected()) {
            throw new IllegalStateException("Not connected to lite-server");
          }
          byte[] queryBytes =
              LiteServerQuery.pack(MasterchainInfoExtQuery.builder().mode(mode).build());

          LiteServerAnswer response;
          response = transport.query(queryBytes).get(queryTimeout, TimeUnit.SECONDS);
          try {
            return (MasterchainInfoExt) response;
          } catch (Exception e) {
            if (response instanceof LiteServerError) {
              throw new Exception(((LiteServerError) response).getMessage());
            }
            throw e;
          }
        });
  }

  public CurrentTime getTime() throws Exception {
    return executeWithRetry(
        () -> {
          if (!connected || !transport.isConnected()) {
            throw new IllegalStateException("Not connected to lite-server");
          }

          byte[] queryBytes = LiteServerQuery.pack(CurrentTimeQuery.builder().build());

          LiteServerAnswer response =
              transport.query(queryBytes).get(queryTimeout, TimeUnit.SECONDS);
          try {
            return (CurrentTime) response;
          } catch (Exception e) {
            if (response instanceof LiteServerError) {
              throw new Exception(((LiteServerError) response).getMessage());
            }
            throw e;
          }
        });
  }

  public Version getVersion() throws Exception {
    return executeWithRetry(
        () -> {
          if (!connected || !transport.isConnected()) {
            throw new IllegalStateException("Not connected to lite-server");
          }

          byte[] queryBytes = LiteServerQuery.pack(VersionQuery.builder().build());

          LiteServerAnswer response =
              transport.query(queryBytes).get(queryTimeout, TimeUnit.SECONDS);
          try {
            return (Version) response;
          } catch (Exception e) {
            if (response instanceof LiteServerError) {
              throw new Exception(((LiteServerError) response).getMessage());
            }
            throw e;
          }
        });
  }

  public ConfigInfo getConfigAll(BlockIdExt id, int mode) throws Exception {
    return executeWithRetry(
        () -> {
          if (!connected || !transport.isConnected()) {
            throw new IllegalStateException("Not connected to lite-server");
          }

          byte[] queryBytes =
              LiteServerQuery.pack(ConfigAllQuery.builder().mode(mode).id(id).build());

          LiteServerAnswer response =
              transport.query(queryBytes).get(queryTimeout, TimeUnit.SECONDS);
          try {
            return (ConfigInfo) response;
          } catch (Exception e) {
            if (response instanceof LiteServerError) {
              throw new Exception(((LiteServerError) response).getMessage());
            }
            throw e;
          }
        });
  }

  public ConfigInfo getConfigParams(BlockIdExt id, int mode, int[] paramList) throws Exception {
    return executeWithRetry(
        () -> {
          if (!connected || !transport.isConnected()) {
            throw new IllegalStateException("Not connected to lite-server");
          }

          byte[] queryBytes =
              LiteServerQuery.pack(
                  ConfigParamsQuery.builder().mode(mode).id(id).paramList(paramList).build());

          LiteServerAnswer response =
              transport.query(queryBytes).get(queryTimeout, TimeUnit.SECONDS);
          try {
            return (ConfigInfo) response;
          } catch (Exception e) {
            if (response instanceof LiteServerError) {
              throw new Exception(((LiteServerError) response).getMessage());
            }
            throw e;
          }
        });
  }

  /** config address */
  public ConfigParams0 getConfigParam0() {
    try {
      Cell c = getConfigParamCell(0);
      return ConfigParams0.deserialize(CellSlice.beginParse(c));
    } catch (Throwable e) {
      return ConfigParams0.builder().configAddr(BigInteger.ZERO).build();
    }
  }

  /** elector address */
  public ConfigParams1 getConfigParam1() {
    try {
      Cell c = getConfigParamCell(1);
      return ConfigParams1.deserialize(CellSlice.beginParse(c));
    } catch (Throwable e) {
      return ConfigParams1.builder().electorAddr(BigInteger.ONE.negate()).build();
    }
  }

  /** minter address */
  public ConfigParams2 getConfigParam2() {
    try {
      Cell c = getConfigParamCell(2);
      return ConfigParams2.deserialize(CellSlice.beginParse(c));
    } catch (Throwable e) {
      return ConfigParams2.builder().minterAddr(BigInteger.ONE.negate()).build();
    }
  }

  /** fee collector address */
  public ConfigParams3 getConfigParam3() {
    try {
      Cell c = getConfigParamCell(3);
      return ConfigParams3.deserialize(CellSlice.beginParse(c));
    } catch (Throwable e) {
      return ConfigParams3.builder().build();
    }
  }

  /** dns root address */
  public ConfigParams4 getConfigParam4() {
    try {
      Cell c = getConfigParamCell(4);
      return ConfigParams4.deserialize(CellSlice.beginParse(c));
    } catch (Throwable e) {
      return ConfigParams4.builder().build();
    }
  }

  /** burning_config */
  public ConfigParams5 getConfigParam5() {
    try {
      Cell c = getConfigParamCell(5);
      return ConfigParams5.deserialize(CellSlice.beginParse(c));
    } catch (Throwable e) {
      return ConfigParams5.builder().build();
    }
  }

  /** mint_new_price:Grams mint_add_price:Grams */
  public ConfigParams6 getConfigParam6() {
    try {
      Cell c = getConfigParamCell(6);
      return ConfigParams6.deserialize(CellSlice.beginParse(c));
    } catch (Throwable e) {
      return ConfigParams6.builder().build();
    }
  }

  /**
   * capabilities#c4 version:uint32 capabilities:uint64 = GlobalVersion; _ GlobalVersion =
   * ConfigParam 8;
   */
  public ConfigParams8 getConfigParam8() {
    try {
      Cell c = getConfigParamCell(8);
      return ConfigParams8.deserialize(CellSlice.beginParse(c));
    } catch (Throwable e) {
      return ConfigParams8.builder().build();
    }
  }

  /** mandatory_params */
  public ConfigParams9 getConfigParam9() {
    try {
      Cell c = getConfigParamCell(9);
      return ConfigParams9.deserialize(CellSlice.beginParse(c));
    } catch (Throwable e) {
      return ConfigParams9.builder().build();
    }
  }

  /** critical_params */
  public ConfigParams10 getConfigParam10() {
    try {
      Cell c = getConfigParamCell(10);
      return ConfigParams10.deserialize(CellSlice.beginParse(c));
    } catch (Throwable e) {
      return ConfigParams10.builder().build();
    }
  }

  /**
   * cfg_vote_setup#91 normal_params:^ConfigProposalSetup critical_params:^ConfigProposalSetup =
   * ConfigVotingSetup; _ ConfigVotingSetup = ConfigParam 11;
   */
  public ConfigParams11 getConfigParam11() {
    try {
      Cell c = getConfigParamCell(11);
      return ConfigParams11.deserialize(CellSlice.beginParse(c));
    } catch (Throwable e) {
      return ConfigParams11.builder().build();
    }
  }

  /** workchains */
  public ConfigParams12 getConfigParam12() {
    try {
      Cell c = getConfigParamCell(12);
      return ConfigParams12.deserialize(CellSlice.beginParse(c));
    } catch (Throwable e) {
      return ConfigParams12.builder().build();
    }
  }

  /** ComplaintPricing */
  public ConfigParams13 getConfigParam13() {
    try {
      Cell c = getConfigParamCell(13);
      return ConfigParams13.deserialize(CellSlice.beginParse(c));
    } catch (Throwable e) {
      return ConfigParams13.builder().build();
    }
  }

  /** BlockCreateFees */
  public ConfigParams14 getConfigParam14() {
    try {
      Cell c = getConfigParamCell(14);
      return ConfigParams14.deserialize(CellSlice.beginParse(c));
    } catch (Throwable e) {
      return ConfigParams14.builder().build();
    }
  }

  /** election timing */
  public ConfigParams15 getConfigParam15() {
    try {
      Cell c = getConfigParamCell(15);
      return ConfigParams15.deserialize(CellSlice.beginParse(c));
    } catch (Throwable e) {
      return ConfigParams15.builder().build();
    }
  }

  /** max min validators */
  public ConfigParams16 getConfigParam16() {
    try {
      Cell c = getConfigParamCell(16);
      return ConfigParams16.deserialize(CellSlice.beginParse(c));
    } catch (Throwable e) {
      return ConfigParams16.builder().build();
    }
  }

  /** max min stake */
  public ConfigParams17 getConfigParam17() {
    try {
      Cell c = getConfigParamCell(17);
      return ConfigParams17.deserialize(CellSlice.beginParse(c));
    } catch (Throwable e) {
      return ConfigParams17.builder().build();
    }
  }

  /** storage prices */
  public ConfigParams18 getConfigParam18() {
    try {
      Cell c = getConfigParamCell(18);
      return ConfigParams18.deserialize(CellSlice.beginParse(c));
    } catch (Throwable e) {
      return ConfigParams18.builder().build();
    }
  }

  /** GasLimitsPrices masterchain */
  public ConfigParams20 getConfigParam20() {
    try {
      Cell c = getConfigParamCell(20);
      return ConfigParams20.deserialize(CellSlice.beginParse(c));
    } catch (Throwable e) {
      return ConfigParams20.builder().build();
    }
  }

  /** GasLimitsPrices workchains */
  public ConfigParams21 getConfigParam21() {
    try {
      Cell c = getConfigParamCell(21);
      return ConfigParams21.deserialize(CellSlice.beginParse(c));
    } catch (Throwable e) {
      return ConfigParams21.builder().build();
    }
  }

  /** BlockLimits masterchain */
  public ConfigParams22 getConfigParam22() {
    try {
      Cell c = getConfigParamCell(22);
      return ConfigParams22.deserialize(CellSlice.beginParse(c));
    } catch (Throwable e) {
      return ConfigParams22.builder().build();
    }
  }

  /** BlockLimits workchains */
  public ConfigParams23 getConfigParam23() {
    try {
      Cell c = getConfigParamCell(23);
      return ConfigParams23.deserialize(CellSlice.beginParse(c));
    } catch (Throwable e) {
      return ConfigParams23.builder().build();
    }
  }

  /** MsgForwardPrices masterchain */
  public ConfigParams24 getConfigParam24() {
    try {
      Cell c = getConfigParamCell(24);
      return ConfigParams24.deserialize(CellSlice.beginParse(c));
    } catch (Throwable e) {
      return ConfigParams24.builder().build();
    }
  }

  /** MsgForwardPrices */
  public ConfigParams25 getConfigParam25() {
    try {
      Cell c = getConfigParamCell(25);
      return ConfigParams25.deserialize(CellSlice.beginParse(c));
    } catch (Throwable e) {
      return ConfigParams25.builder().build();
    }
  }

  /** CatchainConfig */
  public ConfigParams28 getConfigParam28() {
    try {
      Cell c = getConfigParamCell(28);
      return ConfigParams28.deserialize(CellSlice.beginParse(c));
    } catch (Throwable e) {
      return ConfigParams28.builder().build();
    }
  }

  /** ConsensusConfig */
  public ConfigParams29 getConfigParam29() {
    try {
      Cell c = getConfigParamCell(29);
      return ConfigParams29.deserialize(CellSlice.beginParse(c));
    } catch (Throwable e) {
      return ConfigParams29.builder().build();
    }
  }

  /** fundamental_smc_addr */
  public ConfigParams31 getConfigParam31() {
    try {
      Cell c = getConfigParamCell(31);
      return ConfigParams31.deserialize(CellSlice.beginParse(c));
    } catch (Throwable e) {
      return ConfigParams31.builder().build();
    }
  }

  /** prev_validators */
  public ConfigParams32 getConfigParam32() {
    try {
      Cell c = getConfigParamCell(32);
      return ConfigParams32.deserialize(CellSlice.beginParse(c));
    } catch (Throwable e) {
      return ConfigParams32.builder().prevValidatorSet(Validators.builder().build()).build();
    }
  }

  /** prev_temp_validators */
  public ConfigParams33 getConfigParam33() {
    try {
      Cell c = getConfigParamCell(33);
      return ConfigParams33.deserialize(CellSlice.beginParse(c));
    } catch (Throwable e) {
      return ConfigParams33.builder().prevTempValidatorSet(Validators.builder().build()).build();
    }
  }

  /** cur_validators */
  public ConfigParams34 getConfigParam34() {
    try {
      Cell c = getConfigParamCell(34);
      return ConfigParams34.deserialize(CellSlice.beginParse(c));
    } catch (Throwable e) {
      return ConfigParams34.builder().currValidatorSet(Validators.builder().build()).build();
    }
  }

  /** cur_temp_validators */
  public ConfigParams35 getConfigParam35() {
    try {
      Cell c = getConfigParamCell(35);
      return ConfigParams35.deserialize(CellSlice.beginParse(c));
    } catch (Throwable e) {
      return ConfigParams35.builder().currTempValidatorSet(Validators.builder().build()).build();
    }
  }

  /** next_validators */
  public ConfigParams36 getConfigParam36() {
    try {
      Cell c = getConfigParamCell(36);
      return ConfigParams36.deserialize(CellSlice.beginParse(c));
    } catch (Throwable e) {
      return ConfigParams36.builder().nextValidatorSet(Validators.builder().build()).build();
    }
  }

  /** next_temp_validators */
  public ConfigParams37 getConfigParam37() {
    try {
      Cell c = getConfigParamCell(37);
      return ConfigParams37.deserialize(CellSlice.beginParse(c));
    } catch (Throwable e) {
      return ConfigParams37.builder().nextTempValidatorSet(Validators.builder().build()).build();
    }
  }

  private Cell getConfigParamCell(int val) throws Exception {
    ConfigInfo configInfo = getConfigAll(getMasterchainInfo().getLast(), 0);
    return (Cell) configInfo.getConfigParams().getConfig().elements.get(BigInteger.valueOf(val));
  }

  /** ValidatorSignedTempKey */
  public ConfigParams39 getConfigParam39() {
    try {
      Cell c = getConfigParamCell(39);
      return ConfigParams39.deserialize(CellSlice.beginParse(c));
    } catch (Throwable e) {
      return ConfigParams39.builder().validatorSignedTemp(new TonHashMapE(0)).build();
    }
  }

  /** MisbehaviourPunishmentConfig */
  public ConfigParams40 getConfigParam40() {
    try {
      Cell c = getConfigParamCell(40);
      return ConfigParams40.deserialize(CellSlice.beginParse(c));
    } catch (Throwable e) {
      log.error("Error getting config params 40");
      return ConfigParams40.builder().build();
    }
  }

  /** SuspendedAddressList */
  public ConfigParams44 getConfigParam44() {
    try {
      Cell c = getConfigParamCell(44);
      return ConfigParams44.deserialize(CellSlice.beginParse(c));
    } catch (Throwable e) {
      return ConfigParams44.builder().build();
    }
  }

  /** PrecompiledContractsConfig */
  public ConfigParams45 getConfigParam45() {
    try {
      Cell c = getConfigParamCell(45);
      return ConfigParams45.deserialize(CellSlice.beginParse(c));
    } catch (Throwable e) {
      return ConfigParams45.builder().build();
    }
  }

  /** Ethereum bridges */
  public ConfigParams71 getConfigParam71() {
    try {
      Cell c = getConfigParamCell(71);
      return ConfigParams71.deserialize(CellSlice.beginParse(c));
    } catch (Throwable e) {
      return ConfigParams71.builder().build();
    }
  }

  /** Binance Smart Chain bridges */
  public ConfigParams72 getConfigParam72() {
    try {
      Cell c = getConfigParamCell(72);
      return ConfigParams72.deserialize(CellSlice.beginParse(c));
    } catch (Throwable e) {
      return ConfigParams72.builder().build();
    }
  }

  /** Polygon bridges */
  public ConfigParams73 getConfigParam73() {
    try {
      Cell c = getConfigParamCell(73);
      return ConfigParams73.deserialize(CellSlice.beginParse(c));
    } catch (Throwable e) {
      return ConfigParams73.builder().polygonBridge(OracleBridgeParams.builder().build()).build();
    }
  }

  /** ETH-&gt;TON token bridges */
  public ConfigParams79 getConfigParam79() {
    try {
      Cell c = getConfigParamCell(79);
      return ConfigParams79.deserialize(CellSlice.beginParse(c));
    } catch (Throwable e) {
      return ConfigParams79.builder().build();
    }
  }

  /** BNB-&gt;TON token bridges */
  public ConfigParams81 getConfigParam81() {
    try {
      Cell c = getConfigParamCell(81);
      return ConfigParams81.deserialize(CellSlice.beginParse(c));
    } catch (Throwable e) {
      return ConfigParams81.builder().build();
    }
  }

  /** Polygon-&gt;TON token bridges */
  public ConfigParams82 getConfigParam82() {
    try {
      Cell c = getConfigParamCell(82);
      return ConfigParams82.deserialize(CellSlice.beginParse(c));
    } catch (Throwable e) {
      return ConfigParams82.builder()
          .polygonTonTokenBridge(JettonBridgeParamsV1.builder().build())
          .build();
    }
  }

  public BlockData getBlock(BlockIdExt id) throws Exception {
    return executeWithRetry(
        () -> {
          if (!connected || !transport.isConnected()) {
            throw new IllegalStateException("Not connected to lite-server");
          }

          byte[] queryBytes = LiteServerQuery.pack(BlockQuery.builder().id(id).build());

          LiteServerAnswer response =
              transport.query(queryBytes).get(queryTimeout, TimeUnit.SECONDS);
          try {
            return (BlockData) response;
          } catch (Exception e) {
            if (response instanceof LiteServerError) {
              throw new Exception(((LiteServerError) response).getMessage());
            }
            throw e;
          }
        });
  }

  public BlockState getBlockState(BlockIdExt id) throws Exception {
    return executeWithRetry(
        () -> {
          if (!connected || !transport.isConnected()) {
            throw new IllegalStateException("Not connected to lite-server");
          }

          byte[] queryBytes = LiteServerQuery.pack(BlockStateQuery.builder().id(id).build());

          LiteServerAnswer response =
              transport.query(queryBytes).get(queryTimeout, TimeUnit.SECONDS);
          try {
            return (BlockState) response;
          } catch (Exception e) {
            if (response instanceof LiteServerError) {
              throw new Exception(((LiteServerError) response).getMessage());
            }
            throw e;
          }
        });
  }

  public BlockHeader getBlockHeader(BlockIdExt id, int mode) throws Exception {
    return executeWithRetry(
        () -> {
          if (!connected || !transport.isConnected()) {
            throw new IllegalStateException("Not connected to lite-server");
          }

          byte[] queryBytes =
              LiteServerQuery.pack(BlockHeaderQuery.builder().id(id).mode(mode).build());

          LiteServerAnswer response =
              transport.query(queryBytes).get(queryTimeout, TimeUnit.SECONDS);
          try {
            return (BlockHeader) response;
          } catch (Exception e) {
            if (response instanceof LiteServerError) {
              throw new Exception(((LiteServerError) response).getMessage());
            }
            throw e;
          }
        });
  }

  public ValidatorStats getValidatorStats(
      BlockIdExt id, int mode, int limit, byte[] startAfter, int modifiedAfter) throws Exception {
    return executeWithRetry(
        () -> {
          if (!connected || !transport.isConnected()) {
            throw new IllegalStateException("Not connected to lite-server");
          }

          byte[] queryBytes =
              LiteServerQuery.pack(
                  ValidatorStatsQuery.builder()
                      .id(id)
                      .mode(mode)
                      .limit(limit)
                      .startAfter(startAfter)
                      .modifiedAfter(modifiedAfter)
                      .build());

          LiteServerAnswer response =
              transport.query(queryBytes).get(queryTimeout, TimeUnit.SECONDS);
          try {
            return (ValidatorStats) response;
          } catch (Exception e) {
            if (response instanceof LiteServerError) {
              throw new Exception(((LiteServerError) response).getMessage());
            }
            throw e;
          }
        });
  }

  public ShardBlockProof getShardBlockProof(BlockIdExt id) throws Exception {
    return executeWithRetry(
        () -> {
          if (!connected || !transport.isConnected()) {
            throw new IllegalStateException("Not connected to lite-server");
          }

          byte[] queryBytes = LiteServerQuery.pack(ShardBlockProofQuery.builder().id(id).build());

          LiteServerAnswer response =
              transport.query(queryBytes).get(queryTimeout, TimeUnit.SECONDS);
          try {
            return (ShardBlockProof) response;
          } catch (Exception e) {
            if (response instanceof LiteServerError) {
              throw new Exception(((LiteServerError) response).getMessage());
            }
            throw e;
          }
        });
  }

  public PartialBlockProof getBlockProof(int mode, BlockIdExt knownBlock, BlockIdExt targetBlock)
      throws Exception {
    return executeWithRetry(
        () -> {
          if (!connected || !transport.isConnected()) {
            throw new IllegalStateException("Not connected to lite-server");
          }

          byte[] queryBytes =
              LiteServerQuery.pack(
                  BlockProofQuery.builder()
                      .mode(mode)
                      .knownBlock(knownBlock)
                      .targetBlock(targetBlock)
                      .build());

          LiteServerAnswer response =
              transport.query(queryBytes).get(queryTimeout, TimeUnit.SECONDS);
          try {
            return (PartialBlockProof) response;
          } catch (Exception e) {
            if (response instanceof LiteServerError) {
              throw new Exception(((LiteServerError) response).getMessage());
            }
            throw e;
          }
        });
  }

  public BigInteger getBalance(Address address) {
    try {
      return getAccount(address).getAccountStorage().getBalance().getCoins();
    } catch (Throwable e) {
      return BigInteger.ZERO;
    }
  }

  public Account getAccount(Address address) throws Exception {
    return getAccountState(getMasterchainInfo().getLast(), address).getAccount();
  }

  public String getAccountStatus(Address address) throws Exception {
    Account account = getAccountState(getMasterchainInfo().getLast(), address).getAccount();
    if (account == null) {
      return "UNINIT";
    }
    return account.getAccountStorage().getAccountStatus();
  }

  public AccountState getAccountState(BlockIdExt id, Address accountAddress) throws Exception {
    return executeWithRetry(
        () -> {
          if (!connected || !transport.isConnected()) {
            throw new IllegalStateException("Not connected to lite-server");
          }

          byte[] queryBytes =
              LiteServerQuery.pack(
                  AccountStateQuery.builder().id(id).account(accountAddress).build());

          LiteServerAnswer response =
              transport.query(queryBytes).get(queryTimeout, TimeUnit.SECONDS);
          try {
            return (AccountState) response;
          } catch (Exception e) {
            if (response instanceof LiteServerError) {
              throw new Exception(((LiteServerError) response).getMessage());
            }
            throw e;
          }
        });
  }

  public AccountState getAccountStatePruned(BlockIdExt id, Address accountAddress)
      throws Exception {
    return executeWithRetry(
        () -> {
          if (!connected || !transport.isConnected()) {
            throw new IllegalStateException("Not connected to lite-server");
          }

          byte[] queryBytes =
              LiteServerQuery.pack(
                  AccountStatePrunedQuery.builder().id(id).account(accountAddress).build());

          LiteServerAnswer response =
              transport.query(queryBytes).get(queryTimeout, TimeUnit.SECONDS);
          try {
            return (AccountState) response;
          } catch (Exception e) {
            if (response instanceof LiteServerError) {
              throw new Exception(((LiteServerError) response).getMessage());
            }
            throw e;
          }
        });
  }

  public long getSeqno(Address accountAddress) throws Exception {
    try {
      RunMethodResult runMethodResult =
          runMethod(
              getMasterchainInfo().getLast(),
              4,
              accountAddress,
              Utils.calculateMethodId("seqno"),
              new byte[0]);
      if (runMethodResult.getExitCode() != 0) {
        throw new Error("method seqno returned an exit code " + runMethodResult.getExitCode());
      }
      VmStack vmStack =
          VmStack.deserialize(CellSlice.beginParse(Cell.fromBoc(runMethodResult.result)));
      return VmStackValueTinyInt.deserialize(
              CellSlice.beginParse(vmStack.getStack().getTos().get(0).toCell()))
          .getValue()
          .longValue();
    } catch (Exception e) {
      throw new Error("cannot execute seqno");
    }
  }

  public BigInteger getPublicKey(Address accountAddress) {
    try {
      RunMethodResult runMethodResult =
          runMethod(
              getMasterchainInfo().getLast(),
              4,
              accountAddress,
              Utils.calculateMethodId("get_public_key"),
              new byte[0]);
      if (runMethodResult.getExitCode() != 0) {
        throw new Error(
            "method get_public_key returned an exit code " + runMethodResult.getExitCode());
      }
      VmStack vmStack =
          VmStack.deserialize(CellSlice.beginParse(Cell.fromBoc(runMethodResult.result)));
      return VmStackValueInt.deserialize(
              CellSlice.beginParse(vmStack.getStack().getTos().get(0).toCell()))
          .getValue();
    } catch (Exception e) {
      throw new Error("cannot execute get_public_key");
    }
  }

  public long getSubWalletId(Address accountAddress) {
    try {
      RunMethodResult runMethodResult =
          runMethod(
              getMasterchainInfo().getLast(),
              4,
              accountAddress,
              Utils.calculateMethodId("get_subwallet_id"),
              new byte[0]);
      if (runMethodResult.getExitCode() != 0) {
        throw new Error(
            "method get_subwallet_id returned an exit code " + runMethodResult.getExitCode());
      }

      VmStack vmStack =
          VmStack.deserialize(CellSlice.beginParse(Cell.fromBoc(runMethodResult.result)));
      return VmStackValueTinyInt.deserialize(
              CellSlice.beginParse(vmStack.getStack().getTos().get(0).toCell()))
          .getValue()
          .longValue();
    } catch (Exception e) {
      throw new Error("cannot execute get_subwallet_id");
    }
  }

  public long computeReturnedStake(Address validatorAddress) {
    try {
      VmStack vmStackParams =
          VmStack.builder()
              .depth(1)
              .stack(
                  VmStackList.builder()
                      .tos(
                          List.of(
                              VmStackValueInt.builder()
                                  .value(validatorAddress.toBigInteger())
                                  .build()))
                      .build())
              .build();

      RunMethodResult runMethodResult =
          runMethod(
              getMasterchainInfo().getLast(),
              4,
              Address.of("-1:3333333333333333333333333333333333333333333333333333333333333333"),
              Utils.calculateMethodId("compute_returned_stake"),
              vmStackParams.toCell().toBoc());
      if (runMethodResult.getExitCode() != 0) {
        throw new Error(
            "method compute_returned_stake returned an exit code " + runMethodResult.getExitCode());
      }
      VmStack vmStackResult =
          VmStack.deserialize(CellSlice.beginParse(Cell.fromBoc(runMethodResult.result)));
      log.info("vmStackResult: " + vmStackResult);
      return VmStackValueTinyInt.deserialize(
              CellSlice.beginParse(vmStackResult.getStack().getTos().get(0).toCell()))
          .getValue()
          .longValue();
    } catch (Exception e) {
      throw new Error("cannot execute compute_returned_stake");
    }
  }

  public RunMethodResult runMethod(Address accountAddress, String methodName) {
    try {
      return runMethod(
          getMasterchainInfo().getLast(),
          4,
          accountAddress,
          Utils.calculateMethodId(methodName),
          new byte[0]);
    } catch (Exception e) {
      throw new Error("cannot execute runMethod on " + methodName);
    }
  }

  public RunMethodResult runMethod(Address accountAddress, String methodName, byte[] params)
      throws Exception {
    try {
      return runMethod(
          getMasterchainInfo().getLast(),
          4,
          accountAddress,
          Utils.calculateMethodId(methodName),
          params);
    } catch (Exception e) {
      throw new Error("cannot execute runMethod on " + methodName);
    }
  }

  public RunMethodResult runMethod(
      Address accountAddress, String methodName, VmStackValue... params) {
    try {
      List<VmStackValue> vmStackValuesReversed = Arrays.asList(params);
      //      Collections.reverse(vmStackValuesReversed);
      VmStack vmStackParams =
          VmStack.builder()
              .depth(vmStackValuesReversed.size())
              .stack(VmStackList.builder().tos(vmStackValuesReversed).build())
              .build();

      return runMethod(
          getMasterchainInfo().getLast(),
          4,
          accountAddress,
          Utils.calculateMethodId(methodName),
          vmStackParams.toCell().toBoc());
    } catch (Exception e) {
      throw new Error("cannot execute runMethod on " + methodName);
    }
  }

  public RunMethodResult runMethod(
      BlockIdExt id, int mode, Address accountAddress, long methodId, byte[] methodParams)
      throws Exception {
    return executeWithRetry(
        () -> {
          if (!connected || !transport.isConnected()) {
            throw new IllegalStateException("Not connected to lite-server");
          }

          byte[] queryBytes =
              LiteServerQuery.pack(
                  RunSmcMethodQuery.builder()
                      .mode(mode)
                      .id(id)
                      .account(accountAddress)
                      .methodId(methodId)
                      .params(methodParams)
                      .build());

          LiteServerAnswer response =
              transport.query(queryBytes).get(queryTimeout, TimeUnit.SECONDS);
          try {
            return (RunMethodResult) response;
          } catch (Exception e) {
            if (response instanceof LiteServerError) {
              throw new Exception(((LiteServerError) response).getMessage());
            }
            throw e;
          }
        });
  }

  public ShardInfo getShardInfo(BlockIdExt id, int workchain, long shard, boolean exact)
      throws Exception {
    return executeWithRetry(
        () -> {
          if (!connected || !transport.isConnected()) {
            throw new IllegalStateException("Not connected to lite-server");
          }

          byte[] queryBytes =
              LiteServerQuery.pack(
                  ShardInfoQuery.builder()
                      .id(id)
                      .workchain(workchain)
                      .shard(shard)
                      .exact(exact)
                      .build());

          LiteServerAnswer response =
              transport.query(queryBytes).get(queryTimeout, TimeUnit.SECONDS);
          try {
            return (ShardInfo) response;
          } catch (Exception e) {
            if (response instanceof LiteServerError) {
              throw new Exception(((LiteServerError) response).getMessage());
            }
            throw e;
          }
        });
  }

  public AllShardsInfo getAllShardsInfo(BlockIdExt id) throws Exception {
    return executeWithRetry(
        () -> {
          if (!connected || !transport.isConnected()) {
            throw new IllegalStateException("Not connected to lite-server");
          }

          byte[] queryBytes = LiteServerQuery.pack(AllShardsInfoQuery.builder().id(id).build());

          LiteServerAnswer response =
              transport.query(queryBytes).get(queryTimeout, TimeUnit.SECONDS);
          try {
            return (AllShardsInfo) response;
          } catch (Exception e) {
            if (response instanceof LiteServerError) {
              throw new Exception(((LiteServerError) response).getMessage());
            }
            throw e;
          }
        });
  }

  public TransactionInfo getOneTransaction(BlockIdExt id, Address accountAddress, long lt)
      throws Exception {
    return executeWithRetry(
        () -> {
          if (!connected || !transport.isConnected()) {
            throw new IllegalStateException("Not connected to lite-server");
          }

          byte[] queryBytes =
              LiteServerQuery.pack(
                  OneTransactionQuery.builder().id(id).account(accountAddress).lt(lt).build());

          LiteServerAnswer response =
              transport.query(queryBytes).get(queryTimeout, TimeUnit.SECONDS);
          try {
            return (TransactionInfo) response;
          } catch (Exception e) {
            if (response instanceof LiteServerError) {
              throw new Exception(((LiteServerError) response).getMessage());
            }
            throw e;
          }
        });
  }

  public TransactionList getTransactions(Address accountAddress, long lt, byte[] hash, int count)
      throws Exception {
    return executeWithRetry(
        () -> {
          if (!connected || !transport.isConnected()) {
            throw new IllegalStateException("Not connected to lite-server");
          }
          long tempLt = 0;
          byte[] tempHash = new byte[0];
          if ((lt == 0) || (hash == null)) {
            MasterchainInfo masterchainInfo = getMasterchainInfo();
            AccountState account = getAccountState(masterchainInfo.getLast(), accountAddress);
            if ((account.getShardAccounts() != null) && (!account.getShardAccounts().isEmpty())) {
              tempLt = account.getShardAccounts().get(0).getLastTransLt().longValue();
              tempHash = Utils.to32ByteArray(account.getShardAccounts().get(0).lastTransHash);
              log.info("found latest LT {}, hash {}", tempLt, Utils.bytesToHex(tempHash));
            }
          }

          byte[] queryBytes =
              LiteServerQuery.pack(
                  TransactionListQuery.builder()
                      .count(count)
                      .account(accountAddress)
                      .lt((tempLt != 0) ? tempLt : lt)
                      .hash((tempHash.length != 0) ? tempHash : hash)
                      .build());

          LiteServerAnswer response =
              transport.query(queryBytes).get(queryTimeout, TimeUnit.SECONDS);
          try {
            return (TransactionList) response;
          } catch (Exception e) {
            if (response instanceof LiteServerError) {
              throw new Exception(((LiteServerError) response).getMessage());
            }
            throw e;
          }
        });
  }

  public BlockHeader lookupBlock(BlockId id, int mode, long lt, int utime) throws Exception {
    return executeWithRetry(
        () -> {
          if (!connected || !transport.isConnected()) {
            throw new IllegalStateException("Not connected to lite-server");
          }

          byte[] queryBytes =
              LiteServerQuery.pack(
                  LookupBlockQuery.builder().id(id).mode(mode).lt(lt).utime(utime).build());

          LiteServerAnswer response =
              transport.query(queryBytes).get(queryTimeout, TimeUnit.SECONDS);
          try {
            return (BlockHeader) response;
          } catch (Exception e) {
            if (response instanceof LiteServerError) {
              throw new Exception(((LiteServerError) response).getMessage());
            }
            throw e;
          }
        });
  }

  public LookupBlockResult lookupBlockWithProof(
      int mode, BlockId id, BlockIdExt mcId, long lt, int utime) throws Exception {
    return executeWithRetry(
        () -> {
          if (!connected || !transport.isConnected()) {
            throw new IllegalStateException("Not connected to lite-server");
          }

          byte[] queryBytes =
              LiteServerQuery.pack(
                  LookupBlockWithProofQuery.builder()
                      .mode(mode)
                      .id(id)
                      .mcBlockId(mcId)
                      .lt(lt)
                      .utime(utime)
                      .build());

          LiteServerAnswer response =
              transport.query(queryBytes).get(queryTimeout, TimeUnit.SECONDS);
          try {
            return (LookupBlockResult) response;
          } catch (Exception e) {
            if (response instanceof LiteServerError) {
              throw new Exception(((LiteServerError) response).getMessage());
            }
            throw e;
          }
        });
  }

  public BlockTransactions listBlockTransactions(
      BlockIdExt id, int mode, int count, TransactionId3 transactionId3) throws Exception {
    return executeWithRetry(
        () -> {
          if (!connected || !transport.isConnected()) {
            throw new IllegalStateException("Not connected to lite-server");
          }

          byte[] queryBytes = // not to implement
              LiteServerQuery.pack(
                  ListBlockTransactionsQuery.builder()
                      .id(id)
                      .mode(mode)
                      .count(count)
                      .afterTx(transactionId3)
                      .build());

          LiteServerAnswer response =
              transport.query(queryBytes).get(queryTimeout, TimeUnit.SECONDS);
          try {
            return (BlockTransactions) response;
          } catch (Exception e) {
            if (response instanceof LiteServerError) {
              throw new Exception(((LiteServerError) response).getMessage());
            }
            throw e;
          }
        });
  }

  public BlockTransactionsExt listBlockTransactionsExt(
      BlockIdExt id,
      int mode,
      int count,
      TransactionId3 after,
      boolean reverseOrder,
      boolean wantProof)
      throws Exception {
    return executeWithRetry(
        () -> {
          if (!connected || !transport.isConnected()) {
            throw new IllegalStateException("Not connected to lite-server");
          }

          byte[] queryBytes =
              LiteServerQuery.pack(
                  ListBlockTransactionsExtQuery.builder()
                      .id(id)
                      .mode(mode)
                      .count(count)
                      .after(after)
                      .reverseOrder(reverseOrder)
                      .wantProof(wantProof)
                      .build());

          LiteServerAnswer response =
              transport.query(queryBytes).get(queryTimeout, TimeUnit.SECONDS);
          try {
            return (BlockTransactionsExt) response;
          } catch (Exception e) {
            if (response instanceof LiteServerError) {
              throw new Exception(((LiteServerError) response).getMessage());
            }
            throw e;
          }
        });
  }

  /** Close connection */
  public void close() {
    connected = false;
    pingScheduler.shutdown();
    transport.close();

    try {
      if (!pingScheduler.awaitTermination(5, TimeUnit.SECONDS)) {
        pingScheduler.shutdownNow();
      }
    } catch (InterruptedException e) {
      pingScheduler.shutdownNow();
      Thread.currentThread().interrupt();
    }
  }

  /**
   * Check if connected
   *
   * @return true if connected
   */
  public boolean isConnected() {
    return connected && transport.isConnected();
  }

  public DispatchQueueInfo getDispatchQueueInfo(
      BlockIdExt id, int mode, Address afterAddress, int maxAccounts, boolean wantProof)
      throws Exception {
    return executeWithRetry(
        () -> {
          if (!connected || !transport.isConnected()) {
            throw new IllegalStateException("Not connected to lite-server");
          }

          byte[] queryBytes =
              LiteServerQuery.pack(
                  DispatchQueueInfoQuery.builder()
                      .id(id)
                      .mode(mode)
                      .afterAddr(afterAddress)
                      .maxAccounts(maxAccounts)
                      .wantProof(wantProof)
                      .build());

          LiteServerAnswer response =
              transport.query(queryBytes).get(queryTimeout, TimeUnit.SECONDS);
          try {
            return (DispatchQueueInfo) response;
          } catch (Exception e) {
            if (response instanceof LiteServerError) {
              throw new Exception(((LiteServerError) response).getMessage());
            }
            throw e;
          }
        });
  }

  public DispatchQueueMessages getDispatchQueueMessages(
      BlockIdExt id,
      int mode,
      Address addr,
      long afterLt,
      int maxMessages,
      boolean wantProof,
      boolean oneAccount,
      boolean messageBoc)
      throws Exception {
    return executeWithRetry(
        () -> {
          if (!connected || !transport.isConnected()) {
            throw new IllegalStateException("Not connected to lite-server");
          }

          byte[] queryBytes =
              LiteServerQuery.pack(
                  DispatchQueueMessagesQuery.builder()
                      .id(id)
                      .mode(mode)
                      .addr(addr)
                      .afterLt(afterLt)
                      .maxMessages(maxMessages)
                      .wantProof(wantProof)
                      .oneAccount(oneAccount)
                      .messagesBoc(messageBoc)
                      .build());

          LiteServerAnswer response =
              transport.query(queryBytes).get(queryTimeout, TimeUnit.SECONDS);
          try {
            return (DispatchQueueMessages) response;
          } catch (Exception e) {
            if (response instanceof LiteServerError) {
              throw new Exception(((LiteServerError) response).getMessage());
            }
            throw e;
          }
        });
  }

  public LibraryResult getLibraries(List<byte[]> listLibraries) throws Exception {
    return executeWithRetry(
        () -> {
          if (!connected || !transport.isConnected()) {
            throw new IllegalStateException("Not connected to lite-server");
          }

          byte[] queryBytes =
              LiteServerQuery.pack(LibrariesQuery.builder().libraryList(listLibraries).build());

          LiteServerAnswer response =
              transport.query(queryBytes).get(queryTimeout, TimeUnit.SECONDS);
          try {
            return (LibraryResult) response;
          } catch (Exception e) {
            if (response instanceof LiteServerError) {
              throw new Exception(((LiteServerError) response).getMessage());
            }
            throw e;
          }
        });
  }

  public LibraryResultWithProof getLibrariesWithProof(
      BlockIdExt id, int mode, List<byte[]> listLibraries) throws Exception {
    return executeWithRetry(
        () -> {
          if (!connected || !transport.isConnected()) {
            throw new IllegalStateException("Not connected to lite-server");
          }

          byte[] queryBytes =
              LiteServerQuery.pack(
                  LibrariesWithProofQuery.builder()
                      .id(id)
                      .mode(mode)
                      .libraryList(listLibraries)
                      .build());

          LiteServerAnswer response =
              transport.query(queryBytes).get(queryTimeout, TimeUnit.SECONDS);
          try {
            return (LibraryResultWithProof) response;
          } catch (Exception e) {
            if (response instanceof LiteServerError) {
              throw new Exception(((LiteServerError) response).getMessage());
            }
            throw e;
          }
        });
  }

  public OutMsgQueueSizes getOutMsgQueueSizesQuery(int mode, int wc, long shard) throws Exception {
    return executeWithRetry(
        () -> {
          if (!connected || !transport.isConnected()) {
            throw new IllegalStateException("Not connected to lite-server");
          }

          byte[] queryBytes =
              LiteServerQuery.pack(
                  OutMsgQueueSizesQuery.builder().mode(mode).wc(wc).shard(shard).build());

          LiteServerAnswer response =
              transport.query(queryBytes).get(queryTimeout, TimeUnit.SECONDS);
          try {
            return (OutMsgQueueSizes) response;
          } catch (Exception e) {
            if (response instanceof LiteServerError) {
              throw new Exception(((LiteServerError) response).getMessage());
            }
            throw e;
          }
        });
  }

  public BlockOutMsgQueueSize getBlockOutMsgQueueSize(BlockIdExt id, int mode, boolean wantProof)
      throws Exception {
    return executeWithRetry(
        () -> {
          if (!connected || !transport.isConnected()) {
            throw new IllegalStateException("Not connected to lite-server");
          }

          byte[] queryBytes =
              LiteServerQuery.pack(
                  BlockOutMsgQueueSizeQuery.builder()
                      .id(id)
                      .mode(mode)
                      .wantProof(wantProof)
                      .build());

          LiteServerAnswer response =
              transport.query(queryBytes).get(queryTimeout, TimeUnit.SECONDS);
          try {
            return (BlockOutMsgQueueSize) response;
          } catch (Exception e) {
            if (response instanceof LiteServerError) {
              throw new Exception(((LiteServerError) response).getMessage());
            }
            throw e;
          }
        });
  }

  /**
   * Execute a query with retry mechanism
   *
   * @param <T> The return type of the query
   * @param supplier The query supplier
   * @return The query result
   * @throws Exception if all retries fail
   */
  private <T> T executeWithRetry(Callable<T> supplier) throws Exception {
    try {
      return supplier.call();
    } catch (IllegalStateException e) {
      // This is a connection failure (not connected to lite-server)
      // We should retry connecting to another server
      if (e.getMessage() != null && e.getMessage().contains("Not connected to lite-server")) {
        return handleConnectionFailure(supplier, e);
      }
      // For other IllegalStateException, just throw it
      throw e;
    } catch (Exception e) {
      // This is a query failure (connected but query failed)
      // We should not retry, just return the exception
      log.warn(
          "Query failed: {}",
          e.getMessage() == null ? ExceptionUtils.getRootCauseMessage(e) : e.getMessage());

      // Check if the connection is still valid, but don't change the connection state
      // This ensures that isConnected() will return the correct state for subsequent calls
      if (transport != null && !transport.isConnected()) {
        log.warn(
            "Connection lost during query execution, but not reconnecting as per requirements");
        // Update the connected flag to match the transport state
        // This ensures that isConnected() will return false for subsequent calls
        // but we don't trigger any reconnection logic
        connected = false;
      }

      throw e;
    }
  }

  /**
   * Handle connection failure with retry mechanism
   *
   * @param <T> The return type of the query
   * @param supplier The query supplier
   * @param initialException The initial connection exception
   * @return The query result
   * @throws Exception if all connection attempts fail
   */
  private <T> T handleConnectionFailure(Callable<T> supplier, Exception initialException)
      throws Exception {
    Exception lastException = initialException;
    int retries = 0;

    while (retries < maxRetries) {
      log.warn(
          "Connection failed (attempt {}/{}): {}",
          retries + 1,
          maxRetries,
          lastException.getMessage() == null
              ? ExceptionUtils.getRootCauseMessage(lastException)
              : lastException.getMessage());

      if (!useServerRotation
          || globalConfig == null
          || globalConfig.getLiteservers() == null
          || globalConfig.getLiteservers().length <= 1
          || liteServerIndex >= 0) {
        // No server rotation or only one server available or fixed server index
        retries++;
        try {
          // Try to reconnect to the same server
          connectWithRetry();
          return supplier.call();
        } catch (Exception e) {
          lastException = e;
        }
        continue;
      }

      // Try next server
      int nextIndex = (currentServerIndex.get() + 1) % globalConfig.getLiteservers().length;
      currentServerIndex.set(nextIndex);

      try {
        // Close current connection
        close();
        // Connect to next server
        connect(globalConfig.getLiteservers()[nextIndex]);
        log.info("Switched to lite-server at index: {}", nextIndex);
        return supplier.call();
      } catch (Exception connectException) {
        lastException = connectException;
        log.error(
            "Failed to connect to lite-server at index {}: {}",
            nextIndex,
            connectException.getMessage());
      }

      retries++;
    }

    throw new Exception(
        "Failed to connect after "
            + maxRetries
            + " attempts. Last error: "
            + (lastException != null
                ? (lastException.getMessage() != null
                    ? lastException.getMessage()
                    : ExceptionUtils.getStackTrace(lastException))
                : "unknown error"));
  }

  /** Builder for AdnlLiteClient */
  public static class Builder {
    private Client client;
    private TonGlobalConfig globalConfig;
    private LiteServers liteServer;
    private int liteServerIndex = -1;
    private boolean useServerRotation = true;
    private int maxRetries = 5;
    private int queryTimeout = 60;

    /** Create a new builder */
    public Builder() {}

    /**
     * Set the client with keys
     *
     * @param client Client with keys
     * @return Builder
     */
    public Builder client(Client client) {
      this.client = client;
      return this;
    }

    /**
     * Set the global config from a TonGlobalConfig object
     *
     * @param globalConfig TonGlobalConfig object
     * @return Builder
     */
    public Builder globalConfig(TonGlobalConfig globalConfig) {
      this.globalConfig = globalConfig;
      return this;
    }

    public Builder liteServer(LiteServers liteServer) {
      this.liteServer = liteServer;
      return this;
    }

    /**
     * Set the global config from a file path
     *
     * @param configPath Path to global.config.json file
     * @return Builder
     * @throws Exception if file cannot be read
     */
    public Builder configPath(String configPath) throws Exception {
      if (StringUtils.isEmpty(configPath)) {
        throw new IllegalArgumentException("Config path cannot be empty");
      }

      File configFile = new File(configPath);
      if (!configFile.exists()) {
        throw new IllegalArgumentException("Config file not found: " + configPath);
      }

      this.globalConfig = TonGlobalConfig.loadFromPath(configPath);
      return this;
    }

    /**
     * Set the global config from a URL
     *
     * @param configUrl URL to global.config.json file
     * @return Builder
     */
    public Builder configUrl(String configUrl) {
      if (StringUtils.isEmpty(configUrl)) {
        throw new IllegalArgumentException("Config URL cannot be empty");
      }

      this.globalConfig = TonGlobalConfig.loadFromUrl(configUrl);
      return this;
    }

    /**
     * Set the lite server index to use If set, the client will only use this specific lite server
     * and will not rotate
     *
     * @param index Index of the lite server to use
     * @return Builder
     */
    public Builder liteServerIndex(int index) {
      this.liteServerIndex = index;
      this.useServerRotation = false;
      return this;
    }

    /**
     * Set whether to use server rotation on failure Only applicable if liteServerIndex is not set
     *
     * @param useRotation Whether to use server rotation
     * @return Builder
     */
    public Builder useServerRotation(boolean useRotation) {
      this.useServerRotation = useRotation;
      return this;
    }

    /**
     * Set the maximum number of retries
     *
     * @param maxRetries Maximum number of retries
     * @return Builder
     */
    public Builder maxRetries(int maxRetries) {
      this.maxRetries = maxRetries;
      return this;
    }

    /**
     * Set the timeout in seconds for each query
     *
     * @param queryTimeout Number of seconds to wait for query response
     * @return Builder
     */
    public Builder queryTimeout(int queryTimeout) {
      this.queryTimeout = queryTimeout;
      return this;
    }

    /**
     * Build the AdnlLiteClient
     *
     * @return AdnlLiteClient
     */
    public AdnlLiteClient build() throws Exception {
      AdnlLiteClient adnlLiteClient = new AdnlLiteClient(this);
      adnlLiteClient.connectWithRetry();
      return adnlLiteClient;
    }
  }

  public boolean isDeployed(Address address) {
    try {
      return (getAccount(address).getAccountStorage().getAccountState()
          instanceof AccountStateActive);
    } catch (Exception e) {
      return false;
    }
  }

  public void waitForDeployment(Address address, int timeoutSeconds) {

    int i = 0;
    do {
      if (++i * 2 >= timeoutSeconds) {
        throw new Error("Can't deploy contract within specified timeout.");
      }
      Utils.sleep(2);
      log.info("Waiting for deployment to be deployed, balance {}", getBalance(address));
    } while (!isDeployed(address));
  }

  public void waitForBalanceChange(Address address, int timeoutSeconds) {
    log.info("Waiting for balance change (up to {}s) - ({})", timeoutSeconds, address.toRaw());
    BigInteger initialBalance = getBalance(address);
    int i = 0;
    do {
      if (++i * 2 >= timeoutSeconds) {
        throw new Error(
            "Balance of " + address.toRaw() + " was not changed within specified timeout.");
      }
      Utils.sleep(2);
    } while (initialBalance.equals(getBalance(address)));
  }

  public void waitForBalanceChangeWithTolerance(
      Address address, int timeoutSeconds, BigInteger tolerateNanoCoins) {

    BigInteger initialBalance = getBalance(address);
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
      BigInteger currentBalance = getBalance(address);

      diff =
          Math.max(currentBalance.longValue(), initialBalance.longValue())
              - Math.min(currentBalance.longValue(), initialBalance.longValue());
    } while (diff < tolerateNanoCoins.longValue());
  }

  /**
   * Sends Message with deliver confirmation. After the message has been sent to the network this
   * method looks up specified account transactions and returns true if message was found among
   * them. Timeout 60 seconds.
   *
   * @param externalMessage - Message
   * @throws TimeoutException if message not found within a timeout
   */
  public void sendRawMessageWithConfirmation(Message externalMessage, Address account) {
    try {
      SendMsgStatus sendMsgStatus = sendMessage(externalMessage);
      log.info(
          "Message has been successfully sent. Waiting for delivery of message with hash {}",
          Utils.bytesToHex(externalMessage.getNormalizedHash()));

      TransactionList rawTransactions;
      for (int i = 0; i < 12; i++) {
        rawTransactions = getTransactions(account, 0, null, 2);
        for (Transaction tx : rawTransactions.getTransactionsParsed()) {
          if (nonNull(tx.getInOut().getIn())
              && Arrays.equals(
                  tx.getInOut().getIn().getNormalizedHash(), externalMessage.getNormalizedHash())) {
            log.info("Message has been delivered.");
            return;
          }
        }
        Utils.sleep(5);
      }
      log.error("Timeout waiting for message hash");
      throw new Error("Cannot find hash of the sent message");
    } catch (Exception e) {
      log.error("Timeout waiting for message hash");
      throw new Error("Cannot find hash of the sent message");
    }
  }

  public SendMsgStatus sendMessage(Message externalMessage) {
    try {
      return executeWithRetry(
          () -> {
            if (!connected || !transport.isConnected()) {
              throw new IllegalStateException("Not connected to lite-server");
            }

            byte[] queryBytes =
                LiteServerQuery.pack(
                    SendMessageQuery.builder().body(externalMessage.toCell().toBoc()).build());

            LiteServerAnswer response =
                transport.query(queryBytes).get(queryTimeout, TimeUnit.SECONDS);
            try {
              return (SendMsgStatus) response;
            } catch (Exception e) {
              if (response instanceof LiteServerError) {
                return SendMsgStatus.builder()
                    .responseCode(((LiteServerError) response).getCode())
                    .responseMessage(((LiteServerError) response).getMessage())
                    .build();
              }
              throw new Error("Cannot query lite server response", e);
            }
          });
    } catch (Exception e) {
      throw new Error("Cannot execute query", e);
    }
  }

  public List<Participant> getElectionParticipants() {
    List<Participant> participants = new ArrayList<>();
    RunMethodResult result = runMethod(ELECTION_ADDRESS, "participant_list");
    VmStack vmStack = VmStack.deserialize(CellSlice.beginParse(Cell.fromBoc(result.result)));
    for (VmStackValue l : vmStack.getStack().getTos()) {
      VmStackValueTuple tuple = VmStackValueTuple.deserialize(CellSlice.beginParse(l.toCell()));
      BigInteger addr = tuple.getData().getIntByIndex(0);
      BigInteger stake = tuple.getData().getIntByIndex(1);
      participants.add(Participant.builder().address(addr).stake(stake).build());
    }
    return participants;
  }

  public BigInteger getElectionId() {
    RunMethodResult result = runMethod(ELECTION_ADDRESS, "active_election_id");
    if (result.getExitCode() != 0) {
      throw new Error("method seqno returned an exit code " + result.getExitCode());
    }
    VmStack vmStack = VmStack.deserialize(CellSlice.beginParse(Cell.fromBoc(result.result)));
    return VmStackValueTinyInt.deserialize(
            CellSlice.beginParse(vmStack.getStack().getTos().get(0).toCell()))
        .getValue();
  }

  public BigInteger getReturnedStake(String validatorWalletHex) {
    RunMethodResult result =
        runMethod(
            ELECTION_ADDRESS,
            "compute_returned_stake",
            VmStackValueInt.builder()
                .value(new BigInteger(validatorWalletHex.toLowerCase(), 16))
                .build());
    VmStack vmStack = VmStack.deserialize(CellSlice.beginParse(Cell.fromBoc(result.result)));
    return VmStackValueTinyInt.deserialize(
            CellSlice.beginParse(vmStack.getStack().getTos().get(0).toCell()))
        .getValue();
  }
}
