package org.ton.ton4j.smartcontract.unittests;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.ton.ton4j.smartcontract.types.AdnlAddress;
import org.ton.ton4j.utils.Utils;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

@Slf4j
@RunWith(JUnit4.class)
public class TestAdnlAddress {

    @Test
    public void testAdnlAddressToHex() {
        String inputStr1 = "8f3c7aeb8b647a9f94a1e6f9dcb457d63f2f01c8a5f2565a7d6f6bcd9f8f0c8e";
        byte[] inputByte1 = Utils.hexToSignedBytes(inputStr1);

        AdnlAddress addressStr1 = AdnlAddress.of(inputStr1);
        AdnlAddress addressByte1 = AdnlAddress.of(inputByte1);
        String outputStr1 = addressStr1.toHex();
        byte[] outputByte1 = addressByte1.getClonedBytes();

        assertEquals(inputStr1, outputStr1);
        assertArrayEquals(inputByte1, outputByte1);

        String inputStr2 = "deadbeefcafebabe1234567890abcdef0123456789abcdef0123456789abcdef";
        byte[] inputByte2 = Utils.hexToSignedBytes(inputStr2);

        AdnlAddress addressStr2 = AdnlAddress.of(inputStr2);
        AdnlAddress addressByte2 = AdnlAddress.of(inputByte2);
        String outputStr2 = addressStr2.toHex();
        byte[] outputByte2 = addressByte2.getClonedBytes();

        assertEquals(inputStr2, outputStr2);
        assertArrayEquals(inputByte2, outputByte2);
    }

    @Test
    public void testAdnlAddressInvalid() {
        String input1 = "";
        byte[] input2 = new byte[0];
        String input3 = "4f8b2a3e1c6d4e7f9a3b5c6d8e9f0123456789abcdefabcdef0123456789";
        String input4 = "f5c9e40d2a1b0c4d7e8f9a0b1c2d3e4f567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef";
        String input5 = null;

        assertThrows(Error.class, () -> {
            AdnlAddress.of(input1);
        });
        assertThrows(Error.class, () -> {
            AdnlAddress.of(input2);
        });
        assertThrows(Error.class, () -> {
            AdnlAddress.of(input3);
        });
        assertThrows(Error.class, () -> {
            AdnlAddress.of(input4);
        });
        assertThrows(Error.class, () -> {
            AdnlAddress.of(input5);
        });
        assertThrows(Error.class, () -> {
            AdnlAddress.of(AdnlAddress.of(""));
        });
    }
}
