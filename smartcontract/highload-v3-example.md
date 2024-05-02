# SmartContract module

### Example of usage of Highload Wallet V3

```java
        tonlib=Tonlib.builder().testnet(true).ignoreCache(false).build();

        TweetNaclFast.Signature.KeyPair keyPair=Utils.generateSignatureKeyPair();

        Options options=Options.builder()
        .publicKey(keyPair.getPublicKey())
        .walletId(42L)
        .timeout(60*60)
        .build();

        Wallet wallet=new Wallet(WalletVersion.highloadV3,options);
        HighloadWalletV3 contract=wallet.create();

        String nonBounceableAddress=contract.getAddress().toString(true,true,false);
        String bounceableAddress=contract.getAddress().toString(true,true,true);

        log.info("non-bounceable address {}",nonBounceableAddress);
        log.info("    bounceable address {}",bounceableAddress);
        log.info("           raw address {}",contract.getAddress().toString(false));

        // top up new wallet using test-faucet-wallet
        BigInteger balance=TestFaucet.topUpContract(tonlib,Address.of(nonBounceableAddress),Utils.toNano(0.1));
        Utils.sleep(10,"topping up...");
        log.info("new wallet {} balance: {}",contract.getName(),Utils.formatNanoValue(balance));

        long createdAt=Instant.now().getEpochSecond()-60*5;

        HighloadV3Config config=HighloadV3Config.builder()
        .queryId(HighloadQueryId.fromSeqno(0).getQueryId())
        .createdAt(createdAt)
        .build();

        ExtMessageInfo extMessageInfo=contract.deploy(tonlib,keyPair.getSecretKey(),config);
        assertThat(extMessageInfo.getError().getCode()).isZero();

        Utils.sleep(30,"deploying");

        int numberOfRecipients=3;
        BigInteger amountToSendTotal=Utils.toNano(0.01*numberOfRecipients);

        //see code of createNMessages() below
        Cell nMessages=createNMessages(numberOfRecipients,contract,createdAt,null);

        Cell extMsgWith3Mgs=contract.createMessagesToSend(amountToSendTotal,nMessages,createdAt);

        config=HighloadV3Config.builder()
        .body(extMsgWith3Mgs)
        .queryId(HighloadQueryId.fromSeqno(1).getQueryId())
        .createdAt(createdAt)
        .build();

        extMessageInfo=contract.sendTonCoins(tonlib,keyPair.getSecretKey(),config);
        assertThat(extMessageInfo.getError().getCode()).isZero();
        log.info("sent {} messages",numberOfRecipients);


        Cell createNMessages(int numRecipients,HighloadWalletV3 contract,long createdAt)throws NoSuchAlgorithmException{
        List<OutAction> outActions=new ArrayList<>();
        for(int i=0;i<numRecipients; i++){
        Address destinationAddress=Address.of("0:"+Utils.bytesToHex(MessageDigest.getInstance("SHA-256").digest(UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8))));
        log.info("dest {} is {}",i,destinationAddress.toString(true));
        OutAction outAction=ActionSendMsg.builder()
        .mode((byte)3)
        .outMsg(MessageRelaxed.builder()
        .info(InternalMessageInfoRelaxed.builder()
        .bounce(false) // warning, for tests only
        .srcAddr(MsgAddressIntStd.builder()
        .workchainId(contract.getAddress().wc)
        .address(contract.getAddress().toBigInteger())
        .build())
        .dstAddr(MsgAddressIntStd.builder()
        .workchainId(destinationAddress.wc)
        .address(destinationAddress.toBigInteger())
        .build())
        .value(CurrencyCollection.builder()
        .coins(Utils.toNano(0.01))
        .build())
        .createdAt(createdAt)
        .build())
        .build())
        .build();
        outActions.add(outAction);
        }
        }
```

![Class Diagram](http://www.plantuml.com/plantuml/proxy?src=https://raw.githubusercontent.com/neodix42/ton4j/highload-v3-tests/smartcontract/highload-v3.puml)

More examples on how to work with [smart-contracts](../smartcontract/src/main/java/org/ton/java/smartcontract) can be
found [here](../smartcontract/src/test/java/org/ton/java/smartcontract).