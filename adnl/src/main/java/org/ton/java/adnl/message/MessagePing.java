package org.ton.java.adnl.message;

/**
 * ADNL ping message
 * TL: adnl.ping value:long = adnl.Pong
 */
public class MessagePing {
    private long value;
    
    public MessagePing() {}
    
    public MessagePing(long value) {
        this.value = value;
    }
    
    public long getValue() {
        return value;
    }
    
    public void setValue(long value) {
        this.value = value;
    }
    
    @Override
    public String toString() {
        return "MessagePing{value=" + value + "}";
    }
}
