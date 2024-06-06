package org.ton.java.emulator;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.ToNumberPolicy;
import com.iwebpp.crypto.TweetNaclFast;
import com.sun.jna.Native;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.util.Lists;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.CellSlice;
import org.ton.java.cell.TonHashMapE;
import org.ton.java.smartcontract.wallet.v3.WalletV3R2;
import org.ton.java.tlb.types.VmStack;
import org.ton.java.tlb.types.VmStackList;
import org.ton.java.tlb.types.VmStackValueInt;
import org.ton.java.tonlib.Tonlib;
import org.ton.java.tonlib.types.SmcLibraryEntry;
import org.ton.java.tonlib.types.SmcLibraryResult;
import org.ton.java.utils.Utils;

import java.math.BigInteger;
import java.time.Instant;
import java.util.Collections;

import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

@Slf4j
@RunWith(JUnit4.class)
public class TestTvmEmulator {

    static TvmEmulator tvmEmulator;
    static Tonlib tonlib;
    private static final Gson gson = new GsonBuilder().setObjectToNumberStrategy(ToNumberPolicy.BIG_DECIMAL).create();

    static WalletV3R2 contract;

    @BeforeClass
    public static void setUpBeforeClass() {
        TweetNaclFast.Signature.KeyPair keyPair = Utils.generateSignatureKeyPair();
        log.info("pubKey {}", Utils.bytesToHex(keyPair.getPublicKey()));
        tonlib = Tonlib.builder()
                .testnet(true)
                .ignoreCache(false)
                .build();

        contract = WalletV3R2.builder()
                .tonlib(tonlib)
                .keyPair(keyPair)
                .walletId(42)
                .build();

        Cell code = contract.getStateInit().getCode();
        Cell data = contract.getStateInit().getData();

        tvmEmulator = TvmEmulator.builder()
                .pathToEmulatorSharedLib("G:/libs/emulator.dll")
                .codeBoc(code.toBase64())
                .dataBoc(data.toBase64())
                .verbosityLevel(1)
                .build();
        tvmEmulator.setDebugEnabled(true);
    }

    @Test
    public void testInitTvmEmulator() {
        TvmEmulatorI tvmEmulatorI = Native.load("emulator.dll", TvmEmulatorI.class);
        long emulator = tvmEmulatorI.tvm_emulator_create(
                contract.getStateInit().getCode().toBase64(),
                contract.getStateInit().getData().toBase64(),
                2);
        assertNotEquals(0, emulator);
    }

    @Test
    public void testTvmEmulatorSetDebugEnabled() {
        assertTrue(tvmEmulator.setDebugEnabled(true));
    }

    @Test
    public void testTvmEmulatorSetGasLimit() {
        assertTrue(tvmEmulator.setGasLimit(200000));
    }

    @Test
    public void testTvmEmulatorSetLibs() {
        Cell dictLibs = getLibs();

        log.info("TvmEmulator.setLibs() result {}", tvmEmulator.setLibs(dictLibs.toBase64()));
    }

    @Test
    public void testTvmEmulatorSetC7() {
        String address = contract.getAddress().toBounceable();
        String randSeedHex = Utils.sha256("ABC");

        Cell config = tonlib.getConfigAll(128);

        assertTrue(tvmEmulator.setC7(address,
                Instant.now().getEpochSecond(),
                Utils.toNano(1).longValue(),
                randSeedHex
//                , null
                , config.toBase64() // optional
        ));
    }

    @Test
    public void testTvmEmulatorEmulateRunMethod() {

        Cell code = contract.getStateInit().getCode();
        Cell data = contract.getStateInit().getData();

        VmStack stack = VmStack.builder()
                .depth(0)
                .stack(VmStackList.builder()
                        .tos(Lists.emptyList())
                        .build())
                .build();

        String paramsBoc = CellBuilder.beginCell()
                .storeRef(code)
                .storeRef(data)
                .storeRef(stack.toCell())
                .storeRef(CellBuilder.beginCell()
                        .storeRef(stack.toCell()) // c7 ^VmStack
                        .storeRef(getLibs()) // libs ^Cell
                        .endCell()) // params
                .storeUint(85143, 32) // method-id - seqno
                .endCell()
                .toBase64();
        String result = tvmEmulator.emulateRunMethod(1, paramsBoc, Utils.toNano(1).longValue());
        log.info("result emulateRunMethod: {}", result); // todo - return null
    }

    @Test
    public void testTvmEmulatorRunGetMethod() {
        String result = tvmEmulator.runGetMethod(
                78748, // 78748 - get_public_key
                VmStack.builder()
                        .depth(0)
                        .stack(VmStackList.builder()
                                .tos(Lists.emptyList())
                                .build())
                        .build()
                        .toCell().toBase64());
        log.info("result runGetMethod: {}", result);

        GetMethodResult methodResult = gson.fromJson(result, GetMethodResult.class);
        log.info("methodResult: {}", methodResult);
        log.info("methodResult stack: {}", methodResult.getStack());

        Cell cellResult = CellBuilder.beginCell().fromBocBase64(methodResult.getStack()).endCell();
        log.info("cellResult {}", cellResult);
        VmStack stack = VmStack.deserialize(CellSlice.beginParse(cellResult));
        int depth = stack.getDepth();
        log.info("vmStack depth: {}", depth);
        VmStackList vmStackList = stack.getStack();
        log.info("vmStackList: {}", vmStackList.getTos());
        BigInteger pubKey = VmStackValueInt.deserialize(CellSlice.beginParse(vmStackList.getTos().get(0).toCell())).getValue();
        log.info("vmStackList value: {}", pubKey.toString(16)); // pubkey
    }

    @Test
    public void testTvmEmulatorSendExternalMessage() {
//        String result = tvmEmulator.sendExternalMessage();
    }

    @Test
    public void testTvmEmulatorSendInternalMessage() {
//        String result = tvmEmulator.sendInternalMessage();
    }

    @Test
    public void testTvmEmulatorSetPrevBlockInfo() {
//        assertTrue(tvmEmulator.setPrevBlockInfo());
    }


    private static Cell getLibs() {
        SmcLibraryResult result = tonlib.getLibraries(
                Collections.singletonList("wkUmK4wrzl6fzSPKM04dVfqW1M5pqigX3tcXzvy6P3M="));
        log.info("result: {}", result);

        TonHashMapE x = new TonHashMapE(256);

        for (SmcLibraryEntry l : result.getResult()) {
            String cellLibBoc = l.getData();
            Cell lib = Cell.fromBocBase64(cellLibBoc);
            log.info("cell lib {}", lib.toHex());
            x.elements.put(1L, lib);
            x.elements.put(2L, lib); // 2nd because of the bug in hashmap/e
        }

        Cell dictLibs = x.serialize(
                k -> CellBuilder.beginCell().storeUint((Long) k, 256).endCell().getBits(),
                v -> CellBuilder.beginCell().storeRef((Cell) v).endCell()
        );
        return dictLibs;
    }
}
