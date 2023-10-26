package org.ton.java.tonlib;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonElement;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;
import com.google.gson.ToNumberPolicy;
import com.sun.jna.Native;
import lombok.Builder;
import lombok.extern.java.Log;
import org.ton.java.address.Address;
import org.ton.java.tonlib.queries.*;
import org.ton.java.tonlib.types.*;
import org.ton.java.utils.Utils;

import java.io.InputStream;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

@Log
@Builder
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

    /**
     * Ignored if pathToGlobalConfig is not null.
     */
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

    private final TonlibJsonI tonlibJson;

    private static final Gson gson = new GsonBuilder().setObjectToNumberStrategy(ToNumberPolicy.BIG_DECIMAL).create();

    private final long tonlib;

    private boolean synced;

    private final AtomicLong counter = new AtomicLong(0);

    public static TonlibBuilder builder() {
        return new CustomTonlibBuilder();
    }

    private static class CustomTonlibBuilder extends TonlibBuilder {
        @Override
        public Tonlib build() {

            try {
                String tonlibName = null;
                switch (Utils.getOS()) {
                    case LINUX:
                        tonlibName = "tonlibjson.so";
                        break;
                    case LINUX_ARM:
                        tonlibName = "tonlibjson-arm.so";
                        break;
                    case WINDOWS:
                        tonlibName = "tonlibjson.dll";
                        break;
                    case WINDOWS_ARM:
                        tonlibName = "tonlibjson-arm.dll";
                        break;
                    case MAC:
                        tonlibName = "tonlibjson.dylib";
                        break;
                    case MAC_ARM64:
                        tonlibName = "tonlibjson-arm.dylib";
                        break;
                    case UNKNOWN:
                        throw new Error("Operating system is not supported!");
                }

                if (isNull(super.pathToTonlibSharedLib)) {
                    super.pathToTonlibSharedLib = tonlibName;
                }

                if (isNull(super.verbosityLevel)) {
                    super.verbosityLevel = VerbosityLevel.FATAL;
                }

                if (isNull(super.keystorePath)) {
                    super.keystorePath = ".";
                }

                if (super.receiveRetryTimes == 0) {
                    super.receiveRetryTimes = 3;
                }

                if (super.receiveTimeout == 0) {
                    super.receiveTimeout = 10.0;
                }

                super.synced = false;

                String configData = null;
                if (isNull(super.pathToGlobalConfig) && isNull(super.configData)) {
                    InputStream config;
                    if (super.testnet) {
                        super.pathToGlobalConfig = "testnet-global.config.json (integrated resource)";
                        config = Tonlib.class.getClassLoader().getResourceAsStream("testnet-global.config.json");
                    } else {
                        super.pathToGlobalConfig = "global-config.json (integrated resource)";
                        config = Tonlib.class.getClassLoader().getResourceAsStream("global-config.json");
                    }
                    configData = Utils.streamToString(config);

                    if (nonNull(config)) {
                        config.close();
                    }
                }
                else if(nonNull(super.pathToGlobalConfig) && isNull(super.configData)) {
                    if (Files.exists(Paths.get(super.pathToGlobalConfig))) {
                        configData = new String(Files.readAllBytes(Paths.get(super.pathToGlobalConfig)));
                    } else {
                        throw new RuntimeException("Global config is not found in path: " + super.pathToGlobalConfig);
                    }
                }
                else if(isNull(super.pathToGlobalConfig)){
                    configData = super.configData;
                }

                super.tonlibJson = Native.load(super.pathToTonlibSharedLib, TonlibJsonI.class);
                super.tonlib = super.tonlibJson.tonlib_client_json_create();

                System.out.printf("Java Tonlib configuration:\n" +
                                "Location: %s\n" +
                                "Verbosity level: %s (%s)\n" +
                                "Keystore in memory: %s\n" +
                                "Keystore path: %s\nPath to global config: %s\n" +
                                "Testnet: %s\n" +
                                "Receive timeout: %s seconds\n" +
                                "Receive retry times: %s%n\n",
                                "Raw configuration: %s%n",
                        super.pathToTonlibSharedLib, super.verbosityLevel, super.verbosityLevel.ordinal(),
                        super.keystoreInMemory, super.keystorePath, super.pathToGlobalConfig,
                        super.testnet, super.receiveTimeout, super.receiveRetryTimes,super.configData);

                VerbosityLevelQuery verbosityLevelQuery = VerbosityLevelQuery.builder().new_verbosity_level(super.verbosityLevel.ordinal()).build();

                super.tonlibJson.tonlib_client_json_send(super.tonlib, gson.toJson(verbosityLevelQuery));

                String result = super.tonlibJson.tonlib_client_json_receive(super.tonlib, super.receiveTimeout);
                System.out.println("set verbosityLevel result: " + result);

                String initTemplate = Utils.streamToString(Tonlib.class.getClassLoader().getResourceAsStream("init.json"));

                String dataQuery = JsonParser.parseString(configData).getAsJsonObject().toString();

                String q = initTemplate.replace("CFG_PLACEHOLDER", dataQuery);
                if (super.keystoreInMemory) {
                    String keystoreMemory = " 'keystore_type': { '@type': 'keyStoreTypeInMemory' }";
                    q = q.replace("KEYSTORE_TYPE", keystoreMemory);
                } else {
                    String keystoreDirectory = "'keystore_type': {'@type': 'keyStoreTypeDirectory', 'directory': '.' } ";
                    if (super.keystorePath.equals(".")) {
                        q = q.replace("KEYSTORE_TYPE", keystoreDirectory);
                    } else {
                        q = q.replace("KEYSTORE_TYPE", keystoreDirectory.replace(".", super.keystorePath));
                    }
                }

                String setupQueryQ = JsonParser.parseString(q).getAsJsonObject().toString();

                super.tonlibJson.tonlib_client_json_send(super.tonlib, setupQueryQ);

                result = super.tonlibJson.tonlib_client_json_receive(super.tonlib, super.receiveTimeout);
                System.out.println("set tonlib configuration result " + result);

            } catch (Exception e) {
                throw new RuntimeException("Error creating tonlib instance: " + e.getMessage());
            }
            return super.build();
        }
    }

    public void destroy() {
        tonlibJson.tonlib_client_json_destroy(tonlib);
    }

    private String send(String query) {
        String tag = getNextQueryTag();
        query = addTagToQuery(query, tag);
        System.out.println("Sending: " + query);
        try {
            tonlibJson.tonlib_client_json_send(tonlib, query);
            return tag;
        } catch (Throwable e) {
            System.err.println("Error in tonlib.send(), sending query: " + query);
            return "";
        }
    }
   
    private String getNextQueryTag() {
        long extra = counter.incrementAndGet(); 
        return Long.toString(extra);
    }

    private String addTagToQuery(String query, String tag) {
        JsonObject el = gson.fromJson(query, JsonObject.class);
        el.addProperty("@extra", tag);
        return gson.toJson(el);
    }

    private String receive(String tag) {
        int retry = 0;
        while (retry <= receiveRetryTimes) {
            String result = tonlibJson.tonlib_client_json_receive(tonlib, receiveTimeout);
            if (isNull(result)) {
                retry++;
                continue;
            }
            
            JsonObject el = gson.fromJson(result, JsonObject.class);
            if (!el.has("@type")) {
                retry++;
                continue; // WTF?
            }
            String el_type = el.get("@type").getAsString();
            if (el_type.equals ("updateSendLiteServerQuery")) {
                continue;
            }
            if (el_type.equals ("updateSyncState")) {
                JsonObject sync_state = el.get("sync_state").getAsJsonObject();
                if (isNull(sync_state)) {
                    continue;
                }

                String sync_state_type = sync_state.get("@type").getAsString();
                if (sync_state_type.equals("syncStateDone")) {
                    if (!synced) {
                        System.out.println("Synchronized");
                    }
                    synced = true;
                    continue;
                }

                int from_seqno = sync_state.get("from_seqno").getAsInt();
                int to_seqno = sync_state.get("to_seqno").getAsInt();
                int cur_seqno = sync_state.get("current_seqno").getAsInt();

                if (to_seqno <= 0) {
                    to_seqno = 1;
                }

                System.out.println("Synchronizing: " + String.format("%.2f%%", cur_seqno * 100.0 / to_seqno));
                /* we can parse sync state, but we don't use it */
                continue;
            }

            if (!el.has("@extra")) {
                /* also strange */
                continue;
            }
                
            JsonElement extra = el.get("@extra");
            if (!extra.isJsonPrimitive()) {
                /* also strange */
                continue;
            }
            if (!extra.getAsString().equals(tag)) {
                continue;
            }

            el.remove("@extra");
            return gson.toJson(el);
        }
        return null;
    }

    private String syncAndRead(String tag) {
        try {
            return receive(tag);
        } catch (Exception e) {
            log.info("Cannot sync with blockchain. Error: " + e.getMessage());
            return null;
        }
    }

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
        LookupBlockQuery lookupBlockQuery = LookupBlockQuery.builder()
                .mode(mode)
                .id(BlockId.builder()
                        .seqno(seqno)
                        .workchain(workchain)
                        .shard(shard).build())
                .lt(lt)
                .utime(utime)
                .build();

        String blockQuery = gson.toJson(lookupBlockQuery);

        String tag = send(blockQuery);

        String result = syncAndRead(tag);
        return gson.fromJson(result, BlockIdExt.class);
    }

    public MasterChainInfo getLast() {
        GetLastQuery getLastQuery = GetLastQuery.builder().build();

        String tag = send(gson.toJson(getLastQuery));
        String result = syncAndRead(tag);
        return gson.fromJson(result, MasterChainInfo.class);
    }

    public Shards getShards(BlockIdExt id) {
        GetShardsQuery getShardsQuery = GetShardsQuery.builder()
                .id(id)
                .build();

        String tag = send(gson.toJson(getShardsQuery));

        String result = syncAndRead(tag);
        return gson.fromJson(result, Shards.class);
    }

    public Shards getShards(long seqno, long lt, long unixtime) {
        if ((seqno <= 0) && (lt <= 0) && (unixtime <= 0)) {
            throw new Error("Seqno, LT or unixtime should be defined");
        }

        long wc = -1;
        long shard = -9223372036854775808L;

        BlockIdExt fullblock = lookupBlock(seqno, wc, shard, lt, unixtime);

        GetShardsQuery getShardsQuery = GetShardsQuery.builder()
                .id(fullblock)
                .build();

        String tag = send(gson.toJson(getShardsQuery));

        String result = syncAndRead(tag);
        return gson.fromJson(result, Shards.class);
    }

    public Key createNewKey() {
        NewKeyQuery newKeyQuery = NewKeyQuery.builder().build();

        String tag = send(gson.toJson(newKeyQuery));

        String result = syncAndRead(tag);
        return gson.fromJson(result, Key.class);
    }

    public Data encrypt(String data, String secret) {
        EncryptQuery encryptQuery = EncryptQuery.builder()
                .decrypted_data(data)
                .secret(secret).build();

        String tag = send(gson.toJson(encryptQuery));

        String result = syncAndRead(tag);
        return gson.fromJson(result, Data.class);
    }

    public Data decrypt(String data, String secret) {
        DecryptQuery decryptQuery = DecryptQuery.builder()
                .encrypted_data(data)
                .secret(secret).build();

        String tag = send(gson.toJson(decryptQuery));

        String result = syncAndRead(tag);
        return gson.fromJson(result, Data.class);
    }

    public BlockHeader getBlockHeader(BlockIdExt fullblock) {
        BlockHeaderQuery blockHeaderQuery = BlockHeaderQuery.builder()
                .id(fullblock).build();

        String tag = send(gson.toJson(blockHeaderQuery));

        String result = syncAndRead(tag);
        return gson.fromJson(result, BlockHeader.class);
    }
    
    public DnsResolved dnsResolve(String name, AccountAddressOnly addr) {
        if (addr == null) {
          addr = AccountAddressOnly.builder()
              .account_address("-1:E56754F83426F69B09267BD876AC97C44821345B7E266BD956A7BFBFB98DF35C")
              .build ();
        }
        byte[] category = new byte[32];
        Arrays.fill (category, (byte)0);
        DnsResolveQuery query = DnsResolveQuery.builder()
                .account_address (addr)
                .name (name)
                .category (Utils.bytesToBase64 (category))
                .ttl (1)
                .build();

        String tag = send(gson.toJson(query));
        String result = syncAndRead(tag);
        return gson.fromJson(result, DnsResolved.class);
    }
    
    //@formatter:off

    /**
     * TL Spec:
     * raw.getTransactions account_address:accountAddress from_transaction_id:internal.transactionId = raw.Transactions;
     * accountAddress account_address:string = AccountAddress;
     * internal.transactionId lt:int64 hash:bytes = internal.TransactionId;
     * :param account_address: str with raw or user friendly address
     * :param from_transaction_lt: from transaction lt
     * :param from_transaction_hash: from transaction hash in HEX representation
     * :return: dict as
     * {
     * '@type': 'raw.transactions',
     * 'transactions': list[dict as {
     * '@type': 'raw.transaction',
     * 'utime': int,
     * 'data': str,
     * 'transaction_id': internal.transactionId,
     * 'fee': str,
     * 'in_msg': dict as {
     * '@type': 'raw.message',
     * 'source': str,
     * 'destination': str,
     * 'value': str,
     * 'message': str
     * },
     * 'out_msgs': list[dict as raw.message]
     * }],
     * 'previous_transaction_id': internal.transactionId
     * }
     */
    //@formatter:on
    public RawTransactions getRawTransactions(String address, BigInteger fromTxLt, String fromTxHash) {

        if ((fromTxLt == null) || (fromTxHash == null)) {
            FullAccountState fullAccountState = getAccountState(AccountAddressOnly.builder().account_address(address).build());
            fromTxLt = fullAccountState.getLast_transaction_id().getLt();
            fromTxHash = fullAccountState.getLast_transaction_id().getHash();
        }
        GetRawTransactionsQuery getRawTransactionsQuery = GetRawTransactionsQuery.builder()
                .account_address(AccountAddressOnly.builder().account_address(address).build())
                .from_transaction_id(LastTransactionId.builder()
                        .lt(fromTxLt)
                        .hash(fromTxHash)
                        .build())
                .build();

        String tag = send(gson.toJson(getRawTransactionsQuery));

        String result = syncAndRead(tag);
        return gson.fromJson(result, RawTransactions.class);
    }

    public RawTransaction getRawTransaction(byte workchain, ShortTxId tx) {
        String addressHex = Utils.base64ToHexString(tx.getAccount());
        String address = Address.of(workchain + ":" + addressHex).toString(false);
        GetRawTransactionsV2Query getRawTransactionsQuery = GetRawTransactionsV2Query.builder()
                .account_address(AccountAddressOnly.builder().account_address(address).build())
                .from_transaction_id(LastTransactionId.builder()
                        .lt(BigInteger.valueOf(tx.getLt()))
                        .hash(tx.getHash())
                        .build())
                .count(1)
                .try_decode_message(false)
                .build();

        String tag = send(gson.toJson(getRawTransactionsQuery));

        String result = syncAndRead(tag);
        RawTransactions res = gson.fromJson(result, RawTransactions.class);
        List<RawTransaction> t = res.getTransactions ();
        if (t.size() >= 1) {
          return t.get(0);
        } else {
          return RawTransaction.builder().build();
        }
    }

    public RawTransactions getAllRawTransactions(String address, BigInteger fromTxLt, String fromTxHash, long historyLimit) {

        List<RawTransaction> transactions = new ArrayList<>();
        RawTransactions rawTransactions = getRawTransactions(address, fromTxLt, fromTxHash);
        transactions.addAll(rawTransactions.getTransactions());

        while (rawTransactions.getPrevious_transaction_id().getLt().compareTo(BigInteger.ZERO) != 0) {
            rawTransactions = getRawTransactions(address, rawTransactions.getPrevious_transaction_id().getLt(), rawTransactions.getPrevious_transaction_id().getHash());
            transactions.addAll(rawTransactions.getTransactions());
            if (transactions.size() > historyLimit) {
                break;
            }
        }

        rawTransactions.setTransactions(transactions);
        return rawTransactions;
    }

    public BlockTransactions getBlockTransactions(BlockIdExt fullblock, long count, long afterLt, String afterHash) {
        AccountTransactionId afterTx = AccountTransactionId.builder()
                .account(afterHash)
                .lt(afterLt)
                .build();

        return getBlockTransactions(fullblock, count, afterTx);
    }

    public BlockTransactions getBlockTransactions(BlockIdExt fullblock, long count) {

        return getBlockTransactions(fullblock, count, null);
    }

    public BlockTransactions getBlockTransactions(BlockIdExt fullblock, long count, AccountTransactionId afterTx) {
        int mode = 7;
        if (afterTx != null) {
            mode = 7 + 128;
        }

        if (afterTx == null) {
            afterTx = AccountTransactionId.builder()
                    .account("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=")
                    .lt(0)
                    .build();
        }

        GetBlockTransactionsQuery getBlockTransactionsQuery = GetBlockTransactionsQuery.builder()
                .id(fullblock)
                .mode(mode)
                .count(count)
                .after(afterTx)
                .build();

        String tag = send(gson.toJson(getBlockTransactionsQuery));

        String result = syncAndRead(tag);

        return gson.fromJson(result, BlockTransactions.class);
    }

    /**
     * @param fullblock - workchain, shard, seqno, root-hash, file-hash
     * @param count     - limit result
     * @param afterTx   - filter out Tx before this one
     * @return Map<String, RawTransactions>
     */
    public Map<String, RawTransactions> getAllBlockTransactions(BlockIdExt fullblock, long count, AccountTransactionId afterTx) {
        Map<String, RawTransactions> totalTxs = new HashMap();
        BlockTransactions blockTransactions = getBlockTransactions(fullblock, count, afterTx);
        for (ShortTxId tx : blockTransactions.getTransactions()) {
            String addressHex = Utils.base64ToHexString(tx.getAccount());
            String address = Address.of(fullblock.getWorkchain() + ":" + addressHex).toString(false);
            RawTransactions rawTransactions = getRawTransactions(address, BigInteger.valueOf(tx.getLt()), tx.getHash());
            totalTxs.put(address + "|" + tx.getLt(), rawTransactions);
        }
        return totalTxs;
    }

    public RawAccountState getRawAccountState(AccountAddressOnly address) {
        GetRawAccountStateQueryOnly getAccountStateQuery = GetRawAccountStateQueryOnly.builder().account_address(address).build();

        String tag = send(gson.toJson(getAccountStateQuery));

        String result = syncAndRead(tag);
        return gson.fromJson(result, RawAccountState.class);
    }

    public FullAccountState getAccountState(AccountAddressOnly address) {
        GetAccountStateQueryOnly getAccountStateQuery = GetAccountStateQueryOnly.builder().account_address(address).build();

        String tag = send(gson.toJson(getAccountStateQuery));

        String result = syncAndRead(tag);
        return gson.fromJson(result, FullAccountState.class);
    }

    public FullAccountState getAccountState(Address address) {
        AccountAddressOnly accountAddressOnly = AccountAddressOnly.builder()
                .account_address(address.toString(false))
                .build();
        GetAccountStateQueryOnly getAccountStateQuery = GetAccountStateQueryOnly.builder().account_address(accountAddressOnly).build();

        String tag = send(gson.toJson(getAccountStateQuery));

        String result = syncAndRead(tag);
        return gson.fromJson(result, FullAccountState.class);
    }



    /*
    public String getConfigParam(BlockIdExt id, long param, long mode) throws InterruptedException {

        GetConfigParamQuery configParamQuery = GetConfigParamQuery.builder()
                .id(id)
                .param(param)
                .build();

        Gson gson = new GsonBuilder().disableHtmlEscaping().create();

        String query = gson.toJson(configParamQuery);
        log.info(query);
        send(query);
        String result = receive();

        waitForSyncDone(result);

        result = receive();

        log.info(result);
        return "";//gson.fromJson(result, FullAccountState.class);
    }
    */

    public long loadContract(AccountAddressOnly address) {
        LoadContractQuery loadContractQuery = LoadContractQuery.builder()
                .account_address(address)
                .extra(UUID.randomUUID().toString())
                .build();

        String tag = send(gson.toJson(loadContractQuery));

        String result = syncAndRead(tag);

        return gson.fromJson(result, LoadContract.class).getId();

    }

    public RunResult runMethod(Address contractAddress, String methodName) {
        long contractId = loadContract(AccountAddressOnly.builder().account_address(contractAddress.toString(false)).build());
        if (contractId == -1) {
            System.err.println("cannot load contract " + AccountAddressOnly.builder().account_address(contractAddress.toString(false)));
            return null;
        } else {
            return runMethod(contractId, methodName, null);
        }
    }

    public RunResult runMethod(Address contractAddress, String methodName, Deque<String> stackData) {
        long contractId = loadContract(AccountAddressOnly.builder().account_address(contractAddress.toString(false)).build());
        if (contractId == -1) {
            System.err.println("cannot load contract " + AccountAddressOnly.builder().account_address(contractAddress.toString(false)));
            return null;
        } else {
            return runMethod(contractId, methodName, stackData);
        }
    }

    public RunResult runMethod(long contractId, String methodName, Deque<String> stackData) {

        Deque<TvmStackEntry> stack = null;
        if (stackData != null) {
            stack = ParseRunResult.renderTvmStack(stackData);
        }

        RunMethodStrQuery runMethodQuery = RunMethodStrQuery.builder()
                .id(contractId)
                .method(MethodString.builder().name(methodName).build())
                .stack(stack)
                .extra(UUID.randomUUID().toString())
                .build();

        String tag = send(gson.toJson(runMethodQuery));

        String result = syncAndRead(tag);

        RunResultGeneric<String> g = gson.fromJson(result, RunResultGeneric.class);

        return ParseRunResult.getTypedRunResult(g.getStack(), g.getExit_code(), g.getGas_used(), g.getExtra());
    }

    /**
     * Sends raw message, bag of cells encoded in base64
     *
     * @param serializedBoc - base64 encoded BoC
     * @return ExtMessageInfo In case of error might contain error code and message inside
     */
    public ExtMessageInfo sendRawMessage(String serializedBoc) {
        SendRawMessageQuery sendMessageQuery = SendRawMessageQuery.builder()
                .body(serializedBoc).build();

        String tag = send(gson.toJson(sendMessageQuery));

        String result = syncAndRead(tag);

        if ((result == null) || (result.contains("@type") && result.contains("error"))) {
            TonlibError error = gson.fromJson(result, TonlibError.class);
            return ExtMessageInfo.builder()
                    .error(error)
                    .build();
        } else {
            ExtMessageInfo extMessageInfo = gson.fromJson(result, ExtMessageInfo.class);
            extMessageInfo.setError(TonlibError.builder().code(0).build());
            return extMessageInfo;
        }
    }

    public QueryFees estimateFees(String destinationAddress, String body, String initCode, String initData, boolean ignoreChksig) {
        QueryInfo queryInfo = createQuery(destinationAddress, body, initCode, initData);

        EstimateFeesQuery estimateFeesQuery = EstimateFeesQuery.builder()
                .queryId(queryInfo.getId())
                .ignore_chksig(ignoreChksig).build();

        String tag = send(gson.toJson(estimateFeesQuery));

        String result = syncAndRead(tag);

        return gson.fromJson(result, QueryFees.class);
    }

    /**
     * Creates query with body, init-code and init-data to be sent to the destination address
     *
     * @param initCode           - base64 encoded boc
     * @param initData           - base64 encoded boc
     * @param body               - base64 encoded boc
     * @param destinationAddress - friendly or unfriendly address
     * @return QueryInfo
     */
    public QueryInfo createQuery(String destinationAddress, String body, String initCode, String initData) {
        CreateQuery createQuery = CreateQuery.builder()
                .init_code(initCode)
                .init_data(initData)
                .body(body)
                .destination(Destination.builder().account_address(destinationAddress).build())
                .build();

        String tag = send(gson.toJson(createQuery));

        String result = syncAndRead(tag);

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
        SendQuery createQuery = SendQuery.builder()
                .id(queryInfo.getId())
                .build();

        String tag = send(gson.toJson(createQuery));

        String result = syncAndRead(tag);

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

    public boolean createAndSendQuery(String destinationAddress, String body, String initCode, String initData) {
        QueryInfo queryInfo = createQuery(destinationAddress, body, initCode, initData);
        return sendQuery(queryInfo);
    }

    /**
     * very close to createAndSendQuery, but StateInit should be generated outside
     *
     * @param destinationAddress  - friendly or unfriendly wallet address
     * @param body                - serialized base64 encoded body
     * @param initialAccountState - serialized base64 initial account state
     * @return true if query was sent without errors
     */
    public boolean createAndSendMessage(String destinationAddress, String body, String initialAccountState) {
        CreateAndSendRawMessageQuery createAndSendRawMessageQuery = CreateAndSendRawMessageQuery.builder()
                .destination(AccountAddressOnly.builder().account_address(destinationAddress).build())
                .initial_account_state(initialAccountState)
                .data(body)
                .build();

        String tag = send(gson.toJson(createAndSendRawMessageQuery));

        String result = syncAndRead(tag);

        if (result == null) {
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
