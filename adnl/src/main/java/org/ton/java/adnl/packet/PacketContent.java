package org.ton.java.adnl.packet;

import java.util.List;

/**
 * ADNL packet content structure
 * Mirrors the Go implementation of PacketContent
 * TL: adnl.packetContents rand1:bytes flags:# from:flags.0?PublicKey from_short:flags.1?adnl.id.short 
 *     message:flags.2?adnl.Message messages:flags.3?(vector adnl.Message) 
 *     address:flags.4?adnl.addressList priority_address:flags.5?adnl.addressList 
 *     seqno:flags.6?long confirm_seqno:flags.7?long recv_addr_list_version:flags.8?int 
 *     recv_priority_addr_list_version:flags.9?int reinit_date:flags.10?int 
 *     dst_reinit_date:flags.10?int signature:flags.11?bytes rand2:bytes = adnl.PacketContents
 */
public class PacketContent {
    private byte[] rand1;
    private PublicKeyED25519 from;
    private byte[] fromIdShort;
    private List<Object> messages;
    private Object address; // AddressList
    private Object priorityAddress; // AddressList
    private Long seqno;
    private Long confirmSeqno;
    private Integer recvAddrListVersion;
    private Integer recvPriorityAddrListVersion;
    private Integer reinitDate;
    private Integer dstReinitDate;
    private byte[] signature;
    private byte[] rand2;
    
    public PacketContent() {}
    
    // Getters and setters
    public byte[] getRand1() {
        return rand1;
    }
    
    public void setRand1(byte[] rand1) {
        this.rand1 = rand1;
    }
    
    public PublicKeyED25519 getFrom() {
        return from;
    }
    
    public void setFrom(PublicKeyED25519 from) {
        this.from = from;
    }
    
    public byte[] getFromIdShort() {
        return fromIdShort;
    }
    
    public void setFromIdShort(byte[] fromIdShort) {
        this.fromIdShort = fromIdShort;
    }
    
    public List<Object> getMessages() {
        return messages;
    }
    
    public void setMessages(List<Object> messages) {
        this.messages = messages;
    }
    
    public Object getAddress() {
        return address;
    }
    
    public void setAddress(Object address) {
        this.address = address;
    }
    
    public Object getPriorityAddress() {
        return priorityAddress;
    }
    
    public void setPriorityAddress(Object priorityAddress) {
        this.priorityAddress = priorityAddress;
    }
    
    public Long getSeqno() {
        return seqno;
    }
    
    public void setSeqno(Long seqno) {
        this.seqno = seqno;
    }
    
    public Long getConfirmSeqno() {
        return confirmSeqno;
    }
    
    public void setConfirmSeqno(Long confirmSeqno) {
        this.confirmSeqno = confirmSeqno;
    }
    
    public Integer getRecvAddrListVersion() {
        return recvAddrListVersion;
    }
    
    public void setRecvAddrListVersion(Integer recvAddrListVersion) {
        this.recvAddrListVersion = recvAddrListVersion;
    }
    
    public Integer getRecvPriorityAddrListVersion() {
        return recvPriorityAddrListVersion;
    }
    
    public void setRecvPriorityAddrListVersion(Integer recvPriorityAddrListVersion) {
        this.recvPriorityAddrListVersion = recvPriorityAddrListVersion;
    }
    
    public Integer getReinitDate() {
        return reinitDate;
    }
    
    public void setReinitDate(Integer reinitDate) {
        this.reinitDate = reinitDate;
    }
    
    public Integer getDstReinitDate() {
        return dstReinitDate;
    }
    
    public void setDstReinitDate(Integer dstReinitDate) {
        this.dstReinitDate = dstReinitDate;
    }
    
    public byte[] getSignature() {
        return signature;
    }
    
    public void setSignature(byte[] signature) {
        this.signature = signature;
    }
    
    public byte[] getRand2() {
        return rand2;
    }
    
    public void setRand2(byte[] rand2) {
        this.rand2 = rand2;
    }
}
