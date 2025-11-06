# TonCenter Indexer API V3 Client

Java wrapper for TonCenter [Indexer V3](https://toncenter.com/api/v3/index.html) API REST calls. This module provides a thread-safe, type-safe client for interacting with the TON Index API v3.

## Features

- **Complete API Coverage**: All 47 endpoints from the OpenAPI v3 specification
- **Thread-Safe**: Built on OkHttp with connection pooling
- **Type-Safe**: Strongly typed request/response models using Lombok
- **Builder Pattern**: Fluent API for client configuration
- **Error Handling**: Comprehensive exception handling with custom exceptions
- **Flexible Configuration**: Configurable timeouts, debug mode, and network selection

## Installation

Add the dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>org.ton.ton4j</groupId>
    <artifactId>toncenter-indexer-v3</artifactId>
    <version>1.3.3</version>
</dependency>
```

## Jitpack [![jitpack][jitpack-svg]][jitpack]

```xml

<dependency>
    <groupId>org.ton.ton4j</groupId>
    <artifactId>tonlib</artifactId>
    <version>1.3.3</version>
</dependency>
```

## Quick Start

```java
import org.ton.ton4j.toncenterv3.TonCenterV3;
import org.ton.ton4j.toncenterv3.model.ResponseModels.*;

// Create client with builder
TonCenterV3 client = TonCenterV3.builder()
    .mainnet()  // or .testnet()
    .apiKey("your-api-key")
    .build();

// Get masterchain info
MasterchainInfo info = client.getMasterchainInfo();
log.info("Last block: {}", info.getLast().getSeqno());

// Get account states
List<String> addresses = Collections.singletonList(TEST_ADDRESS);
AccountStatesResponse response = client.getAccountStates(addresses, true);

// Get transactions
TransactionsResponse txs = client.getTransactions(
    null, null, null, null,
    addresses, null, null, null,
    null, null, null, null,
    10, 0, "desc"
);

// Always close the client when done
client.close();
```

## Configuration Options

```java
TonCenterV3 client = TonCenterV3.builder()
    .apiKey("your-api-key")                    // Optional: API key for authentication
    .mainnet()                                  // or .testnet() or .endpoint("custom-url")
    .connectTimeout(Duration.ofSeconds(10))     // Connection timeout
    .readTimeout(Duration.ofSeconds(30))        // Read timeout
    .writeTimeout(Duration.ofSeconds(30))       // Write timeout
    .debug()                                    // Enable debug logging
    .uniqueRequests()                           // Add UUID to each request
    .build();
```

## API Endpoints

### Account Methods (6 endpoints)
- `getAccountStates(addresses, includeBoc)` - Query account states
- `getAddressBook(addresses)` - Get address book
- `getAddressInformation(address, useV2)` - Get address information
- `getMetadata(addresses)` - Query address metadata
- `getWalletInformation(address, useV2)` - Get wallet information
- `getWalletStates(addresses)` - Get wallet states

### Action Methods (4 endpoints)
- `getActions(...)` - Get actions by filter
- `getPendingActions(...)` - Get pending actions
- `getTraces(...)` - Get traces
- `getPendingTraces(...)` - Get pending traces

### Blockchain Methods (11 endpoints)
- `getBlocks(...)` - Get blocks by filter
- `getTransactions(...)` - Get transactions
- `getMessages(...)` - Get messages
- `getMasterchainInfo()` - Get masterchain info
- `getMasterchainBlockShards(...)` - Get masterchain block shards
- `getMasterchainBlockShardState(...)` - Get masterchain block shard state
- `getAdjacentTransactions(...)` - Get adjacent transactions
- `getTransactionsByMasterchainBlock(...)` - Get transactions by masterchain block
- `getTransactionsByMessage(...)` - Get transactions by message

### Jetton Methods (4 endpoints)
- `getJettonMasters(...)` - Get jetton masters
- `getJettonWallets(...)` - Get jetton wallets
- `getJettonTransfers(...)` - Get jetton transfers
- `getJettonBurns(...)` - Get jetton burns

### NFT Methods (3 endpoints)
- `getNFTCollections(...)` - Get NFT collections
- `getNFTItems(...)` - Get NFT items
- `getNFTTransfers(...)` - Get NFT transfers

### DNS Methods (1 endpoint)
- `getDNSRecords(...)` - Get DNS records

### Multisig Methods (2 endpoints)
- `getMultisigWallets(...)` - Get multisig wallets
- `getMultisigOrders(...)` - Get multisig orders

### Vesting Methods (1 endpoint)
- `getVestingContracts(...)` - Get vesting contracts

### Stats Methods (1 endpoint)
- `getTopAccountsByBalance(...)` - Get top accounts by balance

### Utils Methods (2 endpoints)
- `decode(opcodes, bodies)` - Decode opcodes and bodies (GET)
- `decodePost(request)` - Decode opcodes and bodies (POST)

### V2 Compatibility Methods (4 endpoints)
- `estimateFee(request)` - Estimate transaction fees
- `sendMessage(request)` - Send message to blockchain
- `runGetMethod(request)` - Run get method on smart contract
- `getAddressInformation(address)` - Get address information (V2 compatible)
- `getWalletInformation(address)` - Get wallet information (V2 compatible)

## Examples

### Get Account Balance

```java
TonCenterV3 client = TonCenterV3.builder()
    .testnet()
    .apiKey("your-api-key")
    .build();

try {
    List<String> addresses = Arrays.asList("EQCkR1cGmnsE45N4K0otPl5EnxnRakmGqeJUNua5fkWhales");
    AccountStatesResponse response = client.getAccountStates(addresses, false);
    
    for (AccountStateFull account : response.getAccounts()) {
        System.out.println("Address: " + account.getAddress());
        System.out.println("Balance: " + account.getBalance());
        System.out.println("Status: " + account.getStatus());
    }
} finally {
    client.close();
}
```

### Get Jetton Transfers

```java
TonCenterV3 client = TonCenterV3.builder()
    .mainnet()
    .apiKey("your-api-key")
    .build();

try {
    JettonTransfersResponse response = client.getJettonTransfers(
        Arrays.asList("owner-address"),  // ownerAddress
        null,                             // jettonWallet
        "jetton-master-address",          // jettonMaster
        "in",                             // direction
        null, null, null, null,           // time/lt filters
        10, 0, "desc"                     // limit, offset, sort
    );
    
    for (JettonTransfer transfer : response.getJettonTransfers()) {
        System.out.println("From: " + transfer.getSource());
        System.out.println("To: " + transfer.getDestination());
        System.out.println("Amount: " + transfer.getAmount());
    }
} finally {
    client.close();
}
```

### Get NFT Items

```java
TonCenterV3 client = TonCenterV3.builder()
    .mainnet()
    .apiKey("your-api-key")
    .build();

try {
    NFTItemsResponse response = client.getNFTItems(
        null,                              // address
        Arrays.asList("owner-address"),    // ownerAddress
        Arrays.asList("collection-addr"),  // collectionAddress
        null,                              // index
        false,                             // sortByLastTransactionLt
        10, 0                              // limit, offset
    );
    
    for (NFTItem item : response.getNftItems()) {
        System.out.println("NFT: " + item.getAddress());
        System.out.println("Owner: " + item.getOwnerAddress());
        System.out.println("Collection: " + item.getCollectionAddress());
    }
} finally {
    client.close();
}
```

### Get Traces with Actions

```java
TonCenterV3 client = TonCenterV3.builder()
    .mainnet()
    .apiKey("your-api-key")
    .build();

try {
    TracesResponse response = client.getTraces(
        "account-address",  // account
        null,               // traceId
        null,               // txHash
        null,               // msgHash
        null,               // mcSeqno
        null, null,         // time filters
        null, null,         // lt filters
        true,               // includeActions
        null,               // supportedActionTypes
        10, 0, "desc"       // limit, offset, sort
    );
    
    for (Trace trace : response.getTraces()) {
        System.out.println("Trace ID: " + trace.getTraceId());
        System.out.println("Transactions: " + trace.getTransactionsOrder().size());
        
        if (trace.getActions() != null) {
            for (Action action : trace.getActions()) {
                System.out.println("  Action: " + action.getType());
                System.out.println("  Success: " + action.getSuccess());
            }
        }
    }
} finally {
    client.close();
}
```

## Thread Safety

The `TonCenterV3` client is thread-safe and can be shared across multiple threads:

```java
TonCenterV3 client = TonCenterV3.builder()
    .mainnet()
    .apiKey("your-api-key")
    .build();

// Use from multiple threads
ExecutorService executor = Executors.newFixedThreadPool(10);
for (int i = 0; i < 100; i++) {
    executor.submit(() -> {
        MasterchainInfo info = client.getMasterchainInfo();
        // Process info...
    });
}

executor.shutdown();
executor.awaitTermination(1, TimeUnit.MINUTES);
client.close();
```

## Error Handling

```java
TonCenterV3 client = TonCenterV3.builder()
    .mainnet()
    .apiKey("your-api-key")
    .build();

try {
    MasterchainInfo info = client.getMasterchainInfo();
    // Process info...
} catch (TonCenterApiException e) {
    // API-level error (e.g., invalid parameters, rate limit)
    System.err.println("API Error: " + e.getMessage());
    System.err.println("Error Code: " + e.getErrorCode());
} catch (TonCenterException e) {
    // Network or other error
    System.err.println("Error: " + e.getMessage());
} finally {
    client.close();
}
```

## Architecture

- **TonCenterV3**: Main client class with builder pattern
- **Network**: Enum for network selection (`MAINNET, TESTNET, MY_LOCAL_TON`)
- **ResponseModels**: All response model classes
- **CommonModels**: Shared model classes (Block, Transaction, Message, etc.)
- **TonCenterException**: Base exception class
- **TonCenterApiException**: API-specific exception with error codes

## Links

- [TonCenter API Documentation](https://toncenter.com/api/v3/)
- [TON Documentation](https://ton.org/docs)

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## Support

- [TonCenter API Documentation](https://toncenter.com/api/v2/)
- [TON Documentation](https://ton.org/docs/)
- [GitHub Issues](https://github.com/ton-blockchain/ton4j/issues)

[jitpack-svg]: https://jitpack.io/v/ton-blockchain/ton4j.svg

[jitpack]: https://jitpack.io/#ton-blockchain/ton4j

[ton-svg]: https://img.shields.io/badge/Based%20on-TON-blue

[ton]: https://ton.org

[maven-central-svg]: https://img.shields.io/maven-central/v/org.ton.ton4j/toncenter-indexer-v3

[maven-central]: https://mvnrepository.com/artifact/org.ton.ton4j/toncenter-indexer-v3