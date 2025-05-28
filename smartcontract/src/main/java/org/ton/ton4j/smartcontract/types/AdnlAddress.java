package org.ton.ton4j.smartcontract.types;

import org.ton.ton4j.utils.Utils;

import static java.util.Objects.isNull;

public class AdnlAddress {

    byte[] bytes;

    public static AdnlAddress of(String anyForm) {
        return new AdnlAddress(anyForm);
    }

    public static AdnlAddress of(byte[] anyForm) {
        return new AdnlAddress(anyForm);
    }

    public static AdnlAddress of(AdnlAddress anyForm) {
        return new AdnlAddress(anyForm);
    }

    private AdnlAddress(AdnlAddress anyForm) {
        isValid(anyForm);
        bytes = anyForm.bytes.clone();
    }

    private AdnlAddress(String anyForm) {
        isValid(anyForm);
        bytes = Utils.hexToSignedBytes(anyForm);
    }

    private AdnlAddress(byte[] anyForm) {
        isValid(anyForm);
        bytes = anyForm.clone();
    }

    public String toHex() {
        String hex = Utils.bytesToHex(bytes);
        return new String(Utils.leftPadBytes(hex.getBytes(), 64, '0'));
    }

    public byte[] getClonedBytes() {
        return bytes.clone();
    }

    public static void isValid(Object anyForm) {
        if ((isNull(anyForm))) {
            throw new Error("Invalid address");
        }
        else if (anyForm instanceof AdnlAddress) {
            byte[] address = ((AdnlAddress) anyForm).bytes;
            if (address.length == 0) {
                throw new Error("Invalid adnl bytes length");
            }
        }
        else if (anyForm instanceof String) {
            if (((String) anyForm).isEmpty()) {
                throw new Error("Invalid address");
            }
            if ((((String) anyForm).length() != 64)) {
                throw new Error("Invalid adnl hex length");
            }
        }
        else if (anyForm instanceof byte[]) {
            byte[] address = (byte[]) anyForm;
            if ((address.length == 0)) {
                throw new Error("Invalid address");
            }
            if ((address.length != 32)) {
                throw new Error("Invalid adnl bytes length");
            }
        }
        else {
            throw new Error("Invalid object type");
        }
    }
}
