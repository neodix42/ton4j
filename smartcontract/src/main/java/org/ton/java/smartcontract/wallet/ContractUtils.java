package org.ton.java.smartcontract.wallet;


import lombok.extern.java.Log;
import org.apache.commons.lang3.StringUtils;
import org.ton.java.address.Address;
import org.ton.java.smartcontract.token.ft.JettonMinter;
import org.ton.java.smartcontract.token.ft.JettonWallet;
import org.ton.java.tonlib.Tonlib;
import org.ton.java.utils.Utils;

import java.math.BigInteger;

@Log
public class ContractUtils {

    public static long getSeqno(Tonlib tonlib, Address address) {
        return tonlib.getSeqno(address);
    }

    public static boolean isDeployed(Tonlib tonlib, Address address) {
        return StringUtils.isNotEmpty(tonlib.getRawAccountState(address).getCode());
    }

    public static void waitForDeployment(Tonlib tonlib, Address address, int timeoutSeconds) {
        log.info("waiting for deployment up to " + timeoutSeconds + " sec");
        int i = 0;
        do {
            if (++i * 2 >= timeoutSeconds) {
                throw new Error("Can't deploy contract within specified timeout.");
            }
            Utils.sleep(2);
        }
        while (!isDeployed(tonlib, address));
    }

    public static void waitForBalanceChange(Tonlib tonlib, Address address, int timeoutSeconds) {
        log.info("waiting for balance change up to " + timeoutSeconds + " sec");
        BigInteger initialBalance = tonlib.getAccountBalance(address);
        int i = 0;
        do {
            if (++i * 2 >= timeoutSeconds) {
                throw new Error("Balance was not changed within specified timeout.");
            }
            Utils.sleep(2);
        }
        while (initialBalance.equals(tonlib.getAccountBalance(address)));
    }

    public static void waitForJettonBalanceChange(Tonlib tonlib, Address jettonMinter, Address address, int timeoutSeconds) {
        log.info("waiting for jetton balance change up to " + timeoutSeconds + " sec");
        BigInteger initialBalance = getJettonBalance(tonlib, jettonMinter, address);
        int i = 0;
        do {
            if (++i * 2 >= timeoutSeconds) {
                throw new Error("Balance was not changed within specified timeout.");
            }
            Utils.sleep(2);
        }
        while (initialBalance.equals(getJettonBalance(tonlib, jettonMinter, address)));
    }

    public static BigInteger getJettonBalance(Tonlib tonlib, Address jettonMinter, Address destinationAddress) {

        try {
            JettonMinter jettonMinterWallet = JettonMinter.builder()
                    .tonlib(tonlib)
                    .customAddress(jettonMinter)
                    .build();

            JettonWallet jettonWallet = jettonMinterWallet.getJettonWallet(destinationAddress);

            return jettonWallet.getBalance();
        } catch (Error e) {
            return new BigInteger("-1");
        }
    }
}