# SmartContract module

### Deploy custom contract

Let's create a new smart contract in Func.
It's based on Simple Wallet and has additional extra field and run method inside it.

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

Create ExampleContract that implements WalletContract and override `createDataCell()` and `createSigningMessage()`
methods.

Copy above BoC in hex format into ExampleContract constructor, see below.

<details>
    <summary>ExampleContract.java</summary>

```java
@Builder
@Getter
public class ExampleContract implements Contract {

    TweetNaclFast.Signature.KeyPair keyPair;
    long initialSeqno;
    long initialExtraField;

    public static class ExampleContractBuilder {
    }

    public static ExampleContractBuilder builder() {
        return new CustomExampleContractBuilder();
    }

    private static class CustomExampleContractBuilder extends ExampleContractBuilder {
        @Override
        public ExampleContract build() {
            if (isNull(super.keyPair)) {
                super.keyPair = Utils.generateSignatureKeyPair();
            }

            return super.build();
        }
    }

    private Tonlib tonlib;
    private long wc;

    @Override
    public Tonlib getTonlib() {
        return tonlib;
    }

    @Override
    public long getWorkchain() {
        return wc;
    }

    @Override
    public String getName() {
        return "exampleContract";
    }

    @Override
    public Cell createDataCell() {
        return CellBuilder.beginCell()
                .storeUint(initialSeqno, 32) // seqno
                .storeBytes(keyPair.getPublicKey()) // 256 bits
                .storeUint(initialExtraField, 64) // stored_x_data
                .endCell();
    }

    @Override
    public Cell createCodeCell() {
        return CellBuilder.beginCell().fromBoc("B5EE9C7241010C0100B2000114FF00F4A413F4BCF2C80B01020120020302014804050094F28308D71820D31FD31FD33F02F823BBF263ED44D0D31FD3FFD33F305152BAF2A105F901541065F910F2A2F800019320D74A96D307D402FB00E8D103A4C8CB1F12CBFFCB3FCB3FC9ED540004D03002012006070201200809001DBDC3676A268698F98E9FF98EB859FC0017BB39CED44D0D31F31D70BFF80202710A0B0022AA77ED44D0D31F31D3FF31D33F31D70B3F0010A897ED44D0D70B1F56A9826C").endCell();
    }

    public Cell createTransferBody(CustomContractConfig config) {

        Cell order = Message.builder()
                .info(InternalMessageInfo.builder()
                        .dstAddr(MsgAddressIntStd.builder()
                                .workchainId(config.getDestination().wc)
                                .address(config.getDestination().toBigInteger())
                                .build())
                        .value(CurrencyCollection.builder().coins(config.getAmount()).build())
                        .build())
                .body(CellBuilder.beginCell()
                        .storeUint(0, 32)
                        .storeString(config.getComment())
                        .endCell())
                .build().toCell();

        CellBuilder message = CellBuilder.beginCell();

        message.storeUint(BigInteger.valueOf(config.getSeqno()), 32); // seqno
        message.storeUint((config.getValidUntil() == 0) ? Instant.now().getEpochSecond() + 60 : config.getValidUntil(), 32);
        message.storeUint(BigInteger.valueOf(config.getExtraField()), 64); // extraField
        message.storeUint((config.getMode() == 0) ? 3 : config.getMode() & 0xff, 8);
        message.storeRef(order);
        return message.endCell();
    }

    public Cell createDeployMessage() {
        CellBuilder message = CellBuilder.beginCell();
        message.storeUint(initialSeqno, 32); //seqno

        for (int i = 0; i < 32; i++) { // valid-until
            message.storeBit(true);
        }
        message.storeUint(initialExtraField, 64); //extra field
        return message.endCell();
    }

    public ExtMessageInfo deploy() {
        Cell body = createDeployMessage();

        Message externalMessage = Message.builder()
                .info(ExternalMessageInfo.builder()
                        .dstAddr(getAddressIntStd())
                        .build())
                .init(getStateInit())
                .body(CellBuilder.beginCell()
                        .storeBytes(Utils.signData(keyPair.getPublicKey(), keyPair.getSecretKey(), body.hash()))
                        .storeCell(body)
                        .endCell())
                .build();

        return tonlib.sendRawMessage(externalMessage.toCell().toBase64());
    }

    public ExtMessageInfo sendTonCoins(CustomContractConfig config) {
        Cell body = createTransferBody(config);
        Message externalMessage = Message.builder()
                .info(ExternalMessageInfo.builder()
                        .dstAddr(getAddressIntStd())
                        .build())
                .body(CellBuilder.beginCell()
                        .storeBytes(Utils.signData(keyPair.getPublicKey(), keyPair.getSecretKey(), body.hash()))
                        .storeCell(body)
                        .endCell())
                .build();

        return tonlib.sendRawMessage(externalMessage.toCell().toBase64());
    }

}

```

</details>

Now you are ready to deploy and use your contract.

```java
Tonlib tonlib = Tonlib.builder()
        .testnet(true)
        .ignoreCache(false)
        .build();

TweetNaclFast.Signature.KeyPair keyPair = Utils.generateSignatureKeyPair();

ExampleContract exampleContract = ExampleContract.builder()
        .tonlib(tonlib)
        .keyPair(keyPair)
        .build();

log.info("pubkey {}", Utils.bytesToHex(exampleContract.getKeyPair().getPublicKey()));

Address address = exampleContract.getAddress();
log.info("contract address {}", address);

// top up new wallet using test-faucet-wallet
BigInteger balance = TestFaucet.topUpContract(tonlib, Address.of(address.toString(true)), Utils.toNano(0.1));
log.info("new wallet {} balance: {}", address.toString(true), Utils.formatNanoValue(balance));

ExtMessageInfo extMessageInfo = exampleContract.deploy();
assertThat(extMessageInfo.getError().getCode()).isZero();

exampleContract.waitForDeployment(45);

log.info("seqno: {}", exampleContract.getSeqno());

RunResult result = tonlib.runMethod(address, "get_x_data");
log.info("gas_used {}, exit_code {} ", result.getGas_used(), result.getExit_code());
TvmStackEntryNumber x_data = (TvmStackEntryNumber) result.getStack().get(0);
log.info("x_data: {}", x_data.getNumber());

result = tonlib.runMethod(address, "get_extra_field");
log.info("gas_used {}, exit_code {} ", result.getGas_used(), result.getExit_code());
TvmStackEntryNumber extra_field = (TvmStackEntryNumber) result.getStack().get(0);
log.info("extra_field: {}", extra_field.getNumber());

Address destinationAddress = Address.of("kf_sPxv06KagKaRmOOKxeDQwApCx3i8IQOwv507XD51JOLka");

CustomContractConfig config = CustomContractConfig.builder()
        .seqno(exampleContract.getSeqno())
        .destination(destinationAddress)
        .amount(Utils.toNano(0.05))
        .extraField(42)
        .comment("no-way")
        .build();

extMessageInfo = exampleContract.sendTonCoins(config);
assertThat(extMessageInfo.getError().getCode()).isZero();

exampleContract.waitForBalanceChange(45);

result = tonlib.runMethod(address, "get_extra_field");
log.info("gas_used {}, exit_code {} ", result.getGas_used(), result.getExit_code());
extra_field = (TvmStackEntryNumber) result.getStack().get(0);
log.info("extra_field: {}", extra_field.getNumber());

assertThat(extra_field.getNumber().longValue()).isEqualTo(42);

```

More examples on how to work with [smart-contracts](../smartcontract/src/main/java/org/ton/java/smartcontract) can be
found [here](../smartcontract/src/test/java/org/ton/java/smartcontract).

[maven-central-svg]: https://img.shields.io/maven-central/v/io.github.neodix42/smartcontract

[maven-central]: https://mvnrepository.com/artifact/io.github.neodix42/smartcontract

[ton-svg]: https://img.shields.io/badge/Based%20on-TON-blue

[ton]: https://ton.org