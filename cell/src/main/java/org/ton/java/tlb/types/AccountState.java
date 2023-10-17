package org.ton.java.tlb.types;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.ton.java.address.Address;

@Builder
@Getter
@Setter
@ToString
public class AccountState {
    boolean isValid;
    Address address;
    StorageInfo storageInfo;
    AccountStorage accountStorage;
}
