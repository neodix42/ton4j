package org.ton.ton4j.tl.types.db.block;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents information about a TON blockchain block.
 * Based on the TL schema definition for db.block.info.
 */
public class BlockInfo {
    private static final int MAGIC = 0x4ac6e727; // db.block.info#4ac6e727
    
    private int flags;
    private boolean afterMerge;
    private boolean afterSplit;
    private boolean beforeSplit;
    private boolean wantSplit;
    private boolean wantMerge;
    private boolean keyBlock;
    private boolean vertSeqnoIncr;
    private int version;
    private boolean notMaster;
    private int genUtime;
    private long startLt;
    private long endLt;
    private long genValidatorListHashShort;
    private long genCatchainSeqno;
    private long minRefMcSeqno;
    private long prevKeyBlockSeqno;
    private List<BlockIdExt> masterRefSeqno;
    
    /**
     * Deserializes a BlockInfo from a ByteBuffer.
     * 
     * @param buffer The ByteBuffer containing the serialized BlockInfo
     * @return The deserialized BlockInfo
     */
    public static BlockInfo deserialize(ByteBuffer buffer) {
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        
        BlockInfo blockInfo = new BlockInfo();
        
        // Skip magic (already verified)
        buffer.getInt();
        
        // Read flags
        blockInfo.flags = buffer.getInt();
        blockInfo.afterMerge = (blockInfo.flags & 1) != 0;
        blockInfo.afterSplit = (blockInfo.flags & 2) != 0;
        blockInfo.beforeSplit = (blockInfo.flags & 4) != 0;
        blockInfo.wantSplit = (blockInfo.flags & 8) != 0;
        blockInfo.wantMerge = (blockInfo.flags & 16) != 0;
        blockInfo.keyBlock = (blockInfo.flags & 32) != 0;
        blockInfo.vertSeqnoIncr = (blockInfo.flags & 64) != 0;
        
        // Read version
        blockInfo.version = buffer.getInt();
        blockInfo.notMaster = (blockInfo.version & 0x80000000) != 0;
        blockInfo.version = blockInfo.version & 0x7FFFFFFF;
        
        // Read timestamps and sequence numbers
        blockInfo.genUtime = buffer.getInt();
        blockInfo.startLt = buffer.getLong();
        blockInfo.endLt = buffer.getLong();
        blockInfo.genValidatorListHashShort = buffer.getLong();
        blockInfo.genCatchainSeqno = buffer.getLong();
        blockInfo.minRefMcSeqno = buffer.getLong();
        blockInfo.prevKeyBlockSeqno = buffer.getLong();
        
        // Read master reference sequence numbers
        int count = buffer.getInt();
        blockInfo.masterRefSeqno = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            BlockIdExt blockIdExt = BlockIdExt.deserialize(buffer);
            blockInfo.masterRefSeqno.add(blockIdExt);
        }
        
        return blockInfo;
    }
    
    /**
     * Serializes this BlockInfo to a ByteBuffer.
     * 
     * @return The serialized ByteBuffer
     */
    public ByteBuffer serialize() {
        // Calculate size
        int size = 4 + // magic
                  4 + // flags
                  4 + // version
                  4 + // genUtime
                  8 + // startLt
                  8 + // endLt
                  8 + // genValidatorListHashShort
                  8 + // genCatchainSeqno
                  8 + // minRefMcSeqno
                  8 + // prevKeyBlockSeqno
                  4; // masterRefSeqno count
        
        // Add size for each BlockIdExt
        for (BlockIdExt blockIdExt : masterRefSeqno) {
            size += blockIdExt.getSerializedSize();
        }
        
        ByteBuffer buffer = ByteBuffer.allocate(size);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        
        // Write magic
        buffer.putInt(MAGIC);
        
        // Write flags
        int flags = 0;
        if (afterMerge) flags |= 1;
        if (afterSplit) flags |= 2;
        if (beforeSplit) flags |= 4;
        if (wantSplit) flags |= 8;
        if (wantMerge) flags |= 16;
        if (keyBlock) flags |= 32;
        if (vertSeqnoIncr) flags |= 64;
        buffer.putInt(flags);
        
        // Write version
        int versionWithFlag = version;
        if (notMaster) versionWithFlag |= 0x80000000;
        buffer.putInt(versionWithFlag);
        
        // Write timestamps and sequence numbers
        buffer.putInt(genUtime);
        buffer.putLong(startLt);
        buffer.putLong(endLt);
        buffer.putLong(genValidatorListHashShort);
        buffer.putLong(genCatchainSeqno);
        buffer.putLong(minRefMcSeqno);
        buffer.putLong(prevKeyBlockSeqno);
        
        // Write master reference sequence numbers
        buffer.putInt(masterRefSeqno.size());
        for (BlockIdExt blockIdExt : masterRefSeqno) {
            blockIdExt.serialize(buffer);
        }
        
        buffer.flip();
        return buffer;
    }
    
    // Getters and setters
    
    public int getFlags() {
        return flags;
    }
    
    public void setFlags(int flags) {
        this.flags = flags;
        this.afterMerge = (flags & 1) != 0;
        this.afterSplit = (flags & 2) != 0;
        this.beforeSplit = (flags & 4) != 0;
        this.wantSplit = (flags & 8) != 0;
        this.wantMerge = (flags & 16) != 0;
        this.keyBlock = (flags & 32) != 0;
        this.vertSeqnoIncr = (flags & 64) != 0;
    }
    
    public boolean isAfterMerge() {
        return afterMerge;
    }
    
    public void setAfterMerge(boolean afterMerge) {
        this.afterMerge = afterMerge;
        updateFlags();
    }
    
    public boolean isAfterSplit() {
        return afterSplit;
    }
    
    public void setAfterSplit(boolean afterSplit) {
        this.afterSplit = afterSplit;
        updateFlags();
    }
    
    public boolean isBeforeSplit() {
        return beforeSplit;
    }
    
    public void setBeforeSplit(boolean beforeSplit) {
        this.beforeSplit = beforeSplit;
        updateFlags();
    }
    
    public boolean isWantSplit() {
        return wantSplit;
    }
    
    public void setWantSplit(boolean wantSplit) {
        this.wantSplit = wantSplit;
        updateFlags();
    }
    
    public boolean isWantMerge() {
        return wantMerge;
    }
    
    public void setWantMerge(boolean wantMerge) {
        this.wantMerge = wantMerge;
        updateFlags();
    }
    
    public boolean isKeyBlock() {
        return keyBlock;
    }
    
    public void setKeyBlock(boolean keyBlock) {
        this.keyBlock = keyBlock;
        updateFlags();
    }
    
    public boolean isVertSeqnoIncr() {
        return vertSeqnoIncr;
    }
    
    public void setVertSeqnoIncr(boolean vertSeqnoIncr) {
        this.vertSeqnoIncr = vertSeqnoIncr;
        updateFlags();
    }
    
    public int getVersion() {
        return version;
    }
    
    public void setVersion(int version) {
        this.version = version;
    }
    
    public boolean isNotMaster() {
        return notMaster;
    }
    
    public void setNotMaster(boolean notMaster) {
        this.notMaster = notMaster;
    }
    
    public int getGenUtime() {
        return genUtime;
    }
    
    public void setGenUtime(int genUtime) {
        this.genUtime = genUtime;
    }
    
    public long getStartLt() {
        return startLt;
    }
    
    public void setStartLt(long startLt) {
        this.startLt = startLt;
    }
    
    public long getEndLt() {
        return endLt;
    }
    
    public void setEndLt(long endLt) {
        this.endLt = endLt;
    }
    
    public long getGenValidatorListHashShort() {
        return genValidatorListHashShort;
    }
    
    public void setGenValidatorListHashShort(long genValidatorListHashShort) {
        this.genValidatorListHashShort = genValidatorListHashShort;
    }
    
    public long getGenCatchainSeqno() {
        return genCatchainSeqno;
    }
    
    public void setGenCatchainSeqno(long genCatchainSeqno) {
        this.genCatchainSeqno = genCatchainSeqno;
    }
    
    public long getMinRefMcSeqno() {
        return minRefMcSeqno;
    }
    
    public void setMinRefMcSeqno(long minRefMcSeqno) {
        this.minRefMcSeqno = minRefMcSeqno;
    }
    
    public long getPrevKeyBlockSeqno() {
        return prevKeyBlockSeqno;
    }
    
    public void setPrevKeyBlockSeqno(long prevKeyBlockSeqno) {
        this.prevKeyBlockSeqno = prevKeyBlockSeqno;
    }
    
    public List<BlockIdExt> getMasterRefSeqno() {
        return masterRefSeqno;
    }
    
    public void setMasterRefSeqno(List<BlockIdExt> masterRefSeqno) {
        this.masterRefSeqno = masterRefSeqno;
    }
    
    private void updateFlags() {
        flags = 0;
        if (afterMerge) flags |= 1;
        if (afterSplit) flags |= 2;
        if (beforeSplit) flags |= 4;
        if (wantSplit) flags |= 8;
        if (wantMerge) flags |= 16;
        if (keyBlock) flags |= 32;
        if (vertSeqnoIncr) flags |= 64;
    }
}
