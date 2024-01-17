package org.ton.java.tlb.types;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;

import java.math.BigInteger;

@Builder
@Getter
@Setter
@ToString
/**
 * transaction$0111
 *   account_addr:bits256
 *   lt:uint64
 *   prev_trans_hash:bits256
 *   prev_trans_lt:uint64
 *   now:uint32
 *   outmsg_cnt:uint15
 *   orig_status:AccountStatus
 *   end_status:AccountStatus
 *   ^[
 *     in_msg:(Maybe ^(Message Any))
 *     out_msgs:(HashmapE 15 ^(Message Any))
 *     ]
 *   total_fees:CurrencyCollection
 *   state_update:^(HASH_UPDATE Account)
 *   description:^TransactionDescr = Transaction;
 */
public class Transaction {
    int magic;
    BigInteger accountAddr;
    BigInteger lt;
    BigInteger prevTxHash;
    BigInteger prevTxLt;
    long now;
    long outMsgCount;
    AccountStates origStatus;
    AccountStates endStatus;
    TransactionIO inOut;
    CurrencyCollection totalFees;
    HashUpdate stateUpdate;
    TransactionDescription description;

    // not in scheme, but might be filled based on request data for flexibility
    byte[] hash;

    private String getMagic() {
        return Long.toBinaryString(magic);
    }

    private String getAccountAddr() {
        return accountAddr.toString(16);
    }

    private String getPrevTxHash() {
        return prevTxHash.toString(16);
    }

    public void dump() {
        //todo
    }

    public Cell toCell() {
        CellBuilder c = CellBuilder.beginCell();
        c.storeUint(0b0111, 4);
        c.storeUint(accountAddr, 256);
        c.storeUint(lt, 64);
        c.storeUint(prevTxHash, 256);
        c.storeUint(prevTxLt, 64);
        c.storeUint(now, 32);
        c.storeUint(outMsgCount, 15);
        c.storeCell(serializeAccountState(origStatus));
        c.storeCell(serializeAccountState(endStatus));
        c.storeCell(totalFees.toCell());

        c.storeRef(inOut.toCell());
        c.storeRef(stateUpdate.toCell());
        c.storeRef(description.toCell());


        return c.endCell();
    }

    public static Cell serializeAccountState(AccountStates state) {
        switch (state) {
            case UNINIT -> {
                return CellBuilder.beginCell().storeUint(0, 2).endCell();
            }
            case FROZEN -> {
                return CellBuilder.beginCell().storeUint(1, 2).endCell();
            }
            case ACTIVE -> {
                return CellBuilder.beginCell().storeUint(2, 2).endCell();
            }
            case NON_EXIST -> {
                return CellBuilder.beginCell().storeUint(3, 2).endCell();
            }
        }
        return null;
    }

    public static AccountStates deserializeAccountState(byte state) {
        switch (state) {
            case 0 -> {
                return AccountStates.UNINIT;
            }
            case 1 -> {
                return AccountStates.FROZEN;
            }
            case 2 -> {
                return AccountStates.ACTIVE;
            }
            case 3 -> {
                return AccountStates.NON_EXIST;
            }
        }
        return null;
    }
}
