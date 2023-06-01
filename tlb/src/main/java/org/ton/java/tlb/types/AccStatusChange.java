package org.ton.java.tlb.types;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;

@Builder
@Getter
@Setter
@ToString
public class AccStatusChange {
    String type;

    public Cell toCell() {
        switch (type) {
            case "UNCHANGED" -> {
                return CellBuilder.beginCell().storeUint(0b0, 1).endCell();
            }
            case "FROZEN" -> {
                return CellBuilder.beginCell().storeUint(0b01, 2).endCell();
            }
            case "DELETED" -> {
                return CellBuilder.beginCell().storeUint(0b10, 2).endCell();
            }
        }
        throw new Error("unknown account status change");
    }
}
