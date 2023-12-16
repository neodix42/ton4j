package org.ton.java.tonlib.types;

import com.jsoniter.JsonIterator;
import com.jsoniter.any.Any;
import org.apache.commons.codec.binary.Hex;
import org.ton.java.cell.Cell;
import org.ton.java.utils.Utils;

import java.util.*;

import static java.util.Objects.isNull;

public class ParseRunResult {
    /**
     * @param elementType - "num", "number", "int", "cell", "slice"
     * @param element     - define type, base64?
     * @return TvmStackEntry
     */
    private static final List<String> values = List.of("num", "number", "int", "cell",
            "slice", "tvm.cell", "tvm.slice", "list", "tuple");

    public static TvmStackEntry renderTvmElement(String elementType, String element) {
        try {
            String data;
            switch (elementType) {
                case "num":
                case "number":
                case "int":
                    return TvmStackEntryNumber.builder().number(TvmNumber.builder().number(element).build()).build();
                case "tvm.cell":
                case "cell":
                    data = Base64.getEncoder().encodeToString(Cell.fromBoc(Hex.decodeHex("cell")).toBoc(false));
                    return TvmStackEntryCell.builder().cell(TvmCell.builder().bytes(data).build()).build();
                case "tvm.slice":
                case "slice":
                    data = Base64.getEncoder().encodeToString(Cell.fromBoc(Hex.decodeHex("cell")).toBoc(false));
                    return TvmStackEntrySlice.builder().slice(TvmSlice.builder().bytes(data).build()).build();
                case "list":
                case "tuple":
                    return TvmStackEntrySlice.builder().build();
                default:
                    throw new Error("Rendering of type " + elementType + " is not implemented");
            }
        }
        catch (Exception e){
            throw new Error("Strange error has occured!");
        }
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

    public static TvmStackEntry parseJsonElement(Any inputElement) {
        Map<String, Any> t = inputElement.asMap();

        String key1 = t.keySet().toArray(String[]::new)[0];
        Any val1 = t.get(key1);
        String key2 = t.keySet().toArray(String[]::new)[1];
        Any val2 = t.get(key2);

        switch (key2) {
            case "number":
                return inputElement.as(TvmStackEntryNumber.class);
            case "cell":
                return inputElement.as(TvmStackEntryCell.class);
            case "slice":
                return inputElement.as(TvmStackEntrySlice.class);
            case "tuple":
                return (TvmStackEntryTuple) parseJsonElement(val2);
            case "list":
                return (TvmStackEntryList) parseJsonElement(val2);
            case "elements":
                List<Any> elements = val2.asList();
                List<Any> resultElements = new ArrayList<>();
                for (Any element : elements) {
                    Map<String, Any> tt = element.asMap();
                    Any v1 = tt.get(tt.keySet().toArray(String[]::new)[0]);
                    Any v2 = tt.get(tt.keySet().toArray(String[]::new)[1]);
                    switch (v1.toString()) {
                        case "tvm.stackEntryNumber":
                            TvmNumber number = v2.as(TvmNumber.class);
                            resultElements.add(Any.wrap(TvmStackEntryNumber.builder()
                                    .number(number)
                                    .build()));
                        case "tvm.stackEntryCell":
                            TvmCell cell = v2.as(TvmCell.class);
                            resultElements.add(Any.wrap(TvmStackEntryCell.builder()
                                    .cell(cell)
                                    .build()));
                        case "tvm.stackEntrySlice":
                            TvmSlice slice = v2.as(TvmSlice.class);
                            resultElements.add(Any.wrap(TvmStackEntrySlice.builder()
                                    .slice(slice)
                                    .build()));
                        case "tuple":
                            resultElements.add(Any.wrap(parseJsonElement(val2)));
                        case "tvm.stackEntryTuple":
                            resultElements.add(Any.wrap(parseJsonElement(v2)));
                        case "list":
                            resultElements.add(Any.wrap(parseJsonElement(val2)));
                        case "tvm.stackEntryList":
                            resultElements.add(Any.wrap(parseJsonElement(v2)));
                        default:
                    }

                    switch (val1.toString()) {
                        case "tvm.tuple":
                            TvmTuple tvmTuple = TvmTuple.builder()
                                    .elements(resultElements)
                                    .build();
                            return TvmStackEntryTuple.builder()
                                    .tuple(tvmTuple)
                                    .build();
                        case "tvm.list":
                            TvmList tvmList = TvmList.builder()
                                    .elements(resultElements)
                                    .build();
                            return TvmStackEntryList.builder()
                                    .list(tvmList)
                                    .build();
                        default:
                    }
                }
            default:
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

    public static Any parseTvmEntryListStack(TvmStackEntryList list) {
        List<Any> els = new ArrayList<>();
        for (Any any : list.getList().getElements()) {
            els.add(Any.wrap(parseJsonElement(any)));
        }
        TvmStackEntryList listResult = TvmStackEntryList.builder().build();
        TvmList tvmList = TvmList.builder().build();
        tvmList.setElements(els);
        listResult.setList(tvmList);
        return Any.wrap(listResult);
    }

    public static TvmStackEntryTuple parseTvmEntryTupleStack(TvmStackEntryTuple list) {
        List<Any> els = new ArrayList<>();
        for (Any o : list.getTuple().getElements()) {
            els.add(Any.wrap(parseJsonElement(o)));
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
                TvmStackEntryList list = JsonIterator.deserialize(resultEscaped, TvmStackEntryList.class);
                list = parseTvmEntryListStack(list).as(TvmStackEntryList.class);
                resultStack.add(list);
            } else if (resultEscaped.substring(0, resultEscaped.indexOf(",")).contains("stackEntryTuple")) {
                TvmStackEntryTuple tuple = JsonIterator.deserialize(resultEscaped, TvmStackEntryTuple.class);
                tuple = parseTvmEntryTupleStack(tuple);
                resultStack.add(tuple);
            } else if (resultEscaped.substring(0, resultEscaped.indexOf(",")).contains("stackEntryNumber")) {
                TvmStackEntryNumber number = JsonIterator.deserialize(resultEscaped, TvmStackEntryNumber.class);
                resultStack.add(number);
            } else if (resultEscaped.substring(0, resultEscaped.indexOf(",")).contains("stackEntryCell")) {
                TvmStackEntryCell cell = JsonIterator.deserialize(resultEscaped, TvmStackEntryCell.class);
                resultStack.add(cell);
            } else if (resultEscaped.substring(0, resultEscaped.indexOf(",")).contains("stackEntrySlice")) {
                TvmStackEntrySlice slice = JsonIterator.deserialize(resultEscaped, TvmStackEntrySlice.class);
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
