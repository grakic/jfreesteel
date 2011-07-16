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

import java.util.Arrays;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;

/**
 * Functions to print and convert bytes into strings.
 * 
 * @author Goran Rakic (grakic@devbase.net)
 */
public class Utils {
    
    private static final Logger log = LoggerFactory.getLogger(Utils.class);

    private Utils() {}  // No instantiation, please.

    /**
     * Formats an integer as a hex string of little-endian, non-zero-padded bytes.
     *
     * @param i the integer to format
     * @return the formatted string representing the integer
     */
    public static String int2HexString(final int i)
    {
        return bytes2HexString(asByteArray(i >>> 24, i >>> 16, i >>> 8, i));
    }

    /**
     * Formats an array of bytes as a string.
     * <p>
     * Silently skips leading zero bytes.
     * TODO(filmil): Silent zero-skipping is weird, check if this is the right thing to do.
     *
     * @param bytes the bytes to print
     * @return formatted byte string, e.g. an array of 0x00, 0x01, 0x02, 0x03 gets printed as: "01:02:03"
     */
    public static String bytes2HexString(byte... bytes) {
        ImmutableList.Builder<String> builder = ImmutableList.builder();
        boolean skipZeros = true;
        for (byte b : bytes) {
            if (skipZeros && b == 0x00) {
                continue;
            }
            skipZeros = false;
            builder.add(String.format("%02X", b));
        }
        return Joiner.on(":").join(builder.build());
    }

    /**
     * Formats a map of integer to byte arrays for printing.
     * <p>
     * Each map entry is written out as a string line, map key first, then an equals sign, then
     * an UTF-8 string interpreted from the bytes.
     * <p>
     * The keys are written out in random order (due to set iteration).
     * TODO(filmil): Why are they written out randomly?
     *
     * @param map the map to format
     * @return the formatted string, one line each.
     */
    public static String map2UTF8String(Map<Integer, byte[]> map) {
        StringBuilder builder = new StringBuilder();
        for (Map.Entry<Integer, byte[]> entry : map.entrySet()) {
            builder.append(
                String.format("%d = %s\n",
                entry.getKey(), bytes2UTF8String(entry.getValue())));
        }
        return builder.toString();
    }

    /**
     * Interprets an array of bytes as an UTF-8 string.
     * <p>
     * Failing that, interprets as ISO-8859-1.
     *
     * Note: Do not use varargs byte... bytes as we could not pass
     * null without explicit cast to byte.
     */
    public static String bytes2UTF8String(byte[] bytes)
    {
        if(bytes == null) {
            return "";
        }
        try {
            return new String(bytes, Charsets.UTF_8);
        } catch(RuntimeException ex) {
            log.warn(
                String.format("Could not convert bytes to UTF-8: %s, %s",
                bytes2HexString(bytes), ex));
            return new String(bytes, Charsets.ISO_8859_1);
        }
    }

    private static byte asByte(int value) {
        return (byte) (value & 0xFF);
    }    

    private static byte[] asByteArray(int... values) {
        byte[] valueBytes = new byte[values.length];
        for(int i = 0; i < values.length; i++) {
            valueBytes[i] = asByte(values[i]);
        }
        return valueBytes;
    }
}
