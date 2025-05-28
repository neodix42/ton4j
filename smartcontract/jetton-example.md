# SmartContract module

## Example of usage of JettonMinter and JettonWallet classes

### Deploy Jetton Minter

```java
Tonlib tonlib = Tonlib.builder().testnet(true).ignoreCache(false).build();
WalletV3R1 adminWallet = GenerateWallet.randomV3R1(tonlib, 2);
WalletV3R1 wallet2 = GenerateWallet.randomV3R1(tonlib, 1);

log.info("admin wallet address {}", adminWallet.getAddress());
log.info("second wallet address {}", wallet2.getAddress());

JettonMinter minter = JettonMinter.builder()
        .tonlib(tonlib)
        .adminAddress(adminWallet.getAddress())
        .content(NftUtils.createOffChainUriCell("https://raw.githubusercontent.com/neodix42/ton4j/main/1-media/neo-jetton.json"))
        .jettonWalletCodeHex(WalletCodes.jettonWallet.getValue())
        .build();

log.info("jetton minter address {}", minter.getAddress());

// DEPLOY MINTER

WalletV3Config walletV3Config = WalletV3Config.builder()
        .walletId(42)
        .seqno(adminWallet.getSeqno())
        .destination(minter.getAddress())
        .amount(Utils.toNano(0.2))
        .stateInit(minter.getStateInit())
        .comment("deploy minter")
        .build();

ExtMessageInfo extMessageInfo = adminWallet.send(walletV3Config);
assertThat(extMessageInfo.getError().getCode()).isZero();
log.info("deploying minter");
minter.waitForDeployment(60);

getMinterInfo(minter); // nothing minted, so zero returned
```

### Mint jettons

```java
walletV3Config = WalletV3Config.builder()
        .walletId(42)
        .seqno(adminWallet.getSeqno())
        .destination(minter.getAddress())
        .amount(Utils.toNano(0.07))
        .body(minter.createMintBody(0,
                adminWallet.getAddress(),
                Utils.toNano(0.07),
                Utils.toNano(100500),
                null,
                null,
                BigInteger.ONE,
                MsgUtils.createTextMessageBody("minting"))
        ).build();

extMessageInfo = adminWallet.send(walletV3Config);
assertThat(extMessageInfo.getError().getCode()).isZero();

Utils.sleep(45, "minting...");

// owner of adminWallet holds his jettons on jettonWallet
Address adminJettonWalletAddress = minter.getJettonWalletAddress(tonlib, adminWallet.getWallet().getAddress());
log.info("admin JettonWalletAddress {}", adminJettonWalletAddress.toString(true, true, true));
```

### Transfer jettons

```java
walletV3Config = WalletV3Config.builder()
        .walletId(42)
        .seqno(adminWallet.getSeqno())
        .destination(adminJettonWallet.getAddress())
        .amount(Utils.toNano(0.057))
        .body(JettonWallet.createTransferBody(
                        0,
                        Utils.toNano(444),
                        wallet2.getAddress(),         // recipient
                        adminWallet.getAddress(),     // response address
                        null, // custom payload
                        BigInteger.ONE, // forward amount
                        MsgUtils.createTextMessageBody("gift") // forward payload
                )
        )
        .build();
extMessageInfo = adminWallet.send(walletV3Config);
assertThat(extMessageInfo.getError().getCode()).isZero();
```

More examples on how to work with [smart-contracts](../smartcontract/src/main/java/org/ton/ton4j/smartcontract) can be
found [here](../smartcontract/src/test/java/org/ton/ton4j/smartcontract).