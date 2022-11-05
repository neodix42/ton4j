package org.ton.java.smartcontract.nft;

import org.apache.commons.lang3.StringUtils;
import org.ton.java.address.Address;
import org.ton.java.bitstring.BitString;
import org.ton.java.cell.Cell;
import org.ton.java.smartcontract.types.Royalty;
import org.ton.java.tonlib.Tonlib;
import org.ton.java.tonlib.types.RunResult;

import java.math.BigInteger;
import java.net.URI;
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
            return new URI(uri).toString().getBytes();
        } catch (Exception e) {
            throw new Error("Cannot serialize URI " + uri);
        }
    }

    /**
     * @param bytes byte[]
     * @return String
     */
    static String parseUri(byte[] bytes) {
        return new String(bytes);
    }

    /**
     * @param uri {string}
     * @return {Cell}
     */
    public static Cell createOffchainUriCell(String uri) {
        Cell cell = new Cell();
        cell.bits.writeUint(OFFCHAIN_CONTENT_PREFIX, 8);
        cell.bits.writeBytes(serializeUri(uri));
        return cell;
    }

    /**
     * @param cell Cell
     * @return String
     */
    public static String parseOffchainUriCell(Cell cell) {
        if ((cell.bits.toByteArray()[0] & 0xFF) != OFFCHAIN_CONTENT_PREFIX) {
            throw new Error("no OFFCHAIN_CONTENT_PREFIX");
        }

        int length = 0;
        Cell c = cell;
        while (nonNull(c)) {
            length += c.bits.toBitArray().length;
            c = c.refs.get(0);
        }

        byte[] bytes = new byte[length];
        length = 0;
        c = cell;
        while (nonNull(c)) {
            bytes = Arrays.copyOfRange(c.bits.toByteArray(), 0, length);
//            bytes.set(c.bits.array, length);
            length += c.bits.toBitArray().length;
            c = c.refs.get(0);
        }
//        return parseUri( bytes.slice(1)); // slice OFFCHAIN_CONTENT_PREFIX
        return parseUri(Arrays.copyOfRange(bytes, 1, bytes.length)); // slice OFFCHAIN_CONTENT_PREFIX
    }


    /**
     * @param bs     BitString
     * @param cursor number
     * @param bits   number
     * @return BigInt
     */
    public static BigInteger readIntFromBitString(BitString bs, int cursor, int bits) {
        BigInteger n = BigInteger.ZERO;
        for (int i = 0; i < bits; i++) {
            n = n.multiply(BigInteger.TWO);
            n = n.add(bs.get(cursor + i) ? BigInteger.ONE : BigInteger.ZERO);
        }
        return n;
    }

    /**
     * @param cell Cell
     * @return Address|null
     */
    public static Address parseAddress(Cell cell) {
        BigInteger n = readIntFromBitString(cell.bits, 3, 8);
        if (n.compareTo(BigInteger.valueOf(127L)) > 0) {
            n = n.subtract(BigInteger.valueOf(256L));
        }
        BigInteger hashPart = readIntFromBitString(cell.bits, 3 + 8, 256);
        if ((n.toString(10) + ":" + hashPart.toString(16)).equals("0:0")) { // todo
            return null;
        }

        String s = n.toString(10) + ":" + StringUtils.leftPad(hashPart.toString(16), 64, '0');
        return new Address(s);
    }

    /**
     * @param tonlib  Tonlib
     * @param address String
     * @return Royalty
     */
    public static Royalty getRoyaltyParams(Tonlib tonlib, Address address) {
        RunResult result = tonlib.runMethod(address, "'royalty_params'");
        System.out.println(result); // todo

//    const royaltyFactor = result[0].toNumber();
//    const royaltyBase = result[1].toNumber();
//    const royalty = royaltyFactor / royaltyBase;
//    const royaltyAddress = parseAddress(result[2]);
//
//        return {royalty, royaltyBase, royaltyFactor, royaltyAddress};
        return Royalty.builder().build();
    }
}
