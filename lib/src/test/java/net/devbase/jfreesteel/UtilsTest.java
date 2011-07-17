package net.devbase.jfreesteel;

import junit.framework.TestCase;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableMap;

/**
 * @author Filip Miletic (filmil@gmail.com)
 */
public class UtilsTest extends TestCase {

    public void testInt2HexString() {
        // TODO(filmil): Is this the expected outcome?
        assertEquals("", Utils.int2HexString(0x00));
        assertEquals("01", Utils.int2HexString(0x01));
        assertEquals("01:00", Utils.int2HexString(0x100));
        assertEquals("01:00:00", Utils.int2HexString(0x10000));
        assertEquals("01:00:00:00", Utils.int2HexString(0x1000000));
        assertEquals("FF:00:00:00", Utils.int2HexString(0xff000000));
        assertEquals("DE:AD:BE:EF", Utils.int2HexString(0xdeadbeef));
        assertEquals("CA:FE:BA:BE", Utils.int2HexString(0xcafebabe));
    }

    public void testBytes2HexString() {
        assertEquals("", Utils.bytes2HexString(asByte(0x00)));
        assertEquals(
            "01:02:03",
            Utils.bytes2HexString(asByte(0x00), asByte(0x01), asByte(0x02), asByte(0x03)));
    }

    public void testBytes2UTF8String() {
        assertEquals("", Utils.bytes2UTF8String((byte[]) null));
        assertEquals("Hello world", Utils.bytes2UTF8String("Hello world".getBytes()));
    }

    public void testMap2UTF8String() {
        String result = Utils.map2UTF8String(ImmutableMap.of(
            0x01, "АБВГДЂ".getBytes(Charsets.UTF_8),
            0x02, "ЕЖЗИЈК".getBytes(Charsets.UTF_8),
            0x03, "\uffff\uffff\uffff".getBytes(Charsets.ISO_8859_1)));
        assertContains("1 = АБВГДЂ\n", result);
        assertContains("2 = ЕЖЗИЈК\n", result);
        assertContains(
            new String("3 = \uffff\uffff\uffff\n".getBytes(Charsets.ISO_8859_1),
                Charsets.ISO_8859_1), result);
    }

    private void assertContains(String expected, String actual) {
        assertTrue(
            String.format("'%s' should contain '%s' but does not", actual, expected),
            actual.contains(expected));
    }

    private byte asByte(int value) {
        return (byte) (value & 0xff);
    }
}
