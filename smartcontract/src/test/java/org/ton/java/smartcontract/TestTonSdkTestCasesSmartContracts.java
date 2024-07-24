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
import org.ton.java.smartcontract.types.WalletCodes;
import org.ton.java.smartcontract.types.WalletV3Config;
import org.ton.java.smartcontract.types.WalletV4R2Config;
import org.ton.java.smartcontract.wallet.v3.WalletV3R2;
import org.ton.java.smartcontract.wallet.v4.WalletV4R2;
import org.ton.java.tlb.types.Message;
import org.ton.java.utils.Utils;

import java.io.IOException;
import java.math.BigDecimal;
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
}
