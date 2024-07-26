package org.ton.java.smartcontract;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.ToNumberPolicy;
import com.iwebpp.crypto.TweetNaclFast;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.ton.java.address.Address;
import org.ton.java.cell.CellBuilder;
import org.ton.java.smartcontract.token.ft.JettonMinterStableCoin;
import org.ton.java.smartcontract.token.ft.JettonWalletStableCoin;
import org.ton.java.smartcontract.types.WalletCodes;
import org.ton.java.smartcontract.types.WalletV3Config;
import org.ton.java.smartcontract.types.WalletV4R2Config;
import org.ton.java.smartcontract.utils.MsgUtils;
import org.ton.java.smartcontract.wallet.v3.WalletV3R2;
import org.ton.java.smartcontract.wallet.v4.WalletV4R2;
import org.ton.java.tlb.types.Message;
import org.ton.java.tonlib.Tonlib;
import org.ton.java.tonlib.types.ExtMessageInfo;
import org.ton.java.utils.Utils;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URL;
import java.nio.charset.Charset;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@RunWith(JUnit4.class)
public class TestTonSdkTestCasesSmartContracts {
    public static final String numbersTestFileUrl = "https://raw.githubusercontent.com/neodix42/ton-sdk-test-cases/main/smartcontracts.json";
    Gson gson = new GsonBuilder().setObjectToNumberStrategy(ToNumberPolicy.LONG_OR_DOUBLE).create();
    String fileContentWithUseCases = IOUtils.toString(new URL(numbersTestFileUrl), Charset.defaultCharset());
    TonSdkTestCases tonSdkTestCases = gson.fromJson(fileContentWithUseCases, TonSdkTestCases.class);

    public TestTonSdkTestCasesSmartContracts() throws IOException {
    }

    @Test
    public void testSmartContracts1() {

        String testId = "smartcontracts-1";
        TonSdkTestCases.TestCase testCase = tonSdkTestCases.getTestCases().get(testId);

        String description = testCase.getDescription();

        log.info("testId: {}", testId);
        log.info("description: {}", description);

        String bocAsHex = testCase.getExpectedOutput().get("bocAsHexWithCrc").toString();

        assertThat(WalletCodes.V3R2.getValue()).isEqualTo(bocAsHex);
        assertThat(WalletV3R2.builder().build().createCodeCell().toHex().toUpperCase()).isEqualTo(bocAsHex);
    }

    @Test
    public void testSmartContracts2() {

        String testId = "smartcontracts-2";
        TonSdkTestCases.TestCase testCase = tonSdkTestCases.getTestCases().get(testId);

        String description = testCase.getDescription();

        log.info("testId: {}", testId);
        log.info("description: {}", description);

        String privateKey = testCase.getInput().get("privateKey").toString();
        Long workchain = (Long) testCase.getInput().get("workchain");
        Long walletId = (Long) testCase.getInput().get("walletId");

        byte[] secretKey = Utils.hexToSignedBytes(privateKey);
        TweetNaclFast.Signature.KeyPair keyPair = TweetNaclFast.Signature.keyPair_fromSeed(secretKey);

        WalletV3R2 contract = WalletV3R2.builder()
                .wc(workchain)
                .keyPair(keyPair)
                .walletId(walletId)
                .build();

        String expectedRawAddress = (String) testCase.getExpectedOutput().get("rawAddress");
        String expectedBocAsHex = (String) testCase.getExpectedOutput().get("externalMessageBocAsHexWithCrc");

        String rawAddress = contract.getAddress().toRaw();
        assertThat(rawAddress).isEqualTo(expectedRawAddress);

        Message msg = contract.prepareDeployMsg();
        assertThat(msg.toCell().toHex(true).toUpperCase()).isEqualTo(expectedBocAsHex);
    }


    @Test
    public void testSmartContracts3() {

        String testId = "smartcontracts-3";
        TonSdkTestCases.TestCase testCase = tonSdkTestCases.getTestCases().get(testId);

        String description = testCase.getDescription();

        log.info("testId: {}", testId);
        log.info("description: {}", description);

        String privateKey = testCase.getInput().get("privateKey").toString();
        Long workchain = (Long) testCase.getInput().get("workchain");
        Long walletId = (Long) testCase.getInput().get("walletId");

        byte[] secretKey = Utils.hexToSignedBytes(privateKey);
        TweetNaclFast.Signature.KeyPair keyPair = TweetNaclFast.Signature.keyPair_fromSeed(secretKey);

        WalletV3R2 contract = WalletV3R2.builder()
                .wc(workchain)
                .keyPair(keyPair)
                .walletId(walletId)
                .build();

        String expectedRawAddress = (String) testCase.getExpectedOutput().get("rawAddress");
        String expectedCodeAsHex = (String) testCase.getExpectedOutput().get("codeAsHex");
        String expectedDataAsHex = (String) testCase.getExpectedOutput().get("dataAsHex");
        String expectedExternalMessageAsHex = (String) testCase.getExpectedOutput().get("externalMessageAsHex");
        String expectedBocAsHex = (String) testCase.getExpectedOutput().get("externalMessageBocAsHexWithCrc");

        String rawAddress = contract.getAddress().toRaw();
        assertThat(rawAddress).isEqualTo(expectedRawAddress);

        assertThat(contract.createCodeCell().bitStringToHex().toUpperCase()).isEqualTo(expectedCodeAsHex);
        assertThat(contract.createDataCell().bitStringToHex().toUpperCase()).isEqualTo(expectedDataAsHex);
        Message msg = contract.prepareDeployMsg();

        assertThat(msg.toCell().bitStringToHex().toUpperCase()).isEqualTo(expectedExternalMessageAsHex);
        assertThat(msg.toCell().toHex(true).toUpperCase()).isEqualTo(expectedBocAsHex);
    }

    @Test
    public void testSmartContracts4() {

        String testId = "smartcontracts-4";
        TonSdkTestCases.TestCase testCase = tonSdkTestCases.getTestCases().get(testId);

        String description = testCase.getDescription();

        log.info("testId: {}", testId);
        log.info("description: {}", description);

        String privateKey = testCase.getInput().get("privateKey").toString();
        Long workchain = (Long) testCase.getInput().get("workchain");
        String destinationAddress = testCase.getInput().get("destinationAddress").toString();
        Long walletId = (Long) testCase.getInput().get("walletId");
        Long seqno = (Long) testCase.getInput().get("seqNo");
        BigDecimal amountTonCoins = new BigDecimal(testCase.getInput().get("amountTonCoins").toString());
        Boolean bounceFlag = (Boolean) testCase.getInput().get("bounceFlag");
        Long validUntil = (Long) testCase.getInput().get("validUntil");
        Long sendMode = (Long) testCase.getInput().get("sendMode");

        byte[] secretKey = Utils.hexToSignedBytes(privateKey);
        TweetNaclFast.Signature.KeyPair keyPair = TweetNaclFast.Signature.keyPair_fromSeed(secretKey);

        WalletV3R2 contract = WalletV3R2.builder()
                .wc(workchain)
                .keyPair(keyPair)
                .walletId(walletId)
                .build();

        String expectedBocAsHex = (String) testCase.getExpectedOutput().get("externalMessageBocAsHexWithCrc");

        WalletV3Config config = WalletV3Config.builder()
                .walletId(walletId)
                .seqno(seqno)
                .destination(Address.of(destinationAddress))
                .amount(Utils.toNano(amountTonCoins))
                .validUntil(validUntil)
                .bounce(bounceFlag)
                .mode(sendMode.intValue())
                .build();
        Message sendMsg = contract.prepareExternalMsg(config);
        assertThat(sendMsg.toCell().toHex(true).toUpperCase()).isEqualTo(expectedBocAsHex);
    }

    @Test
    public void testSmartContracts5() {

        String testId = "smartcontracts-5";
        TonSdkTestCases.TestCase testCase = tonSdkTestCases.getTestCases().get(testId);

        String description = testCase.getDescription();

        log.info("testId: {}", testId);
        log.info("description: {}", description);

        String privateKey = testCase.getInput().get("privateKey").toString();
        Long workchain = (Long) testCase.getInput().get("workchain");
        String destinationAddress = testCase.getInput().get("destinationAddress").toString();
        Long walletId = (Long) testCase.getInput().get("walletId");
        Long seqno = (Long) testCase.getInput().get("seqNo");
        BigDecimal amountTonCoins = new BigDecimal(testCase.getInput().get("amountTonCoins").toString());
        Boolean bounceFlag = (Boolean) testCase.getInput().get("bounceFlag");
        Long validUntil = (Long) testCase.getInput().get("validUntil");
        Long sendMode = (Long) testCase.getInput().get("sendMode");

        byte[] secretKey = Utils.hexToSignedBytes(privateKey);
        TweetNaclFast.Signature.KeyPair keyPair = TweetNaclFast.Signature.keyPair_fromSeed(secretKey);

        WalletV3R2 contract = WalletV3R2.builder()
                .wc(workchain)
                .keyPair(keyPair)
                .walletId(walletId)
                .build();

        String expectedBocAsHex = (String) testCase.getExpectedOutput().get("externalMessageBocAsHexWithCrc");

        WalletV3Config config = WalletV3Config.builder()
                .walletId(walletId)
                .seqno(seqno)
                .destination(Address.of(destinationAddress))
                .amount(Utils.toNano(amountTonCoins))
                .validUntil(validUntil)
                .bounce(bounceFlag)
                .mode(sendMode.intValue())
                .build();
        Message sendMsg = contract.prepareExternalMsg(config);
        assertThat(sendMsg.toCell().toHex(true).toUpperCase()).isEqualTo(expectedBocAsHex);
    }

    @Test
    public void testSmartContracts6() {

        String testId = "smartcontracts-6";
        TonSdkTestCases.TestCase testCase = tonSdkTestCases.getTestCases().get(testId);

        String description = testCase.getDescription();

        log.info("testId: {}", testId);
        log.info("description: {}", description);

        String privateKey = testCase.getInput().get("privateKey").toString();
        Long workchain = (Long) testCase.getInput().get("workchain");
        String destinationAddress = testCase.getInput().get("destinationAddress").toString();
        Long walletId = (Long) testCase.getInput().get("walletId");
        Long seqno = (Long) testCase.getInput().get("seqNo");
        BigDecimal amountTonCoins = new BigDecimal(testCase.getInput().get("amountTonCoins").toString());
        Boolean bounceFlag = (Boolean) testCase.getInput().get("bounceFlag");
        Long validUntil = (Long) testCase.getInput().get("validUntil");
        Long sendMode = (Long) testCase.getInput().get("sendMode");
        String comment = testCase.getInput().get("comment").toString();

        byte[] secretKey = Utils.hexToSignedBytes(privateKey);
        TweetNaclFast.Signature.KeyPair keyPair = TweetNaclFast.Signature.keyPair_fromSeed(secretKey);

        WalletV3R2 contract = WalletV3R2.builder()
                .wc(workchain)
                .keyPair(keyPair)
                .walletId(walletId)
                .build();

        String expectedBocAsHex = (String) testCase.getExpectedOutput().get("externalMessageBocAsHexWithCrc");

        WalletV3Config config = WalletV3Config.builder()
                .walletId(walletId)
                .seqno(seqno)
                .destination(Address.of(destinationAddress))
                .amount(Utils.toNano(amountTonCoins))
                .validUntil(validUntil)
                .bounce(bounceFlag)
                .mode(sendMode.intValue())
                .comment(comment)
                .build();
        Message sendMsg = contract.prepareExternalMsg(config);
        assertThat(sendMsg.toCell().toHex(true).toUpperCase()).isEqualTo(expectedBocAsHex);
    }


    @Test
    public void testSmartContracts7() {

        String testId = "smartcontracts-7";
        TonSdkTestCases.TestCase testCase = tonSdkTestCases.getTestCases().get(testId);

        String description = testCase.getDescription();

        log.info("testId: {}", testId);
        log.info("description: {}", description);

        String bocAsHex = testCase.getExpectedOutput().get("bocAsHexWithCrc").toString();

        assertThat(WalletCodes.V4R2.getValue()).isEqualTo(bocAsHex);
//        assertThat(WalletV4R2.builder().build().createCodeCell().toHex().toUpperCase()).isEqualTo(bocAsHex);
    }

    @Test
    public void testSmartContracts8() {

        String testId = "smartcontracts-8";
        TonSdkTestCases.TestCase testCase = tonSdkTestCases.getTestCases().get(testId);

        String description = testCase.getDescription();

        log.info("testId: {}", testId);
        log.info("description: {}", description);

        String privateKey = testCase.getInput().get("privateKey").toString();
        Long workchain = (Long) testCase.getInput().get("workchain");
        Long walletId = (Long) testCase.getInput().get("walletId");

        byte[] secretKey = Utils.hexToSignedBytes(privateKey);
        TweetNaclFast.Signature.KeyPair keyPair = TweetNaclFast.Signature.keyPair_fromSeed(secretKey);

        WalletV4R2 contract = WalletV4R2.builder()
                .wc(workchain)
                .keyPair(keyPair)
                .walletId(walletId)
                .build();

        String expectedRawAddress = (String) testCase.getExpectedOutput().get("rawAddress");
        String expectedBocAsHex = (String) testCase.getExpectedOutput().get("externalMessageBocAsHexWithCrc");

        String rawAddress = contract.getAddress().toRaw();
        assertThat(rawAddress).isEqualTo(expectedRawAddress);

        Message msg = contract.prepareDeployMsg();

        assertThat(msg.toCell().toHex(true).toUpperCase()).isEqualTo(expectedBocAsHex);
    }


    @Test
    public void testSmartContracts9() {

        String testId = "smartcontracts-9";
        TonSdkTestCases.TestCase testCase = tonSdkTestCases.getTestCases().get(testId);

        String description = testCase.getDescription();

        log.info("testId: {}", testId);
        log.info("description: {}", description);

        String privateKey = testCase.getInput().get("privateKey").toString();
        Long workchain = (Long) testCase.getInput().get("workchain");
        Long walletId = (Long) testCase.getInput().get("walletId");

        byte[] secretKey = Utils.hexToSignedBytes(privateKey);
        TweetNaclFast.Signature.KeyPair keyPair = TweetNaclFast.Signature.keyPair_fromSeed(secretKey);

        WalletV4R2 contract = WalletV4R2.builder()
                .wc(workchain)
                .keyPair(keyPair)
                .walletId(walletId)
                .build();

        String expectedRawAddress = (String) testCase.getExpectedOutput().get("rawAddress");
        String expectedCodeAsHex = (String) testCase.getExpectedOutput().get("codeAsHex");
        String expectedDataAsHex = (String) testCase.getExpectedOutput().get("dataAsHex");
        String expectedExternalMessageAsHex = (String) testCase.getExpectedOutput().get("externalMessageAsHex");
        String expectedBocAsHex = (String) testCase.getExpectedOutput().get("externalMessageBocAsHexWithCrc");

        String rawAddress = contract.getAddress().toRaw();
        assertThat(rawAddress).isEqualTo(expectedRawAddress);

        log.info(contract.createCodeCell().print());

        assertThat(contract.createCodeCell().bitStringToHex().toUpperCase()).isEqualTo(expectedCodeAsHex);
        assertThat(contract.createDataCell().bitStringToHex().toUpperCase()).isEqualTo(expectedDataAsHex);
        Message msg = contract.prepareDeployMsg();

        assertThat(msg.toCell().bitStringToHex().toUpperCase()).isEqualTo(expectedExternalMessageAsHex);
        assertThat(msg.toCell().toHex(true).toUpperCase()).isEqualTo(expectedBocAsHex);
    }

    @Test
    public void testSmartContracts10() {

        String testId = "smartcontracts-10";
        TonSdkTestCases.TestCase testCase = tonSdkTestCases.getTestCases().get(testId);

        String description = testCase.getDescription();

        log.info("testId: {}", testId);
        log.info("description: {}", description);

        String privateKey = testCase.getInput().get("privateKey").toString();
        Long workchain = (Long) testCase.getInput().get("workchain");
        String destinationAddress = testCase.getInput().get("destinationAddress").toString();
        Long walletId = (Long) testCase.getInput().get("walletId");
        Long seqno = (Long) testCase.getInput().get("seqNo");
        BigDecimal amountTonCoins = new BigDecimal(testCase.getInput().get("amountTonCoins").toString());
        Boolean bounceFlag = (Boolean) testCase.getInput().get("bounceFlag");
        Long validUntil = (Long) testCase.getInput().get("validUntil");
        Long sendMode = (Long) testCase.getInput().get("sendMode");

        byte[] secretKey = Utils.hexToSignedBytes(privateKey);
        TweetNaclFast.Signature.KeyPair keyPair = TweetNaclFast.Signature.keyPair_fromSeed(secretKey);

        WalletV4R2 contract = WalletV4R2.builder()
                .wc(workchain)
                .keyPair(keyPair)
                .walletId(walletId)
                .build();

        String expectedBocAsHex = (String) testCase.getExpectedOutput().get("externalMessageBocAsHexWithCrc");

        WalletV4R2Config config = WalletV4R2Config.builder()
                .walletId(walletId)
                .seqno(seqno)
                .destination(Address.of(destinationAddress))
                .amount(Utils.toNano(amountTonCoins))
                .validUntil(validUntil)
                .bounce(bounceFlag)
                .mode(sendMode.intValue())
                .build();
        Message sendMsg = contract.prepareExternalMsg(config);
        assertThat(sendMsg.toCell().toHex(true).toUpperCase()).isEqualTo(expectedBocAsHex);
    }

    @Test
    public void testSmartContracts11() {

        String testId = "smartcontracts-11";
        TonSdkTestCases.TestCase testCase = tonSdkTestCases.getTestCases().get(testId);

        String description = testCase.getDescription();

        log.info("testId: {}", testId);
        log.info("description: {}", description);

        String privateKey = testCase.getInput().get("privateKey").toString();
        Long workchain = (Long) testCase.getInput().get("workchain");
        String destinationAddress = testCase.getInput().get("destinationAddress").toString();
        Long walletId = (Long) testCase.getInput().get("walletId");
        Long seqno = (Long) testCase.getInput().get("seqNo");
        BigDecimal amountTonCoins = new BigDecimal(testCase.getInput().get("amountTonCoins").toString());
        Boolean bounceFlag = (Boolean) testCase.getInput().get("bounceFlag");
        Long validUntil = (Long) testCase.getInput().get("validUntil");
        Long sendMode = (Long) testCase.getInput().get("sendMode");

        byte[] secretKey = Utils.hexToSignedBytes(privateKey);
        TweetNaclFast.Signature.KeyPair keyPair = TweetNaclFast.Signature.keyPair_fromSeed(secretKey);

        WalletV4R2 contract = WalletV4R2.builder()
                .wc(workchain)
                .keyPair(keyPair)
                .walletId(walletId)
                .build();

        String expectedBocAsHex = (String) testCase.getExpectedOutput().get("externalMessageBocAsHexWithCrc");

        WalletV4R2Config config = WalletV4R2Config.builder()
                .walletId(walletId)
                .seqno(seqno)
                .destination(Address.of(destinationAddress))
                .amount(Utils.toNano(amountTonCoins))
                .validUntil(validUntil)
                .bounce(bounceFlag)
                .mode(sendMode.intValue())
                .build();
        Message sendMsg = contract.prepareExternalMsg(config);
        assertThat(sendMsg.toCell().toHex(true).toUpperCase()).isEqualTo(expectedBocAsHex);
    }

    @Test
    public void testSmartContracts12() {

        String testId = "smartcontracts-12";
        TonSdkTestCases.TestCase testCase = tonSdkTestCases.getTestCases().get(testId);

        String description = testCase.getDescription();

        log.info("testId: {}", testId);
        log.info("description: {}", description);

        String privateKey = testCase.getInput().get("privateKey").toString();
        Long workchain = (Long) testCase.getInput().get("workchain");
        String destinationAddress = testCase.getInput().get("destinationAddress").toString();
        Long walletId = (Long) testCase.getInput().get("walletId");
        Long seqno = (Long) testCase.getInput().get("seqNo");
        BigDecimal amountTonCoins = new BigDecimal(testCase.getInput().get("amountTonCoins").toString());
        Boolean bounceFlag = (Boolean) testCase.getInput().get("bounceFlag");
        Long validUntil = (Long) testCase.getInput().get("validUntil");
        Long sendMode = (Long) testCase.getInput().get("sendMode");
        String comment = testCase.getInput().get("comment").toString();

        byte[] secretKey = Utils.hexToSignedBytes(privateKey);
        TweetNaclFast.Signature.KeyPair keyPair = TweetNaclFast.Signature.keyPair_fromSeed(secretKey);

        WalletV4R2 contract = WalletV4R2.builder()
                .wc(workchain)
                .keyPair(keyPair)
                .walletId(walletId)
                .build();

        String expectedBocAsHex = (String) testCase.getExpectedOutput().get("externalMessageBocAsHexWithCrc");

        WalletV4R2Config config = WalletV4R2Config.builder()
                .walletId(walletId)
                .seqno(seqno)
                .destination(Address.of(destinationAddress))
                .amount(Utils.toNano(amountTonCoins))
                .validUntil(validUntil)
                .bounce(bounceFlag)
                .mode(sendMode.intValue())
                .comment(comment)
                .build();
        Message sendMsg = contract.prepareExternalMsg(config);
        assertThat(sendMsg.toCell().toHex(true).toUpperCase()).isEqualTo(expectedBocAsHex);
    }

    @Test
    public void testSmartContracts13() {

        String testId = "smartcontracts-13";
        TonSdkTestCases.TestCase testCase = tonSdkTestCases.getTestCases().get(testId);

        String description = testCase.getDescription();

        log.info("testId: {}", testId);
        log.info("description: {}", description);

        String privateKey = testCase.getInput().get("privateKey").toString();
        Long workchain = (Long) testCase.getInput().get("workchain");
        String destinationAddress = testCase.getInput().get("destinationAddress").toString();
        Long walletId = (Long) testCase.getInput().get("walletId");
        Long seqno = (Long) testCase.getInput().get("seqNo");
        BigDecimal amountTonCoins = new BigDecimal(testCase.getInput().get("amountTonCoins").toString());
        Boolean bounceFlag = (Boolean) testCase.getInput().get("bounceFlag");
        Long validUntil = (Long) testCase.getInput().get("validUntil");
        Long sendMode = (Long) testCase.getInput().get("sendMode");
        String body = testCase.getInput().get("body").toString();

        byte[] secretKey = Utils.hexToSignedBytes(privateKey);
        TweetNaclFast.Signature.KeyPair keyPair = TweetNaclFast.Signature.keyPair_fromSeed(secretKey);

        WalletV4R2 contract = WalletV4R2.builder()
                .wc(workchain)
                .keyPair(keyPair)
                .walletId(walletId)
                .build();

        String expectedBocAsHex = (String) testCase.getExpectedOutput().get("externalMessageBocAsHexWithCrc");

        WalletV4R2Config config = WalletV4R2Config.builder()
                .walletId(walletId)
                .seqno(seqno)
                .destination(Address.of(destinationAddress))
                .amount(Utils.toNano(amountTonCoins))
                .validUntil(validUntil)
                .bounce(bounceFlag)
                .mode(sendMode.intValue())
                .body(CellBuilder.beginCell().storeUint(8, 32).endCell())
                .build();
        Message sendMsg = contract.prepareExternalMsg(config);
        assertThat(sendMsg.toCell().toHex(true).toUpperCase()).isEqualTo(expectedBocAsHex);
    }

    @Test
    public void testSmartContracts14() {

        String testId = "smartcontracts-14";
        TonSdkTestCases.TestCase testCase = tonSdkTestCases.getTestCases().get(testId);

        String description = testCase.getDescription();

        log.info("testId: {}", testId);
        log.info("description: {}", description);

        String usdtMasterContractAddress = testCase.getInput().get("usdtMasterContractAddress").toString();
        BigDecimal amountToncoinsToJettonWallet = new BigDecimal(testCase.getInput().get("amountToncoinsToJettonWallet").toString());
        BigInteger amountNanoUsdt = new BigInteger(testCase.getInput().get("amountNanoUsdt").toString());
        BigInteger forwardAmountNanocoins = new BigInteger(testCase.getInput().get("forwardAmountNanocoins").toString());
        String forwardComment = testCase.getInput().get("forwardComment").toString();

        // careful - mainnet
        Tonlib tonlib = Tonlib.builder()
                .testnet(false)
                .ignoreCache(false)
                .build();

        Address usdtMasterAddress = Address.of(usdtMasterContractAddress);

        //64 bytes private key of your wallet, top it up before using
        byte[] secretKey = Utils.hexToSignedBytes("add");

        //use when you have 64 bytes private key
        TweetNaclFast.Signature.KeyPair keyPair = TweetNaclFast.Signature.keyPair_fromSecretKey(secretKey);

        //use when you have 32 bytes private key
        //TweetNaclFast.Signature.KeyPair keyPair = TweetNaclFast.Signature.keyPair_fromSeed(secretKey);

        // create random wallet
        WalletV3R2 randomDestinationWallet = WalletV3R2.builder()
                .keyPair(Utils.generateSignatureKeyPair())
                .walletId(42)
                .build();

        // use your wallet
        WalletV3R2 myWallet = WalletV3R2.builder()
                .tonlib(tonlib)
                .keyPair(keyPair)
                .walletId(42)
                .build();

        String nonBounceableAddress = myWallet.getAddress().toNonBounceable();
        String bounceableAddress = myWallet.getAddress().toBounceable();
        String rawAddress = myWallet.getAddress().toRaw();

        log.info("non-bounceable address: {}", nonBounceableAddress);
        log.info("    bounceable address: {}", bounceableAddress);
        log.info("    raw address: {}", rawAddress);
        log.info("pub-key {}", Utils.bytesToHex(myWallet.getKeyPair().getPublicKey()));
        log.info("prv-key {}", Utils.bytesToHex(myWallet.getKeyPair().getSecretKey()));

        String status = tonlib.getAccountStatus(Address.of(bounceableAddress));
        log.info("account status {}", status);

        String balance = tonlib.getAccountBalance(Address.of(bounceableAddress));
        log.info("account balance {}", Utils.formatNanoValue(balance));

        // myWallet.deploy();
        // myWallet.waitForDeployment(30);

        // get usdt jetton master (minter) address
        JettonMinterStableCoin usdtMasterWallet = JettonMinterStableCoin.builder()
                .tonlib(tonlib)
                .customAddress(usdtMasterAddress)
                .build();

        log.info("usdt total supply: {}", Utils.formatJettonValue(usdtMasterWallet.getTotalSupply(), 6, 2));

        // get my JettonWallet the one that holds my jettons (USDT) tokens
        JettonWalletStableCoin myJettonWallet = usdtMasterWallet.getJettonWallet(myWallet.getAddress());
        log.info("my jettonWallet balance: {}", Utils.formatJettonValue(myJettonWallet.getBalance(), 6, 2));

        // send my jettons to external address
        WalletV3Config walletV3Config = WalletV3Config.builder()
                .walletId(42)
                .seqno(myWallet.getSeqno())
                .destination(myJettonWallet.getAddress())
                .amount(Utils.toNano(amountToncoinsToJettonWallet))
                .body(JettonWalletStableCoin.createTransferBody(
                        0,
                        amountNanoUsdt,                          // jettons to send
                        randomDestinationWallet.getAddress(),    // recipient
                        null,                                    // response address
                        forwardAmountNanocoins,                  // forward amount
                        MsgUtils.createTextMessageBody(forwardComment))
                ).build();
        ExtMessageInfo extMessageInfo = myWallet.send(walletV3Config);
        assertThat(extMessageInfo.getError().getCode()).isZero();

        Utils.sleep(90, "transferring 0.02 USDT jettons to wallet " + randomDestinationWallet.getAddress());

        BigInteger expectedBalanceOfNanocoinsAtRandomAddress = new BigInteger(testCase.getExpectedOutput().get("balanceOfNanocoinsAtRandomAddress").toString());
        BigInteger expectedBalanceOfJettonsAtRandomAddress = new BigInteger(testCase.getExpectedOutput().get("balanceOfJettonsAtRandomAddress").toString());

        BigInteger balanceOfDestinationWallet = new BigInteger(tonlib.getAccountBalance(randomDestinationWallet.getAddress()));
        log.info("balanceOfDestinationWallet in toncoins: {}", balanceOfDestinationWallet);

        JettonWalletStableCoin randomJettonWallet = usdtMasterWallet.getJettonWallet(randomDestinationWallet.getAddress());
        BigInteger balanceOfJettonWallet = randomJettonWallet.getBalance();
        log.info("balanceOfJettonWallet in jettons: {}", Utils.formatJettonValue(balanceOfJettonWallet, 6, 2));

        assertThat(balanceOfDestinationWallet).isEqualTo(expectedBalanceOfNanocoinsAtRandomAddress);
        assertThat(balanceOfJettonWallet).isEqualTo(expectedBalanceOfJettonsAtRandomAddress);
    }

    @Test
    public void testSmartContracts15() {

        String testId = "smartcontracts-15";
        TonSdkTestCases.TestCase testCase = tonSdkTestCases.getTestCases().get(testId);

        String description = testCase.getDescription();

        log.info("testId: {}", testId);
        log.info("description: {}", description);

        String usdtMasterContractAddress = testCase.getInput().get("usdtMasterContractAddress").toString();
        BigDecimal amountToncoinsToJettonWallet = new BigDecimal(testCase.getInput().get("amountToncoinsToJettonWallet").toString());
        BigInteger amountNanoUsdt = new BigInteger(testCase.getInput().get("amountNanoUsdt").toString());
        BigInteger forwardAmountNanocoins = new BigInteger(testCase.getInput().get("forwardAmountNanocoins").toString());

        // careful - mainnet
        Tonlib tonlib = Tonlib.builder()
                .testnet(false)
                .ignoreCache(false)
                .build();

        Address usdtMasterAddress = Address.of(usdtMasterContractAddress);

        //64 bytes private key of your wallet, top it up before using
        byte[] secretKey = Utils.hexToSignedBytes("add");

        //use when you have 64 bytes private key
        TweetNaclFast.Signature.KeyPair keyPair = TweetNaclFast.Signature.keyPair_fromSecretKey(secretKey);

        //use when you have 32 bytes private key
        //TweetNaclFast.Signature.KeyPair keyPair = TweetNaclFast.Signature.keyPair_fromSeed(secretKey);

        // create random wallet
        WalletV4R2 randomDestinationWallet = WalletV4R2.builder()
                .keyPair(Utils.generateSignatureKeyPair())
                .walletId(42)
                .build();

        // use your wallet
        WalletV4R2 myWallet = WalletV4R2.builder()
                .tonlib(tonlib)
                .keyPair(keyPair)
                .walletId(42)
                .build();

        String nonBounceableAddress = myWallet.getAddress().toNonBounceable();
        String bounceableAddress = myWallet.getAddress().toBounceable();
        String rawAddress = myWallet.getAddress().toRaw();

        log.info("non-bounceable address: {}", nonBounceableAddress);
        log.info("    bounceable address: {}", bounceableAddress);
        log.info("    raw address: {}", rawAddress);
        log.info("pub-key {}", Utils.bytesToHex(myWallet.getKeyPair().getPublicKey()));
        log.info("prv-key {}", Utils.bytesToHex(myWallet.getKeyPair().getSecretKey()));

        String status = tonlib.getAccountStatus(Address.of(bounceableAddress));
        log.info("account status {}", status);

        String balance = tonlib.getAccountBalance(Address.of(bounceableAddress));
        log.info("account balance {}", Utils.formatNanoValue(balance));

        // myWallet.deploy();
        // myWallet.waitForDeployment(30);

        // get usdt jetton master (minter) address
        JettonMinterStableCoin usdtMasterWallet = JettonMinterStableCoin.builder()
                .tonlib(tonlib)
                .customAddress(usdtMasterAddress)
                .build();

        log.info("usdt total supply: {}", Utils.formatJettonValue(usdtMasterWallet.getTotalSupply(), 6, 2));

        // get my JettonWallet the one that holds my jettons (USDT) tokens
        JettonWalletStableCoin myJettonWallet = usdtMasterWallet.getJettonWallet(myWallet.getAddress());
        log.info("my jettonWallet balance: {}", Utils.formatJettonValue(myJettonWallet.getBalance(), 6, 2));

        // send my jettons to external address
        WalletV4R2Config walletV4Config = WalletV4R2Config.builder()
                .walletId(42)
                .seqno(myWallet.getSeqno())
                .destination(myJettonWallet.getAddress())
                .amount(Utils.toNano(amountToncoinsToJettonWallet))
                .body(JettonWalletStableCoin.createTransferBody(
                        0,
                        amountNanoUsdt,                          // jettons to send
                        randomDestinationWallet.getAddress(),    // recipient
                        null,                                    // response address
                        forwardAmountNanocoins,                  // forward amount
                        null)                                    // forward payload
                ).build();
        ExtMessageInfo extMessageInfo = myWallet.send(walletV4Config);
        assertThat(extMessageInfo.getError().getCode()).isZero();

        Utils.sleep(90, "transferring 0.02 USDT jettons to wallet " + randomDestinationWallet.getAddress());

        BigInteger expectedBalanceOfNanocoinsAtRandomAddress = new BigInteger(testCase.getExpectedOutput().get("balanceOfNanocoinsAtRandomAddress").toString());
        BigInteger expectedBalanceOfJettonsAtRandomAddress = new BigInteger(testCase.getExpectedOutput().get("balanceOfJettonsAtRandomAddress").toString());

        BigInteger balanceOfDestinationWallet = new BigInteger(tonlib.getAccountBalance(randomDestinationWallet.getAddress()));
        log.info("balanceOfDestinationWallet in toncoins: {}", balanceOfDestinationWallet);

        JettonWalletStableCoin randomJettonWallet = usdtMasterWallet.getJettonWallet(randomDestinationWallet.getAddress());
        BigInteger balanceOfJettonWallet = randomJettonWallet.getBalance();
        log.info("balanceOfJettonWallet in jettons: {}", Utils.formatJettonValue(balanceOfJettonWallet, 6, 2));

        assertThat(balanceOfDestinationWallet).isEqualTo(expectedBalanceOfNanocoinsAtRandomAddress);
        assertThat(balanceOfJettonWallet).isEqualTo(expectedBalanceOfJettonsAtRandomAddress);
    }
}
