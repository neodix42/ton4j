package org.ton.ton4j.tlb;

import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellSlice;
import org.ton.ton4j.cell.TonHashMap;
import org.ton.ton4j.utils.Utils;

import java.io.Serializable;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static java.util.Objects.isNull;

@Builder
@Data
public class AccountHelper implements Serializable {
  boolean isActive;
  Account state;
  Cell data;
  Cell code;
  BigInteger lastTxLt;
  byte[] lastTxHash;

  public boolean hasGetMethod(String name) {
    if (isNull(code)) {
      return false;
    }
    long hash;
    switch (name) {
      case "recv_internal":
      case "main":
      case "recv_external":
      case "run_ticktock":
        {
          return false;
        }
      default:
        hash = methodNameHash(name);
    }

    CellSlice cs = CellSlice.beginParse(code);
    byte[] hdr = cs.loadBytes(56);

    if (!Utils.bytesToHex(hdr).toLowerCase().contains("ff00f4a413f4bc")) {
      return false;
    }

    CellSlice ref = CellSlice.beginParse(cs.loadRef());

    TonHashMap dict = ref.loadDict(19, k -> k.readUint(19), v -> v);
    for (Map.Entry<Object, Object> entry : dict.elements.entrySet()) {
      if (((BigInteger) entry.getKey()).longValue() == hash) {
        return true;
      }
    }
    return false;
  }

  public static long methodNameHash(String name) {
    return ((Utils.getCRC16ChecksumAsInt(name.getBytes(StandardCharsets.UTF_8)) & 0xffff)
        | 0x10000);
  }
}
