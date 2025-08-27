# ADNL Protocol Implementation for TON

This module provides a complete implementation of the ADNL (Abstract Datagram Network Layer) protocol for TON blockchain, including a lite client that can communicate directly with TON liteservers.

[AdnlLiteClient](src/main/java/org/ton/ton4j/adnl/AdnlLiteClient.java) is not thread safe, which means a new instance of it should be created in each thread.

## Usage

### Basic ADNL Client

```java
byte[] serverPublicKey =
Base64.getDecoder().decode("n4VDnSCUuSpjnCyUk9e3QOOd6o0ItSWYbTnW3Wnn8wk=");
AdnlTcpTransport adnlTcpTransport = new AdnlTcpTransport();
adnlTcpTransport.connect("5.9.10.47", 19949, serverPublicKey);
assertThat(adnlTcpTransport.isConnected()).isTrue();
adnlTcpTransport.close();
```

### ADNL Lite Client for TON Blockchain

```java
TonGlobalConfig tonGlobalConfig = TonGlobalConfig.loadFromUrl(Utils.getGlobalConfigUrlMainnetGithub());

AdnlLiteClient client = AdnlLiteClient.builder().globalConfig(tonGlobalConfig).build();
MasterchainInfo info = client.getMasterchainInfo();

log.info("Last block seqno: {} ", info.getLast().getSeqno());
log.info("Workchain: {}", info.getLast().getWorkchain());
log.info("Shard: {}", info.getLast().getShard());
log.info("init.wc: {}", info.getInit().getWorkchain());

```

There are lots of examples on how to work with [AdnlLiteClient](src/test/java/org/ton/java/adnl/AdnlLiteClientTest.java).

[maven-central-svg]: https://img.shields.io/maven-central/v/io.github.neodix42/adnl

[maven-central]: https://mvnrepository.com/artifact/io.github.neodix42/adnl

[ton-svg]: https://img.shields.io/badge/Based%20on-TON-blue

[ton]: https://ton.org