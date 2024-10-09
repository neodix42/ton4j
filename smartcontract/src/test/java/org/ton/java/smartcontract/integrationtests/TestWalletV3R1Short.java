package org.ton.java.smartcontract.integrationtests;

import java.math.BigInteger;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.ton.java.smartcontract.TestFaucet;
import org.ton.java.smartcontract.wallet.v3.WalletV3R1;
import org.ton.java.utils.Utils;

@Slf4j
@RunWith(JUnit4.class)
public class TestWalletV3R1Short extends CommonTest {


    /*
     * addr - EQA-XwAkPLS-i4s9_N5v0CXGVFecw7lZV2rYeXDAimuWi9zI
     * pub key - 2c188d86ba469755554baad436663b8073145b29f117550432426c513e7c582a
     * prv key - c67cf48806f08929a49416ebebd97078100540ac8a3283646222b4d958b3e9e22c188d86ba469755554baad436663b8073145b29f117550432426c513e7c582a
     */
    @Test
    public void testWalletV3R1() throws InterruptedException {
        WalletV3R1 contract = WalletV3R1.builder()
                .tonlib(tonlib)
                .walletId(42)
                .build();
        log.info("pub key: {}", Utils.bytesToHex(contract.getKeyPair().getPublicKey()));
        log.info("prv key: {}", Utils.bytesToHex(contract.getKeyPair().getSecretKey()));

        BigInteger balance = TestFaucet.topUpContract(tonlib, contract.getAddress(), Utils.toNano(1));
        log.info("walletId {} new wallet {} balance: {}", contract.getWalletId(), contract.getName(), Utils.formatNanoValue(balance));

        contract.deploy();
        contract.waitForDeployment(60);
    }
}
