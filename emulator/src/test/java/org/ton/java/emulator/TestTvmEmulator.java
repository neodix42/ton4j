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
import org.ton.java.address.Address;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.CellSlice;
import org.ton.java.cell.TonHashMapE;
import org.ton.java.smartcontract.FuncCompiler;
import org.ton.java.smartcontract.GenericSmartContract;
import org.ton.java.smartcontract.types.WalletV4R2Config;
import org.ton.java.smartcontract.wallet.v4.WalletV4R2;
import org.ton.java.tlb.types.*;
import org.ton.java.tonlib.Tonlib;
import org.ton.java.tonlib.types.SmcLibraryEntry;
import org.ton.java.tonlib.types.SmcLibraryResult;
import org.ton.java.utils.Utils;

import java.io.IOException;
import java.math.BigInteger;
import java.time.Instant;
import java.util.Collections;
import java.util.concurrent.ExecutionException;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

@Slf4j
@RunWith(JUnit4.class)
public class TestTvmEmulator {

    static TvmEmulator tvmEmulator;
    static Tonlib tonlib;
    private static final Gson gson = new GsonBuilder().setObjectToNumberStrategy(ToNumberPolicy.BIG_DECIMAL).create();
    static WalletV4R2 contract;

    @BeforeClass
    public static void setUpBeforeClass() {
        TweetNaclFast.Signature.KeyPair keyPair = Utils.generateSignatureKeyPair();
        log.info("pubKey {}", Utils.bytesToHex(keyPair.getPublicKey()));
        tonlib = Tonlib.builder()
                .testnet(true)
                .ignoreCache(false)
                .build();

        contract = WalletV4R2.builder()
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
                .verbosityLevel(TvmVerbosityLevel.UNLIMITED)
                .build();
        tvmEmulator.setDebugEnabled(true);
    }

    @Test
    public void testInitTvmEmulator() {
        TvmEmulatorI tvmEmulatorI = Native.load("emulator.dll", TvmEmulatorI.class);
        long emulator = tvmEmulatorI.tvm_emulator_create(
                contract.getStateInit().getCode().toBase64(),
                contract.getStateInit().getData().toBase64(),
                TvmVerbosityLevel.UNLIMITED.ordinal());
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
    public void testTvmEmulatorRunGetMethodGetSeqNo() {
        String result = tvmEmulator.runGetMethod(
                85143 // seqno
        );
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
        BigInteger seqno = VmStackValueTinyInt.deserialize(CellSlice.beginParse(vmStackList.getTos().get(0).toCell())).getValue();
        log.info("seqno value: {}", seqno);
    }

    @Test
    public void testTvmEmulatorRunGetMethodGetSeqNoShortVersion() {
        log.info("seqno value: {}", tvmEmulator.runGetSeqNo());
    }

    @Test
    public void testTvmEmulatorRunGetMethodGetPubKey() {
        String result = tvmEmulator.runGetMethod(
                78748, // get_public_key
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
    public void testTvmEmulatorRunGetMethodGetPubKeyShortVersion() {
        log.info("contract's pubKey: {}", tvmEmulator.runGetPublicKey()); // pubkey
    }

    @Test
    public void testTvmEmulatorSendExternalMessage() {

        String address = contract.getAddress().toBounceable();
        String randSeedHex = Utils.sha256("ABC");
//        Cell configAll = tonlib.getConfigAll(128);

        // optionally set C7
        assertTrue(tvmEmulator.setC7(address,
                Instant.now().getEpochSecond(),
                Utils.toNano(1).longValue(),
                randSeedHex
                , null
//                , configAll.toBase64()
        ));

        WalletV4R2Config config = WalletV4R2Config.builder()
                .operation(0)
                .walletId(42)
                .seqno(0)
                .destination(Address.of("0:258e549638a6980ae5d3c76382afd3f4f32e34482dafc3751e3358589c8de00d"))
                .amount(Utils.toNano(0.331))
                .build();

        assertTrue(tvmEmulator.setLibs(getLibs().toBase64()));

        Message msg = contract.prepareExternalMsg(config);
        String resultBoc = tvmEmulator.sendExternalMessage(msg.getBody().toBase64());

        SendExternalMessageResult result = gson.fromJson(resultBoc, SendExternalMessageResult.class);
        log.info("result sendExternalMessage, exitCode: {}", result.getVm_exit_code());
        log.info("seqno value: {}", tvmEmulator.runGetSeqNo());
    }

    @Test
    public void testTvmEmulatorSendExternalMessageCustom() throws IOException, ExecutionException, InterruptedException {

        FuncCompiler smcFunc = FuncCompiler.builder()
                .contractPath("G:/smartcontracts/new-wallet-v4r2.fc")
                .build();

        String codeCellHex = smcFunc.compile();
        Cell codeCell = CellBuilder.beginCell().fromBoc(codeCellHex).endCell();

        byte[] publicKey = Utils.hexToSignedBytes("82A0B2543D06FEC0AAC952E9EC738BE56AB1B6027FC0C1AA817AE14B4D1ED2FB");
        byte[] secretKey = Utils.hexToSignedBytes("F182111193F30D79D517F2339A1BA7C25FDF6C52142F0F2C1D960A1F1D65E1E4");
        TweetNaclFast.Signature.KeyPair keyPair = TweetNaclFast.Signature.keyPair_fromSeed(secretKey);

        Cell dataCell = CellBuilder.beginCell()
                .storeUint(0, 32)  // seqno
                .storeUint(42, 32) // wallet id
                .storeBytes(keyPair.getPublicKey())
                .storeUint(0, 1)   //plugins dict empty
                .endCell();

        log.info("codeCellHex {}", codeCellHex);
        log.info("dataCellHex {}", dataCell.toHex());

        tvmEmulator = TvmEmulator.builder()
                .pathToEmulatorSharedLib("G:/libs/emulator.dll")
                .codeBoc(codeCell.toBase64())
                .dataBoc(dataCell.toBase64())
                .verbosityLevel(TvmVerbosityLevel.UNLIMITED)
                .build();

        tvmEmulator.setDebugEnabled(true);

        // optionally set C7
//        String address = contract.getAddress().toBounceable();
//        String randSeedHex = Utils.sha256("ABC");
////        Cell configAll = tonlib.getConfigAll(128);
//
//        assertTrue(tvmEmulator.setC7(address,
//                Instant.now().getEpochSecond(),
//                Utils.toNano(3).longValue(),
//                randSeedHex
//                , null
////                , configAll.toBase64()
//        ));

        GenericSmartContract smc = GenericSmartContract.builder()
                .tonlib(tonlib)
                .keyPair(keyPair)
                .code(codeCellHex)
                .data(dataCell.toHex())
                .build();


        WalletV4R2Config config = WalletV4R2Config.builder()
                .operation(0)
                .walletId(42)
                .seqno(0)
                .destination(Address.of("0:258e549638a6980ae5d3c76382afd3f4f32e34482dafc3751e3358589c8de00d"))
                .amount(Utils.toNano(0.331))
                .build();

        assertTrue(tvmEmulator.setLibs(getLibs().toBase64()));
        Cell transferBody = createTransferBody(config);
        Cell signedBody = CellBuilder.beginCell()
                .storeBytes(Utils.signData(keyPair.getPublicKey(), keyPair.getSecretKey(), transferBody.hash()))
                .storeCell(transferBody)
                .endCell();
        log.info("extMsg {}", signedBody.toHex());

        String resultBoc = tvmEmulator.sendExternalMessage(signedBody.toBase64());

        SendExternalMessageResult result = gson.fromJson(resultBoc, SendExternalMessageResult.class);
        log.info("result sendExternalMessage, exitCode: {}", result.getVm_exit_code());

        config = WalletV4R2Config.builder()
                .operation(0)
                .walletId(42)
                .seqno(1) // second transfer with seqno 0 fails with error code 33 - as expected
                .destination(Address.of("0:258e549638a6980ae5d3c76382afd3f4f32e34482dafc3751e3358589c8de00d"))
                .amount(Utils.toNano(0.332))
                .build();

        assertTrue(tvmEmulator.setLibs(getLibs().toBase64()));
        transferBody = createTransferBody(config);
        signedBody = CellBuilder.beginCell()
                .storeBytes(Utils.signData(keyPair.getPublicKey(), keyPair.getSecretKey(), transferBody.hash()))
                .storeCell(transferBody)
                .endCell();
        log.info("extMsg {}", signedBody.toHex());

        resultBoc = tvmEmulator.sendExternalMessage(signedBody.toBase64());

        result = gson.fromJson(resultBoc, SendExternalMessageResult.class);
        log.info("result sendExternalMessage, exitCode: {}", result.getVm_exit_code());
    }

    @Test
    public void testTvmEmulatorSendInternalMessageCustomContract() throws IOException, ExecutionException, InterruptedException {
        FuncCompiler smcFunc = FuncCompiler.builder()
                .contractPath("G:/smartcontracts/new-wallet-v4r2.fc")
                .build();

        String codeCellHex = smcFunc.compile();
        Cell codeCell = CellBuilder.beginCell().fromBoc(codeCellHex).endCell();

        TweetNaclFast.Signature.KeyPair keyPair = Utils.generateSignatureKeyPair();

        Cell dataCell = CellBuilder.beginCell()
                .storeUint(0, 32)  // seqno
                .storeUint(42, 32) // wallet id
                .storeBytes(keyPair.getPublicKey())
                .storeUint(0, 1)   //plugins dict empty
                .endCell();

        log.info("codeCellHex {}", codeCellHex);
        log.info("dataCellHex {}", dataCell.toHex());


        tvmEmulator = TvmEmulator.builder()
                .pathToEmulatorSharedLib("G:/libs/emulator.dll")
                .codeBoc(codeCell.toBase64())
                .dataBoc(dataCell.toBase64())
                .verbosityLevel(TvmVerbosityLevel.UNLIMITED)
                .build();

        String address = contract.getAddress().toBounceable();
        String randSeedHex = Utils.sha256("ABC");
        Cell configAll = tonlib.getConfigAll(128);

        assertTrue(tvmEmulator.setLibs(getLibs().toBase64()));

        // optionally set C7
//        assertTrue(tvmEmulator.setC7(address,
//                Instant.now().getEpochSecond(),
//                Utils.toNano(1).longValue(),
//                randSeedHex
////                , null
//                , configAll.toBase64()
//        ));

//        tvmEmulator.setGasLimit(Utils.toNano(10).longValue());
        tvmEmulator.setDebugEnabled(true);

        Cell body = CellBuilder.beginCell()
                .storeUint(0x706c7567, 32) // op request funds
                .endCell();

        String resultBoc = tvmEmulator.sendInternalMessage(body.toBase64(), Utils.toNano(0.11).longValue());
        log.info("resultBoc {}", resultBoc);
        SendExternalMessageResult result = gson.fromJson(resultBoc, SendExternalMessageResult.class);
        log.info("result sendInternalMessage, exitCode: {}", result.getVm_exit_code());

    }

    @Test
    public void testTvmEmulatorSendInternalMessage() {
        Cell body = CellBuilder.beginCell()
                .storeUint(0x706c7567, 32) // op request funds
                .endCell();

        assertTrue(tvmEmulator.setLibs(getLibs().toBase64()));
        tvmEmulator.setDebugEnabled(true);

        String resultBoc = tvmEmulator.sendInternalMessage(body.toBase64(), Utils.toNano(0.11).longValue());

        SendExternalMessageResult result = gson.fromJson(resultBoc, SendExternalMessageResult.class);
        log.info("result sendInternalMessage, exitCode: {}", result.getVm_exit_code());
    }

    @Test
    public void testTvmEmulatorSetPrevBlockInfo() {
//        assertTrue(tvmEmulator.setPrevBlockInfo());
    }


    private static Cell getLibs() {
        SmcLibraryResult result = tonlib.getLibraries(
                Collections.singletonList("wkUmK4wrzl6fzSPKM04dVfqW1M5pqigX3tcXzvy6P3M="));

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

    private Cell createTransferBody(WalletV4R2Config config) {

        CellBuilder message = CellBuilder.beginCell();

        message.storeUint(config.getWalletId(), 32);

        message.storeUint((config.getValidUntil() == 0) ? Instant.now().getEpochSecond() + 60 : config.getValidUntil(), 32);

        message.storeUint(config.getSeqno(), 32);// msg_seqno

        if (config.getOperation() == 0) {
            Cell order = Message.builder()
                    .info(InternalMessageInfo.builder()
                            .bounce(config.isBounce())
                            .dstAddr(MsgAddressIntStd.builder()
                                    .workchainId(config.getDestination().wc)
                                    .address(config.getDestination().toBigInteger())
                                    .build())
                            .value(CurrencyCollection.builder().coins(config.getAmount()).build())
                            .build())
                    .init(config.getStateInit())
                    .body((isNull(config.getBody()) && nonNull(config.getComment())) ?
                            CellBuilder.beginCell()
                                    .storeUint(0, 32)
                                    .storeString(config.getComment())
                                    .endCell()
                            : config.getBody())
                    .build().toCell();

            message.storeUint(BigInteger.ZERO, 8); // op simple send
            message.storeUint(config.getMode(), 8);
            message.storeRef(order);
        } else if (config.getOperation() == 1) {
            message.storeUint(1, 8); // deploy and install plugin
            message.storeUint(BigInteger.valueOf(config.getNewPlugin().getPluginWc()), 8);
            message.storeCoins(config.getNewPlugin().getAmount()); // plugin balance
            message.storeRef(config.getNewPlugin().getStateInit());
            message.storeRef(config.getNewPlugin().getBody());
        }
        if (config.getOperation() == 2) {
            message.storeUint(2, 8); // install plugin
            message.storeUint(BigInteger.valueOf(config.getDeployedPlugin().getPluginAddress().wc), 8);
            message.storeBytes(config.getDeployedPlugin().getPluginAddress().hashPart);
            message.storeCoins(BigInteger.valueOf(config.getDeployedPlugin().getAmount().longValue()));
            message.storeUint(BigInteger.valueOf(config.getDeployedPlugin().getQueryId()), 64);
            message.endCell();
        }
        if (config.getOperation() == 3) {
            message.storeUint(3, 8); // remove plugin
            message.storeUint(BigInteger.valueOf(config.getDeployedPlugin().getPluginAddress().wc), 8);
            message.storeBytes(config.getDeployedPlugin().getPluginAddress().hashPart);
            message.storeCoins(BigInteger.valueOf(config.getDeployedPlugin().getAmount().longValue()));
            message.storeUint(BigInteger.valueOf(config.getDeployedPlugin().getQueryId()), 64);
            message.endCell();
        }

        return message.endCell();
    }
}
