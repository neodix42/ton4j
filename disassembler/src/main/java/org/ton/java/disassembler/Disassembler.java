package org.ton.java.disassembler;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import org.apache.commons.lang3.StringUtils;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.CellSlice;
import org.ton.java.cell.TonHashMap;
import org.ton.java.disassembler.codepages.CP0Auto;
import org.ton.java.disassembler.consts.KnownMethods;
import org.ton.java.disassembler.structs.Codepage;

public class Disassembler {

    private static String repeatSpaces(int count) {
        return new String(new char[count]).replace('\0', ' ');
    }

    public static String decompile(CellSlice cs, Integer indent) {
        StringBuilder result = new StringBuilder();
        StringBuilder opCode = new StringBuilder();
        Codepage cp = new CP0Auto();

        while (cs.getRestBits() > 0) {
            boolean opCodePart = cs.loadBit();
            opCode.append(opCodePart ? "1" : "0");

            List<String> matches = cp.find(opCode.toString(), 2);
            if (matches.size() > 1) {
                continue;
            }
            if (matches.size() == 1 && opCode.length() != matches.get(0).length()) {
                continue;
            }
            if (matches.isEmpty()) {
                CellBuilder fullCell = CellBuilder.beginCell();
                for (char bit : opCode.toString().toCharArray()) {
                    fullCell.storeBit(bit == '1');
                }
                fullCell.storeSlice(cs);
                populateResult(result, fullCell.endCell().toString(), indent != null ? indent : 0);
                continue;
            }

            Object op = cp.getOp(opCode.toString());
            opCode.setLength(0);
            if (op instanceof String) {
                populateResult(result, (String) op, indent != null ? indent : 0);
            } else if (op instanceof BiFunction) {
                String opTxt = ((BiFunction<CellSlice, Integer, String>) op).apply(cs, indent != null ? indent : 0);
                populateResult(result, opTxt, indent != null ? indent : 0);
            }

            if (cs.getRestBits() == 0 && cs.getRefsCount() > 0) {
                cs = CellSlice.beginParse(cs.loadRef());
            }
        }

        return result.toString();
    }

    public static String decompileMethodsMap(CellSlice cs, int keyLen, Integer indent) {
        TonHashMap methodsMap = cs.loadDict(keyLen,
                k -> k.readInt(keyLen).intValue(),
                v -> v
        );
        Map<Integer, String> methodsMapDecompiled = new LinkedHashMap<>();

        int actualIndent = indent == null ? 0 : indent;

        for (Map.Entry<Object, Object> entry : methodsMap.elements.entrySet()) {
            Integer key = (Integer) entry.getKey();
            Cell val = (Cell) entry.getValue();
            try {
                methodsMapDecompiled.put(key, decompile(CellSlice.beginParse(val), actualIndent + 4));
            } catch (Exception e) {
                System.err.println(e.getMessage());
                String spacesToAdd = repeatSpaces(actualIndent != 0 ? actualIndent : 4);
                String errVal = val.toString() + spacesToAdd;
                methodsMapDecompiled.put(key, errVal);
            }
        }

        StringBuilder result = new StringBuilder();

        populateResult(result, "(:methods", actualIndent);
        actualIndent += 2;

        for (Map.Entry<Integer, String> entry : methodsMapDecompiled.entrySet()) {
            Integer methodId = entry.getKey();
            String code = entry.getValue();
            String methodName = KnownMethods.METHODS.get(methodId) == null
                    ? String.valueOf(methodId)
                    : KnownMethods.METHODS.get(methodId);
            populateResult(result, String.format("%s:\n%s", methodName, code), actualIndent);
        }

        if (StringUtils.isNotEmpty(result)) {
            result.setLength(result.length() - 1); // remove trailing newline
        }

        actualIndent -= 2;
        populateResult(result, ")", actualIndent);

        if (StringUtils.isNotEmpty(result)) {
            result.setLength(result.length() - 1); // remove trailing newline
        }

        return result.toString();
    }

    private static void populateResult(StringBuilder result, String txt, int indent) {
        result.append(repeatSpaces(indent)).append(txt).append("\n");
    }

    public static String fromCode(Cell cell) {
        CellSlice cs = CellSlice.beginParse(cell);
        long magic = cs.loadUint(16).longValue();
        assert (magic == 0xff00) : "Unsupported codepage, magic not equal to 0xff00, found 0x" + Long.toHexString(magic);

        return "SETCP0\n" + decompile(cs, null);
    }

    public static String fromBoc(byte[] boc) {
        Cell cell = Cell.fromBoc(boc);
        return fromCode(cell);
    }
}
