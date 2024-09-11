package org.ton.java.smartcontract.types;

import lombok.Builder;
import lombok.Data;
import org.ton.java.address.Address;
import org.ton.java.cell.Cell;

@Builder
@Data
public class CollectionData {
    long itemsCount;
    long nextItemIndex;
    Address ownerAddress;
    Cell collectionContentCell;
    String collectionContentUri;
}
