package org.ton.ton4j.smartcontract.types;

import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.cell.Cell;

@Builder
@Data
public class HighloadV3BatchItem {
    int mode;
    Cell message;
}
