package org.ton.java.tlb;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.ton.java.cell.Cell;
import org.ton.java.tlb.types.StorageInfo;
import org.ton.java.tlb.types.StorageUsed;

import java.math.BigInteger;

@Slf4j
@RunWith(JUnit4.class)
public class TestTlbSerialization {
    @Test
    public void testLoadShardStateFromCell() {

        StorageUsed storageUsed = StorageUsed.builder()
                .bitsUsed(BigInteger.valueOf(5))
                .cellsUsed(BigInteger.valueOf(3))
                .publicCellsUsed(BigInteger.valueOf(3))
                .build();

        StorageInfo storageInfo = StorageInfo.builder()
                .storageUsed(storageUsed)
                .lastPaid(1709674914)
                .duePayment(BigInteger.valueOf(12))
                .build();

        Cell serializedStorageInfo = storageInfo.toCell();

        log.info("serializedStorageInfo Cell {}", serializedStorageInfo.print());
        log.info("serializedStorageInfo Cell {}", serializedStorageInfo.toHex());
        log.info("serializedStorageInfo Struct {}", storageInfo);
    }
}
