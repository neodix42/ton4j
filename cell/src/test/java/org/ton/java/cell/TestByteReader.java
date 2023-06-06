package org.ton.java.cell;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.ton.java.utils.Utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertThrows;


@Slf4j
@RunWith(JUnit4.class)
public class TestByteReader {

    @Test
    public void testReaderUnSigned() {
        int[] b = Utils.hexToUnsignedBytes("B5EE9C72");
        ByteReader r = new ByteReader(b);
        assertThat(b).isEqualTo(r.readBytes(4));
    }

    @Test
    public void testReaderUnSignedOneByOne() {
        int[] b = Utils.hexToUnsignedBytes("B5EE9C72");
        ByteReader r = new ByteReader(b);
        assertThat(0xB5).isEqualTo(r.readByte());
        assertThat(0xEE).isEqualTo(r.readByte());
        assertThat(0x9C).isEqualTo(r.readByte());
        assertThat(0x72).isEqualTo(r.readByte());
        assertThrows(IllegalArgumentException.class, r::readByte);
    }

    @Test
    public void testReaderUnSignedTwoByTwo() {
        int[] b = Utils.hexToUnsignedBytes("B5EE9C72");
        ByteReader r = new ByteReader(b);
        assertThat(new int[]{0xB5, 0xEE}).isEqualTo(r.readBytes(2));
        assertThat(new int[]{0x9C, 0x72}).isEqualTo(r.readBytes(2));

        assertThrows(IllegalArgumentException.class, r::readByte);
    }

    @Test
    public void testReaderSigned() {
        byte[] b = Utils.hexToSignedBytes("B5EE9C72");
        ByteReader r = new ByteReader(b);
        assertThat(b).isEqualTo(Utils.unsignedBytesToSigned(r.readBytes(4)));
    }

    @Test
    public void testReaderSignedOneByOne() {
        int[] b = Utils.hexToUnsignedBytes("B5EE9C72");
        ByteReader r = new ByteReader(b);
        assertThat(-75).isEqualTo(r.readSignedByte());
        assertThat(-18).isEqualTo(r.readSignedByte());
        assertThat(-100).isEqualTo(r.readSignedByte());
        assertThat(0x72).isEqualTo(r.readSignedByte());
        assertThrows(IllegalArgumentException.class, r::readSignedByte);
    }
}
