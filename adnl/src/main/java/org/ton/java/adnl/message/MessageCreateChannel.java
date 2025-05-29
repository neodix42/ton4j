package org.ton.java.adnl.message;

import java.util.Arrays;

/**
 * ADNL create channel message
 * TL: adnl.message.createChannel key:int256 date:int = adnl.Message
 */
public class MessageCreateChannel {
    private byte[] key;
    private int date;
    
    public MessageCreateChannel() {}
    
    public MessageCreateChannel(byte[] key, int date) {
        this.key = key;
        this.date = date;
    }
    
    public byte[] getKey() {
        return key;
    }
    
    public void setKey(byte[] key) {
        this.key = key;
    }
    
    public int getDate() {
        return date;
    }
    
    public void setDate(int date) {
        this.date = date;
    }
    
    @Override
    public String toString() {
        return "MessageCreateChannel{key=" + Arrays.toString(key) + ", date=" + date + "}";
    }
}
