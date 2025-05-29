package org.ton.java.adnl;

import java.util.logging.Logger;

/**
 * Example demonstrating how to use the ADNL Lite Client
 */
public class AdnlLiteClientExample {
    private static final Logger logger = Logger.getLogger(AdnlLiteClientExample.class.getName());
    
    public static void main(String[] args) {
        try {
            // Example liteserver configuration (testnet)
            String host = "135.181.140.212";
            int port = 13206;
            String publicKey = "K0t2+BCrmzm14VWJBGZBhZZNZKKkUOOOQbDjBkn9+Ow=";
            
            logger.info("Creating ADNL Lite Client...");
            
            // Create and connect to liteserver
            AdnlLiteClient client = new AdnlLiteClient();
            client.connect(host, port, publicKey);
            
            logger.info("Connected to liteserver successfully!");
            
            // Get masterchain info
            AdnlLiteClient.MasterchainInfo info = client.getMasterchainInfo();
            logger.info("Masterchain info: " + info);
            
            // Example of using connection pool
            logger.info("Creating connection pool...");
            LiteClientConnectionPool pool = new LiteClientConnectionPool();
            pool.addConnection(host, port, publicKey);
            
            // Use pool to get masterchain info
            AdnlLiteClient.MasterchainInfo poolInfo = pool.getMasterchainInfo();
            logger.info("Pool masterchain info: " + poolInfo);
            
            // Clean up
            client.close();
            pool.close();
            
            logger.info("Example completed successfully!");
            
        } catch (Exception e) {
            logger.severe("Error in example: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
