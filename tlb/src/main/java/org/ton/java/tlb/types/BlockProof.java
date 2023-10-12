package org.ton.java.tlb.types;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Builder
@Getter
@Setter
@ToString
/**
 * block_signatures_pure#_
 * sig_count:uint32
 * sig_weight:uint64
 * signatures:(HashmapE 16 CryptoSignaturePair) = BlockSignaturesPure;
 *
 * block_signatures#11
 * validator_info:ValidatorBaseInfo
 * pure_signatures:BlockSignaturesPure = BlockSignatures;
 *
 * block_proof#c3
 * proof_for:BlockIdExt
 * root:^Cell
 * signatures:(Maybe ^BlockSignatures) = BlockProof;
 */
public class BlockProof {
    int magic;
    //ValidatorBaseInfo validatorInfo;
    //BlockSignatures blockSignatures;
    // todo required for proof-block parsing
}
