package org.ton.java.tonconnect;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.ToNumberPolicy;
import com.iwebpp.crypto.TweetNaclFast;
import java.math.BigInteger;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.ton.java.address.Address;
import org.ton.java.cell.CellBuilder;
import org.ton.java.smartcontract.types.WalletCodes;
import org.ton.java.tlb.StateInit;
import org.ton.java.tonlib.Tonlib;
import org.ton.java.tonlib.types.RawAccountState;
import org.ton.java.utils.Utils;

@Slf4j
@RunWith(JUnit4.class)
public class TestTonConnect {

  Gson gson = new GsonBuilder().setObjectToNumberStrategy(ToNumberPolicy.LONG_OR_DOUBLE).create();

  /**
   * <a
   * href="https://docs.ton.org/develop/dapps/ton-connect/sign#how-does-it-work">how-does-it-work</a>
   */
  @Test
  public void testTonConnect() throws Exception {

    // User has a wallet with:
    // prvKey f182111193f30d79d517f2339a1ba7c25fdf6c52142f0f2c1d960a1f1d65e1e4
    // pubKey 82a0b2543d06fec0aac952e9ec738be56ab1b6027fc0c1aa817ae14b4d1ed2fb
    // addr   0:2d29bfa071c8c62fa3398b661a842e60f04cb8a915fb3e749ef7c6c41343e16c

    Tonlib tonlib =
        Tonlib.builder()
            .testnet(true)
            .pathToTonlibSharedLib(Utils.getTonlibGithubUrl())
            .ignoreCache(false)
            .build();

    byte[] secretKey =
        Utils.hexToSignedBytes("F182111193F30D79D517F2339A1BA7C25FDF6C52142F0F2C1D960A1F1D65E1E4");
    TweetNaclFast.Signature.KeyPair keyPair = TweetNaclFast.Signature.keyPair_fromSeed(secretKey);
    log.info("prvKey: {}", Utils.bytesToHex(secretKey));
    log.info("pubKey: {}", Utils.bytesToHex(keyPair.getPublicKey()));

    String addressStr = "0:2d29bfa071c8c62fa3398b661a842e60f04cb8a915fb3e749ef7c6c41343e16c";
    Address address = Address.of(addressStr);
    RawAccountState rawAccountState = tonlib.getRawAccountState(address);
    log.info("state {}", rawAccountState);

    // 0. User initiates sign in process at the dapp's frontend, then dapp send request to its
    // backend to generate ton_proof payload.
    // 1. Backend generates a TonProof entity and sends it to a frontend (without signature
    // obviously)

    String payload = "doc-example-<BACKEND_AUTH_ID>";
    String sig = "";

    String proof =
        String.format(
            "{\n"
                + "                \"timestamp\": 1722999580, \n"
                + "                \"domain\": {\n"
                + "                    \"lengthBytes\": 16, \n"
                + "                    \"value\": \"xxx.xxx.com\"\n"
                + "                }, \n"
                + "                \"signature\": \"%s\", \n"
                + // <---------- to be updated
                "                \"payload\": \"%s\"\n"
                + "            }",
            sig, payload);

    log.info("proof: {}", proof);

    TonProof tonProof = gson.fromJson(proof, TonProof.class);

    byte[] message = TonConnect.createMessageForSigning(tonProof, addressStr);

    // 2. Frontend signs in to wallet using TonProof and receives back a signed TonProof. Basically
    // user signs the payload with his private key.

    byte[] signature = Utils.signData(keyPair.getPublicKey(), secretKey, message);
    log.info("signature: {}", Utils.bytesToHex(signature));

    // update TonProof by adding a signature
    tonProof.setSignature(Utils.bytesToBase64SafeUrl(signature));

    log.info("proof (updated): {}", tonProof);

    // 3. Frontend sends signed TonProof to a backend for verification.
    // when a smart-contract does not have get_public_key method, you can calculate public key from
    // its state init.
    StateInit stateInit =
        StateInit.builder()
            .code(CellBuilder.beginCell().fromBocBase64(rawAccountState.getCode()).endCell())
            .data(CellBuilder.beginCell().fromBocBase64(rawAccountState.getData()).endCell())
            .build();

    log.info(
        "wallet code bocHex: {}",
        CellBuilder.beginCell().fromBocBase64(rawAccountState.getCode()).endCell().toHex());
    log.info(
        "wallet version {}",
        WalletCodes.getKeyByValue(
            CellBuilder.beginCell().fromBocBase64(rawAccountState.getCode()).endCell().toHex()));

    BigInteger publicKeyRemote = tonlib.getPublicKey(address);
    // OR handover stateInit to calculate pubkey from contract's data

    String accountString =
        String.format(
            "{\n"
                + "        \"address\": \"%s\", \n"
                + "        \"chain\": \"-239\", \n"
                + "        \"walletStateInit\": \"%s\", \n"
                + // either this
                "        \"publicKey\": \"%s\"\n"
                + // or this
                "    }",
            address.toRaw(), stateInit.toCell().toBase64(), publicKeyRemote.toString(16));

    log.info("accountString: {}", accountString);

    WalletAccount account = gson.fromJson(accountString, WalletAccount.class);

    log.info("account:{}", account);

    assertThat(TonConnect.checkProof(tonProof, account)).isTrue();
  }

  @Test
  public void testTonConnectExample() throws Exception {

    String addressStr = "0:2d29bfa071c8c62fa3398b661a842e60f04cb8a915fb3e749ef7c6c41343e16c";

    // backend prepares
    TonProof tonProof =
        TonProof.builder()
            .timestamp(1722999580)
            .domain(Domain.builder().value("xxx.xxx.com").lengthBytes(16).build())
            .payload("doc-example-<BACKEND_AUTH_ID>")
            .build();

    // wallet signs
    byte[] secretKey =
        Utils.hexToSignedBytes("F182111193F30D79D517F2339A1BA7C25FDF6C52142F0F2C1D960A1F1D65E1E4");
    TweetNaclFast.Signature.KeyPair keyPair = TweetNaclFast.Signature.keyPair_fromSeed(secretKey);
    byte[] message = TonConnect.createMessageForSigning(tonProof, addressStr);
    byte[] signature = Utils.signData(keyPair.getPublicKey(), secretKey, message);
    log.info("signature: {}", Utils.bytesToHex(signature));

    // update TonProof by adding a signature
    tonProof.setSignature(Utils.bytesToBase64SafeUrl(signature));

    // backend verifies
    WalletAccount walletAccount =
        WalletAccount.builder()
            .chain(-239)
            .address(addressStr)
            .publicKey("82a0b2543d06fec0aac952e9ec738be56ab1b6027fc0c1aa817ae14b4d1ed2fb")
            .build();

    assertThat(TonConnect.checkProof(tonProof, walletAccount)).isTrue();
  }
}
