package org.ton.ton4j.smartcontract.integrationtests;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.ton.ton4j.address.Address;
import org.ton.ton4j.cell.CellBuilder;
import org.ton.ton4j.smartcontract.GenericSmartContract;
import org.ton.ton4j.smartcontract.SendResponse;
import org.ton.ton4j.smartcontract.SmartContractCompiler;
import org.ton.ton4j.smartcontract.faucet.MyLocalTonFaucet;
import org.ton.ton4j.smartcontract.faucet.TestnetFaucet;
import org.ton.ton4j.tolk.TolkRunner;
import org.ton.ton4j.tonlib.Tonlib;
import org.ton.ton4j.tonlib.types.ExtMessageInfo;
import org.ton.ton4j.tonlib.types.RunResult;
import org.ton.ton4j.tonlib.types.TvmStackEntryNumber;
import org.ton.ton4j.utils.Utils;

import java.io.File;
import java.math.BigInteger;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@Slf4j
public class TestSmartContractCompiler extends CommonTest {

  @Test
    public void testSmartContractCompiler() throws InterruptedException, URISyntaxException {
    TolkRunner tolkRunner =
        TolkRunner.builder()
            .tolkExecutablePath(Utils.getTolkGithubUrl())
            .tolkExecutablePath(Utils.getArtifactGithubUrl("tolk", "tolk-1.0.0"))
            .build();
    URL resource = TestSmartContractCompiler.class.getResource("/test-1.0.tolk");
    File fiftFile = Paths.get(resource.toURI()).toFile();
    String absolutePath = fiftFile.getAbsolutePath();

    SmartContractCompiler smartContractCompiler =
        SmartContractCompiler.builder()
            .tolkRunner(tolkRunner)
            .contractPath(absolutePath)
            .build();
    String codeBoc = smartContractCompiler.compile();
    log.info("codeBoc {}", codeBoc);

    String dataCellHex =
        CellBuilder.beginCell()
            .storeUint(4, 32) // id
            .storeUint(5, 32) // counter
            .endCell()
            .toHex();

    log.info("dataBoc {}", dataCellHex);

    GenericSmartContract genericSmartContract =
        GenericSmartContract.builder().code(codeBoc).data(dataCellHex).tonlib(tonlib).build();

    Address address = genericSmartContract.getAddress();
    BigInteger balance = TestnetFaucet.topUpContract(tonlib, address, Utils.toNano(0.1));
    log.info("balance genericSmartContract: {}", Utils.formatNanoValue(balance));

    SendResponse sendResponse =
        genericSmartContract.deployWithoutSignature(
            CellBuilder.beginCell().storeUint(4, 32).endCell());
    log.info("extMessageInfo {}", sendResponse);
    assertThat(sendResponse.getCode()).isZero();

    tonlib.waitForDeployment(address, 60);
    RunResult runResult = tonlib.runMethod(address, "currentCounter");
    long currentCounter =
        ((TvmStackEntryNumber) runResult.getStack().get(0)).getNumber().longValue();

    log.info("walletId {}", currentCounter);
  }

  @Test
  public void testSmartContractCompilerMyLocalTonDocker() throws InterruptedException {

    TolkRunner tolkRunner =
        TolkRunner.builder()
            .tolkExecutablePath(Utils.getTolkGithubUrl())
            .build();

    SmartContractCompiler smartContractCompiler =
        SmartContractCompiler.builder()
            .tolkRunner(tolkRunner)
            .contractAsResource("test-1.0.tolk")
            .build();
    String codeBoc = smartContractCompiler.compile();
    log.info("codeBoc {}", codeBoc);

    String dataCellHex =
        CellBuilder.beginCell()
            .storeUint(6, 32) // id
            .storeUint(7, 32) // counter
            .endCell()
            .toHex();

    log.info("dataBoc {}", dataCellHex);

    Tonlib tonlib =
            Tonlib.builder()
                    .pathToGlobalConfig("http://127.0.0.1:8000/localhost.global.config.json")
                    .pathToTonlibSharedLib(Utils.getTonlibGithubUrl())
                    .ignoreCache(false)
                    .build();

    log.info("capa: {}",tonlib.getConfigParam8());
    GenericSmartContract genericSmartContract =
        GenericSmartContract.builder().code(codeBoc).data(dataCellHex).tonlib(tonlib).build();

    Address address = genericSmartContract.getAddress();
    BigInteger balance = MyLocalTonFaucet.topUpContract(tonlib, address, Utils.toNano(0.1));
    log.info("balance genericSmartContract: {}", Utils.formatNanoValue(balance));

    SendResponse sendResponse =
        genericSmartContract.deployWithoutSignature(
            CellBuilder.beginCell().storeUint(6, 32).endCell());
    log.info("extMessageInfo {}", sendResponse);
    assertThat(sendResponse.getCode()).isZero();

    tonlib.waitForDeployment(address, 60);
    RunResult runResult = tonlib.runMethod(address, "currentCounter");
    long currentCounter =
        ((TvmStackEntryNumber) runResult.getStack().get(0)).getNumber().longValue();

    log.info("walletId {}", currentCounter);
  }
}
