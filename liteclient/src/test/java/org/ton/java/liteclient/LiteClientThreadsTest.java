package org.ton.java.liteclient;

import com.anarsoft.vmlens.concurrent.junit.ConcurrentTestRunner;
import com.anarsoft.vmlens.concurrent.junit.ThreadCount;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ton.java.liteclient.api.ResultLastBlock;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@RunWith(ConcurrentTestRunner.class)
public class LiteClientThreadsTest {

    private LiteClient liteClient;

    @Before
    public void executedBeforeEach() throws IOException {
        InputStream liteClientStream = LiteClient.class.getClassLoader().getResourceAsStream("lite-client.exe");
        File f = new File("lite-client.exe");
        FileUtils.copyInputStreamToFile(liteClientStream, f);
        String pathToLiteClient = f.getAbsolutePath();
        liteClientStream.close();

        liteClient = LiteClient.builder()
                .pathToLiteClientBinary(pathToLiteClient)
                .testnet(true)
                .build();
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