package org.ton.java.adnl;

import java.io.File;
import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.ton.java.adnl.globalconfig.LiteServers;
import org.ton.java.adnl.globalconfig.TonGlobalConfig;
import org.ton.ton4j.address.Address;
import org.ton.ton4j.tl.liteserver.queries.*;
import org.ton.ton4j.tl.liteserver.responses.*;
import org.ton.ton4j.tlb.Account;
import org.ton.ton4j.utils.Utils;

/**
 * Native ADNL Lite Client implementation for TON blockchain Uses TCP transport to communicate with
 * lite-servers
 */
@Slf4j
public class AdnlLiteClient {

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
  private final AtomicInteger currentServerIndex = new AtomicInteger(0);

  private AdnlLiteClient(Builder builder) {
    this.transport =
        builder.client != null ? new AdnlTcpTransport(builder.client) : new AdnlTcpTransport();
    this.pingScheduler = Executors.newSingleThreadScheduledExecutor();
    this.globalConfig = builder.globalConfig;
    this.liteServerIndex = builder.liteServerIndex;
    this.useServerRotation = builder.useServerRotation;
    this.maxRetries = builder.maxRetries;

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
              log.info("Ping successful");
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
          log.info("Sending getMasterchainInfo query");

          LiteServerAnswer response;
          response = transport.query(queryBytes).get(60, TimeUnit.SECONDS);
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
          log.info("Sending getMasterchainInfoExt query");

          LiteServerAnswer response;
          response = transport.query(queryBytes).get(60, TimeUnit.SECONDS);
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
          log.info("Sending getTime query");

          LiteServerAnswer response = transport.query(queryBytes).get(60, TimeUnit.SECONDS);
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
          log.info("Sending getVersion query");

          LiteServerAnswer response = transport.query(queryBytes).get(60, TimeUnit.SECONDS);
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

          log.info("Sending getConfigAll query");

          LiteServerAnswer response = transport.query(queryBytes).get(60, TimeUnit.SECONDS);
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

          log.info("Sending getConfigParams query");

          LiteServerAnswer response = transport.query(queryBytes).get(60, TimeUnit.SECONDS);
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

  public BlockData getBlock(BlockIdExt id) throws Exception {
    return executeWithRetry(
        () -> {
          if (!connected || !transport.isConnected()) {
            throw new IllegalStateException("Not connected to lite-server");
          }

          byte[] queryBytes = LiteServerQuery.pack(BlockQuery.builder().id(id).build());
          log.info("Sending getBlock query");

          LiteServerAnswer response = transport.query(queryBytes).get(15, TimeUnit.SECONDS);
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
          log.info("Sending getBlockState query");

          LiteServerAnswer response = transport.query(queryBytes).get(60, TimeUnit.SECONDS);
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

          log.info("Sending getBlockHeader query");

          LiteServerAnswer response = transport.query(queryBytes).get(60, TimeUnit.SECONDS);
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

  public SendMsgStatus sendMessage(byte[] body) throws Exception {
    return executeWithRetry(
        () -> {
          if (!connected || !transport.isConnected()) {
            throw new IllegalStateException("Not connected to lite-server");
          }

          byte[] queryBytes = LiteServerQuery.pack(SendMessageQuery.builder().body(body).build());

          log.info("Sending sendMessage query");

          LiteServerAnswer response = transport.query(queryBytes).get(60, TimeUnit.SECONDS);
          try {
            return (SendMsgStatus) response;
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

          log.info("Sending ValidatorStatsQuery query");

          LiteServerAnswer response = transport.query(queryBytes).get(60, TimeUnit.SECONDS);
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

          log.info("Sending sendMessage query");

          LiteServerAnswer response = transport.query(queryBytes).get(60, TimeUnit.SECONDS);
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

          log.info("Sending sendMessage query");

          LiteServerAnswer response = transport.query(queryBytes).get(60, TimeUnit.SECONDS);
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

  public BigInteger getBalance(Address address) throws Exception {
    MasterchainInfo masterchainInfo = getMasterchainInfo();
    return getAccountState(masterchainInfo.getLast(), address)
        .getAccount()
        .getAccountStorage()
        .getBalance()
        .getCoins();
  }

  public Account getAccount(Address address) throws Exception {
    MasterchainInfo masterchainInfo = getMasterchainInfo();
    return getAccountState(masterchainInfo.getLast(), address).getAccount();
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
          log.info("Sending getAccountState query");

          LiteServerAnswer response = transport.query(queryBytes).get(60, TimeUnit.SECONDS);
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
          log.info("Sending getAccountStatePruned query");

          LiteServerAnswer response = transport.query(queryBytes).get(60, TimeUnit.SECONDS);
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

          log.info("Sending runMethod query");

          LiteServerAnswer response = transport.query(queryBytes).get(60, TimeUnit.SECONDS);
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

          log.info("Sending getShardInfo query");

          LiteServerAnswer response = transport.query(queryBytes).get(60, TimeUnit.SECONDS);
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
          log.info("Sending getAllShardsInfo query");

          LiteServerAnswer response = transport.query(queryBytes).get(60, TimeUnit.SECONDS);
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
          log.info("Sending getOneTransaction query");

          LiteServerAnswer response = transport.query(queryBytes).get(60, TimeUnit.SECONDS);
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
          byte[] tempHash = new byte[32];
          if ((lt == 0) || (hash == null)) {
            MasterchainInfo masterchainInfo = getMasterchainInfo();
            AccountState account = getAccountState(masterchainInfo.getLast(), accountAddress);
            tempLt = account.getShardAccounts().get(0).getLastTransLt().longValue();
            tempHash = account.getShardAccounts().get(0).lastTransHash.toByteArray();
            System.out.println("tempLt " + tempLt + ", tempHash " + Utils.bytesToHex(tempHash));
          }

          byte[] queryBytes =
              LiteServerQuery.pack(
                  TransactionListQuery.builder()
                      .count(count)
                      .account(accountAddress)
                      .lt((lt == 0) ? tempLt : lt)
                      .hash((hash == null) ? tempHash : hash)
                      .build());

          log.info("Sending getTransactions query");

          LiteServerAnswer response = transport.query(queryBytes).get(60, TimeUnit.SECONDS);
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

          log.info("Sending lookupBlock query");

          LiteServerAnswer response = transport.query(queryBytes).get(60, TimeUnit.SECONDS);
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

          log.info("Sending lookupBlockWithProof query");

          LiteServerAnswer response = transport.query(queryBytes).get(60, TimeUnit.SECONDS);
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

          log.info("Sending listBlockTransactions query");

          LiteServerAnswer response = transport.query(queryBytes).get(60, TimeUnit.SECONDS);
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
          log.info("Sending listBlockTransactionsExt query");

          LiteServerAnswer response = transport.query(queryBytes).get(60, TimeUnit.SECONDS);
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
          log.info("Sending getTime query");

          LiteServerAnswer response = transport.query(queryBytes).get(60, TimeUnit.SECONDS);
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
          log.info("Sending getTime query");

          LiteServerAnswer response = transport.query(queryBytes).get(60, TimeUnit.SECONDS);
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

          log.info("Sending getTime query");

          LiteServerAnswer response = transport.query(queryBytes).get(60, TimeUnit.SECONDS);
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

          log.info("Sending getTime query");

          LiteServerAnswer response = transport.query(queryBytes).get(60, TimeUnit.SECONDS);
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

          log.info("Sending getTime query");

          LiteServerAnswer response = transport.query(queryBytes).get(60, TimeUnit.SECONDS);
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

          log.info("Sending getTime query");

          LiteServerAnswer response = transport.query(queryBytes).get(60, TimeUnit.SECONDS);
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
    Exception lastException = null;
    int retries = 0;

    while (retries < maxRetries) {
      try {
        return supplier.call();
      } catch (Exception e) {
        lastException = e;
        log.warn(
            "Query failed (attempt {}/{}): {}",
            retries + 1,
            maxRetries,
            e.getMessage() == null
                ? ExceptionUtils.getRootCauseMessage(lastException)
                : lastException.getMessage());

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

        try {
          // Close current connection
          close();
          // Connect to next server
          connect(globalConfig.getLiteservers()[nextIndex]);
          log.info("Switched to lite-server at index: {}", nextIndex);
        } catch (Exception connectException) {
          log.error(
              "Failed to connect to lite-server at index {}: {}",
              nextIndex,
              connectException.getMessage());
        }

        retries++;
      }
    }

    throw new Exception(
        "Failed after "
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
     * @throws Exception if URL cannot be read
     */
    public Builder configUrl(String configUrl) throws Exception {
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
}
