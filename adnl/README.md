# ADNL (Abstract Datagram Network Layer) Implementation

This module provides a Java implementation of the ADNL protocol used in the TON blockchain.

## Overview

ADNL is a protocol used for peer-to-peer communication in the TON network. It provides:

- Secure communication using Ed25519 and X25519 keys
- Channel-based communication
- Query/response messaging
- Custom message handling

## Usage

### Creating a Transport

```java
// Create a transport with a timeout of 10 seconds
AdnlTransport transport = new AdnlTransport(10);

// Start the transport on port 30310
transport.start(30310);
```

### Connecting to a Peer

```java
// Create a node representing the peer
Node peer = new Node("example.com", 30310, "base64EncodedPublicKey", transport);

// Connect to the peer
transport.connectToPeer(peer);
```

### Sending a Query

```java
// Create query data
Map<String, Object> data = new HashMap<>();
data.put("random_id", CryptoUtils.getRandomBytes(8));

// Send the query synchronously
List<Object> response = transport.sendQueryMessageSync("dht.ping", data, peer);

// Or asynchronously
transport.sendQueryMessage("dht.ping", data, peer);
```

### Handling Queries

```java
// Set a handler for a specific query type
transport.setQueryHandler("dht.ping", new AdnlTransport.QueryHandler() {
    @Override
    public Map<String, Object> handle(Map<String, Object> data) {
        // Process the query
        return responseData; // Return response data
    }
});

// Set a default handler for all query types
transport.setDefaultQueryHandler(new AdnlTransport.QueryHandler() {
    @Override
    public Map<String, Object> handle(Map<String, Object> data) {
        // Process the query
        return responseData; // Return response data
    }
});
```

### Handling Custom Messages

```java
// Set a handler for a specific custom message type
transport.setCustomMessageHandler("custom.message", new AdnlTransport.CustomMessageHandler() {
    @Override
    public Map<String, Object> handle(Map<String, Object> data) {
        // Process the message
        return responseData; // Return response data
    }
});

// Set a default handler for all custom message types
transport.setDefaultCustomMessageHandler(new AdnlTransport.CustomMessageHandler() {
    @Override
    public Map<String, Object> handle(Map<String, Object> data) {
        // Process the message
        return responseData; // Return response data
    }
});
```

### Closing the Transport

```java
// Close the transport when done
transport.close();
```

## Components

- `AdnlTransport`: Main class for ADNL communication
- `AdnlChannel`: Handles encrypted communication between peers
- `Client`: Represents a client with Ed25519 and X25519 keys
- `Server`: Represents a server with Ed25519 and X25519 keys
- `Node`: Represents a peer node in the network
- `TLGenerator`: Handles serialization/deserialization of messages
- `CryptoUtils`: Provides cryptographic utilities

## Dependencies

- BouncyCastle for cryptography
- TweetNaCl for Ed25519/X25519 operations
