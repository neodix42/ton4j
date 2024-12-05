package org.ton.java.smartcontract.unittests;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import com.iwebpp.crypto.TweetNaclFast;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.ton.java.address.Address;
import org.ton.java.smartcontract.utils.MsgUtils;
import org.ton.java.smartcontract.wallet.v4.WalletV4R2;
import org.ton.java.tlb.types.Message;
import org.ton.java.utils.Utils;

@Slf4j
@RunWith(JUnit4.class)
public class TestWalletsV4 {

  /** test new-wallet-v4r2.fc >fift -s new-wallet-v4r2.fc 0 698983191 */
  @Test
  public void testNewWalletV4r2() {

    TweetNaclFast.Signature.KeyPair keyPair = Utils.generateSignatureKeyPair();

    WalletV4R2 contract = WalletV4R2.builder().wc(0).walletId(42).keyPair(keyPair).build();

    Message msg =
        MsgUtils.createExternalMessageWithSignedBody(
            contract.getKeyPair(), contract.getAddress(), contract.getStateInit(), null);
    Address walletAddress = msg.getInit().getAddress();

    String my =
        "Creating new advanced wallet V4 with plugins in workchain "
            + contract.getWc()
            + "\n"
            +
            //                "with unique wallet id " + contract.getWalletId() + "\n" +
            "Loading private key from file new-wallet.pk"
            + "\n"
            + "StateInit: "
            + msg.getInit().toCell().print()
            + "\n"
            + "new wallet address = "
            + walletAddress.toString(false)
            + "\n"
            + "(Saving address to file new-wallet.addr)"
            + "\n"
            + "Non-bounceable address (for init): "
            + walletAddress.toString(true, true, false, true)
            + "\n"
            + "Bounceable address (for later access): "
            + walletAddress.toString(true, true, true, true)
            + "\n"
            + "signing message: "
            + msg.getBody().print()
            + "\n"
            + "External message for initialization is "
            + msg.toCell().print()
            + "\n"
            + Utils.bytesToHex(msg.toCell().toBoc()).toUpperCase()
            + "\n"
            + "(Saved wallet creating query to file new-wallet-query.boc)"
            + "\n";
    log.info(my);
    assertThat(msg.getBody()).isNotNull();
  }
}
