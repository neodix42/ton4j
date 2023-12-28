package org.ton.java.tonlib.base;

import org.ton.java.tonlib.queries.*;
import org.ton.java.tonlib.types.*;

import java.util.HashMap;

public class TypeToClassMap {
    public static HashMap<String,Class<?>> classes = new HashMap<>(){{
        //Responses only.
        put("blocks.accountTransactionId", AccountTransactionId.class);
        put("blocks.header",BlockHeader.class);
        put("ton.blockId",BlockId.class);
        put("ton.blockIdExt", BlockIdExt.class);
        put("blocks.transactions", BlockTransactions.class);
        put("configInfo", ConfigInfo.class);
        put("data",Data.class);
        put("dns.entry", DnsEntry.class);
        put("dns.resolved",DnsResolved.class);
        put("raw.extMessageInfo", ExtMessageInfo.class);
        put("fees",Fees.class);
        put("key", Key.class);
        put("internal.transactionId",LastTransactionId.class);
        put("smc.info",LoadContract.class);
        put("blocks.masterchainInfo", MasterChainInfo.class);
        put("ok", Ok.class);
        put("query.fees", QueryFees.class);
        put("raw.fullAccountState",RawAccountState.class);
        put("raw.message",RawMessage.class);
        put("raw.transaction",RawTransaction.class);
        put("raw.transactions",RawTransactions.class);
        put("smc.runResult",RunResultGeneric.class);
        put("blocks.shards",Shards.class);
        put("syncStateInProgress",SyncStateInProgress.class);
        put("error",TonlibError.class);
        put("tvm.cell", TvmCell.class);
        put("tvm.list", TvmList.class);
        put("tvm.numberDecimal",TvmNumber.class);
        put("tvm.slice", TvmSlice.class);
        put("tvm.stackEntryCell",TvmStackEntryCell.class);
        put("tvm.stackEntryList",TvmStackEntryList.class);
        put("tvm.stackEntryNumber", TvmStackEntryNumber.class);
        put("tvm.stackEntrySlice", TvmStackEntrySlice.class);
        put("tvm.stackEntryTuple", TvmStackEntryTuple.class);
        put("tvm.tuple",TvmTuple.class);
        put("updateSyncState", UpdateSyncState.class);
    }};

}
