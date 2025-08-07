# TonCenter API v2 Java Wrapper

A comprehensive Java wrapper for the [TonCenter API v2](https://toncenter.com/api/v2/) that provides easy access to TON blockchain data and functionality.

## Features

- **Complete API Coverage**: All 27 TonCenter API v2 endpoints implemented
- **Type Safety**: Strongly typed request/response models using Gson
- **Builder Pattern**: Fluent API for easy configuration
- **Network Support**: Both mainnet and testnet endpoints
- **Error Handling**: Comprehensive exception handling with custom exception types
- **HTTP Client**: Built on OkHttp for reliable HTTP communication
- **Logging**: Integrated SLF4J logging for debugging and monitoring
- **Timeouts**: Configurable connection, read, and write timeouts

## Installation

Add the dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>io.github.neodix42</groupId>
    <artifactId>toncenter</artifactId>
    <version>1.0.1</version>
</dependency>
```

## Quick Start

```java
import org.ton.ton4j.toncenter.TonCenter;
import org.ton.ton4j.toncenter.TonResponse;
import org.ton.ton4j.toncenter.model.AddressInformationResponse;

// Create client for testnet
TonCenter client = TonCenter.builder()
    .apiKey("your-api-key")
    .testnet()
    .build();

try {
    // Get address information
    TonResponse<AddressInformationResponse> response = 
        client.getAddressInformation("EQCD39VS5jcptHL8vMjEXrzGaRcCVYto7HUn4bpAOg8xqB2N");
    
    if (response.isSuccess()) {
        System.out.println("Balance: " + response.getResult().getBalance());
    }
} finally {
    client.close();
}
```

## Configuration

### Basic Configuration

```java
// Mainnet with API key
TonCenter client = TonCenter.builder()
    .apiKey("your-api-key")
    .mainnet()
    .build();

// Testnet without API key (limited functionality)
TonCenter client = TonCenter.builder()
    .testnet()
    .build();
```

### Advanced Configuration

```java
TonCenter client = TonCenter.builder()
    .apiKey("your-api-key")
    .network(Network.TESTNET)
    .connectTimeout(Duration.ofSeconds(10))
    .readTimeout(Duration.ofSeconds(30))
    .writeTimeout(Duration.ofSeconds(30))
    .build();
```

### Custom Endpoint

```java
TonCenter client = TonCenter.builder()
    .endpoint("https://your-custom-endpoint.com/api/v2")
    .apiKey("your-api-key")
    .build();
```

## API Methods

### Account Methods (10 endpoints)

```java
// Get basic address information
TonResponse<AddressInformationResponse> info = client.getAddressInformation(address);

// Get extended address information
TonResponse<Object> extendedInfo = client.getExtendedAddressInformation(address);

// Get wallet information
TonResponse<Object> walletInfo = client.getWalletInformation(address);

// Get transaction history
TonResponse<List<TransactionResponse>> transactions = client.getTransactions(address, 10);

// Get address balance
TonResponse<String> balance = client.getAddressBalance(address);

// Get address state
TonResponse<String> state = client.getAddressState(address);

// Address format conversion
TonResponse<String> packed = client.packAddress(rawAddress);
TonResponse<String> unpacked = client.unpackAddress(friendlyAddress);
TonResponse<Object> detected = client.detectAddress(address);

// Get token data (NFT/Jetton)
TonResponse<Object> tokenData = client.getTokenData(address);
```

### Block Methods (9 endpoints)

```java
// Get masterchain information
TonResponse<MasterchainInfoResponse> masterchainInfo = client.getMasterchainInfo();

// Get block signatures
TonResponse<Object> signatures = client.getMasterchainBlockSignatures(seqno);

// Get shard block proof
TonResponse<Object> proof = client.getShardBlockProof(workchain, shard, seqno);

// Get consensus block
TonResponse<Object> consensus = client.getConsensusBlock();

// Lookup block
TonResponse<Object> block = client.lookupBlockBySeqno(workchain, shard, seqno);

// Get shards information
TonResponse<ShardsResponse> shards = client.getShards(seqno);

// Get block transactions
TonResponse<Object> blockTxs = client.getBlockTransactions(workchain, shard, seqno);
TonResponse<Object> blockTxsExt = client.getBlockTransactionsExt(workchain, shard, seqno, null, null, null, null, null);

// Get block header
TonResponse<Object> header = client.getBlockHeader(workchain, shard, seqno);

// Get message queue sizes
TonResponse<Object> queueSizes = client.getOutMsgQueueSizes();
```

### Configuration Methods (2 endpoints)

```java
// Get specific config parameter
TonResponse<Object> configParam = client.getConfigParam(configId);

// Get full configuration
TonResponse<Object> fullConfig = client.getConfigAll();
```

### Transaction Methods (3 endpoints)

```java
// Locate transactions
TonResponse<Object> tx = client.tryLocateTx(source, destination, createdLt);
TonResponse<Object> resultTx = client.tryLocateResultTx(source, destination, createdLt);
TonResponse<Object> sourceTx = client.tryLocateSourceTx(source, destination, createdLt);
```

### Smart Contract Methods (1 endpoint)

```java
// Run get method on smart contract
List<List<Object>> stack = new ArrayList<>();
TonResponse<Object> result = client.runGetMethod(address, "get_method_name", stack);
```

### Send Methods (4 endpoints)

```java
// Send BOC (Bag of Cells)
TonResponse<Object> sendResult = client.sendBoc(bocData);
TonResponse<String> hash = client.sendBocReturnHash(bocData);

// Send query
TonResponse<Object> queryResult = client.sendQuery(address, body, initCode, initData);

// Estimate fees
TonResponse<Object> feeEstimate = client.estimateFee(address, body);
```

## Response Handling

All API methods return a `TonResponse<T>` object:

```java
TonResponse<AddressInformationResponse> response = client.getAddressInformation(address);

if (response.isSuccess()) {
    AddressInformationResponse data = response.getResult();
    System.out.println("Balance: " + data.getBalance());
    System.out.println("State: " + data.getState());
} else {
    System.err.println("Error: " + response.getError());
    System.err.println("Code: " + response.getCode());
}
```

## Error Handling

The wrapper provides comprehensive error handling:

```java
try {
    TonResponse<AddressInformationResponse> response = client.getAddressInformation(address);
    // Handle successful response
} catch (TonCenterApiException e) {
    // API returned an error response
    System.err.println("API Error: " + e.getMessage());
    System.err.println("Error Code: " + e.getErrorCode());
} catch (TonCenterException e) {
    // Network or other error
    System.err.println("Network Error: " + e.getMessage());
}
```

## Exception Types

- `TonCenterException`: Base exception for all TonCenter-related errors
- `TonCenterApiException`: Thrown when the API returns an error response

## Typed Response Models

The wrapper includes strongly typed models for common responses:

- `AddressInformationResponse`: Address information with balance, state, etc.
- `TransactionResponse`: Transaction details
- `MasterchainInfoResponse`: Masterchain state information
- `ShardsResponse`: Shards information with block details
- `SendBocRequest`: Request model for sending BOC data
- `RunGetMethodRequest`: Request model for smart contract method calls

## Convenience Methods

The wrapper provides convenience methods for common use cases:

```java
// Get transactions with default limit (10)
TonResponse<List<TransactionResponse>> txs = client.getTransactions(address);

// Get transactions with custom limit
TonResponse<List<TransactionResponse>> txs = client.getTransactions(address, 5);

// Lookup block by different criteria
TonResponse<Object> block1 = client.lookupBlockBySeqno(workchain, shard, seqno);
TonResponse<Object> block2 = client.lookupBlockByLt(workchain, shard, lt);
TonResponse<Object> block3 = client.lookupBlockByUnixtime(workchain, shard, unixtime);
```

## Logging

The wrapper uses SLF4J for logging. HTTP requests and responses are logged at DEBUG level:

```java
// Enable debug logging to see HTTP requests
Logger logger = LoggerFactory.getLogger(TonCenter.class);
((ch.qos.logback.classic.Logger) logger).setLevel(Level.DEBUG);
```

## Resource Management

Always close the client when done to release resources:

```java
TonCenter client = TonCenter.builder().build();
try {
    // Use client
} finally {
    client.close();
}
```

Or use try-with-resources pattern if implementing AutoCloseable:

```java
// Note: TonCenter doesn't implement AutoCloseable yet, but you can wrap it
try (var wrapper = new AutoCloseableWrapper(client)) {
    // Use client
}
```

## Testing

The wrapper includes comprehensive tests covering all endpoints:

```bash
mvn test
```

Run specific test:

```bash
mvn test -Dtest=TonCenterTest#testGetAddressInformation
```

## API Key

Get your API key from [TonCenter](https://toncenter.com/). Some endpoints work without an API key but with rate limiting.

## Network Endpoints

- **Mainnet**: `https://toncenter.com/api/v2`
- **Testnet**: `https://testnet.toncenter.com/api/v2`

## Dependencies

- **OkHttp**: HTTP client
- **Gson**: JSON serialization/deserialization
- **SLF4J**: Logging facade
- **Lombok**: Code generation (annotations)

## Examples

### Get Wallet Balance

```java
TonCenter client = TonCenter.builder()
    .apiKey("your-api-key")
    .testnet()
    .build();

try {
    String address = "EQCD39VS5jcptHL8vMjEXrzGaRcCVYto7HUn4bpAOg8xqB2N";
    TonResponse<String> response = client.getAddressBalance(address);
    
    if (response.isSuccess()) {
        long balanceNanotons = Long.parseLong(response.getResult());
        double balanceTons = balanceNanotons / 1_000_000_000.0;
        System.out.println("Balance: " + balanceTons + " TON");
    }
} finally {
    client.close();
}
```

### Get Transaction History

```java
TonCenter client = TonCenter.builder()
    .apiKey("your-api-key")
    .testnet()
    .build();

try {
    String address = "EQCD39VS5jcptHL8vMjEXrzGaRcCVYto7HUn4bpAOg8xqB2N";
    TonResponse<List<TransactionResponse>> response = client.getTransactions(address, 5);
    
    if (response.isSuccess()) {
        for (TransactionResponse tx : response.getResult()) {
            System.out.println("Transaction: " + tx.getTransactionId().getHash());
            System.out.println("Time: " + tx.getUtime());
        }
    }
} finally {
    client.close();
}
```

### Call Smart Contract Method

```java
TonCenter client = TonCenter.builder()
    .apiKey("your-api-key")
    .testnet()
    .build();

try {
    String contractAddress = "EQD7vdOGw8KvXW6_OgBR2QpBQq5-9R8N8DCo0peQJZrP_VLu";
    TonResponse<Object> response = client.runGetMethod(contractAddress, "seqno", new ArrayList<>());
    
    if (response.isSuccess()) {
        System.out.println("Contract seqno: " + response.getResult());
    }
} finally {
    client.close();
}
```

## License

This project is licensed under the Apache License 2.0 - see the [LICENSE](../LICENSE) file for details.

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## Support

For issues and questions:
- Create an issue on GitHub
- Check the [TonCenter API documentation](https://toncenter.com/api/v2/)
