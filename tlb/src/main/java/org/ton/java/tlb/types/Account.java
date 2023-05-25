package org.ton.java.tlb.types;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.ton.java.cell.Cell;
import org.ton.java.utils.Utils;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;

import static java.util.Objects.isNull;

@Builder
@Getter
@Setter
public class Account {
    boolean isActive;
    AccountState state;
    Cell data;
    Cell code;
    BigInteger lastTxLt;
    byte[] lastTxHash;

    public boolean hasGetMethod(String name) {
        if (isNull(code)) {
            return false;
        }
        return false; // todo finish account.go
    }

    public static long methodNameHash(String name) {
        // https://github.com/ton-blockchain/ton/blob/24dc184a2ea67f9c47042b4104bbb4d82289fac1/crypto/smc-envelope/SmartContract.h#L75
        // todo review
        return ((Utils.getCRC16ChecksumAsInt(name.getBytes(StandardCharsets.UTF_8)) & 0xffff) | 0x10000);
    }
}
