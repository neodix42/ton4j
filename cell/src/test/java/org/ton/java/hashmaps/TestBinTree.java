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
                .rootHash(BigInteger.valueOf(2))
                .fileHash(BigInteger.valueOf(2))
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
                        .coins(BigInteger.valueOf(2))
                        .build())
                .fundsCreated(CurrencyCollection.builder()
                        .coins(BigInteger.valueOf(2))
                        .build())
                .build();
    }

    private void printLog(Object o) {
        log.info("ShardDescr: {}", o);
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

        Cell c = BinTree.fromDeque(cells).toCell();

        List<ShardDescr> bt = BinTree.deserialize(CellSlice.beginParse(c)).toList();

        for (ShardDescr cc : bt) {
            printLog(cc);
        }

        assertThat(bt.size()).isEqualTo(5);
    }

    @Test
    public void testBinTree() {
        ShardDescr shardDescr1 = createShardDescr(1);
        ShardDescr shardDescr2 = createShardDescr(2);
        ShardDescr shardDescr3 = createShardDescr(3);
        ShardDescr shardDescr4 = createShardDescr(4);
        ShardDescr shardDescr5 = createShardDescr(5);
        ShardDescr shardDescr6 = createShardDescr(6);
        ShardDescr shardDescr7 = createShardDescr(7);
        ShardDescr shardDescr8 = createShardDescr(8);
        ShardDescr shardDescr9 = createShardDescr(9);
        ShardDescr shardDescr10 = createShardDescr(10);

        BinTree tree10 = new BinTree(shardDescr10);
        BinTree tree9 = new BinTree(shardDescr9, tree10, null);
        BinTree tree8 = new BinTree(shardDescr8);
        BinTree tree7 = new BinTree(shardDescr7, tree8, tree9);
        BinTree tree6 = new BinTree(shardDescr6);
        BinTree tree5 = new BinTree(shardDescr5, tree6, tree7);
        BinTree tree4 = new BinTree(shardDescr4, null, tree5);
        BinTree tree3 = new BinTree(shardDescr3);
        BinTree tree2 = new BinTree(shardDescr2, tree3, null);

        BinTree root = new BinTree(shardDescr1, tree2, tree4);

        Deque<ShardDescr> cells = new ArrayDeque<>();
        cells.add(shardDescr1);
        cells.add(shardDescr2);
        cells.add(shardDescr3);
        cells.add(shardDescr4);
        cells.add(shardDescr5);
        cells.add(shardDescr6);
        cells.add(shardDescr7);
        cells.add(shardDescr8);
        cells.add(shardDescr9);
        cells.add(shardDescr10);

        Cell cRootFromDeque = BinTree.fromDeque(cells).toCell();
        Cell cRoot = root.toCell();

        BinTree tree = BinTree.deserialize(CellSlice.beginParse(cRootFromDeque));
        BinTree rootFromCell = BinTree.deserialize(CellSlice.beginParse(cRoot));
        assertThat(tree).isNotNull();
        assertThat(root).isNotNull();

        List<ShardDescr> deserializedTree = tree.toList();
        List<ShardDescr> deserializedRoot = rootFromCell.toList();
        for (int i = 0; i < 10; i++) {
            assertThat(deserializedRoot.get(i).getSeqNo()).isEqualTo(deserializedTree.get(i).getSeqNo());
            printLog(deserializedTree.get(i));
        }

        assertThat(deserializedTree.size()).isEqualTo(10);
        assertThat(deserializedRoot.size()).isEqualTo(10);
    }

    @Test
    public void testBinTreeEmpty() {
        Deque<ShardDescr> cells = new ArrayDeque<>();
        BinTree tree = BinTree.fromDeque(cells);
        BinTree root = new BinTree();

        assertThat(tree).isNull();
        assertThat(root).isNotNull();
    }


    @Test
    public void testBinTreeOne() {
        ShardDescr shardDescr1 = createShardDescr(1);

        Deque<ShardDescr> cells = new ArrayDeque<>();
        cells.add(shardDescr1);

        BinTree root = new BinTree(shardDescr1);

        Cell cTree = BinTree.fromDeque(cells).toCell();
        Cell cRoot = root.toCell();

        List<ShardDescr> deserializedTree = BinTree.deserialize(CellSlice.beginParse(cTree)).toList();
        List<ShardDescr> deserializedRoot = BinTree.deserialize(CellSlice.beginParse(cRoot)).toList();

        for (ShardDescr cc : deserializedTree) {
            printLog(cc);
        }

        assertThat(deserializedTree.size()).isEqualTo(1);
        assertThat(deserializedRoot.size()).isEqualTo(1);
    }

    @Test
    public void testBinTreeTwo() {
        ShardDescr shardDescr1 = createShardDescr(1);
        ShardDescr shardDescr2 = createShardDescr(2);

        Deque<ShardDescr> cells = new ArrayDeque<>();
        cells.add(shardDescr1);
        cells.add(shardDescr2);

        BinTree root = new BinTree(shardDescr1, new BinTree(shardDescr1), null);

        Cell cTree = BinTree.fromDeque(cells).toCell();
        Cell cRoot = root.toCell();

        List<ShardDescr> deserializedTree = BinTree.deserialize(CellSlice.beginParse(cTree)).toList();
        List<ShardDescr> deserializedRoot = BinTree.deserialize(CellSlice.beginParse(cRoot)).toList();

        for (ShardDescr cc : deserializedTree) {
            printLog(cc);
        }

        assertThat(deserializedTree.size()).isEqualTo(2);
        assertThat(deserializedRoot.size()).isEqualTo(2);
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

        Cell c = BinTree.fromDeque(cells).toCell();

        printLog(c);

        List<ShardDescr> bt = BinTree.deserialize(CellSlice.beginParse(c)).toList();

        for (ShardDescr cc : bt) {
            printLog(cc);
        }

        assertThat(bt.size()).isEqualTo(3);
    }
}
