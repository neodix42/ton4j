# Tonlib module

Java Tonlib library uses JNA to access methods in native Tonlib shared library.

## Maven [![Maven Central][maven-central-svg]][maven-central]

```xml
<dependency>
    <groupId>org.ton.java</groupId>
    <artifactId>tonlib</artifactId>
    <version>0.0.1</version>
</dependency>
```

## Jitpack

```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>
```

```xml
<dependency>
    <groupId>com.github.neodiX42.ton-java</groupId>
    <artifactId>tonlib</artifactId>
    <version>0.0.1</version>
</dependency>
```

## Constructor, getLast, lookupBlock, getBlockHeader, getShards

```java
// constructor
Tonlib(String    pathToTonlibSharedLib,   // path to native Tonlib shared library
        int     verbosityLevel,           // values from 0 to 4, where 0 - output is suppresed, 4 - debug level output; default - true
        boolean testnet,                  // switch between testnet and mainnet; default - true (testnet)
        boolean keystoreInMemory,         // switch for keystore location; default - false (keystore in local disk)
        String  keystorePath)             // path to a keystore; default - current directory (.)

Tonlib tonlib = Tonlib.builder().build(); // if path of Tonlib shared library is not specified Tonlib, the module tries to find one in local dir or java.library.path. 

// lookupBlock
BlockIdExt fullblock = tonlib.lookupBlock(304479,-1,-9223372036854775808L,0,0);
log.info(fullblock.toString());

// getLast
MasterChainInfo masterChainInfo=tonlib.getLast();
log.info(masterChainInfo.toString());

// getBlockHeader
BlockHeader header = tonlib.getBlockHeader(masterChainInfo.getLast());
log.info(header.toString());

// getShards
Shards shards = tonlib.getShards(masterChainInfo.getLast());
log.info(shards.toString());

// result
BlockIdExt(type=ton.blockIdExt,workchain=-1,shard=-9223372036854775808,seqno=304479,root_hash=tjYB35S3feJoHhMC4szfQcabzalwv80bMtyMv8S7tdE=,file_hash=5IQbqTASw+TLGC8BB0xZrB08tgnEyk+gUaYZr1Nd3q4=)
MasterChainInfo(type=blocks.masterchainInfo,last=BlockIdExt(type=ton.blockIdExt,workchain=-1,shard=-9223372036854775808,seqno=1095921,root_hash=wbo+N5HMi8UpnZb2RqnIyCW11sT6LNCMD4fcxDiqvb0=,file_hash=60p6sgJC21DE7wLv7+iZwPyJ1EFsl+4mMddiKPunazI=),state_root_hash=K9dEBCioJwCXzXNh0zqzykwA6aBIHbTcKdhxtoWqhh0=,init=BlockIdExt(type=ton.blockIdExt,workchain=-1,shard=0,seqno=0,root_hash=gj+B8wb/AmlPk1z1AhVI484rhrUpgSr2oSFIh56VoSg=,file_hash=Z+IKwYS54DmmJmesw/nAD5DzWadnOCMzee+kdgSYDOg=))
BlockHeader(type=blocks.header,id=BlockId(type=ton.blockId,workchain=-1,shard=-9223372036854775808,seqno=1095921),global_id=-3,version=0,flags=1,after_merge=false,after_split=false,before_split=false,want_merge=true,want_split=false,validator_list_hash_short=568192676,catchain_seqno=14516,min_ref_mc_seqno=1095919,is_key_block=false,prev_key_block_seqno=1095448,start_lt=1343256000000,end_lt=1343256000004,gen_utime=1656623463,prev_blocks=[BlockId(type=ton.blockId,workchain=-1,shard=-9223372036854775808,seqno=1095920)])
Shards(type=blocks.shards,shards=[BlockIdExt(type=ton.blockIdExt,workchain=0,shard=-9223372036854775808,seqno=1310240,root_hash=5+NaC7A3wo/PBYunKBwuWW5aqHTOb8hAHbtnDvFWBHs=,file_hash=34uD8EaAyYcm6rP7NpGeqbgO2GSh2SRFtmKpHe6/j5Y=)])
```

## Get all block transactions

```java
Tonlib tonlib = Tonlib.builder().build();

//lookupBlock
BlockIdExt fullblock = tonlib.lookupBlock(444699, -1, -9223372036854775808L, 0, 0);
log.info(fullblock.toString());

Map<String, RawTransactions> txs = tonlib.getAllBlockTransactions(fullblock,100,null);
for (Map.Entry<String, RawTransactions> entry: txs.entrySet()) {
    for (RawTransaction tx: ((RawTransactions)entry.getValue()).getTransactions()) {
        if ((tx.getIn_msg() != null) && (!tx.getIn_msg().getSource().getAccount_address().equals(""))) {
            log.info("{} <<<<< {} : {} ", tx.getIn_msg().getSource().getAccount_address(), tx.getIn_msg().getDestination().getAccount_address(), tx.getIn_msg().getValueToncoins(9));
        }
        if (tx.getOut_msgs()!=null) {
            for(RawMessage msg:tx.getOut_msgs()) {
                log.info("{} >>>>> {} : {} ", msg.getSource().getAccount_address(), msg.getDestination().getAccount_address(), msg.getValue());
            }
        }
    }
}

// result
BlockIdExt(type=ton.blockIdExt,workchain=-1,shard=-9223372036854775808,seqno=444699,root_hash=BQJzJNbbL4S2O4NhxHug65/hH3BjkKrVXJxVVFegSkc=,file_hash=nifDUPpDWiSBer9h8pnEgfIRbvXFwEfmgtQIwDu5xU8=)

Ef8FzogSIceHbJORQPAj7pPciiR5DLhwgbgxYgxaL_7fQWD2 >>>>> Ef99gz58SExDexFVJteOSZcPDxFeujRcK4Oy9qBhfJC5LwFP:1300000000
Ef8FzogSIceHbJORQPAj7pPciiR5DLhwgbgxYgxaL_7fQWD2 >>>>> Ef-9zaIhcKguvK9OQLMjwZvRXj0TCIQyJgU-AyGlcV7KBBT7:1200000000
Ef8FzogSIceHbJORQPAj7pPciiR5DLhwgbgxYgxaL_7fQWD2 >>>>> Ef_DZWc2mLOQOCozklpa7AcuO5Wf_fERO6FUzzeIU3bSpj65:1200000000
Ef8FzogSIceHbJORQPAj7pPciiR5DLhwgbgxYgxaL_7fQWD2 >>>>> Ef-mnEG7jUm-9VFLDCC-OtJYKGQKkNVPI7HTR8UdZFSTIMag:1200000000
Ef8FzogSIceHbJORQPAj7pPciiR5DLhwgbgxYgxaL_7fQWD2 >>>>> Ef99gz58SExDexFVJteOSZcPDxFeujRcK4Oy9qBhfJC5LwFP:1200000000
Ef8SowSSZBljEdXruvtTNIO1za4k7g30p2m4ZrlzXIQ_xPiU <<<<< Ef8FzogSIceHbJORQPAj7pPciiR5DLhwgbgxYgxaL_7fQWD2:0.974300000
Ef8FzogSIceHbJORQPAj7pPciiR5DLhwgbgxYgxaL_7fQWD2 >>>>> Ef8SowSSZBljEdXruvtTNIO1za4k7g30p2m4ZrlzXIQ_xPiU:1100000000
Ef-9zaIhcKguvK9OQLMjwZvRXj0TCIQyJgU-AyGlcV7KBBT7 <<<<< Ef8FzogSIceHbJORQPAj7pPciiR5DLhwgbgxYgxaL_7fQWD2:0.974300000
Ef8FzogSIceHbJORQPAj7pPciiR5DLhwgbgxYgxaL_7fQWD2 >>>>> Ef-9zaIhcKguvK9OQLMjwZvRXj0TCIQyJgU-AyGlcV7KBBT7:1100000000
Ef_DZWc2mLOQOCozklpa7AcuO5Wf_fERO6FUzzeIU3bSpj65 <<<<< Ef8FzogSIceHbJORQPAj7pPciiR5DLhwgbgxYgxaL_7fQWD2:0.974300000
Ef8FzogSIceHbJORQPAj7pPciiR5DLhwgbgxYgxaL_7fQWD2 <<<<< Ef99gz58SExDexFVJteOSZcPDxFeujRcK4Oy9qBhfJC5LwFP:1.300000000
Ef99gz58SExDexFVJteOSZcPDxFeujRcK4Oy9qBhfJC5LwFP >>>>> Ef8zMzMzMzMzMzMzMzMzMzMzMzMzMzMzMzMzMzMzMzMzM0vF:100000000000000
Ef8zMzMzMzMzMzMzMzMzMzMzMzMzMzMzMzMzMzMzMzMzM0vF <<<<< Ef99gz58SExDexFVJteOSZcPDxFeujRcK4Oy9qBhfJC5LwFP:100,391.556668961 
```

## Get transactions by address

```java
Tonlib tonlib = Tonlib.builder().build();
Address address = Address.of(MY_TESTNET_VALIDATOR_ADDR);
log.info("address: " + address.toString(true));
RawTransactions rawTransactions = tonlib.getRawTransactions(address.toString(false), null, null);
log.info("total txs: {}", rawTransactions.getTransactions().size());

for (RawTransaction tx : rawTransactions.getTransactions()) {
    if ((tx.getIn_msg() != null) && (!tx.getIn_msg().getSource().getAccount_address().equals(""))) {
        log.info("{}, {} <<<<< {} : {} ", Utils.toUTC(tx.getUtime()), tx.getIn_msg().getSource().getAccount_address(), tx.getIn_msg().getDestination().getAccount_address(), tx.getIn_msg().getValueToncoins(9));
    }
    if (tx.getOut_msgs() != null) {
        for (RawMessage msg : tx.getOut_msgs()) {
            log.info("{}, {} >>>>> {} : {} ", Utils.toUTC(tx.getUtime()), msg.getSource().getAccount_address(), msg.getDestination().getAccount_address(), msg.getValue());
        }
    }
}

// result
address:Uf/TIxBFB8kHCUppnaWpc1/C5c0tLDa5tb1zL9ysBgWzgg9E
total txs:10
2022-07-02 04:12:48, Ef8zMzMzMzMzMzMzMzMzMzMzMzMzMzMzMzMzMzMzMzMzM0vF <<<<< Ef_TIxBFB8kHCUppnaWpc1_C5c0tLDa5tb1zL9ysBgWzglKB:1.000000000
2022-07-02 04:12:48, Ef_TIxBFB8kHCUppnaWpc1_C5c0tLDa5tb1zL9ysBgWzglKB >>>>> Ef8zMzMzMzMzMzMzMzMzMzMzMzMzMzMzMzMzMzMzMzMzM0vF:100000000000000
2022-07-02 03:02:38, Ef8zMzMzMzMzMzMzMzMzMzMzMzMzMzMzMzMzMzMzMzMzM0vF <<<<< Ef_TIxBFB8kHCUppnaWpc1_C5c0tLDa5tb1zL9ysBgWzglKB:100,436.195728271
2022-07-02 03:02:38, Ef_TIxBFB8kHCUppnaWpc1_C5c0tLDa5tb1zL9ysBgWzglKB >>>>> Ef8zMzMzMzMzMzMzMzMzMzMzMzMzMzMzMzMzMzMzMzMzM0vF:1000000000
2022-07-02 02:12:27, Ef8zMzMzMzMzMzMzMzMzMzMzMzMzMzMzMzMzMzMzMzMzM0vF <<<<< Ef_TIxBFB8kHCUppnaWpc1_C5c0tLDa5tb1zL9ysBgWzglKB:1.000000000
2022-07-02 02:12:27, Ef_TIxBFB8kHCUppnaWpc1_C5c0tLDa5tb1zL9ysBgWzglKB >>>>> Ef8zMzMzMzMzMzMzMzMzMzMzMzMzMzMzMzMzMzMzMzMzM0vF:100000000000000
2022-07-02 01:02:17, Ef8zMzMzMzMzMzMzMzMzMzMzMzMzMzMzMzMzMzMzMzMzM0vF <<<<< Ef_TIxBFB8kHCUppnaWpc1_C5c0tLDa5tb1zL9ysBgWzglKB:100,342.564499014
2022-07-02 01:02:17, Ef_TIxBFB8kHCUppnaWpc1_C5c0tLDa5tb1zL9ysBgWzglKB >>>>> Ef8zMzMzMzMzMzMzMzMzMzMzMzMzMzMzMzMzMzMzMzMzM0vF:1000000000
2022-07-02 12:12:11, Ef8zMzMzMzMzMzMzMzMzMzMzMzMzMzMzMzMzMzMzMzMzM0vF <<<<< Ef_TIxBFB8kHCUppnaWpc1_C5c0tLDa5tb1zL9ysBgWzglKB:1.000000000
2022-07-02 12:12:11, Ef_TIxBFB8kHCUppnaWpc1_C5c0tLDa5tb1zL9ysBgWzglKB >>>>> Ef8zMzMzMzMzMzMzMzMzMzMzMzMzMzMzMzMzMzMzMzMzM0vF:100000000000000 
```

## Get account state, balance

```java
Tonlib tonlib = Tonlib.builder().build();

Address addr = Address.of("kQBeuMOZrZyCrtvZ1dMaMKmlxpulQFCOYCLI8EYcqMvI6v6E");
log.info("address: " + addr.toString(true));
AccountAddressOnly accountAddressOnly=AccountAddressOnly.builder()
                    .account_address(addr.toString(true))
                    .build();
FullAccountState accountState = tonlib.getAccountState(accountAddressOnly);
log.info(accountState.toString());
log.info("balance: {}", accountState.getBalance());

// result
address:kQBeuMOZrZyCrtvZ1dMaMKmlxpulQFCOYCLI8EYcqMvI6v6E
FullAccountState(address=AccountAddressOnly(account_address=kQBeuMOZrZyCrtvZ1dMaMKmlxpulQFCOYCLI8EYcqMvI6v6E),balance=4995640997,last_transaction_id=LastTransactionId(type=internal.transactionId,lt=491384000001,hash=C17UfhQAJ6kN/6SIWlO6PZAlHuI9aas227Tp/u7QuPU=),block_id=BlockIdExt(type=ton.blockIdExt,workchain=-1,shard=-9223372036854775808,seqno=1146167,root_hash=8L6D qqpFbf2j6kvl3P52SsPj82dl5X9zO+4UWUN314A=,file_hash=TawZEfHM+4E roaoGpwlfFHCVUzRGOO6l62YBSmHz/wk=),sync_utime=1656780177,account_state=AccountState(code=te6cckEBAQEAXwAAuv8AIN0gggFMl7ohggEznLqxnHGw7UTQ0x/XC//jBOCk8mCBAgDXGCDXCx/tRNDTH9P/0VESuvKhIvkBVBBE+RDyovgAAdMfMSDXSpbTB9QC+wDe0aTIyx/L/8ntVLW4bkI=, data=te6cckEBAQEAJgAASAAAAAGuqBbf9RjBhJbdGXyKcCc7giXayXZ++LJoZVYX7yhIdAapyws=, frozen_hash=))
balance:4995640997
```

## Encrypt/decrypt with mnemonic

```java
Tonlib tonlib = Tonlib.builder().build();

String base64mnemonic = Utils.stringToBase64("centring moist twopenny bursary could carbarn abide flirt ground shoelace songster isomeric pis strake jittery penguin gab guileful lierne salivary songbird shore verbal measures");

String dataToEncrypt = Utils.stringToBase64("ABC");
Data encrypted = tonlib.encrypt(dataToEncrypt, base64mnemonic);
log.info("encrypted {}", encrypted.getBytes());

Data decrypted = tonlib.decrypt(encrypted.getBytes(), base64mnemonic);
String dataDecrypted = Utils.base64ToString(decrypted.getBytes());
assertThat("ABC").isEqualTo(dataDecrypted);
```

## Execute run-methods

Get seqno

```java
Tonlib tonlib = Tonlib.builder().build();
Address address = Address.of("kQB7wRCSr02IwL1nOxkEipop3goYb4oN6ZehZMS2jImwyS1t");
RunResult result = tonlib.runMethod(address, "seqno");

log.info("gas_used {}, exit_code {} ", result.getGas_used(), result.getExit_code());

TvmStackEntryNumber seqno = (TvmStackEntryNumber) result.getStackEntry().get(0);
log.info("seqno: {}", eqno.getNumber());

// result 
gas_used 505,exit_code 0
seqno:2
```

Retrieve past_election_ids

```java
Address address = Address.of("-1:3333333333333333333333333333333333333333333333333333333333333333");
RunResult result = tonlib.runMethod(address,"past_election_ids");
TvmStackEntryList listResult = (TvmStackEntryList)result.getStackEntry().get(0);

for(Object o: listResult.getList().getElements()){
    TvmStackEntryNumber electionId = (TvmStackEntryNumber);
    log.info(electionId.getNumber().toString());
}

// result
1656773162
1656780362
```

Execute compute_returned_stake with parameter. Supported parameters types:

* num, number, int
* cell
* slice
* TODO - dict
* TODO - list

Use as array of strings:

* [num, 10]
* [cell, 0xFA0014]          // BoC in hex
* [slice, 0x80C11AAFA0014 ] // BoC in hex

```java
Tonlib tonlib = Tonlib.builder().build();

Address address = Address.of("-1:3333333333333333333333333333333333333333333333333333333333333333");
RunResult result = tonlib.runMethod(address, "compute_returned_stake", null);
log.info("result: {}", result);
assertThat(result.getExit_code()).isEqualTo(2); // error since compute_returned_stake requires an argument

Deque<String> stack = new ArrayDeque<>();
address = Address.of(TESTNET_VALIDATOR_ADDR);
stack.offer("[num, " + address.toDecimal() + "]");

result = tonlib.runMethod(address, "compute_returned_stake", stack);
log.info("result: {} ", result);
```

More examples in [TestTonlibJson](../tonlib/src/test/java/org/ton/java/tonlib/TestTonlibJson.java) and
[TestParseRunMethodResults](../tonlib/src/test/java/org/ton/java/tonlib/TestParseRunMethodResults.java) class.

You can find more examples in `TestTonlibJson` and `TestParseRunMethodResults` classes.


[maven-central-svg]: https://img.shields.io/maven-central/v/org.ton.java/tonlib

[maven-central]: https://mvnrepository.com/artifact/org.ton.java/tonlib

[ton-svg]: https://img.shields.io/badge/Based%20on-TON-blue

[ton]: https://ton.org