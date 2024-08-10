package org.ton.java.smartcontract.wallet;


import org.apache.commons.lang3.StringUtils;
import org.ton.java.address.Address;
import org.ton.java.cell.Cell;
import org.ton.java.smartcontract.types.WalletConfig;
import org.ton.java.smartcontract.wallet.v1.WalletV1R1;
import org.ton.java.tlb.types.Message;
import org.ton.java.tlb.types.MsgAddressIntStd;
import org.ton.java.tlb.types.StateInit;
import org.ton.java.tonlib.Tonlib;
import org.ton.java.tonlib.types.RawTransaction;
import org.ton.java.utils.Utils;

import java.math.BigInteger;
import java.util.List;

/**
 * Interface for all smart contract objects in ton4j.
 */
public interface Contract {

    Tonlib getTonlib();

    long getWorkchain();

    String getName();

    default Address getAddress() {
        return StateInit.builder()
                .code(createCodeCell())
                .data(createDataCell())
                .build().getAddress(getWorkchain());
    }

    default Address getAddress(byte workchain) {
        return getStateInit().getAddress(workchain);
    }

    default MsgAddressIntStd getAddressIntStd() {
        Address ownAddress = getStateInit().getAddress(getWorkchain());
        return MsgAddressIntStd.builder()
                .workchainId(ownAddress.wc)
                .address(ownAddress.toBigInteger())
                .build();
    }

    default MsgAddressIntStd getAddressIntStd(int workchain) {
        Address ownAddress = getStateInit().getAddress();
        return MsgAddressIntStd.builder()
                .workchainId((byte) workchain)
                .address(ownAddress.toBigInteger())
                .build();
    }

    /**
     * @return Cell containing contact code
     */
    Cell createCodeCell();

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
    default StateInit getStateInit() {
        return StateInit.builder()
                .code(createCodeCell())
                .data(createDataCell())
                .build();
    }

    default long getSeqno() {

        if (this instanceof WalletV1R1) {
            throw new Error("Wallet V1R1 does not have seqno method");
        }

        return getTonlib().getSeqno(getAddress());
    }

    default boolean isDeployed() {
        return StringUtils.isNotEmpty(getTonlib().getRawAccountState(getAddress()).getCode());
    }

    default void waitForDeployment(int timeoutSeconds) {
        System.out.println("waiting for deployment up to " + timeoutSeconds + " sec");
        int i = 0;
        do {
            if (++i * 2 >= timeoutSeconds) {
                throw new Error("Can't deploy contract within specified timeout.");
            }
            Utils.sleep(2);
        }
        while (!isDeployed());
    }

    default void waitForBalanceChange(int timeoutSeconds) {
        System.out.println("waiting for balance change up to " + timeoutSeconds + " sec");
        BigInteger initialBalance = getBalance();
        int i = 0;
        do {
            if (++i * 2 >= timeoutSeconds) {
                throw new Error("Balance was not changed within specified timeout.");
            }
            Utils.sleep(2);
        }
        while (initialBalance.equals(getBalance()));
    }

    default BigInteger getBalance() {
        return new BigInteger(getTonlib().getAccountState(getAddress()).getBalance());
    }

    default List<RawTransaction> getTransactions(int historyLimit) {
        return getTonlib().getAllRawTransactions(getAddress().toBounceable(), BigInteger.ZERO, null, historyLimit).getTransactions();
    }

    default List<RawTransaction> getTransactions() {
        return getTonlib().getAllRawTransactions(getAddress().toBounceable(), BigInteger.ZERO, null, 20).getTransactions();
    }

    default Message prepareDeployMsg() {
        throw new Error("not implemented");
    }

    default Message prepareExternalMsg(WalletConfig config) {
        throw new Error("not implemented");
    }
}