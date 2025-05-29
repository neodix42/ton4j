package org.ton.java.adnl.examples;

import org.ton.java.adnl.liteclient.AdnlLiteClient;
import org.ton.java.adnl.liteclient.MasterchainInfo;

/**
 * Example demonstrating ADNL Lite Client usage
 */
public class LiteClientExample {
    
    public static void main(String[] args) {
        System.out.println("=== ADNL Lite Client Example ===");
        
        try {
            // Create lite client for testnet
            System.out.println("Creating lite client for testnet...");
            AdnlLiteClient client = AdnlLiteClient.forTestnet();
            
            // Connect to liteserver
            System.out.println("Connecting to liteserver...");
            client.connect();
            
            if (client.isConnected()) {
                System.out.println("✓ Connected successfully!");
                
                // Test ping
                System.out.println("\nTesting connection with ping...");
                long rtt = client.ping();
                System.out.println("✓ Ping RTT: " + (rtt / 1_000_000.0) + " ms");
                
                // Get masterchain info
                System.out.println("\nGetting masterchain info...");
                MasterchainInfo info = client.getMasterchainInfo();
                System.out.println("✓ Masterchain info received:");
                System.out.println("  - Workchain: " + info.getWorkchain());
                System.out.println("  - Shard: " + info.getShard());
                System.out.println("  - Seqno: " + info.getSeqno());
                System.out.println("  - Unix time: " + info.getUnixTime());
                
                System.out.println("\n✓ All operations completed successfully!");
                
            } else {
                System.out.println("✗ Failed to connect");
            }
            
            // Close connection
            client.close();
            System.out.println("✓ Connection closed");
            
        } catch (Exception e) {
            System.err.println("✗ Error: " + e.getMessage());
            e.printStackTrace();
        }
        
        System.out.println("\n=== Example completed ===");
    }
}
