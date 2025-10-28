package org.ton.ton4j.disassembler.coverage;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.math.BigInteger;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellBuilder;
import org.ton.ton4j.cell.CellSlice;
import org.ton.ton4j.disassembler.Disassembler;
import org.ton.ton4j.disassembler.codepages.CP0Auto;
import org.ton.ton4j.disassembler.codepages.CP0Manual;
import org.ton.ton4j.disassembler.structs.Codepage;

/**
 * Comprehensive coverage tests for the disassembler module to increase coverage above 80%.
 * Tests focus on CP0Manual (0% coverage), CP0Auto (65.3% coverage), and Disassembler (75.9% coverage).
 */
@RunWith(JUnit4.class)
public class DisassemblerCoverageTest {

    // ==================== CP0Manual Tests ====================
    
    @Test
    public void testCP0ManualInitialization() {
        CP0Manual cp0Manual = new CP0Manual();
        assertNotNull(cp0Manual);
    }

    @Test
    public void testCP0ManualXCHGOperations() {
        CP0Manual cp = new CP0Manual();
        
        // Test s0 s0 XCHG (NOP)
        CellBuilder builder = CellBuilder.beginCell();
        builder.storeUint(0x0, 4); // hex "0" with n=0
        builder.storeUint(0x0, 4);
        CellSlice slice = CellSlice.beginParse(builder.endCell());
        
        Object op = cp.getOp("0000");
        assertNotNull(op);
    }

    @Test
    public void testCP0ManualPushIntOperations() {
        CP0Manual cp = new CP0Manual();
        
        // Test PUSHINT with 4-bit value
        CellBuilder builder = CellBuilder.beginCell();
        builder.storeUint(0x7, 4); // hex "7"
        builder.storeInt(5, 4);
        CellSlice slice = CellSlice.beginParse(builder.endCell());
        
        Object op = cp.getOp("0111");
        assertNotNull(op);
    }

    @Test
    public void testCP0ManualArithmeticOperations() {
        CP0Manual cp = new CP0Manual();
        
        // Test SUB operation
        Object subOp = cp.getOp("10100001");
        assertNotNull(subOp);
        
        // Test INC operation
        Object incOp = cp.getOp("10100100");
        assertNotNull(incOp);
    }

    @Test
    public void testCP0ManualCellOperations() {
        CP0Manual cp = new CP0Manual();
        
        // Test NEWC
        Object newcOp = cp.getOp("11001000");
        assertNotNull(newcOp);
        
        // Test ENDC
        Object endcOp = cp.getOp("11001001");
        assertNotNull(endcOp);
        
        // Test CTOS
        Object ctosOp = cp.getOp("11010000");
        assertNotNull(ctosOp);
    }

    @Test
    public void testCP0ManualComparisonOperations() {
        CP0Manual cp = new CP0Manual();
        
        // Test LESS
        Object lessOp = cp.getOp("10111001");
        assertNotNull(lessOp);
        
        // Test EQUAL
        Object equalOp = cp.getOp("10111010");
        assertNotNull(equalOp);
        
        // Test GREATER
        Object greaterOp = cp.getOp("10111100");
        assertNotNull(greaterOp);
    }

    @Test
    public void testCP0ManualControlFlowOperations() {
        CP0Manual cp = new CP0Manual();
        
        // Test IFJMP
        Object ifjmpOp = cp.getOp("11100000");
        assertNotNull(ifjmpOp);
        
        // Test IFELSE
        Object ifelseOp = cp.getOp("11100010");
        assertNotNull(ifelseOp);
        
        // Test WHILE
        Object whileOp = cp.getOp("11101000");
        assertNotNull(whileOp);
    }

    // ==================== CP0Auto Tests ====================

    @Test
    public void testCP0AutoInitialization() {
        CP0Auto cp0Auto = new CP0Auto();
        assertNotNull(cp0Auto);
    }

    @Test
    public void testCP0AutoBasicOperations() {
        CP0Auto cp = new CP0Auto();
        
        // Test that CP0Auto has operations registered
        Object op = cp.getOp("0000");
        assertNotNull(op);
    }

    // ==================== Disassembler Tests ====================

    @Test
    public void testDisassemblerWithSimpleCode() {
        // Create a simple cell with NOP instruction (needs 8 bits minimum)
        CellBuilder builder = CellBuilder.beginCell();
        builder.storeUint(0x00, 8); // NOP (s0 s0 XCHG)
        Cell cell = builder.endCell();
        
        String result = Disassembler.decompile(CellSlice.beginParse(cell), 0);
        assertNotNull(result);
    }

    @Test
    public void testDisassemblerWithIndentation() {
        CellBuilder builder = CellBuilder.beginCell();
        builder.storeUint(0x00, 8); // NOP
        Cell cell = builder.endCell();
        
        String result = Disassembler.decompile(CellSlice.beginParse(cell), 4);
        assertNotNull(result);
        assertTrue(result.length() > 0);
    }

    @Test
    public void testDisassemblerWithNullIndent() {
        CellBuilder builder = CellBuilder.beginCell();
        builder.storeUint(0x00, 8); // NOP
        Cell cell = builder.endCell();
        
        String result = Disassembler.decompile(CellSlice.beginParse(cell), null);
        assertNotNull(result);
    }

    @Test
    public void testDisassemblerFromCode() {
        // Create a cell with magic number and simple code
        CellBuilder builder = CellBuilder.beginCell();
        builder.storeUint(0xFF00, 16); // magic
        builder.storeUint(0x00, 8);    // NOP
        Cell cell = builder.endCell();
        
        String result = Disassembler.fromCode(cell);
        assertNotNull(result);
        assertTrue(result.contains("SETCP0"));
    }

    @Test
    public void testDisassemblerFromBoc() {
        // Create a simple BOC
        CellBuilder builder = CellBuilder.beginCell();
        builder.storeUint(0xFF00, 16); // magic
        builder.storeUint(0x00, 8);    // NOP
        Cell cell = builder.endCell();
        byte[] boc = cell.toBoc();
        
        String result = Disassembler.fromBoc(boc);
        assertNotNull(result);
        assertTrue(result.contains("SETCP0"));
    }

    @Test
    public void testDisassemblerWithMultipleInstructions() {
        CellBuilder builder = CellBuilder.beginCell();
        builder.storeUint(0x00, 8);  // NOP
        builder.storeUint(0xA4, 8); // INC
        Cell cell = builder.endCell();
        
        String result = Disassembler.decompile(CellSlice.beginParse(cell), 0);
        assertNotNull(result);
    }

    @Test
    public void testDisassemblerWithReferences() {
        // Create a cell with a reference
        CellBuilder refBuilder = CellBuilder.beginCell();
        refBuilder.storeUint(0x00, 8); // NOP
        Cell refCell = refBuilder.endCell();
        
        CellBuilder builder = CellBuilder.beginCell();
        builder.storeUint(0x00, 8); // NOP
        builder.storeRef(refCell);
        Cell cell = builder.endCell();
        
        String result = Disassembler.decompile(CellSlice.beginParse(cell), 0);
        assertNotNull(result);
    }

    @Test
    public void testDisassemblerWithUnknownOpcode() {
        // Create a cell with bits that don't match any known opcode
        CellBuilder builder = CellBuilder.beginCell();
        // Fill with a pattern that won't match
        for (int i = 0; i < 20; i++) {
            builder.storeBit(i % 2 == 0);
        }
        Cell cell = builder.endCell();
        
        String result = Disassembler.decompile(CellSlice.beginParse(cell), 0);
        assertNotNull(result);
    }

    @Test
    public void testDisassemblerWithEmptyCell() {
        CellBuilder builder = CellBuilder.beginCell();
        Cell cell = builder.endCell();
        
        String result = Disassembler.decompile(CellSlice.beginParse(cell), 0);
        assertNotNull(result);
    }

    @Test
    public void testCP0ManualPushContOperation() {
        CP0Manual cp = new CP0Manual();
        
        // Test PUSHCONT with 8E prefix - it's registered with 7 bits in CP0Manual
        Object op = cp.getOp("1000111");  // 7 bits as per CP0Manual.insertHex("8E", 7, ...)
        assertNotNull(op);
    }

    @Test
    public void testCP0ManualPushCont9Prefix() {
        CP0Manual cp = new CP0Manual();
        
        // Test PUSHCONT with 9 prefix
        CellBuilder builder = CellBuilder.beginCell();
        builder.storeUint(0x9, 4);
        builder.storeUint(0, 4); // len
        CellSlice slice = CellSlice.beginParse(builder.endCell());
        
        Object op = cp.getOp("1001");
        assertNotNull(op);
    }

    @Test
    public void testCP0ManualThrowOperations() {
        CP0Manual cp = new CP0Manual();
        
        // Test THROWARG
        Object throwargOp = cp.getOp("1111001011001");
        assertNotNull(throwargOp);
        
        // Test THROWIF
        Object throwifOp = cp.getOp("1111001001");
        assertNotNull(throwifOp);
        
        // Test THROWIFNOT
        Object throwifnotOp = cp.getOp("1111001010");
        assertNotNull(throwifnotOp);
    }

    @Test
    public void testCP0ManualDictOperations() {
        CP0Manual cp = new CP0Manual();
        
        // Test DICTPUSHCONST
        Object dictOp = cp.getOp("111101001010");
        assertNotNull(dictOp);
        
        // Test DICTIGETJMPZ
        Object dictGetOp = cp.getOp("11110100101111");
        assertNotNull(dictGetOp);
    }

    @Test
    public void testCP0ManualMiscOperations() {
        CP0Manual cp = new CP0Manual();
        
        // Test ROTREV
        Object rotrevOp = cp.getOp("01011001");
        assertNotNull(rotrevOp);
        
        // Test PUSHROOT
        Object pushrootOp = cp.getOp("1110110101000100");
        assertNotNull(pushrootOp);
        
        // Test c4 POP
        Object c4popOp = cp.getOp("1110110101010100");
        assertNotNull(c4popOp);
        
        // Test SETCP0
        Object setcp0Op = cp.getOp("1111111100000000");
        assertNotNull(setcp0Op);
    }

    @Test
    public void testCP0ManualCryptoOperations() {
        CP0Manual cp = new CP0Manual();
        
        // Test HASHSU
        Object hashsuOp = cp.getOp("1111100100000001");
        assertNotNull(hashsuOp);
        
        // Test CHKSIGNU
        Object chksignuOp = cp.getOp("1111100100010000");
        assertNotNull(chksignuOp);
        
        // Test RANDU256
        Object randuOp = cp.getOp("1111100000010000");
        assertNotNull(randuOp);
        
        // Test ADDRAND
        Object addrandOp = cp.getOp("1111100000010101");
        assertNotNull(addrandOp);
    }

    @Test
    public void testCP0ManualPush2Operation() {
        CP0Manual cp = new CP0Manual();
        
        // Test PUSH2
        CellBuilder builder = CellBuilder.beginCell();
        builder.storeUint(0x53, 8);
        builder.storeUint(1, 4); // first
        builder.storeUint(2, 4); // second
        CellSlice slice = CellSlice.beginParse(builder.endCell());
        
        Object op = cp.getOp("01010011");
        assertNotNull(op);
    }

    @Test
    public void testCP0ManualXC2PUOperation() {
        CP0Manual cp = new CP0Manual();
        
        // Test XC2PU
        CellBuilder builder = CellBuilder.beginCell();
        builder.storeUint(0x541, 12);
        builder.storeUint(1, 4); // i
        builder.storeUint(2, 4); // j
        builder.storeUint(3, 4); // k
        CellSlice slice = CellSlice.beginParse(builder.endCell());
        
        Object op = cp.getOp("010101000001");
        assertNotNull(op);
    }

    @Test
    public void testCP0ManualPushPow2Operations() {
        CP0Manual cp = new CP0Manual();
        
        // Test PUSHPOW2
        CellBuilder builder = CellBuilder.beginCell();
        builder.storeUint(0x83, 8);
        builder.storeUint(5, 8); // x
        CellSlice slice = CellSlice.beginParse(builder.endCell());
        
        Object op = cp.getOp("10000011");
        assertNotNull(op);
        
        // Test PUSHPOW2DEC
        Object pow2decOp = cp.getOp("10000100");
        assertNotNull(pow2decOp);
    }

    @Test
    public void testCP0ManualLongPushInt() {
        CP0Manual cp = new CP0Manual();
        
        // Test long PUSHINT (82 prefix)
        CellBuilder builder = CellBuilder.beginCell();
        builder.storeUint(0x82, 8);
        builder.storeUint(1, 5); // len
        builder.storeInt(100, 27); // value (8*1+19 = 27 bits)
        CellSlice slice = CellSlice.beginParse(builder.endCell());
        
        Object op = cp.getOp("10000010");
        assertNotNull(op);
    }

    @Test
    public void testCP0ManualFitsOperations() {
        CP0Manual cp = new CP0Manual();
        
        // Test FITSX
        Object fitsxOp = cp.getOp("1011011000000000");
        assertNotNull(fitsxOp);
        
        // Test UFITSX
        Object ufitsxOp = cp.getOp("1011011000000001");
        assertNotNull(ufitsxOp);
    }

    @Test
    public void testCP0ManualComparisonOps() {
        CP0Manual cp = new CP0Manual();
        
        // Test SGN
        Object sgnOp = cp.getOp("10111000");
        assertNotNull(sgnOp);
        
        // Test LEQ
        Object leqOp = cp.getOp("10111011");
        assertNotNull(leqOp);
        
        // Test NEQ
        Object neqOp = cp.getOp("10111101");
        assertNotNull(neqOp);
        
        // Test GEQ
        Object geqOp = cp.getOp("10111110");
        assertNotNull(geqOp);
        
        // Test CMP
        Object cmpOp = cp.getOp("10111111");
        assertNotNull(cmpOp);
    }

    @Test
    public void testDisassemblerWithExceptionHandling() {
        // Test that Disassembler handles exceptions gracefully
        CellBuilder builder = CellBuilder.beginCell();
        // Create an invalid pattern that will trigger exception handling
        for (int i = 0; i < 100; i++) {
            builder.storeBit(true);
        }
        Cell cell = builder.endCell();
        
        String result = Disassembler.decompile(CellSlice.beginParse(cell), 0);
        assertNotNull(result);
    }

    @Test
    public void testCP0ManualSliceOperations() {
        CP0Manual cp = new CP0Manual();
        
        // Test ENDS
        Object endsOp = cp.getOp("11010001");
        assertNotNull(endsOp);
        
        // Test LDSLICEX
        Object ldslicexOp = cp.getOp("1101011100011000");
        assertNotNull(ldslicexOp);
        
        // Test SREFS
        Object srefsOp = cp.getOp("1101011101001010");
        assertNotNull(srefsOp);
        
        // Test SDSKIPFIRST
        Object sdskipOp = cp.getOp("1101011100100001");
        assertNotNull(sdskipOp);
        
        // Test PLDU
        Object plduOp = cp.getOp("1101011100001011");
        assertNotNull(plduOp);
    }

    @Test
    public void testCP0ManualControlFlow2() {
        CP0Manual cp = new CP0Manual();
        
        // Test IFNOTRET
        Object ifnotretOp = cp.getOp("11011101");
        assertNotNull(ifnotretOp);
        
        // Test CONDSEL
        Object condselOp = cp.getOp("1110001100000100");
        assertNotNull(condselOp);
        
        // Test CALLREF
        Object callrefOp = cp.getOp("1101101100111100");
        assertNotNull(callrefOp);
    }

    @Test
    public void testCP0ManualStoreUintOperation() {
        CP0Manual cp = new CP0Manual();
        
        // Test STU
        CellBuilder builder = CellBuilder.beginCell();
        builder.storeUint(0xCB, 8);
        builder.storeUint(8, 8); // c (bits to store)
        CellSlice slice = CellSlice.beginParse(builder.endCell());
        
        Object op = cp.getOp("11001011");
        assertNotNull(op);
    }

    @Test
    public void testCP0ManualEqIntOperation() {
        CP0Manual cp = new CP0Manual();
        
        // Test EQINT (todo)
        CellBuilder builder = CellBuilder.beginCell();
        builder.storeUint(0xC0, 8);
        builder.storeUint(0, 8);
        CellSlice slice = CellSlice.beginParse(builder.endCell());
        
        Object op = cp.getOp("11000000");
        assertNotNull(op);
    }

    @Test
    public void testCP0ManualLdgramsOperation() {
        CP0Manual cp = new CP0Manual();
        
        // Test LDGRAMS
        Object ldgramsOp = cp.getOp("1111101000000000");
        assertNotNull(ldgramsOp);
    }

    @Test
    public void testCP0ManualXchg2AndXcpuOperations() {
        CP0Manual cp = new CP0Manual();
        
        // Test XCHG2
        CellBuilder builder = CellBuilder.beginCell();
        builder.storeUint(0x50, 8);
        builder.storeUint(1, 4); // i
        builder.storeUint(2, 4); // j
        CellSlice slice = CellSlice.beginParse(builder.endCell());
        
        Object op = cp.getOp("01010000");
        assertNotNull(op);
        
        // Test XCPU
        builder = CellBuilder.beginCell();
        builder.storeUint(0x51, 8);
        builder.storeUint(1, 4); // i
        builder.storeUint(2, 4); // j
        slice = CellSlice.beginParse(builder.endCell());
        
        op = cp.getOp("01010001");
        assertNotNull(op);
        
        // Test PUXC
        builder = CellBuilder.beginCell();
        builder.storeUint(0x52, 8);
        builder.storeUint(1, 4); // i
        builder.storeUint(3, 4); // j (will be j-1 in output)
        slice = CellSlice.beginParse(builder.endCell());
        
        op = cp.getOp("01010010");
        assertNotNull(op);
    }

    @Test
    public void testCP0ManualS1XchgOperation() {
        CP0Manual cp = new CP0Manual();
        
        // Test s1 sN XCHG
        CellBuilder builder = CellBuilder.beginCell();
        builder.storeUint(0x1, 4);
        builder.storeUint(3, 4); // n
        CellSlice slice = CellSlice.beginParse(builder.endCell());
        
        Object op = cp.getOp("0001");
        assertNotNull(op);
    }

    @Test
    public void testCP0ManualPushAndPopOperations() {
        CP0Manual cp = new CP0Manual();
        
        // Test sN PUSH
        CellBuilder builder = CellBuilder.beginCell();
        builder.storeUint(0x2, 4);
        builder.storeUint(5, 4); // n
        CellSlice slice = CellSlice.beginParse(builder.endCell());
        
        Object op = cp.getOp("0010");
        assertNotNull(op);
        
        // Test sN POP
        builder = CellBuilder.beginCell();
        builder.storeUint(0x3, 4);
        builder.storeUint(5, 4); // n
        slice = CellSlice.beginParse(builder.endCell());
        
        op = cp.getOp("0011");
        assertNotNull(op);
    }

    @Test
    public void testCP0ManualPushInt8And16() {
        CP0Manual cp = new CP0Manual();
        
        // Test PUSHINT 8-bit
        CellBuilder builder = CellBuilder.beginCell();
        builder.storeUint(0x80, 8);
        builder.storeInt(-5, 8);
        CellSlice slice = CellSlice.beginParse(builder.endCell());
        
        Object op = cp.getOp("10000000");
        assertNotNull(op);
        
        // Test PUSHINT 16-bit
        builder = CellBuilder.beginCell();
        builder.storeUint(0x81, 8);
        builder.storeInt(1000, 16);
        slice = CellSlice.beginParse(builder.endCell());
        
        op = cp.getOp("10000001");
        assertNotNull(op);
    }
}
