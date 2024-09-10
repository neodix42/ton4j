package org.ton.java.tlb.types;

import java.io.Serializable;


public enum ExtendedActionType implements Serializable {
    ADD_EXTENSION(2), REMOVE_EXTENSION(3), SET_SIGNATURE_AUTH_FLAG(4);
    private final int value;

    ExtendedActionType(final int newValue) {
        value = newValue;
    }

    public int getValue() {
        return value;
    }

    public int getIndex() {
        return ordinal();
    }

    public static ExtendedActionType getExtensionType(int value) {
        if (value == 2) {
            return ADD_EXTENSION;
        } else if (value == 3) {
            return REMOVE_EXTENSION;
        } else if (value == 4) {
            return SET_SIGNATURE_AUTH_FLAG;
        } else {
            throw new Error("wrong ExtendedActionType");
        }
    }
}