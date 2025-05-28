package org.ton.ton4j.disassembler.codepages;

import java.math.BigInteger;
import org.ton.ton4j.cell.CellSlice;
import org.ton.ton4j.disassembler.Disassembler;
import org.ton.ton4j.disassembler.structs.Codepage;

public class CP0Manual extends Codepage {

    public CP0Manual() {
        init();
    }

    private String repeatSpaces(int count) {
        return new String(new char[count]).replace('\0', ' ');
    }

    private void init() {
        this.insertHex("0", 4, (slice, indent) -> {
            BigInteger n = slice.loadUint(4);
            return n.equals(BigInteger.ZERO) ? "NOP" : String.format("s0 s%d XCHG", n);
        });

        this.insertHex("1", 4, (slice, indent) -> {
            BigInteger n = slice.loadUint(4);
            return String.format("s1 s%d XCHG", n.intValue());
        });

        this.insertHex("2", 4, (slice, indent) -> {
            BigInteger n = slice.loadUint(4);
            return String.format("s%d PUSH", n.intValue());
        });

        this.insertHex("3", 4, (slice, indent) -> {
            BigInteger value = slice.loadUint(4);
            return String.format("s%d POP", value.intValue());
        });

        this.insertHex("4", 4, (slice, indent) -> {
            BigInteger i = slice.loadUint(4);
            BigInteger j = slice.loadUint(4);
            BigInteger k = slice.loadUint(4);
            return String.format("s%d s%d s%d XCHG3", i.intValue(), j.intValue(), k.intValue());
        });

        this.insertHex("50", 8, (slice, indent) -> {
            BigInteger i = slice.loadUint(4);
            BigInteger j = slice.loadUint(4);
            return String.format("s%d s%d XCHG2", i.intValue(), j.intValue());
        });

        this.insertHex("51", 8, (slice, indent) -> {
            BigInteger i = slice.loadUint(4);
            BigInteger j = slice.loadUint(4);
            return String.format("s%d s%d XCPU", i.intValue(), j.intValue());
        });

        this.insertHex("52", 8, (slice, indent) -> {
            BigInteger i = slice.loadUint(4);
            BigInteger j = slice.loadUint(4);
            return String.format("s%d s%d PUXC", i.intValue(), j.subtract(BigInteger.ONE).intValue());
        });

        this.insertHex("53", 8, (slice, indent) -> {
            BigInteger args = slice.loadUint(8);
            BigInteger first = args.shiftRight(4).and(BigInteger.valueOf(0xf));
            BigInteger second = args.and(BigInteger.valueOf(0xf));
            return String.format("s%d s%d PUSH2", first.intValue(), second.intValue());
        });

        this.insertHex("541", 12, (slice, indent) -> {
            BigInteger i = slice.loadUint(4);
            BigInteger j = slice.loadUint(4);
            BigInteger k = slice.loadUint(4);
            return String.format("s%d s%d s%d XC2PU", i.intValue(), j.intValue(), k.intValue());
        });

        this.insertHex("59", 8, (slice, indent) -> "ROTREV");

        this.insertHex("7", 4, (slice, indent) -> {
            BigInteger x = slice.loadInt(4);
            return String.format("%d PUSHINT", x.intValue());
        });

        this.insertHex("80", 8, (slice, indent) -> {
            BigInteger x = slice.loadInt(8);
            return String.format("%d PUSHINT", x.intValue());
        });

        this.insertHex("81", 8, (slice, indent) -> {
            BigInteger x = slice.loadInt(16);
            return String.format("%d PUSHINT", x.intValue());
        });

        this.insertHex("82", 8, (slice, indent) -> {
            BigInteger len = slice.loadUint(5);
            BigInteger n = len.shiftLeft(3).add(BigInteger.valueOf(19));
            BigInteger x = slice.loadInt(n.intValue());
            return String.format("%d PUSHINT", x.intValue());
        });

        this.insertHex("83", 8, (slice, indent) -> {
            BigInteger x = slice.loadUint(8).add(BigInteger.ONE);
            return String.format("%d PUSHPOW2", x.intValue());
        });

        this.insertHex("84", 8, (slice, indent) -> {
            slice.skipBits(8);
            return "PUSHPOW2DEC";
        });

        this.insertHex("8D", 8, (slice, indent) -> {
            BigInteger r = slice.loadUint(3);
            BigInteger len = slice.loadUint(7);
            BigInteger dataLen = len.shiftLeft(3).add(BigInteger.valueOf(6));
            BigInteger x = slice.loadUint(dataLen.intValue());
            return String.format("%d %d %s PUSHSLICE", r.intValue(), len.intValue(), x.toString(16));
        });

        this.insertHex("8E", 7, (slice, indent) -> {
            BigInteger args = slice.loadUint(9);
            BigInteger refs = args.shiftRight(7).and(BigInteger.valueOf(3));
            BigInteger dataBits = args.and(BigInteger.valueOf(127)).shiftLeft(3);
            CellSlice cs = CellSlice.beginParse(slice.loadRef());
            String innerCode = Disassembler.decompile(cs, indent + 2);
            return String.format("<{\n%s%s}> PUSHCONT", innerCode, this.repeatSpaces(indent));
        });

        this.insertHex("9", 4, (slice, indent) -> {
            BigInteger len = slice.loadUint(4);
            String innerCode = Disassembler.decompile(slice, indent + 2);
            return String.format("<{\n%s%s}> PUSHCONT", innerCode, this.repeatSpaces(indent));
        });

        this.insertHex("A1", 8, (slice, indent) -> "SUB");
        this.insertHex("A4", 8, (slice, indent) -> "INC");

        this.insertHex("A9", 8, (slice, indent) -> {
            boolean m = slice.loadBit();
            BigInteger s = slice.loadUint(2);
            boolean c = slice.loadBit();
            BigInteger d = slice.loadUint(2);
            BigInteger f = slice.loadUint(2);
            StringBuilder opName = new StringBuilder();
            if (m) {
                opName.append("MUL");
            }
            if (s.equals(BigInteger.ZERO)) {
                opName.append("DIV");
            } else {
                if (s.equals(BigInteger.ONE)) {
                    opName.append("RSHIFT");
                } else {
                    opName.append("LSHIFT");
                }
                if (!c) {
                    opName.append(" s0");
                } else {
                    BigInteger shift = slice.loadUint(8).add(BigInteger.ONE);
                    opName.append(" ").append(shift.intValue());
                }
            }
            if (d.equals(BigInteger.ONE)) {
                opName.append(" QOUT");
            } else if (d.equals(BigInteger.valueOf(2))) {
                opName.append(" REM");
            } else if (d.equals(BigInteger.valueOf(3))) {
                opName.append(" BOTH");
            }
            if (f.equals(BigInteger.ONE)) {
                opName.append(" R");
            } else if (f.equals(BigInteger.valueOf(2))) {
                opName.append(" C");
            }
            return opName.toString();
        });

        this.insertHex("AA", 8, (slice, indent) -> {
            BigInteger cc = slice.loadUint(8);
            return String.format("%d LSHIFT", cc.intValue());
        });

        this.insertHex("AB", 8, (slice, indent) -> {
            BigInteger cc = slice.loadUint(8).add(BigInteger.ONE);
            return String.format("%d RSHIFT", cc.intValue());
        });

        this.insertHex("AE", 8, (slice, indent) -> "POW2");
        this.insertHex("B0", 8, (slice, indent) -> "AND");
        this.insertHex("B1", 8, (slice, indent) -> "OR");
        this.insertHex("B600", 16, (slice, indent) -> "FITSX");
        this.insertHex("B601", 16, (slice, indent) -> "UFITSX");
        this.insertHex("B8", 8, (slice, indent) -> "SGN");
        this.insertHex("B9", 8, (slice, indent) -> "LESS");
        this.insertHex("BA", 8, (slice, indent) -> "EQUAL");
        this.insertHex("BB", 8, (slice, indent) -> "LEQ");
        this.insertHex("BC", 8, (slice, indent) -> "GREATER");
        this.insertHex("BD", 8, (slice, indent) -> "NEQ");
        this.insertHex("BE", 8, (slice, indent) -> "GEQ");
        this.insertHex("BF", 8, (slice, indent) -> "CMP");

        this.insertHex("C0", 8, (slice, indent) -> {
            slice.skipBits(8);
            return "(todo) EQINT";
        });

        this.insertHex("C8", 8, (slice, indent) -> "NEWC");
        this.insertHex("C9", 8, (slice, indent) -> "ENDC");
        this.insertHex("CB", 8, (slice, indent) -> {
            BigInteger c = slice.loadUint(8);
            return String.format("%d STU", c.intValue());
        });

        this.insertHex("CC", 8, (slice, indent) -> "STREF");
        this.insertHex("CF16", 16, (slice, indent) -> "STSLICER");
        this.insertHex("D0", 8, (slice, indent) -> "CTOS");
        this.insertHex("D1", 8, (slice, indent) -> "ENDS");

        this.insertHex("D2", 8, (slice, indent) -> {
            BigInteger c = slice.loadUint(8).add(BigInteger.ONE);
            return String.format("%d LDI", c.intValue());
        });

        this.insertHex("D3", 8, (slice, indent) -> {
            BigInteger c = slice.loadUint(8).add(BigInteger.ONE);
            return String.format("%d LDU", c.intValue());
        });

        this.insertHex("D4", 8, (slice, indent) -> "LDREF");
        this.insertHex("D721", 16, (slice, indent) -> "SDSKIPFIRST");
        this.insertHex("D74C", 16, (slice, indent) -> "PLDREF");
        this.insertHex("D70B", 16, (slice, indent) -> {
            BigInteger c = slice.loadUint(8).add(BigInteger.ONE);
            return String.format("%d PLDU", c.intValue());
        });

        this.insertHex("D718", 16, (slice, indent) -> "LDSLICEX");
        this.insertHex("D74A", 16, (slice, indent) -> "SREFS");
        this.insertHex("DB3C", 16, (slice, indent) -> "CALLREF");
        this.insertHex("DD", 8, (slice, indent) -> "IFNOTRET");
        this.insertHex("E0", 8, (slice, indent) -> "IFJMP");
        this.insertHex("E2", 8, (slice, indent) -> "IFELSE");
        this.insertHex("E304", 16, (slice, indent) -> "CONDSEL");
        this.insertHex("E8", 8, (slice, indent) -> "WHILE");
        this.insertHex("ED44", 16, (slice, indent) -> "PUSHROOT");
        this.insertHex("ED54", 16, (slice, indent) -> "c4 POP");

        this.insertHex("F2C8", 13, (slice, indent) -> {
            slice.skipBits(11);
            return "THROWARG";
        });

        this.insertHex("F24", 10, (slice, indent) -> {
            BigInteger eCode = slice.loadUint(6);
            return String.format("%s THROWIF", eCode.toString());
        });

        this.insertHex("F28", 10, (slice, indent) -> {
            BigInteger eCode = slice.loadUint(6);
            return String.format("%s THROWIFNOT", eCode.toString());
        });

        this.insertHex("F2FF", 16, (slice, indent) -> "TRY");
        this.insertHex("F800", 16, (slice, indent) -> "ACCEPT");
        this.insertHex("F810", 16, (slice, indent) -> "RANDU256");
        this.insertHex("F823", 16, (slice, indent) -> "NOW");
        this.insertHex("F815", 16, (slice, indent) -> "ADDRAND");
        this.insertHex("F901", 16, (slice, indent) -> "HASHSU");
        this.insertHex("F910", 16, (slice, indent) -> "CHKSIGNU");
        this.insertHex("FB00", 16, (slice, indent) -> "SENDRAWMSG");
        this.insertHex("FA00", 16, (slice, indent) -> "LDGRAMS");
        this.insertHex("FF00", 16, (slice, indent) -> "SETCP0");

        this.insertBin("111101001010", (slice, indent) -> {
            boolean something = slice.loadBit();
            boolean something2 = slice.loadBit();
            BigInteger n = slice.loadUint(10);
            return String.format("%d DICTPUSHCONST", n.intValue());
        });

        this.insertHex("F4BC", 14, (slice, indent) -> {
            slice.skipBits(2);
            return "DICTIGETJMPZ";
        });
    }
}