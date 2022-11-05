package org.ton.java.tonlib.types;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.ToNumberPolicy;
import com.google.gson.internal.LinkedTreeMap;
import org.apache.commons.lang3.StringUtils;
import org.ton.java.cell.Cell;
import org.ton.java.utils.Utils;

import java.util.*;

import static java.util.Objects.isNull;

public class ParseRunResult {
    private static final Gson gson = new GsonBuilder()
            .setObjectToNumberStrategy(ToNumberPolicy.BIG_DECIMAL)
            .setLenient()
            .create();

    /**
     * @param elementType - "num", "number", "int", "cell", "slice"
     * @param element     - define type, base64?
     * @return TvmStackEntry
     */
    public static TvmStackEntry renderTvmElement(String elementType, String element) {

        String[] values = {"num", "number", "int", "cell", "slice", "tvm.cell", "tvm.slice"};

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

    public static TvmStackEntry parseJsonElement(Object element) {
        LinkedTreeMap<String, Object> t = (LinkedTreeMap) element;

        String key1 = (String) t.keySet().toArray()[0];
        Object val1 = t.get(key1);
        String key2 = (String) t.keySet().toArray()[1];
        Object val2 = t.get(key2);

        if (key2.equals("tuple")) {
            return (TvmStackEntryTuple) parseJsonElement(val2);
        } else if (key2.equals("list")) {
            return (TvmStackEntryList) parseJsonElement(val2);
        } else if (key2.equals("elements")) {
            List<String> elements = gson.fromJson(String.valueOf(val2), List.class);
            List<Object> resultElements = new ArrayList<>(); //TvmStackEntry

            for (Object o : elements) {
                LinkedTreeMap<String, Object> tt = (LinkedTreeMap) o;
                String k1 = (String) tt.keySet().toArray()[0];
                Object v1 = tt.get(k1);
                String k2 = (String) tt.keySet().toArray()[1];
                Object v2 = tt.get(k2);

                if (v1.equals("tvm.stackEntryNumber")) {
                    TvmNumber number = gson.fromJson(String.valueOf(v2), TvmNumber.class);
                    TvmStackEntryNumber stackNumber = TvmStackEntryNumber.builder()
                            .number(number)
                            .build();
                    resultElements.add(stackNumber);
                } else if (v1.equals("tvm.stackEntryCell")) {
                    TvmCell cell = gson.fromJson(String.valueOf(v2), TvmCell.class);
                    TvmStackEntryCell stackCell = TvmStackEntryCell.builder()
                            .cell(cell)
                            .build();
                    resultElements.add(stackCell);
                } else if (v1.equals("tvm.stackEntrySlice")) {
                    TvmSlice slice = gson.fromJson(String.valueOf(v2), TvmSlice.class);
                    TvmStackEntrySlice stackSlice = TvmStackEntrySlice.builder()
                            .slice(slice)
                            .build();
                    resultElements.add(stackSlice);
                } else if (v1.equals("tuple")) {
                    resultElements.add(parseJsonElement(val2));
                } else if (v1.equals("tvm.stackEntryTuple")) {
                    resultElements.add(parseJsonElement(v2));
                } else if (v1.equals("list")) {
                    resultElements.add(parseJsonElement(val2));
                } else if (v1.equals("tvm.stackEntryList")) {
                    resultElements.add(parseJsonElement(v2));
                }
            }

            if (val1.equals("tvm.tuple")) {
                TvmTuple tvmTuple = TvmTuple.builder()
                        .elements(resultElements)
                        .build();

                return TvmStackEntryTuple.builder()
                        .tuple(tvmTuple)
                        .build();
            } else if (val1.equals("tvm.list")) {
                TvmList tvmList = TvmList.builder()
                        .elements(resultElements)
                        .build();
                return TvmStackEntryList.builder()
                        .list(tvmList)
                        .build();
            }
        } else if (key2.equals("number")) {
            return gson.fromJson(String.valueOf(element), TvmStackEntryNumber.class);
        } else if (key2.equals("cell")) {
            return gson.fromJson(String.valueOf(element), TvmStackEntryCell.class);
        } else if (key2.equals("slice")) {
            return gson.fromJson(String.valueOf(element), TvmStackEntrySlice.class);
        }
        throw new Error("Error parsing json element");
    }

    public static Deque<String> serializeTvmStack(Deque<TvmStackEntry> tvmStack) {

        Deque<String> stack = new ArrayDeque<>();
        for (TvmStackEntry e : tvmStack) {
            stack.offer(serializeTvmElement(e));
        }
        return stack;
    }

    public static TvmStackEntryList parseTvmEntryListStack(TvmStackEntryList list) {
        List<Object> els = new ArrayList<>();

        for (Object o : list.getList().getElements()) {
            LinkedTreeMap<String, Object> t = (LinkedTreeMap) o;
            els.add(parseJsonElement(t));
        }

        TvmStackEntryList listResult = TvmStackEntryList.builder().build();
        TvmList tvmList = TvmList.builder().build();
        tvmList.setElements(els);
        listResult.setList(tvmList);
        return listResult;
    }

    public static TvmStackEntryTuple parseTvmEntryTupleStack(TvmStackEntryTuple list) {
        List<Object> els = new ArrayList<>();

        for (Object o : list.getTuple().getElements()) {
            LinkedTreeMap<String, Object> t = (LinkedTreeMap) o;
            els.add(parseJsonElement(t));
        }

        TvmStackEntryTuple listResult = TvmStackEntryTuple.builder().build();
        TvmTuple tvmTuple = TvmTuple.builder().build();
        tvmTuple.setElements(els);
        listResult.setTuple(tvmTuple);
        return listResult;
    }

    public static RunResult getTypedRunResult(List<String> stack, long exitCode, long gasUsed) {
        return getTypedRunResult(stack, exitCode, gasUsed, null);
    }

    public static RunResult getTypedRunResult(List<String> stack, long exitCode, long gasUsed, String extra) {

        List<TvmStackEntry> resultStack = new ArrayList<>();
        for (int i = 0; i < stack.size(); i++) {
            String stackElement = String.valueOf(stack.get(i));

            String processResult = stackElement;

            stackElement = Utils.getSafeString(stackElement, processResult, "@type=tvm.cell");
            stackElement = Utils.getSafeString(stackElement, stackElement, "@type=tvm.slice");

            String resultEscaped = stackElement;
            if (resultEscaped.substring(0, resultEscaped.indexOf(",")).contains("stackEntryList")) {
                TvmStackEntryList list = gson.fromJson(resultEscaped, TvmStackEntryList.class);
                list = parseTvmEntryListStack(list);
                resultStack.add(list);
            } else if (resultEscaped.substring(0, resultEscaped.indexOf(",")).contains("stackEntryTuple")) {
                TvmStackEntryTuple tuple = gson.fromJson(resultEscaped, TvmStackEntryTuple.class);
                tuple = parseTvmEntryTupleStack(tuple);
                resultStack.add(tuple);
            } else if (resultEscaped.substring(0, resultEscaped.indexOf(",")).contains("stackEntryNumber")) {
                TvmStackEntryNumber number = gson.fromJson(resultEscaped, TvmStackEntryNumber.class);
                resultStack.add(number);
            } else if (resultEscaped.substring(0, resultEscaped.indexOf(",")).contains("stackEntryCell")) {
//                System.out.println(resultEscaped);
//                TvmStackEntryCell cell = gson.fromJson(resultEscaped, TvmStackEntryCell.class);
                String urlSafeBase64 = StringUtils.substringBetween(resultEscaped, "bytes=", "}");
                TvmStackEntryCell cell = TvmStackEntryCell.builder()
                        .cell(TvmCell.builder()
                                .bytes(urlSafeBase64)
                                .build())
                        .build();

                resultStack.add(cell);
            } else if (resultEscaped.substring(0, resultEscaped.indexOf(",")).contains("stackEntrySlice")) {
//                TvmStackEntrySlice slice = gson.fromJson(resultEscaped, TvmStackEntrySlice.class);
                String urlSafeBase64 = StringUtils.substringBetween(resultEscaped, "bytes=", "}");
                TvmStackEntrySlice slice = TvmStackEntrySlice.builder()
                        .slice(TvmSlice.builder()
                                .bytes(urlSafeBase64)
                                .build())
                        .build();
                resultStack.add(slice);
            } else {
                throw new Error("Unknown type in TVM stack");
            }
        } // end for stack
        return RunResult.builder()
                .stackEntry(resultStack)
                .exit_code(exitCode)
                .gas_used(gasUsed)
                .extra(extra)
                .build();
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
