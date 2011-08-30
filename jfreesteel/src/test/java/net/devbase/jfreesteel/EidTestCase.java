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

import junit.framework.TestCase;

public abstract class EidTestCase extends TestCase {

    protected void assertContains(String expected, String actual) {
        assertTrue(
            String.format("'%s' should contain '%s' but does not", actual, expected),
            actual.contains(expected));
    }

    protected byte asByte(int value) {
        return (byte) (value & 0xff);
    }
}
