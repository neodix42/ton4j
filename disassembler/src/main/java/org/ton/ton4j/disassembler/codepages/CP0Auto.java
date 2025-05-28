package org.ton.ton4j.disassembler.codepages;

import static org.ton.ton4j.disassembler.Disassembler.decompile;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellBuilder;
import org.ton.ton4j.cell.CellSlice;
import org.ton.ton4j.disassembler.Disassembler;
import org.ton.ton4j.disassembler.structs.Codepage;


public class CP0Auto extends Codepage {

    public CP0Auto() {
        init();
    }

    private Cell fetchSubSlice(CellSlice cs, int bits, int refs) {
        CellBuilder cb = CellBuilder.beginCell();
        for (int i = 0; i < bits; i++) {
            cb.storeBit(cs.loadBit());
        }
        for (int i = 0; i < refs; i++) {
            cb.storeRef(cs.loadRef());
        }

        return cb.endCell();
    }

    private String repeatSpaces(int count) {
        return new String(new char[count]).replace('\0', ' ');
    }

    private void init() {
        this.insertHex("0", 4, (slice, indent) -> {
            BigInteger n = slice.loadUint(4);
            if (n.equals(BigInteger.ZERO)) {
                return "NOP";
            }
            return String.format("s0 s%d XCHG", n.intValue());
        });

        this.insertHex("1", 4, (slice, indent) -> {
            BigInteger n = slice.loadUint(4);
            if (n.equals(BigInteger.ZERO)) {
                BigInteger i = slice.loadUint(4);
                BigInteger j = slice.loadUint(4);
                return String.format("s%d s%d XCHG", i.intValue(), j.intValue());
            }
            if (n.equals(BigInteger.ONE)) {
                BigInteger i = slice.loadUint(8);
                return String.format("s0 s%d XCHG", i.intValue());
            }
            return String.format("s1 s%d XCHG", n.intValue());
        });

        this.insertHex("2", 4, (slice, indent) -> {
            BigInteger n = slice.loadUint(4);
            return String.format("s%d PUSH", n.intValue());
        });

        this.insertHex("3", 4, (slice, indent) -> {
            BigInteger n = slice.loadUint(4);
            return String.format("s%d POP", n.intValue());
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

        this.insertHex("540", 12, (slice, indent) -> {
            BigInteger args = slice.loadUint(12);
            BigInteger first = args.shiftRight(8).and(BigInteger.valueOf(0xf));
            BigInteger second = args.shiftRight(4).and(BigInteger.valueOf(0xf));
            BigInteger third = args.and(BigInteger.valueOf(0xf));
            return String.format("s%d s%d s%d XCHG3", first.intValue(), second.intValue(), third.intValue());
        });

        this.insertHex("541", 12, (slice, indent) -> {
            BigInteger args = slice.loadUint(12);
            BigInteger i = args.shiftRight(8).and(BigInteger.valueOf(0xf));
            BigInteger j = args.shiftRight(4).and(BigInteger.valueOf(0xf));
            BigInteger k = args.and(BigInteger.valueOf(0xf));
            return String.format("%d %d %d XC2PU", i.intValue(), j.intValue(), k.intValue());
        });

        this.insertHex("542", 12, (slice, indent) -> {
            BigInteger args = slice.loadUint(12);
            BigInteger i = args.shiftRight(8).and(BigInteger.valueOf(0xf));
            BigInteger j = args.shiftRight(4).and(BigInteger.valueOf(0xf));
            BigInteger k = args.and(BigInteger.valueOf(0xf));
            return String.format("%d %d %d XCPUXC", i.intValue(), j.intValue(), k.subtract(BigInteger.ONE).intValue());
        });

        this.insertHex("543", 12, (slice, indent) -> {
            BigInteger args = slice.loadUint(12);
            BigInteger i = args.shiftRight(8).and(BigInteger.valueOf(0xf));
            BigInteger j = args.shiftRight(4).and(BigInteger.valueOf(0xf));
            BigInteger k = args.and(BigInteger.valueOf(0xf));
            return String.format("%d %d %d XCPU2", i.intValue(), j.intValue(), k.intValue());
        });

        this.insertHex("544", 12, (slice, indent) -> {
            BigInteger args = slice.loadUint(12);
            BigInteger i = args.shiftRight(8).and(BigInteger.valueOf(0xf));
            BigInteger j = args.shiftRight(4).and(BigInteger.valueOf(0xf));
            BigInteger k = args.and(BigInteger.valueOf(0xf));
            return String.format("%d %d %d PUXC2", i.intValue(), j.subtract(BigInteger.ONE).intValue(), k.subtract(BigInteger.ONE).intValue());
        });

        this.insertHex("545", 12, (slice, indent) -> {
            BigInteger args = slice.loadUint(12);
            BigInteger i = args.shiftRight(8).and(BigInteger.valueOf(0xf));
            BigInteger j = args.shiftRight(4).and(BigInteger.valueOf(0xf));
            BigInteger k = args.and(BigInteger.valueOf(0xf));
            return String.format("%d %d %d PUXCPU", i.intValue(), j.subtract(BigInteger.ONE).intValue(), k.subtract(BigInteger.ONE).intValue());
        });

        this.insertHex("546", 12, (slice, indent) -> {
            BigInteger args = slice.loadUint(12);
            BigInteger i = args.shiftRight(8).and(BigInteger.valueOf(0xf));
            BigInteger j = args.shiftRight(4).and(BigInteger.valueOf(0xf));
            BigInteger k = args.and(BigInteger.valueOf(0xf));
            return String.format("%d %d %d PU2XC", i.intValue(), j.subtract(BigInteger.ONE).intValue(), k.subtract(BigInteger.valueOf(2)).intValue());
        });

        this.insertHex("547", 12, (slice, indent) -> {
            BigInteger args = slice.loadUint(12);
            BigInteger i = args.shiftRight(8).and(BigInteger.valueOf(0xf));
            BigInteger j = args.shiftRight(4).and(BigInteger.valueOf(0xf));
            BigInteger k = args.and(BigInteger.valueOf(0xf));
            return String.format("%d %d %d PUSH3", i.intValue(), j.intValue(), k.intValue());
        });

        this.insertHex("55", 8, (slice, indent) -> {
            BigInteger args = slice.loadUint(8);
            BigInteger i = args.shiftRight(4).and(BigInteger.valueOf(0xf));
            BigInteger j = args.and(BigInteger.valueOf(0xf));
            return String.format("%d %d BLKSWAP", i.add(BigInteger.ONE).intValue(), j.add(BigInteger.ONE).intValue());
        });

        this.insertHex("56", 8, (slice, indent) -> {
            BigInteger args = slice.loadUint(8);
            return String.format("s%d PUSH", args.intValue());
        });

        this.insertHex("57", 8, (slice, indent) -> {
            BigInteger args = slice.loadUint(8);
            return String.format("s%d POP", args.intValue());
        });

        this.insertHex("58", 8, (slice, indent) -> "ROT");

        this.insertHex("59", 8, (slice, indent) -> "ROTREV");

        this.insertHex("5a", 8, (slice, indent) -> "2SWAP");

        this.insertHex("5b", 8, (slice, indent) -> "2DROP");

        this.insertHex("5c", 8, (slice, indent) -> "2DUP");

        this.insertHex("5d", 8, (slice, indent) -> "2OVER");

        this.insertHex("5e", 8, (slice, indent) -> {
            BigInteger args = slice.loadUint(8);
            BigInteger i = args.shiftRight(4).and(BigInteger.valueOf(0xf));
            BigInteger j = args.and(BigInteger.valueOf(0xf));
            return String.format("%d %d REVERSE", i.add(BigInteger.valueOf(2)).intValue(), j.intValue());
        });

        this.insertHex("5f", 8, (slice, indent) -> {
            BigInteger i = slice.loadUint(4);
            BigInteger j = slice.loadUint(4);
            if (i.equals(BigInteger.ZERO)) {
                return String.format("%d BLKDROP", j.intValue());
            }
            return String.format("%d %d BLKPUSH", i.intValue(), j.intValue());
        });

        this.insertHex("60", 8, (slice, indent) -> "PICK");
        this.insertHex("61", 8, (slice, indent) -> "ROLL");
        this.insertHex("62", 8, (slice, indent) -> "ROLLREV");
        this.insertHex("63", 8, (slice, indent) -> "BLKSWX");
        this.insertHex("64", 8, (slice, indent) -> "REVX");
        this.insertHex("65", 8, (slice, indent) -> "DROPX");
        this.insertHex("66", 8, (slice, indent) -> "TUCK");
        this.insertHex("67", 8, (slice, indent) -> "XCHGX");
        this.insertHex("68", 8, (slice, indent) -> "DEPTH");
        this.insertHex("69", 8, (slice, indent) -> "CHKDEPTH");
        this.insertHex("6a", 8, (slice, indent) -> "ONLYTOPX");
        this.insertHex("6b", 8, (slice, indent) -> "ONLYX");
        // 7077888 (DUMMY)
        this.insertHex("6c", 8, (slice, indent) -> {
            BigInteger i = slice.loadUint(4);
            BigInteger j = slice.loadUint(4);
            return String.format("%d %d BLKDROP2", i.intValue(), j.intValue());
        });

        this.insertHex("6d", 8, (slice, indent) -> "PUSHNULL");
        this.insertHex("6e", 8, (slice, indent) -> "ISNULL");

        this.insertHex("6f0", 12, (slice, indent) -> {
            BigInteger n = slice.loadUint(4);
            if (n.equals(BigInteger.ZERO)) {
                return "NIL";
            }
            if (n.equals(BigInteger.ONE)) {
                return "SINGLE";
            }
            if (n.equals(BigInteger.valueOf(2))) {
                return "PAIR";
            }
            if (n.equals(BigInteger.valueOf(3))) {
                return "TRIPLE";
            }
            return String.format("%d TUPLE", n.intValue());
        });
        this.insertHex("6f1", 12, (slice, indent) -> {
            BigInteger k = slice.loadUint(4);
            return String.format("%d INDEX", k.intValue());
        });
        this.insertHex("6f2", 12, (slice, indent) -> {
            BigInteger k = slice.loadUint(4);
            return String.format("%d UNTUPLE", k.intValue());
        });
        this.insertHex("6f3", 12, (slice, indent) -> {
            BigInteger k = slice.loadUint(4);
            if (k.equals(BigInteger.ZERO)) {
                return "CHKTUPLE";
            }
            return String.format("%d UNPACKFIRST", k.intValue());
        });
        this.insertHex("6f4", 12, (slice, indent) -> {
            BigInteger k = slice.loadUint(4);
            return String.format("%d EXPLODE", k.intValue());
        });
        this.insertHex("6f5", 12, (slice, indent) -> {
            BigInteger k = slice.loadUint(4);
            return String.format("%d SETINDEX", k.intValue());
        });
        this.insertHex("6f6", 12, (slice, indent) -> {
            BigInteger k = slice.loadUint(4);
            return String.format("%d INDEXQ", k.intValue());
        });
        this.insertHex("6f7", 12, (slice, indent) -> {
            BigInteger k = slice.loadUint(4);
            return String.format("%d SETINDEXQ", k.intValue());
        });
        this.insertHex("6f80", 16, (slice, indent) -> "TUPLEVAR");
        this.insertHex("6f81", 16, (slice, indent) -> "INDEXVAR");
        this.insertHex("6f82", 16, (slice, indent) -> "UNTUPLEVAR");
        this.insertHex("6f83", 16, (slice, indent) -> "UNPACKFIRSTVAR");
        this.insertHex("6f84", 16, (slice, indent) -> "EXPLODEVAR");
        this.insertHex("6f85", 16, (slice, indent) -> "SETINDEXVAR");
        this.insertHex("6f86", 16, (slice, indent) -> "INDEXVARQ");
        this.insertHex("6f87", 16, (slice, indent) -> "SETINDEXVARQ");
        this.insertHex("6f88", 16, (slice, indent) -> "TLEN");
        this.insertHex("6f89", 16, (slice, indent) -> "QTLEN");
        this.insertHex("6f8a", 16, (slice, indent) -> "ISTUPLE");
        this.insertHex("6f8b", 16, (slice, indent) -> "LAST");
        this.insertHex("6f8c", 16, (slice, indent) -> "TPUSH");
        this.insertHex("6f8d", 16, (slice, indent) -> "TPOP");
        // 7310848 (DUMMY)
        this.insertHex("6fa0", 16, (slice, indent) -> "NULLSWAPIF");
        this.insertHex("6fa1", 16, (slice, indent) -> "NULLSWAPIFNOT");
        this.insertHex("6fa2", 16, (slice, indent) -> "NULLROTRIF");
        this.insertHex("6fa3", 16, (slice, indent) -> "NULLROTRIFNOT");
        this.insertHex("6fa4", 16, (slice, indent) -> "NULLSWAPIF2");
        this.insertHex("6fa5", 16, (slice, indent) -> "NULLSWAPIFNOT2");
        this.insertHex("6fa6", 16, (slice, indent) -> "NULLROTRIF2");
        this.insertHex("6fa7", 16, (slice, indent) -> "NULLROTRIFNOT2");
        // 7317504 (DUMMY)
        this.insertHex("6fb", 12, (slice, indent) -> {
            BigInteger i = slice.loadUint(2);
            BigInteger j = slice.loadUint(2);
            return String.format("%d %d INDEX2", i.intValue(), j.intValue());
        });

        // this.insertHex("6fc", 10, (slice, indent) => {
        //     BigInteger i = slice.loadUint(2);
        //     BigInteger j = slice.loadUint(2);
        //     BigInteger k = slice.loadUint(2);
        //     return "String.format("%d %d %d INDEX3", i.intValue(), j.intValue(), k.intValue())";
        // });

        this.insertHex("7", 4, (slice, indent) -> {
            BigInteger args = slice.loadInt(4);
            return String.format("%d PUSHINT", args.intValue());
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
            BigInteger n = len.multiply(BigInteger.valueOf(8)).add(BigInteger.valueOf(19));
            BigInteger x = slice.loadInt(n.intValue());
            return String.format("%s PUSHINT", x.toString());
        });

        this.insertHex("83", 8, (slice, indent) -> {
            BigInteger x = slice.loadUint(8).add(BigInteger.ONE);
            return String.format("%d PUSHPOW2", x.intValue());
        });

        this.insertHex("84", 8, (slice, indent) -> {
            BigInteger x = slice.loadUint(8).add(BigInteger.ONE);
            return String.format("%d PUSHPOW2DEC", x.intValue());
        });

        this.insertHex("850000", 8, (slice, indent) -> {
            BigInteger x = slice.loadUint(8).add(BigInteger.ONE);
            return String.format("%d PUSHNEGPOW2", x.intValue());
        });

        // 8781824 (DUMMY)
        this.insertHex("88", 8, (slice, indent) -> "PUSHREF");
        this.insertHex("89", 8, (slice, indent) -> "PUSHREFSLICE");
        this.insertHex("8a", 8, (slice, indent) -> "PUSHREFCONT");

        this.insertHex("8b", 8, (slice, indent) -> {
            BigInteger x = slice.loadUint(4);
            BigInteger len = x.multiply(BigInteger.valueOf(8)).add(BigInteger.valueOf(4));
            Cell subslice = fetchSubSlice(slice, len.intValue(), 0);
            return "PUSHSLICE";
        });

        this.insertHex("8c0000", 8, (slice, indent) -> {
            BigInteger r = slice.loadUint(2).add(BigInteger.ONE);
            BigInteger xx = slice.loadUint(5);
            BigInteger len = xx.multiply(BigInteger.valueOf(8)).add(BigInteger.ONE);
            Cell subslice = fetchSubSlice(slice, len.intValue(), r.intValue());
            return "PUSHSLICE";
        });

        this.insertHex("8d", 8, (slice, indent) -> {
            BigInteger r = slice.loadUint(3);
            BigInteger xx = slice.loadUint(7);
            BigInteger len = xx.multiply(BigInteger.valueOf(8)).add(BigInteger.valueOf(6));
            Cell subslice = fetchSubSlice(slice, len.intValue(), r.intValue());
            return "PUSHSLICE";
        });

        // 9281536 (DUMMY)
        this.insertHex("8E", 7, (slice, indent) -> {
            BigInteger args = slice.loadUint(9);
            BigInteger refs = args.shiftRight(7).and(BigInteger.valueOf(3));
            BigInteger dataBytes = args.and(BigInteger.valueOf(127)).multiply(BigInteger.valueOf(8));

            Cell subslice = fetchSubSlice(slice, dataBytes.intValue(), refs.intValue());
            String spaces = this.repeatSpaces(indent);
            return String.format("<{%n%s%s}> PUSHCONT", decompile(CellSlice.beginParse(subslice), indent + 2), spaces);
        });

        this.insertHex("9", 4, (slice, indent) -> {
            BigInteger len = slice.loadUint(4).multiply(BigInteger.valueOf(8));
            Cell subslice = fetchSubSlice(slice, len.intValue(), 0);
            String spaces = this.repeatSpaces(indent);
            return String.format("<{%n%s%s}> PUSHCONT", decompile(CellSlice.beginParse(subslice), indent + 2), spaces);
        });

        this.insertHex("a00000", 8, (slice, indent) -> "ADD");
        this.insertHex("a10000", 8, (slice, indent) -> "SUB");
        this.insertHex("a20000", 8, (slice, indent) -> "SUBR");
        this.insertHex("a30000", 8, (slice, indent) -> "NEGATE");
        this.insertHex("a40000", 8, (slice, indent) -> "INC");
        this.insertHex("a50000", 8, (slice, indent) -> "DEC");
        this.insertHex("a60000", 8, (slice, indent) -> {
            BigInteger x = slice.loadInt(8);
            return String.format("%d ADDCONST", x);
        });

        this.insertHex("a70000", 8, (slice, indent) -> {
            BigInteger x = slice.loadInt(8);
            return String.format("%d MULCONST", x);
        });

        this.insertHex("a80000", 8, (slice, indent) -> "MUL");
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
                    opName = new StringBuilder("RSHIFT");
                } else {
                    opName = new StringBuilder("LSHIFT");
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

        // 11079680 (DUMMY)
        // 11132928 (DUMMY)
        this.insertHex("aa", 8, (slice, indent) -> {
            BigInteger cc = slice.loadUint(8);
            return String.format("%d LSHIFT", cc.add(BigInteger.ONE));
        });

        this.insertHex("ab", 8, (slice, indent) -> {
            BigInteger cc = slice.loadUint(8);
            return String.format("%d RSHIFT", cc.add(BigInteger.ONE));
        });

        this.insertHex("ac", 8, (slice, indent) -> "LSHIFT");
        this.insertHex("ad", 8, (slice, indent) -> "RSHIFT");
        this.insertHex("ae", 8, (slice, indent) -> "POW2");

        // 11468800 (DUMMY)
        this.insertHex("b0", 8, (slice, indent) -> "AND");
        this.insertHex("b1", 8, (slice, indent) -> "OR");
        this.insertHex("b2", 8, (slice, indent) -> "XOR");
        this.insertHex("b3", 8, (slice, indent) -> "NOT");
        this.insertHex("b4", 8, (slice, indent) -> {
            BigInteger cc = slice.loadUint(8);
            return String.format("%d FITS", cc.add(BigInteger.ONE));
        });

        this.insertHex("b5", 8, (slice, indent) -> {
            BigInteger cc = slice.loadUint(8);
            return String.format("%d UFITS", cc.add(BigInteger.ONE));
        });

        this.insertHex("b600", 16, (slice, indent) -> "FITSX");
        this.insertHex("b601", 16, (slice, indent) -> "UFITSX");
        this.insertHex("b602", 16, (slice, indent) -> "BITSIZE");
        this.insertHex("b603", 16, (slice, indent) -> "UBITSIZE");
        // 11928576 (DUMMY)
        this.insertHex("b608", 16, (slice, indent) -> "MIN");
        this.insertHex("b609", 16, (slice, indent) -> "MAX");
        this.insertHex("b60a", 16, (slice, indent) -> "MINMAX");
        this.insertHex("b60b", 16, (slice, indent) -> "ABS");
        // 11930624 (DUMMY)
        this.insertHex("b7a0", 16, (slice, indent) -> "QADD");
        this.insertHex("b7a1", 16, (slice, indent) -> "QSUB");
        this.insertHex("b7a2", 16, (slice, indent) -> "QSUBR");
        this.insertHex("b7a3", 16, (slice, indent) -> "QNEGATE");
        this.insertHex("b7a4", 16, (slice, indent) -> "QINC");
        this.insertHex("b7a5", 16, (slice, indent) -> "QDEC");
        this.insertHex("b7a6", 16, (slice, indent) -> {
            BigInteger x = slice.loadInt(8);
            return String.format("%s QADDCONST", x.intValue());
        });
        this.insertHex("b7a7", 16, (slice, indent) -> {
            BigInteger x = slice.loadInt(8);
            return String.format("%s QMULCONST", x.intValue());
        });
        this.insertHex("b7a8", 16, (slice, indent) -> "QMUL");
        this.insertHex("b7a9", 16, (slice, indent) -> {
            boolean m = slice.loadBit();
            BigInteger s = slice.loadUint(2);
            boolean c = slice.loadBit();
            BigInteger d = slice.loadUint(2);
            BigInteger f = slice.loadUint(2);
            StringBuilder opName = new StringBuilder("Q");
            if (m) {
                opName.append("MUL");
            }
            if (s.equals(BigInteger.ZERO)) {
                opName.append("DIV");
            } else {
                if (s.equals(BigInteger.ONE)) {
                    opName = new StringBuilder("RSHIFT");
                } else {
                    opName = new StringBuilder("LSHIFT");
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
        // 12036560 (DUMMY)
        this.insertHex("b7aa", 16, (slice, indent) -> {
            BigInteger cc = slice.loadUint(8);
            return String.format("%s QLSHIFT", cc.add(BigInteger.ONE).intValue());
        });
        this.insertHex("b7ab", 16, (slice, indent) -> {
            BigInteger cc = slice.loadUint(8);
            return String.format("%s QLSHIFT", cc.add(BigInteger.ONE).intValue());
        });
        this.insertHex("b7ac", 16, (slice, indent) -> "QLSHIFT");
        this.insertHex("b7ad", 16, (slice, indent) -> "QRSHIFT");
        this.insertHex("b7ae", 16, (slice, indent) -> "QPOW2");
        // 12037888 (DUMMY)
        this.insertHex("b7b0", 16, (slice, indent) -> "QAND");
        this.insertHex("b7b1", 16, (slice, indent) -> "QOR");
        this.insertHex("b7b2", 16, (slice, indent) -> "QXOR");
        this.insertHex("b7b3", 16, (slice, indent) -> "QNOT");
        this.insertHex("b7b4", 16, (slice, indent) -> {
            BigInteger cc = slice.loadUint(8);
            return String.format("%s QFITS", cc.add(BigInteger.ONE).intValue());
        });
        this.insertHex("b7b5", 16, (slice, indent) -> {
            BigInteger cc = slice.loadUint(8);
            return String.format("%s QUFITS", cc.add(BigInteger.ONE).intValue());
        });
        this.insertHex("b7b600", 24, (slice, indent) -> "QFITSX");
        this.insertHex("b7b601", 24, (slice, indent) -> "QUFITSX");
        this.insertHex("b7b602", 24, (slice, indent) -> "QBITSIZE");
        this.insertHex("b7b603", 24, (slice, indent) -> "QUBITSIZE");
        this.insertHex("b7b608", 24, (slice, indent) -> "QMIN");
        this.insertHex("b7b609", 24, (slice, indent) -> "QMAX");
        this.insertHex("b7b60a", 24, (slice, indent) -> "QMINMAX");
        this.insertHex("b7b60b", 24, (slice, indent) -> "QABS");
        this.insertHex("b7b8", 16, (slice, indent) -> "QSGN");
        this.insertHex("b7b9", 16, (slice, indent) -> "QLESS");
        this.insertHex("b7ba", 16, (slice, indent) -> "QEQUAL");
        this.insertHex("b7bb", 16, (slice, indent) -> "QLEQ");
        this.insertHex("b7bc", 16, (slice, indent) -> "QGREATER");
        this.insertHex("b7bd", 16, (slice, indent) -> "QNEQ");
        this.insertHex("b7be", 16, (slice, indent) -> "QGEQ");
        this.insertHex("b7bf", 16, (slice, indent) -> "QCMP");
        this.insertHex("b7c0", 16, (slice, indent) -> {
            BigInteger x = slice.loadInt(8);
            return String.format("%s QEQINT", x.intValue());
        });
        this.insertHex("b7c1", 16, (slice, indent) -> {
            BigInteger x = slice.loadInt(8);
            return String.format("%d QLESSINT", x.intValue());
        });
        this.insertHex("b7c2", 16, (slice, indent) -> {
            BigInteger x = slice.loadInt(8);
            return String.format("%d QGTINT", x.intValue());
        });
        this.insertHex("b7c3", 16, (slice, indent) -> {
            BigInteger x = slice.loadInt(8);
            return String.format("%d QNEQINT", x.intValue());
        });
        // 12043264 (DUMMY)
        this.insertHex("b8", 8, (slice, indent) -> "SGN");
        this.insertHex("b9", 8, (slice, indent) -> "LESS");
        this.insertHex("ba", 8, (slice, indent) -> "EQUAL");
        this.insertHex("bb", 8, (slice, indent) -> "LEQ");
        this.insertHex("bc", 8, (slice, indent) -> "GREATER");
        this.insertHex("bd", 8, (slice, indent) -> "NEQ");
        this.insertHex("be", 8, (slice, indent) -> "GEQ");
        this.insertHex("bf", 8, (slice, indent) -> "CMP");
        this.insertHex("c0", 8, (slice, indent) -> {
            BigInteger x = slice.loadInt(8);
            return String.format("%d EQINT", x.intValue());
        });
        this.insertHex("c1", 8, (slice, indent) -> {
            BigInteger x = slice.loadInt(8);
            return String.format("%d LESSINT", x.intValue());
        });
        this.insertHex("c2", 8, (slice, indent) -> {
            BigInteger x = slice.loadInt(8);
            return String.format("%d GTINT", x.intValue());
        });
        this.insertHex("c3", 8, (slice, indent) -> {
            BigInteger x = slice.loadInt(8);
            return String.format("%d NEQINT", x.intValue());
        });
        this.insertHex("c4", 8, (slice, indent) -> "ISNAN");
        this.insertHex("c5", 8, (slice, indent) -> "CHKNAN");
        // 12976128 (DUMMY)
        this.insertHex("c700", 16, (slice, indent) -> "SEMPTY");
        this.insertHex("c701", 16, (slice, indent) -> "SDEMPTY");
        this.insertHex("c702", 16, (slice, indent) -> "SREMPTY");
        this.insertHex("c703", 16, (slice, indent) -> "SDFIRST");
        this.insertHex("c704", 16, (slice, indent) -> "SDLEXCMP");
        this.insertHex("c705", 16, (slice, indent) -> "SDEQ");
        // 13043200 (DUMMY)
        this.insertHex("c708", 16, (slice, indent) -> "SDPFX");
        this.insertHex("c709", 16, (slice, indent) -> "SDPFXREV");
        this.insertHex("c70a", 16, (slice, indent) -> "SDPPFX");
        this.insertHex("c70b", 16, (slice, indent) -> "SDPPFXREV");
        this.insertHex("c70c", 16, (slice, indent) -> "SDSFX");
        this.insertHex("c70d", 16, (slice, indent) -> "SDSFXREV");
        this.insertHex("c70e", 16, (slice, indent) -> "SDPSFX");
        this.insertHex("c70f", 16, (slice, indent) -> "SDPSFXREV");
        this.insertHex("c710", 16, (slice, indent) -> "SDCNTLEAD0");
        this.insertHex("c711", 16, (slice, indent) -> "SDCNTLEAD1");
        this.insertHex("c712", 16, (slice, indent) -> "SDCNTTRAIL0");
        this.insertHex("c713", 16, (slice, indent) -> "SDCNTTRAIL1");
        // 13046784 (DUMMY)
        this.insertHex("c8", 8, (slice, indent) -> "NEWC");
        this.insertHex("c9", 8, (slice, indent) -> "ENDC");
        this.insertHex("ca", 8, (slice, indent) -> {
            BigInteger cc = slice.loadUint(8).add(BigInteger.ONE);
            return String.format("%d STI", cc.intValue());
        });
        this.insertHex("cb", 8, (slice, indent) -> {
            BigInteger cc = slice.loadUint(8).add(BigInteger.ONE);
            return String.format("%d STU", cc.intValue());
        });
        this.insertHex("cc", 8, (slice, indent) -> "STREF");
        this.insertHex("cd", 8, (slice, indent) -> "ENDCST");
        this.insertHex("ce", 8, (slice, indent) -> "STSLICE");
        this.insertHex("cf00", 13, (slice, indent) -> {
            BigInteger args = slice.loadUint(3);
            boolean sgnd = args.testBit(0);
            StringBuilder s = new StringBuilder("ST")
                    .append(sgnd ? "I" : "U")
                    .append("X");
            if (args.testBit(1)) {
                s.append("R");
            }
            if (args.testBit(2)) {
                s.append("Q");
            }
            return s.toString();
        });
        this.insertHex("cf08", 13, (slice, indent) -> {
            BigInteger args = slice.loadUint(11);
            int bits = (args.intValue() & 0xff) + 1;
            boolean sgnd = (args.intValue() & 0x100) != 0x100;
            StringBuilder s = new StringBuilder("ST");
            s.append(sgnd ? 'I' : 'U');
            if ((args.intValue() & 0x200) == 0x200) {
                s.append('R');
            }
            if ((args.intValue() & 0x400) == 0x400) {
                s.append('Q');
            }
            return String.format("%d %s", bits, s);
        });
        this.insertHex("cf10", 16, (slice, indent) -> "STREF");
        this.insertHex("cf11", 16, (slice, indent) -> "STBREF");
        this.insertHex("cf12", 16, (slice, indent) -> "STSLICE");
        this.insertHex("cf13", 16, (slice, indent) -> "STB");
        this.insertHex("cf14", 16, (slice, indent) -> "STREFR");
        this.insertHex("cf15", 16, (slice, indent) -> "STBREFR");
        this.insertHex("cf16", 16, (slice, indent) -> "STSLICER");
        this.insertHex("cf17", 16, (slice, indent) -> "STBR");
        this.insertHex("cf18", 16, (slice, indent) -> "STREFQ");
        this.insertHex("cf19", 16, (slice, indent) -> "STBREFQ");
        this.insertHex("cf1a", 16, (slice, indent) -> "STSLICEQ");
        this.insertHex("cf1b", 16, (slice, indent) -> "STBQ");
        this.insertHex("cf1c", 16, (slice, indent) -> "STREFRQ");
        this.insertHex("cf1d", 16, (slice, indent) -> "STBREFRQ");
        this.insertHex("cf1e", 16, (slice, indent) -> "STSLICERQ");
        this.insertHex("cf1f", 16, (slice, indent) -> "STBRQ");
        this.insertHex("cf20", 15, (slice, indent) -> {
            boolean flag = slice.loadBit();
            return flag ? "STREFCONST" : "STREF2CONST";
        });
        // 13574656 (DUMMY)
        this.insertHex("cf23", 16, (slice, indent) -> "ENDXC");
        // 13575168 (DUMMY)
        this.insertHex("cf28", 14, (slice, indent) -> {
            BigInteger args = slice.loadUint(2);
            boolean sgnd = !args.testBit(0);
            return "ST" + (sgnd ? "I" : "U") + "LE" + (args.testBit(1) ? "8" : "4");
        });
        // 13577216 (DUMMY)
        this.insertHex("cf30", 16, (slice, indent) -> "BDEPTH");
        this.insertHex("cf31", 16, (slice, indent) -> "BBITS");
        this.insertHex("cf32", 16, (slice, indent) -> "BREFS");
        this.insertHex("cf33", 16, (slice, indent) -> "BBITREFS");
        // 13579264 (DUMMY)
        this.insertHex("cf35", 16, (slice, indent) -> "BREMBITS");
        this.insertHex("cf36", 16, (slice, indent) -> "BREMREFS");
        this.insertHex("cf37", 16, (slice, indent) -> "BREMBITREFS");
        this.insertHex("cf38", 16, (slice, indent) -> {
            BigInteger cc = slice.loadUint(8);
            return String.format("%d BCHKBITS", cc.add(BigInteger.ONE).intValue());
        });
        this.insertHex("cf39", 16, (slice, indent) -> "BCHKBITS");
        this.insertHex("cf3a", 16, (slice, indent) -> "BCHKREFS");
        this.insertHex("cf3b", 16, (slice, indent) -> "BCHKBITREFS");
        this.insertHex("cf3c", 16, (slice, indent) -> {
            BigInteger cc = slice.loadUint(8);
            return String.format("%d BCHKBITSQ", cc.add(BigInteger.ONE).intValue());
        });
        this.insertHex("cf3d", 16, (slice, indent) -> "BCHKBITSQ");
        this.insertHex("cf3e", 16, (slice, indent) -> "BCHKREFSQ");
        this.insertHex("cf3f", 16, (slice, indent) -> "BCHKBITREFSQ");
        this.insertHex("cf40", 16, (slice, indent) -> "STZEROES");
        this.insertHex("cf41", 16, (slice, indent) -> "STONES");
        this.insertHex("cf42", 16, (slice, indent) -> "STSAME");
        // 13583104 (DUMMY)
        this.insertHex("cf8", 9, (slice, indent) -> {
            BigInteger refs = slice.loadUint(2);
            BigInteger dataBits = slice.loadUint(3).multiply(BigInteger.valueOf(8)).add(BigInteger.ONE);
            fetchSubSlice(slice, dataBits.intValue(), refs.intValue());
            return "STSLICECONST";
        });
        this.insertHex("d0", 8, (slice, indent) -> "CTOS");
        this.insertHex("d1", 8, (slice, indent) -> "ENDS");
        this.insertHex("d2", 8, (slice, indent) -> {
            BigInteger cc = slice.loadUint(8);
            return String.format("%d LDI", cc.add(BigInteger.ONE).intValue());
        });
        this.insertHex("d3", 8, (slice, indent) -> {
            BigInteger cc = slice.loadUint(8);
            return String.format("%d LDU", cc.add(BigInteger.ONE).intValue());
        });
        this.insertHex("d4", 8, (slice, indent) -> "LDREF");
        this.insertHex("d5", 8, (slice, indent) -> "LDREFRTOS");
        this.insertHex("d6", 8, (slice, indent) -> {
            BigInteger cc = slice.loadUint(8);
            return String.format("%d LDSLICE", cc.add(BigInteger.ONE).intValue());
        });
        this.insertHex("d70", 12, (slice, indent) -> {
            boolean longerVersion = slice.loadBit();
            boolean quiet = slice.loadBit();
            boolean preload = slice.loadBit();
            boolean sign = slice.loadBit();
            StringBuilder s = new StringBuilder();
            if (longerVersion) {
                BigInteger length = slice.loadUint(8).add(BigInteger.ONE);
                s.append(length.intValue()).append(" ");
            }
            s.append(preload ? "PLD" : "LD")
                    .append(sign ? "U" : "I")
                    .append(quiet ? "Q" : "");
            return s.toString();
        });
        this.insertHex("d710", 13, (slice, indent) -> {
            BigInteger c = slice.loadUint(3).add(BigInteger.ONE);
            return String.format("%d PLDUZ", 32 * (c.intValue() + 1));
        });
        this.insertHex("d718", 14, (slice, indent) -> {
            boolean quiet = slice.loadBit();
            boolean preload = slice.loadBit();
            return (preload ? "PLD" : "LD") + "SLICEX" + (quiet ? "Q" : "");
        });
        this.insertHex("d71c", 14, (slice, indent) -> {
            boolean quiet = slice.loadBit();
            boolean preload = slice.loadBit();
            BigInteger cc = slice.loadUint(8);
            return String.format("%d %sSLICEX%s", cc.add(BigInteger.ONE).intValue(),
                    preload ? "PLD" : "LD", quiet ? "Q" : "");
        });
        this.insertHex("d720", 16, (slice, indent) -> "SDCUTFIRST");
        this.insertHex("d721", 16, (slice, indent) -> "SDSKIPFIRST");
        this.insertHex("d722", 16, (slice, indent) -> "SDCUTLAST");
        this.insertHex("d723", 16, (slice, indent) -> "SDSKIPLAST");
        this.insertHex("d724", 16, (slice, indent) -> "SDSUBSTR");
        // 14099712 (DUMMY)
        this.insertHex("d726", 16, (slice, indent) -> "SDBEGINSX");
        this.insertHex("d727", 16, (slice, indent) -> "SDBEGINSXQ");
        this.insertHex("d728", 13, (slice, indent) -> {
            slice.loadUint(8);
            return "SDBEGINS";
        });
        this.insertHex("d730", 16, (slice, indent) -> "SCUTFIRST");
        this.insertHex("d731", 16, (slice, indent) -> "SSKIPFIRST");
        this.insertHex("d732", 16, (slice, indent) -> "SCUTLAST");
        this.insertHex("d733", 16, (slice, indent) -> "SSKIPLAST");
        this.insertHex("d734", 16, (slice, indent) -> "SUBSLICE");
        // 14103808 (DUMMY)
        this.insertHex("d736", 16, (slice, indent) -> "SPLIT");
        this.insertHex("d737", 16, (slice, indent) -> "SPLITQ");
        // 14104576 (DUMMY)
        this.insertHex("d739", 16, (slice, indent) -> "XCTOS");
        this.insertHex("d73a", 16, (slice, indent) -> "XLOAD");
        this.insertHex("d73b", 16, (slice, indent) -> "XLOADQ");
        // 14105600 (DUMMY)
        this.insertHex("d741", 16, (slice, indent) -> "SCHKBITS");
        this.insertHex("d742", 16, (slice, indent) -> "SCHKREFS");
        this.insertHex("d743", 16, (slice, indent) -> "SCHKBITREFS");
        // 14107648 (DUMMY)
        this.insertHex("d745", 16, (slice, indent) -> "SCHKBITSQ");
        this.insertHex("d746", 16, (slice, indent) -> "SCHKREFSQ");
        this.insertHex("d747", 16, (slice, indent) -> "SCHKBITREFSQ");
        this.insertHex("d748", 16, (slice, indent) -> "PLDREFVAR");
        this.insertHex("d749", 16, (slice, indent) -> "SBITS");
        this.insertHex("d74a", 16, (slice, indent) -> "SREFS");
        this.insertHex("d74b", 16, (slice, indent) -> "SBITREFS");
        this.insertHex("d74c", 14, (slice, indent) -> {
            BigInteger n = slice.loadUint(2);
            return String.format("%d PLDREFIDX", n.intValue());
        });
        this.insertHex("d750", 12, (slice, indent) -> {
            boolean quiet = slice.loadBit();
            boolean preload = slice.loadBit();
            boolean bit64 = slice.loadBit();
            boolean unsigned = slice.loadBit();
            return String.format("%s%s%s%s",
                    preload ? "PLD" : "LD",
                    unsigned ? "U" : "I",
                    "LE" + (bit64 ? "8" : "4"),
                    quiet ? "Q" : "");
        });
        this.insertHex("d760", 16, (slice, indent) -> "LDZEROES");
        this.insertHex("d761", 16, (slice, indent) -> "LDONES");
        this.insertHex("d762", 16, (slice, indent) -> "LDSAME");
        // 14115584 (DUMMY)
        this.insertHex("d764", 16, (slice, indent) -> "SDEPTH");
        this.insertHex("d765", 16, (slice, indent) -> "CDEPTH");
        // 14116352 (DUMMY)
        this.insertHex("d8", 8, (slice, indent) -> "EXECUTE");
        this.insertHex("d9", 8, (slice, indent) -> "JMPX");
        this.insertHex("da", 8, (slice, indent) -> {
            BigInteger p = slice.loadUint(4);
            BigInteger r = slice.loadUint(4);
            return String.format("%d %d CALLXARGS", p.intValue(), r.intValue());
        });
        this.insertHex("db0", 12, (slice, indent) -> {
            BigInteger p = slice.loadUint(4);
            return String.format("%d CALLXARGS", p.intValue());
        });
        this.insertHex("db1", 12, (slice, indent) -> {
            BigInteger p = slice.loadUint(4);
            return String.format("%d JMPXARGS", p.intValue());
        });
        this.insertHex("db2", 12, (slice, indent) -> {
            BigInteger r = slice.loadUint(4);
            return String.format("%d RETARGS", r.intValue());
        });
        this.insertHex("db30", 16, (slice, indent) -> "RET");
        this.insertHex("db31", 16, (slice, indent) -> "RETALT");
        this.insertHex("db32", 16, (slice, indent) -> "RETBOOL");
        // 14365440 (DUMMY)
        this.insertHex("db34", 16, (slice, indent) -> "CALLCC");
        this.insertHex("db35", 16, (slice, indent) -> "JMPXDATA");
        this.insertHex("db36", 16, (slice, indent) -> {
            BigInteger p = slice.loadUint(4);
            BigInteger r = slice.loadUint(4);
            return String.format("%d %d CALLCCARGS", p.intValue(), r.intValue());
        });
        // 14366464 (DUMMY)
        this.insertHex("db38", 16, (slice, indent) -> "CALLXVARARGS");
        this.insertHex("db39", 16, (slice, indent) -> "RETVARARGS");
        this.insertHex("db3a", 16, (slice, indent) -> "JMPXVARARGS");
        this.insertHex("db3b", 16, (slice, indent) -> "CALLCCVARARGS");
        this.insertHex("db3c", 16, (slice, indent) -> {
            CellSlice subslice = CellSlice.beginParse(slice.loadRef());
            String spaces = this.repeatSpaces(indent);
            return String.format("<{\n%s%s}> CALLREF", decompile(subslice, indent + 2), spaces);
        });
        this.insertHex("db3d", 16, (slice, indent) -> {
            CellSlice subslice = CellSlice.beginParse(slice.loadRef());
            String spaces = this.repeatSpaces(indent);
            return String.format("<{\n%s%s}> JMPREF", decompile(subslice, indent + 2), spaces);
        });
        this.insertHex("db3e", 16, (slice, indent) -> {
            CellSlice subslice = CellSlice.beginParse(slice.loadRef());
            String spaces = this.repeatSpaces(indent);
            return String.format("<{\n%s%s}> JMPREFDATA", decompile(subslice, indent + 2), spaces);
        });
        this.insertHex("db3f", 16, (slice, indent) -> "RETDATA");
        // 14368768 (DUMMY)
        this.insertHex("dc", 8, (slice, indent) -> "IFRET");
        this.insertHex("dd", 8, (slice, indent) -> "IFNOTRET");
        this.insertHex("de", 8, (slice, indent) -> "IF");
        this.insertHex("df", 8, (slice, indent) -> "IFNOT");
        this.insertHex("e0", 8, (slice, indent) -> "IFJMP");
        this.insertHex("e1", 8, (slice, indent) -> "IFNOTJMP");
        this.insertHex("e2", 8, (slice, indent) -> "IFELSE");
        this.insertHex("e300", 16, (slice, indent) -> {
            CellSlice subslice = CellSlice.beginParse(slice.loadRef());
            String spaces = this.repeatSpaces(indent);
            return String.format("<{\n%s%s}> IFREF", decompile(subslice, indent + 2), spaces);
        });
        this.insertHex("e301", 16, (slice, indent) -> {
            CellSlice subslice = CellSlice.beginParse(slice.loadRef());
            String spaces = this.repeatSpaces(indent);
            return String.format("<{\n%s%s}> IFNOTREF", decompile(subslice, indent + 2), spaces);
        });
        this.insertHex("e302", 16, (slice, indent) -> {
            CellSlice subslice = CellSlice.beginParse(slice.loadRef());
            String spaces = this.repeatSpaces(indent);
            return String.format("<{\n%s%s}> IFJMPREF", decompile(subslice, indent + 2), spaces);
        });
        this.insertHex("e303", 16, (slice, indent) -> {
            CellSlice subslice = CellSlice.beginParse(slice.loadRef());
            String spaces = this.repeatSpaces(indent);
            return String.format("<{\n%s%s}> IFNOTJMPREF", decompile(subslice, indent + 2), spaces);
        });
        this.insertHex("e304", 16, (slice, indent) -> "CONDSEL");
        this.insertHex("e305", 16, (slice, indent) -> "CONDSELCHK");
        // 14878208 (DUMMY)
        this.insertHex("e308", 16, (slice, indent) -> "IFRETALT");
        this.insertHex("e309", 16, (slice, indent) -> "IFNOTRETALT");
        // 14879232 (DUMMY)
        this.insertHex("e30d", 16, (slice, indent) -> {
            CellSlice subslice = CellSlice.beginParse(slice.loadRef());
            String spaces = this.repeatSpaces(indent);
            return String.format("<{\n%s%s}> IFREFELSE", decompile(subslice, indent + 2), spaces);
        });
        this.insertHex("e30e", 16, (slice, indent) -> {
            CellSlice subslice = CellSlice.beginParse(slice.loadRef());
            String spaces = this.repeatSpaces(indent);
            return String.format("<{\n%s%s}> IFELSEREF", decompile(subslice, indent + 2), spaces);
        });
        this.insertHex("e30f", 16, (slice, indent) -> {
            CellSlice subslice = CellSlice.beginParse(slice.loadRef());
            String spaces = this.repeatSpaces(indent);
            return String.format("<{\n%s%s}> IFREFELSEREF", decompile(subslice, indent + 2), spaces);
        });
        // 14880768 (DUMMY)
        this.insertHex("e314", 16, (slice, indent) -> "REPEATBRK");
        this.insertHex("e315", 16, (slice, indent) -> "REPEATENDBRK");
        this.insertHex("e316", 16, (slice, indent) -> "UNTILBRK");
        this.insertHex("e317", 16, (slice, indent) -> "UNTILENDBRK");
        this.insertHex("e318", 16, (slice, indent) -> "WHILEBRK");
        this.insertHex("e319", 16, (slice, indent) -> "WHILEENDBRK");
        this.insertHex("e31a", 16, (slice, indent) -> "AGAINBRK");
        this.insertHex("e31b", 16, (slice, indent) -> "AGAINENDBRK");
        // 14883840 (DUMMY)
        this.insertHex("e38", 10, (slice, indent) -> {
            slice.loadUint(6);
            return "(FIXED 879)";
        });
        this.insertHex("e3c", 10, (slice, indent) -> {
            slice.loadUint(6);
            return "(EXT)";
        });
        this.insertHex("e4", 8, (slice, indent) -> "REPEAT");
        this.insertHex("e5", 8, (slice, indent) -> "REPEATEND");
        this.insertHex("e6", 8, (slice, indent) -> "UNTIL");
        this.insertHex("e7", 8, (slice, indent) -> "UNTILEND");
        this.insertHex("e8", 8, (slice, indent) -> "WHILE");
        this.insertHex("e9", 8, (slice, indent) -> "WHILEEND");
        this.insertHex("ea", 8, (slice, indent) -> "AGAIN");
        this.insertHex("eb", 8, (slice, indent) -> "AGAINEND");
        this.insertHex("ec", 8, (slice, indent) -> {
            BigInteger r = slice.loadUint(4);
            BigInteger n = slice.loadUint(4);
            return String.format("%d, %d SETCONTARGS", r.intValue(), n.intValue());
        });
        this.insertHex("ed0", 12, (slice, indent) -> {
            BigInteger p = slice.loadUint(4);
            return String.format("%d RETURNARGS", p.intValue());
        });
        this.insertHex("ed10", 16, (slice, indent) -> "RETURNVARARGS");
        this.insertHex("ed11", 16, (slice, indent) -> "SETCONTVARARGS");
        this.insertHex("ed12", 16, (slice, indent) -> "SETNUMVARARGS");
        // 15536896 (DUMMY)
        this.insertHex("ed1e", 16, (slice, indent) -> "BLESS");
        this.insertHex("ed1f", 16, (slice, indent) -> "BLESSVARARGS");
        // 15540224 (DUMMY)
        this.insertHex("ed4", 12, (slice, indent) -> {
            BigInteger n = slice.loadUint(4);
            return String.format("c%d PUSH", n.intValue());
        });
        this.insertHex("ed5", 12, (slice, indent) -> {
            BigInteger x = slice.loadUint(4);
            return String.format("c%d POP", x.intValue());
        });
        // 15554560 (DUMMY)
        this.insertHex("ed6", 12, (slice, indent) -> {
            BigInteger i = slice.loadUint(4);
            return String.format("c%d SETCONT", i.intValue());
        });
        // 15558656 (DUMMY)
        this.insertHex("ed7", 12, (slice, indent) -> {
            BigInteger i = slice.loadUint(4);
            return String.format("c%d SETRETCTR", i.intValue());
        });
        // 15562752 (DUMMY)
        this.insertHex("ed8", 12, (slice, indent) -> {
            BigInteger i = slice.loadUint(4);
            return String.format("c%d SETALTCTR", i.intValue());
        });
        // 15566848 (DUMMY)
        this.insertHex("ed9", 12, (slice, indent) -> {
            BigInteger i = slice.loadUint(4);
            return String.format("c%d POPSAVE", i.intValue());
        });
        // 15570944 (DUMMY)
        this.insertHex("eda", 12, (slice, indent) -> {
            BigInteger i = slice.loadUint(4);
            return String.format("c%d SAVE", i.intValue());
        });
        this.insertHex("edb", 12, (slice, indent) -> {
            BigInteger i = slice.loadUint(4);
            return String.format("c%d SAVEALT", i.intValue());
        });
        this.insertHex("edc", 12, (slice, indent) -> {
            BigInteger i = slice.loadUint(4);
            return String.format("c%d SAVEBOTH", i.intValue());
        });
        this.insertHex("ede0", 16, (slice, indent) -> "PUSHCTRX");
        this.insertHex("ede1", 16, (slice, indent) -> "POPCTRX");
        this.insertHex("ede2", 16, (slice, indent) -> "SETCONTCTRX");
        this.insertHex("edf0", 16, (slice, indent) -> "BOOLAND");
        this.insertHex("edf1", 16, (slice, indent) -> "BOOLOR");
        this.insertHex("edf2", 16, (slice, indent) -> "COMPOSBOTH");
        this.insertHex("edf3", 16, (slice, indent) -> "ATEXIT");
        this.insertHex("edf4", 16, (slice, indent) -> "ATEXITALT");
        this.insertHex("edf5", 16, (slice, indent) -> "SETEXITALT");
        this.insertHex("edf6", 16, (slice, indent) -> "THENRET");
        this.insertHex("edf7", 16, (slice, indent) -> "THENRETALT");
        this.insertHex("edf8", 16, (slice, indent) -> "INVERT");
        this.insertHex("edf9", 16, (slice, indent) -> "BOOLEVAL");
        this.insertHex("edfa", 16, (slice, indent) -> "SAMEALT");
        this.insertHex("edfb", 16, (slice, indent) -> "SAMEALTSAVE");
        // 15596544 (DUMMY)
        this.insertHex("ee", 8, (slice, indent) -> {
            BigInteger r = slice.loadUint(4);
            BigInteger n = slice.loadUint(4);
            return String.format("%d,%d BLESSARGS", r.intValue(), n.intValue());
        });
        // 15663104 (DUMMY)
        this.insertHex("f0", 8, (slice, indent) -> {
            BigInteger n = slice.loadUint(8);
            return String.format("%d CALLDICT", n.intValue());
        });
        this.insertHex("f10", 10, (slice, indent) -> {
            BigInteger n = slice.loadUint(14);
            return String.format("%d CALL", n.intValue());
        });
        this.insertHex("f14", 10, (slice, indent) -> {
            BigInteger args = slice.loadUint(14);
            return String.format("%d JMP", args.intValue());
        });
        this.insertHex("f18", 10, (slice, indent) -> {
            BigInteger args = slice.loadUint(14);
            return String.format("%d PREPARE", args.intValue());
        });
        // 15843328 (DUMMY)
        this.insertHex("f20", 10, (slice, indent) -> {
            BigInteger nn = slice.loadUint(6);
            return String.format("%d THROW", nn.intValue());
        });
        this.insertHex("F24", 10, (slice, indent) -> {
            BigInteger eCode = slice.loadUint(6);
            return String.format("%s THROWIF", eCode.intValue());
        });
        this.insertHex("F28", 10, (slice, indent) -> {
            BigInteger eCode = slice.loadUint(6);
            return String.format("%s THROWIFNOT", eCode.intValue());
        });
        this.insertHex("f2c0", 13, (slice, indent) -> {
            BigInteger args = slice.loadUint(11);
            return String.format("%d THROW", args.intValue());
        });
        this.insertHex("f2c8", 13, (slice, indent) -> {
            BigInteger x = slice.loadUint(11);
            return String.format("%d THROWARG", x.intValue());
        });
        this.insertHex("f2d0", 13, (slice, indent) -> {
            BigInteger x = slice.loadUint(11);
            return String.format("%d THROWIF", x.intValue());
        });
        // codepage.insertHex("f2d8", 13, (slice, indent) => {
        //     BigInteger args = slice.loadUint(11);
        //     return "(FIXED 1080)";
        // });
        this.insertHex("f2e0", 13, (slice, indent) -> {
            BigInteger x = slice.loadUint(11);
            return String.format("%d THROWIFNOT", x.intValue());
        });
        // codepage.insertHex("f2e8", 13, (slice, indent) => {
        //     BigInteger args = slice.loadUint(11);
        //     return "(FIXED 1088)";
        // });
        this.insertHex("f2f0", 13, (slice, indent) -> {
            boolean inverse = slice.loadBit();
            boolean cond = slice.loadBit();
            boolean arg = slice.loadBit();
            return String.format("THROW%sANY%s%s",
                    arg ? "ARG" : "",
                    (cond || inverse) ? "IF" : "",
                    inverse ? "NOT" : "");
        });
        // 15922688 (DUMMY)
        this.insertHex("f2ff", 16, (slice, indent) -> "TRY");
        this.insertHex("f3", 8, (slice, indent) -> {
            BigInteger p = slice.loadUint(4);
            BigInteger r = slice.loadUint(4);
            return String.format("%d,%d TRYARGS", p.intValue(), r.intValue());
        });
        this.insertHex("f400", 16, (slice, indent) -> "STDICT");
        this.insertHex("f401", 16, (slice, indent) -> "SKIPDICT");
        this.insertHex("f402", 16, (slice, indent) -> "LDDICTS");
        this.insertHex("f403", 16, (slice, indent) -> "PLDDICTS");
        this.insertHex("f404", 16, (slice, indent) -> "LDDICT");
        this.insertHex("f405", 16, (slice, indent) -> "PLDDICT");
        this.insertHex("f406", 16, (slice, indent) -> "LDDICTQ");
        this.insertHex("f407", 16, (slice, indent) -> "PLDDICTQ");
        // 15992832 (DUMMY)

        this.insertHex("f40a", 16, (slice, indent) -> "DICTGET");
        this.insertHex("f40b", 16, (slice, indent) -> "DICTGETREF");
        this.insertHex("f40c", 16, (slice, indent) -> "DICTIGET");
        this.insertHex("f40d", 16, (slice, indent) -> "DICTIGETREF");
        this.insertHex("f40e", 16, (slice, indent) -> "DICTUGET");
        this.insertHex("f40f", 16, (slice, indent) -> "DICTUGETREF");
        // 15994880 (DUMMY)

        this.insertHex("f412", 16, (slice, indent) -> "DICTSET");
        this.insertHex("f413", 16, (slice, indent) -> "DICTSETREF");
        this.insertHex("f414", 16, (slice, indent) -> "DICTISET");
        this.insertHex("f415", 16, (slice, indent) -> "DICTISETREF");
        this.insertHex("f416", 16, (slice, indent) -> "DICTUSET");
        this.insertHex("f417", 16, (slice, indent) -> "DICTUSETREF");

        this.insertHex("f41a", 16, (slice, indent) -> "DICTSETGET");
        this.insertHex("F41B", 16, (slice, indent) -> "DICTSETGETREF");
        this.insertHex("F41C", 16, (slice, indent) -> "DICTISETGET");
        this.insertHex("F41D", 16, (slice, indent) -> "DICTISETGETREF");
        this.insertHex("F41E", 16, (slice, indent) -> "DICTUSETGET");
        this.insertHex("F41F", 16, (slice, indent) -> "DICTUSETGETREF");

        // 15998976 (DUMMY)
        this.insertHex("f420", 13, (slice, indent) -> {
            boolean sls = slice.loadBit();
            boolean sign = slice.loadBit();
            boolean ref = slice.loadBit();
            String type = "";
            if (sls && !sign) {
                type = "I";
            } else if (sls) {
                type = "U";
            }
            return String.format("DICT%sREPLACE%s", type, ref ? "REF" : "");
        });

        this.insertHex("f42a", 13, (slice, indent) -> {
            boolean sls = slice.loadBit();
            boolean sign = slice.loadBit();
            boolean ref = slice.loadBit();
            String type = "";
            if (sls && !sign) {
                type = "I";
            } else if (sls) {
                type = "U";
            }
            return String.format("DICT%sREPLACEGET%s", type, ref ? "REF" : "");
        });
        // 16003072 (DUMMY)
        this.insertHex("f432", 13, (slice, indent) -> {
            boolean sls = slice.loadBit();
            boolean sign = slice.loadBit();
            boolean ref = slice.loadBit();
            String type = "";
            if (sls && !sign) {
                type = "I";
            } else if (sls) {
                type = "U";
            }
            return String.format("DICT%sADD%s", type, ref ? "REF" : "");
        });
        // 16005120 (DUMMY)
        this.insertHex("f43a", 13, (slice, indent) -> {
            boolean sls = slice.loadBit();
            boolean sign = slice.loadBit();
            boolean ref = slice.loadBit();
            String type = "";
            if (sls && !sign) {
                type = "I";
            } else if (sls) {
                type = "U";
            }
            return String.format("DICT%sADDGET%s", type, ref ? "REF" : "");
        });
        // 16007168 (DUMMY)
        this.insertHex("f441", 14, (slice, indent) -> {
            boolean int_ = slice.loadBit();
            boolean usign = slice.loadBit();
            return String.format("DICT%sSETB", int_ ? (usign ? "U" : "I") : "");
        });
        // 16008192 (DUMMY)
        this.insertHex("f445", 14, (slice, indent) -> {
            boolean int_ = slice.loadBit();
            boolean usign = slice.loadBit();
            return String.format("DICT%sSETGETB", int_ ? (usign ? "U" : "I") : "");
        });
        // 16009216 (DUMMY)
        this.insertHex("f449", 14, (slice, indent) -> {
            boolean int_ = slice.loadBit();
            boolean usign = slice.loadBit();
            return String.format("DICT%sREPLACEB", int_ ? (usign ? "U" : "I") : "");
        });
        // 16010240 (DUMMY)
        this.insertHex("f44d", 14, (slice, indent) -> {
            boolean int_ = slice.loadBit();
            boolean usign = slice.loadBit();
            return String.format("DICT%sREPLACEGETB", int_ ? (usign ? "U" : "I") : "");
        });
        // 16011264 (DUMMY)
        this.insertHex("f451", 14, (slice, indent) -> {
            boolean int_ = slice.loadBit();
            boolean usign = slice.loadBit();
            return String.format("DICT%sADDB", int_ ? (usign ? "U" : "I") : "");
        });
        // 16012288 (DUMMY)
        this.insertHex("f455", 14, (slice, indent) -> {
            boolean int_ = slice.loadBit();
            boolean usign = slice.loadBit();
            return String.format("DICT%sADDGETB", int_ ? (usign ? "U" : "I") : "");
        });
        // 16013312 (DUMMY)
        this.insertHex("f459", 16, (slice, indent) -> "DICTDEL");
        this.insertHex("f45A", 16, (slice, indent) -> "DICTIDEL");
        this.insertHex("f45B", 16, (slice, indent) -> "DICTUDEL");

        // 16014336 (DUMMY)
        this.insertHex("f462", 13, (slice, indent) -> {
            boolean int_ = slice.loadBit();
            boolean usign = slice.loadBit();
            boolean ref = slice.loadBit();
            String type = "";
            if (int_ && !usign) {
                type = "I";
            } else if (int_) {
                type = "U";
            }
            return String.format("DICT%sDELGET%s", type, ref ? "REF" : "");
        });

        // 16017408 (DUMMY)
        this.insertHex("f469", 16, (slice, indent) -> "DICTGETOPTREF");
        this.insertHex("f46A", 16, (slice, indent) -> "DICTIGETOPTREF");
        this.insertHex("f46B", 16, (slice, indent) -> "DICTUGETOPTREF");

        this.insertHex("f46d", 16, (slice, indent) -> "DICTSETGETOPTREF");
        this.insertHex("f46e", 16, (slice, indent) -> "DICTISETGETOPTREF");
        this.insertHex("f46f", 16, (slice, indent) -> "DICTUSETGETOPTREF");

        this.insertHex("f47", 12, (slice, indent) -> {
            BigInteger args = slice.loadUint(4);
            if (args.equals(BigInteger.ZERO)) {
                return "PFXDICTSET";
            } else if (args.equals(BigInteger.ONE)) {
                return "PFXDICTREPLACE";
            } else if (args.equals(BigInteger.valueOf(2))) {
                return "PFXDICTADD";
            } else if (args.equals(BigInteger.valueOf(3))) {
                return "PFXDICTDEL";
            }
            String res = "DICT";
            if (args.testBit(3)) {
                res += (args.testBit(2) ? "U" : "I");
            }
            return String.format("%sGET%s%s",
                    res,
                    args.testBit(1) ? "PREV" : "NEXT",
                    args.testBit(0) ? "EQ" : "");
        });
        this.insertHex("f48", 11, (slice, indent) -> {
            boolean remove = slice.loadBit();
            boolean max = slice.loadBit();
            boolean int_ = slice.loadBit();
            boolean usign = slice.loadBit();
            boolean ref = slice.loadBit();
            String type = "";
            if (int_ && !usign) {
                type = "I";
            } else if (int_) {
                type = "U";
            }
            return String.format("DICT%s%s%s%s", type, remove ? "REM" : "", max ? "MAX" : "MIN", ref ? "REF" : "");
        });

        this.insertHex("f4a0", 13, (slice, indent) -> {
            boolean push = slice.loadBit();
            if (push) {
                Cell subslice = fetchSubSlice(slice, 0, 1);
                BigInteger keyLen = slice.loadUint(10);
                String decompiled;
                try {
                    CellSlice csRef = CellSlice.beginParse(CellSlice.beginParse(subslice).loadRef());
                    decompiled = Disassembler.decompileMethodsMap(csRef, keyLen.intValue(), indent);
                } catch (Exception e) {
                    System.err.println(e.getMessage());
                    String spacesToAdd = this.repeatSpaces(indent);
                    decompiled = subslice.toString() + spacesToAdd;
                }
                return String.format("%s %d DICTPUSHCONST", decompiled, keyLen.intValue());
            }
            boolean exec = slice.loadBit();
            boolean usign = slice.loadBit();
            return String.format("DICT%sGET%s", usign ? "U" : "I", exec ? "EXEC" : "JMP");
        });

        this.insertHex("f4a8", 16, (slice, indent) -> "PFXDICTGETQ");
        this.insertHex("f4a9", 16, (slice, indent) -> "PFXDICTGET");
        this.insertHex("f4aa", 16, (slice, indent) -> "PFXDICTGETJMP");
        this.insertHex("f4ab", 16, (slice, indent) -> "PFXDICTGETEXEC");
        // codepage.insertHex("f4ac00", 13, (slice, indent) => {
        //     BigInteger args = slice.loadUint(11);
        //     return "(EXT)";
        // });
        // 16035840 (DUMMY)
        this.insertHex("f4b1", 13, (slice, indent) -> {
            boolean int_ = slice.loadBit();
            boolean usign = slice.loadBit();
            boolean ref = slice.loadBit();
            String type = "";
            if (int_ && !usign) {
                type = "I";
            } else if (int_) {
                type = "U";
            }
            return String.format("SUBDICT%sGET%s", type, ref ? "REF" : "");
        });
        // 16036864 (DUMMY)
        this.insertHex("f4b5", 13, (slice, indent) -> {
            boolean int_ = slice.loadBit();
            boolean usign = slice.loadBit();
            boolean ref = slice.loadBit();
            String type = "";
            if (int_ && !usign) {
                type = "I";
            } else if (int_) {
                type = "U";
            }
            return String.format("SUBDICT%sRPGET%s", type, ref ? "REF" : "");
        });
        // 16037888 (DUMMY)
        this.insertHex("f4bc", 14, (slice, indent) -> {
            boolean exec = slice.loadBit();
            boolean unsigned = slice.loadBit();
            return String.format("DICT%sGET%sZ", unsigned ? "U" : "I", exec ? "EXEC" : "JMP");
        });

        // 16039936 (DUMMY)
        this.insertHex("f800", 16, (slice, indent) -> "ACCEPT");
        this.insertHex("f801", 16, (slice, indent) -> "SETGASLIMIT");

        // 16253440 (DUMMY)
        this.insertHex("f80f", 16, (slice, indent) -> "COMMIT");
        this.insertHex("f810", 16, (slice, indent) -> "RANDU256");
        this.insertHex("f811", 16, (slice, indent) -> "RAND");

        // 16257536 (DUMMY)
        this.insertHex("f814", 16, (slice, indent) -> "SETRAND");
        this.insertHex("f815", 16, (slice, indent) -> "ADDRAND");
        this.insertHex("f82", 12, (slice, indent) -> {
            BigInteger i = slice.loadUint(4);
            switch (i.intValue()) {
                case 0x3:
                    return "NOW";
                case 0x4:
                    return "BLOCKLT";
                case 0x5:
                    return "LTIME";
                case 0x6:
                    return "RANDSEED";
                case 0x7:
                    return "BALANCE";
                case 0x8:
                    return "MYADDR";
                case 0x9:
                    return "CONFIGROOT";
                default:
                    return String.format("%d GETPARAM", i.intValue());
            }
        });
        this.insertHex("f830", 16, (slice, indent) -> "CONFIGDICT");
        // 16265472 (DUMMY)
        this.insertHex("f832", 16, (slice, indent) -> "CONFIGPARAM");
        this.insertHex("f833", 16, (slice, indent) -> "CONFIGOPTPARAM");
        // 16266240 (DUMMY)
        // this.insertHex("f84000", 16, (slice, indent) -> "GETGLOBVAR");
        this.insertHex("f841", 11, (slice, indent) -> {
            BigInteger args = slice.loadUint(5);
            return String.format("%d GETGLOBVAR", args);
        });
        // this.insertHex("f86000", 16, (slice, indent) -> "SETGLOBVAR");
        this.insertHex("f861", 11, (slice, indent) -> {
            BigInteger args = slice.loadUint(5);
            return String.format("%d SETGLOBVAR", args);
        });
        // 16285696 (DUMMY)
        this.insertHex("f900", 16, (slice, indent) -> "HASHCU");
        this.insertHex("f901", 16, (slice, indent) -> "HASHSU");
        this.insertHex("f902", 16, (slice, indent) -> "SHA256U");
        // 16319232 (DUMMY)
        this.insertHex("f910", 16, (slice, indent) -> "CHKSIGNU");
        this.insertHex("f911", 16, (slice, indent) -> "CHKSIGNS");
        // 16323072 (DUMMY)
        this.insertHex("f940", 16, (slice, indent) -> "CDATASIZEQ");
        this.insertHex("f941", 16, (slice, indent) -> "CDATASIZE");
        this.insertHex("f942", 16, (slice, indent) -> "SDATASIZEQ");
        this.insertHex("f943", 16, (slice, indent) -> "SDATASIZE");
        // 16335872 (DUMMY)
        this.insertHex("fa00", 16, (slice, indent) -> "LDGRAMS");
        this.insertHex("fa01", 16, (slice, indent) -> "LDVARINT16");
        this.insertHex("fa02", 16, (slice, indent) -> "STGRAMS");
        this.insertHex("fa03", 16, (slice, indent) -> "STVARINT16");
        this.insertHex("fa04", 16, (slice, indent) -> "LDVARUINT32");
        this.insertHex("fa05", 16, (slice, indent) -> "LDVARINT32");
        this.insertHex("fa06", 16, (slice, indent) -> "STVARUINT32");
        this.insertHex("fa07", 16, (slice, indent) -> "STVARINT32");
        // 16386048 (DUMMY)
        this.insertHex("fa40", 16, (slice, indent) -> "LDMSGADDR");
        this.insertHex("fa41", 16, (slice, indent) -> "LDMSGADDRQ");
        this.insertHex("fa42", 16, (slice, indent) -> "PARSEMSGADDR");
        this.insertHex("fa43", 16, (slice, indent) -> "PARSEMSGADDRQ");
        this.insertHex("fa44", 16, (slice, indent) -> "REWRITESTDADDR");
        this.insertHex("fa45", 16, (slice, indent) -> "REWRITESTDADDRQ");
        this.insertHex("fa46", 16, (slice, indent) -> "REWRITEVARADDR");
        this.insertHex("fa47", 16, (slice, indent) -> "REWRITEVARADDRQ");
        // 16402432 (DUMMY)
        this.insertHex("fb00", 16, (slice, indent) -> "SENDRAWMSG");
        // 16449792 (DUMMY)
        this.insertHex("fb02", 16, (slice, indent) -> "RAWRESERVE");
        this.insertHex("fb03", 16, (slice, indent) -> "RAWRESERVEX");
        this.insertHex("fb04", 16, (slice, indent) -> "SETCODE");
        // 16450816 (DUMMY)
        this.insertHex("fb06", 16, (slice, indent) -> "SETLIBCODE");
        this.insertHex("fb07", 16, (slice, indent) -> "CHANGELIB");
        // 16451584 (DUMMY)
        this.insertHex("fe", 8, (slice, indent) -> {
            BigInteger nn = slice.loadUint(8);
            if (nn.and(BigInteger.valueOf(0xf0)).equals(BigInteger.valueOf(0xf0))) {
                int n = nn.and(BigInteger.valueOf(0x0f)).intValue();
                String str = new String(slice.loadBytes(n + 1), StandardCharsets.UTF_8);
                return String.format("\"%s\" DEBUGSTR", str);
            }
            return String.format("%d DEBUG", nn);
        });

        this.insertHex("ff", 8, (slice, indent) -> {
            BigInteger nn = slice.loadUint(8);
            if (nn.and(BigInteger.valueOf(0xf0)).equals(BigInteger.valueOf(0xf0))) {
                int z = nn.and(BigInteger.valueOf(0x0f)).intValue();
                if (z == 0) {
                    return "SETCPX";
                }
                nn = BigInteger.valueOf(z - 16);
            }
            return String.format("%d SETCP", nn);
        });

    }
}
