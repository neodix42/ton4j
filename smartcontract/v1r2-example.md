# SmartContract module

### Example of usage of Wallet V1R2

```java
Tonlib tonlib = Tonlib.builder()
        .testnet(true)
        .ignoreCache(false)
        .build();

WalletV1R2 contract = WalletV1R2.builder()
        .tonlib(tonlib)
        .initialSeqno(2)
        .build();

String nonBounceableAddress = contract.getAddress().toNonBounceable();
String bounceableAddress = contract.getAddress().toBounceable();

log.info("non-bounceable address {}", nonBounceableAddress);
log.info("    bounceable address {}", bounceableAddress);
log.info("           raw address {}", contract.getAddress().toString(false));

// top up new wallet using test-faucet-wallet        
BigInteger balance = TestnetFaucet.topUpContract(tonlib, Address.of(nonBounceableAddress), Utils.toNano(0.1));
log.info("wallet {} balance: {}", contract.getName(), Utils.formatNanoValue(balance));


SendResponse sendResponse = contract.deploy();
assertThat(sendResponse.getCode()).isZero();

contract.waitForDeployment(45);

log.info("wallet seqno: {}", contract.getSeqno());

WalletV1R2Config config = WalletV1R2Config.builder()
        .seqno(contract.getSeqno())
        .destination(Address.of(TestnetFaucet.BOUNCEABLE))
        .amount(Utils.toNano(0.08))
        .comment("testNewWalletV1R2")
        .build();

// transfer coins from new wallet (back to faucet)
sendResponse = contract.send(config);
assertThat(sendResponse.getCode()).isZero();

contract.waitForBalanceChange(45);

balance = contract.getBalance();
log.info("wallet {} new balance: {}", contract.getName(), Utils.formatNanoValue(balance));
log.info("wallet seqno: {}", contract.getSeqno());
assertThat(balance.longValue()).isLessThan(Utils.toNano(0.02).longValue());
```

More examples on how to work with [smart-contracts](../smartcontract/src/main/java/org/ton/ton4j/smartcontract) can be
found [here](../smartcontract/src/test/java/org/ton/ton4j/smartcontract).