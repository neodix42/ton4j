# SmartContract module

## Maven [![Maven Central][maven-central-svg]][maven-central]

```xml

<dependency>
    <groupId>org.ton.java</groupId>
    <artifactId>smartcontract</artifactId>
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
    <artifactId>smartcontract</artifactId>
    <version>0.0.1</version>
</dependency>
```

## Wallets

Currently, following wallet types and versions are supported:

* simpleR1
* simpleR2
* simpleR3
* v2R1
* v2R2
* v3R1
* v3R2
* v4R1
* v4R2
* Lockup

You can also create and deploy any custom wallet (contract), see below.

[Read more about wallet types here](README-WALLETS.md)

### Create and deploy SimpleR3 wallet

```java
byte[] secretKey = Utils.hexToBytes("F182111193F30D79D517F2339A1BA7C25FDF6C52142F0F2C1D960A1F1D65E1E4");
TweetNaclFast.Signature.KeyPair keyPair = TweetNaclFast.Signature.keyPair_fromSeed(secretKey);

Options options = Options.builder()
    .publicKey(keyPair.getPublicKey())
    .wc(0L)
    .build();

Wallet wallet = new Wallet(WalletVersions.simpleR3,options);
SimpleWalletContractR3 contract = wallet.create();
InitExternalMessage msg = contract.createInitExternalMessage(keyPair.getSecretKey());
Address walletAddress = msg.address;

log.info("new wallet address = {}", walletAddress.toString(false));
log.info("Non-bounceable address (for init): {}", walletAddress.toString(true,true,false,true));
log.info("Bounceable address (for later access): {}", walletAddress.toString(true,true,true,true));

// Before sending wallet's smart contract code, send some Toncoins to non-bouncelable address.

// deploy
Tonlib tonlib = Tonlib.builder().build();
String base64boc = Utils.bytesToBase64(msg.message.toBoc(false));
tonlib.sendRawMessage(base64boc);
```

### Send Toncoins

```java
...
ExternalMessage msg = contract.createTransferMessage(keyPair.getSecretKey(),
"0:258e549638a6980ae5d3c76382afd3f4f32e34482dafc3751e3358589c8de00d", //destination address
    Utils.toNano(1), // toncoin
    1L); // seqno
Address address = msg.address;
log.info("Source wallet address = {}", address.toString(false));
log.info("signing message: {}", msg.signingMessage.print());
log.info("resulting external message: {}", msg.message.print());

// send external message 
Tonlib tonlib = Tonlib.builder().build();
String base64boc = Utils.bytesToBase64(msg.message.toBoc(false));
tonlib.sendRawMessage(base64boc);
```

### Deploy custom contract

Let's create a new smart contract in Func. It's based on Simple Wallet and has additional extra field and run method for
it.

```func
;; custom-code.fc
() recv_internal(slice in_msg) impure {
    ;; do nothing for internal messages
}

() recv_external(slice in_msg) impure {
    var signature = in_msg~load_bits(512); ;; signature in msg body, see createInitExternalMessage()
    var cs = in_msg;
    var (msg_seqno, valid_until, extra_field) = (cs~load_uint(32), cs~load_uint(32), cs~load_uint(64)); ;; payload in message body, see createSigningMessage()
    throw_if(35, valid_until < =  now());
    var ds = get_data().begin_parse();
    var (stored_seqno, public_key, stored_x_data) = (ds~load_uint(32), ds~load_uint(256), ds~load_uint(64)); ;; data in stateInit or in storage. See createDataCell()
    ;; ds.end_parse();
    throw_unless(33, msg_seqno ==  stored_seqno);
    throw_unless(34, check_signature(slice_hash(in_msg), signature, public_key));
    accept_message();
    cs~touch(); ;; process data stored in ext message refs
    while (cs.slice_refs()) { ;; transfer msg. Data inside the body of external msg. See 
        var mode = cs~load_uint(8);
        send_raw_message(cs~load_ref(), mode);
    }
cs.end_parse();
set_data(begin_cell().store_uint(stored_seqno + 1, 32).store_uint(public_key, 256).store_uint(stored_x_data, 64).store_uint(extra_field, 64).end_cell());
}

int seqno() method_id {
    return get_data().begin_parse().preload_uint(32);
}

int get_public_key() method_id {
    var cs = get_data().begin_parse();
    cs~load_uint(32);
    return cs.preload_uint(256);
}

int get_x_data() method_id {
    var cs = get_data().begin_parse();
    cs~load_uint(32);
    cs~load_uint(256);
    return cs.preload_uint(64);
}

int get_extra_field() method_id {
    var cs = get_data().begin_parse();
    cs~load_uint(32);
    cs~load_uint(256);
    cs~load_uint(64);
    return cs.preload_uint(64);
}
```

Generate Fift code out of the smart contract:

```shell 
bash> func -o custom-code.fif -SPA stdlib.fc custom-code.fc
```

As a result you will get a Fift file custom-code.fif:
<details>
    <summary>custom-code.fif</summary>

```
"Asm.fif" include
// automatically generated from `stdlib.fc` `custom-code.fc` 
PROGRAM{
  DECLPROC recv_internal
  DECLPROC recv_external
  85143 DECLMETHOD seqno
  78748 DECLMETHOD get_public_key
  112748 DECLMETHOD get_x_data
  84599 DECLMETHOD get_extra_field
  recv_internal PROC:<{
    //  in_msg
    DROP	// 
  }>
  recv_external PROC:<{
    //  in_msg
    9 PUSHPOW2	//  in_msg _3 = 512
    LDSLICEX	//  signature in_msg
    DUP	//  signature in_msg cs
    32 LDU	//  signature in_msg _9 cs
    32 LDU	//  signature in_msg _9 _12 cs
    64 LDU	//  signature in_msg msg_seqno valid_until extra_field cs
    s0 s2 XCHG
    NOW	//  signature in_msg msg_seqno cs extra_field valid_until _19
    LEQ	//  signature in_msg msg_seqno cs extra_field _20
    35 THROWIF
    c4 PUSH	//  signature in_msg msg_seqno cs extra_field _23
    CTOS	//  signature in_msg msg_seqno cs extra_field ds
    32 LDU	//  signature in_msg msg_seqno cs extra_field _28 ds
    256 LDU	//  signature in_msg msg_seqno cs extra_field _28 _31 ds
    64 LDU	//  signature in_msg msg_seqno cs extra_field _28 _31 _82 _81
    DROP	//  signature in_msg msg_seqno cs extra_field stored_seqno public_key stored_x_data
    s5 s2 XCPU	//  signature in_msg stored_x_data cs extra_field stored_seqno public_key msg_seqno stored_seqno
    EQUAL	//  signature in_msg stored_x_data cs extra_field stored_seqno public_key _38
    33 THROWIFNOT
    s0 s5 XCHG	//  signature public_key stored_x_data cs extra_field stored_seqno in_msg
    HASHSU	//  signature public_key stored_x_data cs extra_field stored_seqno _41
    s0 s6 s5 XC2PU	//  stored_seqno public_key stored_x_data cs extra_field _41 signature public_key
    CHKSIGNU	//  stored_seqno public_key stored_x_data cs extra_field _42
    34 THROWIFNOT
    ACCEPT
    SWAP	//  stored_seqno public_key stored_x_data extra_field cs
    WHILE:<{
      DUP	//  stored_seqno public_key stored_x_data extra_field cs cs
      SREFS	//  stored_seqno public_key stored_x_data extra_field cs _47
    }>DO<{	//  stored_seqno public_key stored_x_data extra_field cs
      8 LDU	//  stored_seqno public_key stored_x_data extra_field mode cs
      LDREF	//  stored_seqno public_key stored_x_data extra_field mode _52 cs
      s0 s2 XCHG	//  stored_seqno public_key stored_x_data extra_field cs _52 mode
      SENDRAWMSG
    }>	//  stored_seqno public_key stored_x_data extra_field cs
    ENDS
    s0 s3 XCHG	//  extra_field public_key stored_x_data stored_seqno
    INC	//  extra_field public_key stored_x_data _57
    NEWC	//  extra_field public_key stored_x_data _57 _58
    32 STU	//  extra_field public_key stored_x_data _60
    s1 s2 XCHG	//  extra_field stored_x_data public_key _60
    256 STU	//  extra_field stored_x_data _62
    64 STU	//  extra_field _64
    64 STU	//  _66
    ENDC	//  _67
    c4 POP
  }>
  seqno PROC:<{
    // 
    c4 PUSH	//  _0
    CTOS	//  _1
    32 PLDU	//  _3
  }>
  get_public_key PROC:<{
    // 
    c4 PUSH	//  _1
    CTOS	//  cs
    32 LDU	//  _9 _8
    NIP	//  cs
    256 PLDU	//  _7
  }>
  get_x_data PROC:<{
    // 
    c4 PUSH	//  _1
    CTOS	//  cs
    32 LDU	//  _12 _11
    NIP	//  cs
    256 LDU	//  _14 _13
    NIP	//  cs
    64 PLDU	//  _10
  }>
  get_extra_field PROC:<{
    // 
    c4 PUSH	//  _1
    CTOS	//  cs
    32 LDU	//  _15 _14
    NIP	//  cs
    256 LDU	//  _17 _16
    NIP	//  cs
    64 LDU	//  _19 _18
    NIP	//  cs
    64 PLDU	//  _13
  }>
}END>c
```    

</details>

Print our BoC in hex format:

```fift
#!/usr/bin/fift -s
"TonUtil.fif" include
"Asm.fif" include

"custom-code.fif" include
2 boc+>B dup Bx. cr

// result
B5EE9C7241010C0100B2000114FF00F4A413F4BCF2C80B01020120020302014804050094F28308D71820D31FD31FD33F02F823BBF263ED44D0D31FD3FFD33F305152BAF2A105F901541065F910F2A2F800019320D74A96D307D402FB00E8D103A4C8CB1F12CBFFCB3FCB3FC9ED540004D03002012006070201200809001DBDC3676A268698F98E9FF98EB859FC0017BB39CED44D0D31F31D70BFF80202710A0B0022AA77ED44D0D31F31D3FF31D33F31D70B3F0010A897ED44D0D70B1F56A9826C
```

Create CustomContract that implements WalletContract and override `createDataCell()` and `createSigningMessage()`
methods.

Copy above BoC in hex format into CustomContract consturctor, see below.

<details>
    <summary>CustomContract.class</summary>

```
public class CustomContract implements WalletContract {

    Options options;
    Address address;

    /**
     * @param options Options
     */
    public CustomContract(Options options) {
        this.options = options;
        options.code = Cell.fromBoc("B5EE9C7241010C0100B2000114FF00F4A413F4BCF2C80B01020120020302014804050094F28308D71820D31FD31FD33F02F823BBF263ED44D0D31FD3FFD33FD15152BAF2A105F901541065F910F2A2F800019320D74A96D307D402FB00E8D103A4C8CB1F12CBFFCB3FCB3FC9ED540004D03002012006070201200809001DBDC3676A268698F98E9FF98EB859FC0017BB39CED44D0D31F31D70BFF80202710A0B0022AA77ED44D0D31F31D3FF31D33F31D70B3F0010A897ED44D0D70B1FFCB329CF");
    }

    @Override
    public String getName() {
        return "customContract";
    }

    @Override
    public Options getOptions() {
        return options;
    }

    @Override
    public Cell createDataCell() {
        System.out.println("CustomContract createDataCell");        
        CellBuilder cell = CellBuilder.beginCell();
        cell.storeUint(BigInteger.ZERO, 32); // seqno
        cell.storeBytes(getOptions().publicKey); // 256 bits
        cell.storeUint(BigInteger.TWO, 64); // stored_x_data
        return cell;
    }

    @Override
    public Address getAddress() {
        if (address = =  null) {
            Address addr = (createStateInit()).address;
            return addr;
        }
        return address;
    }

    @Override
    public Cell createSigningMessage(long seqno) {
        return createSigningMessage(seqno, 4l);
    }

    public Cell createSigningMessage(long seqno, long extraField) {
        System.out.println("CustomContract createSigningMessage");

        CellBuilder message = CellBuilder.beginCell();

        message.storeUint(BigInteger.valueOf(seqno), 32); // seqno

        if (seqno == 0) {
            for (int i = 0; i < 32; i++) {
                message.storeBit(true);
            }
        } else {
            Date date = new Date();
            long timestamp = (long) Math.floor(date.getTime() / (double) 1e3);
            message.storeUint(BigInteger.valueOf(timestamp + 60L), 32);
        }

        message.storeUint(BigInteger.valueOf(extraField), 64); // extraField
        return message;
    }
}
```

</details>

Now you are ready to deploy your custom smart contract.

```java
byte[] secretKey = Utils.hexToBytes("F182111193F30D79D517F2339A1BA7C25FDF6C52142F0F2C1D960A1F1D65E1E4");
TweetNaclFast.Signature.KeyPair keyPair = TweetNaclFast.Signature.keyPair_fromSeed(secretKey);

Options options = Options.builder()
    .publicKey(keyPair.getPublicKey())
    .wc(0L)
    .build();

CustomContract customContract = new CustomContract(options);

InitExternalMessage msg = customContract.createInitExternalMessage(keyPair.getSecretKey());
Address address = msg.address;

log.info("Creating new wallet in workchain {} \n"+
        "Loading private key from file new-wallet.pk\n"+
        "StateInit: {}\nnew wallet address = {}\n"+
        "(Saving address to file new-wallet.addr)\n"+
        "Non-bounceable address (for init): {}\n"+
        "Bounceable address (for later access): {}\n"+
        "signing message: {}\n"+
        "External message for initialization is {}\n"+
        "{}\n(Saved wallet creating query to file new-wallet-query.boc)"
        ,options.wc,msg.stateInit.print(),
        address.toString(false),
        address.toString(true,true,false,true),
        address.toString(true,true,true,true),
        msg.signingMessage.print(),
        msg.message.print(),
        Utils.bytesToHex(msg.message.toBoc(false)).toUpperCase());

```

Send some toincoins to non-bouncelable address above and then upload smart contract using Tonlib

```java
Tonlib tonlib =Tonlib.builder().build();
String base64boc = Utils.bytesToBase64(msg.message.toBoc(false));
log.info(base64boc);
tonlib.sendRawMessage(base64boc);
```

Check if contract was deployed successfully

```java
Tonlib tonlib = Tonlib.builder().build();

RunResult result = tonlib.runMethod(address,"seqno");
TvmStackEntryNumber seqno = (TvmStackEntryNumber) result.getStackEntry();
log.info("seqno: {}", seqno.getNumber());

result=tonlib.runMethod(address,"get_x_data");
TvmStackEntryNumber x_data = (TvmStackEntryNumber) result.getStackEntry();
log.info("x_data: {}", seqno.getNumber());

result=tonlib.runMethod(address,"get_extra_field");
TvmStackEntryNumber extra_field = (TvmStackEntryNumber) result.getStackEntry();
log.info("extra_field: {}", seqno.getNumber());

// result
seqno:1
x_data:2
extra_field:4
```

Transfer Toncoins

```java
Address destinationAddress = Address.of("kf_sPxv06KagKaRmOOKxeDQwApCx3i8IQOwv507XD51JOLka");
BigInteger amount = Utils.toNano(2); //2 Toncoins or 2bln nano-toncoins
long seqNumber = 1;
ExternalMessage extMsg = customContract.createTransferMessage(keyPair.getSecretKey(),destinationAddress,amount,seqNumber);
String base64bocExtMsg = Utils.bytesToBase64(extMsg.message.toBoc(false));
tonlib.sendRawMessage(base64bocExtMsg);  
```

More examples on how to work with [smart-contracts](../smartcontract/src/main/java/org/ton/java/smartcontract) can be
found in [tests](../smartcontract/src/test/java/org/ton/java/smartcontract) class.

[maven-central-svg]: https://img.shields.io/maven-central/v/org.ton.java/smartcontract

[maven-central]: https://mvnrepository.com/artifact/org.ton.java/smartcontract

[ton-svg]: https://img.shields.io/badge/Based%20on-TON-blue

[ton]: https://ton.org