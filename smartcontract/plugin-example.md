# SmartContract module

## Example of usage of WalletV4ContractR2 (subscription, plugin) class

### Deploy Wallet V4R2
```java
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

Wallet wallet = new Wallet(WalletVersion.v4R2, options);
WalletV4ContractR2 contract = wallet.create();

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
        Utils.bytesToHex(msg.message.toBoc(false)).toUpperCase() + "\n" +
        "(Saved wallet creating query to file new-wallet-query.boc)" + "\n";
log.info(my);

// top up new wallet using test-faucet-wallet
Tonlib tonlib = Tonlib.builder()
        .testnet(true)
        .build();

BigInteger balance = TestFaucet.topUpContract(tonlib, Address.of(nonBounceableAddress), Utils.toNano(7));
log.info("new wallet {} balance: {}", contract.getName(), Utils.formatNanoValue(balance));

// deploy wallet-v4
tonlib.sendRawMessage(Utils.bytesToBase64(msg.message.toBoc(false)));

// list plugins
log.info("pluginsList: {}", contract.getPluginsList(tonlib));
```

### Deploy and install (assign) subscription plugin to admin wallet
```java
NewPlugin plugin = NewPlugin.builder()
    .secretKey(keyPair.getSecretKey())
    .seqno(walletCurrentSeqno)
    .pluginWc(options.wc) // reuse wc of the wallet
    .amount(Utils.toNano(0.1)) // initial plugin balance, will be taken from wallet-v4
    .stateInit(contract.createPluginStateInit())
    .body(contract.createPluginBody())
    .build();

contract.deployAndInstallPlugin(tonlib, plugin);
Utils.sleep(25);

log.info("pluginsList: {}", contract.getPluginsList(tonlib));

log.info("pluginAddress {}", Address.of(plugins.get(0).toString(true, true, true));

log.info("plugin {} installed {}", pluginAddress, contract.isPluginInstalled(tonlib, pluginAddress));
```


### Get subscription info
```java
SubscriptionInfo subscriptionInfo = contract.getSubscriptionData(tonlib, pluginAddress);
log.info("{}", subscriptionInfo);

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
Cell header = Contract.createExternalMessageHeader(pluginAddress);
Cell extMessage = Contract.createCommonMsgInfo(header, null, null); 
tonlib.sendRawMessage(Utils.bytesToBase64(extMessage.toBoc(false)));
```


### Uninstall plugin
```java
long walletCurrentSeqno = contract.getSeqno(tonlib);
DeployedPlugin deployedPlugin = DeployedPlugin.builder()
        .seqno(walletCurrentSeqno)
        .amount(Utils.toNano(0.1))
        .pluginAddress(pluginAddress)
        .secretKey(keyPair.getSecretKey())
        .queryId(0)
        .build();

ExternalMessage extMsgRemovePlugin = contract.removePlugin(deployedPlugin);
String extMsgRemovePluginBase64boc = Utils.bytesToBase64(extMsgRemovePlugin.message.toBoc(false));
tonlib.sendRawMessage(extMsgRemovePluginBase64boc);
```
Full integration test can be found [here](../smartcontract/src/test/java/org/ton/java/smartcontract/integrationtests/TestWalletV4R2PluginsDeployTransfer.java). 

More examples on how to work with [smart-contracts](../smartcontract/src/main/java/org/ton/java/smartcontract) can be
found [here](../smartcontract/src/test/java/org/ton/java/smartcontract).