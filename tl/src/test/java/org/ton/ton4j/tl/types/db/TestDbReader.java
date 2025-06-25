package org.ton.ton4j.tl.types.db;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.ton.ton4j.tl.types.db.block.BlockInfo;

/**
 * Test class for demonstrating how to use the DbReader to read TON RocksDB files.
 */
public class TestDbReader {
    
    private static final String TON_DB_PATH = "H:\\G\\Git_Projects\\MyLocalTon\\myLocalTon\\genesis\\db";
    
    /**
     * Main method to run the tests.
     */
    public static void main(String[] args) {
        try {
            TestDbReader test = new TestDbReader();
            test.runTests();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Run all tests.
     */
    public void runTests() throws IOException {
        DbReader dbReader = null;
        try {
            dbReader = new DbReader(TON_DB_PATH);
            
            System.out.println("=== Testing Archive DB ===");
            testReadArchiveDb(dbReader);
            
            System.out.println("\n=== Testing Block Infos ===");
            testReadBlockInfos(dbReader);
            
            System.out.println("\n=== Testing Package Files ===");
            testReadPackageFiles();
            
            System.out.println("\n=== Testing Other DBs ===");
            testReadOtherDbs(dbReader);
        } finally {
            if (dbReader != null) {
                dbReader.close();
            }
        }
    }
    
    /**
     * Test reading archive database.
     */
    public void testReadArchiveDb(DbReader dbReader) throws IOException {
        ArchiveDbReader archiveDbReader = dbReader.getArchiveDbReader();
        
        // Get all available archive keys
        List<String> archiveKeys = archiveDbReader.getArchiveKeys();
        System.out.println("Available archive keys: " + archiveKeys);
        
        // Get all blocks
        Map<String, byte[]> blocks = archiveDbReader.getAllBlocks();
        System.out.println("Total blocks: " + blocks.size());
        
        // Print first few blocks
        int count = 0;
        for (Map.Entry<String, byte[]> entry : blocks.entrySet()) {
            if (count++ < 5) {
                System.out.println("Block hash: " + entry.getKey() + ", size: " + entry.getValue().length + " bytes");
            }
        }
    }
    
    /**
     * Test reading block infos.
     */
    public void testReadBlockInfos(DbReader dbReader) throws IOException {
        // Get all block infos
        Map<String, BlockInfo> blockInfos = dbReader.getAllBlockInfos();
        System.out.println("Total block infos: " + blockInfos.size());
        
        // Print first few block infos
        int count = 0;
        for (Map.Entry<String, BlockInfo> entry : blockInfos.entrySet()) {
            if (count++ < 5) {
                BlockInfo blockInfo = entry.getValue();
                System.out.println("Block hash: " + entry.getKey());
                System.out.println("  Version: " + blockInfo.getVersion());
                System.out.println("  Gen time: " + blockInfo.getGenUtime());
                System.out.println("  Start LT: " + blockInfo.getStartLt());
                System.out.println("  End LT: " + blockInfo.getEndLt());
                System.out.println("  Is key block: " + blockInfo.isKeyBlock());
                System.out.println("  Master refs: " + blockInfo.getMasterRefSeqno().size());
                System.out.println();
            }
        }
    }
    
    /**
     * Test reading package files.
     */
    public void testReadPackageFiles() throws IOException {
        // This test demonstrates how to read a specific package file
        try (PackageReader packageReader = new PackageReader(TON_DB_PATH + "\\archive\\packages\\arch0000\\archive.00000.pack")) {
            // Read all entries in the package
            packageReader.forEach(entry -> {
                System.out.println("Entry filename: " + entry.getFilename() + ", size: " + entry.getData().length + " bytes");
            });
        }
    }
    
    /**
     * Test reading other RocksDB databases.
     */
    public void testReadOtherDbs(DbReader dbReader) throws IOException {
        // Open the celldb database
        RocksDbWrapper cellDb = dbReader.openDb("celldb");
        
        // Print some stats
        System.out.println("CellDB stats:");
        System.out.println(cellDb.getStats());
        
        // Open the files database
        RocksDbWrapper filesDb = dbReader.openDb("files");
        
        // Print some stats
        System.out.println("FilesDB stats:");
        System.out.println(filesDb.getStats());
        
        // Open the adnl database
        RocksDbWrapper adnlDb = dbReader.openDb("adnl");
        
        // Print some stats
        System.out.println("ADNLDB stats:");
        System.out.println(adnlDb.getStats());
    }
}
