package org.ton.java.smartcontract.payments;

import lombok.Builder;
import lombok.Getter;
import org.ton.java.cell.Cell;

@Builder
@Getter
public class Signature {
    Cell cell;
    byte[] signature;
}
