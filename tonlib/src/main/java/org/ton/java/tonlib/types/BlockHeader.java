package org.ton.java.tonlib.types;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.ton.java.tonlib.base.TypedAsyncObject;

import java.util.List;

@SuperBuilder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class BlockHeader extends TypedAsyncObject {
    private BlockId id;
    private long global_id;
    private long version;
    private long flags;
    private boolean after_merge;
    private boolean after_split;
    private boolean before_split;
    private boolean want_merge;
    private boolean want_split;
    private long validator_list_hash_short;
    private long catchain_seqno;
    private long min_ref_mc_seqno;
    private boolean is_key_block;
    private long prev_key_block_seqno;
    private String start_lt;
    private String end_lt;
    private long gen_utime;
    private List<BlockIdExt> prev_blocks;

    @Override
    public String getTypeObjectName() {
        return "blocks.header";
    }
}
