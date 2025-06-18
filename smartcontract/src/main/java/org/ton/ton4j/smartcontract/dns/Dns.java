package org.ton.ton4j.smartcontract.dns;

import static java.util.Objects.nonNull;

import lombok.Builder;
import org.ton.java.adnl.AdnlLiteClient;
import org.ton.ton4j.address.Address;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.tonlib.Tonlib;
import org.ton.ton4j.utils.Utils;

@Builder
public class Dns {

  public static final String DNS_CATEGORY_NEXT_RESOLVER =
      "dns_next_resolver"; // Smart Contract address
  public static final String DNS_CATEGORY_WALLET = "wallet"; // Smart Contract address
  public static final String DNS_CATEGORY_SITE = "site"; // ADNL address

  private Tonlib tonlib;
  private AdnlLiteClient adnlLiteClient;

  public Address getRootDnsAddress() {
    if (nonNull(adnlLiteClient)) {
      return Address.of("-1:" + adnlLiteClient.getConfigParam4().getDnsRootAddr());
    }

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
    if (nonNull(adnlLiteClient)) {
      return DnsUtils.dnsResolve(adnlLiteClient, rootDnsAddress, domain, category, oneStep);
    }
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
    return resolve(domain, DNS_CATEGORY_WALLET);
  }

  /**
   * @param domain String e.g "sub.alice.ton"
   * @return AdnlAddress | null
   */
  public Object getSiteAddress(String domain) {
    return resolve(domain, DNS_CATEGORY_SITE);
  }
}
