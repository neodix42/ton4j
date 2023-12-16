package org.ton.java.tonlib.client;

import com.jsoniter.output.JsonStream;
import lombok.SneakyThrows;
import org.ton.java.address.Address;
import org.ton.java.tonlib.queries.*;
import org.ton.java.tonlib.types.*;
import org.ton.java.utils.Utils;

import java.math.BigInteger;
import java.util.*;

public class TonClient {

    private final TonIO tonIO;
    private final String DEFAULT_DNS_RESOLVER_ADDRESS = "-1:E56754F83426F69B09267BD876AC97C44821345B7E266BD956A7BFBFB98DF35C";

    public TonClient(TonIO tonIO) {
        this.tonIO = tonIO;
    }

    @SneakyThrows
    private Object readResult(UUID uuid) {
        long startTs = System.currentTimeMillis();
        Object result = null;
        while (System.currentTimeMillis() - startTs < 5_000) {
            result = tonIO.getOut().remove(uuid);
            if (result != null) return result;
            synchronized (tonIO.getOut()) {
                tonIO.getOut().wait(5);
            }
        }
        return result;
    }

    public BlockIdExt lookupBlock(long seqno, long workchain, long shard, long lt, long utime) {
        int mode = 0;
        if (seqno != 0) mode += 1;
        if (lt != 0) mode += 2;
        if (utime != 0) mode += 4;
        LookupBlockQuery query = LookupBlockQuery.builder()
                .mode(mode).id(BlockId.builder().seqno(seqno).workchain(workchain)
                        .shard(BigInteger.valueOf(shard)).build()).lt(lt).utime(utime).build();
        query.setType(query.getTypeObjectName());
        tonIO.submitRequest(JsonStream.serialize(query));
        return (BlockIdExt) readResult(query.getTag());
    }

    public MasterChainInfo getLast() {
        GetLastQuery query = GetLastQuery.builder().build();
        query.setType(query.getTypeObjectName());
        tonIO.submitRequest(JsonStream.serialize(query));
        return (MasterChainInfo) readResult(query.getTag());
    }

    public Shards getShards(BlockIdExt id) {
        GetShardsQuery query = GetShardsQuery.builder()
                .id(id)
                .build();
        query.setType(query.getTypeObjectName());
        tonIO.submitRequest(JsonStream.serialize(query));
        return (Shards) readResult(query.getTag());

    }

    public Shards getShards(long seqno, long lt, long unixtime) {
        if ((seqno <= 0) && (lt <= 0) && (unixtime <= 0)) {
            throw new Error("Seqno, LT or unixtime should be defined");
        }
        long wc = -1;
        long shard = -9223372036854775808L;
        BlockIdExt query = lookupBlock(seqno, wc, shard, lt, unixtime);
        GetShardsQuery getShardsQuery = GetShardsQuery.builder()
                .id(query)
                .build();
        query.setType(query.getTypeObjectName());
        tonIO.submitRequest(JsonStream.serialize(getShardsQuery));
        return (Shards) readResult(getShardsQuery.getTag());
    }

    public Key createNewKey() {
        NewKeyQuery query = NewKeyQuery.builder().build();
        query.setType(query.getTypeObjectName());
        tonIO.submitRequest(JsonStream.serialize(query));
        return (Key) readResult(query.getTag());
    }

    public Data encrypt(String data, String secret) {
        EncryptQuery query = EncryptQuery.builder()
                .decrypted_data(data)
                .secret(secret).build();
        query.setType(query.getTypeObjectName());
        tonIO.submitRequest(JsonStream.serialize(query));
        return (Data) readResult(query.getTag());
    }

    public Data decrypt(String data, String secret) {
        DecryptQuery query = DecryptQuery.builder()
                .encrypted_data(data)
                .secret(secret).build();
        query.setType(query.getTypeObjectName());
        tonIO.submitRequest(JsonStream.serialize(query));
        return (Data) readResult(query.getTag());
    }

    public BlockHeader getBlockHeader(BlockIdExt fullblock) {
        BlockHeaderQuery query = BlockHeaderQuery.builder()
                .id(fullblock).build();
        query.setType(query.getTypeObjectName());
        tonIO.submitRequest(JsonStream.serialize(query));
        return (BlockHeader) readResult(query.getTag());
    }

    public DnsResolved dnsResolve(String name, AccountAddressOnly addr) {
        if (addr == null) {
            addr = AccountAddressOnly.builder().account_address(DEFAULT_DNS_RESOLVER_ADDRESS)
                    .build();
        }
        byte[] category = new byte[32];
        Arrays.fill(category, (byte) 0);
        DnsResolveQuery query = DnsResolveQuery.builder()
                .account_address(addr)
                .name(name)
                .category(Utils.bytesToBase64(category))
                .ttl(1)
                .build();
        query.setType(query.getTypeObjectName());
        tonIO.submitRequest(JsonStream.serialize(query));
        return (DnsResolved) readResult(query.getTag());
    }

    public RawTransactions getRawTransactions(String address, BigInteger fromTxLt, String fromTxHash) {
        if ((fromTxLt == null) || (fromTxHash == null)) {
            FullAccountState fullAccountState = getAccountState(AccountAddressOnly.builder().account_address(address).build());
            fromTxLt = fullAccountState.getLast_transaction_id().getLt();
            fromTxHash = fullAccountState.getLast_transaction_id().getHash();
        }
        GetRawTransactionsQuery query = GetRawTransactionsQuery.builder()
                .account_address(AccountAddressOnly.builder().account_address(address).build())
                .from_transaction_id(LastTransactionId.builder()
                        .lt(fromTxLt)
                        .hash(fromTxHash)
                        .build())
                .build();
        query.setType(query.getTypeObjectName());
        tonIO.submitRequest(JsonStream.serialize(query));
        return (RawTransactions) readResult(query.getTag());
    }

    public RawTransaction getRawTransaction(byte workchain, ShortTxId tx) {
        String addressHex = Utils.base64ToHexString(tx.getAccount());
        String address = Address.of(workchain + ":" + addressHex).toString(false);
        GetRawTransactionsV2Query query = GetRawTransactionsV2Query.builder()
                .account_address(AccountAddressOnly.builder().account_address(address).build())
                .from_transaction_id(LastTransactionId.builder()
                        .lt(tx.getLt())
                        .hash(tx.getHash())
                        .build())
                .count(1)
                .try_decode_message(false)
                .build();
        query.setType(query.getTypeObjectName());
        tonIO.submitRequest(JsonStream.serialize(query));
        return ((RawTransactions) readResult(query.getTag())).getTransactions().stream().findFirst().orElse(null);
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
                .lt(BigInteger.valueOf(afterLt))
                .build();
        return getBlockTransactions(fullblock, count, afterTx);
    }

    public BlockTransactions getBlockTransactions(BlockIdExt fullblock, long count) {
        return getBlockTransactions(fullblock, count, null);
    }

    public BlockTransactions getBlockTransactions(BlockIdExt fullblock, long count, AccountTransactionId afterTx) {
        int mode = 7;
        if (afterTx != null) mode = 7 + 128;
        if (afterTx == null) afterTx = AccountTransactionId.builder()
                    .account("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=").lt(BigInteger.valueOf(0)).build();

        GetBlockTransactionsQuery query = GetBlockTransactionsQuery.builder()
                .id(fullblock)
                .mode(mode)
                .count(count)
                .after(afterTx)
                .build();

        query.setType(query.getTypeObjectName());
        tonIO.submitRequest(JsonStream.serialize(query));
        return (BlockTransactions) readResult(query.getTag());
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
            RawTransactions rawTransactions = getRawTransactions(address, tx.getLt(), tx.getHash());
            totalTxs.put(address + "|" + tx.getLt(), rawTransactions);
        }
        return totalTxs;
    }

    public RawAccountState getRawAccountState(AccountAddressOnly address) {
        GetRawAccountStateQueryOnly query = GetRawAccountStateQueryOnly.builder().account_address(address).build();
        query.setType(query.getTypeObjectName());
        tonIO.submitRequest(JsonStream.serialize(query));
        return (RawAccountState) readResult(query.getTag());
    }

    public FullAccountState getAccountState(AccountAddressOnly address) {
        GetAccountStateQueryOnly query = GetAccountStateQueryOnly.builder().account_address(address).build();
        query.setType(query.getTypeObjectName());
        tonIO.submitRequest(JsonStream.serialize(query));
        return (FullAccountState) readResult(query.getTag());
    }

    public FullAccountState getAccountState(Address address) {
        AccountAddressOnly accountAddressOnly = AccountAddressOnly.builder()
                .account_address(address.toString(false))
                .build();
        GetAccountStateQueryOnly query = GetAccountStateQueryOnly.builder().account_address(accountAddressOnly).build();
        query.setType(query.getTypeObjectName());
        tonIO.submitRequest(JsonStream.serialize(query));
        return (FullAccountState) readResult(query.getTag());
    }

    public long loadContract(AccountAddressOnly address) {
        LoadContractQuery query = LoadContractQuery.builder()
                .account_address(address).build();
        query.setType(query.getTypeObjectName());
        tonIO.submitRequest(JsonStream.serialize(query));
        return ((LoadContract) readResult(query.getTag())).getId();
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
        RunMethodStrQuery query = RunMethodStrQuery.builder()
                .id(contractId)
                .method(MethodString.builder().name(methodName).build())
                .stack(stack)
                .build();
        query.setType(query.getTypeObjectName());
        tonIO.submitRequest(JsonStream.serialize(query));
        RunResultGeneric<String> g = ((RunResultGeneric<String>) readResult(query.getTag()));
        return ParseRunResult.getTypedRunResult(g.getStack(), g.getExit_code(), g.getGas_used(), g.getTag().toString());
    }

    /**
     * Sends raw message, bag of cells encoded in base64
     *
     * @param serializedBoc - base64 encoded BoC
     * @return ExtMessageInfo In case of error might contain error code and message inside
     */
    public ExtMessageInfo sendRawMessage(String serializedBoc) {
        SendRawMessageQuery query = SendRawMessageQuery.builder()
                .body(serializedBoc).build();
        query.setType(query.getTypeObjectName());
        tonIO.submitRequest(JsonStream.serialize(query));
        return (ExtMessageInfo) readResult(query.getTag());
    }

    public QueryFees estimateFees(String destinationAddress, String body, String initCode, String initData, boolean ignoreChksig) {
        QueryInfo queryInfo = createQuery(destinationAddress, body, initCode, initData);
        EstimateFeesQuery query = EstimateFeesQuery.builder()
                .queryId(queryInfo.getId())
                .ignore_chksig(ignoreChksig).build();
        query.setType(query.getTypeObjectName());
        tonIO.submitRequest(JsonStream.serialize(query));
        return (QueryFees) readResult(query.getTag());
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
        CreateQuery query = CreateQuery.builder()
                .init_code(initCode)
                .init_data(initData)
                .body(body)
                .destination(Destination.builder().account_address(destinationAddress).build())
                .build();
        query.setType(query.getTypeObjectName());
        tonIO.submitRequest(JsonStream.serialize(query));
        return (QueryInfo) readResult(query.getTag());
    }

    /**
     * Sends/Uploads query to the destination address
     *
     * @param queryInfo - result of createQuery()
     * @return true if query was sent without errors
     */
    public boolean sendQuery(QueryInfo queryInfo) {
        SendQuery query = SendQuery.builder()
                .id(queryInfo.getId())
                .build();
        query.setType(query.getTypeObjectName());
        tonIO.submitRequest(JsonStream.serialize(query));
        return ((Ok) readResult(query.getTag())).getType().equals("ok");
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
        CreateAndSendRawMessageQuery query = CreateAndSendRawMessageQuery.builder()
                .destination(AccountAddressOnly.builder().account_address(destinationAddress).build())
                .initial_account_state(initialAccountState)
                .data(body)
                .build();
        query.setType(query.getTypeObjectName());
        tonIO.submitRequest(JsonStream.serialize(query));
        return ((Ok) readResult(query.getTag())).getType().equals("ok");
    }
}
