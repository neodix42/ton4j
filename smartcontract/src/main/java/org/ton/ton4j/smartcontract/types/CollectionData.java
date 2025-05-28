package org.ton.ton4j.smartcontract.types;

import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.address.Address;
import org.ton.ton4j.cell.Cell;

@Builder
@Data
public class CollectionData {
    long itemsCount;
    long nextItemIndex;
    Address ownerAddress;
    Cell collectionContentCell;
    String collectionContentUri;
}
