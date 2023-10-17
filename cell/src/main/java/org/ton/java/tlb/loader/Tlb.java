package org.ton.java.tlb.loader;

import org.ton.java.address.Address;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.CellSlice;
import org.ton.java.cell.TonHashMapE;
import org.ton.java.tlb.types.*;
import org.ton.java.utils.Utils;

import java.math.BigInteger;
import java.util.Map;

import static java.util.Objects.isNull;

public class Tlb {

    public static Object load(Class clazz, Cell c) {
        if (isNull(c)) {
            return null;
        } else {
            return load(clazz, CellSlice.beginParse(c), false);
        }
    }

    public static Object load(Class clazz, Cell c, boolean skipMagic) {
        if (isNull(c)) {
            return null;
        } else {
            return load(clazz, CellSlice.beginParse(c), skipMagic);
        }
    }

    public static Object load(Class c, CellSlice cs) {
        return load(c, cs, false);
    }

    public static Object load(Class c, CellSlice cs, boolean skipMagic) {

        switch (c.getSimpleName()) {
            case "BlockIdExt":
                return BlockIdExt.builder()
                        .workchain(cs.loadUint(32).longValue())
                        .shard(cs.loadUint(64).longValue())
                        .seqno(cs.loadUint(32).longValue())
                        .rootHash(cs.loadBytes(256))
                        .fileHash(cs.loadBytes(256))
                        .build();
            case "TickTock":
                return TickTock.builder()
                        .tick(cs.loadBit())
                        .tock(cs.loadBit())
                        .build();
            case "StateInit":
                return StateInit.builder()
                        .depth(cs.loadBit() ? cs.loadUint(5) : BigInteger.ZERO)
                        .tickTock(cs.loadBit() ? (TickTock) cs.loadTlb(TickTock.class) : null)
                        .code(cs.loadMaybeRefX())
                        .data(cs.loadMaybeRefX())
                        .lib(cs.loadDictE(256, k -> k.readInt(256), v -> v))
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
                    StateInit stateInit = (StateInit) cs.loadTlb(StateInit.class, skipMagic);
                    accountStorage.setStateInit(stateInit);
                } else {
                    boolean isStatusFrozen = cs.loadBit();
                    if (isStatusFrozen) {
                        accountStorage.setAccountStatus("FROZEN");
                        if (cs.getRestBits() != 0) {
                            int[] stateHash = cs.loadBytes(256);
                            accountStorage.setStateHash(stateHash);
                        }
                    } else {
                        accountStorage.setAccountStatus("UNINIT");
                    }
                }
                accountStorage.setLastTransactionLt(lastTransaction);
                accountStorage.setBalance(coins);
                return accountStorage;
            case "StorageInfo":
                StorageUsed storageUsed = (StorageUsed) cs.loadTlb(StorageUsed.class, skipMagic);
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
                StorageInfo info = (StorageInfo) cs.loadTlb(StorageInfo.class);
                AccountStorage storage = (AccountStorage) cs.loadTlb(AccountStorage.class);

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
                    case 0b11 -> {
                        return "NON_EXIST";
                    }
                    case 0b10 -> {
                        return "ACTIVE";
                    }
                    case 0b01 -> {
                        return "FROZEN";
                    }
                    case 0b00 -> {
                        return "UNINIT";
                    }
                }
            case "StateUpdate":
//                long magic = cs.loadUint(2).longValue();
                return StateUpdate.builder()
//                        .oldOne((ShardState) Tlb.load(ShardState.class, CellSlice.beginParse(cs.loadRef()), skipMagic))
                        .oldOne(cs.loadRef())
                        .newOne(cs.loadRef())
                        .build();
            case "ConfigParams":
                return ConfigParams.builder()
                        .configAddr(Address.of((byte) 0x11, 255, cs.loadBits(256).toByteArray()))
                        .config(CellSlice.beginParse(cs.loadRef()).loadDict(32, k -> k.readUint(32), v -> v))
                        .build();
            case "ShardStateUnsplit":
                return ShardStateUnsplit.builder()
                        .magic(cs.loadUint(32).longValue())
                        .globalId(cs.loadUint(32).intValue())
                        .shardIdent((ShardIdent) cs.loadTlb(ShardIdent.class, skipMagic))
                        .seqno(cs.loadUint(32).longValue())
                        .vertSeqno(cs.loadUint(32).longValue())
                        .genUTime(cs.loadUint(32).longValue())
                        .genLT(cs.loadUint(64))
                        .minRefMCSeqno(cs.loadUint(32).longValue())
                        .outMsgQueueInfo(cs.loadRef())
                        .beforeSplit(cs.loadBit())
                        .accounts(CellSlice.beginParse(cs.loadRef()).loadDictE(256, k -> k.readInt(256), v -> v))
                        .stats(cs.loadRef())
                        .custom(isNull(cs.preloadMaybeRefX()) ? null : (McStateExtra) Tlb.load(McStateExtra.class, cs.loadRef(), skipMagic))
                        .build();
            case "ShardIdent":
                if (!skipMagic) {
                    long magic = cs.loadUint(2).longValue();
                    assert (magic == 0L) : "ShardIdent: magic not equal to 0x0, found " + Long.toHexString(magic);
                }
                return ShardIdent.builder()
                        .magic(0L)
                        .prefixBits(cs.loadUint(6).longValue())
                        .workchain(cs.loadUint(32).longValue())
                        .shardPrefix(cs.loadUint(64))
                        .build();
            case "ShardDesc": // todo
                return ShardDesc.builder().build();
            case "ShardState":
                long tag = cs.preloadUint(32).longValue();
                if (tag == 0x5f327da5L) {
                    ShardStateUnsplit left, right;
                    left = (ShardStateUnsplit) Tlb.load(ShardStateUnsplit.class, cs.loadRef(), skipMagic);
                    right = (ShardStateUnsplit) Tlb.load(ShardStateUnsplit.class, cs.loadRef(), skipMagic);
                    return ShardState.builder()
                            .left(left)
                            .right(right)
                            .build();
                } else if (tag == 0x9023afe2L) {
                    return ShardState.builder()
                            .left((ShardStateUnsplit) cs.loadTlb(ShardStateUnsplit.class, skipMagic))
                            .build();
                } else {
                    throw new Error("unknown shardstate magic, found " + Long.toHexString(tag));
                }
            case "McStateExtra":
                if (isNull(cs)) {
                    return null;
                }
                if (!skipMagic) {
                    long magic = cs.loadUint(16).longValue();
                    assert (magic == 0xcc26L) : "McStateExtra: magic not equal to 0xcc26, found " + Long.toHexString(magic);
                }
                return McStateExtra.builder()
                        .magic(0xcc26L)
                        .shardHashes(cs.loadDictE(32, k -> k.readInt(32), v -> v))
                        .configParams((ConfigParams) cs.loadTlb(ConfigParams.class))
                        .info(cs.loadRef())
                        .globalBalance((CurrencyCollection) cs.loadTlb(CurrencyCollection.class))
                        .build();
            case "McBlockExtra":
                if (!skipMagic) {
                    long magic = cs.loadUint(16).longValue();
                    assert (magic == 0xcca5L) : "McBlockExtra: magic not equal to 0xcca5, found " + Long.toHexString(magic);
                }
                return McBlockExtra.builder()
                        .magic(0xcca5L)
                        .keyBlock(cs.loadBit())
                        .shardHashes(cs.loadDict(32, k -> k.readInt(32), v -> v))
                        .shardFees(cs.loadDict(92, k -> k.readInt(92), v -> v))
                        .build();
            case "CurrencyCollection":
                return CurrencyCollection.builder()
                        .coins(cs.loadCoins())
                        .extraCurrencies(cs.loadDictE(32, k -> k.readInt(32), v -> v))
                        .build();
            case "GlobalVersion":
                if (!skipMagic) {
                    long magic = cs.loadUint(8).longValue();
                    assert (magic == 0xc4L) : "GlobalVersion: magic not equal to 0xc4, found " + Long.toHexString(magic);
                }
                return GlobalVersion.builder()
                        .magic(0xc4L)
                        .version(cs.loadUint(32).longValue())
                        .capabilities(cs.loadUint(64))
                        .build();
            case "ExtBlkRef":
                return ExtBlkRef.builder()
                        .endLt(cs.loadUint(64))
                        .seqno(cs.loadUint(32).intValue())
                        .rootHash(cs.loadUint(256))
                        .fileHash(cs.loadUint(256))
                        .build();
            case "ImportFees":
                return ImportFees.builder()
                        .feesCollected(cs.loadCoins())
                        .valueImported((CurrencyCollection) cs.loadTlb(CurrencyCollection.class))
                        .build();
            case "IntermediateAddress":
                int intermFlag = cs.loadUint(2).intValue();
                switch (intermFlag) {
                    case 0b00 -> {
                        return IntermediateAddressRegular.builder()
                                .use_dest_bits(cs.loadUint(7).intValue()) // todo test if 7 bits
                                .build();
                    }
                    case 0b10 -> {
                        return IntermediateAddressSimple.builder()
                                .workchainId(cs.loadUint(8).intValue())
                                .addrPfx(cs.loadUint(64))
                                .build();
                    }
                    case 0b11 -> {
                        return IntermediateAddressSimple.builder()
                                .workchainId(cs.loadUint(32).intValue())
                                .addrPfx(cs.loadUint(64))
                                .build();
                    }
                }
                throw new Error("unknown interm_addr flag, found " + Integer.toBinaryString(intermFlag));
            case "MsgEnvelope":
                if (!skipMagic) {
                    long magic = cs.loadUint(32).longValue();
                    assert (magic == 4) : "MsgEnvelope: magic not equal to 4, found " + Long.toHexString(magic);
                }
                return MsgEnvelope.builder()
                        .currAddr((IntermediateAddress) cs.loadTlb(IntermediateAddress.class))
                        .nextAddr((IntermediateAddress) cs.loadTlb(IntermediateAddress.class))
                        .fwdFeeRemaining(cs.loadCoins())
                        .msg((Message) cs.loadTlb(Message.class))
                        .build();
            case "InMsg":
                int inMsgFlag = cs.loadUint(3).intValue();
                switch (inMsgFlag) {
                    case 0b000 -> {
                        return InMsgImportExt.builder()
                                .msg((Message) cs.loadTlb(Message.class, skipMagic))
                                .transaction((Transaction) cs.loadTlb(Transaction.class, skipMagic))
                                .build();
                    }
                    case 0b010 -> {
                        return InMsgImportIhr.builder()
                                .msg((Message) cs.loadTlb(Message.class, skipMagic))
                                .transaction((Transaction) cs.loadTlb(Transaction.class, skipMagic))
                                .ihrFee(cs.loadCoins())
                                .proofCreated(cs.loadRef())
                                .build();
                    }
                    case 0b011 -> {
                        return InMsgImportImm.builder()
                                .inMsg((MsgEnvelope) Tlb.load(MsgEnvelope.class, cs.loadRef(), skipMagic))
                                .transaction((Transaction) cs.loadTlb(Transaction.class, skipMagic))
                                .fwdFee(cs.loadCoins())
                                .build();
                    }
                    case 0b100 -> {
                        return InMsgImportFin.builder()
                                .inMsg((MsgEnvelope) Tlb.load(MsgEnvelope.class, cs.loadRef(), skipMagic))
                                .transaction((Transaction) cs.loadTlb(Transaction.class, skipMagic))
                                .fwdFee(cs.loadCoins())
                                .build();
                    }
                    case 0b101 -> {
                        return InMsgImportTr.builder()
                                .inMsg((MsgEnvelope) Tlb.load(MsgEnvelope.class, cs.loadRef(), skipMagic))
                                .outMsg((MsgEnvelope) Tlb.load(MsgEnvelope.class, cs.loadRef(), skipMagic))
                                .transitFee(cs.loadCoins())
                                .build();
                    }
                    case 0b110 -> {
                        return InMsgDiscardFin.builder()
                                .inMsg((MsgEnvelope) Tlb.load(MsgEnvelope.class, cs.loadRef(), skipMagic))
                                .transactionId(cs.loadUint(64))
                                .fwdFee(cs.loadCoins())
                                .build();
                    }
                    case 0b111 -> {
                        return InMsgDiscardTr.builder()
                                .inMsg((MsgEnvelope) Tlb.load(MsgEnvelope.class, cs.loadRef(), skipMagic))
                                .transactionId(cs.loadUint(64))
                                .fwdFee(cs.loadCoins())
                                .proofDelivered(cs.loadRef())
                                .build();
                    }
                }
                throw new Error("unknown in_msg flag, found " + Long.toBinaryString(inMsgFlag));

            case "InMsgDescr":
                TonHashMapE dictInMsg = cs.loadDictE(256, k -> k.readInt(256), v -> Tlb.load(InMsg.class, v, skipMagic));
                for (Map.Entry<Object, Object> entry : dictInMsg.elements.entrySet()) {
                    System.out.println("key " + entry.getKey() + " value " + entry.getValue());
                }
                return InMsgDescr.builder()
                        .inMsg(dictInMsg)
                        .feesCollected(cs.loadCoins())
                        .valueImported((CurrencyCollection) cs.loadTlb(CurrencyCollection.class, skipMagic))
                        .build();
            case "OutMsgDescr":
                TonHashMapE dictOutMsg = cs.loadDictE(256, k -> k.readInt(256), v -> Tlb.load(OutMsg.class, v, skipMagic));
                for (Map.Entry<Object, Object> entry : dictOutMsg.elements.entrySet()) {
                    System.out.println("key " + entry.getKey() + " value " + entry.getValue());
                }
                return OutMsgDescr.builder()
                        .outMsg(dictOutMsg)
                        .currencyCollection((CurrencyCollection) cs.loadTlb(CurrencyCollection.class, skipMagic))
                        .build();
            case "BlockInfo":
                if (!skipMagic) {
                    long magic = cs.loadUint(32).longValue();
                    assert (magic == 0x9bc7a987L) : "BlockInfo: magic not equal to 0x9bc7a987, found " + Long.toHexString(magic);
                }
                BlockInfo blockInfo = BlockInfo.builder()
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
                        .shard((ShardIdent) cs.loadTlb(ShardIdent.class, skipMagic))
                        .genuTime(cs.loadUint(32).longValue())
                        .startLt(cs.loadUint(64))
                        .endLt(cs.loadUint(64))
                        .genValidatorListHashShort(cs.loadUint(32).longValue())
                        .genCatchainSeqno(cs.loadUint(32).longValue())
                        .minRefMcSeqno(cs.loadUint(32).longValue())
                        .prevKeyBlockSeqno(cs.loadUint(32).longValue())
                        .build();
                blockInfo.setGlobalVersion(((blockInfo.getFlags() & 0x1L) == 0x1L) ? (GlobalVersion) cs.loadTlb(GlobalVersion.class, skipMagic) : null);
                blockInfo.setMasterRef(blockInfo.isNotMaster() ? (ExtBlkRef) Tlb.load(ExtBlkRef.class, cs.loadRef(), skipMagic) : null);
                blockInfo.setPrefRef(loadBlkPrevInfo(CellSlice.beginParse(cs.loadRef()), blockInfo.isAfterMerge()));
                blockInfo.setPrefVertRef(blockInfo.isVertSeqnoIncr() ? loadBlkPrevInfo(CellSlice.beginParse(cs.loadRef()), blockInfo.isAfterMerge()) : null);
                return blockInfo;
            case "BlockHandle":
                return BlockHandle.builder()
                        .offset(cs.loadUint(64))
                        .size(cs.loadUint(64))
                        .build();
            case "BlockExtra":
                if (!skipMagic) {
                    long magic = cs.loadUint(32).longValue();
                    assert (magic == 0x4a33f6fdL) : "Block: magic not equal to 0x4a33f6fdL, found " + Long.toHexString(magic);
                }
                BlockExtra blockExtra = BlockExtra.builder()
                        .inMsgDesc((InMsgDescr) Tlb.load(InMsgDescr.class, cs.loadRef(), skipMagic))
                        .outMsgDesc((OutMsgDescr) Tlb.load(OutMsgDescr.class, cs.loadRef(), skipMagic))
                        .shardAccountBlocks(cs.loadRef())
                        .randSeed(cs.loadUint(256))
                        .createdBy(cs.loadUint(256))
                        //      .custom(isNull(cs.preloadMaybeRefX()) ? null : (McBlockExtra) Tlb.load(McBlockExtra.class, CellSlice.beginParse(cs.loadRef()), skipMagic))
                        .build();
                return blockExtra;
            case "Block":
                if (!skipMagic) {
                    long magic = cs.loadUint(32).longValue();
                    assert (magic == 0x11ef55aaL) : "Block: magic not equal to 0x11ef55aa, found " + Long.toHexString(magic);
                }
                return Block.builder()
                        .magic(0x11ef55aaL)
                        .globalId(cs.loadInt(32).intValue())
                        .blockInfo((BlockInfo) Tlb.load(BlockInfo.class, cs.loadRef(), skipMagic))
                        .valueFlow((ValueFlow) Tlb.load(ValueFlow.class, cs.loadRef(), skipMagic))
                        .stateUpdate((StateUpdate) Tlb.load(StateUpdate.class, cs.loadRef(), skipMagic))
                        .extra((BlockExtra) Tlb.load(BlockExtra.class, cs.loadRef(), skipMagic))//  todo test testLoadBlockNotMaster
                        .build();
            case "AccountBlock":
                if (!skipMagic) {
                    long magic = cs.loadUint(32).longValue();
                    assert (magic == 0x5L) : "AccountBlock: magic not equal to 0x5, found " + Long.toHexString(magic);
                }
                return AccountBlock.builder()
                        .magic(0x5)
                        .addr(cs.loadBytes(256))
                        .transactions(cs.loadDictE(64, k -> k.readInt(64), v -> v))
                        .stateUpdate(cs.loadRef())
                        .build();
            case "ValueFlow":
                if (!skipMagic) {
                    long magic = cs.loadUint(32).longValue();
                    assert (magic == 0xb8e48dfbL) : "ValueFlow: magic not equal to 0xb8e48dfb, found " + Long.toHexString(magic);
                }
                Cell c1 = cs.loadRef();
                Cell c2 = cs.loadRef();
                CurrencyCollection fromPrevBlk = (CurrencyCollection) Tlb.load(CurrencyCollection.class, c1, skipMagic);
                CurrencyCollection toNextBlk = (CurrencyCollection) Tlb.load(CurrencyCollection.class, c1, skipMagic);
                CurrencyCollection imported = (CurrencyCollection) Tlb.load(CurrencyCollection.class, c1, skipMagic);
                CurrencyCollection exported = (CurrencyCollection) Tlb.load(CurrencyCollection.class, c1, skipMagic);
                CurrencyCollection feesCollected = (CurrencyCollection) Tlb.load(CurrencyCollection.class, cs, skipMagic);
                CurrencyCollection feesImported = (CurrencyCollection) Tlb.load(CurrencyCollection.class, c2, skipMagic);
                CurrencyCollection recovered = (CurrencyCollection) Tlb.load(CurrencyCollection.class, c2, skipMagic);
                CurrencyCollection created = (CurrencyCollection) Tlb.load(CurrencyCollection.class, c2, skipMagic);
                CurrencyCollection minted = (CurrencyCollection) Tlb.load(CurrencyCollection.class, c2, skipMagic);

                return ValueFlow.builder()
                        .magic(0xb8e48dfbL)
                        .fromPrevBlk(fromPrevBlk)
                        .toNextBlk(toNextBlk)
                        .imported(imported)
                        .exported(exported)
                        .feesCollected(feesCollected)
                        .feesImported(feesImported)
                        .recovered(recovered)
                        .created(created)
                        .minted(minted)
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
            case "MsgAddress":
                MsgAddressExt extMsgAddr = null;
                MsgAddressInt intMsgAddr = null;
                int flagMsg = cs.loadUint(2).intValue();
                switch (flagMsg) {
                    case 0b00 -> {
                        extMsgAddr = MsgAddressExt.builder().build();
                    }
                    case 0b01 -> {
                        int len = cs.loadUint(9).intValue();
                        BigInteger externalAddress = cs.loadUint(len);
                        extMsgAddr = MsgAddressExt.builder()
                                .len(len)
                                .externalAddress(externalAddress)
                                .build();
                    }
                    case 0b10 -> {
                        Anycast anycast = null;
                        if (cs.loadBit()) {
                            anycast = (Anycast) cs.loadTlb(Anycast.class, skipMagic);
                        }
                        intMsgAddr = MsgAddressInt.builder()
                                .anycast(anycast)
                                .workchainId(cs.loadUint(8).intValue())
                                .address(cs.loadUint(256))
                                .build();
                    }
                    case 0b11 -> {
                        Anycast anycast = null;
                        if (cs.loadBit()) {
                            anycast = (Anycast) cs.loadTlb(Anycast.class, skipMagic);
                        }
                        int addrLen = cs.loadUint(9).intValue();
                        intMsgAddr = MsgAddressInt.builder()
                                .anycast(anycast)
                                .addrLen(addrLen)
                                .workchainId(cs.loadUint(32).intValue())
                                .address(cs.loadUint(addrLen))
                                .build();
                    }
                }
                return MsgAddress.builder()
                        .magic(flagMsg)
                        .msgAddressExt(extMsgAddr)
                        .msgAddressInt(intMsgAddr)
                        .build();
            case "Message": {
                if (isNull(cs)) {
                    return Message.builder().build();
                }
                boolean isExternal = cs.preloadBit();
                if (!isExternal) {
                    InternalMessage internalMessage = (InternalMessage) cs.loadTlb(InternalMessage.class, skipMagic);

                    return Message.builder()
                            .msgType("INTERNAL")
                            .msg(AnyMessage.builder()
                                    .payload(internalMessage.getBody())
//                                    .destAddr(internalMessage.getDstAddr()) //todo
//                                    .senderAddr(internalMessage.getSrcAddr())
                                    .build())
                            .build();
                } else {
                    boolean isOut = cs.preloadBitAt(2);
                    if (isOut) {
                        ExternalMessageOut externalMessageOut = (ExternalMessageOut) cs.loadTlb(ExternalMessageOut.class, skipMagic);
                        return Message.builder()
                                .msgType("EXTERNAL_OUT")
                                .msg(AnyMessage.builder()
                                        .payload(externalMessageOut.getBody())
//                                        .destAddr(externalMessageOut.getDstAddr())
//                                        .senderAddr(externalMessageOut.getSrcAddr())
                                        .build())
                                .build();
                    } else {
                        ExternalMessage externalMessage = (ExternalMessage) cs.loadTlb(ExternalMessage.class, skipMagic);
                        return Message.builder()
                                .msgType("EXTERNAL_IN")
                                .msg(AnyMessage.builder()
                                        .payload(externalMessage.getBody())
//                                        .destAddr(externalMessage.getDstAddr())
//                                        .senderAddr(externalMessage.getSrcAddr())
                                        .build())
                                .build();
                    }
                }
                //throw new Error("Unknown msg type ");
            }
            case "MessagesList":
                if (isNull(cs)) {
                    return MessagesList.builder().build();
                }
                return MessagesList.builder()
                        .list(cs.loadDictE(15, k -> k.readInt(15), v -> v))
                        .build();
            case "ComputeSkipReason": {
                int skipReasonFlag = cs.loadUint(2).intValue();

                switch (skipReasonFlag) {
                    case 0b00 -> {
                        return ComputeSkipReason.builder().type("NO_STATE").build();
                    }
                    case 0b01 -> {
                        return ComputeSkipReason.builder().type("BAD_STATE").build();
                    }
                    case 0b10 -> {
                        return ComputeSkipReason.builder().type("NO_GAS").build();
                    }
                    case 0b11 -> {
                        boolean isNotSuspended = cs.loadBit();
                        if (!isNotSuspended) {
                            return ComputeSkipReason.builder().type("SUSPENDED").build();
                        }
                    }
                }
                throw new Error("unknown compute skip reason, found " + Integer.toBinaryString(skipReasonFlag));
            }
            case "ComputePhase": {
                boolean isNotSkipped = cs.loadBit();
                if (isNotSkipped) {
                    ComputePhaseVM phase = (ComputePhaseVM) cs.loadTlb(ComputePhaseVM.class, true); // todo
                    return ComputePhase.builder().phase(phase).build();
                }
                ComputePhaseSkipped phase = (ComputePhaseSkipped) cs.loadTlb(ComputePhaseSkipped.class, true); // todo
                return ComputePhase.builder().phase(phase).build();
            }
            case "BouncePhaseNegFounds": {
                if (!skipMagic) {
                    int magic = cs.loadUint(1).intValue();
                    assert (magic == 0b0) : "BouncePhaseNegFounds: magic not equal to 0b0, found " + Long.toHexString(magic);
                }
                return BouncePhaseNegFounds.builder().build();
            }
            case "BouncePhaseNoFounds": {
                if (!skipMagic) {
                    int magic = cs.loadUint(2).intValue();
                    assert (magic == 0b01) : "BouncePhaseNoFounds: magic not equal to 0b01, found " + Long.toHexString(magic);
                }
                return BouncePhaseNoFounds.builder()
                        .msgSize((StorageUsedShort) cs.loadTlb(StorageUsedShort.class))
                        .reqFwdFees(cs.loadCoins())
                        .build();
            }
            case "BouncePhaseok": {
                if (!skipMagic) {
                    int magic = cs.loadUint(1).intValue();
                    assert (magic == 0b1) : "BouncePhaseok: magic not equal to 0b1, found " + Long.toHexString(magic);
                }
                return BouncePhaseok.builder()
                        .magic(0b1)
                        .msgSize((StorageUsedShort) cs.loadTlb(StorageUsedShort.class))
                        .msgFees(cs.loadCoins())
                        .fwdFees(cs.loadCoins())
                        .build();
            }
            case "BouncePhase": {
                boolean isOk = cs.loadBit();
                if (isOk) {
                    BouncePhaseok phase = (BouncePhaseok) cs.loadTlb(BouncePhaseok.class, true);
                    return BouncePhase.builder().phase(phase).build();
                }
                boolean isNoFunds = cs.loadBit();
                if (isNoFunds) {
                    BouncePhaseNoFounds phase = (BouncePhaseNoFounds) cs.loadTlb(BouncePhaseNoFounds.class, true);
                    return BouncePhase.builder().phase(phase).build();
                }
                BouncePhaseNegFounds phase = (BouncePhaseNegFounds) cs.loadTlb(BouncePhaseNegFounds.class, true);
                return BouncePhase.builder().phase(phase).build();
            }
            case "StoragePhase": {
                return StoragePhase.builder()
                        .storageFeesCollected(cs.loadCoins())
                        .storageFeesDue(cs.loadBit() ? cs.loadCoins() : null)
                        .statusChange((AccStatusChange) cs.loadTlb(AccStatusChange.class))
                        .build();
            }
            case "ActionPhase": {
                return ActionPhase.builder()
                        .success(cs.loadBit())
                        .valid(cs.loadBit())
                        .noFunds(cs.loadBit())
                        .totalFwdFees(cs.loadBit() ? cs.loadCoins() : null)
                        .totalActionFees(cs.loadBit() ? cs.loadCoins() : null)
                        .resultCode(cs.loadUint(32).longValue())
                        .resultCode(cs.loadBit() ? cs.loadUint(32).longValue() : 0)
                        .totalActions(cs.loadUint(16).longValue())
                        .specActions(cs.loadUint(16).longValue())
                        .skippedActions(cs.loadUint(16).longValue())
                        .messagesCreated(cs.loadUint(16).longValue())
                        .actionListHash(cs.loadBytes(256))
                        .totalMsgSize((StorageUsedShort) cs.loadTlb(StorageUsedShort.class))
                        .build();
            }

            case "StorageUsedShort": {
                return StorageUsedShort.builder()
                        .cells(cs.loadUint(7))
                        .bits(cs.loadUint(7))
                        .build();
            }
            case "ComputePhaseSkipped": {
                if (!skipMagic) {
                    int magic = cs.loadUint(1).intValue();
                    assert (magic == 0b0) : "ComputePhaseSkipped: magic not equal to 0b, found " + Long.toHexString(magic);
                }
                return ComputePhaseSkipped.builder()
                        .magic(0)
                        .reason((ComputeSkipReason) cs.loadTlb(ComputeSkipReason.class))
                        .build();
            }
            case "ComputePhaseVM": {
                if (!skipMagic) {
                    int magic = cs.loadUint(1).intValue();
                    assert (magic == 0b1) : "ComputePhaseVM: magic not equal to 0b1, found " + Long.toHexString(magic);
                }
                return ComputePhaseVM.builder()
                        .magic(1)
                        .success(cs.loadBit())
                        .msgStateUsed(cs.loadBit())
                        .accountActivated(cs.loadBit())
                        .gasFees(cs.loadCoins())
                        .details((ComputePhaseVMDetails) Tlb.load(ComputePhaseVMDetails.class, cs.loadRef()))
                        .build();
            }
            case "ComputePhaseVMDetails": {
                return ComputePhaseVMDetails.builder()
                        .gasUsed(cs.loadUint(7))
                        .gasLimit(cs.loadUint(7))
                        .gasCredit(cs.loadBit() ? cs.loadUint(3) : BigInteger.ZERO)
                        .mode(cs.loadUint(8).intValue())
                        .exitCode(cs.loadUint(32).longValue())
                        .exitArg(cs.loadBit() ? cs.loadUint(32).longValue() : 0L)
                        .vMSteps(cs.loadUint(32).longValue())
                        .vMInitStateHash(cs.loadUint(256))
                        .vMFinalStateHash(cs.loadUint(256))
                        .build();
            }
            case "CreditPhase": {
                return CreditPhase.builder()
                        .dueFeesCollected(cs.loadBit() ? cs.loadCoins() : BigInteger.ZERO)
                        .credit((CurrencyCollection) cs.loadTlb(CurrencyCollection.class))
                        .build();
            }
            case "SplitMergeInfo": {
                return SplitMergeInfo.builder()
                        .curShardPfxLen(cs.loadUint(6).intValue())
                        .accSplitDepth(cs.loadUint(6).intValue())
                        .thisAddr(cs.loadBytes(256))
                        .siblingAddr(cs.loadBytes(256))
                        .build();
            }
            case "Transaction": {
                if (!skipMagic) {
                    int magic = cs.loadUint(4).intValue();
                    assert (magic == 0b0111) : "Transaction: magic not equal to 0b0111, found " + Long.toHexString(magic);
                }

                Transaction tx = Transaction.builder()
                        .magic(0b0111)
                        .accountAddr(cs.loadBytes(256))
                        .lt(cs.loadUint(64))
                        .prevTxHash(cs.loadBytes(256))
                        .prevTxLT(cs.loadUint(64))
                        .now(cs.loadUint(32).longValue())
                        .outMsgCount(cs.loadUint(15).intValue())
                        .origStatus(cs.loadString(2))
                        .endStatus(cs.loadString(2))
                        .build();

                System.out.println("tx " + tx); //todo cleanup

                CellSlice io = CellSlice.beginParse(cs.loadRef());
                tx.setInOut(TransactionIO.builder()
                        .in((Message) Tlb.load(Message.class, io.loadMaybeRefX()))
                        .out((MessagesList) Tlb.load(MessagesList.class, io.loadRef()))
                        .build());

                for (Map.Entry<Object, Object> entry : tx.getInOut().getOut().getList().elements.entrySet()) {
                    System.out.println("key " + entry.getKey() + ", value " + ((Cell) entry.getValue()).print());
                    Message i = (Message) Tlb.load(Message.class, (Cell) entry.getValue());
                    System.out.println("i = " + i.toString());
                }

                tx.setTotalFees((CurrencyCollection) cs.loadTlb(CurrencyCollection.class));
                tx.setStateUpdate((HashUpdate) Tlb.load(HashUpdate.class, cs.loadRef()));
                tx.setDescription((TransactionDescription) Tlb.load(TransactionDescription.class, cs.loadRef()));

                return tx;
            }
            case "HashUpdate": {
                if (!skipMagic) {
                    int magic = cs.loadUint(8).intValue();
                    assert (magic == 0x72) : "HashUpdate: magic not equal to 0x72, found " + Long.toHexString(magic);
                }
                return HashUpdate.builder()
                        .magic(0x72)
                        .oldHash(cs.loadUint(256))
                        .newHash(cs.loadUint(256))
                        .build();
            }
            case "TransactionDescription": {
                int pfx = cs.loadUint(3).intValue();
                switch (pfx) {
                    case 0b000 -> {
                        boolean isStorage = cs.loadBit();
                        if (isStorage) {
                            TransactionDescriptionStorage desc = (TransactionDescriptionStorage) cs.loadTlb(TransactionDescriptionStorage.class, true);
                            return TransactionDescription.builder().description(desc).build();
                        }
                        TransactionDescriptionOrdinary descOrdinary = (TransactionDescriptionOrdinary) cs.loadTlb(TransactionDescriptionOrdinary.class, true);
                        return TransactionDescription.builder().description(descOrdinary).build();
                    }
                    case 0b001 -> {
                        TransactionDescriptionTickTock descTickTock = (TransactionDescriptionTickTock) cs.loadTlb(TransactionDescriptionTickTock.class, true);
                        return TransactionDescription.builder().description(descTickTock).build();
                    }
                    case 0b010 -> {
                        boolean isInstall = cs.loadBit();
                        if (isInstall) {
                            TransactionDescriptionSplitInstall descSplit = (TransactionDescriptionSplitInstall) cs.loadTlb(TransactionDescriptionSplitInstall.class, true);
                            return TransactionDescription.builder().description(descSplit).build();
                        }
                        TransactionDescriptionSplitPrepare descSplitPrepare = (TransactionDescriptionSplitPrepare) cs.loadTlb(TransactionDescriptionSplitPrepare.class, true);
                        return TransactionDescription.builder().description(descSplitPrepare).build();
                    }
                    case 0b011 -> {
                        boolean isInstall = cs.loadBit();
                        if (isInstall) {
                            TransactionDescriptionMergeInstall descMerge = (TransactionDescriptionMergeInstall) cs.loadTlb(TransactionDescriptionMergeInstall.class, true);
                            return TransactionDescription.builder().description(descMerge).build();
                        }
                        TransactionDescriptionMergePrepare descMergePrepare = (TransactionDescriptionMergePrepare) cs.loadTlb(TransactionDescriptionMergePrepare.class, true);
                        return TransactionDescription.builder().description(descMergePrepare).build();
                    }
                }
                throw new Error("unknown transaction description type (must be in range [0..3], found " + Integer.toBinaryString(pfx));
            }
            case "TransactionDescriptionStorage": {
                if (!skipMagic) {
                    int magic = cs.loadUint(4).intValue();
                    assert (magic == 0b0001) : "TransactionDescriptionStorage: magic not equal to 0b0001, found " + Long.toHexString(magic);
                }
                return TransactionDescriptionStorage.builder()
                        .magic(0b0001)
                        .storagePhase((StoragePhase) Tlb.load(StoragePhase.class, cs))
                        .build();
            }
            case "TransactionDescriptionOrdinary": {
                if (!skipMagic) {
                    int magic = cs.loadUint(4).intValue();
                    assert (magic == 0b0000) : "TransactionDescriptionOrdinary: magic not equal to 0b0000, found " + Long.toHexString(magic);
                }
                return TransactionDescriptionOrdinary.builder()
                        .magic(0b0000)
                        .creditFirst(cs.loadBit())
                        .storagePhase(cs.loadBit() ? (StoragePhase) cs.loadTlb(StoragePhase.class) : null)
                        .creditPhase(cs.loadBit() ? (CreditPhase) cs.loadTlb(CreditPhase.class) : null)
                        .computePhase((ComputePhase) cs.loadTlb(ComputePhase.class))
                        .actionPhase((ActionPhase) Tlb.load(ActionPhase.class, cs.loadMaybeRefX()))
                        .aborted(cs.loadBit())
                        .bouncePhase(cs.loadBit() ? (BouncePhase) cs.loadTlb(BouncePhase.class) : null)
                        .destroyed(cs.loadBit())
                        .build();


            }
            case "TransactionDescriptionTickTock": {
                if (!skipMagic) {
                    int magic = cs.loadUint(3).intValue();
                    assert (magic == 0b001) : "TransactionDescriptionTickTock: magic not equal to 0b001, found " + Long.toHexString(magic);
                }
                return TransactionDescriptionTickTock.builder()
                        .magic(0b001)
                        .isTock(cs.loadBit())
                        .storagePhase((StoragePhase) cs.loadTlb(StoragePhase.class))
                        .computePhase((ComputePhase) cs.loadTlb(ComputePhase.class))
                        .actionPhase((ActionPhase) Tlb.load(ActionPhase.class, cs.loadMaybeRefX()))
                        .aborted(cs.loadBit())
                        .destroyed(cs.loadBit())
                        .build();
            }
            case "TransactionDescriptionSplitInstall": {
                if (!skipMagic) {
                    int magic = cs.loadUint(4).intValue();
                    assert (magic == 0b0101) : "TransactionDescriptionSplitInstall: magic not equal to 0b0101, found " + Long.toHexString(magic);
                }
                return TransactionDescriptionSplitInstall.builder()
                        .magic(0b0101)
                        .splitInfo((SplitMergeInfo) cs.loadTlb(SplitMergeInfo.class))
                        .prepareTransaction((Transaction) Tlb.load(Transaction.class, cs.loadRef()))
                        .installed(cs.loadBit())
                        .build();
            }
            case "TransactionDescriptionSplitPrepare": {
                if (!skipMagic) {
                    int magic = cs.loadUint(4).intValue();
                    assert (magic == 0b0100) : "TransactionDescriptionSplitPrepare: magic not equal to 0b0100, found " + Long.toHexString(magic);
                }
                return TransactionDescriptionMergeInstall.builder()
                        .magic(0b0100)
                        .splitInfo((SplitMergeInfo) cs.loadTlb(SplitMergeInfo.class))
                        .storagePhase(cs.loadBit() ? (StoragePhase) cs.loadTlb(StoragePhase.class) : null)
                        .computePhase((ComputePhase) cs.loadTlb(ComputePhase.class))
                        .actionPhase((ActionPhase) Tlb.load(ActionPhase.class, cs.loadMaybeRefX()))
                        .aborted(cs.loadBit())
                        .destroyed(cs.loadBit())
                        .build();
            }
            case "TransactionDescriptionMergeInstall": {
                if (!skipMagic) {
                    int magic = cs.loadUint(4).intValue();
                    assert (magic == 0b0111) : "TransactionDescriptionMergeInstall: magic not equal to 0b0111, found " + Long.toHexString(magic);
                }
                return TransactionDescriptionMergeInstall.builder()
                        .magic(0b0111)
                        .splitInfo((SplitMergeInfo) cs.loadTlb(SplitMergeInfo.class))
                        .prepareTransaction((Transaction) Tlb.load(Transaction.class, cs.loadRef(), skipMagic))
                        .storagePhase(cs.loadBit() ? (StoragePhase) cs.loadTlb(StoragePhase.class) : null)
                        .creditPhase(cs.loadBit() ? (CreditPhase) cs.loadTlb(CreditPhase.class) : null)
                        .computePhase((ComputePhase) cs.loadTlb(ComputePhase.class))
                        .actionPhase((ActionPhase) Tlb.load(ActionPhase.class, cs.loadMaybeRefX()))
                        .aborted(cs.loadBit())
                        .destroyed(cs.loadBit())
                        .build();
            }
            case "TransactionDescriptionMergePrepare": {
                if (!skipMagic) {
                    int magic = cs.loadUint(4).intValue();
                    assert (magic == 0b0110) : "TransactionDescriptionMergePrepare: magic not equal to 0b0110, found " + Long.toHexString(magic);
                }
                return TransactionDescriptionMergePrepare.builder()
                        .magic(0b0110)
                        .splitInfo((SplitMergeInfo) cs.loadTlb(SplitMergeInfo.class)) // todo
                        .storagePhase((StoragePhase) cs.loadTlb(StoragePhase.class))// todo
                        .aborted(cs.loadBit())
                        .build();
            }

            case "AccStatusChange":
                boolean isChanged = cs.loadBit();
                if (isChanged) {
                    boolean isDeleted = cs.loadBit();
                    if (isDeleted) {
                        return AccStatusChange.builder().type("DELETED").build();
                    }
                    return AccStatusChange.builder().type("FROZEN").build();
                }
                return AccStatusChange.builder().type("UNCHANGED").build();
            case "InternalMessage":
                if (!skipMagic) {
                    boolean magic = cs.loadBit();
                    assert (!magic) : "InternalMessage: magic not equal to 0, found " + magic;
                }
                return InternalMessage.builder()
                        .magic(0L)
                        .iHRDisabled(cs.loadBit())
                        .bounce(cs.loadBit())
                        .bounced(cs.loadBit())
                        .srcAddr((MsgAddress) cs.loadTlb(MsgAddress.class))
                        .dstAddr((MsgAddress) cs.loadTlb(MsgAddress.class))
                        .value((CurrencyCollection) cs.loadTlb(CurrencyCollection.class))
                        .iHRFee(cs.loadCoins())
                        .fwdFee(cs.loadCoins())
                        .createdLt(cs.loadUint(64))
                        .createdAt(cs.loadUint(32).longValue())
                        .stateInit(cs.loadBit() ? (cs.loadBit() ? (StateInit) Tlb.load(StateInit.class, cs.loadRef()) : (StateInit) cs.loadTlb(StateInit.class)) : null) //review
                        .body(cs.loadBit() ? cs.loadRef() : CellBuilder.beginCell().storeBitString(cs.loadBits(cs.getRestBits()))) // todo
                        .build();
            case "ExternalMessage":
                if (!skipMagic) {
                    int magic = cs.loadUint(2).intValue();
                    assert (magic == 0b10) : "ExternalMessage: magic not equal to 0b10, found " + Long.toBinaryString(magic);
                }
                return ExternalMessage.builder()
                        .magic(2L)
                        .srcAddr((MsgAddress) cs.loadTlb(MsgAddress.class))
                        .dstAddr((MsgAddress) cs.loadTlb(MsgAddress.class))
                        .importFee(cs.loadCoins())
                        .stateInit(cs.loadBit() ? (cs.loadBit() ? (StateInit) Tlb.load(StateInit.class, cs.loadRef()) : (StateInit) cs.loadTlb(StateInit.class)) : null) //review
                        .body(cs.loadBit() ? cs.loadRef() : CellBuilder.beginCell().storeBitString(cs.loadBits(cs.getRestBits())))
                        .build();
            case "ExternalMessageOut":
                if (!skipMagic) {
                    int magic = cs.loadUint(2).intValue();
                    assert (magic == 0b11) : "ExternalMessageOut: magic not equal to 0b11, found " + Long.toBinaryString(magic);
                }
                return ExternalMessageOut.builder()
                        .magic(3L)
                        .srcAddr((MsgAddress) cs.loadTlb(MsgAddress.class))
                        .dstAddr((MsgAddress) cs.loadTlb(MsgAddress.class))
                        .createdLt(cs.loadUint(64))
                        .createdAt(cs.loadUint(32).longValue())
                        .stateInit((StateInit) cs.loadTlb(StateInit.class))
                        .body(cs.loadMaybeRefX())
                        .build();
            case "Text":
                int chunksNum = cs.loadUint(8).intValue();
                int firstSize = 0;
                int lengthOfChunk = 0;
                StringBuilder result = new StringBuilder();
                for (int i = 0; i < chunksNum; i++) {
                    lengthOfChunk = cs.loadUint(8).intValue();
                    if (i == 0) {
                        firstSize = lengthOfChunk;
                    }
                    int[] dataOfChunk = cs.loadBytes(lengthOfChunk * 8);
                    result.append(new String(Utils.unsignedBytesToSigned(dataOfChunk)));

                    if (i < chunksNum - 1) {
                        cs = CellSlice.beginParse(cs.loadRef());
                    }
                }
                return Text.builder()
                        .maxFirstChunkSize(firstSize)
                        .value(result.toString())
                        .build();
        }

        throw new Error("Unknown TLB type: " + c.getSimpleName());
    }

    private static BlkPrevInfo loadBlkPrevInfo(CellSlice cs, boolean afterMerge) {
        BlkPrevInfo blkPrevInfo = BlkPrevInfo.builder().build();
        if (!afterMerge) {
            ExtBlkRef blkRef = (ExtBlkRef) cs.loadTlb(ExtBlkRef.class);
            blkPrevInfo.setPrev1(blkRef);
            return blkPrevInfo;
        }

        ExtBlkRef blkRef1 = (ExtBlkRef) Tlb.load(ExtBlkRef.class, cs.loadRef());
        ExtBlkRef blkRef2 = (ExtBlkRef) Tlb.load(ExtBlkRef.class, cs.loadRef());
        blkPrevInfo.setPrev1(blkRef1);
        blkPrevInfo.setPrev2(blkRef2);
        return blkPrevInfo;
    }
}
