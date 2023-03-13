package org.ton.java.smartcontract.wallet;


import org.ton.java.address.Address;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.smartcontract.types.StateInit;
import org.ton.java.utils.Utils;

import java.math.BigInteger;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

/**
 * Interface for all smart contract objects in ton4j.
 */
public interface Contract {

    Options getOptions();

    Address getAddress();

    /**
     * @return Cell containing contact code
     */
    default Cell createCodeCell() {
        if (isNull(getOptions().code)) {
            throw new Error("Contract: options.code is not defined");
        }
        return getOptions().code;
    }

    /**
     * Method to override
     *
     * @return {Cell} cell contains contract data
     */

    Cell createDataCell();

    /**
     * Message StateInit consists of initial contract code, data and address in a blockchain
     *
     * @return StateInit
     */
    default StateInit createStateInit() {
        Cell codeCell = createCodeCell();
        Cell dataCell = createDataCell();
        Cell stateInit = createStateInit(codeCell, dataCell);

        byte[] stateInitHash = stateInit.hash();

        Address address = Address.of(getOptions().wc + ":" + Utils.bytesToHex(stateInitHash));
        return new StateInit(stateInit, address, codeCell, dataCell);
    }

    // split_depth:(Maybe (## 5))
    // special:(Maybe TickTock)
    // code:(Maybe ^Cell)
    // data:(Maybe ^Cell)
    // library:(Maybe ^Cell) = StateInit;

    /**
     * Message StateInit consists of initial contract code, data and address in a blockchain.
     * Argments library, splitDepth and ticktock in state init is not yet implemented.
     *
     * @param code       Cell
     * @param data       Cell
     * @param library    null
     * @param splitDepth null
     * @param ticktock   null
     * @return Cell
     */
    default Cell createStateInit(Cell code, Cell data, Cell library, Cell splitDepth, Cell ticktock) {

        if (nonNull(library)) {
            throw new Error("Library in state init is not implemented");
        }

        if (nonNull(splitDepth)) {
            throw new Error("Split depth in state init is not implemented");
        }

        if (nonNull(ticktock)) {
            throw new Error("Ticktock in state init is not implemented");
        }

        CellBuilder stateInit = CellBuilder.beginCell();

        stateInit.storeBits(new boolean[]{nonNull(splitDepth), nonNull(ticktock), nonNull(code), nonNull(data), nonNull(library)});

        if (nonNull(code)) {
            stateInit.storeRef(code);
        }
        if (nonNull(data)) {
            stateInit.storeRef(data);
        }
        if (nonNull(library)) {
            stateInit.storeRef(library);
        }

        return stateInit.endCell();
    }

    default Cell createStateInit(Cell code, Cell data) {
        return createStateInit(code, data, null, null, null);
    }

    // extra_currencies$_ dict:(HashmapE 32 (VarUInteger 32))
    // = ExtraCurrencyCollection;
    // currencies$_ grams:Grams other:ExtraCurrencyCollection
    // = CurrencyCollection;

    //int_msg_info$0 ihr_disabled:Bool bounce:Bool
    //src:MsgAddressInt dest:MsgAddressInt
    //value:CurrencyCollection ihr_fee:Grams fwd_fee:Grams
    //created_lt:uint64 created_at:uint32 = CommonMsgInfo;

    /**
     * @param dest               Address
     * @param gramValue          BigInteger, 0
     * @param ihrDisabled        boolean, true
     * @param bounce             boolean, null
     * @param bounced            boolean, false
     * @param src                Address, null
     * @param currencyCollection null,
     * @param ihrFees            number, 0
     * @param fwdFees            number, 0
     * @param createdLt          number, 0
     * @param createdAt          number, 0
     * @return Cell
     */
    static Cell createInternalMessageHeader(Address dest,
                                            BigInteger gramValue,
                                            boolean ihrDisabled,
                                            Boolean bounce,
                                            boolean bounced,
                                            Address src,
                                            byte[] currencyCollection,
                                            BigInteger ihrFees,
                                            BigInteger fwdFees,
                                            BigInteger createdLt,
                                            BigInteger createdAt) {

        CellBuilder message = CellBuilder.beginCell();
        message.storeBit(false);
        message.storeBit(ihrDisabled);

        if (nonNull(bounce)) {
            message.storeBit(bounce);
        } else {
            message.storeBit(dest.isBounceable);
        }
        message.storeBit(bounced);
        message.storeAddress(src);
        message.storeAddress(dest);
        message.storeCoins(gramValue);
        if (currencyCollection.length != 0) {
            throw new Error("Currency collections are not implemented yet");
        }
        message.storeBit(currencyCollection.length != 0);
        message.storeCoins(ihrFees);
        message.storeCoins(fwdFees);
        message.storeUint(createdLt, 64);
        message.storeUint(createdAt, 32);
        return message.endCell();
    }

    default Cell createInternalMessageHeader(String dest,
                                             BigInteger gramValue,
                                             boolean ihrDisabled,
                                             Boolean bounce,
                                             boolean bounced,
                                             Address src,
                                             byte[] currencyCollection,
                                             BigInteger ihrFees,
                                             BigInteger fwdFees,
                                             BigInteger createdLt,
                                             BigInteger createdAt) {
        return createInternalMessageHeader(
                Address.of(dest), gramValue, ihrDisabled, bounce, bounced,
                src, currencyCollection, ihrFees, fwdFees,
                createdLt, createdAt);
    }

    static Cell createInternalMessageHeader(Address dest, BigInteger toncoinValue) {
        return createInternalMessageHeader(
                dest,
                toncoinValue,
                true,
                null,
                false,
                null,
                new byte[0],
                BigInteger.ZERO,
                BigInteger.ZERO,
                BigInteger.ZERO,
                BigInteger.ZERO);
    }

    static Cell createInternalMessageHeader(String dest, BigInteger toncoinValue) {
        return createInternalMessageHeader(
                Address.of(dest),
                toncoinValue,
                true,
                null,
                false,
                null,
                null,
                BigInteger.ZERO,
                BigInteger.ZERO,
                BigInteger.ZERO,
                BigInteger.ZERO);
    }

    /**
     * Message header
     * ext_in_msg_info$10 src:MsgAddressExt dest:MsgAddressInt import_fee:Grams = CommonMsgInfo;
     *
     * @param dest      Address
     * @param src       Address
     * @param importFee BigInteger
     * @return Cell
     */
    static Cell createExternalMessageHeader(Address dest, Address src, BigInteger importFee) {
        CellBuilder message = CellBuilder.beginCell();
        message.storeUint(BigInteger.TWO, 2); //bit $10
        message.storeAddress(src);
        message.storeAddress(dest);
        message.storeCoins(importFee);
        return message;
    }

    /**
     * @param dest      String
     * @param src       Address
     * @param importFee number
     * @return Cell
     */
    static Cell createExternalMessageHeader(String dest, Address src, BigInteger importFee) {
        return createExternalMessageHeader(Address.of(dest), src, importFee);
    }

    /**
     * @param dest      String
     * @param src       String
     * @param importFee BigInteger
     * @return Cell
     */
    static Cell createExternalMessageHeader(String dest, String src, BigInteger importFee) {
        return createExternalMessageHeader(Address.of(dest), Address.of(src), importFee);
    }

    static Cell createExternalMessageHeader(Address dest) {
        return createExternalMessageHeader(dest, null, BigInteger.ZERO);
    }

    static Cell createExternalMessageHeader(String dest) {
        return createExternalMessageHeader(Address.of(dest), null, BigInteger.ZERO);
    }

    //tblkch.pdf, page 57

    /**
     * Create CommonMsgInfo contains header, stateInit, body
     *
     * @param header    Cell
     * @param stateInit Cell
     * @param body      Cell
     * @return Cell
     */
    static Cell createCommonMsgInfo(Cell header, Cell stateInit, Cell body) {
        CellBuilder commonMsgInfo = CellBuilder.beginCell();

        commonMsgInfo.writeCell(header);

        if (nonNull(stateInit)) {
            commonMsgInfo.storeBit(true);
            //-1:  need at least one bit for body
            if (commonMsgInfo.getFreeBits() - 1 >= stateInit.bits.getUsedBits()) {
                commonMsgInfo.storeBit(false);
                commonMsgInfo.writeCell(stateInit);
            } else {
                commonMsgInfo.storeBit(true);
                commonMsgInfo.storeRef(stateInit);
            }
        } else {
            commonMsgInfo.storeBit(false);
        }

        if (nonNull(body)) {
            if ((commonMsgInfo.getFreeBits() >= body.bits.getUsedBits()) && commonMsgInfo.getFreeRefs() >= body.getUsedRefs()) {
                commonMsgInfo.storeBit(false);
                commonMsgInfo.writeCell(body);
            } else {
                commonMsgInfo.storeBit(true);
                commonMsgInfo.storeRef(body);
            }
        } else {
            commonMsgInfo.storeBit(false);
        }
        return commonMsgInfo.endCell();
    }

    /**
     * Create CommonMsgInfo without body and stateInit
     *
     * @param header Cell
     * @return Cell
     */
    static Cell createCommonMsgInfo(Cell header) {
        return createCommonMsgInfo(header, null, null);
    }
}