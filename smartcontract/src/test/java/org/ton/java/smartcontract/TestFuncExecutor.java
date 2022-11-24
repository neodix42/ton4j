package org.ton.java.smartcontract;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.ton.java.smartcontract.executors.func.FuncExecutor;

import java.io.IOException;

@Slf4j
@RunWith(JUnit4.class)

public class TestFuncExecutor {

    @Test
    public void TestExecutor() throws IOException {
        String result = new FuncExecutor().execute("-h");

        log.info("result {}", result);
    }
}
