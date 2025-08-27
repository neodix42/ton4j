package org.ton.ton4j.tlb;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigInteger;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellSlice;

@Slf4j
@RunWith(JUnit4.class)
public class TestBinTree {

  @Test
  public void testBinTreeDeque() {
    Deque<ShardDescr> cells = generateShardDescrDeque(5);
    Cell c = BinTree.fromDeque(cells).toCell();

    List<ShardDescr> bt = BinTree.deserialize(CellSlice.beginParse(c)).toList();

    for (ShardDescr cc : bt) {
      printLog(cc);
    }

    assertThat(bt.size()).isEqualTo(5);
  }

  @Ignore("needs to be reworked")
  @Test
  public void testBinTree() {
    BinTree node = generateBinTree(10);
    Deque<ShardDescr> cells = generateShardDescrDeque(10);

    Cell cTree = BinTree.fromDeque(cells).toCell();
    Cell cRoot = node.toCell();

    BinTree tree = BinTree.deserialize(CellSlice.beginParse(cTree));
    BinTree root = BinTree.deserialize(CellSlice.beginParse(cRoot));
    assertThat(tree).isNotNull();
    assertThat(root).isNotNull();

    List<ShardDescr> deserializedTree = tree.toList();
    List<ShardDescr> deserializedRoot = root.toList();
    deserializedRoot.sort((a, b) -> Math.toIntExact(a.getSeqNo() - b.getSeqNo()));

    for (int i = 0; i < 10; i++) {
      assertThat(deserializedRoot.get(i).getSeqNo()).isEqualTo(deserializedTree.get(i).getSeqNo());
      printLog(deserializedRoot.get(i));
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
    Deque<ShardDescr> cells = generateShardDescrDeque(1);
    BinTree root = generateBinTree(1);

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
    Deque<ShardDescr> cells = generateShardDescrDeque(2);
    BinTree root = generateBinTree(2);

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

  @Ignore("needs to be reworked")
  @Test
  public void testBinTreeThree() {
    Deque<ShardDescr> cells = generateShardDescrDeque(3);
    BinTree root = generateBinTree(3);

    Cell cTree = BinTree.fromDeque(cells).toCell();
    Cell cRoot = root.toCell();

    printLog(cTree);
    printLog(cRoot);

    List<ShardDescr> deserializedTree = BinTree.deserialize(CellSlice.beginParse(cTree)).toList();
    List<ShardDescr> deserializedRoot = BinTree.deserialize(CellSlice.beginParse(cRoot)).toList();

    for (ShardDescr cc : deserializedTree) {
      printLog(cc);
    }

    assertThat(deserializedTree.size()).isEqualTo(3);
    assertThat(deserializedRoot.size()).isEqualTo(3);
  }

  @Ignore("needs to be reworked")
  @Test
  public void testBinTreeLarge() {
    BinTree root = generateBinTree(100);

    Cell cRoot = root.toCell();

    printLog(cRoot);

    List<ShardDescr> deserializedRoot = BinTree.deserialize(CellSlice.beginParse(cRoot)).toList();

    for (ShardDescr cc : deserializedRoot) {
      printLog(cc);
    }

    assertThat(deserializedRoot.size()).isEqualTo(100);
  }

  private Deque<ShardDescr> generateShardDescrDeque(int sz) {
    Deque<ShardDescr> cells = new ArrayDeque<>();

    for (int i = 0; i < sz; i++) {
      cells.add(createShardDescr(i + 1));
    }

    return cells;
  }

  public BinTree generateBinTree(int size) {
    ShardDescr[] shardDescrArr = new ShardDescr[size];

    for (int i = 0; i < size; i++) {
      shardDescrArr[i] = createShardDescr(i + 1);
    }

    return buildTree(shardDescrArr, 0, size - 1);
  }

  private BinTree buildTree(ShardDescr[] shardDescrArr, int start, int end) {
    if (start > end) {
      return null;
    }

    int mid = (start + end) / 2;
    ShardDescr value = shardDescrArr[mid];

    BinTree left = buildTree(shardDescrArr, start, mid - 1);
    BinTree right = buildTree(shardDescrArr, mid + 1, end);

    return new BinTree(value, left, right);
  }

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
        .splitMergeAt(FutureSplitMerge.builder().flag(0b10).splitUTime(42).interval(43).build())
        .feesCollected(CurrencyCollection.builder().coins(BigInteger.valueOf(2)).build())
        .fundsCreated(CurrencyCollection.builder().coins(BigInteger.valueOf(2)).build())
        .build();
  }

  private void printLog(Object o) {
    log.info("ShardDescr: {}", o);
  }
}
