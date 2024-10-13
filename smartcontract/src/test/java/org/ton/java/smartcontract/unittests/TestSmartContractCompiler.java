package org.ton.java.smartcontract.unittests;

import com.iwebpp.crypto.TweetNaclFast;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.ton.java.cell.CellBuilder;
import org.ton.java.smartcontract.GenericSmartContract;
import org.ton.java.smartcontract.SmartContractCompiler;
import org.ton.java.tonlib.Tonlib;
import org.ton.java.utils.Utils;

@Slf4j
@RunWith(JUnit4.class)
public class TestSmartContractCompiler {
  /**
   * Make sure you have fift and func installed in your system. See <a
   * href="https://github.com/ton-blockchain/packages">packages</a> for instructions. Example is
   * based on new-wallet-v4r2.fc smart contract. You can specify path to any smart contract.
   */
  @Test
  public void testSmartContractCompiler() {
    SmartContractCompiler smcFunc =
        SmartContractCompiler.builder()
            .contractAsResource("/contracts/wallets/new-wallet-v4r2.fc")
            .build();

    String codeCellHex = smcFunc.compile();

    TweetNaclFast.Signature.KeyPair keyPair = Utils.generateSignatureKeyPair();

    String dataCellHex =
        CellBuilder.beginCell()
            .storeUint(0, 32) // seqno
            .storeUint(42, 32) // wallet id
            .storeBytes(keyPair.getPublicKey())
            .storeUint(0, 1) // plugins dict empty
            .endCell()
            .toHex();

    log.info("codeCellHex {}", codeCellHex);
    log.info("dataCellHex {}", dataCellHex);

    Tonlib tonlib = Tonlib.builder().testnet(true).ignoreCache(false).build();

    GenericSmartContract smc =
        GenericSmartContract.builder()
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
    //
    //        BigInteger balance = TestnetFaucet.topUpContract(tonlib,
    // Address.of(nonBounceableAddress), Utils.toNano(0.1));
    //        log.info("new wallet {} balance: {}", smc.getName(), Utils.formatNanoValue(balance));
    //
    //        Cell deployMessageBody = CellBuilder.beginCell()
    //                .storeUint(42, 32) // wallet-id
    //                .storeInt(-1, 32)  // valid-until
    //                .storeUint(0, 32)  //seqno
    //                .endCell();
    //
    //        smc.deploy(deployMessageBody);
  }

  @Test
  public void testWalletV5Compiler() {
    SmartContractCompiler smcFunc =
        SmartContractCompiler.builder()
            .contractAsResource("/contracts/wallets/new-wallet-v5.fc")
            .build();

    String codeCellHex = smcFunc.compile();

    log.info("codeCellHex {}", codeCellHex);
  }

  @Test
  public void testLibraryDeployerCompiler() {
    SmartContractCompiler smcFunc =
        SmartContractCompiler.builder()
            .contractAsResource("/contracts/wallets/library-deployer.fc")
            .build();

    String codeCellHex = smcFunc.compile();

    log.info("codeCellHex {}", codeCellHex);
  }
}
