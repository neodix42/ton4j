package com.ton.java.emulator;

import com.sun.jna.Native;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.TonHashMap;
import org.ton.java.emulator.Emulator;
import org.ton.java.emulator.EmulatorI;
import org.ton.java.tonlib.Tonlib;
import org.ton.java.tonlib.types.BlockHeader;
import org.ton.java.tonlib.types.MasterChainInfo;

@Slf4j
@RunWith(JUnit4.class)
public class TestEmulator {

    static Emulator emulator;

    @Test
    public void testInitTonlibJson() {
        EmulatorI emulatorI = Native.load("emulator.dll", EmulatorI.class);
        String configBoc = "configBoc";
        int verbosityLevel = 2;
        long emulator = emulatorI.transaction_emulator_create(configBoc, verbosityLevel);
        System.out.println(emulator);
    }

    @Test
    public void testSimpleTx() {

        Tonlib tonlib = Tonlib.builder().build();
        MasterChainInfo masterChainInfo = tonlib.getLast();
        log.info("last masterChainInfo {}", masterChainInfo);

        BlockHeader blockHeader = tonlib.getBlockHeader(masterChainInfo.getLast());
        log.info("last blockHeader {}", blockHeader);

        Cell cellConfig = createConfig();

        String configBoc = cellConfig.toBase64();

        emulator = Emulator.builder()
                .configBoc(configBoc)
                .verbosityLevel(2)
                .build();
        emulator.setVerbosityLevel(4);
    }

    private Cell createConfig() {
        TonHashMap x = new TonHashMap(32);

        Cell dummyCell = CellBuilder.beginCell().storeUint(1, 16).endCell();

        TonHashMap config31 = new TonHashMap(256);
        config31.elements.put(11L, dummyCell);
        config31.elements.put(12L, dummyCell);
        Cell cellConfig12 = config31.serialize(
                k -> CellBuilder.beginCell().storeUint((Long) k, 265).bits,
                v -> CellBuilder.beginCell().storeCell((Cell) v)
        );

        x.elements.put(12L, dummyCell); // workchain description dictionary is empty (no configuration parameter #12)

        x.elements.put(13L, cellConfig12); // needSpecialSmc flag set

        Cell cellConfig = x.serialize(
                k -> CellBuilder.beginCell().storeUint((Long) k, 32).bits,
                v -> CellBuilder.beginCell().storeCell((Cell) v)
        );

        return cellConfig;

    }
}
