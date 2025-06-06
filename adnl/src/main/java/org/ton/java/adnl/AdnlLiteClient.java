package org.ton.java.adnl;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import lombok.extern.slf4j.Slf4j;
import org.ton.java.adnl.globalconfig.LiteServers;
import org.ton.ton4j.address.Address;
import org.ton.ton4j.tl.queries.*;
import org.ton.ton4j.tl.types.*;
import org.ton.ton4j.utils.Utils;

/**
 * Native ADNL Lite Client implementation for TON blockchain Uses TCP transport to communicate with
 * lite-servers
 */
@Slf4j
public class AdnlLiteClient {

  private final AdnlTcpTransport transport;
  private final ScheduledExecutorService pingScheduler;
  private volatile boolean connected = false;

  /** Create lite client with generated keys */
  public AdnlLiteClient() {
    this.transport = new AdnlTcpTransport();
    this.pingScheduler = Executors.newSingleThreadScheduledExecutor();
  }

  /**
   * Create lite client with specific client keys
   *
   * @param client Client with keys
   */
  public AdnlLiteClient(Client client) {
    this.transport = new AdnlTcpTransport(client);
    this.pingScheduler = Executors.newSingleThreadScheduledExecutor();
  }

  /**
   * Connect to liteserver
   *
   * @param host Server host
   * @param port Server port
   * @param serverPublicKeyBase64 Server's Ed25519 public key (base64 encoded)
   * @throws Exception if connection fails
   */
  public void connect(String host, int port, String serverPublicKeyBase64) throws Exception {
    byte[] serverPublicKey = Base64.getDecoder().decode(serverPublicKeyBase64);
    transport.connect(host, port, serverPublicKey);
    connected = true;

    // Start ping scheduler (every 5 seconds as per specification)
    startPingScheduler();

    log.info("Connected to lite-server " + host + ":" + port);
  }

  public void connect(LiteServers liteServer) throws Exception {
    connect(
        Utils.int2ip(liteServer.getIp()), (int) liteServer.getPort(), liteServer.getId().getKey());
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
    if (!connected || !transport.isConnected()) {
      throw new IllegalStateException("Not connected to lite-server");
    }
    byte[] queryBytes = LiteServerQuery.pack(MasterchainInfoQuery.builder().build());
    log.info("Sending getMasterchainInfo query, size: {} bytes", queryBytes.length);

    LiteServerAnswer response;
    response = transport.query(queryBytes).get(60, TimeUnit.SECONDS);
    return (MasterchainInfo) response;
  }

  public MasterchainInfoExt getMasterchainInfoExt(int mode) throws Exception {
    if (!connected || !transport.isConnected()) {
      throw new IllegalStateException("Not connected to lite-server");
    }
    byte[] queryBytes = LiteServerQuery.pack(MasterchainInfoExtQuery.builder().mode(mode).build());
    log.info("Sending getMasterchainInfoExt query, size: {} bytes", queryBytes.length);

    LiteServerAnswer response;
    response = transport.query(queryBytes).get(60, TimeUnit.SECONDS);
    return (MasterchainInfoExt) response;
  }

  public CurrentTime getTime() throws Exception {
    if (!connected || !transport.isConnected()) {
      throw new IllegalStateException("Not connected to lite-server");
    }

    byte[] queryBytes = LiteServerQuery.pack(CurrentTimeQuery.builder().build());
    log.info("Sending getTime query, size: {} bytes", queryBytes.length);

    LiteServerAnswer response = transport.query(queryBytes).get(60, TimeUnit.SECONDS);
    return (CurrentTime) response;
  }

  public Version getVersion() throws Exception {
    if (!connected || !transport.isConnected()) {
      throw new IllegalStateException("Not connected to lite-server");
    }

    byte[] queryBytes = LiteServerQuery.pack(VersionQuery.builder().build());
    log.info("Sending getVersion query, size: {} bytes", queryBytes.length);

    LiteServerAnswer response = transport.query(queryBytes).get(60, TimeUnit.SECONDS);
    return (Version) response;
  }

  public ConfigAll getConfigAll(BlockIdExt id, int mode) throws Exception {
    if (!connected || !transport.isConnected()) {
      throw new IllegalStateException("Not connected to lite-server");
    }

    byte[] queryBytes = LiteServerQuery.pack(ConfigAllQuery.builder().mode(mode).id(id).build());

    log.info("Sending getConfigAll query, size: {} bytes", queryBytes.length);

    LiteServerAnswer response;
    try {
      response = transport.query(queryBytes).get(60, TimeUnit.SECONDS);
      return (ConfigAll) response;
    } catch (Exception e) {
      log.warn("Error with serialized query approach: {}", e.getMessage(), e);

      // Check if we're still connected before trying the direct binary approach
      if (!transport.isConnected()) {
        log.info("Connection lost, reconnecting is required");
        connected = false;
        throw new IOException("Connection lost, reconnecting is required", e);
      }
    }

    throw new Exception("Was not able to retrieve masterchainInfo from lite server");
  }

  public BlockData getBlock(BlockIdExt id) throws Exception {
    if (!connected || !transport.isConnected()) {
      throw new IllegalStateException("Not connected to lite-server");
    }

    byte[] queryBytes = LiteServerQuery.pack(BlockQuery.builder().id(id).build());
    log.info("Sending getBlock query, size: {} bytes", queryBytes.length);

    LiteServerAnswer response = transport.query(queryBytes).get(15, TimeUnit.SECONDS);
    return (BlockData) response;
  }

  public BlockState getBlockState(BlockIdExt id) throws Exception {
    if (!connected || !transport.isConnected()) {
      throw new IllegalStateException("Not connected to lite-server");
    }

    byte[] queryBytes = LiteServerQuery.pack(BlockStateQuery.builder().id(id).build());
    log.info("Sending getBlockState query, size: {} bytes", queryBytes.length);

    LiteServerAnswer response = transport.query(queryBytes).get(60, TimeUnit.SECONDS);
    return (BlockState) response;
  }

  public BlockHeader getBlockHeader(BlockIdExt id, int mode) throws Exception {
    if (!connected || !transport.isConnected()) {
      throw new IllegalStateException("Not connected to lite-server");
    }

    byte[] queryBytes = LiteServerQuery.pack(BlockHeaderQuery.builder().id(id).mode(mode).build());

    log.info("Sending getBlockHeader query, size: {} bytes", queryBytes.length);

    LiteServerAnswer response = transport.query(queryBytes).get(60, TimeUnit.SECONDS);
    return (BlockHeader) response;
  }

  public SendMsgStatus sendMessage(byte[] body) throws Exception {
    if (!connected || !transport.isConnected()) {
      throw new IllegalStateException("Not connected to lite-server");
    }

    byte[] queryBytes =
        LiteServerQuery.serialize("liteServer.sendMessage body:bytes = liteServer.SendMsgStatus");
    log.info("Sending sendMessage query, size: {} bytes", queryBytes.length);

    LiteServerAnswer response = transport.query(queryBytes).get(60, TimeUnit.SECONDS);
    return (SendMsgStatus) response;
  }

  public AccountState getAccountState(BlockIdExt id, Address accountAddress) throws Exception {
    if (!connected || !transport.isConnected()) {
      throw new IllegalStateException("Not connected to lite-server");
    }

    byte[] queryBytes =
        LiteServerQuery.pack(AccountStateQuery.builder().id(id).account(accountAddress).build());
    //    LiteServerQuery.serialize(
    //        "liteServer.getAccountState id:tonNode.blockIdExt account_address:tonNode.accountId =
    // liteServer.AccountState");
    log.info("Sending getAccountState query, size: {} bytes", queryBytes.length);

    LiteServerAnswer response = transport.query(queryBytes).get(60, TimeUnit.SECONDS);
    return (AccountState) response;
  }

  public RunMethodResult runMethod(
      BlockIdExt id, String accountAddress, String methodName, String methodParams)
      throws Exception {
    if (!connected || !transport.isConnected()) {
      throw new IllegalStateException("Not connected to lite-server");
    }

    byte[] queryBytes =
        LiteServerQuery.serialize(
            "liteServer.runMethod id:tonNode.blockIdExt account_address:tonNode.accountId method_name:string method_params:bytes = liteServer.RunMethodResult");
    log.info("Sending runMethod query, size: {} bytes", queryBytes.length);

    LiteServerAnswer response = transport.query(queryBytes).get(60, TimeUnit.SECONDS);
    return (RunMethodResult) response;
  }

  public ShardInfo getShardInfo(BlockIdExt id, int workchain, long shard, boolean exact)
      throws Exception {
    if (!connected || !transport.isConnected()) {
      throw new IllegalStateException("Not connected to lite-server");
    }

    byte[] queryBytes =
        LiteServerQuery.pack(
            ShardInfoQuery.builder().id(id).workchain(workchain).shard(shard).exact(exact).build());

    log.info("Sending getShardInfo query, size: {} bytes", queryBytes.length);

    LiteServerAnswer response = transport.query(queryBytes).get(60, TimeUnit.SECONDS);
    return (ShardInfo) response;
  }

  public AllShardsInfo getAllShardsInfo(BlockIdExt id) throws Exception {
    if (!connected || !transport.isConnected()) {
      throw new IllegalStateException("Not connected to lite-server");
    }

    byte[] queryBytes = LiteServerQuery.pack(AllShardsInfoQuery.builder().id(id).build());
    log.info("Sending getAllShardsInfo query, size: {} bytes", queryBytes.length);

    LiteServerAnswer response = transport.query(queryBytes).get(60, TimeUnit.SECONDS);
    return (AllShardsInfo) response;
  }

  public TransactionInfo getOneTransaction(BlockIdExt id, Address accountAddress, long lt)
      throws Exception {
    if (!connected || !transport.isConnected()) {
      throw new IllegalStateException("Not connected to lite-server");
    }

    byte[] queryBytes =
        LiteServerQuery.pack(
            OneTransactionQuery.builder().id(id).account(accountAddress).lt(lt).build());
    LiteServerQuery.serialize(
        "liteServer.getOneTransaction id:tonNode.blockIdExt account:tonNode.accountId lt:long = liteServer.TransactionInfo");
    log.info("Sending getOneTransaction query, size: {} bytes", queryBytes.length);

    LiteServerAnswer response = transport.query(queryBytes).get(60, TimeUnit.SECONDS);
    return (TransactionInfo) response;
  }

  public TransactionList getTransactions(Address accountAddress, long lt, byte[] hash, int count)
      throws Exception {
    if (!connected || !transport.isConnected()) {
      throw new IllegalStateException("Not connected to lite-server");
    }

    byte[] queryBytes =
        LiteServerQuery.pack(
            TransactionsQuery.builder()
                .count(count)
                .account(accountAddress)
                .lt(lt)
                .hash(hash)
                .build());

    log.info("Sending getTransactions query, size: {} bytes", queryBytes.length);

    LiteServerAnswer response = transport.query(queryBytes).get(60, TimeUnit.SECONDS);
    return (TransactionList) response;
  }

  public BlockHeader lookupBlock(BlockId id, int mode, long lt, int utime) throws Exception {
    if (!connected || !transport.isConnected()) {
      throw new IllegalStateException("Not connected to lite-server");
    }

    byte[] queryBytes =
        LiteServerQuery.pack(
            LookupBlockQuery.builder().id(id).mode(mode).lt(lt).utime(utime).build());

    log.info("Sending lookupBlock query, size: {} bytes", queryBytes.length);

    LiteServerAnswer response = transport.query(queryBytes).get(60, TimeUnit.SECONDS);
    return (BlockHeader) response;
  }

  public LookupBlockResult lookupBlockWithProof(
      int mode, BlockId id, BlockIdExt mcId, long lt, int utime) throws Exception {
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

    log.info("Sending lookupBlockWithProof query, size: {} bytes", queryBytes.length);

    LiteServerAnswer response = transport.query(queryBytes).get(60, TimeUnit.SECONDS);
    return (LookupBlockResult) response;
  }

  public BlockTransactions listBlockTransactions(
      BlockIdExt id, int mode, int count, TransactionId3 transactionId3) throws Exception {
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

    log.info("Sending listBlockTransactions query, size: {} bytes", queryBytes.length);

    LiteServerAnswer response = transport.query(queryBytes).get(60, TimeUnit.SECONDS);
    return (BlockTransactions) response;
  }

  public BlockTransactionsExt listBlockTransactionsExt(
      BlockIdExt id,
      int mode,
      int count,
      TransactionId3 after,
      boolean reverseOrder,
      boolean wantProof)
      throws Exception {
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
    log.info("Sending listBlockTransactionsExt query, size: {} bytes", queryBytes.length);

    LiteServerAnswer response = transport.query(queryBytes).get(60, TimeUnit.SECONDS);
    return (BlockTransactionsExt) response;
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
}
