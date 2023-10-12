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

    public static Object load(Class clazz, Cell c) {
        if (isNull(c)) {
            return null;
        } else {
            return load(clazz, CellSlice.beginParse(c), false);
        }
    }

    public static Object load(Class clazz, Cell c, boolean skipMagic) {
        if (isNull(c)) {
            return null;
        } else {
            return load(clazz, CellSlice.beginParse(c), skipMagic);
        }
    }

    public static Object load(Class c, CellSlice cs) {
        return load(c, cs, false);
    }

    public static Object load(Class c, CellSlice cs, boolean skipMagic) {

        switch (c.getSimpleName()) {
            case "DbBlockInfo":
                if (!skipMagic) {
                    int magic = Integer.reverseBytes(cs.loadUint(32).intValue());
                    assert (magic == 0x4ac6e727) : "DbBlockInfo: magic not equal to 0x4ac6e727, found " + Long.toHexString(magic);
                }
                DbBlockInfo dbBlockInfo = DbBlockInfo.builder()
                        .magic(0x4ac6e727)
                        .id((BlockIdExt) Tl.load(BlockIdExt.class, cs, skipMagic))
                        .build();
                BigInteger flags = cs.loadUint(32);

                System.out.println("flags " + flags.toString(2));
                dbBlockInfo.setFlags(flags);
                dbBlockInfo.setPrevLeft(flags.testBit(32 - 1) ? (BlockIdExt) Tl.load(BlockIdExt.class, cs) : null);
                dbBlockInfo.setPrevRight(flags.testBit(32 - 2) ? (BlockIdExt) Tl.load(BlockIdExt.class, cs) : null);
                dbBlockInfo.setNextLeft(flags.testBit(32 - 3) ? (BlockIdExt) Tl.load(BlockIdExt.class, cs) : null);
                dbBlockInfo.setNextRight(flags.testBit(32 - 4) ? (BlockIdExt) Tl.load(BlockIdExt.class, cs) : null);
                dbBlockInfo.setLt(flags.testBit(32 - 13) ? cs.loadUint(64) : null);
                dbBlockInfo.setTs(flags.testBit(32 - 14) ? cs.loadUint(32) : null);
                dbBlockInfo.setState(flags.testBit(32 - 17) ? cs.loadUint(256) : null);
                dbBlockInfo.setMasterChainRefSeqNo(flags.testBit(32 - 23) ? cs.loadUint(32) : null);
                return dbBlockInfo;
            case "BlockIdExt":
                return BlockIdExt.builder()
                        .workchain(Utils.intsToInt(Utils.reverseIntArray(cs.loadBytes(32))))
                        .shard(cs.loadUint(64))
                        .seqno(Utils.intsToInt(Utils.reverseIntArray(cs.loadBytes(32))))
                        .root_hash(Utils.reverseIntArray(cs.loadBytes(256)))
                        .file_hash(Utils.reverseIntArray(cs.loadBytes(256)))
                        .build();
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
