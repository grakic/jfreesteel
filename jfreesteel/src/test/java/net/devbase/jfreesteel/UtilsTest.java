package net.devbase.jfreesteel;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;

/**
 * @author Filip Miletic (filmil@gmail.com)
 */
public class UtilsTest extends EidTestCase {

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
    	
    	@SuppressWarnings("serial")
		HashMap<Integer, byte[]> testmap = new HashMap<Integer, byte[]>() {{
            put(0x01, "АБВГДЂ".getBytes(StandardCharsets.UTF_8));
            put(0x02, "ЕЖЗИЈК".getBytes(StandardCharsets.UTF_8));
            put(0x03, "\uffff\uffff\uffff".getBytes(StandardCharsets.ISO_8859_1));
            put(0x04, "AB".getBytes(StandardCharsets.UTF_8));
        }};
    	
        String result = Utils.map2UTF8String(testmap);

        assertContains("1 = АБВГДЂ (D0:90:D0:91:D0:92:D0:93:D0:94:D0:82)\n", result);
        assertContains("2 = ЕЖЗИЈК (D0:95:D0:96:D0:97:D0:98:D0:88:D0:9A)\n", result);
        assertContains(
            new String("3 = \uffff\uffff\uffff (3F:3F:3F)\n".getBytes(StandardCharsets.ISO_8859_1),
            StandardCharsets.ISO_8859_1), result);
        assertContains("4 = AB (41:42)\n", result);
    }
}
