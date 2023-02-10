package org.ton.java.tonlib;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;
import com.iwebpp.crypto.TweetNaclFast;
import com.sun.jna.Native;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.ton.java.address.Address;
import org.ton.java.cell.Cell;
import org.ton.java.mnemonic.Mnemonic;
import org.ton.java.tonlib.types.*;
import org.ton.java.utils.Utils;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.Objects;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@Slf4j
@RunWith(JUnit4.class)
public class TestTonlibJson {

    private static final String KEYSTORE_MEMORY = "'keystore_type': { '@type': 'keyStoreTypeInMemory' }";
    public static final String TON_FOUNDATION = "EQCD39VS5jcptHL8vMjEXrzGaRcCVYto7HUn4bpAOg8xqB2N";
    public static final String ELECTOR_ADDRESSS = "-1:3333333333333333333333333333333333333333333333333333333333333333";

    public String INIT_TEMPLATE = Utils.streamToString(Objects.requireNonNull(TestTonlibJson.class.getClassLoader().getResourceAsStream("init.json")));

    Gson gs = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    @Test
    public void testInitTonlibJson() throws IOException {

        TonlibJsonI tonlibJson = Native.load("tonlibjson.dll", TonlibJsonI.class);

        long tonlib = tonlibJson.tonlib_client_json_create();

        InputStream testConfig = TestTonlibJson.class.getClassLoader().getResourceAsStream("testnet-global.config.json");

        assert testConfig != null;

        String data = Utils.streamToString(testConfig);
        testConfig.close();

        assert data != null;

        String dataQuery = JsonParser.parseString(data).getAsJsonObject().toString();

        String q = INIT_TEMPLATE.replace("CFG_PLACEHOLDER", dataQuery).replace("KEYSTORE_TYPE", KEYSTORE_MEMORY);

        String setupQueryQ = JsonParser.parseString(q).getAsJsonObject().toString();

        tonlibJson.tonlib_client_json_send(tonlib, setupQueryQ);
        String result = tonlibJson.tonlib_client_json_receive(tonlib, 10.0);
        assertThat(result).isNotBlank();
    }

    @Test
    public void testTonlib() {
        Tonlib tonlib = Tonlib.builder()
                .keystoreInMemory(true)
                .build();

//        lookupBlock
//        BlockIdExt fullblock = tonlib.lookupBlock(23512606, -1, -9223372036854775808L, 0, 0);

        BlockIdExt fullblock = tonlib.getLast().getLast();
        log.info(fullblock.toString());

        MasterChainInfo masterChainInfo = tonlib.getLast();
        log.info(masterChainInfo.toString());

        //getBlockHeader
        BlockHeader header = tonlib.getBlockHeader(masterChainInfo.getLast());
        log.info(header.toString());

        //getShards
        Shards shards1 = tonlib.getShards(masterChainInfo.getLast()); // only seqno also ok?
        log.info(shards1.toString());
        assertThat(shards1.getShards()).isNotNull();
    }

    @Test
    public void testTonlibGetLast() {
        Tonlib tonlib = Tonlib.builder()
                .testnet(true)
                .keystoreInMemory(true)
                .build();
        BlockIdExt fullblock = tonlib.getLast().getLast();
        log.info("last {}", fullblock);
        assertThat(fullblock).isNotNull();
    }

    @Test
    public void testTonlibGetAllBlockTransactions() {

        Tonlib tonlib = Tonlib.builder().build();

        BlockIdExt fullblock = tonlib.getLast().getLast();
        assertThat(fullblock).isNotNull();

        log.info(fullblock.toString());

        Map<String, RawTransactions> txs = tonlib.getAllBlockTransactions(fullblock, 100, null);
        for (Map.Entry<String, RawTransactions> entry : txs.entrySet()) {
            for (RawTransaction tx : entry.getValue().getTransactions()) {
                if ((tx.getIn_msg() != null) && (!tx.getIn_msg().getSource().getAccount_address().equals(""))) {
                    log.info("{} <<<<< {} : {} ", tx.getIn_msg().getSource().getAccount_address(), tx.getIn_msg().getDestination().getAccount_address(), Utils.formatNanoValue(tx.getIn_msg().getValue(), 9));
                }
                if (tx.getOut_msgs() != null) {
                    for (RawMessage msg : tx.getOut_msgs()) {
                        log.info("{} >>>>> {} : {} ", msg.getSource().getAccount_address(), msg.getDestination().getAccount_address(), Utils.formatNanoValue(msg.getValue()));
                    }
                }
            }
        }
        assertThat(txs.size()).isNotEqualTo(0);
    }

    @Test
    public void testTonlibGetBlockTransactions() {
        Tonlib tonlib = Tonlib.builder().build();

        for (int i = 0; i < 2; i++) {

            MasterChainInfo lastBlock = tonlib.getLast();
            log.info(lastBlock.toString());

            BlockTransactions blockTransactions = tonlib.getBlockTransactions(lastBlock.getLast(), 100);
            log.info(gs.toJson(blockTransactions));

            for (ShortTxId shortTxId : blockTransactions.getTransactions()) {
                Address acccount = Address.of("-1:" + Utils.base64ToHexString(shortTxId.getAccount()));
                log.info("lt {}, hash {}, account {}", shortTxId.getLt(), shortTxId.getHash(), acccount.toString(false));
                RawTransactions rawTransactions = tonlib.getRawTransactions(acccount.toString(false), BigInteger.valueOf(shortTxId.getLt()), shortTxId.getHash());
                for (RawTransaction tx : rawTransactions.getTransactions()) {
                    if ((tx.getIn_msg() != null) && (!tx.getIn_msg().getSource().getAccount_address().equals(""))) {
                        log.info("{}, {} <<<<< {} : {} ", Utils.toUTC(tx.getUtime()), tx.getIn_msg().getSource().getAccount_address(), tx.getIn_msg().getDestination().getAccount_address(), Utils.formatNanoValue(tx.getIn_msg().getValue()));
                    }
                    if (tx.getOut_msgs() != null) {
                        for (RawMessage msg : tx.getOut_msgs()) {
                            log.info("{}, {} >>>>> {} : {} ", Utils.toUTC(tx.getUtime()), msg.getSource().getAccount_address(), msg.getDestination().getAccount_address(), Utils.formatNanoValue(msg.getValue()));
                        }
                    }
                }
            }
            Utils.sleep(10, "wait for next block");
        }
    }

    @Test
    public void testTonlibGetTxsByAddress() {
        Tonlib tonlib = Tonlib.builder().build();

        Address address = Address.of(TON_FOUNDATION);

        log.info("address: " + address.toString(true));

        RawTransactions rawTransactions = tonlib.getRawTransactions(address.toString(false), null, null);

        log.info("total txs: {}", rawTransactions.getTransactions().size());

        for (RawTransaction tx : rawTransactions.getTransactions()) {
            if ((tx.getIn_msg() != null) && (!tx.getIn_msg().getSource().getAccount_address().equals(""))) {
                log.info("{}, {} <<<<< {} : {} ", Utils.toUTC(tx.getUtime()), tx.getIn_msg().getSource().getAccount_address(), tx.getIn_msg().getDestination().getAccount_address(), Utils.formatNanoValue(tx.getIn_msg().getValue()));
            }
            if (tx.getOut_msgs() != null) {
                for (RawMessage msg : tx.getOut_msgs()) {
                    log.info("{}, {} >>>>> {} : {} ", Utils.toUTC(tx.getUtime()), msg.getSource().getAccount_address(), msg.getDestination().getAccount_address(), Utils.formatNanoValue(msg.getValue()));
                }
            }
        }

        assertThat(rawTransactions.getTransactions().size()).isLessThan(20);
    }

    @Test
    public void testTonlibGetAllTxsByAddress() {
        Tonlib tonlib = Tonlib.builder().build();

        Address address = Address.of(TON_FOUNDATION);

        log.info("address: " + address.toString(true));

        RawTransactions rawTransactions = tonlib.getAllRawTransactions(address.toString(false), null, null, 51);

        log.info("total txs: {}", rawTransactions.getTransactions().size());

        for (RawTransaction tx : rawTransactions.getTransactions()) {
            if ((tx.getIn_msg() != null) && (!tx.getIn_msg().getSource().getAccount_address().equals(""))) {
                log.info("<<<<< {} - {} : {} ", tx.getIn_msg().getSource().getAccount_address(), tx.getIn_msg().getDestination().getAccount_address(), Utils.formatNanoValue(tx.getIn_msg().getValue()));
            }
            if (tx.getOut_msgs() != null) {
                for (RawMessage msg : tx.getOut_msgs()) {
                    log.info(">>>>> {} - {} : {} ", msg.getSource().getAccount_address(), msg.getDestination().getAccount_address(), Utils.formatNanoValue(msg.getValue()));
                }
            }
        }

        assertThat(rawTransactions.getTransactions().size()).isGreaterThan(20);
    }

    /**
     * Create new key pair and sign data using Tonlib library
     */
    @Test
    public void testTonlibNewKey() {
        Tonlib tonlib = Tonlib.builder().build();

        Key key = tonlib.createNewKey();
        log.info(key.toString());
        String pubKey = Utils.base64UrlSafeToHexString(key.getPublic_key());
        byte[] secKey = Utils.base64ToBytes(key.getSecret());

        log.info(pubKey);
        log.info(Utils.bytesToHex(secKey));

        TweetNaclFast.Signature.KeyPair keyPair = Utils.generateSignatureKeyPairFromSeed(secKey);
        byte[] secKey2 = keyPair.getSecretKey();
        log.info(Utils.bytesToHex(secKey2));
        assertThat(Utils.bytesToHex(secKey2).contains(Utils.bytesToHex(secKey))).isTrue();
    }

    /**
     * Encrypt/Decrypt using key
     */
    @Test
    public void testTonlibEncryptDecryptKey() {
        Tonlib tonlib = Tonlib.builder().build();
        String secret = "Q3i3Paa45H/F/Is+RW97lxW0eikF0dPClSME6nbogm0=";
        String dataToEncrypt = Utils.stringToBase64("ABC");
        Data encrypted = tonlib.encrypt(dataToEncrypt, secret);
        log.info("encrypted {}", encrypted.getBytes());

        Data decrypted = tonlib.decrypt(encrypted.getBytes(), secret);
        String dataDecrypted = Utils.base64ToString(decrypted.getBytes());
        log.info("decrypted {}", dataDecrypted);

        assertThat("ABC").isEqualTo(dataDecrypted);
    }

    /**
     * Encrypt/Decrypt with using mnemonic
     */
    @Test
    public void testTonlibEncryptDecryptMnemonic() {
        Tonlib tonlib = Tonlib.builder().build();

        String base64mnemonic = Utils.stringToBase64("centring moist twopenny bursary could carbarn abide flirt ground shoelace songster isomeric pis strake jittery penguin gab guileful lierne salivary songbird shore verbal measures");
        String dataToEncrypt = Utils.stringToBase64("ABC");
        Data encrypted = tonlib.encrypt(dataToEncrypt, base64mnemonic);
        log.info("encrypted {}", encrypted.getBytes());

        Data decrypted = tonlib.decrypt(encrypted.getBytes(), base64mnemonic);
        String dataDecrypted = Utils.base64ToString(decrypted.getBytes());

        assertThat("ABC").isEqualTo(dataDecrypted);
    }

    @Test
    public void testTonlibEncryptDecryptMnemonicModule() throws NoSuchAlgorithmException, InvalidKeyException {
        Tonlib tonlib = Tonlib.builder()
                .verbosityLevel(VerbosityLevel.DEBUG)
                .build();

        String base64mnemonic = Utils.stringToBase64(Mnemonic.generateString(24, ""));

        String dataToEncrypt = Utils.stringToBase64("ABC");
        Data encrypted = tonlib.encrypt(dataToEncrypt, base64mnemonic);
        log.info("encrypted {}", encrypted.getBytes());

        Data decrypted = tonlib.decrypt(encrypted.getBytes(), base64mnemonic);
        String dataDecrypted = Utils.base64ToString(decrypted.getBytes());

        assertThat("ABC").isEqualTo(dataDecrypted);
    }

    @Test
    public void testTonlibRawAccountState() {
        Tonlib tonlib = Tonlib.builder().build();

        Address addr = Address.of("Ef8-sf_0CQDgwW6kNuNY8mUvRW-MGQ34Evffj8O0Z9Ly1tZ4");
        log.info("address: " + addr.toString(true));

        AccountAddressOnly accountAddressOnly = AccountAddressOnly.builder()
                .account_address(addr.toString(true))
                .build();

        RawAccountState accountState = tonlib.getRawAccountState(accountAddressOnly);
        log.info(accountState.toString());
        log.info("balance: {}", accountState.getBalance());
        assertThat(accountState.getCode()).isNotBlank();
    }

    @Test
    public void testTonlibAccountState() {
        Tonlib tonlib = Tonlib.builder().build();

        Address addr = Address.of("Ef8-sf_0CQDgwW6kNuNY8mUvRW-MGQ34Evffj8O0Z9Ly1tZ4");
        log.info("address: " + addr.toString(true));

        AccountAddressOnly accountAddressOnly = AccountAddressOnly.builder()
                .account_address(addr.toString(true))
                .build();

        FullAccountState accountState = tonlib.getAccountState(accountAddressOnly);
        log.info(accountState.toString());
        log.info("balance: {}", accountState.getBalance());
        assertThat(accountState.getLast_transaction_id().getHash()).isNotBlank();
        log.info("last {}", tonlib.getLast());
    }

    @Test
    public void testTonlibKeystorePath() {
        Tonlib tonlib = Tonlib.builder()
                .keystoreInMemory(false)
                .keystorePath("D://")
                .build();
        Address address = Address.of("Ef8-sf_0CQDgwW6kNuNY8mUvRW-MGQ34Evffj8O0Z9Ly1tZ4");
        RunResult result = tonlib.runMethod(address, "seqno");
        log.info("gas_used {}, exit_code {} ", result.getGas_used(), result.getExit_code());
        TvmStackEntryNumber seqno = (TvmStackEntryNumber) result.getStack().get(0);
        log.info("seqno: {}", seqno.getNumber());
        assertThat(result.getExit_code()).isZero();
    }

    @Test
    public void testTonlibRunMethodSeqno() {
        Tonlib tonlib = Tonlib.builder()
                .verbosityLevel(VerbosityLevel.FATAL)
                .build();
        Address address = Address.of(TON_FOUNDATION);
        RunResult result = tonlib.runMethod(address, "seqno");
        log.info("gas_used {}, exit_code {} ", result.getGas_used(), result.getExit_code());
        TvmStackEntryNumber seqno = (TvmStackEntryNumber) result.getStack().get(0);
        log.info("seqno: {}", seqno.getNumber());
        assertThat(result.getExit_code()).isZero();
    }

    @Test
    public void testTonlibRunMethodGetJetton() {
        Tonlib tonlib = Tonlib.builder().build();
        Address address = Address.of("EQBYzFXx0QTPW5Lo63ArbNasI_GWRj7NwcAcJR2IWo7_3nTp");
        RunResult result = tonlib.runMethod(address, "get_jetton_data");
        log.info("gas_used {}, exit_code {} ", result.getGas_used(), result.getExit_code());
        log.info("result: {}", result);
        assertThat(result.getExit_code()).isZero();
    }

    @Test
    public void testTonlibRunMethodParticipantsList() {
        Tonlib tonlib = Tonlib.builder().build();

        Address address = Address.of("-1:3333333333333333333333333333333333333333333333333333333333333333");

        RunResult result = tonlib.runMethod(address, "participant_list");
        log.info(result.toString());
        TvmStackEntryList listResult = (TvmStackEntryList) result.getStack().get(0);
        for (Object o : listResult.getList().getElements()) {
            TvmStackEntryTuple t = (TvmStackEntryTuple) o;
            TvmTuple tuple = t.getTuple();
            TvmStackEntryNumber addr = (TvmStackEntryNumber) tuple.getElements().get(0);
            TvmStackEntryNumber stake = (TvmStackEntryNumber) tuple.getElements().get(1);
            log.info("{}, {}", addr.getNumber(), stake.getNumber());
        }
        assertThat(result.getExit_code()).isZero();
    }

    @Test
    public void testTonlibRunMethodActiveElectionId() {
        Tonlib tonlib = Tonlib.builder().build();
        Address address = Address.of("-1:3333333333333333333333333333333333333333333333333333333333333333");
        RunResult result = tonlib.runMethod(address, "active_election_id");
        TvmStackEntryNumber electionId = (TvmStackEntryNumber) result.getStack().get(0);
        log.info("electionId: {}", electionId.getNumber());
        assertThat(result.getExit_code()).isZero();
    }

    @Test
    public void testTonlibRunMethodPastElectionsId() {
        Tonlib tonlib = Tonlib.builder().build();
        Address address = Address.of("-1:3333333333333333333333333333333333333333333333333333333333333333");
        RunResult result = tonlib.runMethod(address, "past_election_ids");
        TvmStackEntryList listResult = (TvmStackEntryList) result.getStack().get(0);
        for (Object o : listResult.getList().getElements()) {
            TvmStackEntryNumber electionId = (TvmStackEntryNumber) o;
            log.info(electionId.getNumber().toString());
        }
        assertThat(result.getExit_code()).isZero();
    }

    @Test
    public void testTonlibRunMethodPastElections() {
        Tonlib tonlib = Tonlib.builder().build();
        Address address = Address.of("-1:3333333333333333333333333333333333333333333333333333333333333333");
        RunResult result = tonlib.runMethod(address, "past_elections");
        TvmStackEntryList listResult = (TvmStackEntryList) result.getStack().get(0);
        log.info("pastElections: {}", listResult);

        assertThat(result.getExit_code()).isZero();
    }

    @Test
    public void testTonlibGetConfig() {
        Tonlib tonlib = Tonlib
                .builder()
                .verbosityLevel(VerbosityLevel.DEBUG)
                .testnet(true)
                .build();
        MasterChainInfo mc = tonlib.getLast();
        Cell c = tonlib.getConfigParam(mc.getLast(), 22);
        log.info(c.print());
    }

    @Test
    public void testTonlibRunMethodComputeReturnedStake() {
        Tonlib tonlib = Tonlib.builder()
                .build();

        Address elector = Address.of(ELECTOR_ADDRESSS);
        RunResult result = tonlib.runMethod(elector, "compute_returned_stake", null);
        log.info("result: {}", result);
        assertThat(result.getExit_code()).isEqualTo(2); // error since compute_returned_stake requires an argument

        Deque<String> stack = new ArrayDeque<>();
        Address validatorAddress = Address.of("Ef_sR2c8U-tNfCU5klvd60I5VMXUd_U9-22uERrxrrt3uzYi");
        stack.offer("[num," + validatorAddress.toDecimal() + "]");

        result = tonlib.runMethod(elector, "compute_returned_stake", stack);
        BigInteger returnStake = ((TvmStackEntryNumber) result.getStack().get(0)).getNumber();
        log.info("return stake: {} ", Utils.formatNanoValue(returnStake.longValue()));
    }
}