package org.ton.ton4j.smartcontract.payments;

import lombok.Builder;
import lombok.Getter;
import org.ton.ton4j.cell.Cell;

@Builder
@Getter
public class Signature {
    Cell cell;
    byte[] signature;
}
