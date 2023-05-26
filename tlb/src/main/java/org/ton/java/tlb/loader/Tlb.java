package org.ton.java.tlb.loader;

import org.ton.java.address.Address;
import org.ton.java.cell.CellSlice;
import org.ton.java.tlb.types.*;

import java.math.BigInteger;

import static java.util.Objects.isNull;

public class Tlb {

    public static Object load(Class c, CellSlice cs) {

        switch (c.getSimpleName()) {
            case "BlockIdExt": // todo
                return BlockIdExt.builder().build();
            case "TickTock":
                return TickTock.builder()
                        .tick(cs.loadBit())
                        .tock(cs.loadBit())
                        .build();
            case "StateInit":
                BigInteger depth = cs.loadUint(5);
                TickTock tickTock = (TickTock) Tlb.load(TickTock.class, cs);
                return StateInit.builder()
                        .depth(depth)
                        .tickTock(tickTock)
                        .code(cs.loadRef())
                        .data(cs.loadRef())
                        //.lib() // todo
                        .build();
            case "AccountStorage":
                AccountStorage accountStorage = AccountStorage.builder().build();
                BigInteger lastTransaction = cs.loadUint(64);
                BigInteger coins = cs.loadCoins();
                boolean extraExists = cs.loadBit();
                if (extraExists) {
                    throw new Error("extra currency info is not supported for AccountStorage");
                }
                boolean isStatusActive = cs.loadBit();
                if (isStatusActive) {
                    accountStorage.setAccountStatus("ACTIVE");
                    StateInit stateInit = (StateInit) Tlb.load(StateInit.class, cs);
                    accountStorage.setStateInit(stateInit);
                } else {
                    boolean isStatusFrozen = cs.loadBit();
                    if (isStatusFrozen) {
                        accountStorage.setAccountStatus("FROZEN");
                        byte[] stateHash = cs.loadBytes(256);
                        accountStorage.setStateHash(stateHash);
                    } else {
                        accountStorage.setAccountStatus("UNINIT");
                    }
                }
                accountStorage.setLastTransactionLt(lastTransaction);
                accountStorage.setBalance(coins);
                return accountStorage;
            case "StorageInfo":
                StorageUsed storageUsed = (StorageUsed) Tlb.load(StorageUsed.class, cs);
                long lastPaid = cs.loadUint(32).longValue();
                boolean isDuePayment = cs.loadBit();
                return StorageInfo.builder()
                        .storageUsed(storageUsed)
                        .lastPaid(lastPaid)
                        .duePayment(isDuePayment ? cs.loadUint(64) : null)
                        .build();
            case "StorageUsed":
                BigInteger cells = cs.loadVarUInteger(BigInteger.valueOf(7));
                BigInteger bits = cs.loadVarUInteger(BigInteger.valueOf(7));
                BigInteger pubCells = cs.loadVarUInteger(BigInteger.valueOf(7));
                return StorageUsed.builder()
                        .bitsUsed(bits)
                        .cellsUsed(cells)
                        .publicCellsUsed(pubCells)
                        .build();
            case "AccountState": {
                boolean isAccount = cs.loadBit();
                if (!isAccount) {
                    return null;
                }
                Address address = cs.loadAddress();
                StorageInfo info = (StorageInfo) Tlb.load(StorageInfo.class, cs);
                AccountStorage storage = (AccountStorage) Tlb.load(AccountStorage.class, cs);

                return AccountState.builder()
                        .isValid(true)
                        .address(address)
                        .storageInfo(info)
                        .accountStorage(storage)
                        .build();
            }
            case "AccountStatus":
                byte status = cs.loadUint(2).byteValueExact();
                switch (status) {
                    case 0b11:
                        return "NON_EXIST";
                    case 0b10:
                        return "ACTIVE";
                    case 0b01:
                        return "FROZEN";
                    case 0b00:
                        return "UNINIT";
                }
            case "StateUpdate":
                return StateUpdate.builder()
                        .oldOne((ShardState) Tlb.load(ShardState.class, CellSlice.beginParse(cs.loadRef())))
                        .newOne(cs.loadRef())
                        .build();
            case "ConfigParams":
                return ConfigParams.builder()
                        .configAddr(cs.loadAddress())
                        .config(cs.loadDict(32, k -> k.readInt(32), v -> v))
                        .build();
            case "ShardStateUnsplit":
                assert cs.loadUint(32).longValue() == 0x9023afe2L : "ShardStateUnsplit: magic not equal to 0x9023afe2";
                return ShardStateUnsplit.builder()
                        .magic(0x9023afe2L)
                        .globalId(cs.loadUint(32).longValue())
                        .shardIdent((ShardIdent) Tlb.load(ShardIdent.class, cs))
                        .seqno(cs.loadUint(32).longValue())
                        .vertSeqno(cs.loadUint(32).longValue())
                        .genUTime(cs.loadUint(32).longValue())
                        .genLT(cs.loadUint(64))
                        .minRefMCSeqno(cs.loadUint(32).longValue())
                        .outMsgQueueInfo(cs.loadRef())
                        .accounts(CellSlice.beginParse(cs.loadRef()).loadDict(256, k -> k.readInt(256), v -> v))
                        .beforeSplit(cs.loadBit())
                        .stats(cs.loadRef())
//                        .mc(isNull(cs.preloadMaybeRefX()) ? null : (McStateExtra) Tlb.load(McStateExtra.class, CellSlice.beginParse(cs.loadRef()))) // todo fix
                        .build();
            case "ShardIdent":
                //cs.skipBits(2); //magic
                assert cs.loadUint(0).longValue() == 0L;
                return ShardIdent.builder()
                        .magic(0L)
                        .prefixBits(cs.loadUint(6).byteValueExact())
                        .workchain(cs.loadUint(32).longValue())
                        .shardPrefix(cs.loadUint(64))
                        .build();
            case "ShardDesc": // todo
                return ShardDesc.builder().build();
            case "ShardState":
//                CellSlice clone = cs.clone();
                long tag = cs.preloadUint(32).longValue();
                if (tag == 0x5f327da5L) {
                    ShardStateUnsplit left, right;
                    left = (ShardStateUnsplit) Tlb.load(ShardStateUnsplit.class, CellSlice.beginParse(cs.loadRef()));
                    right = (ShardStateUnsplit) Tlb.load(ShardStateUnsplit.class, CellSlice.beginParse(cs.loadRef()));
                    return ShardState.builder()
                            .left(left)
                            .right(right)
                            .build();
                } else if (tag == 0x9023afe2L) {
                    return ShardState.builder()
                            .left((ShardStateUnsplit) Tlb.load(ShardStateUnsplit.class, cs))
                            .build();
                } else {
                    throw new Error("unknown shardstate tag");
                }
            case "McStateExtra":
                if (isNull(cs)) {
                    return null;
                }
                assert cs.loadUint(32).longValue() == 0xcc26L : "McStateExtra: magic not equal to 0xcc26";
                return McStateExtra.builder()
                        .magic(0xcc26L)
                        .shardHashes(cs.loadDict(32, k -> k.readInt(32), v -> v))
                        .configParams((ConfigParams) Tlb.load(ConfigParams.class, cs))
                        .info(cs.loadRef())
                        .globalBalance((CurrencyCollection) Tlb.load(CurrencyCollection.class, cs))
                        .build();
            case "McBlockExtra":
//                cs.skipBits(32);
                assert cs.loadUint(16).longValue() == 0xcca5L : "McBlockExtra: magic not equal to 0xcca5";
                return McBlockExtra.builder()
                        .magic(0xcca5L)
                        .keyBlock(cs.loadBit())
                        .shardHashes(cs.loadDict(32, k -> k.readInt(32), v -> v))
                        .shardFees(cs.loadDict(92, k -> k.readInt(92), v -> v))
                        .build();
            case "CurrencyCollection":
                return CurrencyCollection.builder()
                        .coins(cs.loadCoins())
                        .extraCurrencies(cs.loadDict(32, k -> k.readInt(32), v -> v))
                        .build();
            case "GlobalVersion":
//                cs.skipBits(8); //magic
                assert cs.loadUint(8).longValue() == 0xc4L : "GlobalVersion: magic not equal to 0xc4";
                return GlobalVersion.builder()
                        .magic(0xc4L)
                        .version(cs.loadUint(32).longValue())
                        .capabilities(cs.loadUint(64))
                        .build();
            case "ExtBlkRef":
                return ExtBlkRef.builder()
                        .endLt(cs.loadUint(64))
                        .seqno(cs.loadUint(32).intValue())
                        .rootHash(cs.loadBytes(256))
                        .fileHash(cs.loadBytes(256))
                        .build();
            case "BlockInfoPart":
//                cs.skipBits(32); //magic
                assert cs.loadUint(32).longValue() == 0x9bc7a987L : "BlockInfoPart: magic not equal to 0x9bc7a987";
                return BlockInfoPart.builder()
                        .magic(0x9bc7a987L)
                        .version(cs.loadUint(32).longValue())
                        .notMaster(cs.loadBit())
                        .afterMerge(cs.loadBit())
                        .beforeSplit(cs.loadBit())
                        .afterSplit(cs.loadBit())
                        .wantSplit(cs.loadBit())
                        .wantMerge(cs.loadBit())
                        .keyBlock(cs.loadBit())
                        .vertSeqnoIncr(cs.loadBit())
                        .flags(cs.loadUint(8).longValue())
                        .seqno(cs.loadUint(32).longValue())
                        .vertSeqno(cs.loadUint(32).longValue())
                        .shard((ShardIdent) Tlb.load(ShardIdent.class, cs))
                        .genuTime(cs.loadUint(32).longValue())
                        .startLt(cs.loadUint(64))
                        .endLt(cs.loadUint(64))
                        .genValidatorListHashShort(cs.loadUint(32).longValue())
                        .genCatchainSeqno(cs.loadUint(32).longValue())
                        .minRefMcSeqno(cs.loadUint(32).longValue())
                        .prevKeyBlockSeqno(cs.loadUint(32).longValue())
                        .build();
            case "BlockHandle":
                return BlockHandle.builder()
                        .offset(cs.loadUint(64))
                        .size(cs.loadUint(64))
                        .build();
            case "BlockExtra":
                assert cs.loadUint(32).longValue() == 0x4a33f6fdL : "BlockExtra: magic not equal to 0x4a33f6fd";
                return BlockExtra.builder()
                        .magic(0x4a33f6fdL)
                        .inMsgDesc(cs.loadRef())
                        .outMsgDesc(cs.loadRef())
                        .shardAccountBlocks(cs.loadRef())
                        .randSeed(cs.loadBytes(256))
                        .createdBy(cs.loadBytes(256))
                        .custom(isNull(cs.preloadMaybeRefX()) ? null : (McBlockExtra) Tlb.load(McBlockExtra.class, CellSlice.beginParse(cs.loadRef())))
//                        .custom((McBlockExtra) Tlb.load(McBlockExtra.class, cs))
                        .build();
            case "Block":
                assert cs.loadUint(32).longValue() == 0x11ef55aaL : "Block: magic not equal to 0x11ef55aa";
                return Block.builder()
                        .magic(0x11ef55aaL)
                        .globalId(cs.loadInt(32).intValue())
                        .blockInfo((BlockHeader) Tlb.load(BlockHeader.class, CellSlice.beginParse(cs.loadRef())))
                        .valueFlow(cs.loadRef())
                        .stateUpdate((StateUpdate) Tlb.load(StateUpdate.class, CellSlice.beginParse(cs.loadRef())))
                        .extra((BlockExtra) Tlb.load(BlockExtra.class, CellSlice.beginParse(cs.loadRef())))
                        .build();
            case "AccountBlock":
                cs.skipBits(32); //magic
                return AccountBlock.builder()
                        .magic(0x5)
                        .addr(cs.loadBytes(256))
                        .transactions(cs.loadDict(64, k -> k.readInt(64), v -> v))
                        .stateUpdate(cs.loadRef())
                        .build();


            case "BlockHeader":
                BlockInfoPart infoPart = (BlockInfoPart) Tlb.load(BlockInfoPart.class, cs);
                GlobalVersion globalVersion = ((infoPart.getFlags() & 0x0L) == 1) ? (GlobalVersion) Tlb.load(GlobalVersion.class, cs) : null;
                ExtBlkRef masterRef = infoPart.isNotMaster() ? (ExtBlkRef) Tlb.load(ExtBlkRef.class, CellSlice.beginParse(cs.loadRef())) : null;
                BlkPrevInfo prevRef = loadBlkPrevInfo(CellSlice.beginParse(cs.loadRef()), infoPart.isAfterMerge());
                BlkPrevInfo prevVertRef = infoPart.isVertSeqnoIncr() ? loadBlkPrevInfo(CellSlice.beginParse(cs.loadRef()), false) : null;

                return BlockHeader.builder()
                        .blockInfoPart(infoPart)
                        .genSoftware(globalVersion)
                        .masterRef(masterRef)
                        .prevRef(prevRef)
                        .prevVertRef(prevVertRef)
                        .build();
            case "ValidatorSet":
                return ValidatorSet.builder().build();
            case "ValidatorSetExt":
                return ValidatorSetExt.builder().build();
            case "Validator":
                return Validator.builder().build();
            case "ValidatorAddr":
                return ValidatorAddr.builder().build();
            case "SigPubKeyED25519":
                return SigPubKeyED25519.builder().build();

            case "Message":
                return Message.builder().build();
            case "MessagesList":
                return MessagesList.builder().build();
            case "InternalMessage":
                return InternalMessage.builder().build();
            case "ExternalMessage":
                return ExternalMessage.builder().build();
            case "ExternalMessageOut":
                return ExternalMessageOut.builder().build();
        }

        throw new Error("Unknown TLB type: " + c.getSimpleName());
    }

    public static BlkPrevInfo loadBlkPrevInfo(CellSlice cs, boolean afterMerge) {
        BlkPrevInfo blkPrevInfo = BlkPrevInfo.builder().build();
        if (!afterMerge) {
            ExtBlkRef blkRef = (ExtBlkRef) Tlb.load(ExtBlkRef.class, cs);
            blkPrevInfo.setPrev1(blkRef);
            return blkPrevInfo;
        }

        ExtBlkRef blkRef1 = (ExtBlkRef) Tlb.load(ExtBlkRef.class, CellSlice.beginParse(cs.loadRef()));
        ExtBlkRef blkRef2 = (ExtBlkRef) Tlb.load(ExtBlkRef.class, CellSlice.beginParse(cs.loadRef()));
        blkPrevInfo.setPrev1(blkRef1);
        blkPrevInfo.setPrev2(blkRef2);
        return blkPrevInfo;
    }
}
