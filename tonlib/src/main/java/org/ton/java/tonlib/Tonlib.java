package org.ton.java.tonlib;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;
import com.google.gson.ToNumberPolicy;
import com.sun.jna.Native;
import lombok.Builder;
import lombok.extern.java.Log;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.ton.java.address.Address;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.tonlib.queries.*;
import org.ton.java.tonlib.types.*;
import org.ton.java.utils.Utils;

import java.io.InputStream;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.TimeUnit;

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
     * if not specified and globalConfigAsString is null then integrated global-config.json is used;
     * <p>
     * if not specified and globalConfigAsString is filled then globalConfigAsString is used;
     * <p>
     * If not specified and testnet=true then integrated testnet-global.config.json is used;
     */
    private String pathToGlobalConfig;

    /**
     * if not specified and pathToGlobalConfig is null then integrated global-config.json is used;
     * <p>
     * if not specified and pathToGlobalConfig is filled then pathToGlobalConfig is used;
     */
    private String globalConfigAsString;
    /**
     * Valid values are:<br>
     * 0 - FATAL<br>
     * 1 - ERROR<br>
     * 2 - WARNING<br>
     * 3 - INFO<br>
     * 4 - DEBUG<br>
     */
    private VerbosityLevel verbosityLevel;

    private Boolean ignoreCache;

    /**
     * Ignored if pathToGlobalConfig is not null.
     */
    private boolean testnet;

    private boolean keystoreInMemory;

    private String keystorePath;

    /**
     * Default value 5
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

    RunResultParser runResultParser;
    LibraryResultParser libraryResultParser;

    public static class TonlibBuilder {
    }

    public static TonlibBuilder builder() {
        return new CustomTonlibBuilder();
    }

    private static class CustomTonlibBuilder extends TonlibBuilder {
        @Override
        public Tonlib build() {

            try {
                String tonlibName = switch (Utils.getOS()) {
                    case LINUX -> "tonlibjson.so";
                    case LINUX_ARM -> "tonlibjson-arm.so";
                    case WINDOWS -> "tonlibjson.dll";
                    case WINDOWS_ARM -> "tonlibjson-arm.dll";
                    case MAC -> "tonlibjson.dylib";
                    case MAC_ARM64 -> "tonlibjson-arm.dylib";
                    case UNKNOWN -> throw new Error("Operating system is not supported!");
                };

                if (isNull(super.pathToTonlibSharedLib)) {
                    super.pathToTonlibSharedLib = tonlibName;
                }

                if (isNull(super.verbosityLevel)) {
                    super.verbosityLevel = VerbosityLevel.FATAL;
                }

                if (isNull(super.keystorePath)) {
                    super.keystorePath = ".";
                }

                super.keystorePath = super.keystorePath.replace("\\", "/");

                if (super.receiveRetryTimes == 0) {
                    super.receiveRetryTimes = 5;
                }

                if (super.receiveTimeout == 0) {
                    super.receiveTimeout = 10.0;
                }

                if (isNull(super.ignoreCache)) {
                    super.ignoreCache = true;
                }

                super.synced = false;
                super.runResultParser = new RunResultParser();
                super.libraryResultParser = new LibraryResultParser();

                String configData;
                if (isNull(super.pathToGlobalConfig)) {

                    if (isNull(super.globalConfigAsString)) {
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
                    } else {
                        configData = super.globalConfigAsString;
                    }
                } else {
                    if (Files.exists(Paths.get(super.pathToGlobalConfig))) {
                        configData = new String(Files.readAllBytes(Paths.get(super.pathToGlobalConfig)));
                    } else {
                        throw new RuntimeException("Global config is not found in path: " + super.pathToGlobalConfig);
                    }
                }

                super.tonlibJson = Native.load(super.pathToTonlibSharedLib, TonlibJsonI.class);
                super.tonlib = super.tonlibJson.tonlib_client_json_create();

                System.out.printf("""
                                Java Tonlib configuration:
                                Location: %s
                                Verbosity level: %s (%s)
                                Keystore in memory: %s
                                Keystore path: %s
                                Path to global config: %s
                                Global config as string: %s
                                Ignore cache: %s
                                Testnet: %s
                                Receive timeout: %s seconds
                                Receive retry times: %s%n""",
                        super.pathToTonlibSharedLib, super.verbosityLevel, super.verbosityLevel.ordinal(),
                        super.keystoreInMemory, super.keystorePath, super.pathToGlobalConfig,
                        isNull(super.globalConfigAsString) ? "" : super.globalConfigAsString.substring(0, 33), super.ignoreCache,
                        super.testnet, super.receiveTimeout, super.receiveRetryTimes);

                VerbosityLevelQuery verbosityLevelQuery = VerbosityLevelQuery.builder().new_verbosity_level(super.verbosityLevel.ordinal()).build();

                super.tonlibJson.tonlib_client_json_send(super.tonlib, gson.toJson(verbosityLevelQuery));

                String result = super.tonlibJson.tonlib_client_json_receive(super.tonlib, super.receiveTimeout);
                System.out.println("set verbosityLevel result: " + result);

                String initTemplate = Utils.streamToString(Tonlib.class.getClassLoader().getResourceAsStream("init.json"));

                String dataQuery = JsonParser.parseString(configData).getAsJsonObject().toString();

                String q = initTemplate.replace("CFG_PLACEHOLDER", dataQuery);
                q = q.replace("IGNORE_CACHE", super.ignoreCache.toString());
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

    private String receive() {
        String result = null;
        int retry = 0;
        while (isNull(result)) {
            if (++retry > receiveRetryTimes) {
                throw new RuntimeException("Error in tonlib.receive(), " + receiveRetryTimes + " times was not able retrieve result from Tonlib shared library.");
            }
            result = tonlibJson.tonlib_client_json_receive(tonlib, receiveTimeout);
        }
        return result;
    }

    private String syncAndRead(String query) {
        String response = null;
        try {
            tonlibJson.tonlib_client_json_send(tonlib, query);
            TimeUnit.MILLISECONDS.sleep(200);
            response = receive();
            int retry = 0;
            outterloop:
            do {
                do {
                    if (nonNull(response) && !response.contains("syncStateInProgress") && !response.contains("\"@type\":\"ok\"")) {

                        if (++retry > receiveRetryTimes) {
//                            System.out.println("Last response: " + response);
                            break outterloop;
                        }

                        tonlibJson.tonlib_client_json_send(tonlib, query);
                    }

                    if (response.contains("\"@type\":\"ok\"")) {
                        String queryExtraId = StringUtils.substringBetween(query, "@extra\":\"", "\"}");
                        String responseExtraId = StringUtils.substringBetween(response, "@extra\":\"", "\"}");
                        if (queryExtraId.equals(responseExtraId)) {
                            break outterloop;
                        }
                    }

                    if (response.contains(" : duplicate message\"")) {
                        break outterloop;
                    }
                    TimeUnit.MILLISECONDS.sleep(200);
                    response = receive();

                    UpdateSyncState sync = gson.fromJson(response, UpdateSyncState.class);
                    if (nonNull(sync) && nonNull(sync.getSync_state()) && sync.getType().equals("updateSyncState") && !response.contains("syncStateDone")) {
                        double pct = 0.0;
                        if (sync.getSync_state().getTo_seqno() != 0) {
                            pct = (sync.getSync_state().getCurrent_seqno() * 100) / (double) sync.getSync_state().getTo_seqno();
                        }
                        System.out.println("Synchronized: " + String.format("%.2f%%", pct));
                    }
                    if (isNull(response)) {
                        throw new RuntimeException("Error in waitForSyncDone(), response is null.");
                    }

                } while (response.contains("error") || response.contains("syncStateInProgress"));

                if (response.contains("syncStateDone")) {
                    response = receive();
                }
                if (response.contains("error")) {
                    System.out.println(response);
                }
            } while (response.contains("error") || response.contains("syncStateInProgress"));

            return response;

        } catch (Exception e) {
            log.info(e.getMessage());
            return response;
        }
    }

    public BlockIdExt lookupBlock(long seqno, long workchain, long shard, long lt, long utime) {
        synchronized (gson) {
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

            String result = syncAndRead(gson.toJson(lookupBlockQuery));
            return gson.fromJson(result, BlockIdExt.class);
        }
    }

    public BlockIdExt lookupBlock(long seqno, long workchain, long shard, long lt) {
        return lookupBlock(seqno, workchain, shard, lt, 0);
    }

    public MasterChainInfo getLast() {
        synchronized (gson) {
            GetLastQuery getLastQuery = GetLastQuery.builder()
                    .build();

            String result = syncAndRead(gson.toJson(getLastQuery));
            return gson.fromJson(result, MasterChainInfo.class);
        }
    }

    public MasterChainInfo getMasterChainInfo() {
        return getLast();
    }

    public Shards getShards(BlockIdExt id) {
        synchronized (gson) {
            GetShardsQuery getShardsQuery = GetShardsQuery.builder()
                    .id(id)
                    .build();

            String result = syncAndRead(gson.toJson(getShardsQuery));
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

        synchronized (gson) {
            GetShardsQuery getShardsQuery = GetShardsQuery.builder()
                    .id(fullblock)
                    .build();

            String result = syncAndRead(gson.toJson(getShardsQuery));
            return gson.fromJson(result, Shards.class);
        }
    }

    public Key createNewKey() {
        synchronized (gson) {
            NewKeyQuery newKeyQuery = NewKeyQuery.builder().build();

            String result = syncAndRead(gson.toJson(newKeyQuery));
            return gson.fromJson(result, Key.class);
        }
    }

    public Data encrypt(String data, String secret) {
        synchronized (gson) {
            EncryptQuery encryptQuery = EncryptQuery.builder()
                    .decrypted_data(data)
                    .secret(secret)
                    .build();

            String result = syncAndRead(gson.toJson(encryptQuery));
            return gson.fromJson(result, Data.class);
        }
    }

    public Data decrypt(String data, String secret) {
        synchronized (gson) {
            DecryptQuery decryptQuery = DecryptQuery.builder()
                    .encrypted_data(data)
                    .secret(secret)
                    .build();

            String result = syncAndRead(gson.toJson(decryptQuery));
            return gson.fromJson(result, Data.class);
        }
    }

    public BlockHeader getBlockHeader(BlockIdExt fullblock) {
        synchronized (gson) {
            BlockHeaderQuery blockHeaderQuery = BlockHeaderQuery.builder()
                    .id(fullblock)
                    .build();

            String result = syncAndRead(gson.toJson(blockHeaderQuery));
            return gson.fromJson(result, BlockHeader.class);
        }
    }

    //@formatter:off


    /**
     * @param address    String
     * @param fromTxLt   BigInteger
     * @param fromTxHash String in base64 format
     * @return RawTransactions
     */
    public RawTransactions getRawTransactions(String address, BigInteger fromTxLt, String fromTxHash) {

        if (isNull(fromTxLt) || isNull(fromTxHash)) {
            RawAccountState fullAccountState = getRawAccountState(AccountAddressOnly.builder().account_address(address).build());
            fromTxLt = fullAccountState.getLast_transaction_id().getLt();
            fromTxHash = fullAccountState.getLast_transaction_id().getHash();
        }

        synchronized (gson) {
            GetRawTransactionsQuery getRawTransactionsQuery = GetRawTransactionsQuery.builder()
                    .account_address(AccountAddressOnly.builder().account_address(address).build())
                    .from_transaction_id(LastTransactionId.builder()
                            .lt(fromTxLt)
                            .hash(fromTxHash)
                            .build())
                    .build();

            String result = syncAndRead(gson.toJson(getRawTransactionsQuery));
            return gson.fromJson(result, RawTransactions.class);
        }
    }

    /**
     * Similar to getRawTransactions but limits the number of txs
     *
     * @param address    String
     * @param fromTxLt   BigInteger
     * @param fromTxHash String in base64 format
     * @param limit      int
     * @return RawTransactions
     */
    public RawTransactions getRawTransactions(String address, BigInteger fromTxLt, String fromTxHash, int limit) {

        if (isNull(fromTxLt) || isNull(fromTxHash)) {
            FullAccountState fullAccountState = getAccountState(AccountAddressOnly.builder().account_address(address).build());
            fromTxLt = fullAccountState.getLast_transaction_id().getLt();
            fromTxHash = fullAccountState.getLast_transaction_id().getHash();
        }
        synchronized (gson) {
            GetRawTransactionsQuery getRawTransactionsQuery = GetRawTransactionsQuery.builder()
                    .account_address(AccountAddressOnly.builder().account_address(address).build())
                    .from_transaction_id(LastTransactionId.builder()
                            .lt(fromTxLt)
                            .hash(fromTxHash)
                            .build())
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
    }

    /**
     * @param address      String
     * @param fromTxLt     BigInteger
     * @param fromTxHash   String in base64 format
     * @param historyLimit int
     * @return RawTransactions
     */
    public RawTransactions getAllRawTransactions(String address, BigInteger fromTxLt, String fromTxHash, int historyLimit) {

        RawTransactions rawTransactions = getRawTransactions(address, fromTxLt, fromTxHash);

        if (isNull(rawTransactions.getTransactions())) {
            throw new Error("lite-server cannot return any transactions");
        }
        List<RawTransaction> transactions = new ArrayList<>(rawTransactions.getTransactions());

        while (rawTransactions.getPrevious_transaction_id().getLt().compareTo(BigInteger.ZERO) != 0) {
            rawTransactions = getRawTransactions(address, rawTransactions.getPrevious_transaction_id().getLt(), rawTransactions.getPrevious_transaction_id().getHash());
            if (isNull(rawTransactions.getTransactions()) && !transactions.isEmpty()) {
                return RawTransactions.builder().transactions(transactions).build();
            }
            transactions.addAll(rawTransactions.getTransactions());
            if (transactions.size() > historyLimit) {
                return RawTransactions.builder().transactions(transactions.subList(0, historyLimit)).build();
            }
        }

        if (historyLimit > transactions.size()) {
            return RawTransactions.builder().transactions(transactions).build();
        } else {
            return RawTransactions.builder().transactions(transactions.subList(0, historyLimit)).build();
        }
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
        synchronized (gson) {
            int mode = 7;
            if (nonNull(afterTx)) {
                mode = 7 + 128;
            }

            if (isNull(afterTx)) {
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

            String result = syncAndRead(gson.toJson(getBlockTransactionsQuery));

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
        Map<String, RawTransactions> totalTxs = new HashMap<>();
        BlockTransactions blockTransactions = getBlockTransactions(fullblock, count, afterTx);
        for (ShortTxId tx : blockTransactions.getTransactions()) {
            String addressHex = Utils.base64ToHexString(tx.getAccount());
            String address = Address.of(fullblock.getWorkchain() + ":" + addressHex).toString(false);
            RawTransactions rawTransactions = getRawTransactions(address, BigInteger.valueOf(tx.getLt()), tx.getHash());
            totalTxs.put(address + "|" + tx.getLt(), rawTransactions);
        }
        return totalTxs;
    }

    /**
     * Returns RawAccountState that always contains code and data
     *
     * @param address AccountAddressOnly
     * @return RawAccountState
     */
    public RawAccountState getRawAccountState(AccountAddressOnly address) {
        synchronized (gson) {
            GetRawAccountStateQueryOnly getAccountStateQuery = GetRawAccountStateQueryOnly.builder().account_address(address).build();

            String result = syncAndRead(gson.toJson(getAccountStateQuery));
            return gson.fromJson(result, RawAccountState.class);
        }
    }

    /**
     * Returns status of an address, code and data
     *
     * @param address Address
     * @return account state RawAccountState
     */
    public RawAccountState getRawAccountState(Address address) {
        synchronized (gson) {
            AccountAddressOnly accountAddressOnly = AccountAddressOnly.builder()
                    .account_address(address.toString(false))
                    .build();

            GetRawAccountStateQueryOnly getAccountStateQuery = GetRawAccountStateQueryOnly.builder()
                    .account_address(accountAddressOnly)
                    .build();

            String result = syncAndRead(gson.toJson(getAccountStateQuery));
            return gson.fromJson(result, RawAccountState.class);
        }
    }

    /**
     * Returns status of an address
     *
     * @param address Address
     * @return String, uninitialized, frozen or active
     */
    public String getRawAccountStatus(Address address) {
        synchronized (gson) {
            AccountAddressOnly accountAddressOnly = AccountAddressOnly.builder()
                    .account_address(address.toString(false))
                    .build();

            GetRawAccountStateQueryOnly getAccountStateQuery = GetRawAccountStateQueryOnly.builder()
                    .account_address(accountAddressOnly)
                    .build();

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
    }

    /**
     * With comparison to getRawAccountState returns wallet_id and seqno, not necessarily returns code and data
     *
     * @param address AccountAddressOnly
     * @return FullAccountState
     */
    public FullAccountState getAccountState(AccountAddressOnly address) {
        synchronized (gson) {
            GetAccountStateQueryOnly getAccountStateQuery = GetAccountStateQueryOnly.builder()
                    .account_address(address)
                    .build();

            String result = syncAndRead(gson.toJson(getAccountStateQuery));
            return gson.fromJson(result, FullAccountState.class);
        }
    }

    /**
     * With comparison to getRawAccountState returns wallet_id and seqno, not necessarily returns code and data
     *
     * @param address Address
     * @return FullAccountState
     */
    public FullAccountState getAccountState(Address address) {
        synchronized (gson) {
            AccountAddressOnly accountAddressOnly = AccountAddressOnly.builder()
                    .account_address(address.toString(false))
                    .build();

            GetAccountStateQueryOnly getAccountStateQuery = GetAccountStateQueryOnly.builder().account_address(accountAddressOnly).build();

            String result = syncAndRead(gson.toJson(getAccountStateQuery));
            return gson.fromJson(result, FullAccountState.class);
        }
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


    public Cell getConfigAll(int mode) {
        synchronized (gson) {
            GetConfigAllQuery configParamQuery = GetConfigAllQuery.builder()
                    .mode(mode)
                    .build();

            String result = syncAndRead(gson.toJson(configParamQuery));
            ConfigInfo ci = gson.fromJson(result, ConfigInfo.class);
            return CellBuilder.beginCell().fromBoc(ci.getConfig().getBytes()).endCell();
        }
    }

    public Cell getConfigParam(BlockIdExt id, long param) {
        synchronized (gson) {
            GetConfigParamQuery configParamQuery = GetConfigParamQuery.builder()
                    .id(id)
                    .param(param)
                    .build();

            String result = syncAndRead(gson.toJson(configParamQuery));
            System.out.println(result);
            ConfigInfo ci = gson.fromJson(result, ConfigInfo.class);
            return CellBuilder.beginCell().fromBoc(ci.getConfig().getBytes()).endCell();
        }
    }


    public long loadContract(AccountAddressOnly address) {
        synchronized (gson) {
            LoadContractQuery loadContractQuery = LoadContractQuery.builder()
                    .account_address(address)
                    .build();

            String result = syncAndRead(gson.toJson(loadContractQuery));

            return gson.fromJson(result, LoadContract.class).getId();
        }
    }
    public long loadContract(AccountAddressOnly address, long seqno) {
        synchronized (gson) {
            BlockIdExt fullBlock;
            if (seqno != 0) {
            fullBlock = lookupBlock(seqno,-1,-9223372036854775808L,0 );
            }
            else {
                fullBlock = getMasterChainInfo().getLast();
            }

            LoadContractQuery loadContractQuery = LoadContractQuery.builder()
                    .account_address(address)
                    .build();

            WithBlockQuery withBlockQuery = WithBlockQuery.builder()
                           .id(fullBlock)
                    .function(loadContractQuery)
                    .build();

            String result = syncAndRead(gson.toJson(withBlockQuery));

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
        synchronized (gson) {
            long contractId = loadContract(AccountAddressOnly.builder().account_address(contractAddress.toString(false)).build());
            if (contractId == -1) {
                System.err.println("cannot load contract " + AccountAddressOnly.builder().account_address(contractAddress.toString(false)));
                return null;
            } else {
                return runMethod(contractId, methodName, stackData);
            }
        }
    }

    public RunResult runMethod(long contractId, String methodName, Deque<String> stackData) {
        synchronized (gson) {
            Deque<TvmStackEntry> stack = null;
            if (nonNull(stackData)) {
                stack = ParseRunResult.renderTvmStack(stackData);
            }

            RunMethodStrQuery runMethodQuery = RunMethodStrQuery.builder()
                    .id(contractId)
                    .method(MethodString.builder().name(methodName).build())
                    .stack(stack)
                    .build();

            String result = syncAndRead(gson.toJson(runMethodQuery));

            return runResultParser.parse(result);
        }
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
            throw new Error("can't get/parse result by executing seqno method, exit code " + result.getExit_code());
        }

        TvmStackEntryNumber seqno = (TvmStackEntryNumber) result.getStack().get(0);

        return seqno.getNumber().longValue();
    }

    /**
     * Generic method to call get_subwallet_id method of a contract. There is no check if get_subwallet_id method exists.
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
        synchronized (gson) {
            SendRawMessageQuery sendMessageQuery = SendRawMessageQuery.builder()
                    .body(serializedBoc)
                    .build();

            String result = syncAndRead(gson.toJson(sendMessageQuery));

            if ((isNull(result)) || (result.contains("@type") && result.contains("error"))) {
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
    }

    public QueryFees estimateFees(String destinationAddress, String body, String initCode, String initData, boolean ignoreChksig) {
        QueryInfo queryInfo = createQuery(destinationAddress, body, initCode, initData);

        synchronized (gson) {
            EstimateFeesQuery estimateFeesQuery = EstimateFeesQuery.builder()
                    .queryId(queryInfo.getId())
                    .ignore_chksig(ignoreChksig)
                    .build();

            String result = syncAndRead(gson.toJson(estimateFeesQuery));

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
        synchronized (gson) {
            CreateQuery createQuery = CreateQuery.builder()
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
    }

    /**
     * Sends/Uploads query to the destination address
     *
     * @param queryInfo - result of createQuery()
     * @return true if query was sent without errors
     */
    public boolean sendQuery(QueryInfo queryInfo) {
        synchronized (gson) {
            SendQuery createQuery = SendQuery.builder()
                    .id(queryInfo.getId())
                    .build();

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
        synchronized (gson) {
            CreateAndSendRawMessageQuery createAndSendRawMessageQuery = CreateAndSendRawMessageQuery.builder()
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
    }

    public RawTransaction tryLocateTxByIncomingMessage(Address source, Address destination, long creationLt) {
        Shards shards = getShards(0, creationLt, 0);
        for (BlockIdExt shardData : shards.getShards()) {
            for (int b = 0; b < 3; b++) {
                BlockIdExt block = lookupBlock(0, shardData.getWorkchain(), shardData.getShard(), creationLt + b * 1000000L);
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
                    RawTransactions transactions = getRawTransactions(
                            destination.toString(false),
                            BigInteger.valueOf(candidate.getRight()),
                            candidate.getLeft(),
                            Math.max(count, 10));

                    for (RawTransaction tx : transactions.getTransactions()) {
                        RawMessage in_msg = tx.getIn_msg();
                        String txSource = in_msg.getSource().getAccount_address();
                        if (StringUtils.isNoneEmpty(txSource) && (Address.of(txSource).toString(false).equals(source.toString(false)))) {
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

        String result = syncAndRead(gson.toJson(getRawTransactionsQuery));
        RawTransactions res = gson.fromJson(result, RawTransactions.class);
        List<RawTransaction> t = res.getTransactions ();
        if (t.size() >= 1) {
            return t.get(0);
        } else {
            return RawTransaction.builder().build();
        }
    }

    public RawTransaction tryLocateTxByOutcomingMessage(Address source, Address destination, long creationLt) {
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
                RawTransactions transactions = getRawTransactions(
                        source.toString(false),
                        BigInteger.valueOf(candidate.getRight()),
                        candidate.getLeft(),
                        Math.max(count, 10));

                for (RawTransaction tx : transactions.getTransactions()) {
                    for (RawMessage out_msg : tx.getOut_msgs()) {
                        String txDestination = out_msg.getDestination().getAccount_address();
                        if (StringUtils.isNoneEmpty(txDestination) && (Address.of(txDestination).toString(false).equals(destination.toString(false)))) {
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

        String result = syncAndRead(gson.toJson(query));
        return gson.fromJson(result, DnsResolved.class);
    }

    /**
     *
     * @param librariesHashes list of base64-encoded libraries hashes
     * @return RunResult
     */

    public SmcLibraryResult getLibraries(List<String> librariesHashes) {
        synchronized (gson) {
            GetLibrariesQuery getLibrariesQuery = GetLibrariesQuery.builder()
                    .library_list(librariesHashes)
                    .build();

            String result = syncAndRead(gson.toJson(getLibrariesQuery));
            return libraryResultParser.parse(result);
        }
    }
}
