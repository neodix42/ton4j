import com.iwebpp.crypto.TweetNaclFast;
import java.util.Collections;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.ton.java.address.Address;
import org.ton.java.cell.Cell;
import org.ton.java.emulator.EmulateTransactionResult;
import org.ton.java.emulator.tx.TxEmulator;
import org.ton.java.emulator.tx.TxEmulatorConfig;
import org.ton.java.emulator.tx.TxVerbosityLevel;
import org.ton.java.smartcontract.*;
import org.ton.java.smartcontract.SmartContractCompiler;
import org.ton.java.smartcontract.types.Destination;
import org.ton.java.smartcontract.types.WalletV5Config;
import org.ton.java.smartcontract.wallet.v5.WalletV5;
import org.ton.java.tlb.types.Message;
import org.ton.java.tlb.types.StateInit;
import org.ton.java.tonlib.Tonlib;
import org.ton.java.tonlib.types.BlockIdExt;
import org.ton.java.utils.Utils;

@Slf4j
@RunWith(JUnit4.class)
public class DevEnvTest {

  @Test
  public void testCompileContract() {
    SmartContractCompiler smcFunc =
            SmartContractCompiler.builder().contractAsResource("/simple.fc").build();
    Cell codeCell = smcFunc.compileToCell();
    log.info("codeCell {}", codeCell.print());
  }

  @Test
  public void testTonlibGetLastBlock() {
    Tonlib tonlib = Tonlib.builder().testnet(true).build();
    BlockIdExt block = tonlib.getLast().getLast();
    log.info("block {}", block);
  }

  @Test
  public void testTxEmulatorWalletV5ExternalMsgSimplified() {

    TxEmulator txEmulator =
            TxEmulator.builder()
                    .configType(TxEmulatorConfig.TESTNET)
                    .verbosityLevel(TxVerbosityLevel.UNLIMITED)
                    .build();

    SmartContractCompiler smcFunc =
            SmartContractCompiler.builder().contractAsResource("/new-wallet-v5.fc").build();

    Cell codeCell = smcFunc.compileToCell();

    byte[] publicKey =
            Utils.hexToSignedBytes("82A0B2543D06FEC0AAC952E9EC738BE56AB1B6027FC0C1AA817AE14B4D1ED2FB");
    byte[] secretKey =
            Utils.hexToSignedBytes("F182111193F30D79D517F2339A1BA7C25FDF6C52142F0F2C1D960A1F1D65E1E4");
    TweetNaclFast.Signature.KeyPair keyPair = TweetNaclFast.Signature.keyPair_fromSeed(secretKey);

    WalletV5 walletV5 =
            WalletV5.builder()
                    .keyPair(keyPair)
                    .isSigAuthAllowed(false)
                    .initialSeqno(0)
                    .walletId(42)
                    .build();

    Cell dataCell = walletV5.createDataCell();

    log.info("codeCellHex {}", codeCell.toHex());
    log.info("dataCellHex {}", dataCell.toHex());

    StateInit walletV5StateInit = StateInit.builder().code(codeCell).data(dataCell).build();

    Address address = walletV5StateInit.getAddress();
    log.info("addressRaw {}", address.toRaw());
    log.info("addressBounceable {}", address.toBounceable());

    String rawDummyDestinationAddress =
            "0:258e549638a6980ae5d3c76382afd3f4f32e34482dafc3751e3358589c8de00d";

    WalletV5Config walletV5Config =
            WalletV5Config.builder()
                    .seqno(0)
                    .walletId(42)
                    .body(
                            walletV5
                                    .createBulkTransfer(
                                            Collections.singletonList(
                                                    Destination.builder()
                                                            .bounce(false)
                                                            .address(rawDummyDestinationAddress)
                                                            .amount(Utils.toNano(1))
                                                            .build()))
                                    .toCell())
                    .build();

    Message extMsg = walletV5.prepareExternalMsg(walletV5Config);

    EmulateTransactionResult result =
            txEmulator.emulateTransaction(
                    codeCell, dataCell, Utils.toNano(2), extMsg.toCell().toBase64());

    log.info("result sendExternalMessage[1]: {}", result);
    result.getTransaction().printTransactionFees(true);
  }
}
