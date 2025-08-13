package org.ton.ton4j.toncenter;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.ton.ton4j.address.Address;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellBuilder;
import org.ton.ton4j.cell.CellSlice;
import org.ton.ton4j.tlb.Message;
import org.ton.ton4j.toncenter.model.*;
import org.ton.ton4j.utils.Utils;

import static java.util.Objects.nonNull;
import static org.ton.ton4j.toncenter.model.CommonResponses.*;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.math.BigInteger;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.UUID;

/**
 * TonCenter API v2 client wrapper using OkHttp for HTTP requests and Gson for JSON serialization.
 * Provides synchronous methods for all TonCenter API endpoints.
 */
@Slf4j
public class TonCenter {

  private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

  private String apiKey;
  private String endpoint;
  private Network network;
  private Duration connectTimeout;
  private Duration readTimeout;
  private Duration writeTimeout;
  private boolean debug;
  private boolean uniqueRequests;
  private OkHttpClient httpClient;
  private Gson gson;

  private final int SNAKE_DATA_PREFIX = 0x00;
  private final int CHUNK_DATA_PREFIX = 0x01;
  private final int ONCHAIN_CONTENT_PREFIX = 0x00;
  private final int OFFCHAIN_CONTENT_PREFIX = 0x01;

  // Private constructor for builder pattern
  private TonCenter() {}

  public static class TonCenterBuilder {
    private String apiKey;
    private String endpoint = Network.MAINNET.getEndpoint();
    private Network network;
    private Duration connectTimeout = Duration.ofSeconds(10);
    private Duration readTimeout = Duration.ofSeconds(30);
    private Duration writeTimeout = Duration.ofSeconds(30);
    private boolean debug;
    private boolean uniqueRequests;

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
      if (this.endpoint == null
          || this.endpoint.equals(Network.MAINNET.getEndpoint())
          || this.endpoint.equals(Network.TESTNET.getEndpoint())) {
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

    /**
     * when set prints GET or POST URLs
     * @return TonCenterBuilder
     */
    public TonCenterBuilder debug() {
      this.debug = true;
      return this;
    }

    /**
     * when set appends each GET or POST request with a random parameter t=UUID
     * @return TonCenterBuilder
     */
    public TonCenterBuilder uniqueRequests() {
      this.uniqueRequests = true;
      return this;
    }

    public TonCenter build() {
      // Ensure endpoint matches network
      if (this.network != null) {
        this.endpoint = this.network.getEndpoint();
      }

      TonCenter tonCenter = new TonCenter();
      tonCenter.apiKey = this.apiKey;
      tonCenter.endpoint = this.endpoint;
      tonCenter.network = this.network;
      tonCenter.debug = this.debug;
      tonCenter.uniqueRequests = this.uniqueRequests;
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
    this.gson = new GsonBuilder().setPrettyPrinting().create();

    // Initialize OkHttp client
    OkHttpClient.Builder clientBuilder =
        new OkHttpClient.Builder()
            .connectTimeout(connectTimeout.toMillis(), TimeUnit.MILLISECONDS)
            .readTimeout(readTimeout.toMillis(), TimeUnit.MILLISECONDS)
            .writeTimeout(writeTimeout.toMillis(), TimeUnit.MILLISECONDS);

    // Add API key interceptor if provided
    if (apiKey != null && !apiKey.trim().isEmpty()) {
      clientBuilder.addInterceptor(
          chain -> {
            Request original = chain.request();
            Request.Builder requestBuilder = original.newBuilder().header("X-API-Key", apiKey);
            return chain.proceed(requestBuilder.build());
          });
    }

    // Add logging interceptor
    clientBuilder.addInterceptor(
        chain -> {
          Request request = chain.request();
          Response response = chain.proceed(request);
          if (debug) {
            log.info("HTTP {} {} -> {}", request.method(), request.url(), response.code());
          }

          return response;
        });

    this.httpClient = clientBuilder.build();
  }

  /** Execute GET request with query parameters */
  private <T> TonResponse<T> executeGet(
      String path, Map<String, String> params, Type responseType) {
    HttpUrl.Builder urlBuilder = HttpUrl.parse(endpoint + path).newBuilder();

    // Add query parameters
    if (nonNull(params)) {
      for (Map.Entry<String, String> entry : params.entrySet()) {
        urlBuilder.addQueryParameter(entry.getKey(), entry.getValue());
      }
    }

    // Add API key as query parameter if not using header
    if (nonNull(apiKey) && !apiKey.trim().isEmpty()) {
      urlBuilder.addQueryParameter("api_key", apiKey);
    }
    
    // Add random parameter for unique requests if enabled
    if (uniqueRequests) {
      urlBuilder.addQueryParameter("t", UUID.randomUUID().toString());
    }

    Request request = new Request.Builder().url(urlBuilder.build()).get().build();

    return executeRequest(request, responseType);
  }

  /** Execute POST request with JSON body */
  private <T> TonResponse<T> executePost(String path, Object requestBody, Type responseType) {
    String json = gson.toJson(requestBody);
    RequestBody body = RequestBody.create(json, JSON);

    HttpUrl.Builder urlBuilder = HttpUrl.parse(endpoint + path).newBuilder();

    // Add API key as query parameter if not using header
    if (nonNull(apiKey) && !apiKey.trim().isEmpty()) {
      urlBuilder.addQueryParameter("api_key", apiKey);
    }
    
    // Add random parameter for unique requests if enabled
    if (uniqueRequests) {
      urlBuilder.addQueryParameter("t", UUID.randomUUID().toString());
    }

    Request request = new Request.Builder().url(urlBuilder.build()).post(body).build();

    return executeRequest(request, responseType);
  }

  /** Execute HTTP request and parse response */
  private <T> TonResponse<T> executeRequest(Request request, Type responseType) {
    try (Response response = httpClient.newCall(request).execute()) {
      String responseBody = response.body().string();

      if (!response.isSuccessful()) {
        // Try to parse error response from API
        try {
          Type errorResponseType = new TypeToken<TonResponse<Object>>() {}.getType();
          TonResponse<Object> errorResponse = gson.fromJson(responseBody, errorResponseType);

          if (errorResponse != null && errorResponse.getError() != null) {
            // API returned structured error - use the error code from the response if available
            Integer errorCode =
                errorResponse.getCode() != null ? errorResponse.getCode() : response.code();
            throw new TonCenterApiException(errorResponse.getError(), errorCode);
          }
        } catch (TonCenterApiException e) {
          // Re-throw TonCenterApiException
          throw e;
        } catch (Exception parseException) {
          // If we can't parse the error response, fall back to basic HTTP error
          log.debug("Could not parse error response: {}", parseException.getMessage());
        }

        // Fallback to basic HTTP error with response body if available
        String errorMessage = "HTTP error: " + response.code();
        if (nonNull(responseBody) && !responseBody.trim().isEmpty()) {
          errorMessage += " - " + responseBody;
        } else if (nonNull(response.message())) {
          errorMessage += " " + response.message();
        }
        throw new TonCenterException(errorMessage);
      }

      // Parse the response
      TonResponse<T> tonResponse = gson.fromJson(responseBody, responseType);

      // Check if API returned an error
      if (tonResponse != null && tonResponse.isError()) {
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

  /** Get basic information about the address: balance, code, data, last_transaction_id */
  public TonResponse<AddressInformationResponse> getAddressInformation(String address) {
    Map<String, String> params = new HashMap<>();
    params.put("address", address);

    Type responseType = new TypeToken<TonResponse<AddressInformationResponse>>() {}.getType();
    return executeGet("/getAddressInformation", params, responseType);
  }

  /**
   * Similar to getAddressInformation but tries to parse additional information for known contract
   * types
   */
  public TonResponse<ExtendedAddressInformationResponse> getExtendedAddressInformation(
      String address) {
    Map<String, String> params = new HashMap<>();
    params.put("address", address);

    Type responseType =
        new TypeToken<TonResponse<ExtendedAddressInformationResponse>>() {}.getType();
    return executeGet("/getExtendedAddressInformation", params, responseType);
  }

  /** Retrieve wallet information. Supports more wallet types than getExtendedAddressInformation */
  public TonResponse<WalletInformationResponse> getWalletInformation(String address) {
    Map<String, String> params = new HashMap<>();
    params.put("address", address);

    Type responseType = new TypeToken<TonResponse<WalletInformationResponse>>() {}.getType();
    return executeGet("/getWalletInformation", params, responseType);
  }

  /** Get transaction history of a given address */
  public TonResponse<List<TransactionResponse>> getTransactions(
      String address, Integer limit, Long lt, String hash, Long toLt, Boolean archival) {
    Map<String, String> params = new HashMap<>();
    params.put("address", address);
    if (nonNull(limit)) params.put("limit", limit.toString());
    if (nonNull(lt)) params.put("lt", lt.toString());
    if (nonNull(hash)) params.put("hash", hash);
    if (nonNull(toLt)) params.put("to_lt", toLt.toString());
    if (nonNull(archival)) params.put("archival", archival.toString());

    Type responseType = new TypeToken<TonResponse<List<TransactionResponse>>>() {}.getType();
    return executeGet("/getTransactions", params, responseType);
  }

  /** Get balance (in nanotons) of a given address */
  public TonResponse<String> getAddressBalance(String address) {
    Map<String, String> params = new HashMap<>();
    params.put("address", address);

    Type responseType = new TypeToken<TonResponse<String>>() {}.getType();
    return executeGet("/getAddressBalance", params, responseType);
  }

  public BigInteger getBalance(String address) {
    Map<String, String> params = new HashMap<>();
    params.put("address", address);

    Type responseType = new TypeToken<TonResponse<String>>() {}.getType();
    TonResponse<String> res = executeGet("/getAddressBalance", params, responseType);
    //    log.info(res.toString());
    if (nonNull(res) && res.isSuccess()) {
      return new BigInteger(res.getResult());
    } else {
      throw new Error(res.getError());
    }
  }

  /** Get state of a given address. State can be either unitialized, active or frozen */
  public TonResponse<String> getAddressState(String address) {
    Map<String, String> params = new HashMap<>();
    params.put("address", address);

    Type responseType = new TypeToken<TonResponse<String>>() {}.getType();
    return executeGet("/getAddressState", params, responseType);
  }

  public String getState(String address) {
    Map<String, String> params = new HashMap<>();
    params.put("address", address);

    Type responseType = new TypeToken<TonResponse<String>>() {}.getType();
    TonResponse<String> res = executeGet("/getAddressState", params, responseType);
    if (nonNull(res) && res.isSuccess()) {
      return res.getResult();
    } else {
      throw new Error(res.getError());
    }
  }

  /** Convert an address from raw to human-readable format */
  public TonResponse<String> packAddress(String address) {
    Map<String, String> params = new HashMap<>();
    params.put("address", address);

    Type responseType = new TypeToken<TonResponse<String>>() {}.getType();
    return executeGet("/packAddress", params, responseType);
  }

  /** Convert an address from human-readable to raw format */
  public TonResponse<String> unpackAddress(String address) {
    Map<String, String> params = new HashMap<>();
    params.put("address", address);

    Type responseType = new TypeToken<TonResponse<String>>() {}.getType();
    return executeGet("/unpackAddress", params, responseType);
  }

  /** Get all possible address forms */
  public TonResponse<DetectAddressResponse> detectAddress(String address) {
    Map<String, String> params = new HashMap<>();
    params.put("address", address);

    Type responseType = new TypeToken<TonResponse<DetectAddressResponse>>() {}.getType();
    return executeGet("/detectAddress", params, responseType);
  }

  /** Get NFT or Jetton information */
  public TonResponse<TokenDataResponse> getTokenData(String address) {
    Map<String, String> params = new HashMap<>();
    params.put("address", address);

    Type responseType = new TypeToken<TonResponse<TokenDataResponse>>() {}.getType();
    return executeGet("/getTokenData", params, responseType);
  }

  // ========== BLOCK METHODS ==========

  /** Get up-to-date masterchain state */
  public TonResponse<MasterchainInfoResponse> getMasterchainInfo() {
    Type responseType = new TypeToken<TonResponse<MasterchainInfoResponse>>() {}.getType();
    return executeGet("/getMasterchainInfo", null, responseType);
  }

  /** Get masterchain block signatures */
  public TonResponse<MasterchainBlockSignaturesResponse> getMasterchainBlockSignatures(Long seqno) {
    Map<String, String> params = new HashMap<>();
    params.put("seqno", seqno.toString());

    Type responseType =
        new TypeToken<TonResponse<MasterchainBlockSignaturesResponse>>() {}.getType();
    return executeGet("/getMasterchainBlockSignatures", params, responseType);
  }

  /** Get merkle proof of shardchain block */
  public TonResponse<ShardBlockProofResponse> getShardBlockProof(
      Integer workchain, Long shard, Long seqno, Long fromSeqno) {
    Map<String, String> params = new HashMap<>();
    params.put("workchain", workchain.toString());
    params.put("shard", shard.toString());
    params.put("seqno", seqno.toString());
    if (fromSeqno != null) params.put("from_seqno", fromSeqno.toString());

    Type responseType = new TypeToken<TonResponse<ShardBlockProofResponse>>() {}.getType();
    return executeGet("/getShardBlockProof", params, responseType);
  }

  /** Get consensus block and its update timestamp */
  public TonResponse<ConsensusBlockResponse> getConsensusBlock() {
    Type responseType = new TypeToken<TonResponse<ConsensusBlockResponse>>() {}.getType();
    return executeGet("/getConsensusBlock", null, responseType);
  }

  /** Look up block by either seqno, lt or unixtime */
  public TonResponse<LookupBlockResponse> lookupBlock(
      Integer workchain, Long shard, Long seqno, Long lt, Long unixtime) {
    Map<String, String> params = new HashMap<>();
    params.put("workchain", workchain.toString());
    params.put("shard", shard.toString());
    if (seqno != null) params.put("seqno", seqno.toString());
    if (lt != null) params.put("lt", lt.toString());
    if (unixtime != null) params.put("unixtime", unixtime.toString());

    Type responseType = new TypeToken<TonResponse<LookupBlockResponse>>() {}.getType();
    return executeGet("/lookupBlock", params, responseType);
  }

  /** Get shards information */
  public TonResponse<ShardsResponse> getShards(Long seqno) {
    Map<String, String> params = new HashMap<>();
    params.put("seqno", seqno.toString());

    Type responseType = new TypeToken<TonResponse<ShardsResponse>>() {}.getType();
    return executeGet("/shards", params, responseType);
  }

  /** Get transactions of the given block */
  public TonResponse<BlockTransactionsResponse> getBlockTransactions(
      Integer workchain,
      Long shard,
      Long seqno,
      String rootHash,
      String fileHash,
      Long afterLt,
      String afterHash,
      Integer count) {
    Map<String, String> params = new HashMap<>();
    params.put("workchain", workchain.toString());
    params.put("shard", shard.toString());
    params.put("seqno", seqno.toString());
    if (rootHash != null) params.put("root_hash", rootHash);
    if (fileHash != null) params.put("file_hash", fileHash);
    if (afterLt != null) params.put("after_lt", afterLt.toString());
    if (afterHash != null) params.put("after_hash", afterHash);
    if (count != null) params.put("count", count.toString());

    Type responseType = new TypeToken<TonResponse<BlockTransactionsResponse>>() {}.getType();
    return executeGet("/getBlockTransactions", params, responseType);
  }

  /** Get transactions of the given block (extended version) */
  public TonResponse<BlockTransactionsResponse> getBlockTransactionsExt(
      Integer workchain,
      Long shard,
      Long seqno,
      String rootHash,
      String fileHash,
      Long afterLt,
      String afterHash,
      Integer count) {
    Map<String, String> params = new HashMap<>();
    params.put("workchain", workchain.toString());
    params.put("shard", shard.toString());
    params.put("seqno", seqno.toString());
    if (rootHash != null) params.put("root_hash", rootHash);
    if (fileHash != null) params.put("file_hash", fileHash);
    if (afterLt != null) params.put("after_lt", afterLt.toString());
    if (afterHash != null) params.put("after_hash", afterHash);
    if (count != null) params.put("count", count.toString());

    Type responseType = new TypeToken<TonResponse<BlockTransactionsResponse>>() {}.getType();
    return executeGet("/getBlockTransactionsExt", params, responseType);
  }

  /** Get metadata of a given block */
  public TonResponse<BlockHeaderResponse> getBlockHeader(
      Integer workchain, Long shard, Long seqno, String rootHash, String fileHash) {
    Map<String, String> params = new HashMap<>();
    params.put("workchain", workchain.toString());
    params.put("shard", shard.toString());
    params.put("seqno", seqno.toString());
    if (rootHash != null) params.put("root_hash", rootHash);
    if (fileHash != null) params.put("file_hash", fileHash);

    Type responseType = new TypeToken<TonResponse<BlockHeaderResponse>>() {}.getType();
    return executeGet("/getBlockHeader", params, responseType);
  }

  /** Get info with current sizes of messages queues by shards */
  public TonResponse<OutMsgQueueSizesResponse> getOutMsgQueueSizes() {
    Type responseType = new TypeToken<TonResponse<OutMsgQueueSizesResponse>>() {}.getType();
    return executeGet("/getOutMsgQueueSizes", null, responseType);
  }

  // ========== CONFIGURATION METHODS ==========

  /** Get config by id */
  public TonResponse<ConfigParamResponse> getConfigParam(Integer configId, Long seqno) {
    Map<String, String> params = new HashMap<>();
    params.put("config_id", configId.toString());
    if (seqno != null) params.put("seqno", seqno.toString());

    Type responseType = new TypeToken<TonResponse<ConfigParamResponse>>() {}.getType();
    return executeGet("/getConfigParam", params, responseType);
  }

  /** Get cell with full config */
  public TonResponse<ConfigAllResponse> getConfigAll(Long seqno) {
    Map<String, String> params = new HashMap<>();
    if (seqno != null) params.put("seqno", seqno.toString());

    Type responseType = new TypeToken<TonResponse<ConfigAllResponse>>() {}.getType();
    return executeGet("/getConfigAll", params, responseType);
  }

  // ========== TRANSACTION METHODS ==========

  /** Locate outcoming transaction of destination address by incoming message */
  public TonResponse<LocateTxResponse> tryLocateTx(
      String source, String destination, Long createdLt) {
    Map<String, String> params = new HashMap<>();
    params.put("source", source);
    params.put("destination", destination);
    params.put("created_lt", createdLt.toString());

    Type responseType = new TypeToken<TonResponse<LocateTxResponse>>() {}.getType();
    return executeGet("/tryLocateTx", params, responseType);
  }

  /**
   * Same as tryLocateTx. Locate outcoming transaction of destination address by incoming message
   */
  public TonResponse<LocateTxResponse> tryLocateResultTx(
      String source, String destination, Long createdLt) {
    Map<String, String> params = new HashMap<>();
    params.put("source", source);
    params.put("destination", destination);
    params.put("created_lt", createdLt.toString());

    Type responseType = new TypeToken<TonResponse<LocateTxResponse>>() {}.getType();
    return executeGet("/tryLocateResultTx", params, responseType);
  }

  /** Locate incoming transaction of source address by outcoming message */
  public TonResponse<LocateTxResponse> tryLocateSourceTx(
      String source, String destination, Long createdLt) {
    Map<String, String> params = new HashMap<>();
    params.put("source", source);
    params.put("destination", destination);
    params.put("created_lt", createdLt.toString());

    Type responseType = new TypeToken<TonResponse<LocateTxResponse>>() {}.getType();
    return executeGet("/tryLocateSourceTx", params, responseType);
  }

  // ========== RUN METHOD ==========

  /** Run get method on smart contract */
  public TonResponse<RunGetMethodResponse> runGetMethod(
      String address, Object method, List<List<Object>> stack, Long seqno) {
    RunGetMethodRequest request = new RunGetMethodRequest(address, method, stack, seqno);
    Type responseType = new TypeToken<TonResponse<RunGetMethodResponse>>() {}.getType();
    return executePost("/runGetMethod", request, responseType);
  }

  /** Run get method on smart contract (without seqno) */
  public TonResponse<RunGetMethodResponse> runGetMethod(
      String address, Object method, List<List<Object>> stack) {
    return runGetMethod(address, method, stack, null);
  }

  public long getSeqno(String address) {
    RunGetMethodResponse r = runGetMethod(address, "seqno", new ArrayList<>()).getResult();
    //    log.info("seqno {}", r);
    if ((nonNull(r)) && (r.getExitCode() == 0)) {
      return Long.decode((String) new ArrayList<>(r.getStack().get(0)).get(1));
    } else {
      throw new Error("getSeqno failed, exitCode: " + r.getExitCode());
    }
  }

  public long getSeqno(String address, Long atSeqno) {
    RunGetMethodResponse r = runGetMethod(address, "seqno", new ArrayList<>(), atSeqno).getResult();
    if ((nonNull(r)) && (r.getExitCode() == 0)) {
      return Long.decode((String) new ArrayList<>(r.getStack().get(0)).get(1));
    } else {
      throw new Error("getSeqno failed, exitCode: " + r.getExitCode());
    }
  }

  public BigInteger getPublicKey(String address) {
    RunGetMethodResponse r = runGetMethod(address, "get_public_key", new ArrayList<>()).getResult();
    if ((nonNull(r)) && (r.getExitCode() == 0)) {
      String pubKey = ((String) new ArrayList<>(r.getStack().get(0)).get(1));
      return new BigInteger(pubKey.substring(2), 16);
    } else {
      throw new Error("get_public_key failed, exitCode: " + r.getExitCode());
    }
  }

  public BigInteger getPublicKey(String address, Long atSeqno) {
    RunGetMethodResponse r =
        runGetMethod(address, "get_public_key", new ArrayList<>(), atSeqno).getResult();
    if ((nonNull(r)) && (r.getExitCode() == 0)) {
      String pubKey = ((String) new ArrayList<>(r.getStack().get(0)).get(1));
      return new BigInteger(pubKey.substring(2), 16);
    } else {
      throw new Error("get_public_key failed, exitCode: " + r.getExitCode());
    }
  }

  public long getSubWalletId(String address) {
    RunGetMethodResponse r =
        runGetMethod(address, "get_subwallet_id", new ArrayList<>()).getResult();
    if ((nonNull(r)) && (r.getExitCode() == 0)) {
      String pubKey = ((String) new ArrayList<>(r.getStack().get(0)).get(1));
      return new BigInteger(pubKey.substring(2), 16).longValue();
    } else {
      throw new Error("get_subwallet_id failed, exitCode: " + r.getExitCode());
    }
  }

  public long getSubWalletId(String address, long atSeqno) {
    RunGetMethodResponse r =
        runGetMethod(address, "get_subwallet_id", new ArrayList<>(), atSeqno).getResult();
    if ((nonNull(r)) && (r.getExitCode() == 0)) {
      String pubKey = ((String) new ArrayList<>(r.getStack().get(0)).get(1));
      return new BigInteger(pubKey.substring(2), 16).longValue();
    } else {
      throw new Error("get_subwallet_id failed, exitCode: " + r.getExitCode());
    }
  }

  /**
   * Get jetton wallet address for a given owner
   *
   * @param jettonMasterAddress Jetton master contract address
   * @param ownerAddress Owner wallet address
   * @return Address of the jetton wallet
   */
  public Address getJettonWalletAddress(String jettonMasterAddress, String ownerAddress) {
    List<List<Object>> stack = new ArrayList<>();
    List<Object> addressParam = new ArrayList<>();
    addressParam.add("slice");
    addressParam.add(ownerAddress);
    stack.add(addressParam);

    RunGetMethodResponse response =
        runGetMethod(jettonMasterAddress, "get_wallet_address", stack).getResult();
    String jettonWalletAddressHex = ((String) new ArrayList<>(response.getStack().get(0)).get(1));
    return Address.of(jettonWalletAddressHex);
  }

  /**
   * Get jetton data from a jetton master contract
   *
   * @param jettonMasterAddress Jetton master contract address
   * @return JettonMinterData object containing jetton information
   */
  public JettonMinterData getJettonData(String jettonMasterAddress) {
    List<List<Object>> stack = new ArrayList<>();
    RunGetMethodResponse response =
        runGetMethod(jettonMasterAddress, "get_jetton_data", stack).getResult();

    // Parse total supply
    String totalSupplyHex = ((String) new ArrayList<>(response.getStack().get(0)).get(1));
    BigInteger totalSupply = new BigInteger(totalSupplyHex.substring(2), 16);

    // Parse is_mutable flag
    String isMutableHex = ((String) new ArrayList<>(response.getStack().get(1)).get(1));
    boolean isMutable = new BigInteger(isMutableHex.substring(2), 16).intValue() == -1;

    // Parse admin address
    String adminAddressHex = ((String) new ArrayList<>(response.getStack().get(2)).get(1));
    Address adminAddress = Address.of(adminAddressHex);

    // Parse content cell
    String contentCellHex = ((String) new ArrayList<>(response.getStack().get(3)).get(1));
    Cell jettonContentCell =
        CellBuilder.beginCell().fromBoc(Utils.base64ToBytes(contentCellHex)).endCell();

    // Parse jetton wallet code
    String walletCodeHex = ((String) new ArrayList<>(response.getStack().get(4)).get(1));
    Cell jettonWalletCode =
        CellBuilder.beginCell().fromBoc(Utils.base64ToBytes(walletCodeHex)).endCell();

    // Parse content URI if possible
    String jettonContentUri = null;
    try {
      jettonContentUri = parseOffChainUriCell(jettonContentCell);
    } catch (Error e) {
      // Ignore if can't parse URI
    }

    return JettonMinterData.builder()
        .totalSupply(totalSupply)
        .isMutable(isMutable)
        .adminAddress(adminAddress)
        .jettonContentCell(jettonContentCell)
        .jettonContentUri(jettonContentUri)
        .jettonWalletCode(jettonWalletCode)
        .build();
  }

  // ========== SEND METHODS ==========

  /** Send serialized boc file: fully packed and serialized external message to blockchain */
  public TonResponse<SendBocResponse> sendBoc(String boc) {
    SendBocRequest request = new SendBocRequest(boc);
    Type responseType = new TypeToken<TonResponse<SendBocResponse>>() {}.getType();
    return executePost("/sendBoc", request, responseType);
  }

  /** Send serialized boc file and return message hash */
  public TonResponse<SendBocResponse> sendBocReturnHash(String boc) {
    SendBocRequest request = new SendBocRequest(boc);
    Type responseType = new TypeToken<TonResponse<SendBocResponse>>() {}.getType();
    return executePost("/sendBocReturnHash", request, responseType);
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
      TonResponse<SendBocResponse> result = sendBocReturnHash(externalMessage.toCell().toBase64());
      if (result.isSuccess()) {
        String hash = result.getResult().getHash();
        String hashHex = Utils.base64ToHexString(result.getResult().getHash());
        log.info(
            "Message has been successfully sent. Waiting for delivery of message with hash {}",
            hash);

        for (int i = 0; i < 12; i++) {
          TonResponse<List<TransactionResponse>> txs = getTransactions(account.toBounceable());
          for (TransactionResponse tx : txs.getResult()) {
            if (nonNull(tx.getInMsg())) {
              if (hashHex.equals(Utils.base64ToHexString(tx.getInMsg().getHash()))) {
                log.info("Message has been delivered.");
                return;
              }
            }
          }
          Utils.sleep(5);
        }
      } else {
        log.error("Error sending BoC");
        throw new Error("Error sending BoC");
      }
      log.error("Timeout waiting for message hash");
      throw new Error("Cannot find hash of the sent message");
    } catch (Exception e) {
      log.error("Error sending BoC", e);
      throw new Error("Cannot find hash of the sent message, Error: " + e.getMessage());
    }
  }

  /** Send query - unpacked external message */
  public TonResponse<SendQueryResponse> sendQuery(
      String address, String body, String initCode, String initData) {
    Map<String, Object> requestMap = new HashMap<>();
    requestMap.put("address", address);
    requestMap.put("body", body);
    if (initCode != null && !initCode.isEmpty()) {
      requestMap.put("init_code", initCode);
    }
    if (initData != null && !initData.isEmpty()) {
      requestMap.put("init_data", initData);
    }

    Type responseType = new TypeToken<TonResponse<SendQueryResponse>>() {}.getType();
    return executePost("/sendQuery", requestMap, responseType);
  }

  /** Estimate fees required for query processing */
  public TonResponse<EstimateFeeResponse> estimateFee(
      String address, String body, String initCode, String initData, Boolean ignoreChksig) {
    EstimateFeeRequest request = new EstimateFeeRequest();
    request.setAddress(address);
    request.setBody(body);
    if (initCode != null && !initCode.isEmpty()) {
      request.setInitCode(initCode);
    }
    if (initData != null && !initData.isEmpty()) {
      request.setInitData(initData);
    }
    if (ignoreChksig != null) {
      request.setIgnoreChksig(ignoreChksig);
    }

    Type responseType = new TypeToken<TonResponse<EstimateFeeResponse>>() {}.getType();
    return executePost("/estimateFee", request, responseType);
  }

  // ========== CONVENIENCE METHODS ==========

  /** Get transaction history with default parameters (limit=10) */
  public TonResponse<List<TransactionResponse>> getTransactions(String address) {
    return getTransactions(address, 10, null, null, null, null);
  }

  /** Get transaction history with limit */
  public TonResponse<List<TransactionResponse>> getTransactions(String address, Integer limit) {
    return getTransactions(address, limit, null, null, null, null);
  }

  /** Get config parameter without seqno */
  public TonResponse<ConfigParamResponse> getConfigParam(Integer configId) {
    return getConfigParam(configId, null);
  }

  /** Get full config without seqno */
  public TonResponse<ConfigAllResponse> getConfigAll() {
    return getConfigAll(null);
  }

  /** Get block transactions with minimal parameters */
  public TonResponse<BlockTransactionsResponse> getBlockTransactions(
      Integer workchain, Long shard, Long seqno) {
    return getBlockTransactions(workchain, shard, seqno, null, null, null, null, null);
  }

  /** Get block header with minimal parameters */
  public TonResponse<BlockHeaderResponse> getBlockHeader(
      Integer workchain, Long shard, Long seqno) {
    return getBlockHeader(workchain, shard, seqno, null, null);
  }

  /** Lookup block by seqno only */
  public TonResponse<LookupBlockResponse> lookupBlockBySeqno(
      Integer workchain, Long shard, Long seqno) {
    return lookupBlock(workchain, shard, seqno, null, null);
  }

  /** Lookup block by logical time only */
  public TonResponse<LookupBlockResponse> lookupBlockByLt(Integer workchain, Long shard, Long lt) {
    return lookupBlock(workchain, shard, null, lt, null);
  }

  /** Lookup block by unix time only */
  public TonResponse<LookupBlockResponse> lookupBlockByUnixtime(
      Integer workchain, Long shard, Long unixtime) {
    return lookupBlock(workchain, shard, null, null, unixtime);
  }

  /** Get shard block proof with minimal parameters */
  public TonResponse<ShardBlockProofResponse> getShardBlockProof(
      Integer workchain, Long shard, Long seqno) {
    return getShardBlockProof(workchain, shard, seqno, null);
  }

  /** Estimate fees required for query processing (with default ignoreChksig=true) */
  public TonResponse<EstimateFeeResponse> estimateFee(
      String address, String body, String initCode, String initData) {
    return estimateFee(address, body, initCode, initData, true);
  }

  /** Estimate fees required for query processing (minimal parameters) */
  public TonResponse<EstimateFeeResponse> estimateFee(String address, String body) {
    return estimateFee(address, body, null, null, true);
  }

  /** Close the HTTP client and release resources */
  public void close() {
    if (httpClient != null) {
      httpClient.dispatcher().executorService().shutdown();
      httpClient.connectionPool().evictAll();
    }
  }

  /**
   * @param cell Cell
   * @return String
   */
  private String parseOffChainUriCell(Cell cell) {
    if ((cell.getBits().toByteArray()[0] & 0xFF) != OFFCHAIN_CONTENT_PREFIX) {
      throw new Error("not OFFCHAIN_CONTENT_PREFIX");
    }

    return parseUri(CellSlice.beginParse(cell).skipBits(8).loadSnakeString());
  }

  private String parseUri(String uri) {
    try {
      return URLDecoder.decode(uri, String.valueOf(StandardCharsets.UTF_8));
    } catch (UnsupportedEncodingException e) {
      log.info(e.getMessage());
      return null;
    }
  }
}
