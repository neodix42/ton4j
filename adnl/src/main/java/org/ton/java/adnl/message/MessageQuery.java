package org.ton.java.adnl.message;

import java.util.Arrays;

/**
 * ADNL query message
 * TL: adnl.message.query query_id:int256 query:bytes = adnl.Message
 */
public class MessageQuery {
    private byte[] id;
    private Object data;
    
    public MessageQuery() {}
    
    public MessageQuery(byte[] id, Object data) {
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
        return "MessageQuery{id=" + Arrays.toString(id) + ", data=" + data + "}";
    }
}
