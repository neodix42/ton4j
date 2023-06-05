package org.ton.java.tlb.types;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Builder
@Getter
@Setter
@ToString
public class HashUpdate {
    int magic; //  `tlb:"#72"`
    int[] oldHash; // `tlb:"bits 256"`
    int[] newHash; // `tlb:"bits 256"`
}
