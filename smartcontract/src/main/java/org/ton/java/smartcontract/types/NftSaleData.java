package org.ton.java.smartcontract.types;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.ton.java.address.Address;

import java.math.BigInteger;

@Builder
@Getter
@Setter
@ToString
public class NftSaleData {
    Address marketplaceAddress;
    Address nftAddress;
    Address nftOwnerAddress;
    BigInteger fullPrice;
    BigInteger marketplaceFee;
    Address royaltyAddress;
    BigInteger royaltyAmount;
}
