package org.ton.java.adnl.message;

import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility methods for ADNL messages
 */
public class MessageUtils {
    
    /**
     * Split a large message into parts
     * Mirrors the Go implementation: splitMessage(data []byte, mtu int) []MessagePart
     * @param data Message data to split
     * @param mtu Maximum transmission unit size
     * @return List of message parts
     */
    public static List<MessagePart> splitMessage(byte[] data, int mtu) {
        try {
            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            byte[] hash = sha256.digest(data);
            
            int numParts = data.length / mtu;
            if (data.length % mtu != 0) {
                numParts++;
            }
            
            List<MessagePart> result = new ArrayList<>(numParts);
            
            for (int i = 0; i < numParts; i++) {
                int offset = i * mtu;
                int length = Math.min(mtu, data.length - offset);
                
                byte[] partData = new byte[length];
                System.arraycopy(data, offset, partData, 0, length);
                
                MessagePart part = new MessagePart(
                    hash,
                    data.length,
                    offset,
                    partData
                );
                
                result.add(part);
            }
            
            return result;
        } catch (Exception e) {
            throw new RuntimeException("Error splitting message", e);
        }
    }
}
