package org.ton.java.adnl.packet;

/**
 * ADNL packet flags constants
 * Mirrors the Go implementation flag constants
 */
public class PacketFlags {
    public static final int FLAG_FROM = 0x1;
    public static final int FLAG_FROM_SHORT = 0x2;
    public static final int FLAG_ONE_MESSAGE = 0x4;
    public static final int FLAG_MULTIPLE_MESSAGES = 0x8;
    public static final int FLAG_ADDRESS = 0x10;
    public static final int FLAG_PRIORITY_ADDRESS = 0x20;
    public static final int FLAG_SEQNO = 0x40;
    public static final int FLAG_CONFIRM_SEQNO = 0x80;
    public static final int FLAG_RECV_ADDR_LIST_VER = 0x100;
    public static final int FLAG_RECV_PRIORITY_ADDR_VER = 0x200;
    public static final int FLAG_REINIT_DATE = 0x400;
    public static final int FLAG_SIGNATURE = 0x800;
    public static final int FLAG_PRIORITY = 0x1000;
    public static final int FLAG_ALL = 0x1fff;
    
    private PacketFlags() {
        // Utility class
    }
}
