# SmartContract module

### Example of usage of Highload Wallet V3

```java
HighloadWalletV3 contract=HighloadWalletV3.builder()
        .tonlib(tonlib)
        .walletId(42)
        .build();

        String nonBounceableAddress=contract.getAddress().toNonBounceable();
        String bounceableAddress=contract.getAddress().toBounceable();
        String rawAddress=contract.getAddress().toRaw();

        log.info("non-bounceable address {}",nonBounceableAddress);
        log.info("    bounceable address {}",bounceableAddress);
        log.info("           raw address {}",rawAddress);
        log.info("pub-key {}",Utils.bytesToHex(contract.getKeyPair().getPublicKey()));
        log.info("prv-key {}",Utils.bytesToHex(contract.getKeyPair().getSecretKey()));

// top up new wallet using test-faucet-wallet
        BigInteger balance=TestnetFaucet.topUpContract(tonlib,Address.of(nonBounceableAddress),Utils.toNano(12));
        Utils.sleep(30,"topping up...");
        log.info("new wallet {} balance: {}",contract.getName(),Utils.formatNanoValue(balance));

        HighloadV3Config config=HighloadV3Config.builder()
        .walletId(42)
        .queryId(HighloadQueryId.fromSeqno(0).getQueryId())
        .build();

        SendResponse sendResponse=contract.deploy(config);
        assertThat(sendResponse.getCode()).isZero();

        contract.waitForDeployment(45);

        config=HighloadV3Config.builder()
        .walletId(42)
        .queryId(HighloadQueryId.fromSeqno(1).getQueryId())
        .body(contract.createBulkTransfer(
        createDummyDestinations(300),
        BigInteger.valueOf(HighloadQueryId.fromSeqno(1).getQueryId())))
        .build();

        sendResponse=contract.send(config);
        assertThat(sendResponse.getCode()).isZero();
        log.info("sent 1000 messages");

// help method
        List<Destination> createDummyDestinations(int count)throws NoSuchAlgorithmException{
        List<Destination> result=new ArrayList<>();
        for(int i=0;i<count; i++){
        String dstDummyAddress=Utils.generateRandomAddress(0);

        result.add(Destination.builder()
        .bounce(false)
        .address(dstDummyAddress)
        .amount(Utils.toNano(0.01))
//              .comment("comment-" + i)
        .build());
        }
        return result;
        }
```

![Class Diagram](http://www.plantuml.com/plantuml/proxy?src=https://raw.githubusercontent.com/neodix42/ton4j/highload-v3-tests/smartcontract/highload-v3.puml)

More examples on how to work with [smart-contracts](../smartcontract/src/main/java/org/ton/ton4j/smartcontract) can be
found [here](../smartcontract/src/test/java/org/ton/ton4j/smartcontract).