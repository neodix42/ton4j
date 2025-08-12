package org.ton.ton4j.smartcontract.integrationtests;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

import com.iwebpp.crypto.TweetNaclFast;
import java.math.BigInteger;
import java.util.ArrayList;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.ton.ton4j.adnl.AdnlLiteClient;
import org.ton.ton4j.address.Address;
import org.ton.ton4j.smartcontract.faucet.TestnetFaucet;
import org.ton.ton4j.smartcontract.types.CustomContractConfig;
import org.ton.ton4j.tl.liteserver.responses.RunMethodResult;
import org.ton.ton4j.toncenter.TonCenter;
import org.ton.ton4j.toncenter.TonResponse;
import org.ton.ton4j.toncenter.model.RunGetMethodResponse;
import org.ton.ton4j.tonlib.types.ExtMessageInfo;
import org.ton.ton4j.tonlib.types.RunResult;
import org.ton.ton4j.tonlib.types.TvmStackEntryNumber;
import org.ton.ton4j.utils.Utils;

@Slf4j
@RunWith(JUnit4.class)
public class TestExampleContract extends CommonTest {

  @Test
  public void testExampleContract() throws InterruptedException {
    // echo "F182111193F30D79D517F2339A1BA7C25FDF6C52142F0F2C1D960A1F1D65E1E4" | xxd -r -p - >
    // new-wallet.pk
    // byte[] secretKey =
    // Utils.hexToBytes("F182111193F30D79D517F2339A1BA7C25FDF6C52142F0F2C1D960A1F1D65E1E4");
    // TweetNaclFast.Signature.KeyPair keyPair = Utils.generateSignatureKeyPairFromSeed(secretKey);

    TweetNaclFast.Signature.KeyPair keyPair = Utils.generateSignatureKeyPair();

    ExampleContract exampleContract =
        ExampleContract.builder().tonlib(tonlib).keyPair(keyPair).build();

    log.info("pubkey {}", Utils.bytesToHex(exampleContract.getKeyPair().getPublicKey()));

    Address address = exampleContract.getAddress();
    log.info("contract address {}", address);

    // top up new wallet using test-faucet-wallet
    BigInteger balance =
        TestnetFaucet.topUpContract(tonlib, Address.of(address.toString(true)), Utils.toNano(0.1));
    log.info("new wallet {} balance: {}", address.toString(true), Utils.formatNanoValue(balance));

    ExtMessageInfo extMessageInfo = exampleContract.deploy();
    assertThat(extMessageInfo.getError().getCode()).isZero();

    exampleContract.waitForDeployment(45);

    log.info("seqno: {}", exampleContract.getSeqno());

    RunResult result = tonlib.runMethod(address, "get_x_data");
    log.info("gas_used {}, exit_code {} ", result.getGas_used(), result.getExit_code());
    TvmStackEntryNumber x_data = (TvmStackEntryNumber) result.getStack().get(0);
    log.info("x_data: {}", x_data.getNumber());

    result = tonlib.runMethod(address, "get_extra_field");
    log.info("gas_used {}, exit_code {} ", result.getGas_used(), result.getExit_code());
    TvmStackEntryNumber extra_field = (TvmStackEntryNumber) result.getStack().get(0);
    log.info("extra_field: {}", extra_field.getNumber());

    Address destinationAddress = Address.of("kf_sPxv06KagKaRmOOKxeDQwApCx3i8IQOwv507XD51JOLka");

    CustomContractConfig config =
        CustomContractConfig.builder()
            .seqno(exampleContract.getSeqno())
            .destination(destinationAddress)
            .amount(Utils.toNano(0.05))
            .extraField(42)
            .comment("no-way")
            .build();

    extMessageInfo = exampleContract.send(config);
    assertThat(extMessageInfo.getError().getCode()).isZero();

    exampleContract.waitForBalanceChange(45);

    result = tonlib.runMethod(address, "get_extra_field");
    log.info("gas_used {}, exit_code {} ", result.getGas_used(), result.getExit_code());
    extra_field = (TvmStackEntryNumber) result.getStack().get(0);
    log.info("extra_field: {}", extra_field.getNumber());

    assertThat(extra_field.getNumber().longValue()).isEqualTo(42);
  }

  @Test
  public void testExampleContractAdnlLiteClient() throws Exception {
    // echo "F182111193F30D79D517F2339A1BA7C25FDF6C52142F0F2C1D960A1F1D65E1E4" | xxd -r -p - >
    // new-wallet.pk
    // byte[] secretKey =
    // Utils.hexToBytes("F182111193F30D79D517F2339A1BA7C25FDF6C52142F0F2C1D960A1F1D65E1E4");
    // TweetNaclFast.Signature.KeyPair keyPair = Utils.generateSignatureKeyPairFromSeed(secretKey);

    AdnlLiteClient adnlLiteClient =
        AdnlLiteClient.builder()
            .configUrl(Utils.getGlobalConfigUrlTestnetGithub())
            .liteServerIndex(0)
            .build();

    TweetNaclFast.Signature.KeyPair keyPair = Utils.generateSignatureKeyPair();

    ExampleContract exampleContract =
        ExampleContract.builder().adnlLiteClient(adnlLiteClient).keyPair(keyPair).build();

    log.info("pubkey {}", Utils.bytesToHex(exampleContract.getKeyPair().getPublicKey()));

    Address address = exampleContract.getAddress();
    log.info("contract address {}", address);

    // top up new wallet using test-faucet-wallet
    BigInteger balance =
        TestnetFaucet.topUpContract(
            adnlLiteClient, Address.of(address.toString(true)), Utils.toNano(0.1));
    log.info("new wallet {} balance: {}", address.toString(true), Utils.formatNanoValue(balance));

    ExtMessageInfo extMessageInfo = exampleContract.deploy();
    assertThat(extMessageInfo.getError().getCode()).isZero();

    exampleContract.waitForDeployment(45);

    log.info("seqno: {}", exampleContract.getSeqno());

    RunMethodResult result = adnlLiteClient.runMethod(address, "get_x_data");

    log.info("x_data: {}", result.getIntByIndex(0));

    result = adnlLiteClient.runMethod(address, "get_extra_field");
    log.info("extra_field: {}", result.getIntByIndex(0));

    Address destinationAddress = Address.of("kf_sPxv06KagKaRmOOKxeDQwApCx3i8IQOwv507XD51JOLka");

    CustomContractConfig config =
        CustomContractConfig.builder()
            .seqno(exampleContract.getSeqno())
            .destination(destinationAddress)
            .amount(Utils.toNano(0.05))
            .extraField(42)
            .comment("no-way")
            .build();

    extMessageInfo = exampleContract.send(config);
    assertThat(extMessageInfo.getError().getCode()).isZero();

    exampleContract.waitForBalanceChange(45);

    result = adnlLiteClient.runMethod(address, "get_extra_field");

    log.info("extra_field: {}", result.getIntByIndex(0));

    assertThat(result.getIntByIndex(0).longValue()).isEqualTo(42);
  }

  @Test
  public void testExampleContractTonCenterClient() throws Exception {

    TonCenter tonCenter = TonCenter.builder().apiKey(TESTNET_API_KEY).testnet().build();

    TweetNaclFast.Signature.KeyPair keyPair = Utils.generateSignatureKeyPair();

    ExampleContract exampleContract =
        ExampleContract.builder().tonCenterClient(tonCenter).keyPair(keyPair).build();

    log.info("pubkey {}", Utils.bytesToHex(exampleContract.getKeyPair().getPublicKey()));

    Address address = exampleContract.getAddress();
    log.info("contract address {}", address);

    // top up new wallet using test-faucet-wallet
    BigInteger balance =
        TestnetFaucet.topUpContract(
            tonCenter, Address.of(address.toString(true)), Utils.toNano(0.1), true);
    log.info("new wallet {} balance: {}", address.toString(true), Utils.formatNanoValue(balance));

    ExtMessageInfo extMessageInfo = exampleContract.deploy();
    assertThat(extMessageInfo.getTonCenterError().getCode()).isZero();

    exampleContract.waitForDeployment();

    log.info("seqno: {}", exampleContract.getSeqno());

    Utils.sleep(2);
    TonResponse<RunGetMethodResponse> result =
        tonCenter.runGetMethod(address.toBounceable(), "get_x_data", new ArrayList<>());

    if (result.isSuccess()) {
      log.info("x_data: {}", result.getResult().getStack().get(0));
    }
    Utils.sleep(2);
    result = tonCenter.runGetMethod(address.toBounceable(), "get_extra_field", new ArrayList<>());
    if (result.isSuccess()) {

      log.info("extra_field: {}", result.getResult().getStack().get(0));
    }

    Address destinationAddress = Address.of("kf_sPxv06KagKaRmOOKxeDQwApCx3i8IQOwv507XD51JOLka");

    Utils.sleep(2);
    CustomContractConfig config =
        CustomContractConfig.builder()
            .seqno(exampleContract.getSeqno())
            .destination(destinationAddress)
            .amount(Utils.toNano(0.05))
            .extraField(42)
            .comment("no-way")
            .build();

    extMessageInfo = exampleContract.send(config);
    assertThat(extMessageInfo.getError().getCode()).isZero();

    exampleContract.waitForBalanceChange(45);

    result = tonCenter.runGetMethod(address.toBounceable(), "get_extra_field", new ArrayList<>());

    log.info("extra_field: {}", result.getResult().getStack().get(0));

    assertThat(Long.parseLong(result.getResult().getStack().get(0).toString())).isEqualTo(42);
  }
}
