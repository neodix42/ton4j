package org.ton.java.adnl;

import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class AdnlTest {

    private AdnlTransport clientTransport;
    private AdnlTransport serverTransport;
    private Client serverClient;
    private String serverPubKeyBase64;
    private static final int CLIENT_PORT = 30310;
    private static final int SERVER_PORT = 30311;
    private static final String SERVER_HOST = "127.0.0.1";
    private static final int TIMEOUT = 10;

    @Before
    public void setUp() throws Exception {
        // Create server
        serverClient = Client.generate();
        serverPubKeyBase64 = java.util.Base64.getEncoder().encodeToString(serverClient.getEd25519Public());

        serverTransport = new AdnlTransport(TIMEOUT);
        serverTransport.start(SERVER_PORT);

        // Create client
        clientTransport = new AdnlTransport(TIMEOUT);
        clientTransport.start(CLIENT_PORT);
    }

    @After
    public void tearDown() {
        if (clientTransport != null) {
            clientTransport.close();
        }
        if (serverTransport != null) {
            serverTransport.close();
        }
    }

    @Test
    public void testConnectToPeer() throws Exception {
        // Create a node representing the server
        Node serverNode = new Node(SERVER_HOST, SERVER_PORT, serverPubKeyBase64, clientTransport);

        // Connect to the server
        clientTransport.connectToPeer(serverNode);

        // Verify connection
        assertTrue(serverNode.isConnected());
        assertFalse(serverNode.getChannels().isEmpty());
    }

    @Test
    public void testSendQueryMessage() throws Exception {
        // Set up a query handler on the server
        final CountDownLatch latch = new CountDownLatch(1);
        final Map<String, Object> receivedData = new HashMap<>();

        serverTransport.setQueryHandler("dht.ping", new AdnlTransport.QueryHandler() {
            @Override
            public Map<String, Object> handle(Map<String, Object> data) {
                receivedData.putAll(data);
                latch.countDown();
                return data; // Echo back the data
            }
        });

        // Create a node representing the server
        Node serverNode = new Node(SERVER_HOST, SERVER_PORT, serverPubKeyBase64, clientTransport);

        // Connect to the server
        clientTransport.connectToPeer(serverNode);

        // Send a ping query
        Map<String, Object> pingData = new HashMap<>();
        pingData.put("random_id", CryptoUtils.getRandomBytes(8));

        List<Object> response = clientTransport.sendQueryMessageSync("dht.ping", pingData, serverNode);

        // Wait for the server to receive the query
        assertTrue(latch.await(5, TimeUnit.SECONDS));

        // Verify the response
        assertNotNull(response);
        assertFalse(response.isEmpty());

        // Verify the server received the query
        assertEquals(pingData.get("random_id"), receivedData.get("random_id"));
    }

    // No need for these methods as they're already implemented in AdnlTransport
}
