package org.ton.java.smartcontract.dns;

import org.ton.java.address.Address;
import org.ton.java.cell.Cell;
import org.ton.java.tonlib.Tonlib;
import org.ton.java.utils.Utils;

public class Dns {

    public static final String DNS_CATEGORY_NEXT_RESOLVER = "dns_next_resolver"; // Smart Contract address
    public static final String DNS_CATEGORY_WALLET = "wallet"; // Smart Contract address
    public static final String DNS_CATEGORY_SITE = "site"; // ADNL address

    Tonlib tonlib;

    public Dns(Tonlib tonlib) {
        this.tonlib = tonlib;
    }

    public Address getRootDnsAddress() {
        Cell cell = tonlib.getConfigParam(tonlib.getLast().getLast(), 4);
        byte[] byteArray = cell.getBits().toByteArray();
        if (byteArray.length != 256 / 8) {
            throw new Error("Invalid ConfigParam 4 length " + byteArray.length);
        }
        String hex = Utils.bytesToHex(byteArray);
        return Address.of("-1:" + hex);
    }

    public Object resolve(String domain, String category, boolean oneStep) {
        Address rootDnsAddress = getRootDnsAddress();

        return DnsUtils.dnsResolve(tonlib, rootDnsAddress, domain, category, oneStep);
    }

    public Object resolve(String domain, String category) {
        return resolve(domain, category, false);
    }

    /**
     * @param domain String e.g "sub.alice.ton"
     * @return Address | null
     */
    public Object getWalletAddress(String domain) {
        return this.resolve(domain, DNS_CATEGORY_WALLET);
    }

    /**
     * @param domain String e.g "sub.alice.ton"
     * @return AdnlAddress | null
     */
    public Object getSiteAddress(String domain) {
        return this.resolve(domain, DNS_CATEGORY_SITE);
    }
}
