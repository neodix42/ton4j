package org.ton.java.smartcontract.types;

import lombok.Builder;
import lombok.Data;
import org.ton.java.cell.Cell;

@Builder
@Data
public class HighloadV3BatchItem {
    int mode;
    Cell message;
}
