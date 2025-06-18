package org.ton.ton4j.smartcontract.integrationtests;

import java.math.BigInteger;
import java.util.Collections;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.ton.java.adnl.AdnlLiteClient;
import org.ton.ton4j.address.Address;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellBuilder;
import org.ton.ton4j.smartcontract.LibraryDeployer;
import org.ton.ton4j.smartcontract.faucet.TestnetFaucet;
import org.ton.ton4j.smartcontract.types.WalletCodes;
import org.ton.ton4j.tl.liteserver.responses.LibraryEntry;
import org.ton.ton4j.tl.liteserver.responses.LibraryResult;
import org.ton.ton4j.tonlib.types.SmcLibraryEntry;
import org.ton.ton4j.tonlib.types.SmcLibraryResult;
import org.ton.ton4j.utils.Utils;

@Slf4j
@RunWith(JUnit4.class)
public class TestLibraryDeployer extends CommonTest {

  /**
   * tops up and deploys library deployer (in master workchain -1) that caries in the
   * StateInit.data() v5r1 code
   */
  @Test
  public void testDeployLibraryDeployer() throws InterruptedException {

    Cell walletV5Code = CellBuilder.beginCell().fromBoc(WalletCodes.V5R1.getValue()).endCell();

    LibraryDeployer libraryDeployer =
        LibraryDeployer.builder().tonlib(tonlib).libraryCode(walletV5Code).build();

    log.info("boc {}", walletV5Code.toHex());

    String nonBounceableAddressLib = libraryDeployer.getAddress().toNonBounceable();
    log.info("nonBounceable addressLib {}", nonBounceableAddressLib);
    log.info("raw address {}", libraryDeployer.getAddress().toRaw());

    BigInteger balanceLib =
        TestnetFaucet.topUpContract(tonlib, Address.of(nonBounceableAddressLib), Utils.toNano(1));
    log.info(
        "new wallet {} balance: {}", libraryDeployer.getName(), Utils.formatNanoValue(balanceLib));
    libraryDeployer.deploy();

    Utils.sleep(
        10,
        "Deployment of LibraryDeployer will never happen. Lite-server will return an error, but the library will be deployed");

    SmcLibraryResult smcLibraryResult =
        tonlib.getLibraries(
            Collections.singletonList(
                Utils.bytesToBase64(walletV5Code.getHash())
                //                "IINLe3KxEhR+Gy+0V7hOdNGjDwT3N9T2KmaOlVLSty8="
                ));
    log.info("smcLibraryResult {}", smcLibraryResult);

    for (SmcLibraryEntry l : smcLibraryResult.getResult()) {
      String cellLibBoc = l.getData();
      Cell lib = Cell.fromBocBase64(cellLibBoc);
      log.info("cell lib {}", lib.toHex());
    }
  }

  @Test
  public void testIfLibraryHasBeenDeployed() {
    Cell walletV5Code = CellBuilder.beginCell().fromBoc(WalletCodes.V5R1.getValue()).endCell();
    SmcLibraryResult smcLibraryResult =
        tonlib.getLibraries(Collections.singletonList(Utils.bytesToBase64(walletV5Code.getHash())));
    //                "IINLe3KxEhR+Gy+0V7hOdNGjDwT3N9T2KmaOlVLSty8="));

    log.info("smcLibraryResult {}", smcLibraryResult);

    for (SmcLibraryEntry l : smcLibraryResult.getResult()) {
      String cellLibBoc = l.getData();
      Cell lib = Cell.fromBocBase64(cellLibBoc);
      log.info("cell lib {}", lib.toHex());
      // x.elements.put(1L, lib);
    }
  }

  @Test
  public void testDeployLibraryDeployerAdnlLiteClient() throws Exception {
    AdnlLiteClient adnlLiteClient =
        AdnlLiteClient.builder()
            .configUrl(Utils.getGlobalConfigUrlTestnetGithub())
            .liteServerIndex(0)
            .build();
    Cell walletV5Code = CellBuilder.beginCell().fromBoc(WalletCodes.V5R1.getValue()).endCell();

    LibraryDeployer libraryDeployer =
        LibraryDeployer.builder().adnlLiteClient(adnlLiteClient).libraryCode(walletV5Code).build();

    log.info("boc {}", walletV5Code.toHex());

    String nonBounceableAddressLib = libraryDeployer.getAddress().toNonBounceable();
    log.info("nonBounceable addressLib {}", nonBounceableAddressLib);
    log.info("raw address {}", libraryDeployer.getAddress().toRaw());

    BigInteger balanceLib =
        TestnetFaucet.topUpContract(
            adnlLiteClient, Address.of(nonBounceableAddressLib), Utils.toNano(1));
    log.info(
        "new wallet {} balance: {}", libraryDeployer.getName(), Utils.formatNanoValue(balanceLib));
    libraryDeployer.deploy();

    Utils.sleep(
        10,
        "Deployment of LibraryDeployer will never happen. Lite-server will return an error, but the library will be deployed");

    LibraryResult smcLibraryResult =
        adnlLiteClient.getLibraries(
            Collections.singletonList(
                walletV5Code.getHash()
                //                "IINLe3KxEhR+Gy+0V7hOdNGjDwT3N9T2KmaOlVLSty8="
                ));
    //
    log.info("smcLibraryResult {}", smcLibraryResult);

    for (LibraryEntry lib : smcLibraryResult.getResult()) {
      Cell libCell = Cell.fromBoc(lib.data);
      log.info("cell lib {}", libCell.toHex());
    }
  }

  @Test
  public void testIfLibraryHasBeenDeployedAdnlLiteClient() throws Exception {
    AdnlLiteClient adnlLiteClient =
        AdnlLiteClient.builder()
            .configUrl(Utils.getGlobalConfigUrlTestnetGithub())
            .liteServerIndex(0)
            .build();
    Cell walletV5Code = CellBuilder.beginCell().fromBoc(WalletCodes.V5R1.getValue()).endCell();

    LibraryResult smcLibraryResult =
        adnlLiteClient.getLibraries(Collections.singletonList(walletV5Code.getHash()));
    log.info("smcLibraryResult {}", smcLibraryResult);

    for (LibraryEntry lib : smcLibraryResult.getResult()) {
      Cell libCell = Cell.fromBoc(lib.data);
      log.info("cell lib {}", libCell.toHex());
    }
  }
}
