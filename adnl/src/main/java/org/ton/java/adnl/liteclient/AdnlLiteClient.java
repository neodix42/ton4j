package org.ton.java.adnl.liteclient;

import org.ton.java.adnl.AdnlClient;
import org.ton.java.adnl.message.MessageCustom;
import org.ton.java.adnl.message.MessageQuery;
import org.ton.java.adnl.liteclient.tl.GetMasterchainInfo;
import org.ton.java.adnl.liteclient.tl.GetAccountState;
import org.ton.java.adnl.liteclient.tl.LiteServerQuery;
import org.ton.java.adnl.liteclient.tl.TLParser;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.io.entity.EntityUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * ADNL-based Lite Client implementation
 * This client connects directly to TON liteservers using the ADNL protocol
 * without requiring the native lite-client binary
 */
public class AdnlLiteClient implements AutoCloseable {
    
    private static final Logger log = Logger.getLogger(AdnlLiteClient.class.getName());
    private static final long DEFAULT_TIMEOUT_MS = 10000; // 10 seconds
    
    private final AdnlClient adnlClient;
    private final Gson gson = new Gson();
    private volatile boolean connected = false;
    
    /**
     * Liteserver configuration
     */
    public static class LiteServerConfig {
        private final String ip;
        private final int port;
        private final byte[] publicKey;
        
        public LiteServerConfig(String ip, int port, byte[] publicKey) {
            this.ip = ip;
            this.port = port;
            this.publicKey = publicKey;
        }
        
        public String getIp() { return ip; }
        public int getPort() { return port; }
        public byte[] getPublicKey() { return publicKey; }
    }
    
    /**
     * Create ADNL Lite Client
     * @param config Liteserver configuration
     */
    public AdnlLiteClient(LiteServerConfig config) {
        // Generate our private key
        byte[] ourPrivateKey = generatePrivateKey();
        
        this.adnlClient = new AdnlClient(
            config.getIp(),
            config.getPort(),
            ourPrivateKey,
            config.getPublicKey()
        );
        
        // Set up message handlers
        setupMessageHandlers();
    }
    
    /**
     * Create ADNL Lite Client from global config file
     * @param configPath Path to global config JSON file
     * @param liteserverIndex Index of liteserver to use (0-based)
     */
    public static AdnlLiteClient fromGlobalConfig(String configPath, int liteserverIndex) throws IOException {
        String configJson = new String(Files.readAllBytes(Paths.get(configPath)));
        JsonObject config = JsonParser.parseString(configJson).getAsJsonObject();
        
        com.google.gson.JsonArray liteservers = config.getAsJsonArray("liteservers");
        if (liteserverIndex >= liteservers.size()) {
            throw new IllegalArgumentException("Liteserver index out of bounds");
        }
        
        JsonObject liteserver = liteservers.get(liteserverIndex).getAsJsonObject();
        String ip = liteserver.get("ip").getAsString();
        int port = liteserver.get("port").getAsInt();
        
        // Decode base64 public key
        String publicKeyB64 = liteserver.getAsJsonObject("id").get("key").getAsString();
        byte[] publicKey = Base64.getDecoder().decode(publicKeyB64);
        
        LiteServerConfig serverConfig = new LiteServerConfig(ip, port, publicKey);
        return new AdnlLiteClient(serverConfig);
    }
    
    /**
     * Create ADNL Lite Client for mainnet (uses first available liteserver)
     */
    public static AdnlLiteClient forMainnet() throws IOException {
        // Download and use mainnet config
        String configPath = downloadGlobalConfig(false);
        return fromGlobalConfig(configPath, 0);
    }
    
    /**
     * Create ADNL Lite Client for testnet (uses first available liteserver)
     */
    public static AdnlLiteClient forTestnet() throws IOException {
        // Download and use testnet config
        String configPath = downloadGlobalConfig(true);
        return fromGlobalConfig(configPath, 0);
    }
    
    /**
     * Connect to the liteserver
     */
    public void connect() throws IOException {
        if (connected) {
            return;
        }
        
        log.info("Connecting to liteserver at " + adnlClient.getAddress() + ":" + adnlClient.getPort());
        adnlClient.connect();
        connected = true;
        log.info("Connected successfully");
    }
    
    /**
     * Test connection with ping
     */
    public long ping() throws Exception {
        if (!connected) {
            throw new IllegalStateException("Not connected");
        }
        
        long startTime = System.nanoTime();
        long rtt = adnlClient.ping(DEFAULT_TIMEOUT_MS);
        long endTime = System.nanoTime();
        
        log.fine("Ping RTT: " + rtt + " ns");
        return rtt;
    }
    
    /**
     * Get last block information using TL serialization
     */
    public MasterchainInfo getMasterchainInfo() throws Exception {
        if (!connected) {
            throw new IllegalStateException("Not connected");
        }
        
        // Create TL query for masterchain info
        GetMasterchainInfo query = new GetMasterchainInfo();
        byte[] response = sendTLQuery(query);
        
        // Parse TL response
        return parseMasterchainInfo(response);
    }
    
    /**
     * Get account state
     */
    public JsonObject getAccountState(String address) throws Exception {
        if (!connected) {
            throw new IllegalStateException("Not connected");
        }
        
        JsonObject query = new JsonObject();
        query.addProperty("@type", "liteServer.getAccountState");
        
        JsonObject accountId = new JsonObject();
        accountId.addProperty("@type", "tonNode.blockIdExt");
        // TODO: Parse address and set workchain/shard/seqno
        
        query.add("id", accountId);
        query.addProperty("account", address);
        
        JsonObject response = sendQuery(query);
        return response;
    }
    
    /**
     * Run get method on smart contract
     */
    public JsonObject runGetMethod(String address, String method, Object... params) throws Exception {
        if (!connected) {
            throw new IllegalStateException("Not connected");
        }
        
        JsonObject query = new JsonObject();
        query.addProperty("@type", "liteServer.runSmcMethod");
        
        // TODO: Implement proper method call serialization
        query.addProperty("account", address);
        query.addProperty("method_id", method);
        
        JsonObject response = sendQuery(query);
        return response;
    }
    
    /**
     * Send TL query to liteserver
     */
    public byte[] sendTLQuery(LiteServerQuery query) throws Exception {
        if (!connected) {
            throw new IllegalStateException("Not connected");
        }
        
        log.fine("Sending TL query: " + query.getClass().getSimpleName());
        
        // Serialize query to TL format
        byte[] queryBytes = query.serialize();
        
        // Send query and wait for response
        Object response = adnlClient.query(queryBytes, byte[].class, DEFAULT_TIMEOUT_MS);
        
        if (response instanceof byte[]) {
            return (byte[]) response;
        } else {
            throw new RuntimeException("Unexpected response type: " + response.getClass());
        }
    }
    
    /**
     * Parse masterchain info from TL response
     */
    private MasterchainInfo parseMasterchainInfo(byte[] response) throws Exception {
        TLParser parser = new TLParser(response);
        
        // Read constructor ID
        int constructorId = parser.readInt32();
        
        // Parse liteServer.MasterchainInfo response
        // last:tonNode.blockIdExt state_root_hash:int256 init:tonNode.zeroStateIdExt = liteServer.MasterchainInfo;
        
        // Parse tonNode.blockIdExt (last block)
        int workchain = parser.readInt32();
        long shard = parser.readInt64();
        int seqno = parser.readInt32();
        byte[] rootHash = parser.readInt256();
        byte[] fileHash = parser.readInt256();
        
        // Parse state_root_hash
        byte[] stateRootHash = parser.readInt256();
        
        // Parse tonNode.zeroStateIdExt (init)
        int initWorkchain = parser.readInt32();
        byte[] initRootHash = parser.readInt256();
        byte[] initFileHash = parser.readInt256();
        
        // For now, we'll use current time as unix time (should be parsed from response)
        int unixTime = (int) (System.currentTimeMillis() / 1000);
        
        return new MasterchainInfo(workchain, shard, seqno, rootHash, fileHash, unixTime);
    }
    
    /**
     * Send raw query to liteserver (legacy JSON method)
     */
    public JsonObject sendQuery(JsonObject query) throws Exception {
        if (!connected) {
            throw new IllegalStateException("Not connected");
        }
        
        log.fine("Sending query: " + query);
        
        // Convert JSON to bytes (this would need proper TL serialization in production)
        byte[] queryBytes = query.toString().getBytes();
        
        // Send query and wait for response
        Object response = adnlClient.query(queryBytes, Object.class, DEFAULT_TIMEOUT_MS);
        
        // Convert response back to JSON (this would need proper TL deserialization)
        if (response instanceof byte[]) {
            String responseStr = new String((byte[]) response);
            return JsonParser.parseString(responseStr).getAsJsonObject();
        } else {
            return gson.toJsonTree(response).getAsJsonObject();
        }
    }
    
    /**
     * Send custom message
     */
    public void sendCustomMessage(Object message) throws Exception {
        if (!connected) {
            throw new IllegalStateException("Not connected");
        }
        
        adnlClient.sendCustomMessage(message);
    }
    
    /**
     * Close the connection
     */
    @Override
    public void close() {
        if (connected) {
            log.info("Closing connection");
            adnlClient.close();
            connected = false;
        }
    }
    
    /**
     * Check if connected
     */
    public boolean isConnected() {
        return connected && adnlClient.isConnected();
    }
    
    /**
     * Set up message handlers
     */
    private void setupMessageHandlers() {
        adnlClient.setCustomMessageHandler(this::handleCustomMessage);
        adnlClient.setQueryHandler(this::handleQuery);
        adnlClient.setDisconnectHandler(this::handleDisconnect);
    }
    
    /**
     * Handle incoming custom message
     */
    private void handleCustomMessage(MessageCustom message) {
        log.fine("Received custom message: " + message);
        // Handle custom messages from liteserver
    }
    
    /**
     * Handle incoming query
     */
    private void handleQuery(MessageQuery query) {
        log.fine("Received query: " + query);
        // Handle queries from liteserver (usually not expected)
    }
    
    /**
     * Handle disconnect
     */
    private void handleDisconnect() {
        log.warning("Disconnected from liteserver");
        connected = false;
    }
    
    /**
     * Generate random private key
     */
    private byte[] generatePrivateKey() {
        byte[] privateKey = new byte[32];
        new SecureRandom().nextBytes(privateKey);
        return privateKey;
    }
    
    /**
     * Download global config
     */
    private static String downloadGlobalConfig(boolean testnet) throws IOException {
        String url = testnet ? 
            "https://ton.org/testnet-global.config.json" :
            "https://ton.org/global-config.json";
        
        String filename = testnet ? "testnet-global.config.json" : "global.config.json";
        
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet request = new HttpGet(url);
            
            return httpClient.execute(request, response -> {
                String content = EntityUtils.toString(response.getEntity());
                
                // Save to file
                Files.write(Paths.get(filename), content.getBytes());
                
                return filename;
            });
        }
    }
    
    /**
     * Example usage and testing
     */
    public static void main(String[] args) {
        try {
            // Create client for testnet
            AdnlLiteClient client = AdnlLiteClient.forTestnet();
            
            // Connect
            client.connect();
            
            // Test ping
            long rtt = client.ping();
            System.out.println("Ping RTT: " + rtt + " ns");
            
            // Get masterchain info
            MasterchainInfo masterchainInfo = client.getMasterchainInfo();
            System.out.println("Masterchain info: " + masterchainInfo);
            
            // Close
            client.close();
            
        } catch (Exception e) {
            log.severe("Error in ADNL Lite Client: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
