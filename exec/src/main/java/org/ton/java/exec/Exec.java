package org.ton.java.exec;

import org.ton.java.utils.Utils;
import org.ton.java.cell.Cell;
import org.ton.java.address.Address;
import org.ton.java.cell.CellBuilder;
import org.ton.java.tonlib.Tonlib;
import org.ton.java.tonlib.types.BlockIdExt;
import org.ton.java.tonlib.types.MasterChainInfo;
import org.ton.java.tonlib.types.BlockTransactions;
import org.ton.java.tonlib.types.ShortTxId;
import org.ton.java.tonlib.types.*;

import java.util.concurrent.TimeUnit;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.HashSet;


public class Exec {
  private Tonlib tonlib;
  private HashSet<String> last_shards;
  private long last_mc_seqno;

  public static Address get_address_by_key (byte key[]) {
    CellBuilder cb = CellBuilder.beginCell ();
    boolean c[] = {false, false, true, true, false};
    cb.storeBits (c);

    CellBuilder cb_code = CellBuilder.beginCell ();
    cb_code.storeBytes (Utils.hexToBytes ("FF0020DD2082014C97BA218201339CBAB19F71B0ED44D0D31FD31F31D70BFFE304E0A4F2608308D71820D31FD31FD31FF82313BBF263ED44D0D31FD31FD3FFD15132BAF2A15144BAF2A204F901541055F910F2A3F8009320D74A96D307D402FB00E8D101A4C8CB1FCB1FCBFFC9ED54"));
    cb.storeRef (cb_code.endCell ());

    CellBuilder cb_data = CellBuilder.beginCell ();
    cb_data.storeInt (0, 32); // seqno
    cb_data.storeInt (0, 32); // subwallet
    cb_data.storeBytes (key);
    cb.storeRef (cb_data.endCell ());

    Cell cell = cb.endCell ();
    return new Address ("0:" + Utils.bytesToHex(cell.hash()));
  }
  private static Cell create_message (byte [] signature, int seqno, int valid_until, String payload, byte key[], Address src, Address dst, long grams) {
    CellBuilder cb = CellBuilder.beginCell ();
    if (signature.length > 0) {
      // 10 - external message
      // 00 - src addr none 
      // 10 - addr-std
      // 0 - not anycast
      boolean []b01 = {true, false, false, false, true, false, false};
      cb.storeBits(b01);
      cb.storeInt (src.wc, 8);
      cb.storeBytes (src.hashPart);
      cb.storeCoins (BigInteger.valueOf(0)); // import fee
      if (seqno == 1) {
        cb.storeBit(true); // has_state
        cb.storeBit(false); // state is inlined
        cb.storeBit(false); // splitdepth
        cb.storeBit(false); // special
        cb.storeBit(true); // code
        cb.storeBit(true); // data
        cb.storeBit(false); // libraries

        CellBuilder cb_code = CellBuilder.beginCell ();
        cb_code.storeBytes (Utils.hexToBytes ("FF0020DD2082014C97BA218201339CBAB19F71B0ED44D0D31FD31F31D70BFFE304E0A4F2608308D71820D31FD31FD31FF82313BBF263ED44D0D31FD31FD3FFD15132BAF2A15144BAF2A204F901541055F910F2A3F8009320D74A96D307D402FB00E8D101A4C8CB1FCB1FCBFFC9ED54"));
        cb.storeRef (cb_code.endCell ());

        CellBuilder cb_data = CellBuilder.beginCell ();
        cb_data.storeInt (0, 32); // seqno
        cb_data.storeInt (0, 32); // subwallet
        cb_data.storeBytes (key);
        cb.storeRef (cb_data.endCell ());
      } else {
        cb.storeBit(false);
      }
      cb.storeBit(false); // body inlined
      cb.storeBytes(signature); // signature
    }
    cb.storeInt (0, 32); //subwallet
    cb.storeInt (valid_until, 32); //unix_time
    cb.storeInt (seqno, 32); //seqno
    cb.storeInt (3, 8); // send mode
    
    CellBuilder cb2 = CellBuilder.beginCell ();
    // 0 - internal message
    // 1 - ihr disabled
    boolean []b1 = {false, true};
    cb2.storeBits (b1);
    cb2.storeBit (dst.isBounceable);
    // 0 - bounced
    // 00 - src address is omitted
    // 10 - msg addr std
    // 0 - not anycast
    boolean []b2 = {false, false, false, true, false, false};
    cb2.storeBits (b2);
    cb2.storeInt (dst.wc, 8);
    cb2.storeBytes (dst.hashPart);
    cb2.storeCoins (BigInteger.valueOf(grams));

    // magic:
    //   0 - empty extra currency collection 
    //   8 bits ??
    //   ihr_fee = 0G, 1 bit
    //   fwd_fee = 0G, 1 bit
    //   created_lt = 0LL, 64 bits
    //   created_at = 0I, 32 bits
    for (int i = 0; i < 9 + 1 + 1 + 64 + 32; i++) {
      cb2.storeBit (false);
    }
    if (payload.length() > 0) {
      cb2.storeInt (0, 32);
    }
    cb2.storeString (payload);
    cb.storeRef (cb2.endCell ());
    return cb.endCell ();
  }
  private static Cell create_unsigned_message(int seqno, int valid_until, String payload, byte key[], Address src, Address dst, long grams) {
    byte [] signature = {};
    return create_message (signature, seqno, valid_until, payload, key, src, dst, grams); 
  }
  public static byte[] create_data_to_sign(int seqno, int valid_until, String payload, byte key[], Address src, Address dst, long grams) {
    return create_unsigned_message (seqno, valid_until, payload, key, src, dst, grams).hash (); 
  }
  public static byte[] create_signed_message(byte [] signature, int seqno, int valid_until, String payload, byte key[], Address src, Address dst, long grams) {
    return create_message (signature, seqno, valid_until, payload, key, src, dst, grams).toBoc (); 
  }
  public static byte[] create_in_msg_hash (byte [] signature, int seqno, int valid_until, String payload, byte key[], Address src, Address dst, long grams) {
    return create_message (signature, seqno, valid_until, payload, key, src, dst, grams).toBoc (); 
  }

  public static String uniform_account_name (byte workchain, ShortTxId tx) {
    var addr = new Address (workchain + ":" + Utils.base64ToHexString(tx.getAccount()));
    return addr.toString (true, false, true, false);
  }

  public static String uniform_account_name (byte workchain, AccountAddressOnly a) {
    var addr = new Address (a.getAccount_address ());
    return addr.toString (true, false, true, false);
  }
  public boolean skip_account_in_transaction_list (String account) {
    return false;
  }

  public void new_transaction_callback (byte workchain, ShortTxId tx) {
    if (skip_account_in_transaction_list (uniform_account_name (workchain, tx))) {
      return;
    }
    //System.out.println ("new transaction " + tx.toString ());
    var rt = tonlib.getRawTransaction(workchain, tx); 
    //System.out.println ("raw new transaction " + rt.toString ());

    var in_msg = rt.getIn_msg ();

    // TIC-TOC, only in system accounts
    if (in_msg == null) {
      return;
    }

    var out_msgs = rt.getOut_msgs();

    /* inbound message, probably inbound transfer */
    if (out_msgs.size() == 0) {
      System.out.println ("Inbound transfer: from=" + uniform_account_name (workchain, in_msg.getSource ()) 
                          + " to=" + uniform_account_name (workchain, in_msg.getDestination ()) + " value=" 
                          + in_msg.getValue ()  +" fwd_fee=" + in_msg.getFwd_fee () + " fee=" + rt.getFee () 
                          + " storage_fee=" + rt.getStorage_fee ());
    } else {
      for (var out_msg : out_msgs) {
        System.out.println ("Outbound transfer: from=" + uniform_account_name (workchain, out_msg.getSource ()) 
                            + " to=" + uniform_account_name (workchain, out_msg.getDestination ()) + " value=" 
                            + out_msg.getValue () + " fwd_fee=" + out_msg.getFwd_fee () + " fee(total)=" + rt.getFee () 
                            + " storage_fee(total)=" + rt.getStorage_fee ());
      }
    }
  }
  
  public void scan_new_block_transactions (BlockIdExt block_id) {
    BlockTransactions t = tonlib.getBlockTransactions(block_id, 100);

    while (true) {
      List<ShortTxId> transactions = t.getTransactions ();
      for (var tx : transactions) {
        new_transaction_callback ((byte)block_id.getWorkchain(), tx);
      }
      
      if (!t.isIncomplete () || transactions.size() == 0) {
        break;
      }
 
      var last = transactions.get(transactions.size() - 1);
      t = tonlib.getBlockTransactions (block_id, 100, last.getLt(), last.getHash ());
    }
  }

  public void scan_new_block_transactions_rec (BlockIdExt block_id, int depth) {
    if (last_shards.contains (block_id.toString ())) {
      return;
    }
    if (depth >= 16) {
      // ASSERT?
      return;
    }
    System.out.println ("block=" + block_id + " depth=" + depth);
    BlockHeader head = tonlib.getBlockHeader (block_id);
    if (head.isAfter_split() || head.isAfter_merge()) {
      scan_new_block_transactions (block_id);
      return;
    }

    var prev_blocks = head.getPrev_blocks ();
    // assert? it is not split/merge
    if (prev_blocks.size() != 1) {
      scan_new_block_transactions (block_id);
      return;
    }

    BlockIdExt prev_block = prev_blocks.get (0);

    System.out.println (last_shards);

    scan_new_block_transactions_rec (prev_block, depth + 1);
    scan_new_block_transactions (block_id);
  }

  public void scan_new_mc_block_transactions (long seqno) {
    BlockIdExt block_id = tonlib.lookupBlock(seqno, -1, 0x8000000000000000L, 0, 0);
    System.out.println ("found new MC block: " + block_id.toString ());

    scan_new_block_transactions (block_id);
  
    HashSet<String> new_last_shards = new HashSet<String> ();

    var shardsR = tonlib.getShards(block_id);
    var shards = shardsR.getShards ();
    for (BlockIdExt shard : shards) {
      new_last_shards.add (shard.toString ());
      scan_new_block_transactions_rec (shard, 0);
    }

    last_mc_seqno = block_id.getSeqno ();
    last_shards = new_last_shards;
  }

  public boolean scan_new_transactions () {
    MasterChainInfo mi = tonlib.getLast (); 
    long new_seqno = mi.getLast().getSeqno();
    if (new_seqno == last_mc_seqno) {
      return false;
    }
    while (last_mc_seqno < new_seqno) {
      scan_new_mc_block_transactions (last_mc_seqno + 1);
    }

    return true;
  }

  public void loop() {
    boolean quit = false;
    while (!quit) {
      try {
        scan_new_transactions ();
      } catch (Exception e) {
        // try again later?
        // check exception type?
      }
      try {
        TimeUnit.SECONDS.sleep(1);
      } catch (java.lang.InterruptedException e) {
        quit = true;
      }
    }
    System.out.println ("result mc seqno " + last_mc_seqno);
  }

  public void run () {
    tonlib = Tonlib.builder()
      .pathToGlobalConfig ("config.json")
      .pathToTonlibSharedLib("./libtonlibjson.so")
      .keystoreInMemory(true)
      .verbosityLevel(VerbosityLevel.FATAL)
      .build ();
    MasterChainInfo mi = tonlib.getLast (); 
    System.out.println (mi.getLast().toString ());
    last_mc_seqno = mi.getLast ().getSeqno ();
    last_shards = new HashSet<String> ();
    var shardsR = tonlib.getShards(mi.getLast ());
    var shards = shardsR.getShards ();
    for (BlockIdExt shard : shards) {
      last_shards.add (shard.toString ());  
    }
    System.out.println (mi);
  }

  public long get_seqno (Address addr) {
    AccountAddressOnly accountAddressOnly = AccountAddressOnly.builder()
            .account_address(addr.toString(false))
            .build();
    var account_state = tonlib.getRawAccountState(accountAddressOnly);
    if (account_state == null) {
      return 0;
    }
    var account_data = account_state.getData ();
    if (account_data == null) {
      return 0;
    }
    var cell = Cell.fromBoc (Utils.base64ToBytes (account_data));
    System.out.println (cell);
    return cell.bits.preReadUint (32).intValue ();
  }
  
  private Address dns_resolve_in (String name, AccountAddressOnly acc_only) {
    var result = tonlib.dnsResolve (name, acc_only);
    if (result == null) {
      return null;
    }
    var entries = result.getEntries ();
    if (entries == null) {
      return null;
    }
    for (var entry : entries) {
      var info = entry.getEntry ();
      if (info == null) {
        continue;
      }
      if (info.getType ().equals ("dns.entryDataNextResolver")) {
        /* can return Address (info.getResolver ()), if only *.ton and *.t.me are supported */
        return dns_resolve_in (entry.getName (), info.getResolver ());
      } else {
        return new Address (acc_only.getAccount_address ());
      }
    }
    return new Address (acc_only.getAccount_address ());
  }

  public Address dns_resolve (String name) {
    return dns_resolve_in (name, null);
  }

  public Address dns_get_owner (Address addr) {
    AccountAddressOnly accountAddressOnly = AccountAddressOnly.builder()
            .account_address(addr.toString(false))
            .build();
    var account_state = tonlib.getRawAccountState(accountAddressOnly);
    if (account_state == null) {
      return null;
    }
    var account_data = account_state.getData ();
    if (account_data == null) {
      return null;
    }
    var cell = Cell.fromBoc (Utils.base64ToBytes (account_data));
    cell = cell.refs.get (1);
    var b = cell.bits;
    b.readBits (3);
    var workchain = b.readInt (8).intValue ();
    var data = b.readBytes (32 * 8);
    return new Address ("" + workchain + ":" + Utils.bytesToHex (data));
  }

  public static void main (String args[]) {
    Exec exc = new Exec ();
    exc.run ();
    exc.dns_resolve ("igorasfklsdjfklsjfklsjdf.t.me.");
    var v = exc.dns_resolve ("igor.t.me.");
    var addr = exc.dns_get_owner (v);
    System.out.println (addr.toString (true, true, true, false));
  }
}
