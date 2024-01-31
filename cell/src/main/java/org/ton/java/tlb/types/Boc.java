package org.ton.java.tlb.types;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.utils.Utils;

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
    boolean hasIdx;
    boolean hasCrc32c;
    boolean hasCacheBits;
    int flags;
    int size;
    int offBytes;
    int cells;
    int roots;
    int absent;
    int totalCellsSize;
    int rootList;
    int index;
    int[] cellData;

    public Cell toCell() {
        CellBuilder cell = CellBuilder.beginCell((cells * offBytes + offBytes + size * 4 + totalCellsSize + 10) * 8)
                .storeInt(0xb5ee9c72, 32)
                .storeBit(hasIdx)
                .storeBit(hasCrc32c)
                .storeBit(hasCacheBits)
                .storeUint(flags, 2)
                .storeUint(size, 3)
                .storeUint(offBytes, 8)
                .storeUint(cells, size * 8)
                .storeUint(roots, size * 8)
                .storeUint(absent, size * 8)
                .storeUint(totalCellsSize, offBytes * 8)
                .storeUint(rootList, size * 8)
                .storeUint(index, hasIdx ? cells * offBytes * 8 : 0)
                .storeBytes(cellData, totalCellsSize * 8);

        if (hasCrc32c) {
            int[] checksum = new int[4];
            int[] cellAsByteArray = cell.bits.toUnsignedByteArray();
            checksum = Utils.getCRC32ChecksumAsBytesReversed(cellAsByteArray);

            cell.storeBytes(checksum, hasCrc32c ? 32 : 0);
        }

        return cell.endCell();
    }
}

