# SmartContract module

## Example of usage of WalletV4ContractR2 (subscription, plugin) class

### Deploy Wallet V4R2

```java
TweetNaclFast.Signature.KeyPair keyPair = Utils.generateSignatureKeyPair();

WalletV4R2 contract = WalletV4R2.builder()
        .tonlib(tonlib)
        .keyPair(keyPair)
        .walletId(42)
        .build();

Address walletAddress = contract.getAddress();

String bounceableAddress = walletAddress.toBounceable();
log.info("bounceableAddress: {}", bounceableAddress);
log.info("pub-key {}", Utils.bytesToHex(contract.getKeyPair().getPublicKey()));
log.info("prv-key {}", Utils.bytesToHex(contract.getKeyPair().getSecretKey()));

BigInteger balance = TestFaucet.topUpContract(tonlib, Address.of(nonBounceableAddress), Utils.toNano(7));
log.info("new wallet {} balance: {}", contract.getName(), Utils.formatNanoValue(balance));

// deploy wallet-v4
ExtMessageInfo extMessageInfo = contract.deploy();
assertThat(extMessageInfo.getError().getCode()).isZero();

// list plugins
log.info("pluginsList: {}", contract.getPluginsList(tonlib));
```

### Deploy and install (assign) subscription plugin to admin wallet

```java
Address beneficiaryAddress = Address.of("kf_sPxv06KagKaRmOOKxeDQwApCx3i8IQOwv507XD51JOLka");
log.info("beneficiaryAddress: {}", beneficiaryAddress.toBounceable());

SubscriptionInfo subscriptionInfo = SubscriptionInfo.builder()
        .beneficiary(beneficiaryAddress)
        .subscriptionFee(Utils.toNano(2))
        .period(60)
        .startTime(0)
        .timeOut(30)
        .lastPaymentTime(0)
        .lastRequestTime(0)
        .failedAttempts(0)
        .subscriptionId(12345)
        .build();

Utils.sleep(30);

log.info("beneficiaryWallet balance {}", Utils.formatNanoValue(ContractUtils.getBalance(tonlib, beneficiaryAddress)));

WalletV4R1Config config = WalletV4R1Config.builder()
        .seqno(contract.getSeqno())
        .operation(1) // deploy and install plugin
        .walletId(42)
        .newPlugin(NewPlugin.builder()
                .secretKey(keyPair.getSecretKey())
                .seqno(walletCurrentSeqno)
                .pluginWc(contract.getWc()) // reuse wc of the wallet
                .amount(Utils.toNano(0.1)) // initial plugin balance, will be taken from wallet-v4
                .stateInit(contract.createPluginStateInit(subscriptionInfo))
                .body(contract.createPluginBody())
                .build())
        .build();

extMessageInfo = contract.send(config);
assertThat(extMessageInfo.getError().getCode()).isZero();
```

### Get subscription info

```java
SubscriptionInfo subscriptionInfo = contract.getSubscriptionData(pluginAddress);

where

public class SubscriptionInfo {
    Address walletAddress;
    Address beneficiary;
    BigInteger subscriptionFee;
    long period;
    long startTime;
    long timeOut;
    long lastPaymentTime;
    long lastRequestTime;
    boolean isPaid;
    boolean isPaymentReady;
    long failedAttempts;
    long subscriptionId;
}
```

### Collect service fee

```java
// dummy external message, only destination address is relevant
Cell extMessage = MsgUtils.createExternalMessageWithSignedBody(contract.getKeyPair(), pluginAddress, null, null).toCell();
extMessageInfo = tonlib.sendRawMessage(extMessage.toBase64());
```

### Uninstall plugin

```java
walletCurrentSeqno = contract.getSeqno();

config = WalletV4R1Config.builder()
        .seqno(contract.getSeqno())
        .walletId(config.getWalletId())
        .operation(3) // uninstall plugin
        .deployedPlugin(DeployedPlugin.builder()
                .seqno(walletCurrentSeqno)
                .amount(Utils.toNano(0.1))
                .pluginAddress(Address.of(contract.getPluginsList().get(0)))
                .secretKey(keyPair.getSecretKey())
                .queryId(0)
                .build())
        .build();

extMessageInfo = contract.uninstallPlugin(config);
Utils.sleep(30, "sent uninstall request");
assertThat(extMessageInfo.getError().getCode()).isZero();

```

Full integration test can be
found [here](../smartcontract/src/test/java/org/ton/java/smartcontract/integrationtests/TestWalletV4R2PluginsDeployTransfer.java).

More examples on how to work with [smart-contracts](../smartcontract/src/main/java/org/ton/java/smartcontract) can be
found [here](../smartcontract/src/test/java/org/ton/java/smartcontract).