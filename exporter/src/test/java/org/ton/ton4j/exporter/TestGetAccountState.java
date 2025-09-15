package org.ton.ton4j.exporter;

import lombok.extern.slf4j.Slf4j;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@Slf4j
@RunWith(JUnit4.class)
public class TestGetAccountState {

  public static final String TON_DB_ROOT_PATH =
      "/home/neodix/gitProjects/MyLocalTon/myLocalTon/genesis/db";

  //  @Test
  //  public void testGetAccountState() throws IOException {
  //    Exporter exporter = Exporter.builder().tonDatabaseRootPath(TON_DB_ROOT_PATH).build();
  //
  //    // Test with a known address
  //    Address testAddress =
  //        Address.of("-1:6744E92C6F71C776FBBCEF299E31BF76F39C245CD56F2075B89C6A22026B4131");
  //
  //    long startTime = System.currentTimeMillis();
  //
  //    Account account = exporter.getAccountState(testAddress);
  //
  //    long durationMs = System.currentTimeMillis() - startTime;
  //
  //    log.info("getAccountState() completed in: {}ms", durationMs);
  //
  //    if (account != null) {
  //      log.info("Account state retrieved successfully:");
  //      log.info("  Address: {}", testAddress.toString(false));
  //      log.info("  Balance: {}", Utils.formatNanoValue(account.getBalance()));
  //      log.info("  Account type: {}", account.getClass().getSimpleName());
  //
  //      assertThat(account).isNotNull();
  //      assertThat(account.getBalance()).isNotNull();
  //    } else {
  //      log.info("No account state found for address: {}", testAddress.toString(false));
  //      // This is also a valid result - the account might not exist
  //    }
  //  }
  //
  //  @Test
  //  public void testGetAccountStateVsGetAccountByAddress() throws IOException {
  //    Exporter exporter = Exporter.builder().tonDatabaseRootPath(TON_DB_ROOT_PATH).build();
  //
  //    // Test with a known address
  //    Address testAddress =
  //        Address.of("-1:6744E92C6F71C776FBBCEF299E31BF76F39C245CD56F2075B89C6A22026B4131");
  //
  //    // Test both methods and compare results
  //    long startTime1 = System.currentTimeMillis();
  //    Account accountFromState = exporter.getAccountState(testAddress);
  //    long duration1 = System.currentTimeMillis() - startTime1;
  //
  //    long startTime2 = System.currentTimeMillis();
  //
  //    Account accountFromAddress = exporter.getAccountByAddress(testAddress);
  //    long duration2 = System.currentTimeMillis() - startTime2;
  //
  //    log.info("getAccountState() completed in: {}ms", duration1);
  //    log.info("getAccountByAddress() completed in: {}ms", duration2);
  //
  //    // Both methods should return the same result
  //    if (accountFromState != null && accountFromAddress != null) {
  //      log.info("Both methods returned account data:");
  //      log.info(
  //          "  getAccountState() balance: {}",
  // Utils.formatNanoValue(accountFromState.getBalance()));
  //      log.info(
  //          "  getAccountByAddress() balance: {}",
  //          Utils.formatNanoValue(accountFromAddress.getBalance()));
  //
  //      assertThat(accountFromState.getBalance()).isEqualTo(accountFromAddress.getBalance());
  //    } else if (accountFromState == null && accountFromAddress == null) {
  //      log.info("Both methods returned null (account not found)");
  //    } else {
  //      log.warn("Methods returned different results:");
  //      log.warn("  getAccountState(): {}", accountFromState != null ? "found" : "null");
  //      log.warn("  getAccountByAddress(): {}", accountFromAddress != null ? "found" : "null");
  //    }
  //  }

  //  @Test
  //  public void testGetAccountStateWithLatestBlock() throws IOException {
  //    Exporter exporter = Exporter.builder().tonDatabaseRootPath(TON_DB_ROOT_PATH).build();
  //
  //    // First, get the latest block to verify we're working with recent data
  //    Block latestBlock = exporter.getLast();
  //    assertThat(latestBlock).isNotNull();
  //
  //    log.info("Latest block info:");
  //    log.info("  Workchain: {}", latestBlock.getBlockInfo().getShard().getWorkchain());
  //    log.info(
  //        "  Shard: {}",
  //        latestBlock.getBlockInfo().getShard().convertShardIdentToShard().toString(16));
  //    log.info("  Sequence Number: {}", latestBlock.getBlockInfo().getSeqno());
  //    log.info("  Timestamp: {}", latestBlock.getBlockInfo().getGenuTime());
  //
  //    // Now test account state retrieval
  //    Address testAddress =
  //        Address.of("-1:6744E92C6F71C776FBBCEF299E31BF76F39C245CD56F2075B89C6A22026B4131");
  //
  //    Account account = exporter.getAccountState(testAddress);
  //
  //    if (account != null) {
  //      log.info(
  //          "Account state retrieved from recent blockchain state (seqno: {})",
  //          latestBlock.getBlockInfo().getSeqno());
  //      log.info("  Balance: {}", Utils.formatNanoValue(account.getBalance()));
  //    } else {
  //      log.info("No account found for address in recent blockchain state");
  //    }
  //  }
  //
  //  @Test
  //  public void testExporterGetAccountByAddress() throws IOException {
  //    Exporter exporter = Exporter.builder().tonDatabaseRootPath(TON_DB_ROOT_PATH).build();
  //    Address testAddress =
  //        Address.of("-1:6744E92C6F71C776FBBCEF299E31BF76F39C245CD56F2075B89C6A22026B4131");
  //    long startTime = System.currentTimeMillis();
  //
  //    Account account = exporter.getAccountByAddress(testAddress);
  //    log.info("received account : {}ms", System.currentTimeMillis() - startTime);
  //    log.info("balance {}", Utils.formatNanoValue(account.getBalance()));
  //
  //    assertThat(account).isNotNull();
  //  }
}
