package org.ton.java.tlb.types;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.apache.commons.lang3.tuple.Pair;
import org.ton.java.cell.Cell;

import java.math.BigInteger;
import java.util.List;

import static java.util.Objects.isNull;

@Builder
@Getter
@Setter
@ToString
public class Block {
    long magic;               // `tlb:"#11ef55aa"`
    int globalId;            // `tlb:"## 32"`
    BlockHeader blockInfo;   // `tlb:"^"`
    Cell valueFlow;          // `tlb:"^"`
    StateUpdate stateUpdate; // `tlb:"^"`
    BlockExtra extra;      // `tlb:"^"`

    public List<BlockIdExt> getParentBlocks() {
        Pair<Long, Long> wcShard = convertShardIdentToShard(blockInfo.getBlockInfoPart().getShard());
        if (!blockInfo.getBlockInfoPart().isAfterMerge() && !blockInfo.getBlockInfoPart().isAfterSplit()) {
            return List.of(BlockIdExt.builder()
                    .workchain(wcShard.getLeft())
                    .seqno(blockInfo.getPrevRef().getPrev1().seqno)
                    .root_hash(blockInfo.getPrevRef().getPrev1().getRootHash())
                    .file_hash(blockInfo.getPrevRef().getPrev1().getFileHash())
                    .shard(wcShard.getRight())
                    .build());
        } else if (!blockInfo.getBlockInfoPart().isAfterMerge() && blockInfo.getBlockInfoPart().isAfterSplit()) {
            return List.of(BlockIdExt.builder()
                    .workchain(wcShard.getLeft())
                    .seqno(blockInfo.getPrevRef().getPrev1().seqno)
                    .root_hash(blockInfo.getPrevRef().getPrev1().getRootHash())
                    .file_hash(blockInfo.getPrevRef().getPrev1().getFileHash())
                    .shard(shardParent(wcShard.getRight()))
                    .build());
        }
        if (isNull(blockInfo.getPrevRef().getPrev1())) {
            throw new Error("must be 2 parent blocks after merge");
        }
        return List.of(
                BlockIdExt.builder()
                        .workchain(wcShard.getLeft())
                        .seqno(blockInfo.getPrevRef().getPrev1().seqno)
                        .root_hash(blockInfo.getPrevRef().getPrev1().getRootHash())
                        .file_hash(blockInfo.getPrevRef().getPrev1().getFileHash())
                        .shard(shardChild(wcShard.getRight(), true))
                        .build(),
                BlockIdExt.builder()
                        .workchain(wcShard.getLeft())
                        .seqno(blockInfo.getPrevRef().getPrev1().seqno)
                        .root_hash(blockInfo.getPrevRef().getPrev1().getRootHash())
                        .file_hash(blockInfo.getPrevRef().getPrev1().getFileHash())
                        .shard(shardChild(wcShard.getRight(), false))
                        .build());

    }

    private Pair<Long, Long> convertShardIdentToShard(ShardIdent shardIdent) {
        BigInteger shard = shardIdent.getShardPrefix();
        long pow2 = 1L << (64 - shardIdent.getPrefixBits());
        shard = shard.or(BigInteger.valueOf(pow2));
        return Pair.of(shardIdent.getWorkchain(), shard.longValue());
    }

    private Long shardChild(Long shard, boolean left) {
        Long x = lowerBit64(shard) >> 1;
        if (left) {
            return shard - x;
        } else {
            return shard + x;
        }
    }

    private Long shardParent(Long shard) {
        Long x = lowerBit64(shard);
        return (shard - x) | (x << 1);
    }

    private Long lowerBit64(Long x) {
        return x & bitsNegate64(x);
    }

    private Long bitsNegate64(Long x) {
        return ~x + 1;
    }
}
