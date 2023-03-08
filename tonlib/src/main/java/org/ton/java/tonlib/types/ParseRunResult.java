package org.ton.java.tonlib.types;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.ToNumberPolicy;
import org.ton.java.cell.Cell;
import org.ton.java.utils.Utils;

import java.io.Serializable;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Base64;
import java.util.Deque;

import static java.util.Objects.isNull;


public class ParseRunResult implements Serializable {
    private static final Gson gson = new GsonBuilder()
            .setObjectToNumberStrategy(ToNumberPolicy.BIG_DECIMAL)
            .setLenient()
            .create();

    // key - marker(UUID), value - original string
//    public static Map<String, String> markers = new HashMap<>();

    /**
     * @param elementType - "num", "number", "int", "cell", "slice"
     * @param element     - define type, base64?
     * @return TvmStackEntry
     */
    public static TvmStackEntry renderTvmElement(String elementType, String element) {

        String[] values = {
                "num", "number", "int", "cell", "slice", "tvm.cell", "tvm.slice"
        };

        if (Arrays.asList(values).contains(elementType)) {

            if (elementType.contains("num") || elementType.contains("number") || elementType.contains("int")) {
                return TvmStackEntryNumber.builder().number(TvmNumber.builder().number(element).build()).build();
            } else if (elementType.contains("cell")) {
                byte[] e = Utils.hexToBytes(element);
                Cell cell = Cell.fromBoc(e);
                String cellBase64 = bytesToBase64(cell.toBoc(false));
                return TvmStackEntryCell.builder().cell(TvmCell.builder().bytes(cellBase64).build()).build();
            } else if (elementType.contains("slice")) {
                byte[] e = Utils.hexToBytes(element);
                Cell cell = Cell.fromBoc(e);
                String cellBase64 = bytesToBase64(cell.toBoc(false));
                return TvmStackEntrySlice.builder().slice(TvmSlice.builder().bytes(cellBase64).build()).build();
            } else if (elementType.contains("dict")) {
                // TODO support list, dict and tuple
            } else if (elementType.contains("list")) {

            } else if (elementType.contains("tuple")) {

            }

        } else {
            throw new Error("Rendering of type " + elementType + " is not implemented");
        }
        return TvmStackEntryCell.builder().build();
    }

    /**
     * @param stackData -  Elements like that are expected:
     *                  [["num", 300], ["cell", "0x"], ["dict", {...}]]
     *                  Currently, only "num", "cell" and "slice" are supported.
     *                  To be implemented:
     *                  T: "list", "tuple", "dict"
     * @return TvmStackEntry
     */
    public static Deque<TvmStackEntry> renderTvmStack(Deque<String> stackData) {
        Deque<TvmStackEntry> stack = new ArrayDeque<>();

        for (String e : stackData) {
            String[] a = e.replace("[", "").replace("]", "").split(",");
            String type = a[0].trim();
            String data = a[1].trim();
            stack.offer(renderTvmElement(type, data));
        }
        return stack;
    }

    public static String serializeTvmElement(TvmStackEntry entry) {

        if (entry instanceof TvmStackEntryNumber) {
            return String.format("[\"%s\",%x]", "num", ((TvmStackEntryNumber) entry).getNumber());
        } else if (entry instanceof TvmStackEntryCell) {
            return String.format("[\"%s\",%s]", "cell", ((TvmStackEntryCell) entry).getCell().getBytes());
        } else if (entry instanceof TvmStackEntrySlice) {
            return String.format("[\"%s\",%s]", "cell", ((TvmStackEntrySlice) entry).getSlice().getBytes());
        } else if (entry instanceof TvmStackEntryTuple) {
            return String.format("[\"%s\",%s]", "tuple", ((TvmStackEntryTuple) entry).getTuple());
        } else if (entry instanceof TvmStackEntryList) {
            return String.format("[\"%s\",%s]", "list", ((TvmStackEntryList) entry).getList());
        } else {
            throw new Error("Unknown type");
        }
    }


    public static Deque<String> serializeTvmStack(Deque<TvmStackEntry> tvmStack) {

        Deque<String> stack = new ArrayDeque<>();
        for (TvmStackEntry e : tvmStack) {
            stack.offer(serializeTvmElement(e));
        }
        return stack;
    }

    private static String bytesToBase64(byte[] bytes) {
        return Base64.getEncoder().encodeToString(bytes);
    }

    private static String bytesToBase64UrlSafe(byte[] bytes) {
        return Base64.getUrlEncoder().encodeToString(bytes);
    }

    /**
     * Finds single string-block starting with pattern and ending with CLOSE
     */
    private static String sbb(String str, String pattern) {
        if (isNull(str) || !str.contains(pattern))
            return null;

        int openindex = str.indexOf(pattern) + pattern.indexOf("{");
        int closeIndex = findPosOfClosingBracket(str, pattern);
        if (closeIndex == -1) {
            return null;
        }
        return str.substring(openindex, closeIndex + 1).trim();
    }


    /**
     * Finds matching closing bracket in string starting with pattern
     */
    private static int findPosOfClosingBracket(String str, String pattern) {
        int i;
        int openBracketIndex = pattern.indexOf("{");
        int index = str.indexOf(pattern) + openBracketIndex;

        ArrayDeque<Integer> st = new ArrayDeque<>();

        for (i = index; i < str.length(); i++) {
            if (str.charAt(i) == '{') {
                st.push((int) str.charAt(i));
            } else if (str.charAt(i) == '}') {
                if (st.isEmpty()) {
                    return i;
                }
                st.pop();
                if (st.isEmpty()) {
                    return i;
                }
            }
        }
        return -1;
    }
}
