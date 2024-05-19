package org.ton.java.smartcontract.wallet;


import org.apache.commons.lang3.StringUtils;
import org.ton.java.address.Address;
import org.ton.java.tonlib.Tonlib;
import org.ton.java.utils.Utils;

import java.math.BigInteger;

public class ContractUtils {

    public static long getSeqno(Tonlib tonlib, Address address) {
        return tonlib.getSeqno(address);
    }

    public static boolean isDeployed(Tonlib tonlib, Address address) {
        return StringUtils.isNotEmpty(tonlib.getRawAccountState(address).getCode());
    }

    public static void waitForDeployment(Tonlib tonlib, Address address, int timeoutSeconds) {
        System.out.println("waiting for deployment up to " + timeoutSeconds + " sec");
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
        System.out.println("waiting for balance change up to " + timeoutSeconds + " sec");
        BigInteger initialBalance = getBalance(tonlib, address);
        int i = 0;
        do {
            if (++i * 2 >= timeoutSeconds) {
                throw new Error("Balance was not changed within specified timeout.");
            }
            Utils.sleep(2);
        }
        while (initialBalance.equals(getBalance(tonlib, address)));
    }

    public static BigInteger getBalance(Tonlib tonlib, Address address) {
        return new BigInteger(tonlib.getAccountState(address).getBalance());
    }
}