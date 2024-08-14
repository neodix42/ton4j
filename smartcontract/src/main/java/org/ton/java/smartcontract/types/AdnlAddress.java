package org.ton.java.smartcontract.types;

import org.apache.commons.lang3.StringUtils;
import org.ton.java.utils.Utils;

import static java.util.Objects.isNull;

public class AdnlAddress {

    byte[] bytes;

    public static boolean isValid(String anyForm) {
        try {
            new AdnlAddress(anyForm);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public AdnlAddress(AdnlAddress anyForm) {
        if ((isNull(anyForm)) || (anyForm.bytes.length == 0)) {
            throw new Error("Invalid address");
        }
        bytes = anyForm.bytes.clone();
    }

    public AdnlAddress(String anyForm) {
        if ((isNull(anyForm)) || (anyForm.isEmpty())) {
            throw new Error("Invalid address");
        }
        if ((anyForm.length() != 64)) {
            throw new Error("Invalid adnl hex length");
        }

        bytes = Utils.hexToSignedBytes(anyForm);
    }

    public AdnlAddress(byte[] anyForm) {
        if ((anyForm.length == 0)) {
            throw new Error("Invalid address");
        }

        if ((anyForm.length != 32)) {
            throw new Error("Invalid adnl bytes length");
        }

        bytes = anyForm.clone();
    }

    public String toHex() {
        String hex = Utils.bytesToHex(bytes);
        return StringUtils.leftPad(hex, 64, '0'); // todo test
    }

    public byte[] getBytes() {
        return bytes.clone();
    }
}
