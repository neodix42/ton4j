package org.ton.java;

import com.iwebpp.crypto.TweetNaclFast;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.ton.java.address.Address;
import org.ton.java.blockchain.Blockchain;
import org.ton.java.blockchain.print.BlockPrintInfo;
import org.ton.java.blockchain.types.GetterResult;
import org.ton.java.blockchain.types.Network;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.CellSlice;
import org.ton.java.emulator.tvm.TvmVerbosityLevel;
import org.ton.java.emulator.tx.TxVerbosityLevel;
import org.ton.java.smartcontract.faucet.TestnetFaucet;
import org.ton.java.smartcontract.types.WalletV3Config;
import org.ton.java.smartcontract.wallet.v3.WalletV3R2;
import org.ton.java.smartcontract.wallet.v5.WalletV5;
import org.ton.java.tlb.types.Block;
import org.ton.java.tlb.types.Message;
import org.ton.java.utils.Utils;

import java.math.BigInteger;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@RunWith(JUnit4.class)
public class BlockchainTest {
    Address dummyAddress = Address.of("EQAyjRKDnEpTBNfRHqYdnzGEQjdY4KG3gxgqiG3DpDY46u8G");

    @Test
    public void testDeployV3R2ContractOnEmulator() {
        TweetNaclFast.Signature.KeyPair keyPair = Utils.generateSignatureKeyPair();
        WalletV3R2 wallet = WalletV3R2.builder().keyPair(keyPair).walletId(42).build();
        Blockchain blockchain = Blockchain.builder().network(Network.EMULATOR).contract(wallet).build();
        assertThat(blockchain.deploy(30)).isTrue();
    }

    @Test
    public void testDeployV5ContractOnEmulator() {
        TweetNaclFast.Signature.KeyPair keyPair = Utils.generateSignatureKeyPair();
        WalletV5 wallet =
                WalletV5.builder().keyPair(keyPair).walletId(42).isSigAuthAllowed(true).build();
        Blockchain blockchain = Blockchain.builder().network(Network.EMULATOR).contract(wallet).build();
        assertThat(blockchain.deploy(30)).isTrue();
    }

    @Test
    public void testDeployV5ContractOnMyLocalTon() {
        TweetNaclFast.Signature.KeyPair keyPair = Utils.generateSignatureKeyPair();
        WalletV5 wallet =
                WalletV5.builder().keyPair(keyPair).walletId(42).isSigAuthAllowed(true).build();
        Blockchain blockchain =
                Blockchain.builder()
                        .network(Network.MY_LOCAL_TON)
                        .myLocalTonInstallationPath("G:/Git_Projects/MyLocalTon/myLocalTon")
                        .contract(wallet)
                        .build();
        assertThat(blockchain.deploy(30)).isTrue();
    }

    @Test
    public void testDeployV5ContractOnTestnet() {
        TweetNaclFast.Signature.KeyPair keyPair = Utils.generateSignatureKeyPair();
        WalletV5 wallet =
                WalletV5.builder().keyPair(keyPair).walletId(42).isSigAuthAllowed(true).build();
        Blockchain blockchain = Blockchain.builder().network(Network.TESTNET).contract(wallet).build();
        assertThat(blockchain.deploy(30)).isTrue();
    }

    @Test
    public void testDeployCustomContractContractOnEmulator() {

        Blockchain blockchain =
                Blockchain.builder()
                        .network(Network.EMULATOR)
                        .customContractAsResource("simple.tolk")
                        .customContractDataCell(
                                CellBuilder.beginCell()
                                        .storeUint(0, 32) // seqno
                                        .storeInt(
                                                Utils.getRandomInt(),
                                                32) // unique integer, to make contract address random each time
                                        .endCell())
                        .build();
        assertThat(blockchain.deploy(30)).isTrue();
    }

    @Test
    public void testDeployCustomContractContractOnTestnet() {

        Blockchain blockchain =
                Blockchain.builder()
                        .network(Network.TESTNET)
                        .customContractAsResource("simple.fc")
                        .customContractDataCell(
                                CellBuilder.beginCell()
                                        .storeUint(0, 32) // seqno
                                        .storeInt(Utils.getRandomInt(), 32)
                                        .endCell())
                        .build();
        assertThat(blockchain.deploy(30)).isTrue();
    }

    @Test
    public void testDeployCustomContractContractWithBodyOnTestnet() {

        Blockchain blockchain =
                Blockchain.builder()
                        .network(Network.TESTNET)
                        .customContractAsResource("simple.fc")
                        .customContractDataCell(
                                CellBuilder.beginCell()
                                        .storeUint(1, 32) // seqno
                                        .storeInt(Utils.getRandomInt(), 32)
                                        .endCell())
                        .customContractBodyCell(
                                CellBuilder.beginCell()
                                        .storeUint(1, 32) // seqno
                                        .endCell())
                        .build();
        assertThat(blockchain.deploy(30)).isTrue();
    }

    @Test
    public void testGetMethodsV3R2ContractOnEmulator() {
        TweetNaclFast.Signature.KeyPair keyPair = Utils.generateSignatureKeyPair();
        WalletV3R2 wallet = WalletV3R2.builder().keyPair(keyPair).walletId(42).build();
        Blockchain blockchain = Blockchain.builder().network(Network.EMULATOR).contract(wallet).build();
        assertThat(blockchain.deploy(30)).isTrue();
        GetterResult result = blockchain.runGetMethod("seqno");
        log.info("result {}", result);
        log.info("seqno {}", blockchain.runGetSeqNo());
        log.info("pubKey {}", blockchain.runGetPublicKey());
    }

    @Test
    public void testGetMethodsV5ContractOnEmulator() {
        TweetNaclFast.Signature.KeyPair keyPair = Utils.generateSignatureKeyPair();
        WalletV5 wallet =
                WalletV5.builder().keyPair(keyPair).walletId(42).isSigAuthAllowed(true).build();
        Blockchain blockchain = Blockchain.builder().network(Network.EMULATOR).contract(wallet).build();
        assertThat(blockchain.deploy(30)).isTrue();
        GetterResult result = blockchain.runGetMethod("seqno");
        log.info("result {}", result);
        log.info("seqno {}", blockchain.runGetSeqNo());
        log.info("pubKey {}", blockchain.runGetPublicKey());
        log.info("subWalletId {}", blockchain.runGetSubWalletId());
    }

    @Test
    public void testGetMethodsCustomContractOnEmulator() {
        Blockchain blockchain =
                Blockchain.builder()
                        .network(Network.EMULATOR)
                        .customContractAsResource("simple.fc")
                        .customContractDataCell(
                                CellBuilder.beginCell()
                                        .storeUint(0, 32)
                                        .storeInt(Utils.getRandomInt(), 32)
                                        .endCell())
                        .build();
        GetterResult result = blockchain.runGetMethod("unique");
        log.info("result {}", result);
        log.info("seqno {}", blockchain.runGetSeqNo());
    }

    @Test
    public void testGetMethodsV3R2ContractOnTestnet() {
        TweetNaclFast.Signature.KeyPair keyPair = Utils.generateSignatureKeyPair();
        WalletV3R2 wallet = WalletV3R2.builder().keyPair(keyPair).walletId(42).build();
        Blockchain blockchain = Blockchain.builder().network(Network.TESTNET).contract(wallet).build();
        assertThat(blockchain.deploy(30)).isTrue();
        GetterResult result = blockchain.runGetMethod("seqno");
        log.info("result {}", result);
        log.info("seqno {}", blockchain.runGetSeqNo());
        log.info("pubKey {}", blockchain.runGetPublicKey());
    }

    @Test
    public void testGetMethodsV5ContractOnTestnet() {
        TweetNaclFast.Signature.KeyPair keyPair = Utils.generateSignatureKeyPair();
        WalletV5 wallet =
                WalletV5.builder().keyPair(keyPair).walletId(42).isSigAuthAllowed(true).build();
        Blockchain blockchain = Blockchain.builder().network(Network.TESTNET).contract(wallet).build();
        assertThat(blockchain.deploy(30)).isTrue();
        GetterResult result = blockchain.runGetMethod("seqno");
        log.info("result {}", result);
        log.info("seqno {}", blockchain.runGetSeqNo());
        log.info("pubKey {}", blockchain.runGetPublicKey());
        log.info("subWalletId {}", blockchain.runGetSubWalletId());
    }

    @Test
    public void testGetMethodsCustomContractOnTestnet() {
        Blockchain blockchain =
                Blockchain.builder()
                        .network(Network.TESTNET)
                        .customContractAsResource("simple.fc")
                        .customContractDataCell(
                                CellBuilder.beginCell()
                                        .storeUint(0, 32)
                                        .storeInt(Utils.getRandomInt(), 32)
                                        .endCell())
                        .build();
        assertThat(blockchain.deploy(30)).isTrue();
        GetterResult result = blockchain.runGetMethod("unique");
        log.info("result {}", result);
        log.info("seqno {}", blockchain.runGetSeqNo());
    }

    @Test
    public void testGetMethodsCustomContractOnTestnetTolk() {
        Blockchain blockchain =
                Blockchain.builder()
                        .network(Network.TESTNET)
                        .customContractAsResource("simple.tolk")
                        .customContractDataCell(
                                CellBuilder.beginCell()
                                        .storeUint(0, 32)
                                        .storeInt(Utils.getRandomInt(), 32)
                                        .endCell())
                        .build();
        assertThat(blockchain.deploy(30)).isTrue();
        GetterResult result = blockchain.runGetMethod("unique");
        log.info("result {}", result);
        log.info("seqno {}", blockchain.runGetSeqNo());
    }

    @Test
    public void testSendMessageV3R2ContractOnTestnet() {
        TweetNaclFast.Signature.KeyPair keyPair = Utils.generateSignatureKeyPair();
        WalletV3R2 wallet = WalletV3R2.builder().keyPair(keyPair).walletId(42).build();
        Blockchain blockchain = Blockchain.builder().network(Network.TESTNET).contract(wallet).build();
        assertThat(blockchain.deploy(30)).isTrue();

        WalletV3Config configA =
                WalletV3Config.builder()
                        .walletId(42)
                        .seqno(blockchain.runGetSeqNo().longValue())
                        .destination(Address.of(TestnetFaucet.FAUCET_ADDRESS_RAW))
                        .amount(Utils.toNano(0.05))
                        .comment("ton4j-test")
                        .mode(3)
                        .build();

        Message msg = wallet.prepareExternalMsg(configA);

        blockchain.sendExternal(msg);
    }

    @Test
    public void testSendMessageV3R2ContractOnTestnetCustomWorkchain() {
        TweetNaclFast.Signature.KeyPair keyPair = Utils.generateSignatureKeyPair();
        WalletV3R2 wallet = WalletV3R2.builder().keyPair(keyPair).walletId(42)
                .wc(-1) // custom workchain
                .build();
        Blockchain blockchain = Blockchain.builder().network(Network.TESTNET)
                .initialDeployTopUpAmount(Utils.toNano(1))
                .contract(wallet)
                .build();
        assertThat(blockchain.deploy(30)).isTrue();

        log.info("seqno {}", blockchain.runGetSeqNo());

        WalletV3Config configA =
                WalletV3Config.builder()
                        .walletId(42)
                        .seqno(blockchain.runGetSeqNo().longValue())
                        .destination(Address.of(TestnetFaucet.FAUCET_ADDRESS_RAW))
                        .amount(Utils.toNano(0.1))
                        .comment("ton4j-test-асдф")
                        .mode(3)
                        .build();

        Message msg = wallet.prepareExternalMsg(configA);

        blockchain.sendExternal(msg, 15);
        log.info("seqno {}", blockchain.runGetSeqNo());
    }

    @Test
    public void testSendMessageV3R2ContractOnEmulator() {
        TweetNaclFast.Signature.KeyPair keyPair = Utils.generateSignatureKeyPair();
        WalletV3R2 wallet = WalletV3R2.builder().keyPair(keyPair).walletId(42).build();
        Blockchain blockchain = Blockchain.builder().network(Network.EMULATOR).contract(wallet).build();

        assertThat(blockchain.deploy(30)).isTrue();
        log.info("seqno {}", blockchain.runGetSeqNo());

        WalletV3Config configA =
                WalletV3Config.builder()
                        .walletId(42)
                        .seqno(blockchain.runGetSeqNo().longValue())
                        .destination(Address.of(TestnetFaucet.FAUCET_ADDRESS_RAW))
                        .amount(Utils.toNano(0.05))
                        .comment("ton4j-test")
                        .mode(3)
                        .build();

        Message msg = wallet.prepareExternalMsg(configA);

        blockchain.sendExternal(msg);
        log.info("seqno {}", blockchain.runGetSeqNo());
    }

    @Test
    public void testSendMessageV3R2ContractOnEmulatorCustomWorkchain() {
        TweetNaclFast.Signature.KeyPair keyPair = Utils.generateSignatureKeyPair();
        WalletV3R2 wallet = WalletV3R2.builder().keyPair(keyPair).walletId(42)
                .wc(-1) // custom workchain
                .build();
        Blockchain blockchain = Blockchain.builder().network(Network.EMULATOR).contract(wallet).build();
        assertThat(blockchain.deploy(30)).isTrue();
        log.info("seqno {}", blockchain.runGetSeqNo());

        WalletV3Config configA =
                WalletV3Config.builder()
                        .walletId(42)
                        .seqno(blockchain.runGetSeqNo().longValue())
                        .destination(Address.of(TestnetFaucet.FAUCET_ADDRESS_RAW))
                        .amount(Utils.toNano(0.05))
                        .comment("ton4j-test")
                        .mode(3)
                        .build();

        Message msg = wallet.prepareExternalMsg(configA);

        blockchain.sendExternal(msg);
        log.info("seqno {}", blockchain.runGetSeqNo());
    }

    @Test(expected = Error.class)
    public void testSendMessageV3R2ContractOnEmulatorErrorNoMethod() {
        TweetNaclFast.Signature.KeyPair keyPair = Utils.generateSignatureKeyPair();
        WalletV3R2 wallet = WalletV3R2.builder().keyPair(keyPair).walletId(42).build();
        Blockchain blockchain = Blockchain.builder().network(Network.EMULATOR).contract(wallet).build();
        assertThat(blockchain.deploy(30)).isTrue();
        blockchain.runGetMethod("unique");
    }

    @Test
    public void testSendMessageCustomContractOnTestnetTolk() {
        Blockchain blockchain =
                Blockchain.builder()
                        .network(Network.TESTNET)
                        .customContractAsResource("simple.tolk")
                        .customContractDataCell(
                                CellBuilder.beginCell()
                                        .storeUint(0, 32)
                                        .storeInt(Utils.getRandomInt(), 32)
                                        .endCell())
                        .tvmEmulatorVerbosityLevel(TvmVerbosityLevel.WITH_ALL_STACK_VALUES)
                        .txEmulatorVerbosityLevel(TxVerbosityLevel.WITH_ALL_STACK_VALUES)
                        //            .tonlibVerbosityLevel(VerbosityLevel.DEBUG)
                        .build();

        assertThat(blockchain.deploy(30)).isTrue();

        blockchain.runGetMethod("unique");
        System.out.printf("returned seqno %s\n", blockchain.runGetSeqNo());

        Cell bodyCell =
                CellBuilder.beginCell()
                        .storeUint(1, 32) // seqno
                        .endCell();

        blockchain.sendExternal(bodyCell, 15);
    }

    @Test
    public void testSendMessageCustomContractOnTestnetTolkWithCustomTestnetGlobalConfig() {
        Blockchain blockchain =
                Blockchain.builder()
                        .network(Network.TESTNET)
                        .customGlobalConfigPath("g:/testnet-global.config.json")
                        .customContractAsResource("simple.tolk")
                        .customContractDataCell(
                                CellBuilder.beginCell()
                                        .storeUint(0, 32)
                                        .storeInt(Utils.getRandomInt(), 32)
                                        .endCell())
                        .printTxBlockData(false)
                        .tvmEmulatorVerbosityLevel(TvmVerbosityLevel.WITH_ALL_STACK_VALUES)
                        .txEmulatorVerbosityLevel(TxVerbosityLevel.WITH_ALL_STACK_VALUES)
                        //            .tonlibVerbosityLevel(VerbosityLevel.DEBUG)
                        .build();

        assertThat(blockchain.deploy(30)).isTrue();

        blockchain.runGetMethod("unique");
        System.out.printf("returned seqno %s\n", blockchain.runGetSeqNo());

        Cell bodyCell =
                CellBuilder.beginCell()
                        .storeUint(1, 32) // seqno
                        .endCell();

        blockchain.sendExternal(bodyCell, 15);
    }

    @Test
    public void testSendMessageCustomContractOnTestnetTolkWithCustomWorkchain() {
        Blockchain blockchain =
                Blockchain.builder()
                        .network(Network.TESTNET)
                        .customContractAsResource("simple.tolk")
                        .customContractDataCell(
                                CellBuilder.beginCell()
                                        .storeUint(0, 32)
                                        .storeInt(Utils.getRandomInt(), 32)
                                        .endCell())
                        .customContractWorkchain(-1)
                        .build();

        assertThat(blockchain.deploy(30)).isTrue();

        blockchain.runGetMethod("unique");
        System.out.printf("returned seqno %s\n", blockchain.runGetSeqNo());

        Cell bodyCell =
                CellBuilder.beginCell()
                        .storeUint(1, 32) // seqno
                        .endCell();

        blockchain.sendExternal(bodyCell, 15);
    }

    @Test
    public void testSendMessageCustomContractOnEmulatorTolk() {
        Blockchain blockchain =
                Blockchain.builder()
                        .network(Network.EMULATOR)
                        .customContractAsResource("simple.tolk")
                        .customContractDataCell(
                                CellBuilder.beginCell()
                                        .storeUint(1, 32)
                                        .storeInt(Utils.getRandomInt(), 32)
                                        .endCell())
                        //            .tvmEmulatorVerbosityLevel(TvmVerbosityLevel.WITH_ALL_STACK_VALUES)
                        //            .txEmulatorVerbosityLevel(TxVerbosityLevel.WITH_ALL_STACK_VALUES)
                        .build();

        assertThat(blockchain.deploy(30)).isTrue();

        blockchain.runGetMethod("unique");
        System.out.printf("returned seqno %s\n", blockchain.runGetSeqNo());

        Cell bodyCell =
                CellBuilder.beginCell()
                        .storeUint(1, 32) // seqno
                        .endCell();

        blockchain.sendExternal(bodyCell);
        System.out.printf("returned seqno %s\n", blockchain.runGetSeqNo());
    }

    @Test
    public void testSendMessagesChainCustomContractOnEmulatorTolk() {
        Blockchain blockchain =
                Blockchain.builder()
                        .network(Network.EMULATOR)
                        .customContractAsResource("simple.tolk")
                        .customContractDataCell(
                                CellBuilder.beginCell()
                                        .storeUint(0, 32)
                                        .storeInt(Utils.getRandomInt(), 32)
                                        .endCell())
                        //            .tvmEmulatorVerbosityLevel(TvmVerbosityLevel.WITH_ALL_STACK_VALUES)
                        //            .txEmulatorVerbosityLevel(TxVerbosityLevel.WITH_ALL_STACK_VALUES)
                        .build();
        assertThat(blockchain.deploy(30)).isTrue();
        blockchain.runGetMethod("unique");
        BigInteger currentSeqno = blockchain.runGetSeqNo();
        System.out.printf("returned seqno %s\n", currentSeqno);

        Cell bodyCell =
                CellBuilder.beginCell()
                        .storeUint(0, 32) // seqno
                        .endCell();

        blockchain.sendExternal(bodyCell);

        currentSeqno = blockchain.runGetSeqNo();
        System.out.printf("returned seqno %s\n", currentSeqno);

        bodyCell =
                CellBuilder.beginCell()
                        .storeUint(1, 32) // seqno
                        .endCell();

        blockchain.sendExternal(bodyCell);
        currentSeqno = blockchain.runGetSeqNo();
        System.out.printf("returned seqno %s\n", currentSeqno);
    }

    @Test
    public void testSendMessagesChainCustomContractOnEmulatorFunc() {
        Blockchain blockchain =
                Blockchain.builder()
                        .network(Network.EMULATOR)
                        .customContractAsResource("simple.fc")
                        .customContractDataCell(
                                CellBuilder.beginCell()
                                        .storeUint(0, 32)
                                        .storeInt(Utils.getRandomInt(), 32)
                                        .endCell())
                        .build();
        assertThat(blockchain.deploy(30)).isTrue();
        blockchain.runGetMethod("unique");
        BigInteger currentSeqno = blockchain.runGetSeqNo();
        System.out.printf("returned seqno %s\n", currentSeqno);

        Cell bodyCell =
                CellBuilder.beginCell()
                        .storeUint(0, 32) // seqno
                        .endCell();

        blockchain.sendExternal(bodyCell);

        currentSeqno = blockchain.runGetSeqNo();
        System.out.printf("returned seqno %s\n", currentSeqno);

        bodyCell =
                CellBuilder.beginCell()
                        .storeUint(1, 32) // seqno
                        .endCell();

        blockchain.sendExternal(bodyCell, 15);
        currentSeqno = blockchain.runGetSeqNo();
        System.out.printf("returned seqno %s\n", currentSeqno);
    }

    @Test
    public void testShouldDeserializeBlock3() {
        Cell c =
                CellBuilder.beginCell()
                        .fromBoc(
                                "b5ee9c72e102c101001e7900001c00c200dc01b2024802e4031803340388039803ea0440045204a804ba0512056205ca063206480662069c06e80704072007c607e2088808f409960a180a920ade0b580ba60c200c3a0cde0d200d820d9e0e0e0e880f020f4f101610c610d011481200121e123c12e2130013a6141414b6153a15d71623166f16e81735178017cd1846189318ea190e198819d51a251a711a821a8e1aa01aee1b3c1b4e1b9c1bea1c441c901ca41cf21d461d501d5d1d691dbc1e0a1e171e261e391e861ed21edc1ee91ef81f021fae1fb8205820a8214a2154215e21e4229e2324233223d4248e2495251b252825ca268b2694271b273e274627a828482852285c28e2299d29a42a2a2a422ae02aea2af42b7a2b842b8e2c492c502cd62ce42cee2cf82d002d082d102dc12e512f012fbb2fc230483060311f31be3279328033063314331e332833303338334033f13481353135d336393643366636703762376a3774377e378b3845384c38d238ea399b3a3c3a9d3aa93b633b6a3bf03c0a3cb53cbf3cd83cf2041011ef55aaffffff110102030401a09bc7a987000000000001000000010000000000ffffffff000000000000000065a57c5f00000000000f424000000000000f424cf530ba43000000000000000100000000c400000004000000000000002e050211b8e48dfb465ec8780406071a8af25bc2df9aae9c490d507c11f4d756cb8dc51a86d9efa94e3dae27283ba149ce0009045052419cde96601f375f5ff37f0c0958d99d696a6b31a075787a22769006e057724ab9590a228d7373ef8d28544bea428d454ad789b07c24ceb3fc7a07c32c5900140014111204894a33f6fd789a0f4a1d0352df0638d90269bafa60236a1c793ec9cf4236b6bac9cf6e20a0d9576093fecf8a711917790021c5ceae28344e301649c28a1cdd9dab8c4e62f1c008090a0b00980000000000000000000000005052419cde96601f375f5ff37f0c0958d99d696a6b31a075787a22769006e0575ab4d36de07ce24d78ddc0c37a776ebea7728d08bc5d720cf7ab662a4ffb23e0012d84563886eda33f200422b1c437989ce3804081b6b0b008be01150232a9f88011954fc40018be11097e7962208ebbff6bf2885d99ad2bbab54de961312de7847b8fa3b400a6f8c31d00089b2dcef020490109a06dac2c020c110716d4b19170a07e8775c50a0af5461c5b87fcc5878ff7d437f310933958a9d808000d9996e512621307f2514b5db2c0d8cab0bd8cbd51e3e6c7304b65a1b7c0222dcfa33339c5a7ea640007cca56004a9aaab02091036d616010d0e12090c349ad122ad99ef25e8b7753cd0015b5dbedec46ca98e6b567455a9ef0408f90008101efe9201525302091017d784010f10120bf43cb4609d2e5902366b629cd8c02278e730f5a4e5a40b1ac7b303de9c7dc5e700074405f5e10040595a120341a1ee8154366921e3340d1007d7ed026f32a2d60dd4d48af490b856ce5581c0000750405e5f245b9023afe2ffffff1100ffffffff0000000000000000000000000000000065a57c180000000000000000ffffffff6013141516245b9023afe2ffffff1100ffffffff0000000000000000000000010000000065a57c5f00000000000f424c00000001602425262700110000000000000000102113821158e21bb68cfc80101721330000000000000000000000000000000084563886eda33f2000282c284801019fffb8e8af80f298cefbc832633b545151ef8eb59eb427e724054e1cab56efbf000f221340422b1c4376d19f9002181922130108ac710d90c501c0081a1b219fbf955555555555555555555555555555555555555555555555555555555555555502812a05f200000000000000000000000000000000000000000000000000000000000000000000000000000000002023221460108ac710d4643854001c1d219fbf66666666666666666666666666666666666666666666666666666666666666660502540be4000000000000000000000000000000000000000000000000000000000000000000000000000000000040212165df40845638869f68160000000000000000000000000000000000000000000000000000000000000000000000000000000000041e219bbe8f64c6afbff3dd10d8ba6707790ac9670d540f37a9448b0337baa6a5a92acac021dcd65000000000000000000000000000000000000000000000000000000000000000000000000000000000021f2377cff0000000000000000000000000000000000000000000000000000000000000000212819f400000000000000000000000021158e21a7da0580013c03a3b3c236fcff04f64c6afbff3dd10d8ba6707790ac9670d540f37a9448b0337baa6a5a92acac21881f4800000000000000000000000010ee6b28017f03e2040004811ef55aa00000000000000000000000000000000000000000000000000000000000000002271cff33333333333333333333333333333333333333333333333333333333333333332cc9ba500000000000000000000000001409502f90016d042220049000000000000000000000000000000000000000000000000000000000000000000000000012271cff555555555555555555555555555555555555555555555555555555555555555533a973dc0000000000000000000000001409502f90015d046470211800000000007a124d028293213f67853e1526776d1b23d485fb81f769071332488bcf051b56df075d6bd5b592a1ade8ffd445f48e56ad87fb9c35eaae2697c251894e22a4bf2929bf7b675646300130007821158e21bcc4e71c03032be22390000000000000000000000000000000184563886f3139c7009cc4b4028be2c2455cc26aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaac22b1c4379fc3f1806a9482dbe0213e2000000000000f424982a2b006bb0400000000000000000000000800000000007a1234909ba9b6b469399f9e52775ebeb6c4de942610ad6cc27bdac50ef7fd984a6dbc00173a7e00000000000000020847cbb01de39c3308edf1091131c7dedb780ca3e34d85bdc1a6ab4df16d79e800000000003d092400000000003d092605b0173a7ffffffffffffffffd64515aa043f53a3f60eaf663295c8a035537a617a3cc21f5e94859356c021f6400000000003d092c00000000003d092e058284801018036bd5aa65bd160ccc4f204a44731afdb62bab1ab21c9496af2fc1b4d226ccd000302bf0001f530ba430000000060000000000000000800000000000000000000000282920ce6f4b300f9bafaff9bf8604ac6cceb4b53598d03abc3d113b4803702bad5a69b6f03e7126bc6ee061bd3bb75f53b946845e2eb9067bd5b31527fd91f00be2e2f00abd040000000000000002000000000000000000000000a0a48339bd2cc03e6ebebfe6fe1812b1b33ad2d4d66340eaf0f444ed200dc0aeb569a6dbc0f9c49af1bb8186f4eedd7d4ee51a1178bae419ef56cc549ff647c1002012030310073dfe8cb4af8be0000000000000002000000020000000000000002000000000000000000000000000000000000000000000000000000000000000100b3bfecabb049ff67c5388c8bbc8010e2e757141a27180b24e1450e6eced5c6273178a32d2be2f800000000000000080000000800000000000000080000000000000000000000000000000000000000000000000000000000000004231340422b1c437989ce38063334be23130108ac710d9ba5bc60183536be219fbf955555555555555555555555555555555555555555555555555555555555555502812a05f200393159c085ce242a1fe7dc6f3b4ef0cfbf54eef7ad3f40de588995b86f9c2f46400000000003d0922045231460108ac710d4479c1c013738be219fbf66666666666666666666666666666666666666666666666666666666666666660502b95fd5003794d7a89bf0b1b83d2e445fab3781b81d7584baecb59bebf56b2e2f1fb798f3800000000007a12140412265df40845638869fc774100fb887249507b17fc5674e8ad1ae8058eddc490c24e8c0138896214d64c0a4637800000000007a121cbe39219bbe8f64c6afbff3dd10d8ba6707790ac9670d540f37a9448b0337baa6a5a92acac0213ab668031a93e8c798b2cb3e2acc80b1abde06ed7b06270a03fe422373e797c88470ea8c00000000003d09223d2477cff000000000000000000000000000000000000000000000000000000000000000021881c9400000000000000000003d09121158e21a7f1dd04033c0be3a3b3c0098ff0020dd2082014c97ba9730ed44d0d70b1fe0a4f260810200d71820d70b1fed44d0d31fd3ffd15112baf2a122f901541044f910f2a2f80001d31f31d307d4d101fb00a4c8cb1fcbffc9ed5400480000000045f1e3dc29d6bf453889c449069a2458b2d56701fc3661d498162f743e5ba621284801012d8973441aaae38b20c3fda9274f0a97de1447af2ecd7e424267fcca9901866b0003236fcff04f64c6afbff3dd10d8ba6707790ac9670d540f37a9448b0337baa6a5a92acac21881f4800000000000000000003d093109d5b34017f03e3f402848010145910e27fe37d8dcf1fac777ebb3bda38ae1ea8389f81bfb1bc0079f3f67ef5b0002004811ef55ac000000000000000000000000000000000000000000000000000000000000000028480101986c49971b96062e1fba4410e27249c8d73b0a9380f7ffd44640167e68b215e800032271cff33333333333333333333333333333333333333333333333333333333333333332ce9bcbc00000000000000000003d090d40ae57f54016d0424328480101febfd56fa7c2a5010aea13c738df0c2155d6670e8b055dd312d2372f38f7701b000e015188caa7e20000000000000000000000000000000000000000000000000000000000000000000000000144001f65a5823b65a57fe3609184e72a0000102271cff555555555555555555555555555555555555555555555555555555555555555533a973dc00000000000000000003d0925409502f90015d046472848010160d01ddc9d54e89cf8040b78fab3258c9327109018e4078f7f5c7ff0ba401aa2000c21490000000013311def76051bbabfb248bd9a90d300716f598ffe8f8b65c8fddd8501b0e8c6404828480101aa559cb80ee5ce3f4b1e6107ec3ea424c2b89946804aad09f246e96353cc15c5000e02090d96e778104a4b020300104c4d02090d96e778104e4f0246bfad8011dbd0ddd5549e65b1288c858e77dc3e0a5ba9eae0d756ca2e8b5731753e0030b8b90246bf919848778310ed687087000647581469374dff5e9f0fb1cf744809c5016e12fb0030577502094365b9de0450510246bfa9346e9464c7f146a6be4225b1c98e29a59561fbb67319922eda6acc69f6b9130030617f0245bf2ac017c203ca0d9d008bf543c1364eebf27183668cf1e8b1e0e98af56b2dc1dc00c2b0b10251bf084dd4db5a349ccfcf293baf5f5b626f4a130856b6613ded62877bfecc2536dcd96e7780cd96e77a5d950244bfaeffebddf2ee4f04633ee2bbba628ae4e08b6e0043d11727286b0df248a2e177009389020b6501efe9201054550343beccc243bc18876b43843800323ac0a349ba6ffaf4f87d8e7ba2404e280b7097d814578956024bbec8a2b54087ea747ec1d5ecc652b91406aa6f4c2f479843ebd290b26ad8043eca03dfd2400c589b02016157750106460600940106460600a6024bbf0847cbb01de39c3308edf1091131c7dedb780ca3e34d85bdc1a6ab4df16d79e9017d7840065b9b0343bf084dd4db5a349ccfcf293baf5f5b626f4a130856b6613ded62877bfecc2536dc0a5d755c0106460600a4020766cb73bd5d95010c46060365b9de990343bf24d1ba51931fc51a9af90896c72638a6965587eed9cc6648bb69ab31a7dae44c0a6189600243bf17c9d39d78e8498a560800ddfb11dc48be1bc0edc2fbf90844eceb5ab464961002a59b020161617f01064606009202070ccb72896364020101656602a4bffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff79996e5117ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffbe8000000000001e848a665b944757702010167680297bf955555555555555555555555555555555555555555555555555555555555555502aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaad0000000000003d0920170721203c3c94d7ae89f0cd6db6fe18c55144a27722be36ffd9c9fffd3a729dc3c93d176000960107a7b0397bf66666666666666666666666666666666666666666666666666666666666666660533333333333333333333333333333333333333333333333333333333333333339f000000000007a12008696a6b010350406c01034040b1008272b594683d1b0f1242a1ace8917e2a0c0ee1f0307ce39646065f5038d58ca23ff7d855aea1ede2adcff55d3d37fcf59c03bd19c39f843a14faef85cad3980cf69203af7333333333333333333333333333333333333333333333333333333333333333300000000000f42410000000000000000000000000000000000000000000000000000000000000000000000000000000065a57c5f0001408716d6e008272b594683d1b0f1242a1ace8917e2a0c0ee1f0307ce39646065f5038d58ca23ff79304b9982993165dc7b4b2ccfc64c61bf6455afe7e4fe1233d5f827261360d8e02052030246fb7009e439e2e625a000000000000000000750000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000003af7555555555555555555555555555555555555555555555555555555555555555500000000000f42480000000000000000000000000000000000000000000000000000000000000000000000000000000065a57c5f0001408717273000120008272ae405edf07a33e732225cf9180254cdfea049b9a29cd37e66747b23925b587dedaabd972671678ffefceb6d77eea0619da90081beed6cceee9180f03acf71a9c020530302474b7009e41106e625a0000000000000000002e0000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000003b57ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffef00000000000f42450000000000000000000000000000000000000000000000000000000000000000000000000000000065a57c5f0003e665b94487677780201e0947900827290aec8965afabb16ebc3cb9b408ebae71b618d78788bc80d09843593cac98da490aec8965afabb16ebc3cb9b408ebae71b618d78788bc80d09843593cac98da4001f040901efe92001c0332dca2365b9de400101df990358df40500000000000000000000000000000000000000000000000000000000000000009f000000000007a12007c7d7e0395be8f64c6afbff3dd10d8ba6707790ac9670d540f37a9448b0337baa6a5a92acac02827b26357dff9ee886c5d3383bc8564b386aa079bd4a245819bdd5352d4956564f000000000003d090184858601035040b9010350407f0082725828aa96d32f676df2049228847cc42b15b0a52d9ffa3bc66b36fcedeff20af9606c97d0dc8b63016a6afd67b2fe52cd0e02a3dcdf71f331cf1e8006ca8e51a303af7000000000000000000000000000000000000000000000000000000000000000000000000000f42439440e0c397a3b2ee3cbd81ea78021862f892a5a9e779d1fecd55236d6571eb5200000000000f424165a57c5f00014088081820101a092008272310a1de222ce584cb4e86613b11d31616e9eaa4ff9ee499f19b104cf71eb504f606c97d0dc8b63016a6afd67b2fe52cd0e02a3dcdf71f331cf1e8006ca8e51a3020f0409017d7840181183b7009a27c89c400000000000000000030000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000002010187880103d8209b008272f1f67337526ee9890e137d5255f3150400e63d15dfe04b26653d275f2d0a6b04d4d2e789fd57af824d33228887af40bcfb492aebe39ec74d665726b6471454ab01036410890103f0209503af704f64c6afbff3dd10d8ba6707790ac9670d540f37a9448b0337baa6a5a92acac00000000000f42410000000000000000000000000000000000000000000000000000000000000000000000000000000065a57c5f00074088a8b8c0101608d008272f1f67337526ee9890e137d5255f3150400e63d15dfe04b26653d275f2d0a6b04bf8229e6e684701825a6f8e1fb0f8155aba759d58b82e73b8405092874e23fa80205203024a7a80201db8e8f020120909101014894010120920101209300ab29fe09ec98d5f7fe7ba21b174ce0ef21592ce1aa81e6f528916066f754d4b52559593fc0000000000000000000000000000000000000000000000000000000000000001017d784000000000000001e8484cb4af8be40008be7f827b26357dff9ee886c5d3383bc8564b386aa079bd4a245819bdd5352d495656240e8cae6e880eb79a200000000000f424365a57c5f00012195b1b1bcb081ddbdc9b1908600ab29fe09ec98d5f7fe7ba21b174ce0ef21592ce1aa81e6f528916066f754d4b52559593ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffbd01efe92000000000000001e8488cb4af8be4003af704f64c6afbff3dd10d8ba6707790ac9670d540f37a9448b0337baa6a5a92acac00000000000f4247d59d160ea9c63d10edae2c76ada993f982915bc75f7bcc0fb81280820d15523200000000000f424165a57c5f00014089697980101a099008272bf8229e6e684701825a6f8e1fb0f8155aba759d58b82e73b8405092874e23fa816617de1e9f3a79e4c7d8a3a51ac0c8b54b6bbcd4f824c9f800aa2f36fee2672020f0c0901c9c38018119ab700b959ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffdf3fc13d931abeffcf744362e99c1de42b259c35503cdea5122c0cdeea9a96a4ab2b101c9c380006cb73bc00000000001e848ccb4af8be7fffffffc0009c402468bb800000000000000000040000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000003af704f64c6afbff3dd10d8ba6707790ac9670d540f37a9448b0337baa6a5a92acac00000000000f424873b47eb527e2e6b9a73405d88f31477703ef31e5c4f2b8a2be754d66be0d60ff00000000000f424765a57c5f00074089c9d9e0101609f00827216617de1e9f3a79e4c7d8a3a51ac0c8b54b6bbcd4f824c9f800aa2f36fee2672d4d2e789fd57af824d33228887af40bcfb492aebe39ec74d665726b6471454ab0205303024a7a80201dba0a1020120a2a3010148a6010120a4010120a500ab29fe09ec98d5f7fe7ba21b174ce0ef21592ce1aa81e6f528916066f754d4b52559593fc0000000000000000000000000000000000000000000000000000000000000001017d784000000000000001e8492cb4af8be40008be7f827b26357dff9ee886c5d3383bc8564b386aa079bd4a245819bdd5352d495656240e8cae6e880eb79a200000000000f424a65a57c5f00012195b1b1bcb081ddbdc9b1908600ab29fe09ec98d5f7fe7ba21b174ce0ef21592ce1aa81e6f528916066f754d4b52559593ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffbd01efe92000000000000001e8496cb4af8be40009e42664e625a00000000000000000030000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000061c000000000000600000000000719ae84f17b8f8b22026a975ff55f1ab19fde4a768744d2178dfa63bb533e107a40d03c040103d040ac001fb0000000004000000000000000000004020170aeaf01eb50000000000000000800000000000000000000000000000005a2f25be03d373e41b62e16fb0d999a97235b40ea71f1522a5fa982a817ea60fd265bcbc973e584eb1eab6b3ea65309a0a32cec006e1bee4f781214122612c4d000000000040000000000000007fffffff800000004cb4af986000000c9ad00030020020161b0b1020161b8b90106460600b503af7333333333333333333333333333333333333333333333333333333333333333300000000000f4242cac3c5723cb68f5f7dba257e7f812140298ea360bdbf630cd33c8da2b098829200000000000f424165a57c5f0001408b2b3b40101a0b50082729304b9982993165dc7b4b2ccfc64c61bf6455afe7e4fe1233d5f827261360d8ed855aea1ede2adcff55d3d37fcf59c03bd19c39f843a14faef85cad3980cf692020f04091954fc401811b6b700ab69fe00000000000000000000000000000000000000000000000000000000000000013fccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccd1954fc4000000000000001e8480cb4af8be40009e41778c0a604000000000000000003e00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000005bc00000000000000000000000012d452da449e50b8cf7dd27861f146122afe1b546bb8b70fc8216f0c614139f8e040106460600bd03af7000000000000000000000000000000000000000000000000000000000000000000000000000f42410000000000000000000000000000000000000000000000000000000000000000000000000000000065a57c5f0001408babbbc0101a0bd0082725828aa96d32f676df2049228847cc42b15b0a52d9ffa3bc66b36fcedeff20af9310a1de222ce584cb4e86613b11d31616e9eaa4ff9ee499f19b104cf71eb504f0113040829a40cd41efffe02be01a369fe00000000000000000000000000000000000000000000000000000000000000013fc000000000000000000000000000000000000000000000000000000000000000020000000000001e8480cb4af8be40be020120bfc00015be000003bcb3670dc155500015bfffffffbcbd1a94a2001051df4184")
                        .endCell();
        log.info("CellType {}", c.getCellType());
        Block block = Block.deserialize(CellSlice.beginParse(c));
        log.info(
                "inMsg {}, outMsg {}, account blocks {}, block {}",
                block.getExtra().getInMsgDesc().getCount(),
                block.getExtra().getOutMsgDesc().getCount(),
                block.getExtra().getShardAccountBlocks().elements.size(),
                block);

        BlockPrintInfo.printAllTransactions(block);
        BlockPrintInfo.printAllMessages(block);
    }
}
