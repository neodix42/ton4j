package org.ton.java.tlb.types;

import java.util.ArrayList;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.CellSlice;

/**
 *
 *
 * <pre>
 * block#11ef55aa
 *   global_id:int32
 *   info:^BlockInfo
 *   value_flow:^ValueFlow
 *   state_update:^(MERKLE_UPDATE ShardState)
 *   extra:^BlockExtra = Block;
 *   </pre>
 */
@Builder
@Data
@Slf4j
public class Block {
  long magic;
  int globalId;
  BlockInfo blockInfo;
  ValueFlow valueFlow;
  MerkleUpdate stateUpdate;
  BlockExtra extra;

  public Cell toCell() {
    return CellBuilder.beginCell()
        .storeUint(0x11ef55aa, 32)
        .storeInt(globalId, 32)
        .storeRef(blockInfo.toCell())
        .storeRef(valueFlow.toCell())
        .storeRef(stateUpdate.toCell())
        .storeRef(extra.toCell())
        .endCell();
  }

  public static Block deserialize(CellSlice cs) {

    long magic = cs.loadUint(32).longValue();
    assert (magic == 0x11ef55aaL)
        : "Block: magic not equal to 0x11ef55aa, found 0x" + Long.toHexString(magic);

    Block block =
        Block.builder()
            .magic(0x11ef55aaL)
            .globalId(cs.loadInt(32).intValue())
            .blockInfo(BlockInfo.deserialize(CellSlice.beginParse(cs.loadRef())))
            .build();

    block.setValueFlow(ValueFlow.deserialize(CellSlice.beginParse(cs.loadRef())));

    MerkleUpdate merkleUpdate = MerkleUpdate.deserialize(CellSlice.beginParse(cs.loadRef()));
    block.setStateUpdate(merkleUpdate);
    block.setExtra(BlockExtra.deserialize(CellSlice.beginParse(cs.loadRef())));

    return block;
  }

  public List<Transaction> getAllTransactions() {
    List<Transaction> result = new ArrayList<>();
    Block block = this;

    List<OutMsg> outMsgs = block.getExtra().getOutMsgDesc().getOutMessages();

    for (OutMsg outMsg : outMsgs) {
      if (outMsg instanceof OutMsgExt) {
        result.add(((OutMsgExt) outMsg).getTransaction());
      }
      if (outMsg instanceof OutMsgImm) {
        result.add(((OutMsgImm) outMsg).getTransaction());
      }
      if (outMsg instanceof OutMsgNew) {
        result.add(((OutMsgNew) outMsg).getTransaction());
      }
      if (outMsg instanceof OutMsgNew) {
        result.add(((OutMsgNew) outMsg).getTransaction());
      }
    }

    List<InMsg> inMsgs = block.getExtra().getInMsgDesc().getInMessages();
    for (InMsg inMsg : inMsgs) {
      if (inMsg instanceof InMsgImportExt) {
        result.add(((InMsgImportExt) inMsg).getTransaction());
      }
      if (inMsg instanceof InMsgImportIhr) {
        result.add(((InMsgImportIhr) inMsg).getTransaction());
      }
      if (inMsg instanceof InMsgImportImm) {
        result.add(((InMsgImportImm) inMsg).getTransaction());
      }
      if (inMsg instanceof InMsgImportFin) {
        result.add(((InMsgImportFin) inMsg).getTransaction());
      }
    }
    return result;
  }

  public void printAllTransactions() {
    List<Transaction> txs = getAllTransactions();
    if (txs.isEmpty()) {
      log.info("No transactions");
      return;
    }
    Transaction.printTxHeader();
    for (Transaction tx : txs) {
      tx.printTransactionFees();
    }
    Transaction.printTxFooter();
  }

  public List<MessageFees> getAllMessageFees() {
    List<Transaction> txs = getAllTransactions();
    List<MessageFees> msgFees = new ArrayList<>();
    for (Transaction tx : txs) {
      msgFees.addAll(tx.getAllMessageFees());
    }

    return msgFees;
  }

  public void printAllMessages() {
    List<MessageFees> msgFees = getAllMessageFees();
    if (msgFees.isEmpty()) {
      log.info("No messages");
      return;
    }
    MessageFees.printMessageFeesHeader();
    for (MessageFees msgFee : msgFees) {
      msgFee.printMessageFees();
    }
    MessageFees.printMessageFeesFooter();
  }
}
