package org.ton.java.smartcontract.integrationtests;

import com.iwebpp.crypto.TweetNaclFast;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.ton.java.address.Address;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.smartcontract.GenericSmartContract;
import org.ton.java.smartcontract.SmartContractCompiler;
import org.ton.java.smartcontract.TestFaucet;
import org.ton.java.tonlib.Tonlib;
import org.ton.java.utils.Utils;

import java.io.IOException;
import java.math.BigInteger;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.concurrent.ExecutionException;

@Slf4j
@RunWith(JUnit4.class)
public class TestSmartContractCompiler {
    /**
     * Make sure you have fift and func installed in your system. See <a href="https://github.com/ton-blockchain/packages">packages</a> for instructions.
     * Example is based on new-wallet-v4r2.fc smart contract. You can specify path to any smart contract.
     */
    @Test
    public void testSmartContractCompiler() throws URISyntaxException, InterruptedException, IOException, ExecutionException {
//        URL resource = FuncCompiler.class.getResource("/contracts/stablecoin/contracts/jetton-minter.fc");
        URL resource = SmartContractCompiler.class.getResource("/contracts/wallets/new-wallet-v4r2.fc");
        String contractAbsolutePath = Paths.get(resource.toURI()).toFile().getAbsolutePath();
        SmartContractCompiler smcFunc = SmartContractCompiler.builder()
//                .contractPath("C:/stablecoin/contracts/jetton-minter.fc")
                .contractPath(contractAbsolutePath)
//                .fiftExecutablePath("C:/ProgramData/chocolatey/bin/fift")
//                .funcExecutablePath("C:/ProgramData/chocolatey/bin/func")
//                .fiftAsmLibraryPath("C:/ProgramData/chocolatey/lib/ton/bin/lib")
//                .fiftSmartcontLibraryPath("C:/ProgramData/chocolatey/lib/ton/bin/smartcont")
                .build();

        String codeCellHex = smcFunc.compile();

        TweetNaclFast.Signature.KeyPair keyPair = Utils.generateSignatureKeyPair();

        String dataCellHex = CellBuilder.beginCell()
                .storeUint(0, 32) // seqno
                .storeUint(42, 32) // wallet id
                .storeBytes(keyPair.getPublicKey())
                .storeUint(0, 1) //plugins dict empty
                .endCell()
                .toHex();

        log.info("codeCellHex {}", codeCellHex);
        log.info("dataCellHex {}", dataCellHex);

        Tonlib tonlib = Tonlib.builder()
                .testnet(true)
                .ignoreCache(false)
                .build();

        GenericSmartContract smc = GenericSmartContract.builder()
                .tonlib(tonlib)
                .keyPair(keyPair)
                .code(codeCellHex)
                .data(dataCellHex)
                .build();

        String nonBounceableAddress = smc.getAddress().toNonBounceable();
        String bounceableAddress = smc.getAddress().toBounceable();
        String rawAddress = smc.getAddress().toRaw();

        log.info("non-bounceable address: {}", nonBounceableAddress);
        log.info("    bounceable address: {}", bounceableAddress);
        log.info("    raw address: {}", rawAddress);
        log.info("pub-key {}", Utils.bytesToHex(smc.getKeyPair().getPublicKey()));
        log.info("prv-key {}", Utils.bytesToHex(smc.getKeyPair().getSecretKey()));

        BigInteger balance = TestFaucet.topUpContract(tonlib, Address.of(nonBounceableAddress), Utils.toNano(0.1));
        log.info("new wallet {} balance: {}", smc.getName(), Utils.formatNanoValue(balance));

        Cell deployMessageBody = CellBuilder.beginCell()
                .storeUint(42, 32) // wallet-id
                .storeInt(-1, 32)  // valid-until
                .storeUint(0, 32)  //seqno
                .endCell();

        smc.deploy(deployMessageBody);
        smc.waitForDeployment(60);
    }
}