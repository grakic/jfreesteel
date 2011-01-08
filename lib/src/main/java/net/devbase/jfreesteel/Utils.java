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

import java.util.Map;

/**
 * Some utility functions to print and convert bytes into strings
 * 
 * @author Goran Rakic <grakic@devbase.net>
 */
public class Utils {
    
    public static String int2HexString(final int i)
    {
    	return bytes2HexString(new byte[] {
    			(byte)(i >>> 24),
                (byte)(i >>> 16),
                (byte)(i >>> 8),
                (byte) i});
    }
    
    public static String bytes2HexString(final byte[] bytes)
    {
        String result = "", sep = "";
        for(byte b:bytes)
        {
        	if(sep == "" && b == 0x00) continue;
        	
        	result += sep + String.format("%02X", b);
        	sep = ":";
        }
        return result;
    }   
    
    public static String map2UTF8String(Map<Integer, byte[]> map)
    {
    	String out = "";
    	
    	for(Integer i:map.keySet())
    	{
    		out += i + " = " + bytes2UTF8String(map.get(i)) + "\n";
    	}
    	
    	return out;
    }
    
    public static String bytes2UTF8String(final byte[] bytes)
    {
    	if(bytes == null) return "";
    	
    	try {
    		return new String(bytes, "UTF-8");
    	}
    	catch(Exception e)
    	{
    		return new String(bytes);
    	}
    }
    
}
