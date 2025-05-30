# ADNL Protocol Implementation for TON

This module provides a complete implementation of the ADNL (Abstract Datagram Network Layer) protocol for TON blockchain, including a lite client that can communicate directly with TON liteservers.

## ⚠️ Important: Transport Selection

**For liteclient connections to TON liteservers, use:**
- `AdnlTcpTransport` + `AdnlLiteClient` (TCP-based, follows ADNL-TCP specification)

**Deprecated for liteclient use:**
- `AdnlTransport` (UDP-based with channel creation, incompatible with liteservers)

The UDP-based transport uses `createChannel`/`confirmChannel` messages which are not supported by liteservers. Always use the TCP implementation for liteserver communication.

## Features

- **Complete ADNL Protocol Implementation**: Full support for ADNL over TCP as specified in the TON documentation
- **Cryptographic Security**: Ed25519 key exchange, AES-256-CTR encryption, and HMAC-SHA256 authentication
- **TL Serialization**: Proper Type Language (TL) serialization for liteserver communication
- **Lite Client**: Direct communication with TON liteservers without requiring native binaries
- **Connection Management**: Connection pooling and automatic reconnection
- **Message Types**: Support for all ADNL message types (ping, pong, query, answer, custom, etc.)

## Architecture

### Core Components

1. **AdnlClient**: Main ADNL client implementation
2. **AdnlTcpTransport**: TCP transport layer with encryption
3. **AdnlLiteClient**: High-level lite client for TON blockchain queries
4. **CryptoUtils**: Cryptographic utilities for key generation and encryption
5. **Message System**: Complete message type hierarchy
6. **TL Serialization**: Type Language serialization for liteserver API

### Message Types

- `MessagePing` / `MessagePong`: Connection testing
- `MessageQuery` / `MessageAnswer`: Request-response pattern
- `MessageCustom`: Custom application messages
- `MessageCreateChannel` / `MessageConfirmChannel`: Channel establishment
- `MessagePart`: Message fragmentation support
- `MessageNop`: No-operation messages

### Packet Structure

- `PacketContent`: Encrypted packet payload
- `PacketFlags`: Packet metadata and flags
- `PublicKeyED25519`: Ed25519 public key representation

## Usage

### Basic ADNL Client

```java
// Create ADNL client
byte[] ourPrivateKey = CryptoUtils.generatePrivateKey();
byte[] serverPublicKey = Base64.getDecoder().decode("server_public_key_base64");

AdnlClient client = new AdnlClient("server_ip", port, ourPrivateKey, serverPublicKey);

// Connect
client.connect();

// Send ping
long rtt = client.ping(5000);
System.out.println("RTT: " + rtt + " ns");

// Send custom message
client.sendCustomMessage("Hello, TON!");

// Close
client.close();
```

### Lite Client for TON Blockchain

```java
// Create lite client for testnet
AdnlLiteClient liteClient = AdnlLiteClient.forTestnet();

// Connect
liteClient.connect();

// Get masterchain info
MasterchainInfo info = liteClient.getMasterchainInfo();
System.out.println("Last block seqno: " + info.getSeqno());

// Get account state
JsonObject accountState = liteClient.getAccountState("EQD...");

// Run smart contract method
JsonObject result = liteClient.runGetMethod("EQD...", "get_balance");

// Close
liteClient.close();
```

### Using Global Config

```java
// From downloaded config file
AdnlLiteClient client = AdnlLiteClient.fromGlobalConfig("global.config.json", 0);

// For mainnet (downloads config automatically)
AdnlLiteClient mainnetClient = AdnlLiteClient.forMainnet();

// For testnet (downloads config automatically)
AdnlLiteClient testnetClient = AdnlLiteClient.forTestnet();
```

### Connection Pool

```java
LiteClientConnectionPool pool = new LiteClientConnectionPool();

// Add connections from global config
pool.addConnectionsFromGlobalConfig("global.config.json", 3);

// Execute query with automatic load balancing
MasterchainInfo info = pool.executeQuery(client -> client.getMasterchainInfo());

// Close pool
pool.close();
```

## TL Serialization

The implementation includes proper TL (Type Language) serialization for communicating with TON liteservers:

### Supported Queries

- `GetMasterchainInfo`: Get latest masterchain block information
- `GetAccountState`: Get account state for a specific address
- More queries can be easily added by extending `LiteServerQuery`

### Custom TL Queries

```java
public class CustomQuery extends LiteServerQuery {
    private static final int CONSTRUCTOR_ID = 0x12345678;
    
    @Override
    public int getConstructorId() {
        return CONSTRUCTOR_ID;
    }
    
    @Override
    protected void serializeData(ByteArrayOutputStream baos) throws IOException {
        // Serialize your data here
        writeInt32(baos, someValue);
        writeBytes(baos, someData);
    }
}
```

## Security Features

- **Ed25519 Key Exchange**: Secure key agreement using Curve25519
- **AES-256-CTR Encryption**: All messages are encrypted
- **HMAC-SHA256 Authentication**: Message integrity and authenticity
- **Replay Protection**: Sequence numbers prevent replay attacks
- **Perfect Forward Secrecy**: Session keys are ephemeral

## Protocol Compliance

This implementation follows the official ADNL protocol specification:
- [ADNL-TCP-Liteserver Documentation](https://github.com/xssnick/ton-deep-doc/blob/patch-1/ADNL-TCP-Liteserver.md)
- Compatible with reference implementations in Go and C++

## Testing

Run the test suite:

```bash
mvn test
```

Unit tests cover:
- TL serialization/deserialization
- Cryptographic operations
- Message handling
- Configuration parsing
- Connection management

Network tests (disabled by default) can test real connections to liteservers.

## Dependencies

- **BouncyCastle**: Cryptographic operations
- **Gson**: JSON parsing for configuration files
- **Apache HttpClient**: HTTP operations for config download
- **JUnit 5**: Testing framework

## Performance

- **Low Latency**: Direct TCP connections to liteservers
- **Connection Pooling**: Multiple connections for load balancing
- **Efficient Serialization**: Optimized TL serialization
- **Memory Efficient**: Minimal object allocation in hot paths

## Error Handling

The implementation includes comprehensive error handling:
- Connection timeouts and retries
- Cryptographic verification failures
- Malformed packet detection
- Network interruption recovery

## Thread Safety

- All public APIs are thread-safe
- Connection pools support concurrent access
- Message handlers are executed in separate threads

## Future Enhancements

- [ ] ADNL over UDP support
- [ ] Channel-based communication
- [ ] Advanced query caching
- [ ] Metrics and monitoring
- [ ] More TL query types
- [ ] WebSocket transport option

## Contributing

When adding new features:
1. Follow the existing code style
2. Add comprehensive tests
3. Update documentation
4. Ensure thread safety
5. Maintain protocol compliance

## License

This implementation is part of the ton4j project and follows the same licensing terms.
