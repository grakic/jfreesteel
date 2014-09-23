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

import net.devbase.jfreesteel.EidInfo.Builder;
import net.devbase.jfreesteel.EidInfo.Tag;

import org.json.simple.JSONObject;

/**
 * @author filmil@gmail.com (Filip Miletic)
 */
public class EidInfoTest extends EidTestCase {

    public void testKnownTag() {
        EidInfo info = new EidInfo.Builder()
            .addValue(EidInfo.Tag.DOC_REG_NO, "1000")
            .build();
        assertEquals("1000", info.get(Tag.DOC_REG_NO));

        assertContains("Document reg. number: 1000", info.toString());
    }

    public void testValidation() {
        EidInfo info = new EidInfo.Builder()
            .addValue(Tag.PERSONAL_NUMBER, "0000000000000")
            .build();
        assertEquals("0000000000000", info.get(Tag.PERSONAL_NUMBER));
    }

    public void testPlaceOfBirth() {
        EidInfo info = new EidInfo.Builder()
            .addValue(Tag.PLACE_OF_BIRTH, "City")
            .addValue(Tag.COMMUNITY_OF_BIRTH, "Community")
            .addValue(Tag.STATE_OF_BIRTH, "State")
            .build();
        assertEquals("City, Community\nState", info.getPlaceOfBirthFull());
    }

    public void testGetPlaceFull() {
        Builder builder = new EidInfo.Builder()
            .addValue(Tag.STREET, "Street")
            .addValue(Tag.HOUSE_NUMBER, "55")
            .addValue(Tag.HOUSE_LETTER, "letter")
            .addValue(Tag.ENTRANCE, "entrance")
            .addValue(Tag.FLOOR, "floor")
            .addValue(Tag.PLACE, "Place")
            .addValue(Tag.COMMUNITY, "Community")
            .addValue(Tag.STATE, "State");
        assertEquals(
            "Street 55letter entrance, floor\n" +
            "Place, Community\n" +
            "State",
            builder.build().getPlaceFull(null, null, null));
        assertEquals(
            "Street 55letter entrance, floor\n" +
            "Place, Community\n" +
            "State",
            builder.build().getPlaceFull("", "", ""));

        builder.addValue(Tag.APPARTMENT_NUMBER, "1212");
        assertEquals(
            "Street 55letter entrance, floor, 1212\n" +
            "Place, Community\n" +
            "State",
            builder.build().getPlaceFull(null, null, null));
    }

    public void testGetPlaceFull_noEntranceAndNoFloor() {
        Builder builder = new EidInfo.Builder()
            .addValue(Tag.STREET, "Street")
            .addValue(Tag.HOUSE_NUMBER, "55")
            .addValue(Tag.HOUSE_LETTER, "L")
            .addValue(Tag.APPARTMENT_NUMBER, "1212")
            .addValue(Tag.PLACE, "Place")
            .addValue(Tag.COMMUNITY, "Community")
            .addValue(Tag.STATE, "State");
        assertEquals(
            "Street 55L/1212\n" +
            "Place, Community\n" +
            "State",
            builder.build().getPlaceFull(null, null, null));
    }

    public void testGetPlaceFull_srb() {
        Builder builder = new EidInfo.Builder()
            .addValue(Tag.STREET, "Street")
            .addValue(Tag.HOUSE_NUMBER, "55")
            .addValue(Tag.HOUSE_LETTER, "L")
            .addValue(Tag.APPARTMENT_NUMBER, "1212")
            .addValue(Tag.PLACE, "Place")
            .addValue(Tag.COMMUNITY, "Community")
            .addValue(Tag.STATE, "SRB");
        assertEquals("Street 55L/1212\n" +
                "Place, Community\n" +
                "REPUBLIKA SRBIJA", builder.build().getPlaceFull(null, null, null));
    }

    public void testGetPlaceFull_formatted() {
        Builder builder = new EidInfo.Builder()
            .addValue(Tag.STREET, "Street")
            .addValue(Tag.HOUSE_NUMBER, "55")
            .addValue(Tag.HOUSE_LETTER, "L")
            .addValue(Tag.ENTRANCE, "E")
            .addValue(Tag.FLOOR, "666")
            .addValue(Tag.PLACE, "Place")
            .addValue(Tag.COMMUNITY, "Community")
            .addValue(Tag.STATE, "State");
        assertEquals(
            "Street 55L AA E, BB 666\n" +
            "Place, Community\n" +
            "State",
            builder.build().getPlaceFull("AA %s", "BB %s", "CC %s"));
    }

    public void testGetNameFull() {
        EidInfo info = new EidInfo.Builder()
            .addValue(Tag.GIVEN_NAME, "Name")
            .addValue(Tag.PARENT_GIVEN_NAME, "Parent")
            .addValue(Tag.SURNAME, "Surname")
            .build();
        assertEquals("Name Parent Surname", info.getNameFull());
    }

    private EidInfo buildEidInfoForSimpleGetters() {
        Builder builder = new EidInfo.Builder()
            .addValue(Tag.DOC_REG_NO, "1000")
            .addValue(Tag.ISSUING_DATE, "12.12.2010")
            .addValue(Tag.EXPIRY_DATE, "12.12.2014")
            .addValue(Tag.ISSUING_AUTHORITY, "MUP R SRBIJE")
            .addValue(Tag.PERSONAL_NUMBER, "0000000000000")
            .addValue(Tag.SURNAME, "Surname")
            .addValue(Tag.GIVEN_NAME, "Name")
            .addValue(Tag.PARENT_GIVEN_NAME, "Parent")
            .addValue(Tag.SEX, "M")
            .addValue(Tag.PLACE_OF_BIRTH, "City of birth")
            .addValue(Tag.COMMUNITY_OF_BIRTH, "Community of birth")
            .addValue(Tag.STATE_OF_BIRTH, "State of birth")
            .addValue(Tag.DATE_OF_BIRTH, "01.01.1950")
            .addValue(Tag.STATE, "State")
            .addValue(Tag.COMMUNITY, "Community")
            .addValue(Tag.PLACE, "City")
            .addValue(Tag.STREET, "Street")
            .addValue(Tag.HOUSE_NUMBER, "55")
            .addValue(Tag.HOUSE_LETTER, "L")
            .addValue(Tag.ENTRANCE, "E")
            .addValue(Tag.FLOOR, "666")
            .addValue(Tag.APPARTMENT_NUMBER, "1212");
        return builder.build();
    }

    public void testJSON() {
        EidInfo info = buildEidInfoForSimpleGetters();
        JSONObject infoJson = info.toJSON();
        assertEquals(infoJson.get("doc_reg_no"), "1000");    
    }

    public void testGetDocRegNo() {
        EidInfo info = buildEidInfoForSimpleGetters();
        assertEquals("1000", info.get(Tag.DOC_REG_NO));
    }
    public void testGetIssuingDate() {
        EidInfo info = buildEidInfoForSimpleGetters();
        assertEquals("12.12.2010", info.get(Tag.ISSUING_DATE));
    }
    public void testGetExpiryDate() {
        EidInfo info = buildEidInfoForSimpleGetters();
        assertEquals("12.12.2014", info.get(Tag.EXPIRY_DATE));
    }
    public void testGetIssuingAuthority() {
        EidInfo info = buildEidInfoForSimpleGetters();
        assertEquals("MUP R SRBIJE", info.get(Tag.ISSUING_AUTHORITY));
    }
    public void testGetPersonalNumber() {
        EidInfo info = buildEidInfoForSimpleGetters();
        assertEquals("0000000000000", info.get(Tag.PERSONAL_NUMBER));
    }
    public void testGetSurname() {
        EidInfo info = buildEidInfoForSimpleGetters();
        assertEquals("Surname", info.get(Tag.SURNAME));
    }
    public void testGetGivenName() {
        EidInfo info = buildEidInfoForSimpleGetters();
        assertEquals("Name", info.get(Tag.GIVEN_NAME));
    }
    public void testGetParentGivenName() {
        EidInfo info = buildEidInfoForSimpleGetters();
        assertEquals("Parent", info.get(Tag.PARENT_GIVEN_NAME));
    }
    public void testGetSex() {
        EidInfo info = buildEidInfoForSimpleGetters();
        assertEquals("M", info.get(Tag.SEX));
    }
    public void testGetPlaceOfBirth() {
        EidInfo info = buildEidInfoForSimpleGetters();
        assertEquals("City of birth", info.get(Tag.PLACE_OF_BIRTH));
    }
    public void testGetCommunityOfBirth() {
        EidInfo info = buildEidInfoForSimpleGetters();
        assertEquals("Community of birth", info.get(Tag.COMMUNITY_OF_BIRTH));
    }
    public void testGetStateOfBirth() {
        EidInfo info = buildEidInfoForSimpleGetters();
        assertEquals("State of birth", info.get(Tag.STATE_OF_BIRTH));
    }
    public void testGetDateOfBirth() {
        EidInfo info = buildEidInfoForSimpleGetters();
        assertEquals("01.01.1950", info.get(Tag.DATE_OF_BIRTH));
    }
    public void testGetState() {
        EidInfo info = buildEidInfoForSimpleGetters();
        assertEquals("State", info.get(Tag.STATE));
    }
    public void testGetCommunity() {
        EidInfo info = buildEidInfoForSimpleGetters();
        assertEquals("Community", info.get(Tag.COMMUNITY));
    }
    public void testGetPlace() {
        EidInfo info = buildEidInfoForSimpleGetters();
        assertEquals("City", info.get(Tag.PLACE));
    }
    public void testGetStreet() {
        EidInfo info = buildEidInfoForSimpleGetters();
        assertEquals("Street", info.get(Tag.STREET));
    }
    public void testGetHouseNumber() {
        EidInfo info = buildEidInfoForSimpleGetters();
        assertEquals("55", info.get(Tag.HOUSE_NUMBER));
    }
    public void testGetHouseLetter() {
        EidInfo info = buildEidInfoForSimpleGetters();
        assertEquals("L", info.get(Tag.HOUSE_LETTER));
    }
    public void testGetEntrance() {
        EidInfo info = buildEidInfoForSimpleGetters();
        assertEquals("E", info.get(Tag.ENTRANCE));
    }
    public void testGetFloor() {
        EidInfo info = buildEidInfoForSimpleGetters();
        assertEquals("666", info.get(Tag.FLOOR));
    }
    public void testGetAppartmentNumber() {
        EidInfo info = buildEidInfoForSimpleGetters();
        assertEquals("1212", info.get(Tag.APPARTMENT_NUMBER));
    }
}
