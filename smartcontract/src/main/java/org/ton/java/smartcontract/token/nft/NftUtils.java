package org.ton.java.smartcontract.token.nft;

import org.ton.java.address.Address;
import org.ton.java.bitstring.BitString;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.CellSlice;
import org.ton.java.smartcontract.types.Royalty;
import org.ton.java.tonlib.Tonlib;
import org.ton.java.tonlib.types.RunResult;
import org.ton.java.tonlib.types.TvmStackEntryCell;
import org.ton.java.tonlib.types.TvmStackEntryNumber;

import java.math.BigInteger;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static java.util.Objects.nonNull;

public class NftUtils {


    public static final int SNAKE_DATA_PREFIX = 0x00;
    public static final int CHUNK_DATA_PREFIX = 0x01;
    public static final int ONCHAIN_CONTENT_PREFIX = 0x00;
    public static final int OFFCHAIN_CONTENT_PREFIX = 0x01;

    /**
     * @param uri String
     * @return byte[]
     */
    public static byte[] serializeUri(String uri) {
        try {
            return URLEncoder.encode(uri, StandardCharsets.UTF_8).getBytes();
        } catch (Exception e) {
            throw new Error("Cannot serialize URI " + uri);
        }
    }

    /**
     * @param bytes byte[]
     * @return String
     */
    static String parseUri(byte[] bytes) {
        return URLDecoder.decode(new String(bytes), StandardCharsets.UTF_8);
    }

    /**
     * @param uri String
     * @return Cell
     */
    public static Cell createOffchainUriCell(String uri) {
        CellBuilder cell = CellBuilder.beginCell();
        cell.storeUint(OFFCHAIN_CONTENT_PREFIX, 8);
        cell.storeBytes(uri.getBytes(StandardCharsets.UTF_8));
        return cell.endCell();
    }

    /**
     * @param cell Cell
     * @return String
     */
    public static String parseOffchainUriCell(Cell cell) {
        if ((cell.getBits().toByteArray()[0] & 0xFF) != OFFCHAIN_CONTENT_PREFIX) {
            throw new Error("no OFFCHAIN_CONTENT_PREFIX");
        }

        int length = 0;
        Cell c = cell;
        while (nonNull(c)) {
            length += c.getBits().toByteArray().length;
            if (c.getUsedRefs() != 0) {
                c = c.refs.get(0);
            } else {
                c = null;
            }
        }

        byte[] bytes = new byte[length];
        length = 0;
        c = cell;
        while (nonNull(c)) {
            bytes = Arrays.copyOfRange(c.getBits().toByteArray(), 0, length);
            length += c.getBits().toByteArray().length;
            if (c.getUsedRefs() != 0) {
                c = c.refs.get(0);
            } else {
                bytes = Arrays.copyOfRange(c.getBits().toByteArray(), 0, length);
                c = null;
            }
        }
        return parseUri(Arrays.copyOfRange(bytes, 1, bytes.length)); // slice OFFCHAIN_CONTENT_PREFIX
    }

    /**
     * TODO onchain content
     * The first byte is 0x00 and the rest is key/value dictionary.
     * Key is sha256 hash of string.
     * Value is data encoded as described in "Data serialization" paragraph.
     *
     * @param name        String name of Jetton
     * @param description String description of Jetton
     * @return cell Cell
     */
    public static Cell createOnchainDataCell(String name, String description) { // https://github.com/ton-blockchain/TIPs/issues/64
        CellBuilder cell = CellBuilder.beginCell();
        cell.storeUint(ONCHAIN_CONTENT_PREFIX, 8);
        cell.storeBytes(name.getBytes(StandardCharsets.UTF_8));
        cell.storeString(description);
        return cell.endCell();
    }

    /**
     * @param bs     BitString
     * @param cursor number
     * @param bits   number
     * @return BigInt
     */
    public static BigInteger readIntFromBitString(BitString bs, int cursor, int bits) {
        BitString cloned = bs.clone();
        cloned.readBits(cursor);
        BigInteger n = BigInteger.ZERO;
        for (int i = 0; i < bits; i++) {
            n = n.multiply(BigInteger.TWO);
            n = n.add(cloned.readBit() ? BigInteger.ONE : BigInteger.ZERO);
        }
        return n;
    }

    /**
     * @param cell Cell
     * @return Address|null
     */
    public static Address parseAddress(Cell cell) {

        return CellSlice.beginParse(cell).loadAddress();
//        String result; todo wtf?
//
//        try {
//            BigInteger n = readIntFromBitString(cell.getBits(), 3, 8);
//            if (n.compareTo(BigInteger.valueOf(127L)) > 0) {
//                n = n.subtract(BigInteger.valueOf(256L));
//            }
//            BigInteger hashPart = readIntFromBitString(cell.getBits(), 3 + 8, 256);
//            if ((n.toString(10) + ":" + hashPart.toString(16)).equals("0:0")) {
//                return null;
//            }
//
//            result = n.toString(10) + ":" + StringUtils.leftPad(hashPart.toString(16), 64, '0');
//        } catch (Exception e) {
//            return null;
//        }
//        return Address.of(result);
    }

    /**
     * @param tonlib  Tonlib
     * @param address String
     * @return Royalty
     */
    public static Royalty getRoyaltyParams(Tonlib tonlib, Address address) {
        RunResult result = tonlib.runMethod(address, "royalty_params");

        TvmStackEntryNumber royaltyFactorNumber = (TvmStackEntryNumber) result.getStack().get(0);
        BigInteger royaltyFactor = royaltyFactorNumber.getNumber();

        TvmStackEntryNumber royaltyBaseNumber = (TvmStackEntryNumber) result.getStack().get(1);
        BigInteger royaltyBase = royaltyBaseNumber.getNumber();

        double royalty = royaltyFactor.divide(royaltyBase).doubleValue();
        TvmStackEntryCell royaltyAddressCell = (TvmStackEntryCell) result.getStack().get(2);
        Address royaltyAddress = NftUtils.parseAddress(CellBuilder.beginCell().fromBoc(royaltyAddressCell.getCell().getBytes()).endCell());

        return Royalty.builder()
                .royaltyFactor(royaltyFactor)
                .royaltyBase(royaltyBase)
                .royalty(royalty)
                .royaltyAddress(royaltyAddress)
                .build();
    }
}
