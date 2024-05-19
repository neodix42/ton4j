package org.ton.java.smartcontract.unittests;

import com.iwebpp.crypto.TweetNaclFast;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.ton.java.address.Address;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.mnemonic.Mnemonic;
import org.ton.java.mnemonic.Pair;
import org.ton.java.smartcontract.utils.MsgUtils;
import org.ton.java.smartcontract.wallet.v1.WalletV1R1;
import org.ton.java.smartcontract.wallet.v1.WalletV1R2;
import org.ton.java.smartcontract.wallet.v1.WalletV1R3;
import org.ton.java.tlb.types.ExternalMessageInfo;
import org.ton.java.tlb.types.Message;
import org.ton.java.tlb.types.MsgAddressIntStd;
import org.ton.java.utils.Utils;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@Slf4j
@RunWith(JUnit4.class)
public class TestWalletsV1 {

    /**
     * test new-wallet.fif
     * >fift -s new-wallet.fif 0
     */
    @Test
    public void testNewWalletV1() {
        // echo "F182111193F30D79D517F2339A1BA7C25FDF6C52142F0F2C1D960A1F1D65E1E4" | xxd -r -p - > new-wallet.pk
        byte[] secretKey = Utils.hexToSignedBytes("F182111193F30D79D517F2339A1BA7C25FDF6C52142F0F2C1D960A1F1D65E1E4");
        TweetNaclFast.Signature.KeyPair keyPair = TweetNaclFast.Signature.keyPair_fromSeed(secretKey);

//        Options options = Options.builder()
//                .publicKey(keyPair.getPublicKey())
//                .secretKey(keyPair.getSecretKey())
//                .wc(0L)
//                .build();
//
//        Wallet wallet = new Wallet(WalletVersion.V1R3, options);
//        WalletV1ContractR3 contract = wallet.create();

        WalletV1R3 contract = WalletV1R3.builder()
                .wc(0)
                .keyPair(keyPair)
                .build();

        assertThat(contract.createCodeCell().bitStringToHex()).isEqualTo("FF0020DD2082014C97BA218201339CBAB19C71B0ED44D0D31FD70BFFE304E0A4F260810200D71820D70B1FED44D0D31FD3FFD15112BAF2A122F901541044F910F2A2F80001D31F3120D74A96D307D402FB00DED1A4C8CB1FCBFFC9ED54");

        Cell body = CellBuilder.beginCell().storeUint(0, 32).endCell();
        Message msg = Message.builder()
                .info(ExternalMessageInfo.builder()
                        .dstAddr(contract.getAddressIntStd())
                        .build())
                .init(contract.getStateInit())
                .body(CellBuilder.beginCell()
                        .storeBytes(Utils.signData(contract.getKeyPair().getPublicKey(), contract.getKeyPair().getSecretKey(), body.hash()))
                        .storeCell(body)
                        .endCell())
                .build();
//        Message msg = contract.createExternalMessage(contract.getAddress(), true, null);

//        Message msg = contract.createTransferMessage(WalletV1R3Config.builder()
//                .destination(contract.getAddress())
//                .build());
        Address address = msg.getInit().getAddress();

        String my = "Creating new wallet in workchain " + contract.getWc() + "\n";
        my = my + "Loading private key from file new-wallet.pk" + "\n";
        my = my + "StateInit: " + msg.getInit().toCell().print() + "\n";
        my = my + "new wallet address = " + address.toString(false) + "\n";
        my = my + "(Saving address to file new-wallet.addr)" + "\n";
        my = my + "Non-bounceable address (for init): " + address.toString(true, true, false, true) + "\n";
        my = my + "Bounceable address (for later access): " + address.toString(true, true, true, true) + "\n";
        my = my + "signing message: " + msg.getBody().print() + "\n";
        my = my + "External message for initialization is " + msg.toCell().print() + "\n";
        my = my + Utils.bytesToHex(msg.getBody().toBoc()).toUpperCase() + "\n";
        my = my + "(Saved wallet creating query to file new-wallet-query.boc)" + "\n";
        log.info(my);

        assertThat(address.toString(false)).isEqualTo("0:258e549638a6980ae5d3c76382afd3f4f32e34482dafc3751e3358589c8de00d");
        assertThat(msg.toCell().bitStringToHex()).isEqualTo("88004B1CA92C714D3015CBA78EC7055FA7E9E65C68905B5F86EA3C66B0B1391BC01A11900AF60938844B0DDEE0D7F5C6C6B55C2D7F661170E029B8978AACCD402F2FF03FDD08D94398DB0826DA42FA96A6CBA73D232370025BF544D3A954208C990600A00000001_");
        assertThat(Utils.bytesToHex(msg.toCell().toBoc(true, false, false, false, false)).toUpperCase()).isEqualTo("B5EE9C724101030100F10002CF88004B1CA92C714D3015CBA78EC7055FA7E9E65C68905B5F86EA3C66B0B1391BC01A11900AF60938844B0DDEE0D7F5C6C6B55C2D7F661170E029B8978AACCD402F2FF03FDD08D94398DB0826DA42FA96A6CBA73D232370025BF544D3A954208C990600A000000010010200BAFF0020DD2082014C97BA218201339CBAB19C71B0ED44D0D31FD70BFFE304E0A4F260810200D71820D70B1FED44D0D31FD3FFD15112BAF2A122F901541044F910F2A2F80001D31F3120D74A96D307D402FB00DED1A4C8CB1FCBFFC9ED5400480000000082A0B2543D06FEC0AAC952E9EC738BE56AB1B6027FC0C1AA817AE14B4D1ED2FB111EE9AE");

    }

    /**
     * test wallet-v3.fif
     * >fift -s wallet.fif new-wallet 0:258e549638a6980ae5d3c76382afd3f4f32e34482dafc3751e3358589c8de00d 1 1 -n
     */
    @Test
    public void testCreateTransferMessageWalletV1() {
        byte[] secretKey = Utils.hexToSignedBytes("F182111193F30D79D517F2339A1BA7C25FDF6C52142F0F2C1D960A1F1D65E1E4");
        TweetNaclFast.Signature.KeyPair keyPair = TweetNaclFast.Signature.keyPair_fromSeed(secretKey);
//
//        Options options = Options.builder()
//                .publicKey(keyPair.getPublicKey())
//                .secretKey(keyPair.getSecretKey())
//                .wc(0L)
//                .build();
//
//        WalletV1ContractR3 contract  = new Wallet(WalletVersion.V1R3, options).create();

        WalletV1R3 contract = WalletV1R3.builder()
                .wc(0)
                .keyPair(keyPair)
                .build();

        assertThat(contract.createCodeCell().getBits().toHex()).isEqualTo("FF0020DD2082014C97BA218201339CBAB19C71B0ED44D0D31FD70BFFE304E0A4F260810200D71820D70B1FED44D0D31FD3FFD15112BAF2A122F901541044F910F2A2F80001D31F3120D74A96D307D402FB00DED1A4C8CB1FCBFFC9ED54");

        Address destination = Address.of("0:258e549638a6980ae5d3c76382afd3f4f32e34482dafc3751e3358589c8de00d");
        Cell body = CellBuilder.beginCell().storeUint(1, 32).endCell();
        Message msg = Message.builder()
                .info(ExternalMessageInfo.builder()
                        .dstAddr(MsgAddressIntStd.builder()
                                .workchainId(destination.wc)
                                .address(destination.toBigInteger())
                                .build())
                        .build())
                .init(contract.getStateInit())
                .body(CellBuilder.beginCell()
                        .storeBytes(Utils.signData(contract.getKeyPair().getPublicKey(), contract.getKeyPair().getSecretKey(), body.hash()))
                        .storeCell(body)
                        .endCell())
                .build();

//        Message msg = contract.createTransferMessage(WalletV1R3Config.builder()
//                .destination(Address.of("0:258e549638a6980ae5d3c76382afd3f4f32e34482dafc3751e3358589c8de00d"))
//                .amount(Utils.toNano(1))
//                .seqno(1L)
//                .build());
        Address address = msg.getInit().getAddress();

        String my = "Source wallet address =  " + address.toString(false) + "\n" +
                address.toString(true, true, true, true) + "\n" +
                "Loading private key from file new-wallet.pk" + "\n" +
                "Transferring GR$1. to account 0QAljlSWOKaYCuXTx2OCr9P08y40SC2vw3UeM1hYnI3gDY7I = 0:258e549638a6980ae5d3c76382afd3f4f32e34482dafc3751e3358589c8de00d seqno=0x1 bounce=0\n" +
                "Body of transfer message is x{}" + "\n\n" +
                "signing message: " + msg.getBody().print() + "\n" +
                "resulting external message: " + msg.toCell().print() + "\n" +
                Utils.bytesToHex(msg.toCell().toBoc()).toUpperCase() + "\n" +
                "(Saved to file wallet-query.boc)\n";

        log.info(my);

        String fiftOutput = " Source wallet address = 0:258e549638a6980ae5d3c76382afd3f4f32e34482dafc3751e3358589c8de00d\n" +
                "kQAljlSWOKaYCuXTx2OCr9P08y40SC2vw3UeM1hYnI3gDdMN\n" +
                "Loading private key from file new-wallet.pk\n" +
                "Transferring GR$1. to account kQAljlSWOKaYCuXTx2OCr9P08y40SC2vw3UeM1hYnI3gDdMN = 0:258e549638a6980ae5d3c76382afd3f4f32e34482dafc3751e3358589c8de00d seqno=0x0 bounce=-1\n" +
                "Body of transfer message is x{}\n" +
                "StateInit is x{4_}\n" +
                "signing message: x{0000000003}\n" +
                " x{620012C72A4B1C534C0572E9E3B1C157E9FA79971A2416D7E1BA8F19AC2C4E46F006A1DCD6500000000000000000000000000000}\n" +
                "resulting external message: x{88004B1CA92C714D3015CBA78EC7055FA7E9E65C68905B5F86EA3C66B0B1391BC01A051C5EF7744A7BE077B709344A506A88CF1B461EB8FD741BB582EA927A1DD2FEF75C78DB1413E32D28C92B20772FDA585BA890D306B451988BE1D0D22158B6D858000000001C_}\n" +
                " x{620012C72A4B1C534C0572E9E3B1C157E9FA79971A2416D7E1BA8F19AC2C4E46F006A1DCD6500000000000000000000000000000}\n" +
                "B5EE9C724101020100A10001CF88004B1CA92C714D3015CBA78EC7055FA7E9E65C68905B5F86EA3C66B0B1391BC01A051C5EF7744A7BE077B709344A506A88CF1B461EB8FD741BB582EA927A1DD2FEF75C78DB1413E32D28C92B20772FDA585BA890D306B451988BE1D0D22158B6D858000000001C010068620012C72A4B1C534C0572E9E3B1C157E9FA79971A2416D7E1BA8F19AC2C4E46F006A1DCD6500000000000000000000000000000FD4B06B3\n" +
                "        (Saved to file wallet - query.boc)";
        log.info(fiftOutput);

        assertThat(msg.toCell().bitStringToHex()).isEqualTo("0000000103");
        assertThat(msg.getBody().getBits().toHex()).isEqualTo("88004B1CA92C714D3015CBA78EC7055FA7E9E65C68905B5F86EA3C66B0B1391BC01A046F5F6B2149017D1A00412E7B7D6D6191A312A4C7538B5E336D5FC197C83CAA9B8EF152B4177FDE30C2A53641339FB84BD26995129046FDC7E61CE1A8FD11E870000000081C_");
        assertThat(Utils.bytesToHex(msg.getBody().toBoc()).toUpperCase()).isEqualTo("B5EE9C724101020100A10001CF88004B1CA92C714D3015CBA78EC7055FA7E9E65C68905B5F86EA3C66B0B1391BC01A046F5F6B2149017D1A00412E7B7D6D6191A312A4C7538B5E336D5FC197C83CAA9B8EF152B4177FDE30C2A53641339FB84BD26995129046FDC7E61CE1A8FD11E870000000081C010068420012C72A4B1C534C0572E9E3B1C157E9FA79971A2416D7E1BA8F19AC2C4E46F006A1DCD6500000000000000000000000000000BD829330");
    }

    @Test
    public void testNewWalletV1R1() {
        byte[] secretKey = Utils.hexToSignedBytes("F182111193F30D79D517F2339A1BA7C25FDF6C52142F0F2C1D960A1F1D65E1E4");
        TweetNaclFast.Signature.KeyPair keyPair = TweetNaclFast.Signature.keyPair_fromSeed(secretKey);

        WalletV1R1 contract = WalletV1R1.builder()
                .wc(0)
                .keyPair(keyPair)
                .build();
        assertThat(contract.getAddress()).isNotNull();
    }

    @Test
    public void testNewWalletV1R2() {
        WalletV1R2 contract = WalletV1R2.builder()
                .wc(0)
                .build();
        assertThat(contract.getAddress()).isNotNull();
        log.info("pubkey " + Utils.bytesToHex(contract.getKeyPair().getPublicKey()));
        log.info("seckey " + Utils.bytesToHex(contract.getKeyPair().getSecretKey()));
    }

    @Test
    public void testNewWalletV1R2Mnemonic() throws NoSuchAlgorithmException, InvalidKeyException {
        List<String> mnemonic = Mnemonic.generate(24);
        Pair keyPair = Mnemonic.toKeyPair(mnemonic);

        log.info("pubkey " + Utils.bytesToHex(keyPair.getPublicKey()));
        log.info("seckey " + Utils.bytesToHex(keyPair.getSecretKey()));

        TweetNaclFast.Signature.KeyPair keyPairSig = TweetNaclFast.Signature.keyPair_fromSeed(keyPair.getSecretKey());

        log.info("pubkey " + Utils.bytesToHex(keyPairSig.getPublicKey()));
        log.info("seckey " + Utils.bytesToHex(keyPairSig.getSecretKey()));


        WalletV1R1 contract = WalletV1R1.builder()
                .wc(0)
                .keyPair(keyPairSig)
                .build();
        assertThat(contract.getAddress()).isNotNull();

        Message msg = MsgUtils.createExternalMessageWithSignedBody(keyPairSig, contract.getAddress(), contract.getStateInit(), null);
        log.info("msg {}", msg.getInit().getAddress().toString(false));

    }
}
