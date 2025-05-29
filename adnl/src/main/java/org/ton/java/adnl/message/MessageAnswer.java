package org.ton.java.adnl.message;

import java.util.Arrays;

/**
 * ADNL answer message
 * TL: adnl.message.answer query_id:int256 answer:bytes = adnl.Message
 */
public class MessageAnswer {
    private byte[] id;
    private Object data;
    
    public MessageAnswer() {}
    
    public MessageAnswer(byte[] id, Object data) {
        this.id = id;
        this.data = data;
    }
    
    public byte[] getId() {
        return id;
    }
    
    public void setId(byte[] id) {
        this.id = id;
    }
    
    public Object getData() {
        return data;
    }
    
    public void setData(Object data) {
        this.data = data;
    }
    
    @Override
    public String toString() {
        return "MessageAnswer{id=" + Arrays.toString(id) + ", data=" + data + "}";
    }
}
