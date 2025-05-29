package org.ton.java.adnl.message;

/**
 * ADNL custom message
 * TL: adnl.message.custom data:bytes = adnl.Message
 */
public class MessageCustom {
    private Object data;
    
    public MessageCustom() {}
    
    public MessageCustom(Object data) {
        this.data = data;
    }
    
    public Object getData() {
        return data;
    }
    
    public void setData(Object data) {
        this.data = data;
    }
    
    @Override
    public String toString() {
        return "MessageCustom{data=" + data + "}";
    }
}
