package org.ton.java.adnl;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Native ADNL Lite Client implementation for TON blockchain Uses TCP transport to communicate with
 * liteservers
 */
public class AdnlLiteClient {
  private static final Logger logger = Logger.getLogger(AdnlLiteClient.class.getName());

  private final AdnlTcpTransport transport;
  private final TLGenerator.TLSchemas schemas;
  private final ScheduledExecutorService pingScheduler;
  private volatile boolean connected = false;

  /** Create lite client with generated keys */
  public AdnlLiteClient() {
    this.transport = new AdnlTcpTransport();
    this.schemas = createLiteserverSchemas();
    this.pingScheduler = Executors.newSingleThreadScheduledExecutor();
  }

  /**
   * Create lite client with specific client keys
   *
   * @param client Client with keys
   */
  public AdnlLiteClient(Client client) {
    this.transport = new AdnlTcpTransport(client);
    this.schemas = createLiteserverSchemas();
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

    logger.info("Connected to liteserver " + host + ":" + port);
  }

  /** Start ping scheduler to maintain connection */
  private void startPingScheduler() {
    pingScheduler.scheduleAtFixedRate(
        () -> {
          try {
            if (connected && transport.isConnected()) {
              transport.ping().get(5, TimeUnit.SECONDS);
              logger.fine("Ping successful");
            }
          } catch (Exception e) {
            logger.log(Level.WARNING, "Ping failed", e);
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
      throw new IllegalStateException("Not connected to liteserver");
    }

    // Create a direct binary representation of the getMasterchainInfo query
    // This matches exactly what the Go implementation sends
    // The TL-B schema for liteServer.getMasterchainInfo is:
    // liteServer.getMasterchainInfo = liteServer.MasterchainInfo;
    // Constructor ID: 0x2ee6b589 (little endian: 89 b5 e6 2e)
    byte[] queryBytes = new byte[] {
        (byte) 0xdf, (byte) 0x06, (byte) 0x8c, (byte) 0x79, // liteServer.query constructor
        (byte) 0x04, // bytes length (4 bytes)
        (byte) 0x2e, (byte) 0xe6, (byte) 0xb5, (byte) 0x89, // liteServer.getMasterchainInfo constructor
        (byte) 0x00, (byte) 0x00, (byte) 0x00 // padding to align to 4 bytes
    };

    // Log the query details for debugging
    logger.info("Sending getMasterchainInfo query, size: " + queryBytes.length + " bytes");
    logger.info("Query hex: " + bytesToHex(queryBytes));

    // Try alternative approach - create the query using the TL serializer
    Object response = null;
    try {
      // Create getMasterchainInfo query
      Map<String, Object> getMasterchainInfoQuery = new HashMap<>();
      getMasterchainInfoQuery.put("@type", "liteServer.getMasterchainInfo");
      
      // Wrap in liteServer.query
      Map<String, Object> liteQuery = new HashMap<>();
      liteQuery.put("@type", "liteServer.query");
      liteQuery.put("data", schemas.serialize("liteServer.getMasterchainInfo", getMasterchainInfoQuery, true));

      byte[] serializedQuery = schemas.serialize("liteServer.query", liteQuery, true);
      logger.info("Serialized query size: " + serializedQuery.length + " bytes");
      logger.info("Serialized query hex: " + bytesToHex(serializedQuery));
      
      try {
        // Send query and wait for response with increased timeout
        response = transport.query(serializedQuery).get(60, TimeUnit.SECONDS);
        logger.info("Received response of type: " + (response != null ? response.getClass().getName() : "null"));
      } catch (Exception e) {
        logger.log(Level.WARNING, "Error with serialized query: " + e.getMessage(), e);
        
        // Check if we're still connected
        if (!transport.isConnected()) {
          logger.warning("Connection lost during query, reconnecting is required");
          connected = false;
          throw new IOException("Connection lost during query", e);
        }
        
        throw e;
      }
    } catch (Exception e) {
      logger.log(Level.WARNING, "Error with serialized query approach: " + e.getMessage(), e);
      
      // Check if we're still connected before trying the direct binary approach
      if (!transport.isConnected()) {
        logger.warning("Connection lost, reconnecting is required");
        connected = false;
        throw new IOException("Connection lost, reconnecting is required", e);
      }
      
      // Fall back to the direct binary approach
      try {
        response = transport.query(queryBytes).get(60, TimeUnit.SECONDS);
        logger.info("Received response using direct binary approach: " + (response != null ? response.getClass().getName() : "null"));
      } catch (Exception ex) {
        // Check connection state again
        if (!transport.isConnected()) {
          connected = false;
        }
        throw ex;
      }
    }

    // Parse response
    try {
      if (response instanceof byte[]) {
        byte[] responseBytes = (byte[]) response;
        logger.info("Received response, size: " + responseBytes.length + " bytes");
        logger.info("Response hex: " + bytesToHex(responseBytes));
        
        // First, check if this is an ADNL answer message
        try {
          Map<String, Object> responseMap = schemas.deserializeToMap(responseBytes);
          logger.info("Deserialized response type: " + responseMap.get("@type"));
          
          if ("adnl.message.answer".equals(responseMap.get("@type"))) {
            // Extract the answer field which contains the actual response
            Object answer = responseMap.get("answer");
            if (answer instanceof byte[]) {
              byte[] answerBytes = (byte[]) answer;
              logger.info("Extracted answer from ADNL message, size: " + answerBytes.length + " bytes");
              
              // Try to deserialize the answer content
              Map<String, Object> answerMap = schemas.deserializeToMap(answerBytes);
              if ("liteServer.masterchainInfo".equals(answerMap.get("@type"))) {
                return parseMasterchainInfo(answerMap);
              }
            }
          } else if ("liteServer.masterchainInfo".equals(responseMap.get("@type"))) {
            // Direct masterchain info response
            return parseMasterchainInfo(responseMap);
          }
        } catch (Exception e) {
          logger.log(Level.INFO, "Could not parse response as TL object: " + e.getMessage());
        }
      } else if (response instanceof Map) {
        @SuppressWarnings("unchecked")
        Map<String, Object> responseMap = (Map<String, Object>) response;
        logger.info("Received map response with type: " + responseMap.get("@type"));
        
        if ("liteServer.masterchainInfo".equals(responseMap.get("@type"))) {
          return parseMasterchainInfo(responseMap);
        } else if ("adnl.message.answer".equals(responseMap.get("@type"))) {
          // Extract the answer field which contains the actual response
          Object answer = responseMap.get("answer");
          if (answer instanceof byte[]) {
            try {
              Map<String, Object> answerMap = schemas.deserializeToMap((byte[]) answer);
              if ("liteServer.masterchainInfo".equals(answerMap.get("@type"))) {
                return parseMasterchainInfo(answerMap);
              }
            } catch (Exception e) {
              logger.log(Level.INFO, "Could not parse answer as TL object: " + e.getMessage());
            }
          }
        }
        
        // Try to find the required fields directly in the map as a fallback
        if (responseMap.containsKey("last") && responseMap.containsKey("state_root_hash") && responseMap.containsKey("init")) {
          logger.info("Found masterchain info fields directly in response map");
          return parseMasterchainInfo(responseMap);
        }
      }
    } catch (Exception e) {
      logger.log(Level.WARNING, "Error parsing response: " + e.getMessage(), e);
    }

    throw new Exception("Invalid response format: " + (response != null ? response.getClass().getName() : "null"));
  }
  
  /**
   * Convert bytes to hex string for debugging
   */
  private String bytesToHex(byte[] bytes) {
    StringBuilder sb = new StringBuilder();
    for (byte b : bytes) {
      sb.append(String.format("%02x", b & 0xff));
    }
    return sb.toString();
  }

  /**
   * Run smart contract method
   *
   * @param blockId Block ID
   * @param accountId Account ID
   * @param methodId Method ID
   * @param params Method parameters (BoC encoded)
   * @return RunMethodResult
   * @throws Exception if query fails
   */
  public RunMethodResult runSmcMethod(
      BlockIdExt blockId, AccountId accountId, long methodId, byte[] params) throws Exception {
    if (!connected) {
      throw new IllegalStateException("Not connected to liteserver");
    }

    // Create runSmcMethod query
    Map<String, Object> query = new HashMap<>();
    query.put("@type", "liteServer.runSmcMethod");
    query.put("mode", 4); // Only want result
    query.put("id", blockIdToMap(blockId));
    query.put("account", accountIdToMap(accountId));
    query.put("method_id", methodId);
    query.put("params", params);

    // Wrap in liteServer.query
    Map<String, Object> liteQuery = new HashMap<>();
    liteQuery.put("@type", "liteServer.query");
    liteQuery.put("data", schemas.serialize("liteServer.runSmcMethod", query, true));

    // Send query and wait for response
    Object response =
        transport
            .query(schemas.serialize("liteServer.query", liteQuery, true))
            .get(30, TimeUnit.SECONDS);

    // Parse response
    if (response instanceof byte[]) {
      Object[] deserialized = schemas.deserialize((byte[]) response);
      if (deserialized[0] instanceof Map) {
        @SuppressWarnings("unchecked")
        Map<String, Object> responseMap = (Map<String, Object>) deserialized[0];
        return parseRunMethodResult(responseMap);
      }
    }

    throw new Exception("Invalid response format");
  }

  /**
   * Get account state
   *
   * @param blockId Block ID
   * @param accountId Account ID
   * @return AccountState
   * @throws Exception if query fails
   */
  public AccountState getAccountState(BlockIdExt blockId, AccountId accountId) throws Exception {
    if (!connected) {
      throw new IllegalStateException("Not connected to liteserver");
    }

    // Create getAccountState query
    Map<String, Object> query = new HashMap<>();
    query.put("@type", "liteServer.getAccountState");
    query.put("id", blockIdToMap(blockId));
    query.put("account", accountIdToMap(accountId));

    // Wrap in liteServer.query
    Map<String, Object> liteQuery = new HashMap<>();
    liteQuery.put("@type", "liteServer.query");
    liteQuery.put("data", schemas.serialize("liteServer.getAccountState", query, true));

    // Send query and wait for response
    Object response =
        transport
            .query(schemas.serialize("liteServer.query", liteQuery, true))
            .get(30, TimeUnit.SECONDS);

    // Parse response
    if (response instanceof byte[]) {
      Object[] deserialized = schemas.deserialize((byte[]) response);
      if (deserialized[0] instanceof Map) {
        @SuppressWarnings("unchecked")
        Map<String, Object> responseMap = (Map<String, Object>) deserialized[0];
        return parseAccountState(responseMap);
      }
    }

    throw new Exception("Invalid response format");
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

  /** Parse masterchain info from response */
  private MasterchainInfo parseMasterchainInfo(Map<String, Object> response) {
    @SuppressWarnings("unchecked")
    Map<String, Object> last = (Map<String, Object>) response.get("last");

    BlockIdExt lastBlock =
        new BlockIdExt(
            (Integer) last.get("workchain"),
            (Long) last.get("shard"),
            (Integer) last.get("seqno"),
            (byte[]) last.get("root_hash"),
            (byte[]) last.get("file_hash"));

    byte[] stateRootHash = (byte[]) response.get("state_root_hash");

    @SuppressWarnings("unchecked")
    Map<String, Object> init = (Map<String, Object>) response.get("init");
    ZeroStateIdExt initState =
        new ZeroStateIdExt(
            (Integer) init.get("workchain"),
            (byte[]) init.get("root_hash"),
            (byte[]) init.get("file_hash"));

    return new MasterchainInfo(lastBlock, stateRootHash, initState);
  }

  /** Parse run method result from response */
  private RunMethodResult parseRunMethodResult(Map<String, Object> response) {
    int mode = (Integer) response.get("mode");

    @SuppressWarnings("unchecked")
    Map<String, Object> id = (Map<String, Object>) response.get("id");
    BlockIdExt blockId =
        new BlockIdExt(
            (Integer) id.get("workchain"),
            (Long) id.get("shard"),
            (Integer) id.get("seqno"),
            (byte[]) id.get("root_hash"),
            (byte[]) id.get("file_hash"));

    @SuppressWarnings("unchecked")
    Map<String, Object> shardblk = (Map<String, Object>) response.get("shardblk");
    BlockIdExt shardBlock =
        new BlockIdExt(
            (Integer) shardblk.get("workchain"),
            (Long) shardblk.get("shard"),
            (Integer) shardblk.get("seqno"),
            (byte[]) shardblk.get("root_hash"),
            (byte[]) shardblk.get("file_hash"));

    int exitCode = (Integer) response.get("exit_code");
    byte[] result = (byte[]) response.get("result");

    return new RunMethodResult(mode, blockId, shardBlock, exitCode, result);
  }

  /** Parse account state from response */
  private AccountState parseAccountState(Map<String, Object> response) {
    @SuppressWarnings("unchecked")
    Map<String, Object> id = (Map<String, Object>) response.get("id");
    BlockIdExt blockId =
        new BlockIdExt(
            (Integer) id.get("workchain"),
            (Long) id.get("shard"),
            (Integer) id.get("seqno"),
            (byte[]) id.get("root_hash"),
            (byte[]) id.get("file_hash"));

    @SuppressWarnings("unchecked")
    Map<String, Object> shardblk = (Map<String, Object>) response.get("shardblk");
    BlockIdExt shardBlock =
        new BlockIdExt(
            (Integer) shardblk.get("workchain"),
            (Long) shardblk.get("shard"),
            (Integer) shardblk.get("seqno"),
            (byte[]) shardblk.get("root_hash"),
            (byte[]) shardblk.get("file_hash"));

    byte[] shardProof = (byte[]) response.get("shard_proof");
    byte[] proof = (byte[]) response.get("proof");
    byte[] state = (byte[]) response.get("state");

    return new AccountState(blockId, shardBlock, shardProof, proof, state);
  }

  /** Convert BlockIdExt to map */
  private Map<String, Object> blockIdToMap(BlockIdExt blockId) {
    Map<String, Object> map = new HashMap<>();
    map.put("@type", "tonNode.blockIdExt");
    map.put("workchain", blockId.getWorkchain());
    map.put("shard", blockId.getShard());
    map.put("seqno", blockId.getSeqno());
    map.put("root_hash", blockId.getRootHash());
    map.put("file_hash", blockId.getFileHash());
    return map;
  }

  /** Convert AccountId to map */
  private Map<String, Object> accountIdToMap(AccountId accountId) {
    Map<String, Object> map = new HashMap<>();
    map.put("@type", "liteServer.accountId");
    map.put("workchain", accountId.getWorkchain());
    map.put("id", accountId.getId());
    return map;
  }

  /** Create TL schemas for liteserver protocol */
  private static TLGenerator.TLSchemas createLiteserverSchemas() {
    List<TLGenerator.TLSchema> schemas = new ArrayList<>();

    // Liteserver query wrapper
    schemas.add(
        new TLGenerator.TLSchema(
            intToBytes(0xdf068c79), "liteServer.query", "Object", mapOf("data", "bytes")));

    // Core liteserver methods
    schemas.add(
        new TLGenerator.TLSchema(
            intToBytes(0x2ee6b589),
            "liteServer.getMasterchainInfo",
            "liteServer.MasterchainInfo",
            new HashMap<>()));

    schemas.add(
        new TLGenerator.TLSchema(
            intToBytes(0x81288385),
            "liteServer.masterchainInfo",
            "liteServer.MasterchainInfo",
            mapOf(
                "last",
                "tonNode.blockIdExt",
                "state_root_hash",
                "int256",
                "init",
                "tonNode.zeroStateIdExt")));

    schemas.add(
        new TLGenerator.TLSchema(
            intToBytes(0x0a2e0100),
            "liteServer.runSmcMethod",
            "liteServer.RunMethodResult",
            mapOf(
                "mode",
                "#",
                "id",
                "tonNode.blockIdExt",
                "account",
                "liteServer.accountId",
                "method_id",
                "long",
                "params",
                "bytes")));

    schemas.add(
        new TLGenerator.TLSchema(
            intToBytes(0xc7c72d20),
            "liteServer.runMethodResult",
            "liteServer.RunMethodResult",
            mapOf(
                "mode",
                "#",
                "id",
                "tonNode.blockIdExt",
                "shardblk",
                "tonNode.blockIdExt",
                "exit_code",
                "int",
                "result",
                "mode.2?bytes")));

    schemas.add(
        new TLGenerator.TLSchema(
            intToBytes(0x8d5a0100),
            "liteServer.getAccountState",
            "liteServer.AccountState",
            mapOf("id", "tonNode.blockIdExt", "account", "liteServer.accountId")));

    schemas.add(
        new TLGenerator.TLSchema(
            intToBytes(0x4e5a0100),
            "liteServer.accountState",
            "liteServer.AccountState",
            mapOf(
                "id",
                "tonNode.blockIdExt",
                "shardblk",
                "tonNode.blockIdExt",
                "shard_proof",
                "bytes",
                "proof",
                "bytes",
                "state",
                "bytes")));

    // Supporting types
    schemas.add(
        new TLGenerator.TLSchema(
            intToBytes(0x9b895a00),
            "tonNode.blockIdExt",
            "tonNode.BlockIdExt",
            mapOf(
                "workchain",
                "int",
                "shard",
                "long",
                "seqno",
                "int",
                "root_hash",
                "int256",
                "file_hash",
                "int256")));

    schemas.add(
        new TLGenerator.TLSchema(
            intToBytes(0x8b895a00),
            "tonNode.zeroStateIdExt",
            "tonNode.ZeroStateIdExt",
            mapOf("workchain", "int", "root_hash", "int256", "file_hash", "int256")));

    schemas.add(
        new TLGenerator.TLSchema(
            intToBytes(0x8a895a00),
            "liteServer.accountId",
            "liteServer.AccountId",
            mapOf("workchain", "int", "id", "int256")));

    // ADNL message schemas
    schemas.add(
        new TLGenerator.TLSchema(
            intToBytes(0x7af98bb4),
            "adnl.message.query",
            "adnl.Message",
            mapOf("query_id", "bytes", "query", "bytes")));
    schemas.add(
        new TLGenerator.TLSchema(
            intToBytes(0x4c2d4977),
            "adnl.message.answer",
            "adnl.Message",
            mapOf("query_id", "bytes", "answer", "bytes")));

    return new TLGenerator.TLSchemas(schemas);
  }

  /** Convert int to bytes (little endian) */
  private static byte[] intToBytes(int value) {
    return java.nio.ByteBuffer.allocate(4)
        .order(java.nio.ByteOrder.LITTLE_ENDIAN)
        .putInt(value)
        .array();
  }

  /** Create map from key-value pairs */
  private static <K, V> Map<K, V> mapOf(Object... keyValues) {
    Map<K, V> map = new HashMap<>();
    for (int i = 0; i < keyValues.length; i += 2) {
      @SuppressWarnings("unchecked")
      K key = (K) keyValues[i];
      @SuppressWarnings("unchecked")
      V value = (V) keyValues[i + 1];
      map.put(key, value);
    }
    return map;
  }

  // Data classes for liteserver responses

  /** Masterchain info */
  public static class MasterchainInfo {
    private final BlockIdExt last;
    private final byte[] stateRootHash;
    private final ZeroStateIdExt init;

    public MasterchainInfo(BlockIdExt last, byte[] stateRootHash, ZeroStateIdExt init) {
      this.last = last;
      this.stateRootHash = stateRootHash;
      this.init = init;
    }

    public BlockIdExt getLast() {
      return last;
    }

    public byte[] getStateRootHash() {
      return stateRootHash;
    }

    public ZeroStateIdExt getInit() {
      return init;
    }
  }

  /** Block ID extended */
  public static class BlockIdExt {
    private final int workchain;
    private final long shard;
    private final int seqno;
    private final byte[] rootHash;
    private final byte[] fileHash;

    public BlockIdExt(int workchain, long shard, int seqno, byte[] rootHash, byte[] fileHash) {
      this.workchain = workchain;
      this.shard = shard;
      this.seqno = seqno;
      this.rootHash = rootHash;
      this.fileHash = fileHash;
    }

    public int getWorkchain() {
      return workchain;
    }

    public long getShard() {
      return shard;
    }

    public int getSeqno() {
      return seqno;
    }

    public byte[] getRootHash() {
      return rootHash;
    }

    public byte[] getFileHash() {
      return fileHash;
    }
  }

  /** Zero state ID extended */
  public static class ZeroStateIdExt {
    private final int workchain;
    private final byte[] rootHash;
    private final byte[] fileHash;

    public ZeroStateIdExt(int workchain, byte[] rootHash, byte[] fileHash) {
      this.workchain = workchain;
      this.rootHash = rootHash;
      this.fileHash = fileHash;
    }

    public int getWorkchain() {
      return workchain;
    }

    public byte[] getRootHash() {
      return rootHash;
    }

    public byte[] getFileHash() {
      return fileHash;
    }
  }

  /** Account ID */
  public static class AccountId {
    private final int workchain;
    private final byte[] id;

    public AccountId(int workchain, byte[] id) {
      this.workchain = workchain;
      this.id = id;
    }

    public int getWorkchain() {
      return workchain;
    }

    public byte[] getId() {
      return id;
    }
  }

  /** Run method result */
  public static class RunMethodResult {
    private final int mode;
    private final BlockIdExt id;
    private final BlockIdExt shardblk;
    private final int exitCode;
    private final byte[] result;

    public RunMethodResult(
        int mode, BlockIdExt id, BlockIdExt shardblk, int exitCode, byte[] result) {
      this.mode = mode;
      this.id = id;
      this.shardblk = shardblk;
      this.exitCode = exitCode;
      this.result = result;
    }

    public int getMode() {
      return mode;
    }

    public BlockIdExt getId() {
      return id;
    }

    public BlockIdExt getShardblk() {
      return shardblk;
    }

    public int getExitCode() {
      return exitCode;
    }

    public byte[] getResult() {
      return result;
    }
  }

  /** Account state */
  public static class AccountState {
    private final BlockIdExt id;
    private final BlockIdExt shardblk;
    private final byte[] shardProof;
    private final byte[] proof;
    private final byte[] state;

    public AccountState(
        BlockIdExt id, BlockIdExt shardblk, byte[] shardProof, byte[] proof, byte[] state) {
      this.id = id;
      this.shardblk = shardblk;
      this.shardProof = shardProof;
      this.proof = proof;
      this.state = state;
    }

    public BlockIdExt getId() {
      return id;
    }

    public BlockIdExt getShardblk() {
      return shardblk;
    }

    public byte[] getShardProof() {
      return shardProof;
    }

    public byte[] getProof() {
      return proof;
    }

    public byte[] getState() {
      return state;
    }
  }
}
