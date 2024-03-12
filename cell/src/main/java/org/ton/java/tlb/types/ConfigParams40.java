package org.ton.java.tlb.types;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.CellSlice;

import java.math.BigInteger;

@Builder
@Getter
@Setter
@ToString
/**
 * misbehaviour_punishment_config_v1#01
 *   default_flat_fine:Grams
 *   default_proportional_fine:uint32
 *   severity_flat_mult:uint16
 *   severity_proportional_mult:uint16
 *   unpunishable_interval:uint16
 *   long_interval:uint16
 *   long_flat_mult:uint16
 *   long_proportional_mult:uint16
 *   medium_interval:uint16
 *   medium_flat_mult:uint16
 *   medium_proportional_mult:uint16
 *    = MisbehaviourPunishmentConfig;
 * _ MisbehaviourPunishmentConfig = ConfigParam 40;
 */
public class ConfigParams40 {
    long magic;
    BigInteger defaultFlatFine;
    long defaultProportionalFine;
    int severityFlatMult;
    int severityProportionalMult;
    int unpunishableInterval;
    int longInterval;
    int longFlatMult;
    int longProportionalMult;
    int mediumInterval;
    int mediumFlatMult;
    int mediumProportionalMult;

    public Cell toCell() {
        return CellBuilder.beginCell()
                .storeUint(0x01, 8)
                .storeCoins(defaultFlatFine)
                .storeUint(defaultProportionalFine, 32)
                .storeUint(severityFlatMult, 16)
                .storeUint(severityProportionalMult, 16)
                .storeUint(unpunishableInterval, 16)
                .storeUint(longInterval, 16)
                .storeUint(longFlatMult, 16)
                .storeUint(longProportionalMult, 16)
                .storeUint(mediumInterval, 16)
                .storeUint(mediumFlatMult, 16)
                .storeUint(mediumProportionalMult, 16)
                .endCell();
    }

    public static ConfigParams40 deserialize(CellSlice cs) {
        return ConfigParams40.builder()
                .magic(cs.loadUint(8).longValue())
                .defaultFlatFine(cs.loadCoins())
                .defaultProportionalFine(cs.loadUint(32).longValue())
                .severityFlatMult(cs.loadUint(16).intValue())
                .severityProportionalMult(cs.loadUint(16).intValue())
                .unpunishableInterval(cs.loadUint(16).intValue())
                .longInterval(cs.loadUint(16).intValue())
                .longFlatMult(cs.loadUint(16).intValue())
                .longProportionalMult(cs.loadUint(16).intValue())
                .mediumInterval(cs.loadUint(16).intValue())
                .mediumFlatMult(cs.loadUint(16).intValue())
                .mediumProportionalMult(cs.loadUint(16).intValue())
                .build();
    }
}
