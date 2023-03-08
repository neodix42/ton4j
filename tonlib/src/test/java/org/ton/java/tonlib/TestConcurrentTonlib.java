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
import java.util.List;
import java.util.random.RandomGenerator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.ton.java.tonlib.TestTonlibJson.ELECTOR_ADDRESSS;

@Slf4j
@RunWith(ConcurrentTestRunner.class)
public class TestConcurrentTonlib {

    /**
     * There is no need for @extra field, since each Tonlib instance handling only its requests
     */

    private static final List<String> addresses = List.of("EQCtPHFrtkIw3UC2rNfSgVWYT1MiMLDUtgMy2M7j1P_eNMDq", "EQBrA5RGdTvbITagqlTT4nTflJG_NTcVqS929TV0eTjRPe1Q", "EQCs04A1_QPPSoovBGoa17D1N24wWHVeKYzrbB2H9a9grW-n");
    private static final List<String> pubKeys = List.of("de225cb04a68d05b9f5be64c42f30842d76600bd681d654470bb982ffa218d9c", "1990e4eb05d6f309970837c3659f2e4194c2d2aa3ce6fd973bf12dbf8213feb8", "0d9b934a00a0cd64ad95bb6cf5dc1268eea862fba0882bc58f52a57dbd6abb7b");

//    private static Tonlib tonlib = Tonlib.builder().build(); // you can't use one tonlib instance for parallel queries

    @Test
    @ThreadCount(6)
    public void testTonlibRunMethod1() {
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

    @Test
    @ThreadCount(6)
    public void testTonlibRunMethod2() {

        Tonlib tonlib = Tonlib.builder()
                .ignoreCache(false)
                .build(); // instead, spawn new instance each time

        int i = RandomGenerator.getDefault().nextInt(0, 3);
        RunResult result = tonlib.runMethod(Address.of(addresses.get(i)), "get_public_key");
        TvmStackEntryNumber pubkey = (TvmStackEntryNumber) result.getStack().get(0);

        String resultPubKey = pubkey.getNumber().toString(16);
        log.info("{} with resultPubKey[{}]: {}", addresses.get(i), i, resultPubKey);

        assertThat(resultPubKey).isEqualTo(pubKeys.get(i));
    }
}
