# Utils module

## Maven [![Maven Central][maven-central-svg]][maven-central]

```xml

<dependency>
    <groupId>io.github.neodix42</groupId>
    <artifactId>utils</artifactId>
    <version>0.0.2</version>
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
    <groupId>io.github.neodix42.ton4j</groupId>
    <artifactId>utils</artifactId>
    <version>0.0.2</version>
</dependency>
```

## Public/Private Keys

You can use Utils methods to generate new key pair or import an existing private key.

```java
// generate new key kair
TweetNaclFast.Signature.KeyPair keyPair = Utils.generateSignatureKeyPair();
```

```java
// import existing private key
byte[] secretKey = Utils.hexToBytes("F182111193F30D79D517F2339A1BA7C25FDF6C52142F0F2C1D960A1F1D65E1E4");
TweetNaclFast.Signature.KeyPair keyPair=Utils.generateSignatureKeyPairFromSeed(secretKey);

byte[] pubKey = keyPair.getPublicKey();
byte[] secKey = keyPair.getSecretKey();

String msg = "ABC";
String msgHashSha256 = Utils.sha256(msg);

TweetNacl.Signature sig = new TweetNacl.Signature(pubKey,secKey);

// sign msg with signature
byte[] signed = sig.detached(Utils.hexToBytes(msgHashSha256));
```

## Other helpful methods

```java
static String base64ToBitString(String base64)

static byte[] base64ToBytes(String base64)

static byte[] base64SafeUrlToBytes(String base64)

static String base64ToHexString(String base64)

static String base64ToString(String base64)

static String base64UrlSafeToHexString(String base64)

static String bitStringToBase64(String binary)

static String bitStringToBase64UrlSafe(String binary)

static String bytesToBase64(byte[]bytes)

static String bytesToBase64SafeUrl(byte[]bytes)

static String bytesToHex(byte[]raw)

static boolean compareBytes(byte[]a,byte[]b)

static byte[]concatBytes(byte[]a,byte[]b)

static com.iwebpp.crypto.TweetNaclFast.Box.KeyPair generateKeyPair()

static com.iwebpp.crypto.TweetNaclFast.Box.KeyPair generatePairFromSecretKey(byte[] secretKey)

static com.iwebpp.crypto.TweetNaclFast.Signature.KeyPair generateSignatureKeyPair()

static com.iwebpp.crypto.TweetNaclFast.Signature.KeyPair generateSignatureKeyPairFromSeed(byte[] secretKey)

static byte[]getCRC16ChecksumAsBytes(byte[] bytes)

static String getCRC16ChecksumAsHex(byte[] bytes)

static int getCRC16ChecksumAsInt(byte[] bytes)

static byte[]getCRC32ChecksumAsBytes(byte[] bytes)

static byte[]getCRC32ChecksumAsBytesReversed(byte[] bytes)

static String getCRC32ChecksumAsHex(byte[] bytes)

static Long getCRC32ChecksumAsLong(byte[] bytes) //uses POLY 0x1EDC6F41

static Utils.OS getOS()

static String getSafeString(String originalResult, String processResult,String template)

static com.iwebpp.crypto.TweetNaclFast.Signature getSignature(byte[] pubKey,byte[] prvKey)

static String hexStringToBase64(String hex)

static String hexStringToBase64UrlSafe(String hex)

static byte[] hexToBytes(String hex)

static byte[] intToByteArray(int value)

static int readNBytesFromArray(int n, byte[] ui8array)

static String sha256(byte[]bytes)

static String sha256(String base)

static byte[]signData(byte[]pubKey,byte[]prvKey,byte[]data) //returns signature

static String stringToBase64(String str)

static String toUTC(long timestamp)

static String streamToString(InputStream is)

static String formatNanoValue(String value) // returns formatted, e.g. 100,451.515633556 

static String formatNanoValue(String value, int scale) // rounds to scale, e.g. 100,451.52 

static BigInteger toNano(BigDecimal toncoins)

static BigInteger toNano(double toncoins)

static BigInteger toNano(float toncoins)

static BigInteger toNano(String toncoins)

static BigInteger toNano(long toncoins)

static BigDecimal fromNano(long toncoins)

static BigDecimal fromNano(long toncoins, int scale)
```

More examples in [TestUtils](../utils/src/test/java/org/ton/java/utils/TestUtils.java) class.

[maven-central-svg]: https://img.shields.io/maven-central/v/io.github.neodix42/utils

[maven-central]: https://mvnrepository.com/artifact/io.github.neodix42/utils

[ton-svg]: https://img.shields.io/badge/Based%20on-TON-blue

[ton]: https://ton.org