package org.ton.java.address;

import org.ton.java.utils.Utils;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;

import static java.util.Objects.isNull;

public class Address {

    private static final byte bounceable_tag = 0x11;
    private static final byte non_bounceable_tag = 0x51;
    private static final int test_flag = 0x80;

    public byte wc;
    public byte[] hashPart;
    public boolean isTestOnly;
    public boolean isUserFriendly;
    public boolean isBounceable;
    public boolean isUrlSafe;

    private Address() {

    }

    public Address(String address) {

        if (isNull(address)) {
            throw new IllegalArgumentException("Address is null");
        }

        if (!address.contains(":")) {
            if (address.contains("-") || address.contains("_")) {
                isUrlSafe = true;
                //convert to unsafe URL
                address = address.replace("-", "+").replace("_", "/");
            } else {
                isUrlSafe = false;
            }
        }

        if (address.indexOf(':') > -1) {
            String[] arr = address.split(":");

            if (arr.length != 2) {
                throw new Error("Invalid address " + address);
            }

            byte wcInternal = Byte.parseByte(arr[0]);

            if (wcInternal != 0 && wcInternal != -1) {
                throw new Error("Invalid address wc " + address);
            }

            String hex = arr[1];
            if (hex.length() != 64) {
                if (hex.length() == 63) {
                    hex = "0" + hex;
                } else {
                    throw new Error("Invalid address hex " + address);
                }
            }

            isUserFriendly = false;
            wc = wcInternal;
            hashPart = Utils.hexToBytes(hex);
            isTestOnly = false;
            isBounceable = false;
        } else {
            isUserFriendly = true;
            Address parseResult = parseFriendlyAddress(address);
            wc = parseResult.wc;
            hashPart = parseResult.hashPart;
            isTestOnly = parseResult.isTestOnly;
            isBounceable = parseResult.isBounceable;
        }
    }

    public Address(Address address) {

        if (isNull(address)) {
            throw new IllegalArgumentException("Address is null");
        }

        wc = address.wc;
        hashPart = address.hashPart;
        isTestOnly = address.isTestOnly;
        isUserFriendly = address.isUserFriendly;
        isBounceable = address.isBounceable;
        isUrlSafe = address.isUrlSafe;

    }

    public static Address of(String address) {
        return new Address(address);
    }

    public static Address of(byte tag, int wc, byte[] hashCrc) { // todo testing
//        byte[] tagArray = ByteBuffer.allocate(1).put(tag).array();
//        byte[] wcArray = ByteBuffer.allocate(1).put(wc).array();
//        byte[] tagWc = Utils.concatBytes(tagArray, wcArray);
//        Utils.concatBytes(tagWc, hashCrc);

        int tagI = tag & 0xff;
        boolean isTestOnly = false;
        boolean isBounceable;

        if ((tagI & test_flag) != 0) {
            isTestOnly = true;
            tagI = (byte) (tagI ^ test_flag);
        }
        if ((tagI != bounceable_tag) && (tagI != non_bounceable_tag)) {
            throw new Error("Unknown address tag");
        }

        byte workchain;
        if ((wc & 0xff) == 0xff) {
            workchain = -1;
        } else {
            workchain = (byte) wc;
        }

        isBounceable = tagI == bounceable_tag;

        Address addr = new Address();
        addr.wc = workchain;
        addr.hashPart = hashCrc;
        addr.isTestOnly = isTestOnly;
        addr.isBounceable = isBounceable;
        return addr;
    }

    public static Address of(Address address) {
        return new Address(address);
    }

    public String toDecimal() {
        return new BigInteger(Utils.bytesToHex(hashPart), 16).toString(10);
    }

    public String toHex() {
        return Utils.bytesToHex(hashPart);
    }

    /**
     * Save address to file in 36-byte format
     */
    public void saveToFile(String filename) throws IOException {
        byte[] wcBytes = ByteBuffer.allocate(4).putInt(wc).array();
        Files.write(Paths.get(filename), Utils.concatBytes(hashPart, wcBytes));
    }

    public String toString() {
        return toString(isUserFriendly, isUrlSafe, isBounceable, isTestOnly);
    }

    public String toString(boolean isUserFriendly) {
        return toString(isUserFriendly, isUrlSafe, isBounceable, isTestOnly);
    }

    public String toString(boolean isUserFriendly, boolean isUrlSafe) {
        return toString(isUserFriendly, isUrlSafe, isBounceable, isTestOnly);
    }

    public String toString(boolean isUserFriendly, boolean isUrlSafe, boolean isBounceable) {
        return toString(isUserFriendly, isUrlSafe, isBounceable, isTestOnly);
    }

    public String toString(boolean isUserFriendly,
                           boolean isUrlSafe,
                           boolean isBounceable,
                           boolean isTestOnly) {

        if (!isUserFriendly) {
            return wc + ":" + Utils.bytesToHex(hashPart);
        } else {
            int tag = isBounceable ? bounceable_tag : non_bounceable_tag;
            if (isTestOnly) {
                tag |= test_flag;
            }

            byte[] addr = new byte[34];
            byte[] addressWithChecksum = new byte[36];
            addr[0] = (byte) tag;
            addr[1] = (byte) wc;

            System.arraycopy(hashPart, 0, addr, 2, 32);

            byte[] crc16 = Utils.getCRC16ChecksumAsBytes(addr);
            System.arraycopy(addr, 0, addressWithChecksum, 0, 34);
            System.arraycopy(crc16, 0, addressWithChecksum, 34, 2);

            String addressBase64 = Utils.bytesToBase64(addressWithChecksum);

            if (isUrlSafe) {
                addressBase64 = Utils.bytesToBase64SafeUrl(addressWithChecksum);
            }
            return addressBase64;
        }
    }

    public static boolean isValid(String address) {
        try {
            Address.of(address);
            return true;
        } catch (Throwable e) {
            return false;
        }
    }

    public static Address parseFriendlyAddress(String addressString) {
        if (addressString.length() != 48) {
            throw new Error("User-friendly address should contain strictly 48 characters");
        }
        byte[] data = Utils.base64ToBytes(addressString);
        if (data.length != 36) { // 1byte tag + 1byte workchain + 32 bytes hash + 2 byte crc
            throw new Error("Unknown address type: byte length is not equal to 36");
        }

        byte[] addr = Arrays.copyOfRange(data, 0, 34);
        byte[] crc = Arrays.copyOfRange(data, 34, 36);

        byte[] calculatedCrc16 = Utils.getCRC16ChecksumAsBytes(addr);

        if (!(calculatedCrc16[0] == crc[0] && calculatedCrc16[1] == crc[1])) {
            throw new Error("Wrong crc16 hashsum");
        }
        int tag = addr[0] & 0xff;
        boolean isTestOnly = false;
        boolean isBounceable = false;

        if ((tag & test_flag) != 0) {
            isTestOnly = true;
            tag = (byte) (tag ^ test_flag);
        }
        if ((tag != bounceable_tag) && (tag != non_bounceable_tag)) {
            throw new Error("Unknown address tag");
        }

        isBounceable = tag == bounceable_tag;

        byte workchain;
        if ((addr[1] & 0xff) == 0xff) {
            workchain = -1;
        } else {
            workchain = addr[1];
        }
        if (workchain != 0 && workchain != -1) {
            throw new Error("Invalid address wc " + workchain);
        }

        byte[] hashPart = Arrays.copyOfRange(addr, 2, 34);

        Address parsedAddress = new Address();
        parsedAddress.wc = workchain;
        parsedAddress.hashPart = hashPart;
        parsedAddress.isTestOnly = isTestOnly;
        parsedAddress.isBounceable = isBounceable;

        return parsedAddress;
    }
}
