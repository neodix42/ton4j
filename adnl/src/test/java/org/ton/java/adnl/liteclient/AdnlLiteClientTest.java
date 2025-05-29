package org.ton.java.adnl.liteclient;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;
import org.ton.java.adnl.liteclient.tl.GetMasterchainInfo;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for ADNL Lite Client
 */
public class AdnlLiteClientTest {
    
    @Test
    public void testTLSerialization() throws Exception {
        GetMasterchainInfo query = new GetMasterchainInfo();
        byte[] serialized = query.serialize();
        
        // Should contain constructor ID (4 bytes)
        assertEquals(4, serialized.length);
        
        // Verify constructor ID is correct (little endian)
        int constructorId = ((serialized[3] & 0xFF) << 24) |
                           ((serialized[2] & 0xFF) << 16) |
                           ((serialized[1] & 0xFF) << 8) |
                           (serialized[0] & 0xFF);
        
        assertEquals(0x2ee6b589, constructorId);
    }
    
    @Test
    public void testLiteServerConfig() {
        byte[] publicKey = new byte[32];
        for (int i = 0; i < 32; i++) {
            publicKey[i] = (byte) i;
        }
        
        AdnlLiteClient.LiteServerConfig config = 
            new AdnlLiteClient.LiteServerConfig("127.0.0.1", 46995, publicKey);
        
        assertEquals("127.0.0.1", config.getIp());
        assertEquals(46995, config.getPort());
        assertArrayEquals(publicKey, config.getPublicKey());
    }
    
    @Test
    public void testMasterchainInfo() {
        byte[] rootHash = new byte[32];
        byte[] fileHash = new byte[32];
        
        MasterchainInfo info = new MasterchainInfo(-1, -9223372036854775808L, 12345, 
                                                  rootHash, fileHash, 1640995200);
        
        assertEquals(-1, info.getWorkchain());
        assertEquals(-9223372036854775808L, info.getShard());
        assertEquals(12345, info.getSeqno());
        assertEquals(1640995200, info.getUnixTime());
        assertArrayEquals(rootHash, info.getRootHash());
        assertArrayEquals(fileHash, info.getFileHash());
    }
    
    @Test
    @Disabled("Requires actual network connection to liteserver")
    public void testRealConnection() throws Exception {
        // This test would require a real liteserver connection
        // Disabled by default to avoid network dependencies in unit tests
        
        AdnlLiteClient client = AdnlLiteClient.forTestnet();
        try {
            client.connect();
            assertTrue(client.isConnected());
            
            // Test ping
            long rtt = client.ping();
            assertTrue(rtt > 0);
            
            // Test getting masterchain info
            MasterchainInfo info = client.getMasterchainInfo();
            assertNotNull(info);
            assertTrue(info.getSeqno() > 0);
            
        } finally {
            client.close();
        }
    }
    
    @Test
    public void testConfigParsing() throws Exception {
        // Test parsing a sample config
        String sampleConfig = "{" +
            "\"liteservers\": [" +
            "{" +
            "\"ip\": \"127.0.0.1\"," +
            "\"port\": 46995," +
            "\"id\": {" +
            "\"key\": \"AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=\"" +
            "}" +
            "}" +
            "]" +
            "}";
        
        // Write sample config to temp file
        java.nio.file.Path tempFile = java.nio.file.Files.createTempFile("test-config", ".json");
        java.nio.file.Files.write(tempFile, sampleConfig.getBytes());
        
        try {
            AdnlLiteClient client = AdnlLiteClient.fromGlobalConfig(tempFile.toString(), 0);
            assertNotNull(client);
        } finally {
            java.nio.file.Files.deleteIfExists(tempFile);
        }
    }
}
