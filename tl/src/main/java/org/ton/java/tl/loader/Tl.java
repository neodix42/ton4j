package org.ton.java.tl.loader;

import org.ton.java.cell.Cell;
import org.ton.java.cell.CellSlice;
import org.ton.java.tl.types.BlockIdExt;
import org.ton.java.tl.types.DbBlockInfo;
import org.ton.java.tl.types.Text;
import org.ton.java.utils.Utils;

import java.math.BigInteger;

import static java.util.Objects.isNull;

/**
 * In digits in TL typed data placed in little endian format.
 */
public class Tl {

    public static final long DB_BLOCK_INFO_MAGIC = 0x4ac6e727;

    public static Object load(Class clazz, Cell c) {
        if (isNull(c)) {
            return null;
        } else {
            return load(clazz, CellSlice.beginParse(c));
        }
    }

    public static Object load(Class c, CellSlice cs) {

        switch (c.getSimpleName()) {
            case "DbBlockInfo":
                int magic = Integer.reverseBytes(cs.loadUint(32).intValue());
                assert (magic == DB_BLOCK_INFO_MAGIC) : "DbBlockInfo: magic not equal to 0x4ac6e727, found " + Long.toHexString(magic);
                DbBlockInfo dbBlockInfo = DbBlockInfo.builder()
                        .magic(DB_BLOCK_INFO_MAGIC)
                        .id((BlockIdExt) Tl.load(BlockIdExt.class, cs))
                        .build();
                int f = Integer.reverseBytes(cs.loadUint(32).intValue());
//                System.out.println("f " + f);

//                BigInteger flags = cs.loadUint(32);
                BigInteger flags = BigInteger.valueOf(f);

                // todo improve little endian reading
                dbBlockInfo.setFlags(flags);
                dbBlockInfo.setPrevLeft(flags.testBit(1) ? (BlockIdExt) Tl.load(BlockIdExt.class, cs) : null);
                dbBlockInfo.setPrevRight(flags.testBit(2) ? (BlockIdExt) Tl.load(BlockIdExt.class, cs) : null);
                dbBlockInfo.setNextLeft(flags.testBit(3) ? (BlockIdExt) Tl.load(BlockIdExt.class, cs) : null);
                dbBlockInfo.setNextRight(flags.testBit(4) ? (BlockIdExt) Tl.load(BlockIdExt.class, cs) : null);
                dbBlockInfo.setLt(flags.testBit(13) ? BigInteger.valueOf(Long.reverseBytes(cs.loadUint(64).longValue())) : null);
                dbBlockInfo.setTs(flags.testBit(14) ? BigInteger.valueOf(Integer.reverseBytes(cs.loadUint(32).intValue())) : null);
                dbBlockInfo.setState(flags.testBit(17) ? Utils.reverseIntArray(cs.loadBytes(256)) : null);
                dbBlockInfo.setMasterChainRefSeqNo(flags.testBit(23) ? BigInteger.valueOf(Integer.reverseBytes(cs.loadUint(32).intValue())) : null);
                return dbBlockInfo;
            case "BlockIdExt": {
                try {
                    BlockIdExt blockIdExt = BlockIdExt.builder()
                            .workchain(Utils.intsToInt(Utils.reverseIntArray(cs.loadBytes(32))))
                            .shard(Long.reverseBytes(cs.loadUint(64).longValue()))
                            .seqno(Utils.intsToInt(Utils.reverseIntArray(cs.loadBytes(32))))
                            .rootHash(Utils.reverseIntArray(cs.loadBytes(256)))
                            .fileHash(Utils.reverseIntArray(cs.loadBytes(256)))
                            .build();
                    return blockIdExt;

                } catch (Throwable t) {
                    return null;
                }
            }
            case "Text":
                int chunksNum = cs.loadUint(8).intValue();
                int firstSize = 0;
                int lengthOfChunk = 0;
                StringBuilder result = new StringBuilder();
                for (int i = 0; i < chunksNum; i++) {
                    lengthOfChunk = cs.loadUint(8).intValue();
                    if (i == 0) {
                        firstSize = lengthOfChunk;
                    }
                    int[] dataOfChunk = cs.loadBytes(lengthOfChunk * 8);
                    result.append(new String(Utils.unsignedBytesToSigned(dataOfChunk)));

                    if (i < chunksNum - 1) {
                        cs = CellSlice.beginParse(cs.loadRef());
                    }
                }
                return Text.builder()
                        .maxFirstChunkSize(firstSize)
                        .value(result.toString())
                        .build();
        }

        throw new Error("Unknown TLB type: " + c.getSimpleName());
    }
}
