package org.ton.java.smartcontract.types;

import org.ton.java.cell.Cell;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Builder
@Getter
@Setter
@ToString
public class HighloadV3BatchItem {
    byte mode;
    Cell message;
}
