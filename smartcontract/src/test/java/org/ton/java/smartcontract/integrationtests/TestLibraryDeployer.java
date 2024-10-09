package org.ton.java.smartcontract.integrationtests;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.ton.java.address.Address;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.smartcontract.LibraryDeployer;
import org.ton.java.smartcontract.TestFaucet;
import org.ton.java.smartcontract.types.WalletCodes;
import org.ton.java.tonlib.types.SmcLibraryEntry;
import org.ton.java.tonlib.types.SmcLibraryResult;
import org.ton.java.utils.Utils;

import java.math.BigInteger;
import java.util.Collections;

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
        TestFaucet.topUpContract(tonlib, Address.of(nonBounceableAddressLib), Utils.toNano(1));
    log.info(
        "new wallet {} balance: {}", libraryDeployer.getName(), Utils.formatNanoValue(balanceLib));
    libraryDeployer.deploy();
    //
    Utils.sleep(
        10,
        "deployment of LibraryDeployer will never happen an Lite-server will return an error, but the library will be deployed");

    SmcLibraryResult smcLibraryResult =
        tonlib.getLibraries(
            Collections.singletonList(
                Utils.bytesToBase64(walletV5Code.getHash())
                //                "IINLe3KxEhR+Gy+0V7hOdNGjDwT3N9T2KmaOlVLSty8="
                ));
    //
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
        tonlib.getLibraries(
            Collections.singletonList(
                Utils.bytesToBase64(walletV5Code.getHash())
                //                "IINLe3KxEhR+Gy+0V7hOdNGjDwT3N9T2KmaOlVLSty8="
                ));
    //
    log.info("smcLibraryResult {}", smcLibraryResult);

    for (SmcLibraryEntry l : smcLibraryResult.getResult()) {
      String cellLibBoc = l.getData();
      Cell lib = Cell.fromBocBase64(cellLibBoc);
      log.info("cell lib {}", lib.toHex());
      // x.elements.put(1L, lib);
    }
  }
}
