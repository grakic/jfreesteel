/*
 * jfreesteel: Serbian eID Viewer Library (GNU LGPLv3)
 * Copyright (C) 2011 Goran Rakic
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License version
 * 3.0 as published by the Free Software Foundation.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, see
 * http://www.gnu.org/licenses/.
 */

package net.devbase.jfreesteel;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import javax.xml.bind.DatatypeConverter;

/**
 * Functions to print and convert bytes into strings.
 *
 * @author Goran Rakic (grakic@devbase.net)
 */
public class Utils {

    private static final Logger log = LoggerFactory.getLogger(Utils.class);

    private Utils() {}  // No instantiation, please.

    /**
     * Formats an integer as a hex string of little-endian, non-zero-padded
     * bytes.
     *
     * @param i the integer to format
     * @return the formatted string representing the integer
     */
    public static String int2HexString(final int i) {
        return bytes2HexString(asByteArray(i >>> 24, i >>> 16, i >>> 8, i));
    }

    /**
     * Formats an array of bytes as a string showing values in hex.
     * <p>
     * Silently skips leading zero bytes.
     * <p>
     *
     * @param bytes the bytes to print
     * @return formatted byte string, e.g. an array of 0x00, 0x01, 0x02, 0x03
     *         gets printed as: "01:02:03"
     */
    public static String bytes2HexString(byte... bytes) {
        return bytes2HexStringWithSeparator(":", bytes);
    }

    /**
     * Same as bytes2HexString(), but without bytes seprator
     * @param bytes
     * @return
     */
    public static String bytes2HexStringCompact(byte... bytes) {
    	return bytes2HexStringWithSeparator("", bytes);    	
    }

    private static String bytes2HexStringWithSeparator(String separator, byte... bytes) {
    	StringBuffer sb = new StringBuffer();
        boolean first = true;
        for (byte b : bytes) {
            if (first && b == 0x00) {
                continue;
            }
            if (!first) {
            	sb.append(separator);
            }
            sb.append(String.format("%02X", b));
            first = false;
        }
        return sb.toString();
    }

    /**
     * Formats a map of objects to byte arrays for printing.
     * <p>
     * Each map entry is written out as a string line, map key first, then an
     * equals sign, then an UTF-8 string interpreted from the bytes, then the
     * recovered bytes written out as strings.
     * <p>
     * The map keys are sorted by the natural order of the key type.
     * <p>
     * Example:
     *
     * <pre>
     * {@code
     * 42 = Hello World ()
     * }
     * </pre>
     *
     * @param <T> a comparable key type (comparable for sorting)
     * @param map the map to format
     * @return the formatted string, one line each.
     */
    public static <T extends Comparable<T>> String map2UTF8String(Map<T, byte[]> map) {
        StringBuilder builder = new StringBuilder();
        for (Map.Entry<?, byte[]> entry : map.entrySet()) {
            byte[] value = entry.getValue();
            builder.append(
                    String.format("%d = %s (%s)\n",
                    entry.getKey(),
                    bytes2UTF8String(value),
                    bytes2HexString(value)));
        }
        return builder.toString();
    }

    /**
     * Interprets an array of bytes as an string in a given charset.
     *
     * @param charsetName Charset name known to Java String class (UTF-8, ISO-8859-1,...).
     * @param bytes the bytes to convert to string, {@code null} allowed.
     */
    public static String bytes2String(String charsetName, byte... bytes) {
        try {
            return new String(bytes, charsetName);
        } catch (UnsupportedEncodingException ex) {
            log.warn(String.format("Could not convert bytes to unknown encoding %s", 
                    charsetName, ex));
        } catch (RuntimeException ex) {
            log.warn(String.format("Could not convert bytes to %s: %s, %s", 
                    charsetName, bytes2HexString(bytes), ex));
        } 
        return null;
    }

    /**
     * Interprets an array of bytes as an UTF-8 string.
     * <p>
     * Failing that, interprets as ISO-8859-1 or return a hex string.
     *
     * @param bytes the bytes to convert to string, {@code null} allowed.
     */
    public static String bytes2UTF8String(byte... bytes) {
        if (bytes == null) {
            return "";
        }
        String ret;
        if ((ret = bytes2String("UTF-8", bytes)) != null) {
            return ret;
        }
        else if ((ret = bytes2String("ISO-8859-1", bytes)) != null) {
            return ret;
        }
        else {
            return bytes2HexString(bytes);
        }
    }

    private static byte asByte(int value) {
        return (byte) (value & 0xFF);
    }

    public static byte[] asByteArray(int... values) {
        byte[] valueBytes = new byte[values.length];
        for (int i = 0; i < values.length; i++) {
            valueBytes[i] = asByte(values[i]);
        }
        return valueBytes;
    }

    public static byte[] int2Bytes(int value) {
        byte bytes[] = new byte[4];
        bytes[0] = asByte(value >> 0);
        bytes[1] = asByte(value >> 8);;
        bytes[2] = asByte(value >> 16);
        bytes[3] = asByte(value >> 24);
        return bytes;
    }

    public static int bytes2Int(byte... bytes) {
        int value = 0;
        int shift = 0;
        for(byte b : bytes) {
            value |= (0xff & b) << shift;
            shift+=8;
        }
        return value;
    }

    public static boolean allEquals(int needle, byte... bytes) {
        int i = 0;
        while(i < bytes.length && bytes[i] == needle) i++;
        return i == bytes.length;
    }    

    public static String image2Base64String(Image image) {
        BufferedImage bufferedImage = new BufferedImage(
                image.getWidth(null), image.getHeight(null),
                BufferedImage.TYPE_INT_BGR);
        bufferedImage.createGraphics().drawImage(image, 0, 0, null);

        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            ImageIO.write(bufferedImage, "jpg", output);
            byte[] bytes = output.toByteArray();
            return DatatypeConverter.printBase64Binary(bytes);
        } catch (IOException e) {
            log.warn("Error writing image to byte array", e);
        }
        return null;
    }

    /* Compares two java version strings */
    static public int compareVersions(String v1, String v2) throws RuntimeException {

        final Pattern versionPattern = Pattern.compile("([0-9]+)\\.([0-9]+)\\.([0-9]+)(_([0-9]+))?");
        Matcher m1 = versionPattern.matcher(v1);
        Matcher m2 = versionPattern.matcher(v2);

        if (m1.matches() && m2.matches()) {
            int p1, p2;

            // Feature major
            p1 = Integer.parseInt(m1.group(1));
            p2 = Integer.parseInt(m2.group(1));
            if (p1 != p2) return p2-p1;

            // Feature minor
            p1 = Integer.parseInt(m1.group(2));
            p2 = Integer.parseInt(m2.group(2));
            if (p1 != p2) return p2-p1;

            // Maintenance
            p1 = Integer.parseInt(m1.group(3));
            p2 = Integer.parseInt(m2.group(3));
            if (p1 != p2) return p2-p1;

            // Update
            String u1 = m1.group(5);
            String u2 = m2.group(5);
            if (u1 == null && u2 == null) return 0;
            if (u1 != null && u2 == null) return -1;
            if (u1 == null && u2 != null) return 1;

            p1 = Integer.parseInt(u1.replaceFirst ("^0*", ""));
            p2 = Integer.parseInt(u2.replaceFirst ("^0*", ""));
            return p2-p1;

        } else {
            throw new RuntimeException("Can not compare version "+v1+" to "+v2);
        }
    }    
}
