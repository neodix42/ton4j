package org.ton.ton4j.tl.types.db;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.ton.ton4j.cell.ByteReader;
import org.ton.ton4j.tl.types.db.block.BlockInfo;

/**
 * Specialized reader for TON archive database.
 */
public class ArchiveDbReader implements Closeable {
    private static final Logger log = Logger.getLogger(ArchiveDbReader.class.getName());
    
    private final String dbPath;
    private final Map<String, RocksDbWrapper> indexDbs = new HashMap<>();
    private final Map<String, PackageReader> packageReaders = new HashMap<>();
    private final Map<String, ArchiveInfo> archiveInfos = new HashMap<>();
    
    /**
     * Creates a new ArchiveDbReader.
     * 
     * @param dbPath Path to the archive database directory
     * @throws IOException If an I/O error occurs
     */
    public ArchiveDbReader(String dbPath) throws IOException {
        this.dbPath = dbPath;
        
        // Discover all archive folders
        discoverArchives();
    }
    
    /**
     * Discovers all archive folders and their associated index and package files.
     * 
     * @throws IOException If an I/O error occurs
     */
    private void discoverArchives() throws IOException {
        Path packagesPath = Paths.get(dbPath, "packages");
        if (!Files.exists(packagesPath)) {
            log.warning("Packages directory not found: " + packagesPath);
            return;
        }
        
        // Find all archive folders (arch0000, arch0001, etc.)
        Pattern archPattern = Pattern.compile("arch(\\d+)");
        
        Files.list(packagesPath)
            .filter(Files::isDirectory)
            .forEach(archDir -> {
                String dirName = archDir.getFileName().toString();
                Matcher matcher = archPattern.matcher(dirName);
                
                if (matcher.matches()) {
                    try {
                        int archiveId = Integer.parseInt(matcher.group(1));
                        
                        // Find all index files in this directory
                        List<Path> indexFiles = Files.list(archDir)
                            .filter(path -> path.toString().endsWith(".index"))
                            .collect(Collectors.toList());
                        
                        // Find all package files in this directory
                        List<Path> packageFiles = Files.list(archDir)
                            .filter(path -> path.toString().endsWith(".pack"))
                            .collect(Collectors.toList());
                        
                        // Create archive info
                        for (Path indexFile : indexFiles) {
                            String indexName = indexFile.getFileName().toString();
                            String baseName = indexName.substring(0, indexName.lastIndexOf('.'));
                            
                            // Find matching package file
                            Path packageFile = packageFiles.stream()
                                .filter(path -> path.getFileName().toString().startsWith(baseName) && 
                                               path.getFileName().toString().endsWith(".pack"))
                                .findFirst()
                                .orElse(null);
                            
                            if (packageFile != null) {
                                String archiveKey = dirName + "/" + baseName;
                                archiveInfos.put(archiveKey, new ArchiveInfo(
                                    archiveId,
                                    indexFile.toString(),
                                    packageFile.toString()
                                ));
                                
                                log.info("Discovered archive: " + archiveKey + 
                                    " (index: " + indexFile + ", package: " + packageFile + ")");
                            }
                        }
                    } catch (IOException e) {
                        log.severe("Error discovering archives in " + archDir + ": " + e.getMessage());
                    }
                }
            });
    }
    
    /**
     * Gets all available archive keys.
     * 
     * @return List of archive keys
     */
    public List<String> getArchiveKeys() {
        return new ArrayList<>(archiveInfos.keySet());
    }
    
    /**
     * Reads a block by its hash.
     * 
     * @param hash The block hash
     * @return The block data, or null if not found
     * @throws IOException If an I/O error occurs
     */
    public byte[] readBlock(String hash) throws IOException {
        // Try to find the block in any archive
        for (Map.Entry<String, ArchiveInfo> archiveEntry : archiveInfos.entrySet()) {
            String archiveKey = archiveEntry.getKey();
            ArchiveInfo archiveInfo = archiveEntry.getValue();
            
            try {
                // Get the index DB
                RocksDbWrapper indexDb = getIndexDb(archiveKey, archiveInfo.indexPath);
                
                // Try to get the offset from this index
                byte[] offsetBytes = indexDb.get(hash.getBytes());
                if (offsetBytes != null) {
                    // Convert the offset to a long
                    long offset = ByteBuffer.wrap(offsetBytes).order(ByteOrder.LITTLE_ENDIAN).getLong();
                    
                    // Get the package reader
                    PackageReader packageReader = getPackageReader(archiveKey, archiveInfo.packagePath);
                    
                    // Get the entry at the offset
                    PackageReader.PackageEntry entry = packageReader.getEntryAt(offset);
                    
                    return entry.getData();
                }
            } catch (IOException e) {
                log.warning("Error reading block " + hash + " from archive " + 
                    archiveKey + ": " + e.getMessage());
            }
        }
        
        // Block not found in any archive
        return null;
    }
    
    /**
     * Reads a block info by its hash.
     * 
     * @param hash The block hash
     * @return The block info, or null if not found
     * @throws IOException If an I/O error occurs
     */
    public BlockInfo readBlockInfo(String hash) throws IOException {
        byte[] data = readBlock(hash);
        if (data == null) {
            return null;
        }
        
        ByteReader reader = new ByteReader(data);
        byte[] magicBytes = new byte[4];
        System.arraycopy(data, 0, magicBytes, 0, 4);
        String magicId = bytesToHex(magicBytes);
        
        if (magicId.equals("27e7c64a")) { // db.block.info#4ac6e727
            ByteReader valueReader = new ByteReader(data);
            int[] bytesArray = valueReader.readBytes();
            byte[] signedBytes = new byte[bytesArray.length];
            for (int i = 0; i < bytesArray.length; i++) {
                signedBytes[i] = (byte) bytesArray[i];
            }
            ByteBuffer buffer = ByteBuffer.wrap(signedBytes);
            return BlockInfo.deserialize(buffer);
        } else {
            throw new IOException("Invalid block info magic: " + magicId);
        }
    }
    
    /**
     * Gets all blocks from all archives.
     * 
     * @return Map of block hash to block data
     * @throws IOException If an I/O error occurs
     */
    public Map<String, byte[]> getAllBlocks() throws IOException {
        Map<String, byte[]> blocks = new HashMap<>();
        
        // Iterate through all archives
        for (Map.Entry<String, ArchiveInfo> entry : archiveInfos.entrySet()) {
            String archiveKey = entry.getKey();
            ArchiveInfo archiveInfo = entry.getValue();
            
            try {
                // Get the index DB
                RocksDbWrapper indexDb = getIndexDb(archiveKey, archiveInfo.indexPath);
                
                // Get all key-value pairs from this index
                indexDb.forEach((key, value) -> {
                    try {
                        String hash = new String(key);
                        long offset = ByteBuffer.wrap(value).order(ByteOrder.LITTLE_ENDIAN).getLong();
                        
                        // Get the package reader
                        PackageReader packageReader = getPackageReader(archiveKey, archiveInfo.packagePath);
                        
                        // Get the entry at the offset
                        PackageReader.PackageEntry packageEntry = packageReader.getEntryAt(offset);
                        
                        blocks.put(hash, packageEntry.getData());
                    } catch (IOException e) {
                        log.warning("Error reading block from archive " + 
                            archiveKey + ": " + e.getMessage());
                    }
                });
            } catch (IOException e) {
                log.warning("Error reading blocks from archive " + 
                    archiveKey + ": " + e.getMessage());
            }
        }
        
        return blocks;
    }
    
    /**
     * Gets all block infos from all archives.
     * 
     * @return Map of block hash to block info
     * @throws IOException If an I/O error occurs
     */
    public Map<String, BlockInfo> getAllBlockInfos() throws IOException {
        Map<String, BlockInfo> blockInfos = new HashMap<>();
        
        Map<String, byte[]> blocks = getAllBlocks();
        for (Map.Entry<String, byte[]> entry : blocks.entrySet()) {
            String hash = entry.getKey();
            byte[] data = entry.getValue();
            
            try {
                ByteReader reader = new ByteReader(data);
                byte[] magicBytes = new byte[4];
                System.arraycopy(data, 0, magicBytes, 0, 4);
                String magicId = bytesToHex(magicBytes);
                
                if (magicId.equals("27e7c64a")) { // db.block.info#4ac6e727
                    ByteReader valueReader = new ByteReader(data);
                    int[] bytesArray = valueReader.readBytes();
                    byte[] signedBytes = new byte[bytesArray.length];
                    for (int i = 0; i < bytesArray.length; i++) {
                        signedBytes[i] = (byte) bytesArray[i];
                    }
                    ByteBuffer buffer = ByteBuffer.wrap(signedBytes);
                    BlockInfo blockInfo = BlockInfo.deserialize(buffer);
                    
                    blockInfos.put(hash, blockInfo);
                }
            } catch (Exception e) {
                log.warning("Error parsing block info for hash " + hash + ": " + e.getMessage());
            }
        }
        
        return blockInfos;
    }
    
    /**
     * Gets an index DB for a specific archive.
     * 
     * @param archiveKey The archive key
     * @param indexPath Path to the index file
     * @return The index DB
     * @throws IOException If an I/O error occurs
     */
    private RocksDbWrapper getIndexDb(String archiveKey, String indexPath) throws IOException {
        if (!indexDbs.containsKey(archiveKey)) {
            indexDbs.put(archiveKey, new RocksDbWrapper(indexPath));
        }
        
        return indexDbs.get(archiveKey);
    }
    
    /**
     * Gets a package reader for a specific archive.
     * 
     * @param archiveKey The archive key
     * @param packagePath Path to the package file
     * @return The package reader
     * @throws IOException If an I/O error occurs
     */
    private PackageReader getPackageReader(String archiveKey, String packagePath) throws IOException {
        if (!packageReaders.containsKey(archiveKey)) {
            packageReaders.put(archiveKey, new PackageReader(packagePath));
        }
        
        return packageReaders.get(archiveKey);
    }
    
    @Override
    public void close() throws IOException {
        // Close all index DBs
        for (RocksDbWrapper db : indexDbs.values()) {
            db.close();
        }
        
        // Close all package readers
        for (PackageReader reader : packageReaders.values()) {
            reader.close();
        }
    }
    
    /**
     * Converts a byte array to a hexadecimal string.
     * 
     * @param bytes The byte array to convert
     * @return The hexadecimal string
     */
    private static String bytesToHex(byte[] bytes) {
        StringBuilder hex = new StringBuilder(2 * bytes.length);
        for (byte b : bytes) {
            hex.append(Character.forDigit((b >> 4) & 0xF, 16))
               .append(Character.forDigit((b & 0xF), 16));
        }
        return hex.toString().toLowerCase();
    }
    
    /**
     * Information about an archive.
     */
    private static class ArchiveInfo {
        private final int id;
        private final String indexPath;
        private final String packagePath;
        
        public ArchiveInfo(int id, String indexPath, String packagePath) {
            this.id = id;
            this.indexPath = indexPath;
            this.packagePath = packagePath;
        }
    }
}
