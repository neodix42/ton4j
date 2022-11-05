package org.ton.java.smartcontract.types;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import org.ton.java.address.Address;
import org.ton.java.cell.Cell;

@Builder
@Getter
@ToString
public class CollectionInfo {
    String collectionContentUri;
    Cell collectionContent;
    Address ownerAddress;
    long nextItemIndex;
}
