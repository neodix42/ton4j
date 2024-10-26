package org.ton.java.liteclient;

import static org.assertj.core.api.Assertions.assertThat;

import com.anarsoft.vmlens.concurrent.junit.ConcurrentTestRunner;
import com.anarsoft.vmlens.concurrent.junit.ThreadCount;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ton.java.liteclient.api.ResultLastBlock;

@Slf4j
@RunWith(ConcurrentTestRunner.class)
public class LiteClientThreadsTest {

  private LiteClient liteClient;

  @Before
  public void executedBeforeEach() {
    liteClient = LiteClient.builder().testnet(true).build();
  }

  @Test
  @ThreadCount(6)
  public void testLiteClientLastThreads() throws Exception {
    String resultLast = liteClient.executeLast();
    assertThat(resultLast).isNotEmpty();
    ResultLastBlock resultLastBlock = LiteClientParser.parseLast(resultLast);
    log.info("testLiteClientLastThreads tonBlockId {}", resultLastBlock);
    assertThat(resultLastBlock).isNotNull();
    String resultShards = liteClient.executeAllshards(resultLastBlock);
    log.info("testLiteClientLastThreads resultShards {}", resultShards);
    assertThat(resultShards).isNotEmpty();
    String resultBlock = liteClient.executeDumpblock(resultLastBlock);
    assertThat(resultBlock).isNotEmpty();
  }
}
