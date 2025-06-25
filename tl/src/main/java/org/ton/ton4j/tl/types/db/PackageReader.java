package org.ton.ton4j.tl.types.db;

import java.io.Closeable;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.function.Consumer;

/**
 * Reader for TON package files.
 */
public class PackageReader implements Closeable {
    
    private static final int PACKAGE_HEADER_MAGIC = 0xae8fdd01;
    private static final short ENTRY_HEADER_MAGIC = 0x1e8b;
    
    private final RandomAccessFile file;
    private long currentPosition;
    
    /**
     * Creates a new PackageReader.
     * 
     * @param path Path to the package file
     * @throws IOException If an I/O error occurs
     */
    public PackageReader(String path) throws IOException {
        file = new RandomAccessFile(path, "r");
        
        // Verify package header magic
        int magic = readInt();
        if (magic != PACKAGE_HEADER_MAGIC) {
            throw new IOException("Invalid package header magic: 0x" + 
                    Integer.toHexString(magic) + ", expected: 0x" + 
                    Integer.toHexString(PACKAGE_HEADER_MAGIC));
        }
        
        currentPosition = 4; // After header
    }
    
    /**
     * Reads the next entry in the package.
     * 
     * @return The next entry, or null if the end of the file is reached
     * @throws IOException If an I/O error occurs
     */
    public PackageEntry readNextEntry() throws IOException {
        if (currentPosition >= file.length()) {
            return null;
        }
        
        file.seek(currentPosition);
        
        // Read entry header
        short entryMagic = readShort();
        if (entryMagic != ENTRY_HEADER_MAGIC) {
            throw new IOException("Invalid entry header magic: 0x" + 
                    Integer.toHexString(entryMagic) + ", expected: 0x" + 
                    Integer.toHexString(ENTRY_HEADER_MAGIC));
        }
        
        short filenameLength = readShort();
        int dataSize = readInt();
        
        // Read filename
        byte[] filenameBytes = new byte[filenameLength];
        file.readFully(filenameBytes);
        String filename = new String(filenameBytes);
        
        // Read data
        byte[] data = new byte[dataSize];
        file.readFully(data);
        
        // Update position for next read
        currentPosition = file.getFilePointer();
        
        return new PackageEntry(filename, data);
    }
    
    /**
     * Gets an entry at a specific offset.
     * 
     * @param offset The offset in the file
     * @return The entry
     * @throws IOException If an I/O error occurs
     */
    public PackageEntry getEntryAt(long offset) throws IOException {
        long oldPosition = currentPosition;
        currentPosition = offset + 4; // Skip package header
        PackageEntry entry = readNextEntry();
        currentPosition = oldPosition;
        return entry;
    }
    
    /**
     * Iterates through all entries in the package.
     * 
     * @param consumer Consumer for entries
     * @throws IOException If an I/O error occurs
     */
    public void forEach(Consumer<PackageEntry> consumer) throws IOException {
        currentPosition = 4; // Reset to start (after header)
        
        PackageEntry entry;
        while ((entry = readNextEntry()) != null) {
            consumer.accept(entry);
        }
    }
    
    private int readInt() throws IOException {
        byte[] bytes = new byte[4];
        file.readFully(bytes);
        return ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).getInt();
    }
    
    private short readShort() throws IOException {
        byte[] bytes = new byte[2];
        file.readFully(bytes);
        return ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).getShort();
    }
    
    @Override
    public void close() throws IOException {
        file.close();
    }
    
    /**
     * Represents an entry in a package file.
     */
    public static class PackageEntry {
        private final String filename;
        private final byte[] data;
        
        public PackageEntry(String filename, byte[] data) {
            this.filename = filename;
            this.data = data;
        }
        
        public String getFilename() {
            return filename;
        }
        
        public byte[] getData() {
            return data;
        }
    }
}
