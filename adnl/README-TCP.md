# ADNL-TCP Lite Client Implementation

This document describes the complete ADNL-TCP protocol implementation for TON blockchain liteserver communication.

## Overview

This implementation provides a native Java ADNL-TCP client that can communicate directly with TON liteservers without requiring external binaries. It follows the [ADNL-TCP specification](https://github.com/xssnick/ton-deep-doc/blob/patch-1/ADNL-TCP-Liteserver.md) and is based on the Go reference implementation from [tonutils-go](https://github.com/xssnick/tonutils-go/tree/dev-v1-11/liteclient).

## Key Features

- **Pure Java Implementation**: No external dependencies on TON binaries
- **TCP-based Transport**: Implements ADNL-TCP protocol for reliable communication
- **Connection Pooling**: Support for multiple liteserver connections with load balancing
- **Automatic Reconnection**: Health monitoring and automatic failover
- **Complete TL Schema Support**: Full support for liteserver TL schemas
- **Thread-safe**: Concurrent query execution with proper synchronization

## Architecture

### Core Components

1. **AdnlTcpTransport**: Low-level TCP transport implementing ADNL-TCP protocol
2. **AdnlLiteClient**: High-level lite client with liteserver query methods
3. **LiteClientConnectionPool**: Connection pool manager with load balancing
4. **TLGenerator**: TL schema serialization/deserialization
5. **CryptoUtils**: Cryptographic utilities for ECDH, AES-CTR, etc.

### Protocol Implementation

#### Handshake Process

```
1. Client generates 160 random bytes for cipher construction
2. Client builds handshake packet (256 bytes):
   - [32 bytes] Server key ID (SHA256 of TL schema + public key)
   - [32 bytes] Client Ed25519 public key
   - [32 bytes] Checksum of 160 random bytes
   - [160 bytes] Encrypted random data using derived AES-CTR cipher
3. Server responds with empty packet to confirm connection
```

#### Packet Format

```
[4 bytes LE] Packet size
[32 bytes] Random nonce
[N bytes] Payload (TL-serialized)
[32 bytes] SHA256 checksum of (nonce + payload)
```

#### Encryption

- **Key Derivation**: ECDH between client X25519 private key and server X25519 public key
- **Cipher**: AES-CTR with keys derived from handshake random data
- **Separate Ciphers**: Different read/write ciphers for bidirectional communication

## Usage Examples

### Basic Connection

```java
// Create lite client
AdnlLiteClient client = new AdnlLiteClient();

// Connect to liteserver
client.connect("135.181.140.212", 13206, "K0t3+IWLOXHYMvMcrGZDPs+pn58a17LFbnXoQkKc2xw=");

// Get masterchain info
AdnlLiteClient.MasterchainInfo info = client.getMasterchainInfo();
System.out.println("Last block seqno: " + info.getLast().getSeqno());

// Close connection
client.close();
```

### Connection Pool

```java
// Create connection pool
LiteClientConnectionPool pool = new LiteClientConnectionPool();

// Add connections
pool.addConnection("135.181.140.212", 13206, "K0t3+IWLOXHYMvMcrGZDPs+pn58a17LFbnXoQkKc2xw=");
pool.addConnection("other.liteserver.com", 13206, "other_key_base64");

// Execute queries with automatic load balancing
AdnlLiteClient.MasterchainInfo info = pool.getMasterchainInfo();

// Close all connections
pool.close();
```

### Smart Contract Queries

```java
// Get account state
AdnlLiteClient.AccountId accountId = new AdnlLiteClient.AccountId(0, accountAddress);
AdnlLiteClient.AccountState state = client.getAccountState(blockId, accountId);

// Run smart contract method
AdnlLiteClient.RunMethodResult result = client.runSmcMethod(
    blockId, 
    accountId, 
    85143, // method ID (e.g., seqno)
    new byte[0] // parameters
);

if (result.getExitCode() == 0) {
    // Method executed successfully
    byte[] resultData = result.getResult();
}
```

## Supported Liteserver Methods

### Core Methods

- **getMasterchainInfo**: Get current masterchain state
- **getAccountState**: Get account state and proof
- **runSmcMethod**: Execute smart contract get-method
- **getBlock**: Get block data (can be added)
- **getState**: Get state data (can be added)

### TL Schemas

The implementation includes complete TL schemas for:

- **TCP Protocol**: ping, pong, authentication
- **ADNL Messages**: query, answer, createChannel, confirmChannel
- **Liteserver Protocol**: All liteserver query/response types
- **TON Node Types**: blockIdExt, zeroStateIdExt, accountId

## Configuration

### Liteserver Configuration

You can use the official TON global config or specify liteservers manually:

```java
// From global config (JSON)
LiteClientConnectionPool.GlobalConfig config = parseGlobalConfig(configJson);
pool.addConnectionsFromConfig(config);

// Manual configuration
pool.addConnection("host", port, "base64_public_key");
```

### Connection Parameters

- **Connection Timeout**: 10 seconds
- **Read Timeout**: 30 seconds
- **Ping Interval**: 5 seconds
- **Health Check Interval**: 30 seconds
- **Query Timeout**: 30 seconds

## Error Handling

### Connection Errors

```java
try {
    client.connect(host, port, publicKey);
} catch (Exception e) {
    // Handle connection failure
    logger.error("Failed to connect: " + e.getMessage());
}
```

### Query Errors

```java
try {
    AdnlLiteClient.MasterchainInfo info = client.getMasterchainInfo();
} catch (Exception e) {
    // Handle query failure
    if (e.getMessage().contains("timeout")) {
        // Query timeout
    } else {
        // Other error
    }
}
```

### Pool Failover

The connection pool automatically handles failover:

```java
// Pool will try multiple connections automatically
try {
    AdnlLiteClient.MasterchainInfo info = pool.getMasterchainInfo();
} catch (Exception e) {
    // All connections failed
    logger.error("All liteserver connections failed: " + e.getMessage());
}
```

## Performance Considerations

### Connection Pooling

- Use connection pools for production applications
- Maintain 2-3 connections for redundancy
- Monitor connection health regularly

### Query Optimization

- Reuse connections for multiple queries
- Implement query caching where appropriate
- Use appropriate timeouts for your use case

### Memory Management

- Close connections when done
- Use try-with-resources pattern where possible
- Monitor for connection leaks

## Security Considerations

### Key Management

- Store liteserver public keys securely
- Validate server certificates if using custom liteservers
- Use secure random number generation for client keys

### Network Security

- Use TLS/VPN for additional transport security if needed
- Validate all incoming data
- Implement rate limiting for queries

## Testing

### Unit Tests

```bash
# Run all tests
mvn test

# Run specific test
mvn test -Dtest=AdnlLiteClientTest#testSingleConnection
```

### Manual Testing

```java
// Run manual test
java -cp target/classes:target/test-classes org.ton.java.adnl.AdnlLiteClientTest
```

### Integration Testing

The test suite includes integration tests that connect to real TON liteservers. These tests verify:

- Connection establishment
- Handshake protocol
- Query/response handling
- Connection pooling
- Error handling

## Troubleshooting

### Common Issues

1. **Connection Timeout**
   - Check network connectivity
   - Verify liteserver address and port
   - Check firewall settings

2. **Handshake Failure**
   - Verify server public key is correct
   - Check for network interference
   - Ensure proper key format (base64)

3. **Query Failures**
   - Check if liteserver is responsive
   - Verify query parameters
   - Check for protocol version mismatches

### Debug Logging

Enable debug logging to troubleshoot issues:

```java
Logger.getLogger("org.ton.java.adnl").setLevel(Level.FINE);
```

### Network Analysis

Use network tools to analyze traffic:

```bash
# Monitor TCP connections
netstat -an | grep :13206

# Capture packets (if needed)
tcpdump -i any port 13206
```

## Comparison with Other Implementations

### vs. tonlib-java

| Feature | ADNL-TCP | tonlib-java |
|---------|----------|-------------|
| Dependencies | Pure Java | Requires tonlib binary |
| Performance | Direct TCP | JNI overhead |
| Deployment | Single JAR | Binary + JAR |
| Debugging | Full Java stack traces | Limited visibility |
| Customization | Full source control | Limited to tonlib API |

### vs. ton4j UDP Implementation

| Feature | ADNL-TCP | UDP ADNL |
|---------|----------|----------|
| Reliability | TCP guarantees | UDP best-effort |
| Connection State | Persistent | Stateless |
| Liteserver Support | Native | Requires adaptation |
| Complexity | Higher | Lower |
| Performance | Better for queries | Better for discovery |

## Future Enhancements

### Planned Features

1. **Additional Liteserver Methods**
   - getBlock, getState, getTransactions
   - getShardInfo, getValidatorSet
   - getLibraries, getConfigParams

2. **Advanced Connection Management**
   - Automatic server discovery
   - Connection quality metrics
   - Adaptive load balancing

3. **Performance Optimizations**
   - Connection multiplexing
   - Query pipelining
   - Response caching

4. **Monitoring and Metrics**
   - Connection statistics
   - Query performance metrics
   - Health monitoring dashboard

## Contributing

### Development Setup

1. Clone the repository
2. Import into IDE (IntelliJ IDEA recommended)
3. Run tests to verify setup
4. Make changes and test thoroughly

### Code Style

- Follow existing Java conventions
- Add comprehensive JavaDoc comments
- Include unit tests for new features
- Update documentation as needed

### Testing Guidelines

- Test with real liteservers when possible
- Include both positive and negative test cases
- Test error conditions and edge cases
- Verify thread safety for concurrent operations

## License

This implementation is part of the ton4j project and follows the same license terms.

## References

- [ADNL-TCP Specification](https://github.com/xssnick/ton-deep-doc/blob/patch-1/ADNL-TCP-Liteserver.md)
- [TON Liteserver Protocol](https://ton.org/docs/develop/dapps/apis/adnl)
- [tonutils-go Reference Implementation](https://github.com/xssnick/tonutils-go/tree/dev-v1-11/liteclient)
- [TON Global Config](https://ton.org/global-config.json)
