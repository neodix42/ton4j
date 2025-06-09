package org.ton.java.adnl.examples;

import org.ton.java.adnl.AdnlLiteClient;
import org.ton.ton4j.tl.liteserver.responses.MasterchainInfo;

/** Example demonstrating ADNL Lite Client usage */
public class LiteClientExample {

  public static void main(String[] args) {
    System.out.println("=== ADNL Lite Client Example ===");

    try {
      // Create lite client with testnet config
      System.out.println("Creating lite client for testnet...");
      AdnlLiteClient client = new AdnlLiteClient();

      // Connect to liteserver (using testnet config)
      System.out.println("Connecting to liteserver...");
      client.connect("135.181.140.212", 13206, "K0t3+IWLOXHYMvMcrGZDPs+pn58a17LFbnXoQkKc2xw=");

      if (client.isConnected()) {
        System.out.println("✓ Connected successfully!");

        // Get masterchain info
        System.out.println("\nGetting masterchain info...");
        MasterchainInfo info = client.getMasterchainInfo();
        System.out.println("✓ Masterchain info received:");
        System.out.println("  - Last block seqno: " + info.getLast().getSeqno());
        System.out.println("  - Last block workchain: " + info.getLast().getWorkchain());
        System.out.println("  - Last block shard: " + info.getLast().getShard());

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
