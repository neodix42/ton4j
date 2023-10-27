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
import java.util.concurrent.CompletableFuture; 
import java.util.concurrent.locks.ReentrantLock; 
import java.lang.Thread;

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

    private TonlibJsonI tonlibJson;

    private static final Gson gson = new GsonBuilder().setObjectToNumberStrategy(ToNumberPolicy.BIG_DECIMAL).create();

    private long tonlib;

    private boolean synced;
    private boolean crashedDuringInit;
    private boolean isStopping;


    public static TonlibBuilder builder() {
        return new CustomTonlibBuilder();
    }

    private HashMap<String, CompletableFuture<String>> pending_futures;
    private ReentrantLock mutex;
    private AtomicLong counter;
    

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
                super.crashedDuringInit = false;
                super.isStopping = false;

                super.pending_futures = new HashMap<String, CompletableFuture<String>>();
                super.mutex = new ReentrantLock();
                super.counter = new AtomicLong(0);

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
                super.configData = configData;

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
            } catch (Exception e) {
                throw new RuntimeException("Error creating tonlib instance: " + e.getMessage());
            }
            Tonlib tonlib = super.build();
            tonlib.spawnMainThread();
            return tonlib;
        }
    }

    public void destroy() {
        tonlibJson.tonlib_client_json_destroy(tonlib);
    }

    private void removePendingFuture(String tag) {
      mutex.lock();
      try {
          if (pending_futures.containsKey(tag)) {
              pending_futures.remove(tag);
          }
      } finally {
          mutex.unlock();
      }
    }

    private String sendAndReceive(String query, double timeout) {
        if (timeout == 0) {
            timeout = receiveTimeout;
        }
        String tag = getNextQueryTag();
  
        CompletableFuture<String> future = new CompletableFuture<String>();
       
        mutex.lock();
        pending_futures.put(tag, future);
        mutex.unlock();

        query = addTagToQuery(query, tag);
        System.out.println("Sending: " + query);
        try {
            tonlibJson.tonlib_client_json_send(tonlib, query);
        } catch (Throwable e) {
            removePendingFuture(tag);
            System.err.println("Error in tonlib.send(), sending query: " + query);
            return null;
        }
      
        try {
          String result = future.get((long)(timeout * 1000), TimeUnit.MILLISECONDS);
          removePendingFuture(tag);
          return result;
        } catch (Exception e) {
          removePendingFuture(tag);
          return null;
        }
    }
    
    private String sendAndReceive(String query) {
      return sendAndReceive(query, 0);
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

    private void initTonlib() {
        try {
            tonlibJson = Native.load(pathToTonlibSharedLib, TonlibJsonI.class);
            tonlib = tonlibJson.tonlib_client_json_create();

            VerbosityLevelQuery verbosityLevelQuery = VerbosityLevelQuery.builder().new_verbosity_level(verbosityLevel.ordinal()).build();

            tonlibJson.tonlib_client_json_send(tonlib, gson.toJson(verbosityLevelQuery));

            String result = tonlibJson.tonlib_client_json_receive(tonlib, receiveTimeout);
            System.out.println("set verbosityLevel result: " + result);

            String initTemplate = Utils.streamToString(Tonlib.class.getClassLoader().getResourceAsStream("init.json"));
            
            System.out.println("Found initTemplate " + initTemplate);

            String dataQuery = JsonParser.parseString(configData).getAsJsonObject().toString();
            
            System.out.println("Sending data query " + dataQuery);

            String q = initTemplate.replace("CFG_PLACEHOLDER", dataQuery);
            if (keystoreInMemory) {
                String keystoreMemory = " 'keystore_type': { '@type': 'keyStoreTypeInMemory' }";
                q = q.replace("KEYSTORE_TYPE", keystoreMemory);
            } else {
                String keystoreDirectory = "'keystore_type': {'@type': 'keyStoreTypeDirectory', 'directory': '.' } ";
                if (keystorePath.equals(".")) {
                    q = q.replace("KEYSTORE_TYPE", keystoreDirectory);
                } else {
                    q = q.replace("KEYSTORE_TYPE", keystoreDirectory.replace(".", keystorePath));
                }
            }

            String setupQueryQ = JsonParser.parseString(q).getAsJsonObject().toString();

            System.out.println("Sending setup query " + setupQueryQ);

            tonlibJson.tonlib_client_json_send(tonlib, setupQueryQ);

            result = tonlibJson.tonlib_client_json_receive(tonlib, receiveTimeout);
            System.out.println("set tonlib configuration result " + result);
        } catch (Exception e) {
            System.out.println("caught exception " + e.toString());
            crashedDuringInit = true;
        }
    }

    private void runMainThread() {
        while (!isStopping) {
            String result = tonlibJson.tonlib_client_json_receive(tonlib, 1.0);
            if (isNull(result)) {
                continue;
            }
            
            JsonObject el = gson.fromJson(result, JsonObject.class);
            if (!el.has("@type")) {
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

            String tag = extra.getAsString();
            el.remove("@extra");
           
            CompletableFuture<String> future = null;

            mutex.lock();
            try {
              if (pending_futures.containsKey(tag)) {
                future = pending_futures.get(tag);
                pending_futures.remove(tag);
              }
            } finally {
              mutex.unlock();
            }
               
            if (!isNull(future)) {
                future.complete(gson.toJson(el));
            }
        }
    }

    private static class TonlibMainThread extends Thread {
        private Tonlib tonlib;

        TonlibMainThread(Tonlib _tonlib) {
          tonlib = _tonlib;
        }

        @Override public void run() {
            tonlib.initTonlib();
            tonlib.runMainThread();
        }
    };

    void spawnMainThread() {
        TonlibMainThread thread = new TonlibMainThread(this);
        thread.start();
    }

    private static boolean resultIsError(String result) {
            JsonObject el = gson.fromJson(result, JsonObject.class);
            if (!el.has("@type")) {
                return false;
            }
            String el_type = el.get("@type").getAsString();
            return el_type.equals("error");
    }

    public BlockIdExt waitSync() throws Exception {
        while (true) {
            String result = sendAndReceive("{\"@type\":\"sync\"}", 3600);
            if (isNull(result) || resultIsError(result) || !synced) {
                if (crashedDuringInit) {
                    throw new Error("tonlib crashed during initialization");
                }
                try {
                    TimeUnit.SECONDS.sleep(1);
                } catch (Exception e) {
                }
                continue;
            } else {
                return gson.fromJson(result, BlockIdExt.class);
            }
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

        String result = sendAndReceive(gson.toJson(lookupBlockQuery));
        if (isNull(result) || resultIsError(result)) {
            return null;
        } else {
            return gson.fromJson(result, BlockIdExt.class);
        }
    }

    public MasterChainInfo getLast() {
        GetLastQuery getLastQuery = GetLastQuery.builder().build();

        String result = sendAndReceive(gson.toJson(getLastQuery));
        if (isNull(result) || resultIsError(result)) {
            return null;
        } else {
            return gson.fromJson(result, MasterChainInfo.class);
        }
    }

    public Shards getShards(BlockIdExt id) {
        GetShardsQuery getShardsQuery = GetShardsQuery.builder()
                .id(id)
                .build();

        String result = sendAndReceive(gson.toJson(getShardsQuery));
        if (isNull(result) || resultIsError(result)) {
            return null;
        } else {
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

        GetShardsQuery getShardsQuery = GetShardsQuery.builder()
                .id(fullblock)
                .build();

        String result = sendAndReceive(gson.toJson(getShardsQuery));
        if (isNull(result) || resultIsError(result)) {
            return null;
        } else {
            return gson.fromJson(result, Shards.class);
        }
    }

    public Key createNewKey() {
        NewKeyQuery newKeyQuery = NewKeyQuery.builder().build();

        String result = sendAndReceive(gson.toJson(newKeyQuery));
        if (isNull(result) || resultIsError(result)) {
            return null;
        } else {
            return gson.fromJson(result, Key.class);
        }
    }

    public Data encrypt(String data, String secret) {
        EncryptQuery encryptQuery = EncryptQuery.builder()
                .decrypted_data(data)
                .secret(secret).build();

        String result = sendAndReceive(gson.toJson(encryptQuery));
        if (isNull(result) || resultIsError(result)) {
            return null;
        } else {
            return gson.fromJson(result, Data.class);
        }
    }

    public Data decrypt(String data, String secret) {
        DecryptQuery decryptQuery = DecryptQuery.builder()
                .encrypted_data(data)
                .secret(secret).build();

        String result = sendAndReceive(gson.toJson(decryptQuery));
        if (isNull(result) || resultIsError(result)) {
            return null;
        } else {
            return gson.fromJson(result, Data.class);
        }
    }

    public BlockHeader getBlockHeader(BlockIdExt fullblock) {
        BlockHeaderQuery blockHeaderQuery = BlockHeaderQuery.builder()
                .id(fullblock).build();

        String result = sendAndReceive(gson.toJson(blockHeaderQuery));
        if (isNull(result) || resultIsError(result)) {
            return null;
        } else {
            return gson.fromJson(result, BlockHeader.class);
        }
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

        String result = sendAndReceive(gson.toJson(query));
        if (isNull(result) || resultIsError(result)) {
            return null;
        } else {
            return gson.fromJson(result, DnsResolved.class);
        }
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

        String result = sendAndReceive(gson.toJson(getRawTransactionsQuery));
        if (isNull(result) || resultIsError(result)) {
            return null;
        } else {
            return gson.fromJson(result, RawTransactions.class);
        }
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

        String result = sendAndReceive(gson.toJson(getRawTransactionsQuery));
        if (isNull(result) || resultIsError(result)) {
            return null;
        }
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

        String result = sendAndReceive(gson.toJson(getBlockTransactionsQuery));
        if (isNull(result) || resultIsError(result)) {
            return null;
        } else {
            return gson.fromJson(result, BlockTransactions.class);
        }
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

        String result = sendAndReceive(gson.toJson(getAccountStateQuery));
        if (isNull(result) || resultIsError(result)) {
            return null;
        } else {
            return gson.fromJson(result, RawAccountState.class);
        }
    }

    public FullAccountState getAccountState(AccountAddressOnly address) {
        GetAccountStateQueryOnly getAccountStateQuery = GetAccountStateQueryOnly.builder().account_address(address).build();

        String result = sendAndReceive(gson.toJson(getAccountStateQuery));
        if (isNull(result) || resultIsError(result)) {
            return null;
        } else {
            return gson.fromJson(result, FullAccountState.class);
        }
    }

    public FullAccountState getAccountState(Address address) {
        AccountAddressOnly accountAddressOnly = AccountAddressOnly.builder()
                .account_address(address.toString(false))
                .build();
        GetAccountStateQueryOnly getAccountStateQuery = GetAccountStateQueryOnly.builder().account_address(accountAddressOnly).build();

        String result = sendAndReceive(gson.toJson(getAccountStateQuery));
        if (isNull(result) || resultIsError(result)) {
            return null;
        } else {
            return gson.fromJson(result, FullAccountState.class);
        }
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

        String result = sendAndReceive(gson.toJson(loadContractQuery));
        if (isNull(result) || resultIsError(result)) {
            return 0;
        } else {
            return gson.fromJson(result, LoadContract.class).getId();
        }
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

        String result = sendAndReceive(gson.toJson(runMethodQuery));
        if (isNull(result) || resultIsError(result)) {
            return null;
        }

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

        String result = sendAndReceive(gson.toJson(sendMessageQuery));
        if (isNull(result) || resultIsError(result)) {
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

        String result = sendAndReceive(gson.toJson(estimateFeesQuery));
        if (isNull(result) || resultIsError(result)) {
            return null;
        } else {
            return gson.fromJson(result, QueryFees.class);
        }
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

        String result = sendAndReceive(gson.toJson(createQuery));
        if (isNull(result) || resultIsError(result)) {
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
        SendQuery sendQuery = SendQuery.builder()
                .id(queryInfo.getId())
                .build();

        String result = sendAndReceive(gson.toJson(sendQuery));
        if (isNull(result) || resultIsError(result)) {
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

        String result = sendAndReceive(gson.toJson(createAndSendRawMessageQuery));
        if (isNull(result) || resultIsError(result)) {
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

    public void setStopping() {
      isStopping = true;
    }
}
