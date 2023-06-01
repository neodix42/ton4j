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
    byte[] oldHash; // `tlb:"bits 256"`
    byte[] newHash; // `tlb:"bits 256"`
}
