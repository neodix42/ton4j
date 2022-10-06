package org.ton.java.cell;

import java.util.List;

public class BocHeader {

    int has_idx;
    int hash_crc32;
    int has_cache_bits;
    int flags;
    int size_bytes;
    int off_bytes;
    int cells_num;
    int roots_num;
    int absent_num;
    int tot_cells_size;
    List<Integer> root_list;
    int[] index;
    byte[] cells_data;
}
