package org.ton.java.smartcontract.unittests;

import com.iwebpp.crypto.TweetNaclFast;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.ton.java.address.Address;
import org.ton.java.smartcontract.types.InitExternalMessage;
import org.ton.java.smartcontract.wallet.Options;
import org.ton.java.utils.Utils;

@Slf4j
@RunWith(JUnit4.class)
public class TestCustomContract {

    @Test
    public void testCustomContract() {
        // echo "F182111193F30D79D517F2339A1BA7C25FDF6C52142F0F2C1D960A1F1D65E1E4" | xxd -r -p - > new-wallet.pk
        //byte[] secretKey = Utils.hexToBytes("F182111193F30D79D517F2339A1BA7C25FDF6C52142F0F2C1D960A1F1D65E1E4");
        //TweetNaclFast.Signature.KeyPair keyPair = Utils.generateSignatureKeyPairFromSeed(secretKey);
        TweetNaclFast.Signature.KeyPair keyPair = Utils.generateSignatureKeyPair();

        Options options = Options.builder()
                .publicKey(keyPair.getPublicKey())
                .wc(0L)
                .build();

        log.info("pubkey {}", Utils.bytesToHex(options.publicKey));

        CustomContract customContract = new CustomContract(options);

        InitExternalMessage msg = customContract.createInitExternalMessage(keyPair.getSecretKey());
        Address address = msg.address;

        log.info("Creating new wallet in workchain {}\nStateInit: {}\nnew wallet address = {}\n(Saving address to file new-wallet.addr)\nNon-bounceable address (for init): {}\nBounceable address (for later access): {}\nsigning message: {}\nExternal message for initialization is {}\n{}\n(Saved wallet creating query to file new-wallet-query.boc)\n"
                , options.wc, msg.stateInit.print(), address.toString(false), address.toString(true, true, false, true), address.toString(true, true, true, true), msg.signingMessage.print(), msg.message.print(), Utils.bytesToHex(msg.message.toBoc()).toUpperCase());

        // put a breakpoint and send toincoins to non-bouncelable wallet address, only then deploy smart contract using Tonlib

/* proceed manually
        Tonlib tonlib = Tonlib.builder().build();
        String base64boc = Utils.bytesToBase64(msg.message.toBocNew());
        log.info(base64boc);
        ExtMessageInfo resultRawMsg = tonlib.sendRawMessage(base64boc); // deploy
        log.info("body_hash {}, error {}", resultRawMsg.getBody_hash(), resultRawMsg.getError().getCode());

        // breakpoint, pause

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
        BigInteger amount = Utils.toNano(2); //2 Toncoins or 2bln nano-toncoins
        long seqNumber = 1;
        ExternalMessage extMsg = customContract.createTransferMessage(keyPair.getSecretKey(), destinationAddress, amount, seqNumber);
        String base64bocExtMsg = Utils.bytesToBase64(extMsg.message.toBocNew());
        tonlib.sendRawMessage(base64bocExtMsg);
 */
    }
}
