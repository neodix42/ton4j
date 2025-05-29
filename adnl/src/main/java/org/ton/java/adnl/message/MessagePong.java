package org.ton.java.adnl.message;

/**
 * ADNL pong message
 * TL: adnl.pong value:long = adnl.Pong
 */
public class MessagePong {
    private long value;
    
    public MessagePong() {}
    
    public MessagePong(long value) {
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
        return "MessagePong{value=" + value + "}";
    }
}
