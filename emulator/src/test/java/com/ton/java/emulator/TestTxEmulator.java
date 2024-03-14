package com.ton.java.emulator;

import com.sun.jna.Native;
import lombok.extern.slf4j.Slf4j;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.TonHashMapE;
import org.ton.java.emulator.TxEmulator;
import org.ton.java.emulator.TxEmulatorI;
import org.ton.java.tonlib.Tonlib;
import org.ton.java.tonlib.types.SmcLibraryEntry;
import org.ton.java.tonlib.types.SmcLibraryResult;

import java.util.List;

import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

@Slf4j
@RunWith(JUnit4.class)
public class TestTxEmulator {

    static TxEmulator txEmulator;
    static Tonlib tonlib;
    static Cell config;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        tonlib = Tonlib.builder().build();
        config = tonlib.getConfigAll(128);
        txEmulator = TxEmulator.builder()
                .configBoc(config.toBase64())
                .build();
    }

    @Test
    public void testInitTxEmualator() {
        TxEmulatorI txEmulatorI = Native.load("emulator.dll", TxEmulatorI.class);
        long emulator = txEmulatorI.transaction_emulator_create(config.toBase64(), 2);
        assertNotEquals(0, emulator);
    }

    @Test
    public void testSetVerbosityLevel() {
        txEmulator.setVerbosityLevel(4);
    }

    @Test
    public void testTxEmulatorIgnoreCheckSignature() {
        assertTrue(txEmulator.setIgnoreCheckSignature(true));
    }

    @Test
    public void testTxEmulatorSetLt() {
        assertTrue(txEmulator.setEmulatorLt(200000));
    }

    @Test
    public void testTxEmulatorSetLibs() {
        SmcLibraryResult result = tonlib.getLibraries(
                List.of("wkUmK4wrzl6fzSPKM04dVfqW1M5pqigX3tcXzvy6P3M="));
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
                k -> CellBuilder.beginCell().storeUint((Long) k, 256).bits,
                v -> CellBuilder.beginCell().storeRef((Cell) v)
        );

        log.info("txEmulator.setLibs() result {}", txEmulator.setLibs(dictLibs.toBase64()));
    }
}
