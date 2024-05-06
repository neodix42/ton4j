package org.ton.java.smartcontract.integrationtests;

import com.iwebpp.crypto.TweetNaclFast;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.ton.java.address.Address;
import org.ton.java.cell.Cell;
import org.ton.java.smartcontract.TestFaucet;
import org.ton.java.smartcontract.types.DeployedPlugin;
import org.ton.java.smartcontract.types.NewPlugin;
import org.ton.java.smartcontract.types.WalletV4R1Config;
import org.ton.java.smartcontract.types.WalletVersion;
import org.ton.java.smartcontract.wallet.Options;
import org.ton.java.smartcontract.wallet.Wallet;
import org.ton.java.smartcontract.wallet.v4.SubscriptionInfo;
import org.ton.java.smartcontract.wallet.v4.WalletV4ContractR2;
import org.ton.java.tonlib.Tonlib;
import org.ton.java.tonlib.types.ExtMessageInfo;
import org.ton.java.tonlib.types.FullAccountState;
import org.ton.java.tonlib.types.RunResult;
import org.ton.java.tonlib.types.VerbosityLevel;
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
        tonlib = Tonlib.builder()
                .testnet(true)
                .ignoreCache(false)
                .verbosityLevel(VerbosityLevel.DEBUG)
                .build();

        TweetNaclFast.Signature.KeyPair keyPair = Utils.generateSignatureKeyPair();

        Options options = Options.builder()
                .publicKey(keyPair.getPublicKey())
                .secretKey(keyPair.getSecretKey())
                .wc(0L)
                .walletId(42L)
                .build();

        WalletV4ContractR2 contract = new Wallet(WalletVersion.V4R2, options).create();
//
//        Message msg = contract.createExternalMessage(contract.getAddress(), false, null);
        Address walletAddress = contract.getAddress();
//
        String nonBounceableAddress = walletAddress.toString(true, true, false, true);
        String bounceableAddress = walletAddress.toString(true, true, true, true);
        log.info("bounceableAddress: {}", bounceableAddress);

        BigInteger balance = TestFaucet.topUpContract(tonlib, Address.of(nonBounceableAddress), Utils.toNano(7));
        log.info("new wallet {} balance: {}", contract.getName(), Utils.formatNanoValue(balance));

        // deploy wallet-v4
//        tonlib.sendRawMessage(msg.toCell().toBase64());
        WalletV4R1Config config = WalletV4R1Config.builder()
                .subWalletId(42)
                .build();

        ExtMessageInfo extMessageInfo = contract.deploy(tonlib, config);
        assertThat(extMessageInfo.getError().getCode()).isZero();

        //check if state of the new contract/wallet has changed from un-init to active
        FullAccountState state;
        int i = 0;
        do {
            Utils.sleep(8, "waiting for account state");
            state = tonlib.getAccountState(walletAddress);
            if (i++ > 10) {
                throw new Error("time out getting account state");
            }
        } while (state.getAccount_state().getWallet_id() == 0);

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

        SubscriptionInfo subscriptionInfo = SubscriptionInfo.builder()
                .beneficiary(Address.of("kf_sPxv06KagKaRmOOKxeDQwApCx3i8IQOwv507XD51JOLka"))
                .subscriptionFee(Utils.toNano(2))
                .period(60)
                .startTime(0)
                .timeOut(30)
                .lastPaymentTime(0)
                .lastRequestTime(0)
                .failedAttempts(0)
                .subscriptionId(12345)
                .build();

        i = 0;
        do {
            Utils.sleep(5);
            state = tonlib.getAccountState(subscriptionInfo.getBeneficiary());
            if (i++ > 10) {
                throw new Error("time out getting account state");
            }
        } while (isNull(state.getAccount_state().getCode()));

        log.info("beneficiaryWallet balance {}", Utils.formatNanoValue(state.getBalance()));

        config = WalletV4R1Config.builder()
                .seqno(contract.getSeqno(tonlib))
                .operation(1) // deploy and install plugin
                .subWalletId(42)
                .newPlugin(NewPlugin.builder()
                        .secretKey(keyPair.getSecretKey())
                        .seqno(walletCurrentSeqno)
                        .pluginWc(options.wc) // reuse wc of the wallet
                        .amount(Utils.toNano(0.1)) // initial plugin balance, will be taken from wallet-v4
                        .stateInit(contract.createPluginStateInit(subscriptionInfo))
                        .body(contract.createPluginBody())
                        .build())
                .build();

        extMessageInfo = contract.sendTonCoins(tonlib, config);
        assertThat(extMessageInfo.getError().getCode()).isZero();

        Utils.sleep(45);

        // create and deploy plugin -- end

        // get plugin list
        List<String> plugins = contract.getPluginsList(tonlib);
        log.info("pluginsList: {}", plugins.get(0));

        Address pluginAddress = Address.of(plugins.get(0));
        log.info("pluginAddress {}", pluginAddress.toString(false));

        subscriptionInfo = contract.getSubscriptionData(tonlib, pluginAddress);

        log.info("{}", subscriptionInfo);
        log.info("plugin hash {}", new BigInteger(pluginAddress.hashPart).toString());

        log.info("plugin {} installed {}", pluginAddress, contract.isPluginInstalled(tonlib, pluginAddress));

        // Collect fee - first time

        Cell extMessage = contract.createExternalMessage(pluginAddress, false, null).toCell();
        extMessageInfo = tonlib.sendRawMessage(extMessage.toBase64());
        assertThat(extMessageInfo.getError().getCode()).isZero();

        i = 0;
        do {
            Utils.sleep(5);
            state = tonlib.getAccountState(subscriptionInfo.getBeneficiary());
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
        } while (state.getAccount_state().getSeqno() == 1);

        log.info("walletV4 balance: {}", Utils.formatNanoValue(state.getBalance()));

        subscriptionInfo = contract.getSubscriptionData(tonlib, pluginAddress);
        log.info("{}", subscriptionInfo);

        assertThat(subscriptionInfo.getLastPaymentTime()).isNotEqualTo(0);

        // collect fee - second time

        Utils.sleep(90);

        extMessage = contract.createExternalMessage(pluginAddress, false, null).toCell();

        extMessageInfo = tonlib.sendRawMessage(extMessage.toBase64());
        assertThat(extMessageInfo.getError().getCode()).isZero();

        i = 0;
        do {
            Utils.sleep(10);
            state = tonlib.getAccountState(subscriptionInfo.getBeneficiary());
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

        config = WalletV4R1Config.builder()
                .operation(3) // uninstall plugin
                .deployedPlugin(DeployedPlugin.builder()
                        .seqno(walletCurrentSeqno)
                        .amount(Utils.toNano(0.1))
                        .pluginAddress(Address.of(contract.getPluginsList(tonlib).get(0)))
                        .secretKey(keyPair.getSecretKey())
                        .queryId(0)
                        .build())
                .build();


        extMessageInfo = contract.uninstallPlugin(tonlib, config);
        Utils.sleep(30, "sent uninstall request");
        assertThat(extMessageInfo.getError().getCode()).isZero();

        // uninstall plugin -- end

        Utils.sleep(30);
        List<String> list = contract.getPluginsList(tonlib);
        log.info("pluginsList: {}", list);
        assertThat(list.isEmpty()).isTrue();

        config.setDestination(Address.of(FAUCET_ADDRESS_RAW));
        config.setAmount(Utils.toNano(0.33));

        extMessageInfo = contract.sendTonCoins(tonlib, config);
        Utils.sleep(30, "sent toncoins");
        assertThat(extMessageInfo.getError().getCode()).isZero();
    }
}
