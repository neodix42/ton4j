package com.ton.java.emulator;

import com.sun.jna.Native;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.TonHashMap;
import org.ton.java.cell.TonHashMapE;
import org.ton.java.emulator.TxEmulator;
import org.ton.java.emulator.TxEmulatorI;
import org.ton.java.tlb.types.ConfigParams12;
import org.ton.java.tlb.types.WorkchainDescr;
import org.ton.java.tlb.types.WorkchainDescrV1;
import org.ton.java.tlb.types.WorkchainFormatBasic;
import org.ton.java.tonlib.Tonlib;
import org.ton.java.tonlib.types.BlockHeader;
import org.ton.java.tonlib.types.MasterChainInfo;

import java.math.BigInteger;

@Slf4j
@RunWith(JUnit4.class)
public class TestTxEmulator {

    static TxEmulator txEmulator;

    @Test
    public void testInitTonlibJson() {
        TxEmulatorI txEmulatorI = Native.load("emulator.dll", TxEmulatorI.class);
        String configBoc = "configBoc";
        int verbosityLevel = 2;
        long emulator = txEmulatorI.transaction_emulator_create(configBoc, verbosityLevel);
        System.out.println(emulator);
    }

    @Test
    public void testSimpleTx() {

        Tonlib tonlib = Tonlib.builder()
//                .verbosityLevel(VerbosityLevel.DEBUG)
                .build();
        MasterChainInfo masterChainInfo = tonlib.getLast();
        log.info("last masterChainInfo {}", masterChainInfo);

        BlockHeader blockHeader = tonlib.getBlockHeader(masterChainInfo.getLast());
        log.info("last blockHeader {}", blockHeader);

        Cell config = tonlib.getConfigAll(128);

//        Cell cellConfig = createEmulatorConfig();

        String configBoc = config.toBase64();

        txEmulator = TxEmulator.builder()
                .configBoc(configBoc)
                .verbosityLevel(2)
                .build();
        txEmulator.setVerbosityLevel(4);
    }


    private Cell createEmulatorConfig() {

        WorkchainDescr workchainDescr = WorkchainDescrV1.builder()
                .workchain(-1)
                .acceptMsgs(true)
                .active(true)
                .basic(true)
                .minSplit(1)
                .maxSplit(10)
                .enabledSince(System.currentTimeMillis() / 1000)
                .format(WorkchainFormatBasic.builder()
                        .vmMode(BigInteger.ONE)
                        .vmVersion(6)
                        .build())
                .version(0)
                .flags(1)
                .zeroStateRootHash(BigInteger.ZERO)
                .zeroStateFileHash(BigInteger.ZERO)
                .build();

        TonHashMapE workchains = new TonHashMapE(32);
        workchains.elements.put(0L, workchainDescr);
        workchains.elements.put(1L, workchainDescr);
        ConfigParams12 configParams12 = ConfigParams12.builder()
                .workchains(workchains)
                .build();
//        String config12base64 = configParams12.toCell().toBase64();

        TonHashMap configDict = new TonHashMap(32);

        Cell dummyCell = CellBuilder.beginCell().storeUint(1, 16).endCell();

//        TonHashMap config31 = new TonHashMap(256);
//        config31.elements.put(11L, dummyCell);
//        config31.elements.put(12L, dummyCell);
//        Cell cellConfig12 = config31.serialize(
//                k -> CellBuilder.beginCell().storeUint((Long) k, 265).bits,
//                v -> CellBuilder.beginCell().storeCell((Cell) v)
//        );

        configDict.elements.put(12L, configParams12.toCell()); // workchain description dictionary is empty (no configuration parameter #12)
        configDict.elements.put(13L, configParams12.toCell()); // workchain description dictionary is empty (no configuration parameter #12)

//        x.elements.put(13L, configParams12.toCell()); // needSpecialSmc flag set

        Cell cellConfig = configDict.serialize(
                k -> CellBuilder.beginCell().storeUint((Long) k, 32).bits,
                v -> CellBuilder.beginCell().storeCell((Cell) v)
        );

        return cellConfig;

    }
}
