package org.ton.java.tlb.types;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.CellSlice;
import org.ton.java.utils.Utils;

import java.math.BigInteger;
import java.util.List;

/**
 * serialized_boc#b5ee9c72
 * has_idx:(## 1)
 * has_crc32c:(## 1)
 * has_cache_bits:(## 1)
 * flags:(## 2) { flags = 0 }
 * size:(## 3) { size <= 4 }
 * off_bytes:(## 8) { off_bytes <= 8 }
 * cells:(##(size * 8))
 * roots:(##(size * 8)) { roots >= 1 }
 * absent:(##(size * 8)) { roots + absent <= cells }
 * tot_cells_size:(##(off_bytes * 8))
 * root_list:(roots * ##(size * 8))
 * index:has_idx?(cells * ##(off_bytes * 8))
 * cell_data:(tot_cells_size * [ uint8 ])
 * crc32c:has_crc32c?uint32
 * = BagOfCells;
 */
@Builder
@Getter
@Setter
@ToString
public class Boc {
    long magic;
    boolean hasIdx;
    boolean hasCrc32c;
    boolean hasCacheBits;
    boolean hasTopHash;
    boolean hasIntHashes;
    int size;
    int offBytes;
    int cells;
    int roots;
    int absent;
    int totalCellsSize;
    List<BigInteger> rootList;
    List<BigInteger> index;
    byte[] cellData;
    long crc32c;

    public Cell toCell() {
        CellBuilder cell = CellBuilder.beginCell((cells * offBytes + offBytes + size * 4 + totalCellsSize + 10) * 8)
                .storeInt(0xb5ee9c72, 32)
                .storeBit(hasIdx)
                .storeBit(hasCrc32c)
                .storeBit(hasCacheBits)
                .storeBit(hasTopHash)
                .storeBit(hasIntHashes)
                .storeUint(size, 3)
                .storeUint(offBytes, 8)
                .storeUint(cells, size * 8)
                .storeUint(roots, size * 8)
                .storeUint(absent, size * 8)
                .storeUint(totalCellsSize, offBytes * 8)
                .storeList(rootList, size * 8)
                .storeList(index, hasIdx ? cells * offBytes * 8 : 0)
                .storeBytes(cellData, totalCellsSize * 8);

        if (hasCrc32c) {
            byte[] cellAsByteArray = cell.toSignedByteArray();
            byte[] checksum = Utils.getCRC32ChecksumAsBytesReversed(cellAsByteArray);

            cell.storeBytes(checksum, hasCrc32c ? 32 : 0);
        }

        return cell.endCell();
    }

    public static Boc deserialize(CellSlice cs) {
        Boc boc = Boc.builder().magic(cs.loadUint(32).longValue()).build();
        boc.setHasIdx(cs.loadBit());
        boc.setHasCrc32c(cs.loadBit());
        boc.setHasCacheBits(cs.loadBit());
        boc.setHasTopHash(cs.loadBit());
        boc.setHasIntHashes(cs.loadBit());
        boc.setSize(cs.loadUint(3).intValue());
        boc.setOffBytes(cs.loadUint(8).intValue());
        boc.setCells(cs.loadUint(boc.getSize() * 8).intValue());
        boc.setRoots(cs.loadUint(boc.getSize() * 8).intValue());
        boc.setAbsent(cs.loadUint(boc.getSize() * 8).intValue());
        boc.setTotalCellsSize(cs.loadUint(boc.getOffBytes() * 8).intValue());
        boc.setRootList(cs.loadList(boc.getRoots(), boc.getSize() * 8));
        boc.setIndex(boc.isHasIdx() ? cs.loadList(boc.getCells(), boc.getOffBytes() * 8) : null);
        boc.setCellData(cs.loadBytes(boc.getTotalCellsSize() * 8));
        boc.setCrc32c(boc.isHasCrc32c() ? cs.loadUint(32).longValue() : 0);
        return boc;
    }
}

