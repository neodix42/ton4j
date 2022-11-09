package org.ton.java.smartcontract.types;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.ton.java.address.Address;
import org.ton.java.cell.Cell;

@Builder
@Getter
@Setter
@ToString
public class CollectionData {
    long itemsCount;
    long nextItemIndex;
    Address ownerAddress;
    Cell collectionContentCell;
    String collectionContentUri;
}
