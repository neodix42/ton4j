package org.ton.java.smartcontract.dns;

import org.ton.java.address.Address;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.smartcontract.token.nft.NftUtils;
import org.ton.java.smartcontract.types.AdnlAddress;
import org.ton.java.tonlib.Tonlib;
import org.ton.java.tonlib.types.RunResult;
import org.ton.java.tonlib.types.TvmStackEntryCell;
import org.ton.java.tonlib.types.TvmStackEntryNumber;
import org.ton.java.utils.Utils;

import java.math.BigInteger;
import java.util.*;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

public class DnsUtils {

    static final String DNS_CATEGORY_NEXT_RESOLVER = "dns_next_resolver"; // Smart Contract address
    static final String DNS_CATEGORY_WALLET = "wallet"; // Smart Contract address
    static final String DNS_CATEGORY_SITE = "site"; // ADNL address

    /**
     * @param category String
     * @return BigInteger
     */
    static BigInteger categoryToInt(String category) {
        if ((isNull(category)) || (category.length() == 0)) {
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
        CellBuilder cellBuilder = CellBuilder.beginCell();
        cellBuilder.storeUint(0x9fd3, 16); // https://github.com/ton-blockchain/ton/blob/7e3df93ca2ab336716a230fceb1726d81bac0a06/crypto/block/block.tlb#L827
        cellBuilder.storeAddress(smartContractAddress);
        cellBuilder.storeUint(0, 8);
        return cellBuilder.endCell();
    }

    /**
     * @param adnlAddress AdnlAddress
     * @return Cell
     */
    public static Cell createAdnlAddressRecord(AdnlAddress adnlAddress) {
        CellBuilder cellBuilder = CellBuilder.beginCell();
        cellBuilder.storeUint(0xad01, 16); // https://github.com/ton-blockchain/ton/blob/7e3df93ca2ab336716a230fceb1726d81bac0a06/crypto/block/block.tlb#L821
        cellBuilder.storeBytes(adnlAddress.getBytes());
        cellBuilder.storeUint(0, 8);
        return cellBuilder.endCell();
    }

    /**
     * @param smartContractAddress Address
     * @return Cell
     */
    public static Cell createNextResolverRecord(Address smartContractAddress) {
        CellBuilder cellBuilder = CellBuilder.beginCell();
        cellBuilder.storeUint(0xba93, 16); // https://github.com/ton-blockchain/ton/blob/7e3df93ca2ab336716a230fceb1726d81bac0a06/crypto/block/block.tlb#L819
        cellBuilder.storeAddress(smartContractAddress);
        cellBuilder.storeUint(0, 8);
        return cellBuilder.endCell();
    }

    /**
     * @param cell    Cell
     * @param prefix0 int
     * @param prefix1 int
     * @return Address|null
     */
    private static Address parseSmartContractAddressImpl(Cell cell, int prefix0, int prefix1) {
        if ((cell.bits.toByteArray()[0] & 0xFF) != prefix0 || (cell.bits.toByteArray()[1] & 0xFF) != prefix1) {
            throw new Error("Invalid dns record value prefix");
        }
        cell.bits = cell.bits.cloneFrom(2);
        return NftUtils.parseAddress(cell);
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
        if ((cell.bits.toByteArray()[0] & 0xFF) != 0xad || (cell.bits.toByteArray()[1] & 0xFF) != 0x01) { // todo
            throw new Error("Invalid dns record value prefix");
        }
        byte[] bytes = Arrays.copyOfRange(cell.bits.toByteArray(), 2, 2 + 32);// cell.bits.array.slice(2, 2 + 32); // skip prefix - first 16 bits
        return new AdnlAddress(bytes);
    }

    /**
     * @param tonlib         Tonlib
     * @param dnsAddress     String address of dns smart contract
     * @param rawDomainBytes byte[]
     * @param category       String | undefined category of requested DNS record
     * @param oneStep        boolean | undefined non-recursive
     * @return Cell | Address | AdnlAddress | null
     */
    private static Object dnsResolveImpl(Tonlib tonlib, Address dnsAddress, byte[] rawDomainBytes, String category, boolean oneStep) {
        int len = rawDomainBytes.length * 8;

        CellBuilder domainCell = CellBuilder.beginCell();
        domainCell.storeBytes(rawDomainBytes);

        BigInteger categoryInteger = categoryToInt(category);

        Deque<String> stack = new ArrayDeque<>();

        stack.offer("[slice, " + domainCell.endCell().toHex(true) + "]");
        stack.offer("[num, " + categoryInteger + "]");

        RunResult result = tonlib.runMethod(dnsAddress, "dnsresolve", stack);
        if (result.getStack().size() != 2) {
            throw new Error("Invalid dnsresolve response");
        }

        Cell cell = null;

        int resultLen = ((TvmStackEntryNumber) result.getStack().get(0)).getNumber().intValue();
        Object r = result.getStack().get(1);
        if (r instanceof TvmStackEntryCell) {
            TvmStackEntryCell cellResult = (TvmStackEntryCell) result.getStack().get(1);
            cell = CellBuilder.fromBoc(Utils.base64ToBytes(cellResult.getCell().getBytes()));
        }

        if ((nonNull(cell)) && (isNull(cell.bits))) {
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
//                    dnsResolveImpl(tonlib, nextAddress.toString(), rawDomainBytes.slice(resultLen / 8), category, false);
                    return dnsResolveImpl(tonlib, nextAddress, Arrays.copyOfRange(rawDomainBytes, resultLen / 8, rawDomainBytes.length), category, false);
                    // rawDomainBytes.slice(resultLen / 8), category, false);
//                    return dnsResolveImpl(tonlib, nextAddress, Arrays.copyOfRange(rawDomainBytes, 0, resultLen / 8), category, false);// rawDomainBytes.slice(resultLen / 8), category, false);
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
        if (isNull(domain) || domain.length() == 0) {
            throw new Error("Empty domain");
        }
        if (domain.equals(".")) {
            return new byte[]{0};
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

        List<String> arr = new ArrayList<>(List.of(domain.split("\\.")));

        for (String part : arr) {
            if (part.length() == 0) {
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
     * @param tonlib         Tonlib
     * @param rootDnsAddress Address of root DNS smart contract
     * @param domain         String e.g "sub.alice.ton"
     * @param category       String | undefined category of requested DNS record
     * @param oneStep        boolean | undefined non-recursive
     * @return Cell | Address | AdnlAddress | null
     */
    public static Object dnsResolve(Tonlib tonlib, Address rootDnsAddress, String domain, String category, boolean oneStep) {
        byte[] rawDomainBytes = domainToBytes(domain);

        return dnsResolveImpl(tonlib, rootDnsAddress, rawDomainBytes, category, oneStep);
    }
}