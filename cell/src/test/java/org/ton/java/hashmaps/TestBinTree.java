package org.ton.java.hashmaps;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.ton.java.cell.BinTree;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellSlice;
import org.ton.java.tlb.types.CurrencyCollection;
import org.ton.java.tlb.types.FutureSplitMerge;
import org.ton.java.tlb.types.ShardDescr;

import java.math.BigInteger;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@RunWith(JUnit4.class)
public class TestBinTree {

    private ShardDescr createShardDescr(int seqno) {
        return ShardDescr.builder()
                .magic(0xb)
                .seqNo(seqno)
                .regMcSeqno(1)
                .startLt(BigInteger.TEN)
                .endLt(BigInteger.TEN)
                .rootHash(BigInteger.TWO)
                .fileHash(BigInteger.TWO)
                .beforeSplit(true)
                .beforeMerge(true)
                .wantSplit(false)
                .wantMerge(false)
                .nXCCUpdated(true)
                .flags(1)
                .nextCatchainSeqNo(11)
                .nextValidatorShard(BigInteger.TEN)
                .minRefMcSeqNo(22)
                .genUTime(12345)
                .splitMergeAt(FutureSplitMerge.builder()
                        .flag(0b10)
                        .splitUTime(42)
                        .interval(43)
                        .build())
                .feesCollected(CurrencyCollection.builder()
                        .coins(BigInteger.TWO)
                        .build())
                .fundsCreated(CurrencyCollection.builder()
                        .coins(BigInteger.TWO)
                        .build())
                .build();
    }

    @Test
    public void testBinTreeDeque() {
        ShardDescr shardDescr1 = createShardDescr(1);
        ShardDescr shardDescr2 = createShardDescr(2);
        ShardDescr shardDescr3 = createShardDescr(3);
        ShardDescr shardDescr4 = createShardDescr(4);
        ShardDescr shardDescr5 = createShardDescr(5);


        Deque<ShardDescr> cells = new ArrayDeque<>();
        cells.add(shardDescr1);
        cells.add(shardDescr2);
        cells.add(shardDescr3);
        cells.add(shardDescr4);
        cells.add(shardDescr5);


        Cell c = new BinTree(cells).toCell();

        List<ShardDescr> bt = BinTree.deserialize(CellSlice.beginParse(c));

        for (ShardDescr cc : bt) {
            log.info("ShardDescr: {}", cc);
        }

        assertThat(bt.size()).isEqualTo(5);
    }

    @Test
    public void testBinTreeEmpty() {
        Deque<ShardDescr> cells = new ArrayDeque<>();
        Cell c = new BinTree(cells).toCell();
    }


    @Test
    public void testBinTreeOne() {
        ShardDescr shardDescr1 = createShardDescr(1);

        Deque<ShardDescr> cells = new ArrayDeque<>();
        cells.add(shardDescr1);

        Cell c = new BinTree(cells).toCell();

        List<ShardDescr> bt = BinTree.deserialize(CellSlice.beginParse(c));

        for (ShardDescr cc : bt) {
            log.info("ShardDescr: {}", cc);
        }

        assertThat(bt.size()).isEqualTo(1);
    }

    @Test
    public void testBinTreeTwo() {
        ShardDescr shardDescr1 = createShardDescr(1);
        ShardDescr shardDescr2 = createShardDescr(2);

        Deque<ShardDescr> cells = new ArrayDeque<>();
        cells.add(shardDescr1);
        cells.add(shardDescr2);

        Cell c = new BinTree(cells).toCell();

        List<ShardDescr> bt = BinTree.deserialize(CellSlice.beginParse(c));

        for (ShardDescr cc : bt) {
            log.info("ShardDescr: {}", cc);
        }

        assertThat(bt.size()).isEqualTo(2);
    }

    @Test
    public void testBinTreeThree() {
        ShardDescr shardDescr1 = createShardDescr(1);
        ShardDescr shardDescr2 = createShardDescr(2);
        ShardDescr shardDescr3 = createShardDescr(3);

        Deque<ShardDescr> cells = new ArrayDeque<>();
        cells.add(shardDescr1);
        cells.add(shardDescr2);
        cells.add(shardDescr3);

        Cell c = new BinTree(cells).toCell();

        log.info("ShardDescr: {}", c);

        List<ShardDescr> bt = BinTree.deserialize(CellSlice.beginParse(c));

        for (ShardDescr cc : bt) {
            log.info("ShardDescr: {}", cc);
        }

        assertThat(bt.size()).isEqualTo(3);
    }
}
