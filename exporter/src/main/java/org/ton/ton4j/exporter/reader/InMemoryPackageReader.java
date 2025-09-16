package org.ton.ton4j.exporter.reader;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellBuilder;
import org.ton.ton4j.cell.CellSlice;
import org.ton.ton4j.tlb.Block;

/**
 * Ultra-high performance in-memory package reader that loads entire pack files into RAM
 * for zero-disk-IO block processing. Optimized for systems with massive RAM (128GB+).
 * 
 * This reader loads the entire 10MB pack file into memory and pre-computes all block
 * offsets and sizes for O(1) access during processing.
 */
@Slf4j
public class InMemoryPackageReader implements PackageReaderInterface {

    private static final int PACKAGE_HEADER_MAGIC = 0xae8fdd01;
    private static final short ENTRY_HEADER_MAGIC = 0x1e8b;

    @Getter
    private final byte[] packageBuffer;
    private final ByteBuffer buffer;
    
    // Pre-computed maps for ultra-fast block access
    private final Map<String, BlockEntry> blockEntries;
    private final Map<String, byte[]> blockDataCache;
    
    // Statistics
    private final long totalSize;
    private final int totalEntries;
    
    /**
     * Creates an in-memory package reader from a complete package byte array.
     * This constructor performs all parsing upfront for maximum runtime performance.
     * 
     * @param packageData Complete package file data (typically ~10MB)
     * @throws IOException If package format is invalid
     */
    public InMemoryPackageReader(byte[] packageData) throws IOException {
        this.packageBuffer = packageData;
        this.totalSize = packageData.length;
        this.buffer = ByteBuffer.wrap(packageData).order(ByteOrder.LITTLE_ENDIAN);
        
        // Pre-size maps for optimal performance (estimate ~1000 blocks per package)
        this.blockEntries = new HashMap<>(1024);
        this.blockDataCache = new HashMap<>(1024);
        
        // Parse entire package structure upfront
        this.totalEntries = parseAllEntriesUpfront();
        
        log.debug("InMemoryPackageReader initialized: {} bytes, {} entries, {} blocks cached", 
            totalSize, totalEntries, blockEntries.size());
    }
    
    /**
     * Parses the entire package structure upfront and caches all block metadata.
     * This eliminates all parsing overhead during block processing.
     * 
     * @return Total number of entries found
     * @throws IOException If package format is invalid
     */
    private int parseAllEntriesUpfront() throws IOException {
        buffer.position(0);
        
        // Verify package header magic
        int magic = buffer.getInt();
        if (magic != PACKAGE_HEADER_MAGIC) {
            throw new IOException(
                "Invalid package header magic: 0x" + Integer.toHexString(magic) +
                ", expected: 0x" + Integer.toHexString(PACKAGE_HEADER_MAGIC));
        }
        
        int entryCount = 0;
        
        // Parse all entries and cache block metadata
        while (buffer.position() < buffer.limit()) {
            int entryStartPos = buffer.position();
            
            try {
                // Read entry header (8 bytes total)
                int header0 = buffer.getInt();
                int entryMagic = header0 & 0xFFFF;
                int filenameLength = (header0 >>> 16) & 0xFFFF;
                
                if (entryMagic != ENTRY_HEADER_MAGIC) {
                    throw new IOException(
                        "Invalid entry header magic: 0x" + Integer.toHexString(entryMagic) +
                        ", expected: 0x" + Integer.toHexString(ENTRY_HEADER_MAGIC));
                }
                
                int dataSize = buffer.getInt();
                
                // Read filename
                byte[] filenameBytes = new byte[filenameLength];
                buffer.get(filenameBytes);
                String filename = new String(filenameBytes);
                
                // Calculate data position
                int dataStartPos = buffer.position();
                
                // Skip over data for now
                buffer.position(dataStartPos + dataSize);
                
                entryCount++;
                
                // Cache block entries for fast access
                if (filename.startsWith("block_")) {
                    String hash = extractHashFromFilename(filename);
                    if (hash != null) {
                        BlockEntry blockEntry = new BlockEntry(
                            filename, hash, dataStartPos, dataSize);
                        blockEntries.put(hash, blockEntry);
                    }
                }
                
            } catch (Exception e) {
                log.warn("Error parsing entry at position {}: {}", entryStartPos, e.getMessage());
                break;
            }
        }
        
        return entryCount;
    }
    
    /**
     * Gets block data with ultra-fast O(1) access from pre-computed cache.
     * This method performs zero parsing - all metadata was computed during construction.
     * 
     * @param hash Block hash
     * @return Block data, or null if not found
     */
    public byte[] getBlockData(String hash) {
        // Check cache first
        byte[] cachedData = blockDataCache.get(hash);
        if (cachedData != null) {
            return cachedData;
        }
        
        // Get from pre-computed entry map
        BlockEntry entry = blockEntries.get(hash);
        if (entry == null) {
            return null;
        }
        
        // Extract data from buffer (very fast array copy)
        byte[] blockData = new byte[entry.dataSize];
        System.arraycopy(packageBuffer, entry.dataOffset, blockData, 0, entry.dataSize);
        
        // Cache for future access (optional - uses more RAM but faster repeated access)
        blockDataCache.put(hash, blockData);
        
        return blockData;
    }
    
    /**
     * Gets all block data as a map. This method leverages pre-computed metadata
     * for maximum performance.
     * 
     * @return Map of hash to block data
     */
    public Map<String, byte[]> getAllBlocks() {
        Map<String, byte[]> allBlocks = new HashMap<>(blockEntries.size());
        
        for (Map.Entry<String, BlockEntry> entry : blockEntries.entrySet()) {
            String hash = entry.getKey();
            byte[] blockData = getBlockData(hash);
            if (blockData != null) {
                allBlocks.put(hash, blockData);
            }
        }
        
        return allBlocks;
    }
    
    /**
     * Processes all blocks using a callback function. This is the most efficient
     * way to process all blocks as it avoids creating intermediate collections.
     * 
     * @param processor Callback function for each block
     */
    public void forEachBlock(BlockProcessor processor) {
        for (Map.Entry<String, BlockEntry> entry : blockEntries.entrySet()) {
            String hash = entry.getKey();
            byte[] blockData = getBlockData(hash);
            if (blockData != null) {
                processor.process(hash, blockData);
            }
        }
    }
    
    /**
     * Extracts hash from a filename like "block_(-1,8000000000000000,100):hash1:hash2".
     * Returns the first hash (hash1) which is typically used as the key.
     */
    private String extractHashFromFilename(String filename) {
        try {
            if (filename.contains("):")) {
                int colonIndex = filename.indexOf("):");
                if (colonIndex != -1) {
                    String hashPart = filename.substring(colonIndex + 2);
                    int nextColonIndex = hashPart.indexOf(':');
                    if (nextColonIndex != -1) {
                        return hashPart.substring(0, nextColonIndex);
                    } else {
                        return hashPart;
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Error extracting hash from filename {}: {}", filename, e.getMessage());
        }
        return null;
    }
    
    // PackageReaderInterface implementation (for compatibility)
    
    @Override
    public Object getEntryAt(long offset) throws IOException {
        // This method is less efficient for in-memory reader, but provided for compatibility
        if (offset < 4 || offset >= packageBuffer.length) {
            throw new IOException("Invalid offset: " + offset);
        }
        
        ByteBuffer tempBuffer = ByteBuffer.wrap(packageBuffer).order(ByteOrder.LITTLE_ENDIAN);
        tempBuffer.position((int) offset);
        
        // Read entry header
        int header0 = tempBuffer.getInt();
        int entryMagic = header0 & 0xFFFF;
        int filenameLength = (header0 >>> 16) & 0xFFFF;
        
        if (entryMagic != ENTRY_HEADER_MAGIC) {
            throw new IOException("Invalid entry header magic at offset " + offset);
        }
        
        int dataSize = tempBuffer.getInt();
        
        // Read filename
        byte[] filenameBytes = new byte[filenameLength];
        tempBuffer.get(filenameBytes);
        String filename = new String(filenameBytes);
        
        // Read data
        byte[] data = new byte[dataSize];
        tempBuffer.get(data);
        
        return new PackageEntry(filename, data);
    }
    
    @Override
    public void forEach(Consumer<Object> consumer) throws IOException {
        for (Map.Entry<String, BlockEntry> entry : blockEntries.entrySet()) {
            BlockEntry blockEntry = entry.getValue();
            byte[] data = getBlockData(entry.getKey());
            if (data != null) {
                consumer.accept(new PackageEntry(blockEntry.filename, data));
            }
        }
    }
    
    @Override
    public Map<String, byte[]> readAllEntries() throws IOException {
        return getAllBlocks();
    }
    
    @Override
    public void close() throws IOException {
        // Nothing to close - all data is in memory
        blockDataCache.clear();
        log.debug("InMemoryPackageReader closed, cache cleared");
    }
    
    // Statistics and monitoring
    
    public long getTotalSize() {
        return totalSize;
    }
    
    public int getTotalEntries() {
        return totalEntries;
    }
    
    public int getBlockCount() {
        return blockEntries.size();
    }
    
    public int getCacheSize() {
        return blockDataCache.size();
    }
    
    public long getEstimatedMemoryUsage() {
        long baseSize = totalSize; // Package buffer
        long metadataSize = blockEntries.size() * 100; // Rough estimate for metadata
        long cacheSize = blockDataCache.values().stream()
            .mapToLong(data -> data.length)
            .sum();
        return baseSize + metadataSize + cacheSize;
    }
    
    /**
     * Internal class to store pre-computed block metadata
     */
    private static class BlockEntry {
        final String filename;
        final String hash;
        final int dataOffset;
        final int dataSize;
        
        BlockEntry(String filename, String hash, int dataOffset, int dataSize) {
            this.filename = filename;
            this.hash = hash;
            this.dataOffset = dataOffset;
            this.dataSize = dataSize;
        }
    }
    
    /**
     * Functional interface for block processing callbacks
     */
    @FunctionalInterface
    public interface BlockProcessor {
        void process(String blockHash, byte[] blockData);
    }
    
    /**
     * Package entry class for compatibility
     */
    @Getter
    public static class PackageEntry {
        private final String filename;
        private final byte[] data;
        
        public PackageEntry(String filename, byte[] data) {
            this.filename = filename;
            this.data = data;
        }
        
        public Cell getCell() {
            return CellBuilder.beginCell().fromBoc(data).endCell();
        }
        
        public Block getBlock() {
            return Block.deserialize(
                CellSlice.beginParse(CellBuilder.beginCell().fromBoc(data).endCell()));
        }
    }
}
