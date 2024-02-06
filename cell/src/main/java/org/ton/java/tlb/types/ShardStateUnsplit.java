package org.ton.java.tlb.types;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.CellSlice;

import java.math.BigInteger;

@Builder
@Getter
@Setter
@ToString
/**
 * shard_state#9023afe2
 *   global_id:int32
 *   shard_id:ShardIdent
 *   seq_no:uint32
 *   vert_seq_no:#
 *   gen_utime:uint32
 *   gen_lt:uint64
 *   min_ref_mc_seqno:uint32
 *   out_msg_queue_info:^OutMsgQueueInfo
 *   before_split:(## 1)
 *   accounts:^ShardAccounts
 *   ^[ overload_history:uint64
 *     underload_history:uint64
 *     total_balance:CurrencyCollection
 *     total_validator_fees:CurrencyCollection
 *     libraries:(HashmapE 256 LibDescr)
 *     master_ref:(Maybe BlkMasterInfo) ]
 *   custom:(Maybe ^McStateExtra)
 *   = ShardStateUnsplit;
 */
public class ShardStateUnsplit {
    long magic;
    int globalId;
    ShardIdent shardIdent;
    long seqno;
    long vertSeqno;
    long genUTime;
    BigInteger genLt;
    long minRefMCSeqno;
    Cell outMsgQueueInfo;
    //    OutMsgQueueInfo outMsgQueueInfo;
    boolean beforeSplit;
    //    ShardAccounts shardAccounts;
    Cell shardAccounts;
    ShardStateInfo shardStateInfo;
    McStateExtra custom;

    private String getMagic() {
        return Long.toHexString(magic);
    }

    public Cell toCell() {
        return CellBuilder.beginCell()
                .storeUint(0x9023afe2, 32)
                .storeUint(globalId, 32)
                .storeCell(shardIdent.toCell())
                .storeUint(seqno, 32)
                .storeUint(vertSeqno, 32)
                .storeUint(genUTime, 32)
                .storeUint(genLt, 64)
                .storeUint(minRefMCSeqno, 32)
                .storeRef(outMsgQueueInfo) // todo
                .storeBit(beforeSplit)
                .storeRef(shardAccounts) // todo
                .storeRef(shardStateInfo.toCell())
                .storeRefMaybe(custom.toCell())
                .endCell();
    }

    public static ShardStateUnsplit deserialize(CellSlice cs) {
        ShardStateUnsplit shardStateUnsplit = ShardStateUnsplit.builder()
                .magic(cs.loadUint(32).longValue())
                .globalId(cs.loadInt(32).intValue())
                .shardIdent(ShardIdent.deserialize(cs))
                .seqno(cs.loadUint(32).longValue())
                .vertSeqno(cs.loadUint(32).longValue())
                .genUTime(cs.loadUint(32).longValue())
                .genLt(cs.loadUint(64))
                .minRefMCSeqno(cs.loadUint(32).longValue())
//                .outMsgQueueInfo(OutMsgQueueInfo.deserialize(CellSlice.beginParse(cs.loadRef())))
                .outMsgQueueInfo(cs.loadRef())
                .build();
        shardStateUnsplit.setBeforeSplit(cs.loadBit());
//        shardStateUnsplit.setShardAccounts(ShardAccounts.deserialize(CellSlice.beginParse(cs.loadRef())));
        shardStateUnsplit.setShardAccounts(cs.loadRef());


        shardStateUnsplit.setShardStateInfo(ShardStateInfo.deserialize(CellSlice.beginParse(cs.loadRef())));
        
        shardStateUnsplit.setCustom(cs.loadBit() ? McStateExtra.deserialize(CellSlice.beginParse(cs.loadRef())) : null);
        return shardStateUnsplit;
    }
}
