package org.ton.ton4j.smartcontract.unittests;

import com.iwebpp.crypto.TweetNaclFast;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.ton.ton4j.cell.CellBuilder;
import org.ton.ton4j.fift.FiftRunner;
import org.ton.ton4j.func.FuncRunner;
import org.ton.ton4j.smartcontract.GenericSmartContract;
import org.ton.ton4j.smartcontract.SmartContractCompiler;
import org.ton.ton4j.tolk.TolkRunner;
import org.ton.ton4j.utils.Utils;

@Slf4j
@RunWith(JUnit4.class)
public class TestSmartContractCompiler {
  /**
   * Make sure you have fift and func installed in your system. See <a
   * href="https://github.com/ton-blockchain/packages">packages</a> for instructions. Example is
   * based on new-wallet-v4r2.fc smart contract. You can specify path to any smart contract.
   */
  static String funcPath = Utils.getFuncGithubUrl();

  static String fiftPath = Utils.getFiftGithubUrl();
  static String tolkPath = Utils.getTolkGithubUrl();

  @Test
  public void testSmartContractCompiler() {
    SmartContractCompiler smcFunc =
        SmartContractCompiler.builder()
            .contractAsResource("contracts/wallets/new-wallet-v4r2.fc")
            .funcRunner(FuncRunner.builder().funcExecutablePath(funcPath).build())
            .fiftRunner(FiftRunner.builder().fiftExecutablePath(fiftPath).build())
            .tolkRunner(TolkRunner.builder().tolkExecutablePath(tolkPath).build())
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

    GenericSmartContract smc =
        GenericSmartContract.builder().keyPair(keyPair).code(codeCellHex).data(dataCellHex).build();

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
            .contractAsResource("contracts/wallets/new-wallet-v5.fc")
            .funcRunner(FuncRunner.builder().funcExecutablePath(funcPath).build())
            .fiftRunner(FiftRunner.builder().fiftExecutablePath(fiftPath).build())
            .tolkRunner(TolkRunner.builder().tolkExecutablePath(tolkPath).build())
            .build();

    String codeCellHex = smcFunc.compile();

    log.info("codeCellHex {}", codeCellHex);
  }

  @Test
  public void testLibraryDeployerCompiler() {
    SmartContractCompiler smcFunc =
        SmartContractCompiler.builder()
            .contractPath("G:/smartcontracts/library-deployer.fc")
            .funcRunner(FuncRunner.builder().funcExecutablePath(funcPath).build())
            .fiftRunner(FiftRunner.builder().fiftExecutablePath(fiftPath).build())
            .tolkRunner(TolkRunner.builder().tolkExecutablePath(tolkPath).build())
            .build();

    String codeCellHex = smcFunc.compile();

    log.info("codeCellHex {}", codeCellHex);
  }

  @Test
  public void testSmartContractCompilerFuncSimple() {
    SmartContractCompiler smcFunc =
        SmartContractCompiler.builder()
            .contractAsResource("simple.fc")
            .printFiftAsmOutput(true)
            .funcRunner(FuncRunner.builder().funcExecutablePath(funcPath).build())
            .fiftRunner(FiftRunner.builder().fiftExecutablePath(fiftPath).build())
            .tolkRunner(TolkRunner.builder().tolkExecutablePath(tolkPath).build())
            .build();
    smcFunc.compile();
  }

  @Test
  public void testSmartContractCompilerTolkSimple() {
    SmartContractCompiler smcTolk =
        SmartContractCompiler.builder()
            .contractAsResource("simple.tolk")
            .funcRunner(FuncRunner.builder().funcExecutablePath(funcPath).build())
            .fiftRunner(FiftRunner.builder().fiftExecutablePath(fiftPath).build())
            .tolkRunner(TolkRunner.builder().tolkExecutablePath(tolkPath).build())
            .printFiftAsmOutput(true)
            .build();
    smcTolk.compile();
  }
}
