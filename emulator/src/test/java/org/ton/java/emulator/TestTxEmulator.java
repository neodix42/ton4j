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
import org.ton.java.liteclient.LiteClient;
import org.ton.java.tlb.types.*;
import org.ton.java.tonlib.Tonlib;
import org.ton.java.tonlib.types.SmcLibraryEntry;
import org.ton.java.tonlib.types.SmcLibraryResult;
import org.ton.java.utils.Utils;

import java.math.BigInteger;
import java.util.List;

import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

@Slf4j
@RunWith(JUnit4.class)
public class TestTxEmulator {

    static TxEmulator txEmulator;
    static Tonlib tonlib;
    static Cell config;
    static LiteClient liteClient;

    @BeforeClass
    public static void setUpBeforeClass() {
        liteClient = LiteClient.builder()
                .pathToLiteClientBinary("G:\\Git_Projects\\ton4j\\emulator\\lite-client.exe")
                .build();
        tonlib = Tonlib.builder().build();
        config = tonlib.getConfigAll(128);
        txEmulator = TxEmulator.builder()
                .verbosityLevel(4)
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
        Cell dictLibs = getLibs();

        log.info("txEmulator.setLibs() result {}", txEmulator.setLibs(dictLibs.toBase64()));
    }


    @Test
    public void testTxEmulatorEmulateTickTockTx() {
//        Cell dictLibs = getLibs();
//        txEmulator.setLibs(dictLibs.toBase64());

//        ResultLastBlock lightBlock = LiteClientParser.parseLast(liteClient.executeLast());
//        Block block = LiteClientParser.parseDumpblock(liteClient.executeDumpblock(lightBlock), true, true);
//        log.info("block: {}", block);

        ShardAccount shardAccount = ShardAccount.builder()
                .account(
                        Account.builder()
                                .isNone(false)
                                .address(MsgAddressIntStd.builder()
                                        .workchainId((byte) -1)
                                        .address(new BigInteger("000000000000000000000000000000000000000000000000000000000000000", 16))
//                                        .address(new BigInteger("333333333333333333333333333333333333333333333333333333333333333", 16))
//                                        .address(new BigInteger("555555555555555555555555555555555555555555555555555555555555555", 16))
                                        .build())
                                .storageInfo(StorageInfo.builder()
                                        .storageUsed(StorageUsed.builder()
                                                .cellsUsed(BigInteger.ZERO)
                                                .bitsUsed(BigInteger.ZERO)
                                                .publicCellsUsed(BigInteger.ZERO)
                                                .build())
                                        .lastPaid(123654)
                                        .duePayment(Utils.toNano(2))
                                        .build())
                                .accountStorage(AccountStorage.builder()
//                                        .lastTransactionLt(BigInteger.TWO)
                                        .balance(CurrencyCollection.builder()
                                                .coins(Utils.toNano(20))
                                                .build())
                                        .accountState(AccountStateActive.builder()
                                                .stateInit(StateInit.builder()
//                                                        .depth(BigInteger.valueOf(1))
//                                                        .tickTock(TickTock.builder()
//                                                                .tick(false)
//                                                                .tock(true)
//                                                                .build())
                                                        .code(Cell.fromBoc("b5ee9c7241010101004e000098ff0020dd2082014c97ba9730ed44d0d70b1fe0a4f260810200d71820d70b1fed44d0d31fd3ffd15112baf2a122f901541044f910f2a2f80001d31f31d307d4d101fb00a4c8cb1fcbffc9ed5470102286"))
//                                                        .data(CellBuilder.beginCell().storeBit(true).endCell())
                                                        .build())
                                                .build())
                                        .accountStatus("ACTIVE")
                                        .build())
                                .build()
                )
//                .lastTransLt(BigInteger.ONE)
                .lastTransHash(BigInteger.TWO)
                .build();

        log.info("shardAccount: {}", shardAccount);
        String shardAccountBocBase64 = shardAccount.toCell().toBase64();
        log.info("shardAccountCellBocBase64: {}", shardAccountBocBase64);
        String result = txEmulator.emulateTickTockTransaction(shardAccountBocBase64, false);
        log.info("result {}", result);
    }

    @Test
    public void testTxEmulatorEmulateTx() {
//        Cell dictLibs = getLibs();
//        txEmulator.setLibs(dictLibs.toBase64());

//        ResultLastBlock lightBlock = LiteClientParser.parseLast(liteClient.executeLast());
//        Block block = LiteClientParser.parseDumpblock(liteClient.executeDumpblock(lightBlock), true, true);
//        log.info("block: {}", block);

        txEmulator.setVerbosityLevel(4);
        log.info("set setDebugEnabled 4 {}", txEmulator.setDebugEnabled(true));

        ShardAccount shardAccount = ShardAccount.builder()
                .account(
                        Account.builder()
                                .isNone(false)
                                .address(MsgAddressIntStd.builder()
                                        .workchainId((byte) -1)
                                        .address(new BigInteger("000000000000000000000000000000000000000000000000000000000000000", 16))
//                                        .address(new BigInteger("333333333333333333333333333333333333333333333333333333333333333", 16))
//                                        .address(new BigInteger("555555555555555555555555555555555555555555555555555555555555555", 16))
                                        .build())
                                .storageInfo(StorageInfo.builder()
                                        .storageUsed(StorageUsed.builder()
                                                .cellsUsed(BigInteger.ZERO)
                                                .bitsUsed(BigInteger.ZERO)
                                                .publicCellsUsed(BigInteger.ZERO)
                                                .build())
                                        .lastPaid(123654)
                                        .duePayment(Utils.toNano(2))
                                        .build())
                                .accountStorage(AccountStorage.builder()
//                                        .lastTransactionLt(BigInteger.TWO)
                                        .balance(CurrencyCollection.builder()
                                                .coins(Utils.toNano(20))
                                                .build())
                                        .accountState(AccountStateActive.builder()
                                                .stateInit(StateInit.builder()
//                                                        .depth(BigInteger.valueOf(1))
//                                                        .tickTock(TickTock.builder()
//                                                                .tick(false)
//                                                                .tock(true)
//                                                                .build())
                                                        .code(Cell.fromBoc("b5ee9c7241010101004e000098ff0020dd2082014c97ba9730ed44d0d70b1fe0a4f260810200d71820d70b1fed44d0d31fd3ffd15112baf2a122f901541044f910f2a2f80001d31f31d307d4d101fb00a4c8cb1fcbffc9ed5470102286"))
//                                                        .data(CellBuilder.beginCell().storeBit(true).endCell())
                                                        .build())
                                                .build())
                                        .accountStatus("ACTIVE")
                                        .build())
                                .build()
                )
//                .lastTransLt(BigInteger.ONE)
                .lastTransHash(BigInteger.TWO)
                .build();

        log.info("shardAccount: {}", shardAccount);
        String shardAccountBocBase64 = shardAccount.toCell().toBase64();
        log.info("shardAccountCellBocBase64: {}", shardAccountBocBase64);

        Message msgExtIn = Message.builder()
                .info(ExternalMessage.builder()
                        .srcAddr(MsgAddressExtNone.builder().build())
                        .dstAddr(MsgAddressIntStd.builder()
                                .workchainId((byte) -1)
                                .address(BigInteger.valueOf(20))
                                .build())
                        .importFee(BigInteger.TEN)
                        .build())
                .body(CellBuilder.beginCell().storeBit(true).endCell())
                .build();

        Message msgInternal = Message.builder()
                .info(InternalMessage.builder()
                        .iHRDisabled(false)
                        .bounce(false)
                        .bounced(false)
                        .srcAddr(MsgAddressIntStd.builder()
                                .workchainId((byte) -1)
                                .address(BigInteger.valueOf(20))
                                .build())
                        .dstAddr(MsgAddressIntStd.builder()
                                .workchainId((byte) -1)
                                .address(BigInteger.valueOf(22))
                                .build())
                        .value(CurrencyCollection.builder()
                                .coins(BigInteger.TWO)
                                .build())
                        .iHRFee(BigInteger.ZERO)
                        .fwdFee(BigInteger.ZERO)
                        .createdLt(BigInteger.ZERO)
                        .createdAt(0L)
                        .build())
                .build();


        int keySizeX = 15;
        TonHashMapE outMsgs = new TonHashMapE(keySizeX);

        outMsgs.elements.put(100L, msgInternal.toCell());
        outMsgs.elements.put(101L, msgInternal.toCell());


        InMsg inMsgTr = InMsgImportTr.builder()
                .inMsg(MsgEnvelope.builder()
                        .currAddr(IntermediateAddressSimple.builder()
                                .workchainId(-1)
                                .addrPfx(BigInteger.TEN)
                                .build())
                        .nextAddr(IntermediateAddressSimple.builder()
                                .workchainId(-1)
                                .addrPfx(BigInteger.valueOf(11))
                                .build())
                        .msg(msgExtIn)
//                        .msg(msgInternal)
                        .build())
                .outMsg(MsgEnvelope.builder()
                        .currAddr(IntermediateAddressSimple.builder()
                                .workchainId(-1)
                                .addrPfx(BigInteger.valueOf(11))
                                .build())
                        .nextAddr(IntermediateAddressSimple.builder()
                                .workchainId(-1)
                                .addrPfx(BigInteger.valueOf(11))
                                .build())
                        .msg(msgExtIn)
//                        .msg(msgInternal)
                        .build())
                .transitFee(BigInteger.valueOf(3000))
                .build();


        InMsg inMsgExt = InMsgImportExt.builder()
                .msg(msgInternal)
                .transaction(Transaction.builder()
                        .accountAddr(new BigInteger("000000000000000000000000000000000000000000000000000000000000000", 16))
                        .lt(BigInteger.ZERO)
                        .prevTxHash(BigInteger.ZERO)
                        .origStatus(AccountStates.ACTIVE)
                        .endStatus(AccountStates.ACTIVE)
                        .totalFees(CurrencyCollection.builder()
                                .coins(BigInteger.TEN)
                                .build())
                        .inOut(TransactionIO.builder()
//                                .in(msgExtIn)
                                .in(msgInternal)
                                .out(outMsgs)
                                .build())
                        .stateUpdate(HashUpdate.builder()
                                .oldHash(BigInteger.valueOf(42))
                                .newHash(BigInteger.valueOf(43))
                                .build())
                        .description(TransactionDescriptionOrdinary.builder()
                                        .creditFirst(false)
                                        .actionPhase(ActionPhase.builder()
                                                .success(true)
                                                .valid(true)
                                                .noFunds(false)
                                                .statusChange(AccStatusChange.builder()
                                                                .type("UNCHANGED")
                                                                .build())
                                                .resultCode(0)
                                                .totalActions(1)
                                                .specActions(0)
                                                .skippedActions(0)
                                                .messagesCreated(0)
                                                .totalMsgSize(StorageUsedShort.builder()
                                                        .bits(BigInteger.TWO)
                                                        .cells(BigInteger.ZERO)
                                                        .build())
                                                .build())
//                                                .computePhase(ComputeSkipReason.builder()
//                                                        .type("NO_STATE")
//                                                        .build())
                                        .computePhase(ComputePhaseVM.builder()
                                                .success(true)
                                                .msgStateUsed(false)
                                                .accountActivated(true)
                                                .gasFees(BigInteger.ZERO)
                                                .details(ComputePhaseVMDetails.builder()
                                                        .gasUsed(BigInteger.valueOf(1000))
                                                        .gasLimit(BigInteger.ZERO)
                                                        .gasCredit(BigInteger.ZERO)
                                                        .mode(0)
                                                        .exitCode(0)
                                                        .exitArg(0)
                                                        .vMSteps(2)
                                                        .vMInitStateHash(BigInteger.ONE)
                                                        .vMFinalStateHash(BigInteger.TWO)
                                                        .build())
                                                .build())
                                        .aborted(false)
                                        .destroyed(false)
                                        .build())
                        .build())
                .build();

        String result = txEmulator.emulateTransaction(shardAccountBocBase64, inMsgExt.toCell().toBase64());
        log.info("result {}", result);
    }

    private static Cell getLibs() {
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
        return dictLibs;
    }
}
