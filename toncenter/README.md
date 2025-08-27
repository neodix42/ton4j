# TonCenter API v2 Java Wrapper

[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0)
[![Based on TON][ton-svg]][ton]
![GitHub last commit](https://img.shields.io/github/last-commit/neodiX42/ton4j)

A comprehensive Java wrapper for the [TonCenter API v2](https://toncenter.com/api/v2/) that provides easy access to TON blockchain data and functionality.

## Features

- ✅ **Complete API Coverage**: All 28 TonCenter API v2 endpoints implemented
- ✅ **Network Support**: Both Mainnet and Testnet support
- ✅ **HTTP Client**: Built on OkHttp for reliable HTTP communication
- ✅ **Logging**: Built-in request/response logging using SLF4J
- ✅ **Resource Management**: Proper cleanup with close() method

## Installation

Add the dependency to your `pom.xml`:

## Maven [![Maven Central][maven-central-svg]][maven-central]

```xml
<dependency>
    <groupId>io.github.neodix42</groupId>
    <artifactId>toncenter</artifactId>
    <version>1.1.1</version>
</dependency>
```

## Jitpack [![jitpack][jitpack-svg]][jitpack]

```xml

<dependency>
    <groupId>io.github.neodix42.ton4j</groupId>
    <artifactId>tonlib</artifactId>
    <version>1.1.1</version>
</dependency>
```

## Quick Start

```java
import org.ton.ton4j.toncenter.TonCenter;
import org.ton.ton4j.toncenter.Network;
import org.ton.ton4j.toncenter.model.*;

// Create client with API key
TonCenter client = TonCenter.builder()
    .apiKey("your-api-key-here")
    .network(Network.MAINNET)
    .build();

try {
    // Get address information
    TonResponse<AddressInformationResponse> response = 
        client.getAddressInformation("EQCD39VS5jcptHL8vMjEXrzGaRcCVYto7HUn4bpAOg8xqB2N");
    
    if (response.isSuccess()) {
        AddressInformationResponse info = response.getResult();
        System.out.println("Balance: " + info.getBalance());
        System.out.println("State: " + info.getState());
    }
    
    // Get recent transactions
    TonResponse<List<TransactionResponse>> txResponse = 
        client.getTransactions("EQCD39VS5jcptHL8vMjEXrzGaRcCVYto7HUn4bpAOg8xqB2N", 10);
    
    if (txResponse.isSuccess()) {
        List<TransactionResponse> transactions = txResponse.getResult();
        System.out.println("Found " + transactions.size() + " transactions");
    }
    
} finally {
    client.close(); // Important: cleanup resources
}
```

## Configuration Options

```java
TonCenter client = TonCenter.builder()
    .apiKey("your-api-key")                    // Optional: API key for higher rate limits
    .mainnet()                                 // MAINNET or TESTNET
    .connectTimeout(Duration.ofSeconds(10))    // Connection timeout
    .readTimeout(Duration.ofSeconds(30))       // Read timeout  
    .writeTimeout(Duration.ofSeconds(30))      // Write timeout
    .build();
```

## API Endpoints Coverage

### Account Methods (10 endpoints)
- `getAddressInformation(address)` - Basic address information
- `getExtendedAddressInformation(address)` - Extended address information
- `getWalletInformation(address)` - Wallet-specific information
- `getTransactions(address, ...)` - Transaction history
- `getAddressBalance(address)` - Address balance
- `getAddressState(address)` - Address state
- `packAddress(address)` - Convert raw to user-friendly format
- `unpackAddress(address)` - Convert user-friendly to raw format
- `detectAddress(address)` - Get all address forms
- `getTokenData(address)` - NFT/Jetton information
- `getLibraries([]hash)` - retrieve libraries by hash

### Block Methods (9 endpoints)
- `getMasterchainInfo()` - Current masterchain state
- `getMasterchainBlockSignatures(seqno)` - Block signatures
- `getShardBlockProof(...)` - Merkle proof of shardchain block
- `getConsensusBlock()` - Consensus block information
- `lookupBlock(...)` - Find block by seqno/lt/unixtime
- `getShards(seqno)` - Shard information
- `getBlockTransactions(...)` - Block transactions
- `getBlockTransactionsExt(...)` - Extended block transactions
- `getBlockHeader(...)` - Block metadata
- `getOutMsgQueueSizes()` - Message queue sizes

### Configuration Methods (2 endpoints)
- `getConfigParam(configId)` - Get config parameter
- `getConfigAll()` - Get full config

### Transaction Methods (3 endpoints)
- `tryLocateTx(...)` - Locate outgoing transaction
- `tryLocateResultTx(...)` - Same as tryLocateTx
- `tryLocateSourceTx(...)` - Locate incoming transaction

### Run Method (1 endpoint)
- `runGetMethod(address, method, stack)` - Execute smart contract get method

### Send Methods (4 endpoints)
- `sendBoc(boc)` - Send serialized message
- `sendBocReturnHash(boc)` - Send message and return hash
- `sendQuery(...)` - Send unpacked external message
- `estimateFee(...)` - Estimate transaction fees

## Error Handling

The wrapper provides comprehensive error handling:

```java
try {
    TonResponse<AddressInformationResponse> response = 
        client.getAddressInformation("invalid-address");
} catch (TonCenterApiException e) {
    // API returned an error (4xx, 5xx with structured error response)
    System.err.println("API Error [" + e.getErrorCode() + "]: " + e.getMessage());
} catch (TonCenterException e) {
    // Network error, parsing error, or other issues
    System.err.println("Client Error: " + e.getMessage());
}
```

## Response Format

All API methods return a `TonResponse<T>` object:

```java
public class TonResponse<T> {
    private Boolean ok;           // Success indicator
    private T result;            // Response data (when successful)
    private String error;        // Error message (when failed)
    private Integer code;        // Error code (when failed)
    
    public boolean isSuccess() { return Boolean.TRUE.equals(ok); }
    public boolean isError() { return !isSuccess(); }
    // ... getters
}
```

## Network Selection

```java
// Mainnet (default)
TonCenter mainnet = TonCenter.builder()
    .network(Network.MAINNET)
    .build();
//or
TonCenter mainnet = TonCenter.builder()
        .mainnet()
        .build();

// Testnet
TonCenter testnet = TonCenter.builder()
    .network(Network.TESTNET)
    .build();
//or
TonCenter mainnet = TonCenter.builder()
        .testnet()
        .build();
```

## Convenience Methods

The wrapper includes convenience methods for common operations:

```java
// Get transactions with default limit (10)
getTransactions(address);

// Get transactions with custom limit
getTransactions(address, 20);

// Get config parameter without seqno
getConfigParam(0);

// Lookup block by seqno only
lookupBlockBySeqno(workchain, shard, seqno);

// Estimate fees with default parameters
estimateFee(address, body);

getSeqno(address);

getPublicKey(address);

getSeqno(address, atSeqno);

getPublicKey(address);

getSubWalletId(address);

getSubWalletId(address, atSeqno);

getJettonWalletAddress(jettonMasterAddress, ownerAddress);

JettonMinterData getJettonData(jettonMasterAddress);

```

## Logging

The wrapper uses SLF4J for logging. HTTP requests and responses are logged at DEBUG level:

```java
// Add to your logback.xml or log4j2.xml
<logger name="org.ton.ton4j.toncenter.TonCenter" level="DEBUG"/>
```

## Thread Safety

The `TonCenter` client is thread-safe and can be shared across multiple threads, as well as used as one instance in multiple threads.
## Resource Management

Always close the client to release HTTP connection resources:

```java
TonCenter client = TonCenter.builder().build();
try {
    // Use client...
} finally {
    client.close(); // Important!
}
```

Or use try-with-resources pattern if you implement AutoCloseable.

## TON Center API Rate Limits

- Without API key: 1 request per second
- With API key: Higher limits (check TonCenter documentation)

Get your API key from [TonCenter](https://toncenter.com/).

## Examples

### Get Wallet Balance

```java
TonResponse<AddressBalanceResponse> response = 
    client.getAddressBalance("EQCD39VS5jcptHL8vMjEXrzGaRcCVYto7HUn4bpAOg8xqB2N");

if (response.isSuccess()) {
    String balance = response.getResult().getBalance();
    System.out.println("Balance: " + balance + " nanotons");
}
```

### Execute Smart Contract Method

```java
List<List<Object>> stack = new ArrayList<>(); // Empty stack for seqno method

TonResponse<RunGetMethodResponse> response = 
    client.runGetMethod("EQD7vdOGw8KvXW6_OgBR2QpBQq5-9R8N8DCo0peQJZrP_VLu", "seqno", stack);

if (response.isSuccess()) {
    RunGetMethodResponse result = response.getResult();
    System.out.println("Exit code: " + result.getExitCode());
    System.out.println("Stack: " + result.getStack());
}
```

### Get Recent Transactions

```java
TonResponse<List<TransactionResponse>> response = 
    client.getTransactions("EQCD39VS5jcptHL8vMjEXrzGaRcCVYto7HUn4bpAOg8xqB2N", 5);

if (response.isSuccess()) {
    for (TransactionResponse tx : response.getResult()) {
        System.out.println("Transaction: " + tx.getTransactionId().getHash());
        System.out.println("Time: " + tx.getUtime());
        System.out.println("Fee: " + tx.getFee());
    }
}
```

## Dependencies

- **OkHttp**: HTTP client
- **Gson**: JSON serialization/deserialization  
- **SLF4J**: Logging facade
- **Lombok**: Code generation (compile-time only)

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## Support

- [TonCenter API Documentation](https://toncenter.com/api/v2/)
- [TON Documentation](https://ton.org/docs/)
- [GitHub Issues](https://github.com/neodix42/ton4j/issues)

[jitpack-svg]: https://jitpack.io/v/neodiX42/ton4j.svg

[jitpack]: https://jitpack.io/#neodiX42/ton4j

[ton-svg]: https://img.shields.io/badge/Based%20on-TON-blue

[ton]: https://ton.org

[maven-central-svg]: https://img.shields.io/maven-central/v/io.github.neodix42/toncenter

[maven-central]: https://mvnrepository.com/artifact/io.github.neodix42/toncenter