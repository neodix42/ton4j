package org.ton.java.smartcontract.payments;

import org.ton.java.cell.Cell;

import java.math.BigInteger;

import static java.util.Objects.nonNull;

public class PaymentsUtils {
    public static final long tag_init = 0x696e6974;
    public static final long tag_cooperative_close = 0x436c6f73;
    public static final long tag_cooperative_commit = 0x43436d74;
    public static final long tag_start_uncooperative_close = 0x556e436c;
    public static final long tag_challenge_state = 0x43686751;
    public static final long tag_settle_conditionals = 0x436c436e;
    public static final long tag_state = 0x43685374;

    public static final long op_top_up_balance = 1741148801; // crc32("top_up_balance add_A:Coins add_B:Coins = InternalMsgBody");
    public static final long op_init_channel = 235282626; // crc32("init_channel is_A:Bool signature:bits512 tag:# = tag 1768843636 channel_id:uint128 balance_A:Coins balance_B:Coins = InternalMsgBody");
    public static final long op_cooperative_close = 1433884798; // crc32("cooperative_close sig_A:^bits512 sig_B:^bits512 tag:# = tag 1131179891 channel_id:uint128 balance_A:Coins balance_B:Coins seqno_A:uint64 seqno_B:uint64 = InternalMsgBody");
    public static final long op_cooperative_commit = 2040604399; // crc32("cooperative_commit sig_A:^bits512 sig_B:^bits512 tag:# = tag 1128492404 channel_id:uint128 seqno_A:uint64 seqno_B:uint64 = InternalMsgBody");
    public static final long op_start_uncooperative_close = 521476815; // crc32("start_uncooperative_close signed_by_A:Bool signature:bits512 tag:# = tag 1433289580 channel_id:uint128 sch_A:^SignedSemiChannel sch_B:^SignedSemiChannel = InternalMsgBody");
    public static final long op_challenge_quarantined_state = 143567410; // crc32("challenge_quarantined_state challenged_by_A:Bool signature:bits512 tag:# = tag 1130915665 channel_id:uint128 sch_A:^SignedSemiChannel sch_B:^SignedSemiChannel = InternalMsgBody");
    public static final long op_settle_conditionals = 1727459433; // crc32("settle_conditionals from_A:Bool signature:bits512 tag:# = tag 1131168622 channel_id:uint128 conditionals_to_settle:HashmapE 32 Cell = InternalMsgBody");
    public static final long op_finish_uncooperative_close = 625158801; // crc32("finish_uncooperative_close = InternalMsgBody");
    public static final long op_channel_closed = -572749638; // crc32("channel_closed channel_id:uint128 = InternalMsgBody");

    public static void writePublicKey(Cell cell, byte[] publicKey) {
        if (publicKey.length != 256 / 8) {
            throw new Error("invalid publicKey length");
        }
        cell.bits.writeBytes(publicKey);
    }

    public static void writeSignature(Cell cell, byte[] signature) {
        if (signature.length != 512 / 8) {
            throw new Error("invalid signature length");
        }
        cell.bits.writeBytes(signature);
    }

    public static Cell createSignatureCell(byte[] signature) {
        Cell cell = new Cell();
        writeSignature(cell, signature);
        return cell;
    }

    public static void writeMayBe(Cell cell, Cell ref) {
        if (nonNull(ref)) {
            cell.bits.writeBit(true);
            if (cell.refs.size() >= 4) {
                throw new Error("refs overflow");
            }
            cell.refs.add(ref);
        } else {
            cell.bits.writeBit(false);
        }
    }

    public static Cell createTopUpBalance(BigInteger coinsA, BigInteger coinsB) {
        Cell cell = new Cell();
        cell.bits.writeUint(op_top_up_balance, 32); // OP
        cell.bits.writeCoins(coinsA);
        cell.bits.writeCoins(coinsB);
        return cell;
    }

    public static Cell createInitChannelBody(BigInteger channelId, BigInteger balanceA, BigInteger balanceB) {
        Cell cell = new Cell();
        cell.bits.writeUint(tag_init, 32);
        cell.bits.writeUint(channelId, 128);
        cell.bits.writeCoins(balanceA);
        cell.bits.writeCoins(balanceB);
        return cell;
    }

    public static Cell createCooperativeCloseChannelBody(BigInteger channelId, BigInteger balanceA, BigInteger balanceB, BigInteger seqnoA, BigInteger seqnoB) {
        Cell cell = new Cell();
        cell.bits.writeUint(tag_cooperative_close, 32);
        cell.bits.writeUint(channelId, 128);
        cell.bits.writeCoins(balanceA);
        cell.bits.writeCoins(balanceB);
        cell.bits.writeUint(seqnoA, 64);
        cell.bits.writeUint(seqnoB, 64);
        return cell;
    }

    public static Cell createCooperativeCommitBody(BigInteger channelId, BigInteger seqnoA, BigInteger seqnoB) {
        Cell cell = new Cell();
        cell.bits.writeUint(tag_cooperative_commit, 32);
        cell.bits.writeUint(channelId, 128);
        cell.bits.writeUint(seqnoA, 64);
        cell.bits.writeUint(seqnoB, 64);
        return cell;
    }

    public static Cell createConditionalPayment(BigInteger amount, Cell condition) {
        Cell cell = new Cell();
        cell.bits.writeCoins(amount);
        cell.writeCell(condition);
        return cell;
    }

    public static Cell createSemiChannelBody(BigInteger seqno, BigInteger sentCoins, Cell conditionals) {
        Cell cell = new Cell();
        cell.bits.writeUint(seqno, 64); // body start
        cell.bits.writeCoins(sentCoins);
        writeMayBe(cell, conditionals);  // HashmapE 32 ConditionalPayment
        return cell;
    }

    public static Cell createSemiChannelState(BigInteger channelId, Cell semiChannelBody, Cell counterpartySemiChannelBody) {
        Cell cell = new Cell();
        cell.bits.writeUint(tag_state, 32);
        cell.bits.writeUint(channelId, 128);
        cell.writeCell(semiChannelBody);
        writeMayBe(cell, counterpartySemiChannelBody);
        return cell;
    }

    public static Cell createSignedSemiChannelState(byte[] signature, Cell state) {
        Cell cell = new Cell();
        writeSignature(cell, signature);
        cell.writeCell(state);
        return cell;
    }

    public static Cell createStartUncooperativeCloseBody(BigInteger channelId, Cell signedSemiChannelStateA, Cell signedSemiChannelStateB) {
        Cell cell = new Cell();
        cell.bits.writeUint(tag_start_uncooperative_close, 32);
        cell.bits.writeUint(channelId, 128);
        cell.refs.add(signedSemiChannelStateA);
        cell.refs.add(signedSemiChannelStateB);
        return cell;
    }

    public static Cell createChallengeQuarantinedStateBody(BigInteger channelId, Cell signedSemiChannelStateA, Cell signedSemiChannelStateB) {
        Cell cell = new Cell();
        cell.bits.writeUint(tag_challenge_state, 32);
        cell.bits.writeUint(channelId, 128);
        cell.refs.add(signedSemiChannelStateA);
        cell.refs.add(signedSemiChannelStateB);
        return cell;
    }

    public static Cell createSettleConditionalsBody(BigInteger channelId, Cell conditionalsToSettle) {
        Cell cell = new Cell();
        cell.bits.writeUint(tag_settle_conditionals, 32);
        cell.bits.writeUint(channelId, 128);
        writeMayBe(cell, conditionalsToSettle); // HashmapE 32 Cell
        return cell;
    }

    public static Cell createFinishUncooperativeClose() {
        Cell cell = new Cell();
        cell.bits.writeUint(op_finish_uncooperative_close, 32); // OP
        return cell;
    }

    public static Cell createOneSignature(long op, boolean isA, byte[] signature, Cell cell) {
        Cell c = new Cell();
        c.bits.writeUint(op, 32); // OP
        c.bits.writeBit(isA);
        writeSignature(c, signature);
        c.writeCell(c);
        return c;
    }

    public static Cell createTwoSignature(long op, byte[] signatureA, byte[] signatureB, Cell cell) {
        Cell c = new Cell();
        c.bits.writeUint(op, 32); // OP
        c.refs.add(createSignatureCell(signatureA));
        c.refs.add(createSignatureCell(signatureB));
        c.writeCell(cell);
        return c;
    }
}
