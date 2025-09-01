package org.ton.ton4j.tlb;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.math.BigInteger;

/**
 * BlockLocation represents the file location information for a block stored in packages.
 * This includes the package_id (which package file contains the block), offset within
 * the package, and size of the stored block data.
 * 
 * This is based on the original TON C++ implementation where the global index maps:
 * file_hash → (package_id, offset, size)
 */
@Builder
@Data
public class BlockLocation implements Serializable {
    /**
     * The package identifier that determines which package file contains this block.
     * For Files database packages: corresponds to the package file number (e.g., 0000000100.pack)
     * For traditional archive packages: corresponds to the archive sequence number
     */
    long packageId;
    
    /**
     * The offset of the block within the package file (uint64_t in C++)
     */
    BigInteger offset;
    
    /**
     * The size of the stored block data (uint64_t in C++)
     */
    BigInteger size;
    
    /**
     * The block hash for reference (optional, used for debugging/validation)
     */
    String hash;
    
    /**
     * Creates a BlockLocation from the components typically stored in the global index.
     * 
     * @param packageId The package identifier
     * @param offset The offset within the package
     * @param size The size of the block data
     * @return A new BlockLocation instance
     */
    public static BlockLocation create(long packageId, long offset, long size) {
        return BlockLocation.builder()
                .packageId(packageId)
                .offset(BigInteger.valueOf(offset))
                .size(BigInteger.valueOf(size))
                .build();
    }
    
    /**
     * Creates a BlockLocation with hash for reference.
     * 
     * @param hash The block hash
     * @param packageId The package identifier
     * @param offset The offset within the package
     * @param size The size of the block data
     * @return A new BlockLocation instance
     */
    public static BlockLocation create(String hash, long packageId, long offset, long size) {
        return BlockLocation.builder()
                .hash(hash)
                .packageId(packageId)
                .offset(BigInteger.valueOf(offset))
                .size(BigInteger.valueOf(size))
                .build();
    }
    
    /**
     * Validates that this BlockLocation contains reasonable values.
     * 
     * @return true if the location appears valid
     */
    public boolean isValid() {
        return offset != null && size != null 
                && offset.compareTo(BigInteger.ZERO) >= 0 
                && size.compareTo(BigInteger.ZERO) > 0
                && size.compareTo(BigInteger.valueOf(100_000_000)) <= 0; // Max 100MB per block
    }
    
    @Override
    public String toString() {
        return String.format("BlockLocation{packageId=%d, offset=%s, size=%s, hash='%s'}", 
                packageId, offset, size, hash != null ? hash : "null");
    }
}
