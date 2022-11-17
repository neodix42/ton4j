package org.ton.java.tonlib;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.ToNumberPolicy;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.ton.java.cell.CellBuilder;
import org.ton.java.tonlib.types.*;
import org.ton.java.utils.Utils;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@Slf4j
@RunWith(JUnit4.class)
public class TestParseRunMethodResults {

    private static final Gson gson = new GsonBuilder().setObjectToNumberStrategy(ToNumberPolicy.BIG_DECIMAL).create();

    @Test
    public void tesTvmRenderStack() {

        CellBuilder c1 = CellBuilder.beginCell();
        c1.storeUint((long) Math.pow(2, 25), 26);

        Deque<String> d = new ArrayDeque<>();

        d.offer("[num, 300]");
        d.offer("[cell, " + Utils.bytesToHex(c1.endCell().toBoc(false)) + "]");

        Deque<TvmStackEntry> r0 = ParseRunResult.renderTvmStack(d);
        Deque<TvmStackEntry> r1 = ((ArrayDeque) r0).clone();
        log.info(r0.pop().toString());
        log.info(r0.pop().toString());
        assertThat(r0.isEmpty()).isTrue();

        log.info("size {}", r1.size());

        Deque<String> s = ParseRunResult.serializeTvmStack(r1);
        log.info(s.pop());
        log.info(s.pop());
        assertThat(r0.size()).isZero();
    }

    @Test
    public void tesTvmRenderParticipantsList() {

        String tonlib_result_participants_list = Utils.streamToString(Objects.requireNonNull(TestTonlibJson.class.getClassLoader().getResourceAsStream("participants_list.json")));

        TvmStackEntryList list = gson.fromJson(tonlib_result_participants_list, TvmStackEntryList.class);

        log.info(list.toString());

        TvmStackEntryList listResult = ParseRunResult.parseTvmEntryListStack(list);

        for (Object l : listResult.getList().getElements()) {
            TvmStackEntryTuple tuple = (TvmStackEntryTuple) l;
            log.info(tuple.toString());
        }

        assertThat(listResult.getList().getElements()).isNotEqualTo(0);
    }

    @Test
    public void testGetTypeRunResultStackTuple() {
        String stackTuple = Utils.streamToString(Objects.requireNonNull(TestTonlibJson.class.getClassLoader().getResourceAsStream("RunMethodResultStackEntryTuple.json")));

        RunResultGeneric<String> g = gson.fromJson(stackTuple, RunResultGeneric.class);
        log.info("generic: {}", g);
        RunResult result = ParseRunResult.getTypedRunResult(g.getStack(), 0, 0);
        log.info(result.toString());
    }

    @Test
    public void testGetTypeRunResultStackList() {
        String stackTuple = Utils.streamToString(Objects.requireNonNull(TestTonlibJson.class.getClassLoader().getResourceAsStream("RunMethodResultStackEntryList.json")));

        RunResultGeneric<String> g = gson.fromJson(stackTuple, RunResultGeneric.class);
        log.info("generic: {}", g);
        RunResult result = ParseRunResult.getTypedRunResult(g.getStack(), 0, 0);
        log.info(result.toString());
    }

    @Test
    public void testGetTypeRunResultStackTupleWithList() {
        String stackTuple = Utils.streamToString(Objects.requireNonNull(TestTonlibJson.class.getClassLoader().getResourceAsStream("RunMethodResultStackEntryTupleWithList.json")));

        RunResultGeneric<String> g = gson.fromJson(stackTuple, RunResultGeneric.class);
        log.info("g: {}", g);
        RunResult result = ParseRunResult.getTypedRunResult(g.getStack(), 0, 0);
        log.info(result.toString());
    }

    @Test
    public void testGetTypeRunResultStackEntryListWithTuple() {
        String stackTuple = Utils.streamToString(Objects.requireNonNull(TestTonlibJson.class.getClassLoader().getResourceAsStream("RunMethodResultStackEntryListWithTuple.json")));

        RunResultGeneric<String> g = gson.fromJson(stackTuple, RunResultGeneric.class);
        log.info("g: {}", g);
        RunResult result = ParseRunResult.getTypedRunResult(g.getStack(), 0, 0);
        log.info(result.toString());
    }

    @Test
    public void testGetTypeRunResultStackEntryListPastElections() {
        String stackTuple = Utils.streamToString(Objects.requireNonNull(TestTonlibJson.class.getClassLoader().getResourceAsStream("RunMethodResultPastElections.json")));

        RunResultGeneric<String> g = gson.fromJson(stackTuple, RunResultGeneric.class);
        log.info("g: {}", g);
        RunResult result = ParseRunResult.getTypedRunResult(g.getStack(), 0, 0);
        log.info("result {}", result.toString());
        log.info("{}", ParseRunResult.markers.size());
    }

    @Test
    public void testGetTypeRunResultGetSubscriptionData() {
        String stackTuple = Utils.streamToString(Objects.requireNonNull(TestTonlibJson.class.getClassLoader().getResourceAsStream("RunMethodGetSubscriptionData.json")));

        RunResultGeneric<String> g = gson.fromJson(stackTuple, RunResultGeneric.class);
        log.info("g: {}", g);

        RunResult result = ParseRunResult.getTypedRunResult(g.getStack(), 0, 0);
        log.info(result.toString());

        TvmStackEntryTuple walletAddr = (TvmStackEntryTuple) result.getStackEntry().get(0);
        TvmStackEntryNumber wc = (TvmStackEntryNumber) walletAddr.getTuple().getElements().get(0);
        TvmStackEntryNumber hash = (TvmStackEntryNumber) walletAddr.getTuple().getElements().get(1);
        log.info("walletAddr: {}:{}", wc.getNumber(), hash.getNumber().toString(16));

        TvmStackEntryTuple beneficiaryAddr = (TvmStackEntryTuple) result.getStackEntry().get(1);
        TvmStackEntryNumber beneficiaryAddrWc = (TvmStackEntryNumber) beneficiaryAddr.getTuple().getElements().get(0);
        TvmStackEntryNumber beneficiaryAddrHash = (TvmStackEntryNumber) beneficiaryAddr.getTuple().getElements().get(1);
        log.info("walletAddr: {}:{}", beneficiaryAddrWc.getNumber(), beneficiaryAddrHash.getNumber().toString(16));

        TvmStackEntryNumber amount = (TvmStackEntryNumber) result.getStackEntry().get(2);
        log.info("amount: {}", amount.getNumber());

        TvmStackEntryNumber period = (TvmStackEntryNumber) result.getStackEntry().get(3);
        log.info("period: {}", period.getNumber());

        TvmStackEntryNumber startTime = (TvmStackEntryNumber) result.getStackEntry().get(4);
        log.info("startTime: {}", startTime.getNumber());

        TvmStackEntryNumber timeOut = (TvmStackEntryNumber) result.getStackEntry().get(5);
        log.info("timeOut: {}", timeOut.getNumber());

        TvmStackEntryNumber lastPaymentTime = (TvmStackEntryNumber) result.getStackEntry().get(6);
        log.info("lastPaymentTime: {}", lastPaymentTime.getNumber());

        TvmStackEntryNumber lastRequestTime = (TvmStackEntryNumber) result.getStackEntry().get(7);
        log.info("lastRequestTime: {}", lastRequestTime.getNumber());

        TvmStackEntryNumber failedAttempts = (TvmStackEntryNumber) result.getStackEntry().get(8);
        log.info("failedAttempts: {}", failedAttempts.getNumber());

        TvmStackEntryNumber subscriptionId = (TvmStackEntryNumber) result.getStackEntry().get(9);
        log.info("subscriptionId: {}", subscriptionId.getNumber());
    }
}
