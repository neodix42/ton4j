package org.ton.java.smartcontract.integrationtests;

import com.iwebpp.crypto.TweetNaclFast;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.ton.java.address.Address;
import org.ton.java.cell.CellBuilder;
import org.ton.java.smartcontract.TestFaucet;
import org.ton.java.smartcontract.types.CustomContractConfig;
import org.ton.java.smartcontract.wallet.Options;
import org.ton.java.tlb.types.Message;
import org.ton.java.tonlib.Tonlib;
import org.ton.java.tonlib.types.ExtMessageInfo;
import org.ton.java.tonlib.types.RunResult;
import org.ton.java.tonlib.types.TvmStackEntryNumber;
import org.ton.java.tonlib.types.VerbosityLevel;
import org.ton.java.utils.Utils;

import java.math.BigInteger;
import java.time.Instant;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

@Slf4j
@RunWith(JUnit4.class)
public class TestCustomContract {

    @Test
    public void testCustomContract() throws InterruptedException {
        // echo "F182111193F30D79D517F2339A1BA7C25FDF6C52142F0F2C1D960A1F1D65E1E4" | xxd -r -p - > new-wallet.pk
        //byte[] secretKey = Utils.hexToBytes("F182111193F30D79D517F2339A1BA7C25FDF6C52142F0F2C1D960A1F1D65E1E4");
        //TweetNaclFast.Signature.KeyPair keyPair = Utils.generateSignatureKeyPairFromSeed(secretKey);

        Tonlib tonlib = Tonlib.builder()
                .testnet(true)
                .ignoreCache(false)
                .verbosityLevel(VerbosityLevel.DEBUG)
                .build();

        TweetNaclFast.Signature.KeyPair keyPair = Utils.generateSignatureKeyPair();

        Options options = Options.builder()
                .publicKey(keyPair.getPublicKey())
                .secretKey(keyPair.getSecretKey())
                .wc(0L)
                .build();

        log.info("pubkey {}", Utils.bytesToHex(options.publicKey));

        CustomContract customContract = new CustomContract(options);

        Message msg = customContract.createExternalMessage(customContract.getAddress(),
                true,
                CellBuilder.beginCell()
                        .storeUint(0, 32) // seqno
                        .storeUint(Instant.now().getEpochSecond() + 5 * 60L, 32)  //valid-until
                        .storeUint(0, 64) //extra-field
                        .endCell());
        Address address = msg.getInit().getAddress();

        log.info("Creating new wallet in workchain {}\nStateInit: {}\nnew wallet address = {}\n(Saving address to file new-wallet.addr)\nNon-bounceable address (for init): {}\nBounceable address (for later access): {}\nsigning message: {}\nExternal message for initialization is {}\n{}\n(Saved wallet creating query to file new-wallet-query.boc)\n"
                , options.wc, msg.getInit().toCell().print(), address.toString(false), address.toString(true, true, false, true), address.toString(true, true, true, true), msg.getBody().print(), msg.getBody().print(), Utils.bytesToHex(msg.toCell().toBoc()).toUpperCase());

        // top up new wallet using test-faucet-wallet
        BigInteger balance = TestFaucet.topUpContract(tonlib, Address.of(address.toString(true)), Utils.toNano(0.1));
        log.info("new wallet {} balance: {}", address.toString(true), Utils.formatNanoValue(balance));

        String base64boc = msg.toCell().toBase64();
        log.info(base64boc);
        ExtMessageInfo resultRawMsg = tonlib.sendRawMessage(base64boc); // deploy
        log.info("body_hash {}, error {}", resultRawMsg.getBody_hash(), resultRawMsg.getError().getCode());

        Utils.sleep(30, "deploying");

        RunResult result = tonlib.runMethod(address, "seqno");
        log.info("gas_used {}, exit_code {} ", result.getGas_used(), result.getExit_code());
        TvmStackEntryNumber seqno = (TvmStackEntryNumber) result.getStack().get(0);
        log.info("seqno: {}", seqno.getNumber());

        result = tonlib.runMethod(address, "get_x_data");
        log.info("gas_used {}, exit_code {} ", result.getGas_used(), result.getExit_code());
        TvmStackEntryNumber x_data = (TvmStackEntryNumber) result.getStack().get(0);
        log.info("x_data: {}", x_data.getNumber());

        result = tonlib.runMethod(address, "get_extra_field");
        log.info("gas_used {}, exit_code {} ", result.getGas_used(), result.getExit_code());
        TvmStackEntryNumber extra_field = (TvmStackEntryNumber) result.getStack().get(0);
        log.info("extra_field: {}", extra_field.getNumber());

        Address destinationAddress = Address.of("kf_sPxv06KagKaRmOOKxeDQwApCx3i8IQOwv507XD51JOLka");

        CustomContractConfig config = CustomContractConfig.builder()
                .seqno(1)
                .destination(destinationAddress)
                .amount(Utils.toNano(0.05))
                .extraField(42)
                .comment("no-way")
                .build();

        ExtMessageInfo extMessageInfo = customContract.sendTonCoins(tonlib, config);
        assertThat(extMessageInfo.getError().getCode()).isZero();

        Utils.sleep(5);

        result = tonlib.runMethod(address, "get_extra_field");
        log.info("gas_used {}, exit_code {} ", result.getGas_used(), result.getExit_code());
        extra_field = (TvmStackEntryNumber) result.getStack().get(0);
        log.info("extra_field: {}", extra_field.getNumber());

    }
}
