package org.ton.java.smartcontract.integrationtests;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

import com.iwebpp.crypto.TweetNaclFast;
import java.math.BigInteger;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.ton.java.address.Address;
import org.ton.java.smartcontract.faucet.TestnetFaucet;
import org.ton.java.smartcontract.types.CustomContractConfig;
import org.ton.java.tonlib.Tonlib;
import org.ton.java.tonlib.types.ExtMessageInfo;
import org.ton.java.tonlib.types.RunResult;
import org.ton.java.tonlib.types.TvmStackEntryNumber;
import org.ton.java.utils.Utils;

@Slf4j
@RunWith(JUnit4.class)
public class TestExampleContract {

  @Test
  public void testExampleContract() throws InterruptedException {
    // echo "F182111193F30D79D517F2339A1BA7C25FDF6C52142F0F2C1D960A1F1D65E1E4" | xxd -r -p - >
    // new-wallet.pk
    // byte[] secretKey =
    // Utils.hexToBytes("F182111193F30D79D517F2339A1BA7C25FDF6C52142F0F2C1D960A1F1D65E1E4");
    // TweetNaclFast.Signature.KeyPair keyPair = Utils.generateSignatureKeyPairFromSeed(secretKey);

    Tonlib tonlib = Tonlib.builder().testnet(true).ignoreCache(false).build();

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
}
