package org.ton.java.emulator;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.junit.Assert.*;
import static org.ton.java.smartcontract.wallet.v4.WalletV4R2.createPluginDataCell;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.ToNumberPolicy;
import com.iwebpp.crypto.TweetNaclFast;
import com.sun.jna.Native;
import java.io.IOException;
import java.math.BigInteger;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import lombok.extern.slf4j.Slf4j;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.ton.java.address.Address;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.CellSlice;
import org.ton.java.cell.TonHashMapE;
import org.ton.java.emulator.tvm.*;
import org.ton.java.smartcontract.GenericSmartContract;
import org.ton.java.smartcontract.SmartContractCompiler;
import org.ton.java.smartcontract.types.NewPlugin;
import org.ton.java.smartcontract.types.WalletV4R2Config;
import org.ton.java.smartcontract.wallet.v4.SubscriptionInfo;
import org.ton.java.smartcontract.wallet.v4.WalletV4R2;
import org.ton.java.tlb.types.*;
import org.ton.java.tonlib.Tonlib;
import org.ton.java.tonlib.types.BlockIdExt;
import org.ton.java.tonlib.types.SmcLibraryEntry;
import org.ton.java.tonlib.types.SmcLibraryResult;
import org.ton.java.utils.Utils;

@Slf4j
@RunWith(JUnit4.class)
public class TestTvmEmulator {

  static TvmEmulator tvmEmulator;
  static Tonlib tonlib;
  private static final Gson gson =
      new GsonBuilder().setObjectToNumberStrategy(ToNumberPolicy.BIG_DECIMAL).create();
  static WalletV4R2 walletV4R2;

  @BeforeClass
  public static void setUpBeforeClass() {
    byte[] secretKey =
        Utils.hexToSignedBytes("F182111193F30D79D517F2339A1BA7C25FDF6C52142F0F2C1D960A1F1D65E1E4");
    TweetNaclFast.Signature.KeyPair keyPair = TweetNaclFast.Signature.keyPair_fromSeed(secretKey);

    log.info("pubKey {}", Utils.bytesToHex(keyPair.getPublicKey()));
    log.info("prvKey {}", Utils.bytesToHex(keyPair.getSecretKey()));
    tonlib = Tonlib.builder()
            .pathToTonlibSharedLib("/home/neodix/gitProjects/ton-neodix/build/tonlib/libtonlibjson.so")
            .testnet(true).ignoreCache(false).build();

    walletV4R2 = WalletV4R2.builder().tonlib(tonlib).keyPair(keyPair).walletId(42).build();

    Address walletAddress = walletV4R2.getAddress();

    String bounceableAddress = walletAddress.toBounceable();
    log.info("bounceableAddress: {}", bounceableAddress);
    log.info("rawAddress: {}", walletAddress.toRaw());
    log.info("pub-key {}", Utils.bytesToHex(walletV4R2.getKeyPair().getPublicKey()));
    log.info("prv-key {}", Utils.bytesToHex(walletV4R2.getKeyPair().getSecretKey()));

    Cell code = walletV4R2.getStateInit().getCode();
    Cell data = walletV4R2.getStateInit().getData();

    tvmEmulator =
        TvmEmulator.builder()
            .pathToEmulatorSharedLib("/home/neodix/gitProjects/ton-neodix/build/emulator/libemulator.so")
            .codeBoc(code.toBase64())
            .dataBoc(data.toBase64())
            .verbosityLevel(TvmVerbosityLevel.UNLIMITED)
            .build();
    tvmEmulator.setDebugEnabled(true);
  }

  @Test
  public void testInitTvmEmulator() {
    //    TvmEmulatorI tvmEmulatorI = Native.load("emulator.dll", TvmEmulatorI.class);
    TvmEmulatorI tvmEmulatorI = Native.load("/home/neodix/gitProjects/ton-neodix/build/emulator/libemulator.so", TvmEmulatorI.class);
    long emulator =
        tvmEmulatorI.tvm_emulator_create(
            walletV4R2.getStateInit().getCode().toBase64(),
            walletV4R2.getStateInit().getData().toBase64(),
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

  /**
   * The “global variables” may be helpful in implementing some high-level smart-contract languages.
   * They are in fact stored as components of the Tuple at c7: the k-th global variable simply is
   * the k-th component of this Tuple, for 1 ≤ k ≤ 254. By convention, the 0-th component is used
   * for the “configuration parameters” of A.11.4, so it is not available as a global variable.
   *
   * <p>The pseudorandom number generator uses the random seed (parameter #6, cf. A.11.4), an
   * unsigned 256-bit Integer, and other data kept in c7.
   */
  @Test
  public void testTvmEmulatorSetC7() {
    String address = walletV4R2.getAddress().toBounceable();
    String randSeedHex = Utils.sha256("ABC");

    Cell config = tonlib.getConfigAll(128); // 128 - all config

    assertTrue(
        tvmEmulator.setC7(
            address,
            Instant.now().getEpochSecond(),
            Utils.toNano(1).longValue(), // smc balance
            randSeedHex,
            config.toBase64() // optional
            ));
  }

  @Test
  public void testTvmEmulatorEmulateRunMethod() {

    VmStack stack =
        VmStack.builder()
            .depth(0)
            .stack(VmStackList.builder().tos(Collections.emptyList()).build())
            .build();

    String paramsBocBase64 =
        CellBuilder.beginCell()
            .storeRef(walletV4R2.getStateInit().getCode())
            .storeRef(walletV4R2.getStateInit().getData())
            .storeRef(stack.toCell())
            .storeRef(
                CellBuilder.beginCell()
                    .storeRef(stack.toCell()) // c7 ^VmStack
//                    .storeRef(getLibs()) // libs ^Cell
                    .endCell())
            .storeUint(Utils.calculateMethodId("seqno"), 32) // method-id - seqno
            .endCell()
            .toBase64();

    Cell c = CellBuilder.beginCell().fromBocBase64(paramsBocBase64).endCell();
    log.info("cellPrint {}", c.print());
    long gasLimit = Utils.toNano(1).longValue();
    String result =
        tvmEmulator.emulateRunMethod(
            paramsBocBase64.length(), paramsBocBase64, gasLimit); // todo why null
    log.info("result emulateRunMethod: {}", result);
  }

  @Test
  public void testTvmEmulatorRunGetMethodGetSeqNo() {
    GetMethodResult methodResult = tvmEmulator.runGetMethod(Utils.calculateMethodId("seqno"));
    log.info("result runGetMethod: {}", methodResult);

    log.info("methodResult stack: {}", methodResult.getStack());

    //    Cell cellResult =
    // CellBuilder.beginCell().fromBocBase64(methodResult.getStack()).endCell();
    //        log.info("cellResult {}", cellResult);
    VmStack stack = methodResult.getStack();
    int depth = stack.getDepth();
    log.info("vmStack depth: {}", depth);
    VmStackList vmStackList = stack.getStack();
    log.info("vmStackList: {}", vmStackList.getTos());
    BigInteger seqno =
        VmStackValueTinyInt.deserialize(CellSlice.beginParse(vmStackList.getTos().get(0).toCell()))
            .getValue();
    log.info("seqno value: {}", seqno);
  }

  @Test
  public void testTvmEmulatorRunGetMethodGetSeqNoShortVersion() {
    log.info("seqno value: {}", tvmEmulator.runGetSeqNo());
  }

  @Test
  public void testTvmEmulatorRunGetMethodGetPubKey() {
    GetMethodResult methodResult =
        tvmEmulator.runGetMethod(
            Utils.calculateMethodId("get_public_key"),
            VmStack.builder()
                .depth(0)
                .stack(VmStackList.builder().tos(Collections.emptyList()).build())
                .build()
                .toCell()
                .toBase64());

    log.info("methodResult: {}", methodResult);
    log.info("methodResult stack: {}", methodResult.getStack());

    //    Cell cellResult =
    // CellBuilder.beginCell().fromBocBase64(methodResult.getStack()).endCell();
    //    log.info("cellResult {}", cellResult);
    VmStack stack = methodResult.getStack();
    int depth = stack.getDepth();
    log.info("vmStack depth: {}", depth);
    VmStackList vmStackList = stack.getStack();
    log.info("vmStackList: {}", vmStackList.getTos()); // ok
    VmStackValue stackValue =
        VmStackValue.deserialize(CellSlice.beginParse(vmStackList.getTos().get(0).toCell()));
    log.info("stackValue value: {}", stackValue);
    BigInteger pubKey =
        VmStackValueInt.deserialize(CellSlice.beginParse(stackValue.toCell())).getValue();
    log.info("vmStackList value: {}", pubKey.toString(16)); // pubkey
  }

  @Test
  public void testTvmEmulatorRunGetMethodGetPubKeyShortVersion() {
    log.info("contract's pubKey: {}", tvmEmulator.runGetPublicKey()); // pubkey
  }

  @Test
  public void testTvmEmulatorRunGetMethodGetPluginList() {
    String stackSerialized =
        VmStack.builder()
            .depth(0)
            .stack(VmStackList.builder().tos(Collections.emptyList()).build())
            .build()
            .toCell()
            .toBase64();

    GetMethodResult methodResult =
        tvmEmulator.runGetMethod(Utils.calculateMethodId("get_plugin_list"), stackSerialized);
    log.info("methodResult: {}", methodResult);
    log.info("methodResult stack: {}", methodResult.getStack());

    Cell cellResult = methodResult.getStack().toCell();
    log.info("cellResult {}", cellResult.print());
    VmStackValueTuple tuple = VmStackValueTuple.deserialize(CellSlice.beginParse(cellResult));
    log.info("tuple {}", tuple);
  }

  @Test
  public void testTvmEmulatorRunGetMethodIsPluginInstalled() {
    String stackSerialized =
        VmStack.builder()
            .depth(0)
            .stack(
                VmStackList.builder()
                    .tos(
                        Arrays.asList(
                            VmStackValueTinyInt.builder().value(BigInteger.ZERO).build(),
                            VmStackValueTinyInt.builder().value(BigInteger.ZERO).build()))
                    .build())
            .build()
            .toCell()
            .toBase64();

    GetMethodResult methodResult =
        tvmEmulator.runGetMethod(Utils.calculateMethodId("is_plugin_installed"), stackSerialized);
    log.info("methodResult: {}", methodResult);
    log.info("methodResult stack: {}", methodResult.getStack());

    Cell cellResult = methodResult.getStack().toCell();
    log.info("cellResult {}", cellResult.print());
    VmStackValueTuple tuple = VmStackValueTuple.deserialize(CellSlice.beginParse(cellResult));
    log.info("tuple {}", tuple);
  }

  @Test
  public void testTvmEmulatorSendExternalMessage() {

    String address = walletV4R2.getAddress().toBounceable();
    String randSeedHex = Utils.sha256("ABC");
    //        Cell configAll = tonlib.getConfigAll(128);

    // optionally set C7
    assertTrue(
        tvmEmulator.setC7(
            address, Instant.now().getEpochSecond(), Utils.toNano(1).longValue(), randSeedHex, null
            //                , configAll.toBase64()
            ));

    WalletV4R2Config config =
        WalletV4R2Config.builder()
            .operation(0)
            .walletId(42)
            .seqno(0)
            .destination(
                Address.of("0:258e549638a6980ae5d3c76382afd3f4f32e34482dafc3751e3358589c8de00d"))
            .amount(Utils.toNano(0.124))
            .build();

    //        assertTrue(tvmEmulator.setLibs(getLibs().toBase64()));

    Message msg = walletV4R2.prepareExternalMsg(config);
    SendExternalMessageResult result = tvmEmulator.sendExternalMessage(msg.getBody().toBase64());

    log.info("result sendExternalMessage, {}", result);
    //        log.info("result sendExternalMessage, actions: {}", result.getActions());
    log.info("seqno value: {}", tvmEmulator.runGetSeqNo());
    OutList actions = result.getActions();
    log.info("compute phase actions {}", actions);
    log.info("new code cell {}", result.getNewCodeCell().print());
    log.info("new data cell {}", result.getNewDataCell().print());

    assertEquals(1, tvmEmulator.runGetSeqNo().longValue());

    // send one more time
    config =
        WalletV4R2Config.builder()
            .operation(0)
            .walletId(42)
            .seqno(1)
            .destination(
                Address.of("0:258e549638a6980ae5d3c76382afd3f4f32e34482dafc3751e3358589c8de00d"))
            .amount(Utils.toNano(0.123))
            .build();

    msg = walletV4R2.prepareExternalMsg(config);
    tvmEmulator.sendExternalMessage(msg.getBody().toBase64());

    assertEquals(2, tvmEmulator.runGetSeqNo().longValue());
  }

  @Test
  public void testTvmEmulatorSendExternalMessageCustom() throws IOException {

    SmartContractCompiler smcFunc =
        SmartContractCompiler.builder()
            .contractPath("G:/smartcontracts/new-wallet-v4r2.fc")
            .build();

    String codeCellHex = smcFunc.compile();
    Cell codeCell = CellBuilder.beginCell().fromBoc(codeCellHex).endCell();

    byte[] publicKey =
        Utils.hexToSignedBytes("82A0B2543D06FEC0AAC952E9EC738BE56AB1B6027FC0C1AA817AE14B4D1ED2FB");
    byte[] secretKey =
        Utils.hexToSignedBytes("F182111193F30D79D517F2339A1BA7C25FDF6C52142F0F2C1D960A1F1D65E1E4");
    TweetNaclFast.Signature.KeyPair keyPair = TweetNaclFast.Signature.keyPair_fromSeed(secretKey);

    Cell dataCell =
        CellBuilder.beginCell()
            .storeUint(0, 32) // seqno
            .storeUint(42, 32) // wallet id
            .storeBytes(keyPair.getPublicKey())
            .storeBit(false) // plugins dict empty
            .endCell();

    log.info("codeCellHex {}", codeCellHex);
    log.info("dataCellHex {}", dataCell.toHex());

    Address address = StateInit.builder().code(codeCell).data(dataCell).build().getAddress();
    log.info("addressRaw {}", address.toRaw());
    log.info("addressBounceable {}", address.toBounceable());

    tvmEmulator =
        TvmEmulator.builder()
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

    GenericSmartContract smc =
        GenericSmartContract.builder()
            .tonlib(tonlib)
            .keyPair(keyPair)
            .code(codeCellHex)
            .data(dataCell.toHex())
            .build();

    WalletV4R2Config config =
        WalletV4R2Config.builder()
            .operation(0)
            .walletId(42)
            .seqno(0)
            .destination(
                Address.of("0:258e549638a6980ae5d3c76382afd3f4f32e34482dafc3751e3358589c8de00d"))
            .amount(Utils.toNano(0.331))
            .build();

    //        assertTrue(tvmEmulator.setLibs(getLibs().toBase64()));

    Cell transferBody = createTransferBody(config);

    Cell signedBody =
        CellBuilder.beginCell()
            .storeBytes(
                Utils.signData(keyPair.getPublicKey(), keyPair.getSecretKey(), transferBody.hash()))
            .storeCell(transferBody)
            .endCell();
    log.info("extMsg {}", signedBody.toHex());

    SendExternalMessageResult resultBoc = tvmEmulator.sendExternalMessage(signedBody.toBase64());

    log.info("result sendExternalMessage, {}", resultBoc);

    config =
        WalletV4R2Config.builder()
            .operation(0)
            .walletId(42)
            .seqno(1) // second transfer with seqno 0 fails with error code 33 - as expected
            .destination(
                Address.of("0:258e549638a6980ae5d3c76382afd3f4f32e34482dafc3751e3358589c8de00d"))
            .amount(Utils.toNano(0.332))
            .build();

    //    assertTrue(tvmEmulator.setLibs(getLibs().toBase64()));

    transferBody = createTransferBody(config);
    signedBody =
        CellBuilder.beginCell()
            .storeBytes(
                Utils.signData(keyPair.getPublicKey(), keyPair.getSecretKey(), transferBody.hash()))
            .storeCell(transferBody)
            .endCell();
    log.info("extMsg {}", signedBody.toHex());

    SendExternalMessageResult result = tvmEmulator.sendExternalMessage(signedBody.toBase64());

    log.info("result sendExternalMessage, {}", result);

    // is plugin installed - answer no
    Address beneficiaryAddress = Address.of("kf_sPxv06KagKaRmOOKxeDQwApCx3i8IQOwv507XD51JOLka");
    log.info("beneficiaryAddress: {}", beneficiaryAddress.toBounceable());
    log.info("beneficiaryAddress (raw): {}", beneficiaryAddress.toRaw());

    //        BigInteger addr = new
    // BigInteger("106857336580611253476560029380260470526545417460249276654115413311849265711416");
    //        log.info("ha {}", addr);
    //        log.info("ha {}", beneficiaryAddress.toBigInteger());
    String stackSerialized =
        VmStack.builder()
            .depth(2)
            .stack(
                VmStackList.builder()
                    .tos(
                        Arrays.asList(
                            VmStackValueTinyInt.builder().value(BigInteger.ZERO).build(),
                            VmStackValueInt.builder().value(address.toBigInteger()).build()))
                    .build())
            .build()
            .toCell()
            .toBase64();

    // is_plugin_installed
    GetMethodResult methodResult =
        tvmEmulator.runGetMethod(Utils.calculateMethodId("is_plugin_installed"), stackSerialized);
    log.info("methodResult stack: {}", methodResult.getStack());

    //    Cell cellResult =
    // CellBuilder.beginCell().fromBocBase64(methodResult.getStack()).endCell();
    //    log.info("cellResult {}", cellResult.print());
    VmStack stack = methodResult.getStack();
    int depth = stack.getDepth();
    log.info("vmStack depth: {}", depth);
    VmStackList vmStackList = stack.getStack();
    log.info("vmStackList: {}", vmStackList.getTos()); // should be empty

    // get plugin list - first time - result - empty
    stackSerialized =
        VmStack.builder()
            .depth(0)
            .stack(VmStackList.builder().tos(Collections.emptyList()).build())
            .build()
            .toCell()
            .toBase64();

    methodResult =
        tvmEmulator.runGetMethod(Utils.calculateMethodId("get_plugin_list"), stackSerialized);
    log.info("methodResult: {}", methodResult);
    log.info("methodResult stack: {}", methodResult.getStack());

    Cell cellResult = methodResult.getStack().toCell();
    log.info("cellResult {}", cellResult.print());
    VmStackValueTuple tuple = VmStackValueTuple.deserialize(CellSlice.beginParse(cellResult));
    log.info("get_plugin_list first result tuple {}", tuple); // no plugins yet, should be empty

    // install plugin
    log.info("installing plugin");
    SubscriptionInfo subscriptionInfo =
        SubscriptionInfo.builder()
            .beneficiary(beneficiaryAddress)
            .subscriptionFee(Utils.toNano(0.11))
            .period(60)
            .startTime(0)
            .timeOut(30)
            .lastPaymentTime(0)
            .lastRequestTime(0)
            .failedAttempts(0)
            .subscriptionId(12345)
            .build();

    // log.info("beneficiaryWallet balance {}",
    // Utils.formatNanoValue(tonlib.getAccountBalance(beneficiaryAddress)));

    StateInit pluginStateInit =
        StateInit.builder()
            .code(
                CellBuilder.beginCell()
                    .fromBoc(
                        "B5EE9C7241020F01000262000114FF00F4A413F4BCF2C80B0102012002030201480405036AF230DB3C5335A127A904F82327A128A90401BC5135A0F823B913B0F29EF800725210BE945387F0078E855386DB3CA4E2F82302DB3C0B0C0D0202CD06070121A0D0C9B67813F488DE0411F488DE0410130B048FD6D9E05E8698198FD201829846382C74E2F841999E98F9841083239BA395D497803F018B841083AB735BBED9E702984E382D9C74688462F863841083AB735BBED9E70156BA4E09040B0A0A080269F10FD22184093886D9E7C12C1083239BA39384008646582A803678B2801FD010A65B5658F89659FE4B9FD803FC1083239BA396D9E40E0A04F08E8D108C5F0C708210756E6B77DB3CE00AD31F308210706C7567831EB15210BA8F48305324A126A904F82326A127A904BEF27109FA4430A619F833D078D721D70B3F5260A11BBE8E923036F82370708210737562732759DB3C5077DE106910581047103645135042DB3CE0395F076C2232821064737472BA0A0A0D09011A8E897F821064737472DB3CE0300A006821B39982100400000072FB02DE70F8276F118010C8CB055005CF1621FA0214F40013CB6912CB1F830602948100A032DEC901FB000030ED44D0FA40FA40FA00D31FD31FD31FD31FD31FD307D31F30018021FA443020813A98DB3C01A619F833D078D721D70B3FA070F8258210706C7567228018C8CB055007CF165004FA0215CB6A12CB1F13CB3F01FA02CB00C973FB000E0040C8500ACF165008CF165006FA0214CB1F12CB1FCB1FCB1FCB1FCB07CB1FC9ED54005801A615F833D020D70B078100D1BA95810088D721DED307218100DDBA028100DEBA12B1F2E047D33F30A8AB0FE5855AB4")
                    .endCell())
            .data(
                createPluginDataCell(
                    address,
                    subscriptionInfo.getBeneficiary(),
                    subscriptionInfo.getSubscriptionFee(),
                    subscriptionInfo.getPeriod(),
                    subscriptionInfo.getStartTime(),
                    subscriptionInfo.getTimeOut(),
                    subscriptionInfo.getLastPaymentTime(),
                    subscriptionInfo.getLastRequestTime(),
                    subscriptionInfo.getFailedAttempts(),
                    subscriptionInfo.getSubscriptionId()))
            .build();

    log.info("plugin address Bounceable: {}", pluginStateInit.getAddress().toBounceable());
    log.info("plugin address Raw: {}", pluginStateInit.getAddress().toRaw());

    config =
        WalletV4R2Config.builder()
            .seqno(2)
            .operation(1) // deploy and install plugin
            .walletId(42)
            .newPlugin(
                NewPlugin.builder()
                    .secretKey(keyPair.getSecretKey())
                    .seqno(2)
                    .pluginWc(address.wc) // reuse wc of the wallet
                    .amount(
                        Utils.toNano(0.1)) // initial plugin balance, will be taken from wallet-v4
                    .stateInit(pluginStateInit.toCell())
                    .body(
                        CellBuilder.beginCell()
                            .storeUint(
                                new BigInteger("706c7567", 16).add(new BigInteger("80000000", 16)),
                                32) // OP
                            .endCell())
                    .build())
            .build();

    transferBody = createTransferBody(config);
    signedBody =
        CellBuilder.beginCell()
            .storeBytes(
                Utils.signData(keyPair.getPublicKey(), keyPair.getSecretKey(), transferBody.hash()))
            .storeCell(transferBody)
            .endCell();
    log.info("install plugin extMsg {}", signedBody.toHex());

    result = tvmEmulator.sendExternalMessage(signedBody.toBase64());
    log.info("result deploy plugin sendExternalMessage, {}", result);

    // get plugin list - second time - result one entry
    stackSerialized =
        VmStack.builder()
            .depth(0)
            .stack(VmStackList.builder().tos(Collections.emptyList()).build())
            .build()
            .toCell()
            .toBase64();

    methodResult =
        tvmEmulator.runGetMethod(Utils.calculateMethodId("get_plugin_list"), stackSerialized);

    log.info("get_plugin_list methodResult: {}", methodResult);
    log.info("get_plugin_list methodResult stack: {}", methodResult.getStack());

    //    cellResult = CellBuilder.beginCell().fromBocBase64(methodResult.getStack()).endCell();
    //    log.info("get_plugin_list second cellResult {}", cellResult.print());
    stack = methodResult.getStack();
    depth = stack.getDepth();
    log.info("vmStack depth: {}", depth);
    vmStackList = stack.getStack();
    log.info(
        "get_plugin_list second result vmStackList: {}",
        vmStackList.getTos()); // should be one entry with plugin address
    // plugin with address
    // 38034472829642612572964913615375954737329097997866829358853605638742012097504
    // extract plugin addresses

    // is_plugin_installed - with parameters [] - answer yes

    stackSerialized =
        VmStack.builder()
            .depth(2)
            .stack(
                VmStackList.builder()
                    .tos(
                        Arrays.asList(
                            VmStackValueInt.builder().value(BigInteger.ZERO).build(),
                            VmStackValueInt.builder()
                                .value(
                                    new BigInteger(
                                        "38034472829642612572964913615375954737329097997866829358853605638742012097504"))
                                .build()))
                    .build())
            .build()
            .toCell()
            .toBase64();

    log.info("is installed ????");
    methodResult =
        tvmEmulator.runGetMethod(Utils.calculateMethodId("is_plugin_installed"), stackSerialized);

    log.info("methodResult stack: {}", methodResult.getStack());

    stack = methodResult.getStack();
    depth = stack.getDepth();
    log.info("vmStack depth: {}", depth);
    vmStackList = stack.getStack();
    log.info("vmStackList: {}", vmStackList.getTos());
  }

  @Test
  public void testTvmEmulatorSendInternalMessageCustomContract() throws IOException {
    SmartContractCompiler smcFunc =
        SmartContractCompiler.builder()
            .contractPath("G:/smartcontracts/new-wallet-v4r2.fc")
            .build();

    String codeCellHex = smcFunc.compile();
    Cell codeCell = CellBuilder.beginCell().fromBoc(codeCellHex).endCell();

    TweetNaclFast.Signature.KeyPair keyPair = Utils.generateSignatureKeyPair();

    Cell dataCell =
        CellBuilder.beginCell()
            .storeUint(0, 32) // seqno
            .storeUint(42, 32) // wallet id
            .storeBytes(keyPair.getPublicKey())
            .storeUint(0, 1) // plugins dict empty
            .endCell();

    log.info("codeCellHex {}", codeCellHex);
    log.info("dataCellHex {}", dataCell.toHex());

    tvmEmulator =
        TvmEmulator.builder()
            .pathToEmulatorSharedLib("/home/neodix/gitProjects/ton-neodix/build/emulator/libemulator.so")
            .codeBoc(codeCell.toBase64())
            .dataBoc(dataCell.toBase64())
            .verbosityLevel(TvmVerbosityLevel.UNLIMITED)
            .build();

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

    Cell body =
        CellBuilder.beginCell()
            .storeUint(0x706c7567, 32) // op request funds
            .endCell();

    SendInternalMessageResult result =
        tvmEmulator.sendInternalMessage(body.toBase64(), Utils.toNano(0.11).longValue());

    log.info("result sendInternalMessage, {}", result);
  }

  @Test
  public void testTvmEmulatorSendInternalMessage() {
    Cell body =
        CellBuilder.beginCell()
            .storeUint(0x706c7567, 32) // op request funds
            .endCell();

    tvmEmulator.setDebugEnabled(true);

    SendInternalMessageResult result =
        tvmEmulator.sendInternalMessage(body.toBase64(), Utils.toNano(0.11).longValue());

    log.info("result sendInternalMessage, {}", result);

    OutList actions = result.getActions();
    log.info("compute phase actions {}", actions);
    log.info("new code cell {}", result.getNewCodeCell().print());
    log.info("new data cell {}", result.getNewDataCell().print());
  }

  @Test
  public void testTvmEmulatorSetPrevBlockInfo() {
    BlockIdExt lastBlock = tonlib.getLast().getLast();
    log.info("lastBlockId: {}", lastBlock);

    ArrayList<VmStackValue> stack = new ArrayList<>();
    stack.add(
        VmStackValueTinyInt.builder().value(BigInteger.valueOf(lastBlock.getWorkchain())).build());
    stack.add(
        VmStackValueTinyInt.builder().value(BigInteger.valueOf(lastBlock.getShard())).build());
    stack.add(
        VmStackValueTinyInt.builder().value(BigInteger.valueOf(lastBlock.getSeqno())).build());
    stack.add(
        VmStackValueInt.builder()
            .value(new BigInteger(Utils.base64ToHexString(lastBlock.getRoot_hash()), 16))
            .build());
    stack.add(
        VmStackValueInt.builder()
            .value(new BigInteger(Utils.base64ToHexString(lastBlock.getFile_hash()), 16))
            .build());

    VmStackValueTuple vmStackValueTuple =
        VmStackValueTuple.builder().data(VmTuple.builder().values(stack).build()).build();

    Cell deserializedTuple =
        CellBuilder.beginCell().fromBocBase64(vmStackValueTuple.toCell().toBase64()).endCell();
    log.info("test deserialization: {}", deserializedTuple.print());

    assertTrue(tvmEmulator.setPrevBlockInfo(vmStackValueTuple.toCell().toBase64()));
  }

  private static Cell getLibs() {
    SmcLibraryResult result =
        tonlib.getLibraries(
            Collections.singletonList("wkUmK4wrzl6fzSPKM04dVfqW1M5pqigX3tcXzvy6P3M="));

    TonHashMapE x = new TonHashMapE(256);

    for (SmcLibraryEntry l : result.getResult()) {
      String cellLibBoc = l.getData();
      Cell lib = Cell.fromBocBase64(cellLibBoc);
      //      log.info("cell lib {}", lib.toHex());
      x.elements.put(1L, lib);
    }

    return x.serialize(
        k -> CellBuilder.beginCell().storeUint((Long) k, 256).endCell().getBits(),
        v -> CellBuilder.beginCell().storeRef((Cell) v).endCell());
  }

  private Cell createTransferBody(WalletV4R2Config config) {

    CellBuilder message = CellBuilder.beginCell();

    message.storeUint(config.getWalletId(), 32);

    message.storeUint(
        (config.getValidUntil() == 0)
            ? Instant.now().getEpochSecond() + 60
            : config.getValidUntil(),
        32);

    message.storeUint(config.getSeqno(), 32); // msg_seqno

    if (config.getOperation() == 0) {
      Cell order =
          Message.builder()
              .info(
                  InternalMessageInfo.builder()
                      .bounce(config.isBounce())
                      .dstAddr(
                          MsgAddressIntStd.builder()
                              .workchainId(config.getDestination().wc)
                              .address(config.getDestination().toBigInteger())
                              .build())
                      .value(CurrencyCollection.builder().coins(config.getAmount()).build())
                      .build())
              .init(config.getStateInit())
              .body(
                  (isNull(config.getBody()) && nonNull(config.getComment()))
                      ? CellBuilder.beginCell()
                          .storeUint(0, 32)
                          .storeString(config.getComment())
                          .endCell()
                      : config.getBody())
              .build()
              .toCell();

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

  @Test
  public void testTvmEmulatorParsePluginListResultWithOneEntry() {

    String bocBase64 =
        "te6cckEBBgEARgADDAAAAQcAAgECAwAAAgYHAAIEBQACAAASAQAAAAAAAAAAAEQCAFQWv62UIb68RxRkPHIBa4xGgl2dhv3r6zAHZjygzHPgkqH6lg";
    Cell cell = CellBuilder.beginCell().fromBocBase64(bocBase64).endCell();
    log.info("cell print: {}", cell.print());
    log.info("cell boc: {}", cell.toHex());

    VmStack stack = VmStack.deserialize(CellSlice.beginParse(cell));
    log.info("vmStack depth: {}", stack.getDepth());
    VmStackList vmStackList = stack.getStack();
    log.info("vmStackList: {}", vmStackList.getTos());
  }
}
