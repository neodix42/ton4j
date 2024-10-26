import static org.assertj.core.api.Assertions.assertThat;

import com.iwebpp.crypto.TweetNaclFast;
import java.math.BigInteger;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.ton.java.address.Address;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.emulator.EmulateTransactionResult;
import org.ton.java.emulator.tvm.TvmEmulator;
import org.ton.java.emulator.tvm.TvmVerbosityLevel;
import org.ton.java.emulator.tx.TxEmulator;
import org.ton.java.emulator.tx.TxEmulatorConfig;
import org.ton.java.emulator.tx.TxVerbosityLevel;
import org.ton.java.smartcontract.*;
import org.ton.java.smartcontract.GenericSmartContract;
import org.ton.java.smartcontract.SmartContractCompiler;
import org.ton.java.smartcontract.faucet.TestnetFaucet;
import org.ton.java.smartcontract.utils.MsgUtils;
import org.ton.java.tlb.types.Message;
import org.ton.java.tlb.types.ShardAccount;
import org.ton.java.tlb.types.StateInit;
import org.ton.java.tonlib.Tonlib;
import org.ton.java.tonlib.types.BlockIdExt;
import org.ton.java.utils.Utils;

@Slf4j
@RunWith(JUnit4.class)
public class DevEnvTest {

  Cell codeCell;
  Cell dataCell;
  StateInit stateInit;

  TweetNaclFast.Signature.KeyPair keyPair = Utils.generateSignatureKeyPair();
  Address dummyAddress = Address.of("EQAyjRKDnEpTBNfRHqYdnzGEQjdY4KG3gxgqiG3DpDY46u8G");

  @Test
  public void testCompileSmartContract() {
    SmartContractCompiler smcFunc =
            SmartContractCompiler.builder().contractAsResource("/simple.fc").build();

    assertThat(smcFunc.compile()).isNotNull();
  }

  @Test
  public void testSendExternalMessageInEmulator() {

    compile();

    TxEmulator txEmulator =
            TxEmulator.builder()
                    .configType(TxEmulatorConfig.TESTNET)
                    .verbosityLevel(TxVerbosityLevel.UNLIMITED)
                    .build();

    txEmulator.setDebugEnabled(true);

    double initialBalanceInToncoins = 0.1;

    Cell bodyCell =
            CellBuilder.beginCell()
                    .storeUint(0, 32) // seqno
                    .endCell();

    Message extMsg = MsgUtils.createExternalMessage(dummyAddress, null, bodyCell);

    EmulateTransactionResult result =
            txEmulator.emulateTransaction(
                    codeCell, dataCell, Utils.toNano(initialBalanceInToncoins), extMsg.toCell().toBase64());

    ShardAccount newShardAccount = result.getNewShardAccount();
    log.info("result sendExternalMessage[1]: {}", result);
    result.getTransaction().printTransactionFees(true, true);
    result.getTransaction().printAllMessages(true);
    log.info("end balance after #1 tx: {}", Utils.formatNanoValue(newShardAccount.getBalance().toString()));

    bodyCell =
            CellBuilder.beginCell()
                    .storeUint(1, 32) // increased seqno
                    .endCell();

    extMsg = MsgUtils.createExternalMessage(dummyAddress, null, bodyCell);

    result = txEmulator.emulateTransaction(newShardAccount.toCell().toBase64(), extMsg.toCell().toBase64());
    result.getTransaction().printTransactionFees(true, true);
    result.getTransaction().printAllMessages(true);
    log.info("end balance after #2 tx: {}", Utils.formatNanoValue(result.getNewShardAccount().getBalance().toString()));

    TvmEmulator tvmEmulator =
            TvmEmulator.builder()
                    .codeBoc(codeCell.toBase64())
                    .dataBoc(result.getNewStateInit().getData().toBase64())
                    .verbosityLevel(TvmVerbosityLevel.UNLIMITED)
                    .build();

    log.info("updated seqno {}",tvmEmulator.runGetSeqNo());
    assertThat(tvmEmulator.runGetSeqNo()).isEqualTo(2);
  }

  @Test
  public void testNewContractInEmulatorSendInternalMessage() {

    compile();

    TxEmulator txEmulator =
            TxEmulator.builder()
                    .configType(TxEmulatorConfig.TESTNET)
                    .verbosityLevel(TxVerbosityLevel.UNLIMITED)
                    .build();

    txEmulator.setDebugEnabled(true);

    double initialBalanceInToncoins = 1;

    Cell bodyCell =
            CellBuilder.beginCell()
                    .storeUint(3, 32) // seqno
                    .endCell();

    Message internalMessageMsg =
            MsgUtils.createInternalMessageWithSourceAddress(dummyAddress, stateInit.getAddress(), Utils.toNano(0.1), null, bodyCell, false);

    EmulateTransactionResult result =
            txEmulator.emulateTransaction(
                    codeCell,
                    dataCell,
                    Utils.toNano(initialBalanceInToncoins),
                    internalMessageMsg.toCell().toBase64());

    log.info("result {}", result);

    result.getTransaction().printTransactionFees(true, true);
    result.getTransaction().printAllMessages(true);

    assertThat(result.isSuccess()).isTrue();


    TvmEmulator tvmEmulator =
            TvmEmulator.builder()
                    .codeBoc(codeCell.toBase64())
                    .dataBoc(result.getNewStateInit().getData().toBase64())
                    .verbosityLevel(TvmVerbosityLevel.UNLIMITED)
                    .build();

    log.info("updated seqno {}",tvmEmulator.runGetSeqNo());
    assertThat(tvmEmulator.runGetSeqNo()).isEqualTo(3);
  }

  @Test
  public void testDeployContractInTestnet() throws InterruptedException {

    compile();

    Tonlib tonlib = Tonlib.builder().testnet(true).ignoreCache(false).build();

    GenericSmartContract smc =
            GenericSmartContract.builder()
                    .tonlib(tonlib)
                    .keyPair(keyPair)
                    .code(codeCell.toHex())
                    .data(dataCell.toHex())
                    .build();

    double initialBalanceInToncoins = 0.1;

    BigInteger balance =
            TestnetFaucet.topUpContract(
                    tonlib, Address.of(smc.getAddress().toNonBounceableTestnet()), Utils.toNano(initialBalanceInToncoins));
    log.info("new wallet balance {}", Utils.formatNanoValue(balance));

    Cell deployMessageBody =
            CellBuilder.beginCell()
                    .storeUint(0, 32) // seqno
                    .endCell();

    assertThat(smc.deployWithoutSignature(deployMessageBody)).isNotNull();
    smc.waitForDeployment(20);
  }

  @Test
  public void testTonlibGetLastBlock() {
    Tonlib tonlib = Tonlib.builder().testnet(true).build();
    BlockIdExt block = tonlib.getLast().getLast();
    log.info("block {}", block);
  }

  private void compile() {
    SmartContractCompiler smcFunc =
            SmartContractCompiler.builder().contractAsResource("/simple.fc").build();

    codeCell = smcFunc.compileToCell();

    dataCell =
            CellBuilder.beginCell()
                    .storeUint(0, 32) // seqno
                    .endCell();

    GenericSmartContract smc =
            GenericSmartContract.builder().keyPair(keyPair).code(codeCell.toHex()).data(dataCell.toHex()).build();

    String nonBounceableAddress = smc.getAddress().toNonBounceable();
    String bounceableAddress = smc.getAddress().toBounceable();
    String rawAddress = smc.getAddress().toRaw();

    log.info("non-bounceable address: {}", nonBounceableAddress);
    log.info("    bounceable address: {}", bounceableAddress);
    log.info("    raw address: {}", rawAddress);
    log.info("pub-key {}", Utils.bytesToHex(smc.getKeyPair().getPublicKey()));
    log.info("prv-key {}", Utils.bytesToHex(smc.getKeyPair().getSecretKey()));

    stateInit = StateInit.builder().code(codeCell).data(dataCell).build();
  }
}
