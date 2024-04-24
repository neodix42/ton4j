package org.ton.java.smartcontract.integrationtests;

import com.iwebpp.crypto.TweetNaclFast;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.ton.java.address.Address;
import org.ton.java.cell.Cell;
import org.ton.java.smartcontract.TestFaucet;
import org.ton.java.smartcontract.types.*;
import org.ton.java.smartcontract.wallet.Contract;
import org.ton.java.smartcontract.wallet.Options;
import org.ton.java.smartcontract.wallet.Wallet;
import org.ton.java.smartcontract.wallet.v4.SubscriptionInfo;
import org.ton.java.smartcontract.wallet.v4.WalletV4ContractR2;
import org.ton.java.tonlib.types.ExtMessageInfo;
import org.ton.java.tonlib.types.FullAccountState;
import org.ton.java.tonlib.types.RunResult;
import org.ton.java.utils.Utils;

import java.math.BigInteger;
import java.util.List;

import static java.util.Objects.isNull;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.ton.java.smartcontract.TestFaucet.FAUCET_ADDRESS_RAW;

@Slf4j
@RunWith(JUnit4.class)
public class TestWalletV4R2PluginsDeployTransfer extends CommonTest {

    @Test
    public void testPlugins() throws InterruptedException {
        TweetNaclFast.Signature.KeyPair keyPair = Utils.generateSignatureKeyPair();

        Options options = Options.builder()
                .publicKey(keyPair.getPublicKey())
                .wc(0L)
                .walletId(42L)
                .subscriptionConfig(SubscriptionInfo.builder()
                        .beneficiary(Address.of("kf_sPxv06KagKaRmOOKxeDQwApCx3i8IQOwv507XD51JOLka"))
                        .subscriptionFee(Utils.toNano(2))
                        .period(60)
                        .startTime(0)
                        .timeOut(30)
                        .lastPaymentTime(0)
                        .lastRequestTime(0)
                        .failedAttempts(0)
                        .subscriptionId(12345)
                        .build())
                .build();

        WalletV4ContractR2 contract = new Wallet(WalletVersion.V4R2, options).create();

        InitExternalMessage msg = contract.createInitExternalMessage(keyPair.getSecretKey());
        Address walletAddress = msg.address;

        String nonBounceableAddress = walletAddress.toString(true, true, false, true);
        String bounceableAddress = walletAddress.toString(true, true, true, true);

        String my = "\nCreating new advanced wallet V4 with plugins in workchain " + options.wc + "\n" +
                "with unique wallet id " + options.walletId + "\n" +
                "Loading private key from file new-wallet.pk" + "\n" +
                "StateInit: " + msg.stateInit.print() + "\n" +
                "new wallet address = " + walletAddress.toString(false) + "\n" +
                "(Saving address to file new-wallet.addr)" + "\n" +
                "Non-bounceable address (for init): " + nonBounceableAddress + "\n" +
                "Bounceable address (for later access): " + bounceableAddress + "\n" +
                "signing message: " + msg.signingMessage.print() + "\n" +
                "External message for initialization is " + msg.message.print() + "\n" +
                Utils.bytesToHex(msg.message.toBoc()).toUpperCase() + "\n" +
                "(Saved wallet creating query to file new-wallet-query.boc)" + "\n";
        log.info(my);

        BigInteger balance = TestFaucet.topUpContract(tonlib, Address.of(nonBounceableAddress), Utils.toNano(7));
        log.info("new wallet {} balance: {}", contract.getName(), Utils.formatNanoValue(balance));

        // deploy wallet-v4
        tonlib.sendRawMessage(msg.message.toBase64());

        //check if state of the new contract/wallet has changed from un-init to active
        FullAccountState state;
        int i = 0;
        do {
            Utils.sleep(5, "waiting for account state");
            state = tonlib.getAccountState(walletAddress);
            if (i++ > 10) {
                throw new Error("time out getting account state");
            }
        } while (StringUtils.isEmpty(state.getAccount_state().getCode()));

        log.info("subwallet-id from fullAccountState {}", state.getAccount_state().getWallet_id());

        long walletCurrentSeqno = contract.getSeqno(tonlib);
        log.info("walletV4 balance: {}", Utils.formatNanoValue(state.getBalance()));
        log.info("seqno: {}", walletCurrentSeqno);
        log.info("subWalletId: {}", contract.getWalletId(tonlib));
        log.info("pubKey: {}", Utils.bytesToHex(contract.getPublicKey(tonlib)));
        log.info("pluginsList: {}", contract.getPluginsList(tonlib));

        RunResult result = tonlib.runMethod(Address.of(bounceableAddress), "get_subwallet_id");
        log.info("V4R2 get_subwallet_id {}", result);
        // create and deploy plugin -- start
        i = 0;
        do {
            Utils.sleep(5);
            state = tonlib.getAccountState(options.subscriptionConfig.getBeneficiary());
            if (i++ > 10) {
                throw new Error("time out getting account state");
            }
        } while (isNull(state.getAccount_state().getCode()));

        log.info("beneficiaryWallet balance {}", Utils.formatNanoValue(state.getBalance()));

        NewPlugin plugin = NewPlugin.builder()
                .secretKey(keyPair.getSecretKey())
                .seqno(walletCurrentSeqno)
                .pluginWc(options.wc) // reuse wc of the wallet
                .amount(Utils.toNano(0.1)) // initial plugin balance, will be taken from wallet-v4
                .stateInit(contract.createPluginStateInit())
                .body(contract.createPluginBody())
                .build();

        ExtMessageInfo extMessageInfo = contract.deployAndInstallPlugin(tonlib, plugin);
        assertThat(extMessageInfo.getError().getCode()).isZero();

        Utils.sleep(45);

        // create and deploy plugin -- end

        // get plugin list
        List<String> plugins = contract.getPluginsList(tonlib);
        log.info("pluginsList: {}", plugins.get(0));

        Address pluginAddress = Address.of(plugins.get(0));
        log.info("pluginAddress {}", pluginAddress.toString(false));

        SubscriptionInfo subscriptionInfo = contract.getSubscriptionData(tonlib, pluginAddress);

        log.info("{}", subscriptionInfo);

        log.info("plugin {} installed {}", pluginAddress, contract.isPluginInstalled(tonlib, pluginAddress));

        // Collect very first service fee

        Cell header = Contract.createExternalMessageHeader(pluginAddress);
        Cell extMessage = Contract.createCommonMsgInfo(header, null, null); // dummy external message, only destination address is relevant
        String extMessageBase64boc = Utils.bytesToBase64(extMessage.toBoc());
        tonlib.sendRawMessage(extMessageBase64boc);

        Utils.sleep(30);
        i = 0;
        do {
            Utils.sleep(5);
            state = tonlib.getAccountState(options.subscriptionConfig.getBeneficiary());
            if (i++ > 10) {
                throw new Error("time out getting account state");
            }
        } while (isNull(state.getAccount_state().getCode()));

        log.info("beneficiaryWallet balance {}", Utils.formatNanoValue(state.getBalance()));

        i = 0;
        do {
            Utils.sleep(5);
            state = tonlib.getAccountState(walletAddress);
            if (i++ > 10) {
                throw new Error("time out getting account state");
            }
        } while (isNull(state.getAccount_state().getCode()));

        log.info("walletV4 balance: {}", Utils.formatNanoValue(state.getBalance()));

        subscriptionInfo = contract.getSubscriptionData(tonlib, pluginAddress);
        log.info("{}", subscriptionInfo);

        assertThat(subscriptionInfo.getLastPaymentTime()).isNotEqualTo(0);

        // collect fee again

        Utils.sleep(90);

        header = Contract.createExternalMessageHeader(pluginAddress);
        extMessage = Contract.createCommonMsgInfo(header, null, null);
        extMessageBase64boc = Utils.bytesToBase64(extMessage.toBoc());
        tonlib.sendRawMessage(extMessageBase64boc);

        i = 0;
        do {
            Utils.sleep(5);
            state = tonlib.getAccountState(options.subscriptionConfig.getBeneficiary());
            if (i++ > 10) {
                throw new Error("time out getting account state");
            }
        } while (isNull(state.getAccount_state().getCode()));

        log.info("beneficiaryWallet balance {}", Utils.formatNanoValue(state.getBalance()));

        i = 0;
        do {
            Utils.sleep(5);
            state = tonlib.getAccountState(walletAddress);
            if (i++ > 10) {
                throw new Error("time out getting account state");
            }
        } while (isNull(state.getAccount_state().getCode()));

        log.info("walletV4 balance: {}", Utils.formatNanoValue(state.getBalance()));

        subscriptionInfo = contract.getSubscriptionData(tonlib, pluginAddress);
        log.info("{}", subscriptionInfo);

        // uninstall/remove plugin from the wallet -- start

        log.info("Uninstalling plugin {}", Address.of(contract.getPluginsList(tonlib).get(0)));

        walletCurrentSeqno = contract.getSeqno(tonlib);
        DeployedPlugin deployedPlugin = DeployedPlugin.builder()
                .seqno(walletCurrentSeqno)
                .amount(Utils.toNano(0.1))
                .pluginAddress(Address.of(contract.getPluginsList(tonlib).get(0)))
                .secretKey(keyPair.getSecretKey())
                .queryId(0)
                .build();

        ExternalMessage extMsgRemovePlugin = contract.removePlugin(deployedPlugin);
        String extMsgRemovePluginBase64boc = Utils.bytesToBase64(extMsgRemovePlugin.message.toBoc());
        tonlib.sendRawMessage(extMsgRemovePluginBase64boc);

        // uninstall plugin -- end

        Utils.sleep(30);
        List<String> list = contract.getPluginsList(tonlib);
        log.info("pluginsList: {}", list);
        assertThat(list.isEmpty()).isTrue();

        contract.sendTonCoins(tonlib, keyPair.getSecretKey(), Address.of(FAUCET_ADDRESS_RAW), Utils.toNano(0.33));
    }
}
