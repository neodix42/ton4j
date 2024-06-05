package org.ton.java.emulator;

import com.sun.jna.Native;
import lombok.extern.slf4j.Slf4j;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.TonHashMapE;
import org.ton.java.tonlib.Tonlib;
import org.ton.java.tonlib.types.SmcLibraryEntry;
import org.ton.java.tonlib.types.SmcLibraryResult;

import java.math.BigInteger;
import java.util.Collections;

import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

@Slf4j
@RunWith(JUnit4.class)
public class TestTvmEmulator {

    static TvmEmulator tvmEmulator;
    static Tonlib tonlib;
//    static Cell config;

    @BeforeClass
    public static void setUpBeforeClass() {

        tonlib = Tonlib.builder().build();
//        config = tonlib.getConfigAll(128);
        tvmEmulator = TvmEmulator.builder()
                .codeBoc(null)
                .dataBoc(null)
                .build();
    }

    @Test
    public void testInitTvmEmulator() {
        TvmEmulatorI tvmEmulatorI = Native.load("emulator.dll", TvmEmulatorI.class);
        long emulator = tvmEmulatorI.tvm_emulator_create("codeBoc", "dataBoc", 2);
        assertNotEquals(0, emulator);
    }

    @Test
    public void testTvmEmulatorSetDebugEnabled() {
        assertTrue(tvmEmulator.setDebugEnabled(true));
    }

    @Test
    public void testTvmEmulatorSetGasLimit() {
        assertTrue(tvmEmulator.setGasLimit(BigInteger.valueOf(200000)));
    }

    @Test
    public void testTvmEmulatorSetLibs() {
        Cell dictLibs = getLibs();

        log.info("TvmEmulator.setLibs() result {}", tvmEmulator.setLibs(dictLibs.toBase64()));
    }

    @Test
    public void testTvmEmulatorSetC7() {
        assertTrue(tvmEmulator.setC7());
    }

    @Test
    public void testTvmEmulatorEmulateRunMethod() {
        String result = tvmEmulator.emulateRunMethod();
    }

    @Test
    public void testTvmEmulatorRunGetMethod() {
        String result = tvmEmulator.runGetMethod();
    }

    @Test
    public void testTvmEmulatorSendExternalMessage() {
        String result = tvmEmulator.sendExternalMessage();
    }

    @Test
    public void testTvmEmulatorSendInternalMessage() {
        String result = tvmEmulator.sendInternalMessage();
    }

    @Test
    public void testTvmEmulatorSetPrevBlockInfo() {
        assertTrue(tvmEmulator.setPrevBlockInfo());
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
