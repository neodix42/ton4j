package org.ton.ton4j.toncenter;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.ton.ton4j.toncenter.model.*;

import java.io.IOException;
import java.lang.reflect.Type;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * TonCenter API v2 client wrapper using OkHttp for HTTP requests and Gson for JSON serialization.
 * Provides synchronous methods for all TonCenter API endpoints.
 */
@Slf4j
public class TonCenter {

    private static final String MAINNET_ENDPOINT = "https://toncenter.com/api/v2";
    private static final String TESTNET_ENDPOINT = "https://testnet.toncenter.com/api/v2";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    
    private String apiKey;
    private String endpoint;
    private Network network;
    private Duration connectTimeout;
    private Duration readTimeout;
    private Duration writeTimeout;
    
    private OkHttpClient httpClient;
    private Gson gson;
    
    // Private constructor for builder pattern
    private TonCenter() {
    }

    public static class TonCenterBuilder {
        private String apiKey;
        private String endpoint = MAINNET_ENDPOINT;
        private Network network;
        private Duration connectTimeout = Duration.ofSeconds(10);
        private Duration readTimeout = Duration.ofSeconds(30);
        private Duration writeTimeout = Duration.ofSeconds(30);
        
        public TonCenterBuilder apiKey(String apiKey) {
            this.apiKey = apiKey;
            return this;
        }
        
        public TonCenterBuilder endpoint(String endpoint) {
            this.endpoint = endpoint;
            return this;
        }
        
        public TonCenterBuilder connectTimeout(Duration connectTimeout) {
            this.connectTimeout = connectTimeout;
            return this;
        }
        
        public TonCenterBuilder readTimeout(Duration readTimeout) {
            this.readTimeout = readTimeout;
            return this;
        }
        
        public TonCenterBuilder writeTimeout(Duration writeTimeout) {
            this.writeTimeout = writeTimeout;
            return this;
        }
        
        public TonCenterBuilder network(Network network) {
            this.network = network;
            // Set endpoint based on network if not explicitly set
            if (this.endpoint == null || this.endpoint.equals(MAINNET_ENDPOINT) || this.endpoint.equals(TESTNET_ENDPOINT)) {
                this.endpoint = network.getEndpoint();
            }
            return this;
        }
        
        public TonCenterBuilder mainnet() {
            return network(Network.MAINNET);
        }
        
        public TonCenterBuilder testnet() {
            return network(Network.TESTNET);
        }
        
        public TonCenter build() {
            // Ensure endpoint matches network
            if (this.network != null) {
                this.endpoint = this.network.getEndpoint();
            }
            else {
                this.endpoint = endpoint;
            }
            
            TonCenter tonCenter = new TonCenter();
            tonCenter.apiKey = this.apiKey;
            tonCenter.endpoint = this.endpoint;
            tonCenter.network = this.network;
            tonCenter.connectTimeout = this.connectTimeout;
            tonCenter.readTimeout = this.readTimeout;
            tonCenter.writeTimeout = this.writeTimeout;
            tonCenter.initializeClients();
            return tonCenter;
        }
    }

    public static TonCenterBuilder builder() {
        return new TonCenterBuilder();
    }
    
    private void initializeClients() {
        // Initialize Gson
        this.gson = new GsonBuilder()
                .setPrettyPrinting()
                .create();
        
        // Initialize OkHttp client
        OkHttpClient.Builder clientBuilder = new OkHttpClient.Builder()
                .connectTimeout(connectTimeout.toMillis(), TimeUnit.MILLISECONDS)
                .readTimeout(readTimeout.toMillis(), TimeUnit.MILLISECONDS)
                .writeTimeout(writeTimeout.toMillis(), TimeUnit.MILLISECONDS);
        
        // Add API key interceptor if provided
        if (apiKey != null && !apiKey.trim().isEmpty()) {
            clientBuilder.addInterceptor(chain -> {
                Request original = chain.request();
                Request.Builder requestBuilder = original.newBuilder()
                        .header("X-API-Key", apiKey);
                return chain.proceed(requestBuilder.build());
            });
        }
        
        // Add logging interceptor
        clientBuilder.addInterceptor(chain -> {
            Request request = chain.request();
            log.debug("HTTP {} {}", request.method(), request.url());
            
            Response response = chain.proceed(request);
            log.debug("HTTP {} {} -> {}", request.method(), request.url(), response.code());
            
            return response;
        });
        
        this.httpClient = clientBuilder.build();
    }
    
    /**
     * Execute GET request with query parameters
     */
    private <T> TonResponse<T> executeGet(String path, Map<String, String> params, Type responseType) {
        HttpUrl.Builder urlBuilder = HttpUrl.parse(endpoint + path).newBuilder();
        
        // Add query parameters
        if (params != null) {
            for (Map.Entry<String, String> entry : params.entrySet()) {
                if (entry.getValue() != null) {
                    urlBuilder.addQueryParameter(entry.getKey(), entry.getValue());
                }
            }
        }
        
        // Add API key as query parameter if not using header
        if (apiKey != null && !apiKey.trim().isEmpty()) {
            urlBuilder.addQueryParameter("api_key", apiKey);
        }
        
        Request request = new Request.Builder()
                .url(urlBuilder.build())
                .get()
                .build();
        
        return executeRequest(request, responseType);
    }
    
    /**
     * Execute POST request with JSON body
     */
    private <T> TonResponse<T> executePost(String path, Object requestBody, Type responseType) {
        String json = gson.toJson(requestBody);
        RequestBody body = RequestBody.create(json, JSON);
        
        HttpUrl.Builder urlBuilder = HttpUrl.parse(endpoint + path).newBuilder();
        
        // Add API key as query parameter if not using header
        if (apiKey != null && !apiKey.trim().isEmpty()) {
            urlBuilder.addQueryParameter("api_key", apiKey);
        }
        
        Request request = new Request.Builder()
                .url(urlBuilder.build())
                .post(body)
                .build();
        
        return executeRequest(request, responseType);
    }
    
    /**
     * Execute HTTP request and parse response
     */
    private <T> TonResponse<T> executeRequest(Request request, Type responseType) {
        try (Response response = httpClient.newCall(request).execute()) {
            String responseBody = response.body().string();
            
            if (!response.isSuccessful()) {
                throw new TonCenterException("HTTP error: " + response.code() + " " + response.message());
            }
            
            // Parse the response
            TonResponse<T> tonResponse = gson.fromJson(responseBody, responseType);
            
            // Check if API returned an error
            if (tonResponse.isError()) {
                throw new TonCenterApiException(tonResponse.getError(), tonResponse.getCode());
            }
            
            return tonResponse;
            
        } catch (IOException e) {
            throw new TonCenterException("Network error: " + e.getMessage(), e);
        } catch (Exception e) {
            if (e instanceof TonCenterException) {
                throw e;
            }
            throw new TonCenterException("Unexpected error: " + e.getMessage(), e);
        }
    }
    
    // ========== ACCOUNT METHODS ==========
    
    /**
     * Get basic information about the address: balance, code, data, last_transaction_id
     */
    public TonResponse<AddressInformationResponse> getAddressInformation(String address) {
        Map<String, String> params = new HashMap<>();
        params.put("address", address);
        
        Type responseType = new TypeToken<TonResponse<AddressInformationResponse>>(){}.getType();
        return executeGet("/getAddressInformation", params, responseType);
    }
    
    /**
     * Get transaction history of a given address
     */
    public TonResponse<List<TransactionResponse>> getTransactions(String address, Integer limit, Long lt, String hash, Long toLt, Boolean archival) {
        Map<String, String> params = new HashMap<>();
        params.put("address", address);
        if (limit != null) params.put("limit", limit.toString());
        if (lt != null) params.put("lt", lt.toString());
        if (hash != null) params.put("hash", hash);
        if (toLt != null) params.put("to_lt", toLt.toString());
        if (archival != null) params.put("archival", archival.toString());
        
        Type responseType = new TypeToken<TonResponse<List<TransactionResponse>>>(){}.getType();
        return executeGet("/getTransactions", params, responseType);
    }
    
    /**
     * Get balance (in nanotons) of a given address
     */
    public TonResponse<AddressBalanceResponse> getAddressBalance(String address) {
        Map<String, String> params = new HashMap<>();
        params.put("address", address);
        
        Type responseType = new TypeToken<TonResponse<AddressBalanceResponse>>(){}.getType();
        return executeGet("/getAddressBalance", params, responseType);
    }
    
    /**
     * Get state of a given address. State can be either unitialized, active or frozen
     */
    public TonResponse<AddressStateResponse> getAddressState(String address) {
        Map<String, String> params = new HashMap<>();
        params.put("address", address);
        
        Type responseType = new TypeToken<TonResponse<AddressStateResponse>>(){}.getType();
        return executeGet("/getAddressState", params, responseType);
    }
    
    /**
     * Similar to getAddressInformation but tries to parse additional information for known contract types
     */
    public TonResponse<ExtendedAddressInformationResponse> getExtendedAddressInformation(String address) {
        Map<String, String> params = new HashMap<>();
        params.put("address", address);
        
        Type responseType = new TypeToken<TonResponse<ExtendedAddressInformationResponse>>(){}.getType();
        return executeGet("/getExtendedAddressInformation", params, responseType);
    }
    
    /**
     * Retrieve wallet information. Supports more wallet types than getExtendedAddressInformation
     */
    public TonResponse<WalletInformationResponse> getWalletInformation(String address) {
        Map<String, String> params = new HashMap<>();
        params.put("address", address);
        
        Type responseType = new TypeToken<TonResponse<WalletInformationResponse>>(){}.getType();
        return executeGet("/getWalletInformation", params, responseType);
    }
    
    /**
     * Get NFT or Jetton information
     */
    public TonResponse<Object> getTokenData(String address) {
        Map<String, String> params = new HashMap<>();
        params.put("address", address);
        
        Type responseType = new TypeToken<TonResponse<Object>>(){}.getType();
        return executeGet("/getTokenData", params, responseType);
    }
    
    /**
     * Convert an address from raw to human-readable format
     */
    public TonResponse<String> packAddress(String address) {
        Map<String, String> params = new HashMap<>();
        params.put("address", address);
        
        Type responseType = new TypeToken<TonResponse<String>>(){}.getType();
        return executeGet("/packAddress", params, responseType);
    }
    
    /**
     * Convert an address from human-readable to raw format
     */
    public TonResponse<String> unpackAddress(String address) {
        Map<String, String> params = new HashMap<>();
        params.put("address", address);
        
        Type responseType = new TypeToken<TonResponse<String>>(){}.getType();
        return executeGet("/unpackAddress", params, responseType);
    }
    
    /**
     * Get all possible address forms
     */
    public TonResponse<DetectAddressResponse> detectAddress(String address) {
        Map<String, String> params = new HashMap<>();
        params.put("address", address);
        
        Type responseType = new TypeToken<TonResponse<DetectAddressResponse>>(){}.getType();
        return executeGet("/detectAddress", params, responseType);
    }
    
    // ========== BLOCK METHODS ==========
    
    /**
     * Get up-to-date masterchain state
     */
    public TonResponse<MasterchainInfoResponse> getMasterchainInfo() {
        Type responseType = new TypeToken<TonResponse<MasterchainInfoResponse>>(){}.getType();
        return executeGet("/getMasterchainInfo", null, responseType);
    }
    
    /**
     * Get masterchain block signatures
     */
    public TonResponse<Object> getMasterchainBlockSignatures(Integer seqno) {
        Map<String, String> params = new HashMap<>();
        params.put("seqno", seqno.toString());
        
        Type responseType = new TypeToken<TonResponse<Object>>(){}.getType();
        return executeGet("/getMasterchainBlockSignatures", params, responseType);
    }
    
    /**
     * Get merkle proof of shardchain block
     */
    public TonResponse<Object> getShardBlockProof(Integer workchain, Long shard, Long seqno, Integer fromSeqno) {
        Map<String, String> params = new HashMap<>();
        params.put("workchain", workchain.toString());
        params.put("shard", shard.toString());
        params.put("seqno", seqno.toString());
        if (fromSeqno != null) params.put("from_seqno", fromSeqno.toString());
        
        Type responseType = new TypeToken<TonResponse<Object>>(){}.getType();
        return executeGet("/getShardBlockProof", params, responseType);
    }
    
    /**
     * Get consensus block and its update timestamp
     */
    public TonResponse<Object> getConsensusBlock() {
        Type responseType = new TypeToken<TonResponse<Object>>(){}.getType();
        return executeGet("/getConsensusBlock", null, responseType);
    }
    
    /**
     * Look up block by either seqno, lt or unixtime
     */
    public TonResponse<Object> lookupBlock(Integer workchain, Long shard, Integer seqno, Long lt, Integer unixtime) {
        Map<String, String> params = new HashMap<>();
        params.put("workchain", workchain.toString());
        params.put("shard", shard.toString());
        if (seqno != null) params.put("seqno", seqno.toString());
        if (lt != null) params.put("lt", lt.toString());
        if (unixtime != null) params.put("unixtime", unixtime.toString());
        
        Type responseType = new TypeToken<TonResponse<Object>>(){}.getType();
        return executeGet("/lookupBlock", params, responseType);
    }
    
    /**
     * Get shards information
     */
    public TonResponse<ShardsResponse> getShards(Integer seqno) {
        Map<String, String> params = new HashMap<>();
        params.put("seqno", seqno.toString());
        
        Type responseType = new TypeToken<TonResponse<ShardsResponse>>(){}.getType();
        return executeGet("/shards", params, responseType);
    }
    
    /**
     * Get transactions of the given block
     */
    public TonResponse<BlockTransactionsResponse> getBlockTransactions(Integer workchain, Long shard, Long seqno, String rootHash, String fileHash, Long afterLt, String afterHash, Integer count) {
        Map<String, String> params = new HashMap<>();
        params.put("workchain", workchain.toString());
        params.put("shard", shard.toString());
        params.put("seqno", seqno.toString());
        if (rootHash != null) params.put("root_hash", rootHash);
        if (fileHash != null) params.put("file_hash", fileHash);
        if (afterLt != null) params.put("after_lt", afterLt.toString());
        if (afterHash != null) params.put("after_hash", afterHash);
        if (count != null) params.put("count", count.toString());
        
        Type responseType = new TypeToken<TonResponse<BlockTransactionsResponse>>(){}.getType();
        return executeGet("/getBlockTransactions", params, responseType);
    }
    
    /**
     * Get transactions of the given block (extended version)
     */
    public TonResponse<BlockTransactionsResponse> getBlockTransactionsExt(Integer workchain, Long shard, Long seqno, String rootHash, String fileHash, Long afterLt, String afterHash, Integer count) {
        Map<String, String> params = new HashMap<>();
        params.put("workchain", workchain.toString());
        params.put("shard", shard.toString());
        params.put("seqno", seqno.toString());
        if (rootHash != null) params.put("root_hash", rootHash);
        if (fileHash != null) params.put("file_hash", fileHash);
        if (afterLt != null) params.put("after_lt", afterLt.toString());
        if (afterHash != null) params.put("after_hash", afterHash);
        if (count != null) params.put("count", count.toString());
        
        Type responseType = new TypeToken<TonResponse<BlockTransactionsResponse>>(){}.getType();
        return executeGet("/getBlockTransactionsExt", params, responseType);
    }
    
    /**
     * Get metadata of a given block
     */
    public TonResponse<BlockHeaderResponse> getBlockHeader(Integer workchain, Long shard, Long seqno, String rootHash, String fileHash) {
        Map<String, String> params = new HashMap<>();
        params.put("workchain", workchain.toString());
        params.put("shard", shard.toString());
        params.put("seqno", seqno.toString());
        if (rootHash != null) params.put("root_hash", rootHash);
        if (fileHash != null) params.put("file_hash", fileHash);
        
        Type responseType = new TypeToken<TonResponse<BlockHeaderResponse>>(){}.getType();
        return executeGet("/getBlockHeader", params, responseType);
    }
    
    /**
     * Get info with current sizes of messages queues by shards
     */
    public TonResponse<Object> getOutMsgQueueSizes() {
        Type responseType = new TypeToken<TonResponse<Object>>(){}.getType();
        return executeGet("/getOutMsgQueueSizes", null, responseType);
    }
    
    // ========== CONFIGURATION METHODS ==========
    
    /**
     * Get config by id
     */
    public TonResponse<Object> getConfigParam(Integer configId, Integer seqno) {
        Map<String, String> params = new HashMap<>();
        params.put("config_id", configId.toString());
        if (seqno != null) params.put("seqno", seqno.toString());
        
        Type responseType = new TypeToken<TonResponse<Object>>(){}.getType();
        return executeGet("/getConfigParam", params, responseType);
    }
    
    /**
     * Get cell with full config
     */
    public TonResponse<Object> getConfigAll(Integer seqno) {
        Map<String, String> params = new HashMap<>();
        if (seqno != null) params.put("seqno", seqno.toString());
        
        Type responseType = new TypeToken<TonResponse<Object>>(){}.getType();
        return executeGet("/getConfigAll", params, responseType);
    }
    
    // ========== TRANSACTION METHODS ==========
    
    /**
     * Locate outcoming transaction of destination address by incoming message
     */
    public TonResponse<Object> tryLocateTx(String source, String destination, Long createdLt) {
        Map<String, String> params = new HashMap<>();
        params.put("source", source);
        params.put("destination", destination);
        params.put("created_lt", createdLt.toString());
        
        Type responseType = new TypeToken<TonResponse<Object>>(){}.getType();
        return executeGet("/tryLocateTx", params, responseType);
    }
    
    /**
     * Same as tryLocateTx. Locate outcoming transaction of destination address by incoming message
     */
    public TonResponse<Object> tryLocateResultTx(String source, String destination, Long createdLt) {
        Map<String, String> params = new HashMap<>();
        params.put("source", source);
        params.put("destination", destination);
        params.put("created_lt", createdLt.toString());
        
        Type responseType = new TypeToken<TonResponse<Object>>(){}.getType();
        return executeGet("/tryLocateResultTx", params, responseType);
    }
    
    /**
     * Locate incoming transaction of source address by outcoming message
     */
    public TonResponse<Object> tryLocateSourceTx(String source, String destination, Long createdLt) {
        Map<String, String> params = new HashMap<>();
        params.put("source", source);
        params.put("destination", destination);
        params.put("created_lt", createdLt.toString());
        
        Type responseType = new TypeToken<TonResponse<Object>>(){}.getType();
        return executeGet("/tryLocateSourceTx", params, responseType);
    }
    
    // ========== SEND METHODS ==========
    
    /**
     * Send serialized boc file: fully packed and serialized external message to blockchain
     */
    public TonResponse<Object> sendBoc(String boc) {
        SendBocRequest request = new SendBocRequest(boc);
        Type responseType = new TypeToken<TonResponse<Object>>(){}.getType();
        return executePost("/sendBoc", request, responseType);
    }
    
    /**
     * Send serialized boc file and return message hash
     */
    public TonResponse<String> sendBocReturnHash(String boc) {
        SendBocRequest request = new SendBocRequest(boc);
        Type responseType = new TypeToken<TonResponse<String>>(){}.getType();
        return executePost("/sendBocReturnHash", request, responseType);
    }
    
    /**
     * Run get method on smart contract
     */
    public TonResponse<Object> runGetMethod(String address, Object method, List<List<Object>> stack, Integer seqno) {
        RunGetMethodRequest request = new RunGetMethodRequest(address, method, stack, seqno);
        Type responseType = new TypeToken<TonResponse<Object>>(){}.getType();
        return executePost("/runGetMethod", request, responseType);
    }
    
    /**
     * Run get method on smart contract (without seqno)
     */
    public TonResponse<Object> runGetMethod(String address, Object method, List<List<Object>> stack) {
        return runGetMethod(address, method, stack, null);
    }
    
    /**
     * Send query - unpacked external message
     */
    public TonResponse<Object> sendQuery(String address, String body, String initCode, String initData) {
        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("address", address);
        requestMap.put("body", body);
        if (initCode != null && !initCode.isEmpty()) {
            requestMap.put("init_code", initCode);
        }
        if (initData != null && !initData.isEmpty()) {
            requestMap.put("init_data", initData);
        }
        
        Type responseType = new TypeToken<TonResponse<Object>>(){}.getType();
        return executePost("/sendQuery", requestMap, responseType);
    }
    
    /**
     * Estimate fees required for query processing
     */
    public TonResponse<Object> estimateFee(String address, String body, String initCode, String initData, Boolean ignoreChksig) {
        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("address", address);
        requestMap.put("body", body);
        if (initCode != null && !initCode.isEmpty()) {
            requestMap.put("init_code", initCode);
        }
        if (initData != null && !initData.isEmpty()) {
            requestMap.put("init_data", initData);
        }
        if (ignoreChksig != null) {
            requestMap.put("ignore_chksig", ignoreChksig);
        }
        
        Type responseType = new TypeToken<TonResponse<Object>>(){}.getType();
        return executePost("/estimateFee", requestMap, responseType);
    }
    
    /**
     * Estimate fees required for query processing (with default ignoreChksig=true)
     */
    public TonResponse<Object> estimateFee(String address, String body, String initCode, String initData) {
        return estimateFee(address, body, initCode, initData, true);
    }
    
    /**
     * Estimate fees required for query processing (minimal parameters)
     */
    public TonResponse<Object> estimateFee(String address, String body) {
        return estimateFee(address, body, null, null, true);
    }
    
    // ========== CONVENIENCE METHODS ==========
    
    /**
     * Get transaction history with default parameters (limit=10)
     */
    public TonResponse<List<TransactionResponse>> getTransactions(String address) {
        return getTransactions(address, 10, null, null, null, null);
    }
    
    /**
     * Get transaction history with limit
     */
    public TonResponse<List<TransactionResponse>> getTransactions(String address, Integer limit) {
        return getTransactions(address, limit, null, null, null, null);
    }
    
    /**
     * Get config parameter without seqno
     */
    public TonResponse<Object> getConfigParam(Integer configId) {
        return getConfigParam(configId, null);
    }
    
    /**
     * Get full config without seqno
     */
    public TonResponse<Object> getConfigAll() {
        return getConfigAll(null);
    }
    
    /**
     * Get block transactions with minimal parameters
     */
    public TonResponse<BlockTransactionsResponse> getBlockTransactions(Integer workchain, Long shard, Long seqno) {
        return getBlockTransactions(workchain, shard, seqno, null, null, null, null, null);
    }
    
    /**
     * Get block header with minimal parameters
     */
    public TonResponse<BlockHeaderResponse> getBlockHeader(Integer workchain, Long shard, Long seqno) {
        return getBlockHeader(workchain, shard, seqno, null, null);
    }
    
    /**
     * Lookup block by seqno only
     */
    public TonResponse<Object> lookupBlockBySeqno(Integer workchain, Long shard, Integer seqno) {
        return lookupBlock(workchain, shard, seqno, null, null);
    }
    
    /**
     * Lookup block by logical time only
     */
    public TonResponse<Object> lookupBlockByLt(Integer workchain, Long shard, Long lt) {
        return lookupBlock(workchain, shard, null, lt, null);
    }
    
    /**
     * Lookup block by unix time only
     */
    public TonResponse<Object> lookupBlockByUnixtime(Integer workchain, Long shard, Integer unixtime) {
        return lookupBlock(workchain, shard, null, null, unixtime);
    }
    
    /**
     * Get shard block proof with minimal parameters
     */
    public TonResponse<Object> getShardBlockProof(Integer workchain, Long shard, Long seqno) {
        return getShardBlockProof(workchain, shard, seqno, null);
    }
    
    /**
     * Close the HTTP client and release resources
     */
    public void close() {
        if (httpClient != null) {
            httpClient.dispatcher().executorService().shutdown();
            httpClient.connectionPool().evictAll();
        }
    }
}
