package org.ton.ton4j.smartcontract.dns;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

import java.math.BigInteger;
import java.util.*;

import com.google.gson.internal.LinkedTreeMap;
import org.ton.ton4j.adnl.AdnlLiteClient;
import org.ton.ton4j.address.Address;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellBuilder;
import org.ton.ton4j.cell.CellSlice;
import org.ton.ton4j.smartcontract.token.nft.NftUtils;
import org.ton.ton4j.smartcontract.types.AdnlAddress;
import org.ton.ton4j.tl.liteserver.responses.RunMethodResult;
import org.ton.ton4j.tlb.VmCellSlice;
import org.ton.ton4j.tlb.VmStackValueInt;
import org.ton.ton4j.tlb.VmStackValueSlice;
import org.ton.ton4j.toncenter.TonCenter;
import org.ton.ton4j.toncenter.model.RunGetMethodResponse;
import org.ton.ton4j.tonlib.Tonlib;
import org.ton.ton4j.tonlib.types.RunResult;
import org.ton.ton4j.tonlib.types.TvmStackEntryCell;
import org.ton.ton4j.tonlib.types.TvmStackEntryNumber;
import org.ton.ton4j.utils.Utils;

public class DnsUtils {

  static final String DNS_CATEGORY_NEXT_RESOLVER = "dns_next_resolver"; // Smart Contract address
  static final String DNS_CATEGORY_WALLET = "wallet"; // Smart Contract address
  static final String DNS_CATEGORY_SITE = "site"; // ADNL address

  /**
   * @param category String
   * @return BigInteger
   */
  static BigInteger categoryToInt(String category) {
    if ((isNull(category)) || (category.isEmpty())) {
      return BigInteger.ZERO;
    }

    byte[] categoryBytes = category.getBytes();
    String categoryHash = Utils.sha256(categoryBytes);
    return new BigInteger(categoryHash, 16);
  }

  /**
   * @param smartContractAddress Address
   * @return Cell
   */
  public static Cell createSmartContractAddressRecord(Address smartContractAddress) {
    return CellBuilder.beginCell()
        .storeUint(
            0x9fd3,
            16) // https://github.com/ton-blockchain/ton/blob/7e3df93ca2ab336716a230fceb1726d81bac0a06/crypto/block/block.tlb#L827
        .storeAddress(smartContractAddress)
        .storeUint(0, 8)
        .endCell();
  }

  /**
   * @param adnlAddress AdnlAddress
   * @return Cell
   */
  public static Cell createAdnlAddressRecord(AdnlAddress adnlAddress) {
    return CellBuilder.beginCell()
        .storeUint(
            0xad01,
            16) // https://github.com/ton-blockchain/ton/blob/7e3df93ca2ab336716a230fceb1726d81bac0a06/crypto/block/block.tlb#L821
        .storeBytes(adnlAddress.getClonedBytes())
        .storeUint(0, 8)
        .endCell();
  }

  /**
   * @param smartContractAddress Address
   * @return Cell
   */
  public static Cell createNextResolverRecord(Address smartContractAddress) {
    return CellBuilder.beginCell()
        .storeUint(
            0xba93,
            16) // https://github.com/ton-blockchain/ton/blob/7e3df93ca2ab336716a230fceb1726d81bac0a06/crypto/block/block.tlb#L819
        .storeAddress(smartContractAddress)
        .storeUint(0, 8)
        .endCell();
  }

  /**
   * @param cell Cell
   * @param prefix0 int
   * @param prefix1 int
   * @return Address|null
   */
  private static Address parseSmartContractAddressImpl(Cell cell, int prefix0, int prefix1) {
    if ((cell.getBits().toByteArray()[0] & 0xFF) != prefix0
        || (cell.getBits().toByteArray()[1] & 0xFF) != prefix1) {
      throw new Error("Invalid dns record value prefix");
    }

    // cell.bits = cell.getBits().cloneFrom(2 * 8);
    Cell c = CellSlice.beginParse(cell).skipBits(16).sliceToCell();

    return NftUtils.parseAddress(c); // todo parseSmartContractAddressImpl
  }

  /**
   * @param cell Cell
   * @return Address|null
   */
  static Address parseSmartContractAddressRecord(Cell cell) {
    return parseSmartContractAddressImpl(cell, 0x9f, 0xd3);
  }

  /**
   * @param cell Cell
   * @return Address|null
   */
  static Address parseNextResolverRecord(Cell cell) {
    return parseSmartContractAddressImpl(cell, 0xba, 0x93);
  }

  /**
   * @param cell Cell
   * @return AdnlAddress
   */
  static AdnlAddress parseAdnlAddressRecord(Cell cell) {
    if ((cell.getBits().toByteArray()[0] & 0xFF) != 0xad
        || (cell.getBits().toByteArray()[1] & 0xFF) != 0x01) {
      throw new Error("Invalid dns record value prefix");
    }
    byte[] bytes = Arrays.copyOfRange(cell.getBits().toByteArray(), 2, 2 + 32);
    return AdnlAddress.of(bytes);
  }

  /**
   * @param tonlib Tonlib
   * @param dnsAddress String address of dns smart contract
   * @param rawDomainBytes byte[]
   * @param category String | undefined category of requested DNS record
   * @param oneStep boolean | undefined non-recursive
   * @return Cell | Address | AdnlAddress | null
   */
  private static Object dnsResolveImpl(
      Tonlib tonlib, Address dnsAddress, byte[] rawDomainBytes, String category, boolean oneStep) {
    int len = rawDomainBytes.length * 8;

    Cell domainCell = CellBuilder.beginCell().storeBytes(rawDomainBytes).endCell();

    BigInteger categoryInteger = categoryToInt(category);

    Deque<String> stack = new ArrayDeque<>();

    stack.offer("[slice, " + domainCell.toHex(true) + "]");
    stack.offer("[num, " + categoryInteger + "]");

    RunResult result = tonlib.runMethod(dnsAddress, "dnsresolve", stack);
    if (result.getStack().size() != 2) {
      throw new Error("Invalid dnsresolve response");
    }

    Cell cell = null;

    int resultLen = ((TvmStackEntryNumber) result.getStack().get(0)).getNumber().intValue();
    System.out.println("tonlib resultLen=" + resultLen);
    Object r = result.getStack().get(1);
    if (r instanceof TvmStackEntryCell) {
      TvmStackEntryCell cellResult = (TvmStackEntryCell) result.getStack().get(1);
      cell =
          CellBuilder.beginCell()
              .fromBoc(Utils.base64ToBytes(cellResult.getCell().getBytes()))
              .endCell();
    }

    if ((nonNull(cell)) && (isNull(cell.getBits()))) {
      throw new Error("Invalid dnsresolve response");
    }

    if (resultLen == 0) {
      return null;
    }

    if ((resultLen % 8) != 0) {
      throw new Error("Domain split not at a component boundary");
    }

    if (resultLen > len) {
      throw new Error("Invalid response " + resultLen + "/" + len);
    } else if (resultLen == len) {
      if (DNS_CATEGORY_NEXT_RESOLVER.equals(category)) {
        return nonNull(cell) ? parseNextResolverRecord(cell) : null;
      } else if (DNS_CATEGORY_WALLET.equals(category)) {
        return nonNull(cell) ? parseSmartContractAddressRecord(cell) : null;
      } else if (DNS_CATEGORY_SITE.equals(category)) {
        return nonNull(cell) ? parseAdnlAddressRecord(cell) : null;
      } else {
        return null;
      }
    } else { // partial resolved
      if (isNull(cell)) {
        return null; // domain cannot be resolved
      } else {
        Address nextAddress = parseNextResolverRecord(cell);
        if (oneStep) {
          if (category.equals(DNS_CATEGORY_NEXT_RESOLVER)) {
            return nextAddress;
          } else {
            return null;
          }
        } else {
          return dnsResolveImpl(
              tonlib,
              nextAddress,
              Arrays.copyOfRange(rawDomainBytes, resultLen / 8, rawDomainBytes.length),
              category,
              false);
        }
      }
    }
  }

  private static Object dnsResolveImpl(
      TonCenter tonCenterClient,
      Address dnsAddress,
      byte[] rawDomainBytes,
      String category,
      boolean oneStep) {
    int len = rawDomainBytes.length * 8;

    Cell domainCell = CellBuilder.beginCell().storeBytes(rawDomainBytes).endCell();

    BigInteger categoryInteger = categoryToInt(category);

    List<List<Object>> stack = new ArrayList<>();
    List<Object> domainParam = new ArrayList<>();
    domainParam.add("tvm.Slice");
    domainParam.add(domainCell.toBase64());
    stack.add(domainParam);
    
    List<Object> categoryParam = new ArrayList<>();
    categoryParam.add("num");
    categoryParam.add(categoryInteger.toString());
    stack.add(categoryParam);

    RunGetMethodResponse runMethodResult = tonCenterClient.runGetMethod(dnsAddress.toBounceable(), "dnsresolve", stack).getResult();
    if (runMethodResult.getStack().size() != 2) {
      throw new Error("Invalid dnsresolve response");
    }
    
    int resultLen = Integer.parseInt(((String) new ArrayList<>(runMethodResult.getStack().get(0)).get(1)).substring(2), 16);
    
    Cell cell = null;
    List<Object> elements = new ArrayList<>(runMethodResult.getStack().get(1));
    LinkedTreeMap<String, String> l = (LinkedTreeMap<String, String>) elements.get(1);
    String cellBase64 = l.get("bytes");
    if (nonNull(cellBase64) && !cellBase64.isEmpty()) {
      cell = CellBuilder.beginCell()
          .fromBoc(Utils.base64ToBytes(cellBase64))
          .endCell();
    }

    if ((nonNull(cell)) && (isNull(cell.getBits()))) {
      throw new Error("Invalid dnsresolve response");
    }

    if (resultLen == 0) {
      return null;
    }

    if ((resultLen % 8) != 0) {
      throw new Error("Domain split not at a component boundary");
    }

    if (resultLen > len) {
      throw new Error("Invalid response " + resultLen + "/" + len);
    } else if (resultLen == len) {
      if (DNS_CATEGORY_NEXT_RESOLVER.equals(category)) {
        return nonNull(cell) ? parseNextResolverRecord(cell) : null;
      } else if (DNS_CATEGORY_WALLET.equals(category)) {
        return nonNull(cell) ? parseSmartContractAddressRecord(cell) : null;
      } else if (DNS_CATEGORY_SITE.equals(category)) {
        return nonNull(cell) ? parseAdnlAddressRecord(cell) : null;
      } else {
        return null;
      }
    } else { // partial resolved
      if (isNull(cell)) {
        return null; // domain cannot be resolved
      } else {
        Address nextAddress = parseNextResolverRecord(cell);
        if (oneStep) {
          if (category.equals(DNS_CATEGORY_NEXT_RESOLVER)) {
            return nextAddress;
          } else {
            return null;
          }
        } else {
          return dnsResolveImpl(
              tonCenterClient,
              nextAddress,
              Arrays.copyOfRange(rawDomainBytes, resultLen / 8, rawDomainBytes.length),
              category,
              false);
        }
      }
    }
  }

  private static Object dnsResolveImpl(
      AdnlLiteClient adnlLiteClient,
      Address dnsAddress,
      byte[] rawDomainBytes,
      String category,
      boolean oneStep) {
    int len = rawDomainBytes.length * 8;

    Cell domainCell = CellBuilder.beginCell().storeBytes(rawDomainBytes).endCell();

    BigInteger categoryInteger = categoryToInt(category);

    RunMethodResult runMethodResult =
        adnlLiteClient.runMethod(
            dnsAddress,
            "dnsresolve",
            VmStackValueSlice.builder()
                .cell(VmCellSlice.builder().cell(domainCell).build())
                .build(),
            VmStackValueInt.builder().value(categoryInteger).build());
    if (runMethodResult.getExitCode() != 0) {
      throw new Error("method dnsresolve returned an exit code " + runMethodResult.getExitCode());
    }
    int resultLen = runMethodResult.getIntByIndex(0).intValue();
    Cell cell = runMethodResult.getCellByIndex(1);

    if ((nonNull(cell)) && (isNull(cell.getBits()))) {
      throw new Error("Invalid dnsresolve response");
    }

    if (resultLen == 0) {
      return null;
    }

    if ((resultLen % 8) != 0) {
      throw new Error("Domain split not at a component boundary");
    }

    if (resultLen > len) {
      throw new Error("Invalid response " + resultLen + "/" + len);
    } else if (resultLen == len) {
      if (DNS_CATEGORY_NEXT_RESOLVER.equals(category)) {
        return nonNull(cell) ? parseNextResolverRecord(cell) : null;
      } else if (DNS_CATEGORY_WALLET.equals(category)) {
        return nonNull(cell) ? parseSmartContractAddressRecord(cell) : null;
      } else if (DNS_CATEGORY_SITE.equals(category)) {
        return nonNull(cell) ? parseAdnlAddressRecord(cell) : null;
      } else {
        return null;
      }
    } else { // partial resolved
      if (isNull(cell)) {
        return null; // domain cannot be resolved
      } else {
        Address nextAddress = parseNextResolverRecord(cell);
        if (oneStep) {
          if (category.equals(DNS_CATEGORY_NEXT_RESOLVER)) {
            return nextAddress;
          } else {
            return null;
          }
        } else {
          return dnsResolveImpl(
              adnlLiteClient,
              nextAddress,
              Arrays.copyOfRange(rawDomainBytes, resultLen / 8, rawDomainBytes.length),
              category,
              false);
        }
      }
    }
  }

  /**
   * Verify and convert domain
   *
   * @param domain String
   * @return byte[]
   */
  static byte[] domainToBytes(String domain) {
    if (isNull(domain) || domain.isEmpty()) {
      throw new Error("Empty domain");
    }
    if (domain.equals(".")) {
      return new byte[] {0};
    }

    domain = domain.toLowerCase();
    for (int i = 0; i < domain.length(); i++) {
      if (domain.charAt(i) <= 32) {
        throw new Error("Bytes in range 0..32 are not allowed in domain names");
      }
    }

    for (int i = 0; i < domain.length(); i++) {
      String s = domain.substring(i, i + 1);
      for (int c = 127; c <= 159; c++) { // another control codes range
        if (s.equals(String.valueOf(c))) {
          throw new Error("Bytes in range 127..159 are not allowed in domain names");
        }
      }
    }

    List<String> arr = new ArrayList<>(Arrays.asList(domain.split("\\.")));

    for (String part : arr) {
      if (part.isEmpty()) {
        throw new Error("Domain name cannot have an empty component");
      }
    }
    Collections.reverse(arr);
    String rawDomain = String.join("\0", arr) + '\0';
    if (rawDomain.length() < 126) {
      rawDomain = '\0' + rawDomain;
    }

    return rawDomain.getBytes();
  }

  /**
   * @param tonlib Tonlib
   * @param rootDnsAddress Address of root DNS smart contract
   * @param domain String e.g "sub.alice.ton"
   * @param category String | undefined category of requested DNS record
   * @param oneStep boolean | undefined non-recursive
   * @return Cell | Address | AdnlAddress | null
   */
  public static Object dnsResolve(
      Tonlib tonlib, Address rootDnsAddress, String domain, String category, boolean oneStep) {
    byte[] rawDomainBytes = domainToBytes(domain);

    return dnsResolveImpl(tonlib, rootDnsAddress, rawDomainBytes, category, oneStep);
  }

  public static Object dnsResolve(
      AdnlLiteClient adnlLiteClient,
      Address rootDnsAddress,
      String domain,
      String category,
      boolean oneStep) {
    byte[] rawDomainBytes = domainToBytes(domain);

    return dnsResolveImpl(adnlLiteClient, rootDnsAddress, rawDomainBytes, category, oneStep);
  }
  
  public static Object dnsResolve(
      TonCenter tonCenterClient,
      Address rootDnsAddress,
      String domain,
      String category,
      boolean oneStep) {
    byte[] rawDomainBytes = domainToBytes(domain);

    return dnsResolveImpl(tonCenterClient, rootDnsAddress, rawDomainBytes, category, oneStep);
  }
}
