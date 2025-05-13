package org.ton.ton4j.smartcontract.types;

import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.address.Address;

import java.math.BigInteger;

@Builder
@Data
public class NftSaleData {
    Address marketplaceAddress;
    Address nftAddress;
    Address nftOwnerAddress;
    BigInteger fullPrice;
    BigInteger marketplaceFee;
    Address royaltyAddress;
    BigInteger royaltyAmount;
}
