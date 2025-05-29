package org.ton.java.adnl.message;

import java.util.Arrays;

/**
 * ADNL confirm channel message
 * TL: adnl.message.confirmChannel key:int256 peer_key:int256 date:int = adnl.Message
 */
public class MessageConfirmChannel {
    private byte[] key;
    private byte[] peerKey;
    private int date;
    
    public MessageConfirmChannel() {}
    
    public MessageConfirmChannel(byte[] key, byte[] peerKey, int date) {
        this.key = key;
        this.peerKey = peerKey;
        this.date = date;
    }
    
    public byte[] getKey() {
        return key;
    }
    
    public void setKey(byte[] key) {
        this.key = key;
    }
    
    public byte[] getPeerKey() {
        return peerKey;
    }
    
    public void setPeerKey(byte[] peerKey) {
        this.peerKey = peerKey;
    }
    
    public int getDate() {
        return date;
    }
    
    public void setDate(int date) {
        this.date = date;
    }
    
    @Override
    public String toString() {
        return "MessageConfirmChannel{key=" + Arrays.toString(key) + 
               ", peerKey=" + Arrays.toString(peerKey) + ", date=" + date + "}";
    }
}
