package org.ton.java.smartcontract.types;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.ton.java.address.Address;
import org.ton.java.cell.Cell;

import java.math.BigInteger;

@Builder
@Getter
@Setter
public class DnsData {
    //Promise<{isInitialized: boolean, index: BN, collectionAddress: Address|null, ownerAddress: Address|null, contentCell: Cell}>}
    boolean isInitialized;
    BigInteger index;
    Address collectionAddress;
    Address ownerAddress;
    Cell contentCell;
}
