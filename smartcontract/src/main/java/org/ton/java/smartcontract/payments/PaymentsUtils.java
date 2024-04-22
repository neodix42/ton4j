package org.ton.java.smartcontract.payments;

import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;

import java.math.BigInteger;

public class PaymentsUtils {
    public static final long tag_init = 0x696e6974;
    public static final long tag_cooperative_close = 0x436c6f73;
    public static final long tag_cooperative_commit = 0x43436d74;
    public static final long tag_start_uncooperative_close = 0x556e436c;
    public static final long tag_challenge_state = 0x43686751;
    public static final long tag_settle_conditionals = 0x436c436e;
    public static final long tag_state = 0x43685374;

    public static final long op_top_up_balance = 0x67c7d281;// 0x67c7d281  // crc32("top_up_balance add_A:Coins add_B:Coins = InternalMsgBody");
    public static final long op_init_channel = 235282626; // crc32("init_channel is_A:Bool signature:bits512 tag:# = tag 1768843636 channel_id:uint128 balance_A:Coins balance_B:Coins = InternalMsgBody");
    public static final long op_cooperative_close = 1433884798; // crc32("cooperative_close sig_A:^bits512 sig_B:^bits512 tag:# = tag 1131179891 channel_id:uint128 balance_A:Coins balance_B:Coins seqno_A:uint64 seqno_B:uint64 = InternalMsgBody");
    public static final long op_cooperative_commit = 2040604399; // crc32("cooperative_commit sig_A:^bits512 sig_B:^bits512 tag:# = tag 1128492404 channel_id:uint128 seqno_A:uint64 seqno_B:uint64 = InternalMsgBody");
    public static final long op_start_uncooperative_close = 521476815; // crc32("start_uncooperative_close signed_by_A:Bool signature:bits512 tag:# = tag 1433289580 channel_id:uint128 sch_A:^SignedSemiChannel sch_B:^SignedSemiChannel = InternalMsgBody");
    public static final long op_challenge_quarantined_state = 143567410; // crc32("challenge_quarantined_state challenged_by_A:Bool signature:bits512 tag:# = tag 1130915665 channel_id:uint128 sch_A:^SignedSemiChannel sch_B:^SignedSemiChannel = InternalMsgBody");
    public static final long op_settle_conditionals = 1727459433; // crc32("settle_conditionals from_A:Bool signature:bits512 tag:# = tag 1131168622 channel_id:uint128 conditionals_to_settle:HashmapE 32 Cell = InternalMsgBody");
    public static final long op_finish_uncooperative_close = 625158801; // crc32("finish_uncooperative_close = InternalMsgBody");
    public static final long op_channel_closed = -572749638; // crc32("channel_closed channel_id:uint128 = InternalMsgBody");

    public static Cell createSignatureCell(byte[] signature) {
        CellBuilder cell = CellBuilder.beginCell();
        cell.storeBytes(signature);
        return cell.endCell();
    }

    public static Cell createTopUpBalance(BigInteger coinsA, BigInteger coinsB) {
        CellBuilder cell = CellBuilder.beginCell();
        cell.storeUint(op_top_up_balance, 32); // OP
        cell.storeCoins(coinsA);
        cell.storeCoins(coinsB);
        return cell.endCell();
    }

    public static Cell createInitChannelBody(BigInteger channelId, BigInteger balanceA, BigInteger balanceB) {
        CellBuilder cell = CellBuilder.beginCell();
        cell.storeUint(tag_init, 32); // 0x696e6974
        cell.storeUint(channelId, 128);
        cell.storeCoins(balanceA);
        cell.storeCoins(balanceB);
        return cell.endCell();
    }

    public static Cell createCooperativeCloseChannelBody(BigInteger channelId, BigInteger balanceA, BigInteger balanceB, BigInteger seqnoA, BigInteger seqnoB) {
        CellBuilder cell = CellBuilder.beginCell();
        cell.storeUint(tag_cooperative_close, 32);
        cell.storeUint(channelId, 128);
        cell.storeCoins(balanceA);
        cell.storeCoins(balanceB);
        cell.storeUint(seqnoA, 64);
        cell.storeUint(seqnoB, 64);
        return cell.endCell();
    }

    public static Cell createCooperativeCommitBody(BigInteger channelId, BigInteger seqnoA, BigInteger seqnoB) {
        CellBuilder cell = CellBuilder.beginCell();
        cell.storeUint(tag_cooperative_commit, 32);
        cell.storeUint(channelId, 128);
        cell.storeUint(seqnoA, 64);
        cell.storeUint(seqnoB, 64);
        return cell.endCell();
    }

    public static Cell createConditionalPayment(BigInteger amount, Cell condition) {
        CellBuilder cell = CellBuilder.beginCell();
        cell.storeCoins(amount);
        cell.storeCell(condition);
        return cell.endCell();
    }

    public static Cell createSemiChannelBody(BigInteger seqno, BigInteger sentCoins, Cell conditionals) {
        CellBuilder cell = CellBuilder.beginCell();
        cell.storeUint(seqno, 64); // body start
        cell.storeCoins(sentCoins);
        cell.storeRefMaybe(conditionals);
        return cell.endCell();
    }

    public static Cell createSemiChannelState(BigInteger channelId, Cell semiChannelBody, Cell counterpartySemiChannelBody) {
        CellBuilder cell = CellBuilder.beginCell();
        cell.storeUint(tag_state, 32);
        cell.storeUint(channelId, 128);
        cell.writeCell(semiChannelBody);
        cell.storeRefMaybe(counterpartySemiChannelBody);
        return cell.endCell();
    }

    public static Cell createSignedSemiChannelState(byte[] signature, Cell state) {
        CellBuilder cell = CellBuilder.beginCell();
        cell.storeBytes(signature);
        cell.writeCell(state);
        return cell.endCell();
    }

    public static Cell createStartUncooperativeCloseBody(BigInteger channelId, Cell signedSemiChannelStateA, Cell signedSemiChannelStateB) {
        CellBuilder cell = CellBuilder.beginCell();
        cell.storeUint(tag_start_uncooperative_close, 32);
        cell.storeUint(channelId, 128);
        cell.storeRef(signedSemiChannelStateA);
        cell.storeRef(signedSemiChannelStateB);
        return cell.endCell();
    }

    public static Cell createChallengeQuarantinedStateBody(BigInteger channelId, Cell signedSemiChannelStateA, Cell signedSemiChannelStateB) {
        CellBuilder cell = CellBuilder.beginCell();
        cell.storeUint(tag_challenge_state, 32);
        cell.storeUint(channelId, 128);
        cell.storeRef(signedSemiChannelStateA);
        cell.storeRef(signedSemiChannelStateB);
        return cell.endCell();
    }

    public static Cell createSettleConditionalsBody(BigInteger channelId, Cell conditionalsToSettle) {
        CellBuilder cell = CellBuilder.beginCell();
        cell.storeUint(tag_settle_conditionals, 32);
        cell.storeUint(channelId, 128);
        cell.storeRefMaybe(conditionalsToSettle);
        return cell.endCell();
    }

    public static Cell createFinishUncooperativeClose() {
        CellBuilder cell = CellBuilder.beginCell();
        cell.storeUint(op_finish_uncooperative_close, 32); // OP
        return cell.endCell();
    }

    public static Cell createOneSignature(long op, boolean isA, byte[] signature, Cell cell) {
        CellBuilder c = CellBuilder.beginCell();
        c.storeUint(op, 32); // OP
        c.storeBit(isA);
        c.storeBytes(signature);
        c.storeCell(cell);
        return c.endCell();
    }

    public static Cell createTwoSignature(long op, byte[] signatureA, byte[] signatureB, Cell cell) {
        CellBuilder c = CellBuilder.beginCell();
        c.storeUint(op, 32); // OP
        c.storeRef(createSignatureCell(signatureA));
        c.storeRef(createSignatureCell(signatureB));
        c.storeCell(cell);
        return c.endCell();
    }
}
