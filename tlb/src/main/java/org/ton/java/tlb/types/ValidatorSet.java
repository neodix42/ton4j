package org.ton.java.tlb.types;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.ton.java.cell.TonHashMap;

@Builder
@Getter
@Setter
@ToString
public class ValidatorSet {
    long magic;     // Magic            `tlb:"#11"`
    long uTimeSince;// uint32           `tlb:"## 32"`
    long uTimeUntil;// uint32           `tlb:"## 32"`
    int total;      // uint16           `tlb:"## 16"`
    int main;       // uint6            `tlb:"## 16"`
    TonHashMap list;//                  `tlb:"dict 16"`
}
