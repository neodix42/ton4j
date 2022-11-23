package org.ton.java.tonlib;

import com.anarsoft.vmlens.concurrent.junit.ConcurrentTestRunner;
import com.anarsoft.vmlens.concurrent.junit.ThreadCount;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ton.java.address.Address;
import org.ton.java.tonlib.types.MasterChainInfo;
import org.ton.java.tonlib.types.RunResult;
import org.ton.java.tonlib.types.TvmStackEntryNumber;
import org.ton.java.utils.Utils;

import java.math.BigInteger;
import java.util.ArrayDeque;
import java.util.Deque;

import static org.assertj.core.api.Assertions.assertThat;
import static org.ton.java.tonlib.TestTonlibJson.ELECTOR_ADDRESSS;

@Slf4j
@RunWith(ConcurrentTestRunner.class)
public class TestConcurrentTonlib {

    /**
     * There is no need for @extra field, since each Tonlib instance handling only its requests
     */
    @Test
    @ThreadCount(6)
    public void testTonlibRunMethod() {
        Tonlib tonlib = Tonlib.builder()
                .build();

        MasterChainInfo last = tonlib.getLast();
        log.info("last: {}", last);

        Address elector = Address.of(ELECTOR_ADDRESSS);
        Deque<String> stack = new ArrayDeque<>();
        Address address = Address.of("Ef_sR2c8U-tNfCU5klvd60I5VMXUd_U9-22uERrxrrt3uzYi");
        stack.offer("[num, " + address.toDecimal() + "]");

        RunResult result = tonlib.runMethod(elector, "compute_returned_stake", stack);

        BigInteger returnStake = ((TvmStackEntryNumber) result.getStack().get(0)).getNumber();

        log.info("return stake: {} ", Utils.formatNanoValue(returnStake.longValue()));
        assertThat(result.getExit_code()).isEqualTo(0L);
    }
}
