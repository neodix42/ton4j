package org.ton.java.adnl;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.crypto.Cipher;

public class AdnlTransport {

  private final Logger logger = Logger.getLogger(AdnlTransport.class.getName());

  private DatagramSocket socket;
  private SocketProtocol protocol;
  private final Map<String, Node> peersByHost = new HashMap<>();
  private final Map<byte[], Node> peersByKeyId = new HashMap<>();
  private final Map<String, CompletableFuture<List<Object>>> tasks = new ConcurrentHashMap<>();
  private final TLGenerator.TLSchemas schemas;
  private final Client client;
  private final byte[] localId;
  private final Map<byte[], AdnlChannel> channels = new HashMap<>();
  private int timeout;
  private Thread listenerThread;
  private volatile boolean running = false;

  public AdnlTransport(int timeout) {
    this.timeout = timeout;
    this.schemas = TLGenerator.withDefaultSchemas();
    this.client = Client.generate();
    this.localId = this.client.getKeyId();
  }

  public void start(int port) throws Exception {
    this.protocol = new SocketProtocol(timeout);
    this.protocol.start(port);
    this.socket = protocol.getSocket();
    startListener();
  }

  private void startListener() {
    running = true;
    listenerThread = new Thread(() -> {
      while (running) {
        try {
          DatagramPacket packet = protocol.receive();
          byte[] data = Arrays.copyOf(packet.getData(), packet.getLength());
          processIncomingPacket(data, packet.getSocketAddress());
        } catch (InterruptedException e) {
          if (running) {
            logger.log(Level.WARNING, "Listener interrupted", e);
          }
        } catch (Exception e) {
          logger.log(Level.SEVERE, "Error processing packet", e);
        }
      }
    });
    listenerThread.setDaemon(true);
    listenerThread.start();
  }

  public void close() {
    running = false;
    if (listenerThread != null) {
      listenerThread.interrupt();
    }
    protocol.stop();
  }

  public void connectToPeer(Node peer) {
    try {
      // Create a channel with the peer
      int timestamp = (int) (System.currentTimeMillis() / 1000);
      Client channelClient = Client.generate();
      
      Map<String, Object> createChannelMessage = new HashMap<>();
      createChannelMessage.put("@type", "adnl.message.createChannel");
      createChannelMessage.put("key", channelClient.getEd25519Public());
      createChannelMessage.put("date", timestamp);
      
      Map<String, Object> defaultMessage = new HashMap<>();
      defaultMessage.put("@type", "adnl.message.query");
      defaultMessage.put("query_id", CryptoUtils.getRandomBytes(32));
      
      Map<String, Object> queryData = new HashMap<>();
      defaultMessage.put("query", schemas.serialize(schemas.getByName("dht.getSignedAddressList"), queryData));
      
      Map<String, Object> from = new HashMap<>();
      from.put("@type", "pub.ed25519");
      from.put("key", CryptoUtils.hex(client.getEd25519Public()));
      
      Map<String, Object> data = new HashMap<>();
      data.put("from", schemas.serialize(schemas.getByName("pub.ed25519"), from));
      
      List<Map<String, Object>> messages = new ArrayList<>();
      messages.add(createChannelMessage);
      messages.add(defaultMessage);
      data.put("messages", messages);
      
      Map<String, Object> address = new HashMap<>();
      address.put("addrs", new ArrayList<>());
      address.put("version", timestamp);
      address.put("reinit_date", timestamp);
      address.put("priority", 0);
      address.put("expire_at", 0);
      data.put("address", address);
      
      data.put("recv_addr_list_version", timestamp);
      data.put("reinit_date", timestamp);
      data.put("dst_reinit_date", 0);
      
      List<Object> messages_result = sendMessageOutsideChannel(data, peer);
      
      Map<String, Object> confirmChannel = (Map<String, Object>) messages_result.get(0);
      if (!"adnl.message.confirmChannel".equals(confirmChannel.get("@type"))) {
        throw new RuntimeException("Expected adnl.message.confirmChannel, got " + confirmChannel.get("@type"));
      }
      
      byte[] peerKey = (byte[]) confirmChannel.get("key");
      Server channelPeer = new Server(peer.getHost(), peer.getPort(), peerKey);
      AdnlChannel channel = new AdnlChannel(channelClient, channelPeer, client.getKeyId(), peer.getKeyId());
      
      channels.put(peer.getKeyId(), channel);
      peer.getChannels().add(channel);
      
      peer.startPing();
      peer.setConnected(true);
      peersByKeyId.put(peer.getKeyId(), peer);
      peersByHost.put(peer.getHost() + ":" + peer.getPort(), peer);
      
    } catch (Exception e) {
      logger.log(Level.SEVERE, "Error connecting to peer", e);
      throw new RuntimeException("Failed to connect to peer", e);
    }
  }

  private List<Object> sendMessageOutsideChannel(Map<String, Object> data, Node peer) throws InterruptedException, ExecutionException, TimeoutException {
    data = preparePacketContentMsg(data, peer);
    int sendingSeqno = (int) data.get("seqno");
    
    List<CompletableFuture<Object>> futures = createFutures(data);
    
    byte[] serialized = schemas.serialize(schemas.getByName("adnl.packetContents"), computeFlagsForPacket(data));
    byte[] signature = client.sign(serialized);
    
    Map<String, Object> dataWithSignature = new HashMap<>(data);
    dataWithSignature.put("signature", signature);
    byte[] serializedWithSignature = schemas.serialize(schemas.getByName("adnl.packetContents"), computeFlagsForPacket(dataWithSignature));
    
    byte[] checksum = sha256(serializedWithSignature);
    byte[] sharedKey = CryptoUtils.getSharedKey(client.getX25519Private(), peer.getX25519Public());
    Cipher cipher = CryptoUtils.createAESCtrCipher(sharedKey, checksum, Cipher.ENCRYPT_MODE);
    byte[] encrypted = CryptoUtils.aesCtrTransform(cipher, serializedWithSignature);
    
    byte[] result = new byte[peer.getKeyId().length + checksum.length + encrypted.length];
    System.arraycopy(peer.getKeyId(), 0, result, 0, peer.getKeyId().length);
    System.arraycopy(checksum, 0, result, peer.getKeyId().length, checksum.length);
    System.arraycopy(encrypted, 0, result, peer.getKeyId().length + checksum.length, encrypted.length);
    
    try {
      DatagramPacket packet = new DatagramPacket(result, result.length, new InetSocketAddress(peer.getHost(), peer.getPort()));
      socket.send(packet);
    } catch (IOException e) {
      logger.log(Level.SEVERE, "Error sending packet", e);
      throw new RuntimeException("Failed to send packet", e);
    }
    
    if (peer.getSeqno() == sendingSeqno) {
      peer.incrementSeqno();
    } else {
      throw new RuntimeException("Sending seqno " + sendingSeqno + ", client seqno: " + peer.getSeqno());
    }
    
    if (!futures.isEmpty()) {
      List<Object> results = new ArrayList<>();
      for (CompletableFuture<Object> future : futures) {
        results.add(future.get(timeout, TimeUnit.SECONDS));
      }
      return results;
    }
    
    return new ArrayList<>();
  }

  private List<CompletableFuture<Object>> createFutures(Map<String, Object> data) {
    List<CompletableFuture<Object>> futures = new ArrayList<>();
    
    if (data.containsKey("message")) {
      CompletableFuture<Object> future = processOutgoingMessage((Map<String, Object>) data.get("message"));
      if (future != null) {
        futures.add(future);
      }
    }
    
    if (data.containsKey("messages")) {
      List<Map<String, Object>> messages = (List<Map<String, Object>>) data.get("messages");
      for (Map<String, Object> message : messages) {
        CompletableFuture<Object> future = processOutgoingMessage(message);
        if (future != null) {
          futures.add(future);
        }
      }
    }
    
    return futures;
  }

  private CompletableFuture<Object> processOutgoingMessage(Map<String, Object> message) {
    String type = (String) message.get("@type");
    CompletableFuture<Object> future = new CompletableFuture<>();
    
    if ("adnl.message.query".equals(type)) {
      byte[] queryId = (byte[]) message.get("query_id");
      tasks.put(CryptoUtils.hex(queryId), (CompletableFuture) future);
    } else if ("adnl.message.createChannel".equals(type)) {
      Object keyObj = message.get("key");
      String key;
      if (keyObj instanceof byte[]) {
        key = CryptoUtils.hex((byte[]) keyObj);
      } else {
        key = keyObj.toString();
      }
      tasks.put(key, (CompletableFuture) future);
    } else {
      return null;
    }
    
    return future;
  }

  private Map<String, Object> preparePacketContentMsg(Map<String, Object> data, Node peer) {
    if (!data.containsKey("rand1") || !data.containsKey("rand2")) {
      data.put("rand1", CryptoUtils.getRandomBytes(15));
      data.put("rand2", CryptoUtils.getRandomBytes(15));
    }
    
    if (!data.containsKey("seqno")) {
      if (peer == null) {
        throw new RuntimeException("Must either specify seqno in data or provide peer to method");
      }
      data.put("seqno", peer.getSeqno());
    }
    
    if (!data.containsKey("confirm_seqno")) {
      if (peer == null) {
        throw new RuntimeException("Must either specify confirm_seqno in data or provide peer to method");
      }
      data.put("confirm_seqno", peer.getConfirmSeqno());
    }
    
    return data;
  }

  private Map<String, Object> computeFlagsForPacket(Map<String, Object> data) {
    int flags = 0;
    if (data.containsKey("from")) flags |= 1 << 0;
    if (data.containsKey("from_short")) flags |= 1 << 1;
    if (data.containsKey("message")) flags |= 1 << 2;
    if (data.containsKey("messages")) flags |= 1 << 3;
    if (data.containsKey("address")) flags |= 1 << 4;
    if (data.containsKey("priority_address")) flags |= 1 << 5;
    if (data.containsKey("seqno")) flags |= 1 << 6;
    if (data.containsKey("confirm_seqno")) flags |= 1 << 7;
    if (data.containsKey("recv_addr_list_version")) flags |= 1 << 8;
    if (data.containsKey("recv_priority_addr_list_version")) flags |= 1 << 9;
    if (data.containsKey("reinit_date") || data.containsKey("dst_reinit_date")) flags |= 1 << 10;
    if (data.containsKey("signature")) flags |= 1 << 11;
    
    data.put("flags", flags);
    return data;
  }

  private void processIncomingPacket(byte[] packet, java.net.SocketAddress addr) {
    try {
      byte[] keyId = Arrays.copyOf(packet, 32);
      
      if (Arrays.equals(keyId, localId)) {
        // Packet for us directly
        byte[] serverPublicKey = Arrays.copyOfRange(packet, 32, 64);
        byte[] checksum = Arrays.copyOfRange(packet, 64, 96);
        byte[] encrypted = Arrays.copyOfRange(packet, 96, packet.length);
        
        Server peerCrypto = new Server("", 0, serverPublicKey);
        byte[] sharedKey = CryptoUtils.getSharedKey(client.getX25519Private(), peerCrypto.getX25519Public());
        
        Cipher cipher = CryptoUtils.createAESCtrCipher(sharedKey, checksum, Cipher.DECRYPT_MODE);
        byte[] decrypted = CryptoUtils.aesCtrTransform(cipher, encrypted);
        
        byte[] calculatedChecksum = sha256(decrypted);
        if (!Arrays.equals(calculatedChecksum, checksum)) {
          logger.warning("Invalid checksum in packet");
          return;
        }
        
        Object[] deserialized = schemas.deserialize(decrypted);
        Map<String, Object> response = (Map<String, Object>) deserialized[0];
        
        Node peer = null;
        if (response.containsKey("from_short")) {
          Map<String, Object> fromShort = (Map<String, Object>) response.get("from_short");
          String id = (String) fromShort.get("id");
          peer = peersByKeyId.get(CryptoUtils.hexToSignedBytes(id));
        }
        
        if (peer != null) {
          int receivedSeqno = (int) response.getOrDefault("seqno", 0);
          if (receivedSeqno > peer.getConfirmSeqno()) {
            peer.setConfirmSeqno(receivedSeqno);
          }
        }
        
        processMessage(response, peer);
      } else {
        // Try to find channel
        for (Map.Entry<byte[], AdnlChannel> entry : channels.entrySet()) {
          AdnlChannel channel = entry.getValue();
          if (Arrays.equals(keyId, channel.getServerKeyId())) {
            byte[] checksum = Arrays.copyOfRange(packet, 32, 64);
            byte[] encrypted = Arrays.copyOfRange(packet, 64, packet.length);
            byte[] decrypted = channel.decrypt(encrypted, checksum);
            
            Object[] deserialized = schemas.deserialize(decrypted);
            Map<String, Object> response = (Map<String, Object>) deserialized[0];
            
            Node peer = peersByKeyId.get(entry.getKey());
            if (peer != null) {
              int receivedSeqno = (int) response.getOrDefault("seqno", 0);
              if (receivedSeqno > peer.getConfirmSeqno()) {
                peer.setConfirmSeqno(receivedSeqno);
              }
            }
            
            processMessage(response, peer);
            return;
          }
        }
        
        logger.warning("Unknown key ID from node: " + CryptoUtils.hex(keyId));
      }
    } catch (Exception e) {
      logger.log(Level.SEVERE, "Error processing incoming packet", e);
    }
  }

  private void processMessage(Map<String, Object> response, Node peer) {
    if (response.containsKey("message")) {
      processIncomingMessage((Map<String, Object>) response.get("message"), peer);
    }
    
    if (response.containsKey("messages")) {
      List<Map<String, Object>> messages = (List<Map<String, Object>>) response.get("messages");
      for (Map<String, Object> message : messages) {
        processIncomingMessage(message, peer);
      }
    }
  }

  private final Map<String, QueryHandler> queryHandlers = new ConcurrentHashMap<>();
  private final Map<String, CustomMessageHandler> customHandlers = new ConcurrentHashMap<>();
  
  private void processIncomingMessage(Map<String, Object> message, Node peer) {
    String type = (String) message.get("@type");
    
    if ("adnl.message.answer".equals(type)) {
      byte[] queryId = (byte[]) message.get("query_id");
      String queryIdHex = CryptoUtils.hex(queryId);
      CompletableFuture<List<Object>> future = tasks.remove(queryIdHex);
      if (future != null) {
        List<Object> result = new ArrayList<>();
        result.add(message.get("answer"));
        future.complete(result);
      }
    } else if ("adnl.message.confirmChannel".equals(type)) {
      Object peerKeyObj = message.get("peer_key");
      String peerKey;
      if (peerKeyObj instanceof byte[]) {
        peerKey = CryptoUtils.hex((byte[]) peerKeyObj);
      } else {
        peerKey = peerKeyObj.toString();
      }
      CompletableFuture<List<Object>> future = tasks.remove(peerKey);
      if (future != null) {
        List<Object> result = new ArrayList<>();
        result.add(message);
        future.complete(result);
      }
    } else if ("adnl.message.query".equals(type)) {
      if (peer == null) {
        logger.info("Received query message from unknown peer: " + message);
        return;
      }
      processQueryMessage(message, peer);
    } else if ("adnl.message.custom".equals(type)) {
      if (peer == null) {
        logger.info("Received custom message from unknown peer: " + message);
        return;
      }
      processCustomMessage(message, peer);
    }
  }
  
  private void processQueryMessage(Map<String, Object> message, Node peer) {
    try {
      byte[] queryId = (byte[]) message.get("query_id");
      byte[] queryBytes = (byte[]) message.get("query");
      
      Object[] deserialized = schemas.deserialize(queryBytes);
      Map<String, Object> query = (Map<String, Object>) deserialized[0];
      String queryType = (String) query.get("@type");
      
      QueryHandler handler = queryHandlers.get(queryType);
      if (handler == null) {
        handler = queryHandlers.get("default");
      }
      
      if (handler != null) {
        Map<String, Object> response = handler.handle(query);
        if (response != null) {
          sendAnswerMessage(response, queryId, peer);
        }
      }
    } catch (Exception e) {
      logger.log(Level.SEVERE, "Error processing query message", e);
    }
  }
  
  private void processCustomMessage(Map<String, Object> message, Node peer) {
    try {
      Map<String, Object> data = (Map<String, Object>) message.get("data");
      String dataType = (String) data.get("@type");
      
      CustomMessageHandler handler = customHandlers.get(dataType);
      if (handler == null) {
        handler = customHandlers.get("default");
      }
      
      if (handler != null) {
        Map<String, Object> response = handler.handle(data);
        if (response != null) {
          sendCustomMessage(response, peer);
        }
      }
    } catch (Exception e) {
      logger.log(Level.SEVERE, "Error processing custom message", e);
    }
  }
  
  private void sendAnswerMessage(Map<String, Object> response, byte[] queryId, Node peer) {
    try {
      Map<String, Object> message = new HashMap<>();
      message.put("@type", "adnl.message.answer");
      message.put("query_id", queryId);
      message.put("answer", response);
      
      Map<String, Object> packetData = new HashMap<>();
      packetData.put("message", message);
      
      AdnlChannel channel = getChannelForPeer(peer);
      sendMessageInChannel(packetData, channel, peer);
    } catch (Exception e) {
      logger.log(Level.SEVERE, "Error sending answer message", e);
    }
  }
  
  private void sendCustomMessage(Map<String, Object> data, Node peer) {
    try {
      Map<String, Object> message = new HashMap<>();
      message.put("@type", "adnl.message.custom");
      message.put("data", data);
      
      Map<String, Object> packetData = new HashMap<>();
      packetData.put("message", message);
      
      AdnlChannel channel = getChannelForPeer(peer);
      sendMessageInChannel(packetData, channel, peer);
    } catch (Exception e) {
      logger.log(Level.SEVERE, "Error sending custom message", e);
    }
  }

  public List<Object> sendQueryMessageSync(String schemaName, Map<String, Object> data, Node peer) {
    try {
      Map<String, Object> message = new HashMap<>();
      message.put("@type", "adnl.message.query");
      message.put("query_id", CryptoUtils.getRandomBytes(32));
      message.put("query", schemas.serialize(schemas.getByName(schemaName), data));
      
      Map<String, Object> packetData = new HashMap<>();
      packetData.put("message", message);
      
      AdnlChannel channel = getChannelForPeer(peer);
      
      return sendMessageInChannel(packetData, channel, peer);
    } catch (Exception e) {
      logger.log(Level.SEVERE, "Error sending query message", e);
      throw new RuntimeException("Failed to send query message", e);
    }
  }

  public void sendQueryMessage(String schemaName, Map<String, Object> data, Node peer) {
    new Thread(() -> {
      try {
        sendQueryMessageSync(schemaName, data, peer);
      } catch (Exception e) {
        logger.log(Level.SEVERE, "Error in async query message", e);
      }
    }).start();
  }

  private List<Object> sendMessageInChannel(Map<String, Object> data, AdnlChannel channel, Node peer) throws InterruptedException, ExecutionException, TimeoutException {
    data = preparePacketContentMsg(data, peer);
    int sendingSeqno = (int) data.get("seqno");
    
    List<CompletableFuture<Object>> futures = createFutures(data);
    
    if (channel == null) {
      if (peer.getChannels().isEmpty()) {
        throw new RuntimeException("Peer has no channels and channel was not provided");
      }
      channel = peer.getChannels().get(0);
    }
    
    if (peer.getSeqno() == sendingSeqno) {
      peer.incrementSeqno();
    } else {
      throw new RuntimeException("Sending seqno " + sendingSeqno + ", client seqno: " + peer.getSeqno());
    }
    
    byte[] serialized = schemas.serialize(schemas.getByName("adnl.packetContents"), data);
    byte[] encrypted = channel.encrypt(serialized);
    
    try {
      DatagramPacket packet = new DatagramPacket(encrypted, encrypted.length, new InetSocketAddress(peer.getHost(), peer.getPort()));
      socket.send(packet);
    } catch (IOException e) {
      logger.log(Level.SEVERE, "Error sending packet", e);
      throw new RuntimeException("Failed to send packet", e);
    }
    
    if (!futures.isEmpty()) {
      List<Object> results = new ArrayList<>();
      for (CompletableFuture<Object> future : futures) {
        results.add(future.get(timeout, TimeUnit.SECONDS));
      }
      return results;
    }
    
    return new ArrayList<>();
  }

  private AdnlChannel getChannelForPeer(Node peer) {
    if (peer.getChannels().isEmpty()) {
      return null;
    }
    return peer.getChannels().get(0);
  }

  public void removePeer(byte[] keyId) {
    Node peer = peersByKeyId.remove(keyId);
    if (peer != null) {
      peersByHost.remove(peer.getHost() + ":" + peer.getPort());
      channels.remove(keyId);
    }
  }

  private byte[] sha256(byte[] data) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      return digest.digest(data);
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException("SHA-256 algorithm not available", e);
    }
  }
  
  /**
   * Interface for handling query messages
   */
  public interface QueryHandler {
    /**
     * Handle a query message
     * @param data Query data
     * @return Response data, or null if no response should be sent
     */
    Map<String, Object> handle(Map<String, Object> data);
  }
  
  /**
   * Interface for handling custom messages
   */
  public interface CustomMessageHandler {
    /**
     * Handle a custom message
     * @param data Custom message data
     * @return Response data, or null if no response should be sent
     */
    Map<String, Object> handle(Map<String, Object> data);
  }
  
  /**
   * Set a query handler for a specific query type
   * @param type Query type
   * @param handler Handler
   */
  public void setQueryHandler(String type, QueryHandler handler) {
    queryHandlers.put(type, handler);
  }
  
  /**
   * Set a default query handler
   * @param handler Handler
   */
  public void setDefaultQueryHandler(QueryHandler handler) {
    queryHandlers.put("default", handler);
  }
  
  /**
   * Set a custom message handler for a specific message type
   * @param type Message type
   * @param handler Handler
   */
  public void setCustomMessageHandler(String type, CustomMessageHandler handler) {
    customHandlers.put(type, handler);
  }
  
  /**
   * Set a default custom message handler
   * @param handler Handler
   */
  public void setDefaultCustomMessageHandler(CustomMessageHandler handler) {
    customHandlers.put("default", handler);
  }
}
