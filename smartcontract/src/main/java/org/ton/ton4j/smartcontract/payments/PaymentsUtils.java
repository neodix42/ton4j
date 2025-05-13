package org.ton.ton4j.smartcontract.payments;

import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellBuilder;

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
        return CellBuilder.beginCell()
                .storeBytes(signature)
                .endCell();
    }

    public static Cell createTopUpBalance(BigInteger coinsA, BigInteger coinsB) {
        return CellBuilder.beginCell()
                .storeUint(op_top_up_balance, 32) // OP
                .storeCoins(coinsA)
                .storeCoins(coinsB)
                .endCell();
    }

    public static Cell createInitChannelBody(BigInteger channelId, BigInteger balanceA, BigInteger balanceB) {
        return CellBuilder.beginCell()
                .storeUint(tag_init, 32) // 0x696e6974
                .storeUint(channelId, 128)
                .storeCoins(balanceA)
                .storeCoins(balanceB)
                .endCell();
    }


//    public static Signature createStartUncooperativeClose(Cell signedSemiChannelStateA, Cell signedSemiChannelStateB) {
//        return createOneSignature(op_start_uncooperative_close,
//                createStartUncooperativeCloseBody(
//                        channelConfig.getChannelId(),
//                        signedSemiChannelStateA,
//                        signedSemiChannelStateB));
//    }
//
//    public static Signature createChallengeQuarantinedState( Cell signedSemiChannelStateA, Cell signedSemiChannelStateB) {
//        return createOneSignature( op_challenge_quarantined_state,
//                createChallengeQuarantinedStateBody(
//                        channelConfig.getChannelId(),
//                        signedSemiChannelStateA,
//                        signedSemiChannelStateB));
//    }


    public static Cell createCooperativeCloseChannelBody(BigInteger channelId, BigInteger balanceA, BigInteger balanceB, BigInteger seqnoA, BigInteger seqnoB) {
        return CellBuilder.beginCell()
                .storeUint(tag_cooperative_close, 32)
                .storeUint(channelId, 128)
                .storeCoins(balanceA)
                .storeCoins(balanceB)
                .storeUint(seqnoA, 64)
                .storeUint(seqnoB, 64)
                .endCell();
    }

    public static Cell createCooperativeCommitBody(BigInteger channelId, BigInteger seqnoA, BigInteger seqnoB) {
        return CellBuilder.beginCell()
                .storeUint(tag_cooperative_commit, 32)
                .storeUint(channelId, 128)
                .storeUint(seqnoA, 64)
                .storeUint(seqnoB, 64)
                .endCell();
    }

    public static Cell createConditionalPayment(BigInteger amount, Cell condition) {
        return CellBuilder.beginCell()
                .storeCoins(amount)
                .storeCell(condition)
                .endCell();
    }

    public static Cell createSemiChannelBody(BigInteger seqno, BigInteger sentCoins, Cell conditionals) {
        return CellBuilder.beginCell()
                .storeUint(seqno, 64) // body start
                .storeCoins(sentCoins)
                .storeRefMaybe(conditionals)
                .endCell();
    }

    public static Cell createSemiChannelState(BigInteger channelId, Cell semiChannelBody, Cell counterpartySemiChannelBody) {
        return CellBuilder.beginCell()
                .storeUint(tag_state, 32)
                .storeUint(channelId, 128)
                .storeCell(semiChannelBody)
                .storeRefMaybe(counterpartySemiChannelBody)
                .endCell();
    }

    public static Cell createSignedSemiChannelState(byte[] signature, Cell state) {
        return CellBuilder.beginCell()
                .storeBytes(signature)
                .storeCell(state)
                .endCell();

    }

    public static Cell createStartUncooperativeCloseBody(BigInteger channelId, Cell signedSemiChannelStateA, Cell signedSemiChannelStateB) {
        return CellBuilder.beginCell()
                .storeUint(tag_start_uncooperative_close, 32)
                .storeUint(channelId, 128)
                .storeRef(signedSemiChannelStateA)
                .storeRef(signedSemiChannelStateB)
                .endCell();
    }

    public static Cell createChallengeQuarantinedStateBody(BigInteger channelId, Cell signedSemiChannelStateA, Cell signedSemiChannelStateB) {
        return CellBuilder.beginCell()
                .storeUint(tag_challenge_state, 32)
                .storeUint(channelId, 128)
                .storeRef(signedSemiChannelStateA)
                .storeRef(signedSemiChannelStateB)
                .endCell();
    }

    public static Cell createSettleConditionalsBody(BigInteger channelId, Cell conditionalsToSettle) {
        return CellBuilder.beginCell()
                .storeUint(tag_settle_conditionals, 32)
                .storeUint(channelId, 128)
                .storeRefMaybe(conditionalsToSettle)
                .endCell();
    }

    public static Cell createFinishUncooperativeClose() {
        return CellBuilder.beginCell()
                .storeUint(op_finish_uncooperative_close, 32) // OP
                .endCell();
    }

    public static Cell createOneSignature(long op, boolean isA, byte[] signature, Cell cell) {
        return CellBuilder.beginCell()
                .storeUint(op, 32) // OP
                .storeBit(isA)
                .storeBytes(signature)
                .storeCell(cell)
                .endCell();
    }

    public static Cell createTwoSignature(long op, byte[] signatureA, byte[] signatureB, Cell cell) {
        return CellBuilder.beginCell()
                .storeUint(op, 32) // OP
                .storeRef(createSignatureCell(signatureA))
                .storeRef(createSignatureCell(signatureB))
                .storeCell(cell)
                .endCell();
    }
}
