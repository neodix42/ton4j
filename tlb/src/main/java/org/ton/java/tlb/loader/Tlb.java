package org.ton.java.tlb.loader;

import org.ton.java.cell.CellSlice;
import org.ton.java.tlb.types.*;

public class Tlb {

    public static Object load(Class c, CellSlice cs) {

        switch (c.getSimpleName()) {
            case "BlockIdExt":
                return BlockIdExt.builder().build();
            case "StateUpdate":
                return StateUpdate.builder().build();
            case "ConfigParams":
                return ConfigParams.builder().build();
            case "ShardStateUnsplit":
                cs.skipBits(32); // magic todo review
                return ShardStateUnsplit.builder()
                        .magic(0x9023afe2)
                        .globalId(cs.loadUint(32).longValue())
                        .shardIdent((ShardIdent) Tlb.load(ShardIdent.class, cs))
                        .seqno(cs.loadUint(32).longValue())
                        .vertSeqno(cs.loadUint(32).longValue())
                        .genUTime(cs.loadUint(32).longValue())
                        .genLT(cs.loadUint(64))
                        .minRefMCSeqno(cs.loadUint(32).longValue())
                        .outMsgQueueInfo(cs.loadRef())
                        //.accounts(cs.loadDict()) // todo
                        .beforeSplit(cs.loadBit())
                        .stats(cs.loadRef())
                        .mc((McStateExtra) Tlb.load(McStateExtra.class, cs))
                        .build();
            case "ShardIdent":
                cs.skipBits(2); //magic
                return ShardIdent.builder()
                        .magic(0)
                        .prefixBits(cs.loadUint(6).byteValueExact()) // todo review
                        .workchain(cs.loadUint(64).intValue())
                        .shardPrefix(cs.loadUint(64))
                        .build();
            case "ShardDesc":
                return ShardDesc.builder().build();
            case "McStateExtra":
                cs.skipBits(8); // magic
                return McStateExtra.builder()
                        .magic(0xcc26)
//                        .shardHashes(cs.loadDict())
                        .configParams((ConfigParams) Tlb.load(ConfigParams.class, cs))
                        .info(cs.loadRef())
                        .globalBalance((CurrencyCollection) Tlb.load(CurrencyCollection.class, cs))
                        .build();
            case "McBlockExtra":
                cs.skipBits(16); // magic todo review
                return McBlockExtra.builder()
                        .magic(0xcca5)
                        .keyBlock(cs.loadBit())
//                        .shardHashes()
//                        .shardFees()
                        .build();
            case "GlobalVersion":
                cs.skipBits(1); //magic
                return GlobalVersion.builder()
                        .magic(0xc4)
                        .version(cs.loadUint(32).longValue())
                        .capabilities(cs.loadUint(64))
                        .build();
            case "ExtBlkRef":
                return ExtBlkRef.builder()
                        .endLt(cs.loadUint(64))
                        .seqno(cs.loadUint(32).intValue())
                        .rootHash(cs.loadBytes(32))
                        .fileHash(cs.loadBytes(32))
                        .build();
            case "BlockInfoPart":
                cs.skipBits(32); //magic
                return BlockInfoPart.builder()
                        .magic(0x9bc7a987)
                        .version(cs.loadUint(32).longValue())
                        .notMaster(cs.loadBit())
                        .afterMerge(cs.loadBit())
                        .beforeSplit(cs.loadBit())
                        .afterSplit(cs.loadBit())
                        .wantSplit(cs.loadBit())
                        .wantMerge(cs.loadBit())
                        .keyBlock(cs.loadBit())
                        .vertSeqnoIncr(cs.loadBit())
                        .flags(cs.loadUint(8).intValue())
                        .seqno(cs.loadUint(32).longValue())
                        .vertSeqno(cs.loadUint(32).longValue())
                        .shard((ShardIdent) Tlb.load(ShardIdent.class, cs)) // todo review
                        .genuTime(cs.loadUint(32).longValue())
                        .startLt(cs.loadUint(64))
                        .endLt(cs.loadUint(64))
                        .genValidatorListHashShort(cs.loadUint(32).longValue())
                        .genCatchainSeqno(cs.loadUint(32).longValue())
                        .minRefMcSeqno(cs.loadUint(32).longValue())
                        .prevKeyBlockSeqno(cs.loadUint(32).longValue())
                        .build();
            case "BlockHandle":
                return BlockHandle.builder()
                        .offset(cs.loadUint(64))
                        .size(cs.loadUint(64))
                        .build();
            case "BlockExtra":
                cs.skipBits(32); //magic
                return BlockExtra.builder()
                        .magic(0x4a33f6fd)
                        .inMsgDesc(cs.loadRef())
                        .outMsgDesc(cs.loadRef())
                        .shardAccountBlocks(cs.loadRef())
                        .randSeed(cs.loadBytes(256))
                        .createdBy(cs.loadBytes(256))
                        .custom((McBlockExtra) Tlb.load(McBlockExtra.class, cs))
                        .build();
            case "Block":
                cs.skipBits(32); //magic
                return Block.builder()
                        .magic(0x11ef55aa)
                        .globalId(cs.loadInt(32).intValue())
                        .blockInfo((BlockHeader) Tlb.load(BlockHeader.class, CellSlice.beginParse(cs.loadRef())))
                        .valueFlow(cs.loadRef())
                        .stateUpdate((StateUpdate) Tlb.load(StateUpdate.class, CellSlice.beginParse(cs.loadRef())))
                        .extra((McBlockExtra) Tlb.load(McBlockExtra.class, CellSlice.beginParse(cs.loadRef())))
                        .build();
        }

        throw new Error("Unknown TLB type");
    }
}
