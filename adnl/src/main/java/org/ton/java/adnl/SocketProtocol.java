package org.ton.java.adnl;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Socket protocol for ADNL
 */
public class SocketProtocol {
    private static final Logger logger = Logger.getLogger(SocketProtocol.class.getName());
    private static final int BUFFER_SIZE = 65536;
    
    private DatagramSocket socket;
    private final int timeout;
    private final BlockingQueue<DatagramPacket> receiveQueue = new LinkedBlockingQueue<>();
    private Thread receiveThread;
    private volatile boolean running = false;
    
    /**
     * Create a socket protocol with the specified timeout
     * @param timeout Timeout in seconds
     */
    public SocketProtocol(int timeout) {
        this.timeout = timeout;
    }
    
    /**
     * Start the socket protocol on the specified port
     * @param port Port
     * @throws SocketException If the socket cannot be created
     */
    public void start(int port) throws SocketException {
        socket = new DatagramSocket(port);
        socket.setSoTimeout(1000); // 1 second timeout for receive
        
        running = true;
        receiveThread = new Thread(this::receiveLoop);
        receiveThread.setDaemon(true);
        receiveThread.start();
        
        logger.info("Socket protocol started on port " + port);
    }
    
    /**
     * Stop the socket protocol
     */
    public void stop() {
        running = false;
        if (receiveThread != null) {
            receiveThread.interrupt();
        }
        if (socket != null) {
            socket.close();
        }
        logger.info("Socket protocol stopped");
    }
    
    /**
     * Get the socket
     * @return Socket
     */
    public DatagramSocket getSocket() {
        return socket;
    }
    
    /**
     * Send a packet
     * @param packet Packet
     * @throws IOException If the packet cannot be sent
     */
    public void send(DatagramPacket packet) throws IOException {
        if (socket == null || socket.isClosed()) {
            throw new IOException("Socket is closed");
        }
        socket.send(packet);
    }
    
    /**
     * Receive a packet
     * @return Packet
     * @throws InterruptedException If the thread is interrupted
     * @throws IOException If the packet cannot be received
     */
    public DatagramPacket receive() throws InterruptedException, IOException {
        DatagramPacket packet = receiveQueue.poll(timeout, TimeUnit.SECONDS);
        if (packet == null) {
            throw new SocketTimeoutException("Receive timeout");
        }
        return packet;
    }
    
    /**
     * Receive loop
     */
    private void receiveLoop() {
        byte[] buffer = new byte[BUFFER_SIZE];
        
        while (running) {
            try {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);
                
                // Copy the data to avoid buffer reuse issues
                byte[] data = new byte[packet.getLength()];
                System.arraycopy(packet.getData(), packet.getOffset(), data, 0, packet.getLength());
                
                DatagramPacket copy = new DatagramPacket(data, data.length, packet.getSocketAddress());
                receiveQueue.offer(copy);
            } catch (SocketTimeoutException e) {
                // Ignore timeout, just continue the loop
            } catch (IOException e) {
                if (running) {
                    logger.log(Level.WARNING, "Error receiving packet", e);
                }
            }
        }
    }
}
