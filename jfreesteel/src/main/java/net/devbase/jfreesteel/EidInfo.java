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

import java.util.HashMap;
import java.util.Map;

import org.json.simple.JSONObject;

/**
 * Simple class to hold and reformat data read from eID
 *
 * @author Filip Miletic (filmil@gmail.com)
 * @author Nikolic Aleksandar (nikolic.alek@gmail.com)
 */
public class EidInfo {

    /** eID information codes. */
    public enum Tag {
        /** Dummy tag */
        NULL(0, "ignored", "Ignored"),

        /** Registered document number. */
        DOC_REG_NO(101, "doc_reg_no", "Document reg. number"),
        /** The issuing date, e.g. 01.01.2011. */
        ISSUING_DATE(102, "issuing_date", "Issuing date"),
        /** The date that the ID expires */
        EXPIRY_DATE(103, "expiry_date", "Expiry date"),
        /** The authority, e.g. "Ministry of the Interior". */
        ISSUING_AUTHORITY(104, "issuing_authority", "Issuing authority"),

        /** The person's unique identifier number.
         *
         * While mostly unique, due to the non-bulletproof number allocation scheme, has actually
         * been known to repeat for some rare individuals. The last digit is mod 11 checksum, but
         * due the same reason, there exist numbers with incorrect checksum.
         */
        PERSONAL_NUMBER(201, "personal_number", "Personal number"),
        /** Person's last name, e.g. "Smith" for some John Smith */
        SURNAME(202, "surname", "Surname"),
        /** The given name, e.g. "John" for some John Smith. */
        GIVEN_NAME(203, "given_name", "Given name"),
        /**
         * The parent's given name, the usual 'parenthood' middle name used to disambiguate
         * similarly named persons.
         * <p>
         * E.g. "Wiley" for some John Wiley Smith.
         */
        PARENT_GIVEN_NAME(204, "parent_given_name", "Parent given name"),
        /** The gender of the person. */
        SEX(205, "sex", "Gender"),

        /** The place the person was born in, e.g. "Belgrade" */
        PLACE_OF_BIRTH(301, "place_of_birth", "Place of birth"),
        /** The community/municipality the person was born in, e.g. "Savski Venac" */
        COMMUNITY_OF_BIRTH(302, "community_of_birth", "Community of birth"),
        STATE_OF_BIRTH(303, "state_of_birth", "State of birth"),
        STATE_OF_BIRTH_CODE(304, "state_of_birth_code", "State of birth code"),
        DATE_OF_BIRTH(305, "date_of_birth", "Date of birth"),

        /** The state of the person residence */
        STATE(401, "state", "State"),
        /** The community/municipality of the person residence */
        COMMUNITY(402, "community", "Community"),
        /** The place of the person residence */
        PLACE(403, "place", "Place"),
        /** The street name of the person residence */
        STREET(404, "street", "Street name"),
        /** The house number of the person residence */
        HOUSE_NUMBER(405, "house_number", "House number"),
        /** The house letter of the person residence */
        HOUSE_LETTER(406, "house_letter", "House letter"),
        /** The entrance label of the person residence */
        ENTRANCE(407, "entrance_label", "Entrance label"),
        /** The floor number of the person residence */
        FLOOR(408, "floor_number", "Floor number"),
        /** The appartment number of the person residence */
        APPARTMENT_NUMBER(409, "appartment_number", "Appartment number"),
        /** Address update date */
        ADDRESS_DATE(410, "address_date", "Address date");

        private final int code;
        private final String key;
        private final String name;

        /**
         * Initializes a tag with the corresponding raw encoding value.
         */
        Tag(int code, String key, String name) {
            this.code = code;
            this.key = key;
            this.name = name;
        }
        /** Gets the numeric tag code corresponding to this enum. */
        public int getCode() {
            return code;
        }
        /** Gets the string tag key corresponding to this enum. */
        public String getKey() {
            return key;
        }
        /** Gets the string tag name corresponding to this enum. */
        public String getName() {
            return name;
        }
        @Override
        public String toString() {
            return name;
        }
    }

    /** Builds an instance of EID info. */
    public static class Builder {

        Map<Tag, String> builder;

        public Builder() {
            builder = new HashMap<Tag, String>();
        }

        /**
        * Adds the value to the information builder.
        *
        * @throws IllegalArgumentException if the same tag is added twice.
        */
        public Builder addValue(Tag tag, String value) {
            builder.put(tag, value);
            return this;
        }

        public EidInfo build() {
            return new EidInfo(builder);
        }
    }

    private Map<Tag, String> fields;

    private EidInfo(Map<Tag, String> fields) {
        this.fields = fields;
    }

    /** Returns the value associated with the supplied tag. */
    public String get(Tag tag) {
        return fields.get(tag);
    }

    /** Returns if there is a value associated with the supplied tag. */
    public boolean has(Tag tag) {
    	String value = get(tag);
    	return (value != null && value.length() > 0);
    }

    /** Append tag value using given separator as a prefix. */
    private void appendTo(StringBuilder builder, String separator, Tag tag) {
        if (has(tag)) {
            builder.append(separator);
            builder.append(get(tag));
        }
    }

    /** Append tag value without any prefix. */
    private void appendTo(StringBuilder builder, Tag tag) {
        appendTo(builder, "", tag);
    }

    /** Append formatted tag value using given separator as a prefix */
    private void appendTo(StringBuilder builder, String separator, String format, Tag tag) {
        if (has(tag)) {
            builder.append(separator);
            builder.append(String.format(format, get(tag)));
        }
    }

    /**
     * Require at least one %s in the format string
     *
     * If given format is null or does not contain %s, replace with "%s"
     */
    private String sanitizeFormat(String format) {
        return (format != null && format.contains("%s"))
            ? format
            : "%s";
    }
    
    private String formatDate(String date) {
        return date.length() == 8 ? 
                String.format("%s.%s.%s", date.substring(0, 2), date.substring(2, 4), date.substring(4, 8)) 
                : date;
    }

    /**
     * Get given name, parent given name and a surname as a single string.
     *
     * @return Nicely formatted full name
     */
    public String getNameFull() {
        return String.format(
            "%s %s %s", get(Tag.GIVEN_NAME), get(Tag.PARENT_GIVEN_NAME), get(Tag.SURNAME));
    }

    /**
     * Get place of residence as multiline string. Format parameters can be used to provide better
     * output or null/empty strings can be passed for no special formating.
     *
     * For example if floorLabelFormat is "%s. sprat" returned string will contain "5. sprat" for
     * floor number 5.
     *
     * Recommended values for Serbian are "ulaz %s", "%s. sprat" and "br. %s"
     *
     * @param entranceLabelFormat String to format entrance label or null
     * @param floorLabelFormat String to format floor label or null
     * @param appartmentLabelFormat String to format apartment label or null
     * @return Nicely formatted place of residence as multiline string
     *
     * FIXME: Use one parameterized format string to allow both "Main street 11" and 
     * "11 Main street"
     * 
     * FIXME: Think about how to handle short form format with missing ENTRANCE/FLOOR 
     * label (line 298).
     */
    public String getPlaceFull(
            String entranceLabelFormat, String floorLabelFormat, String appartmentLabelFormat) {

        StringBuilder out = new StringBuilder();

        entranceLabelFormat = sanitizeFormat(entranceLabelFormat);
        floorLabelFormat = sanitizeFormat(floorLabelFormat);
        appartmentLabelFormat = sanitizeFormat(appartmentLabelFormat);

        // Main street, Main street 11, Main street 11A
        appendTo(out, Tag.STREET);
        appendTo(out, " ", Tag.HOUSE_NUMBER);
        appendTo(out, Tag.HOUSE_LETTER);

        // For entranceLabel = "ulaz %s" gives "Main street 11A ulaz 2"
        appendTo(out, " ", entranceLabelFormat, Tag.ENTRANCE);

        // For floorLabel = "%s. sprat" gives "Main street 11 ulaz 2, 5. sprat"
        appendTo(out, ", ", floorLabelFormat, Tag.FLOOR);

        if (has(Tag.APPARTMENT_NUMBER)) {
            // For appartmentLabel "br. %s" gives "Main street 11 ulaz 2, 5. sprat, br. 4"
            if (has(Tag.ENTRANCE) || has(Tag.FLOOR)) {
                appendTo(out, ", ", appartmentLabelFormat, Tag.APPARTMENT_NUMBER);
            } else {
                // short form: Main street 11A/4
                appendTo(out, "/", Tag.APPARTMENT_NUMBER);
            }
        }

        appendTo(out, "\n", Tag.PLACE);
        appendTo(out, ", ", Tag.COMMUNITY);

        String rawState = get(Tag.STATE);

        out.append("\n");

        if (rawState.contentEquals("SRB")) {
            // small cheat for a better output
            out.append("REPUBLIKA SRBIJA");
        } else {
            out.append(rawState);
        }

        return out.toString();
    }

    /**
     * Get full place of birth as a multiline string, including community and state if present.
     *
     * @return Nicely formatted place of birth as a multiline string.
     */
    public String getPlaceOfBirthFull()
    {
        StringBuilder out = new StringBuilder();

        appendTo(out, Tag.PLACE_OF_BIRTH);
        appendTo(out, ", ", Tag.COMMUNITY_OF_BIRTH);
        appendTo(out, "\n", Tag.STATE_OF_BIRTH);

        return out.toString();
    }

    public String getDocRegNo() {
        return get(Tag.DOC_REG_NO);
    }
    public String getIssuingDate() {
        return formatDate(get(Tag.ISSUING_DATE));
    }
    public String getExpiryDate() {
        return formatDate(get(Tag.EXPIRY_DATE));
    }
    public String getIssuingAuthority() {
        return get(Tag.ISSUING_AUTHORITY);
    }
    public String getPersonalNumber() {
        return get(Tag.PERSONAL_NUMBER);
    }
    public String getSurname() {
        return get(Tag.SURNAME);
    }
    public String getGivenName() {
        return get(Tag.GIVEN_NAME);
    }
    public String getParentGivenName() {
        return get(Tag.PARENT_GIVEN_NAME);
    }
    public String getSex() {
        return get(Tag.SEX);
    }
    public String getPlaceOfBirth() {
        return get(Tag.PLACE_OF_BIRTH);
    }
    public String getCommunityOfBirth() {
        return get(Tag.COMMUNITY_OF_BIRTH);
    }
    public String getStateOfBirth() {
        return get(Tag.STATE_OF_BIRTH);
    }
    public String getDateOfBirth() {
        return formatDate(get(Tag.DATE_OF_BIRTH));
    }
    public String getState() {
        return get(Tag.STATE);
    }
    public String getCommunity() {
        return get(Tag.COMMUNITY);
    }
    public String getPlace() {
        return get(Tag.PLACE);
    }
    public String getStreet() {
        return get(Tag.STREET);
    }
    public String getHouseNumber() {
        return get(Tag.HOUSE_NUMBER);
    }
    public String getHouseLetter() {
        return get(Tag.HOUSE_LETTER);
    }
    public String getEntrance() {
        return get(Tag.ENTRANCE);
    }
    public String getFloor() {
        return get(Tag.FLOOR);
    }
    public String getAppartmentNumber() {
        return get(Tag.APPARTMENT_NUMBER);
    }
    public String getAddressDate() {
        String value = get(Tag.ADDRESS_DATE);
        if (value == null || value.equals("01010001"))
            return null;
        return formatDate(get(Tag.APPARTMENT_NUMBER));
    }

    @Override
    public String toString() {
        StringBuilder out = new StringBuilder();
        for (Map.Entry<Tag, String> field : fields.entrySet()) {
            out.append(String.format("%s: %s", field.getKey(), field.getValue()));
        }
        return out.toString();
    }

    @SuppressWarnings("unchecked")
	public JSONObject toJSON() {
        JSONObject obj = new JSONObject();
        obj.put("name_full", getNameFull());
        obj.put("place_full", getPlaceFull("ulaz %s", "%s. sprat", "br. %s"));
        obj.put("place_of_birth_full", getPlaceOfBirthFull());
        for (Map.Entry<Tag, String> field : fields.entrySet()) {
            Tag tag = field.getKey();
            obj.put(tag.getKey(), field.getValue());
        }
        return obj;
    }
}
