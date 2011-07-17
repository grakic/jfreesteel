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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple class to hold and reformat data read from eID
 * 
 * @author Nikolic Aleksandar (nikolic.alek@gmail.com)
 */
public class EidInfo {

    private final static Logger logger = LoggerFactory.getLogger(EidInfo.class);
    
    private String docRegNo = "";
    private String issuingDate = "";
    private String expiryDate = "";
    private String issuingAuthority = "";
    
    private String personalNumber = "";
    private String surname = "";
    private String givenName = "";
    private String parentGivenName = "";
    private String sex = "";
    private String placeOfBirth = "";
    private String communityOfBirth = "";
    private String stateOfBirth = "";
    private String dateOfBirth = "";
    
    private String state = "";
    private String community = "";
    private String place = "";
    private String street = "";
    private String houseNumber = "";
    private String houseLetter = "";
    private String entrance = "";
    private String floor = "";
    private String appartmentNumber = "";
    
    public EidInfo() {

    }

    /**
     * Get given name, parent given name and a surname as a single string.
     *
     * @return Nicely formatted full name
     */
    public String getNameFull() {
        return givenName + " " + parentGivenName + " " + surname;
    }

    /**
     * Get place of residence as multiline string. Format paramters can be used to provide better
     * output or null/empty strings can be passed for no special formating.
     * 
     * For example if floorLabelFormat is "%s. sprat" returned string will contain "5. sprat" for
     * floor number 5.
     * 
     * Recommended values for Serbian are "ulaz %s", "%s. sprat" and "br. %s"
     * 
     * @param entranceLabelFormat String to format entrance label or null
     * @param floorLabelFormat String to format floor label or null
     * @param appartmentLabelFormat String to format appartment label or null
     * @return Nicely formatted place of residence as multiline string
     */
    public String getPlaceFull(String entranceLabelFormat, String floorLabelFormat, String appartmentLabelFormat) {
        StringBuilder out = new StringBuilder();

        if (entranceLabelFormat == null || entranceLabelFormat.isEmpty()) {
            entranceLabelFormat = "%s";
        }
        if (floorLabelFormat == null || floorLabelFormat.isEmpty()) {
            floorLabelFormat = "%s";
        }
        if (appartmentLabelFormat == null || appartmentLabelFormat.isEmpty()) {
            appartmentLabelFormat = "%s";
        }
        
        if (!street.isEmpty()) {
            // Neka ulica
            out.append(street);                   
        }
        if (!houseNumber.isEmpty()) {
            // Neka ulica 11
            out.append(" ");
            out.append(houseNumber);
        }
        if (!houseLetter.isEmpty()) {
            // Neka ulica 11A
            out.append(houseLetter);
        }
        
        // For entranceLabel = "ulaz %s" gives "Neka ulica 11A ulaz 2"
        if (!entrance.isEmpty()) {
            out.append(" ");
            out.append(String.format(entranceLabelFormat, entrance));
        }

        // For floorLabel = "%s. sprat" gives "Neka ulica 11 ulaz 2, 5. sprat"
        if (!floor.isEmpty()) {
            out.append(", ");
            out.append(String.format(floorLabelFormat, floor));
        }

        if (!appartmentNumber.isEmpty()) {
            // For appartmentLabel "br. %s" gives "Neka ulica 11 ulaz 2, 5. sprat, br. 4"
            if (!entrance.isEmpty() || !floor.isEmpty()) {
                out.append(", " + String.format(appartmentLabelFormat, appartmentNumber));
            } else {
                // short form: Neka ulica 11A/4
                out.append("/" + appartmentNumber);
            }
        }

        out.append("\n");
        out.append(place);
        if (!community.isEmpty()) {
            out.append(", ");
            out.append(community);
        }

        out.append("\n");
        if (state.contentEquals("SRB")) {
            // small cheat for better output
            out.append("REPUBLIKA SRBIJA");
        } else {
            out.append(state);
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
        String out = new String(placeOfBirth);

        if (!communityOfBirth.isEmpty()) {
            out += ", " + communityOfBirth;
        }
        if (!stateOfBirth.isEmpty()) {
            out += "\n" + stateOfBirth;
        }
        
        return out;
    }
    
    public String getDocRegNo() {
        return docRegNo;
    }
    public void setDocRegNo(String docRegNo) {
        this.docRegNo = docRegNo;
    }
    public String getIssuingDate() {
        return issuingDate;
    }
    public void setIssuingDate(String issuingDate) {
        this.issuingDate = issuingDate;
    }
    public String getExpiryDate() {
        return expiryDate;
    }
    public void setExpiryDate(String expiryDate) {
        this.expiryDate = expiryDate;
    }
    public String getIssuingAuthority() {
        return issuingAuthority;
    }
    public void setIssuingAuthority(String issuingAuthority) {
        this.issuingAuthority = issuingAuthority;
    }
    public String getPersonalNumber() {
        return personalNumber;
    }
    public void setPersonalNumber(String personalNumber) throws Exception
    {
        // there are valid personal numbers with invalid checksum, check for format only
        Pattern pattern = Pattern.compile("^[0-9]{13}$", Pattern.CASE_INSENSITIVE);  
        Matcher matcher = pattern.matcher(personalNumber);  
        if (matcher.matches()) {  
            this.personalNumber = personalNumber;
        } else {
            throw new Exception("Invalid personal number.");
        }
    }
    public String getSurname() {
        return surname;
    }
    public void setSurname(String surname) {
        this.surname = surname;
    }
    public String getGivenName() {
        return givenName;
    }
    public void setGivenName(String givenName) {
        this.givenName = givenName;
    }
    public String getParentGivenName() {
        return parentGivenName;
    }
    public void setParentGivenName(String parentGivenName) {
        this.parentGivenName = parentGivenName;
    }
    public String getSex() {
        return sex;
    }
    public void setSex(String sex) {
        this.sex = sex;
    }
    public String getPlaceOfBirth() {
        return placeOfBirth;
    }
    public void setPlaceOfBirth(String placeOfBirth) {
        this.placeOfBirth = placeOfBirth;
    }
    public String getCommunityOfBirth() {
        return communityOfBirth;
    }
    public void setCommunityOfBirth(String communityOfBirth) {
        this.communityOfBirth = communityOfBirth;
    }
    public String getStateOfBirth() {
        return stateOfBirth;
    }
    public void setStateOfBirth(String stateOfBirth) {
        this.stateOfBirth = stateOfBirth;
    }
    public String getDateOfBirth() {
        return dateOfBirth;
    }
    public void setDateOfBirth(String dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }
    public String getState() {
        return state;
    }
    public void setState(String state) {
        this.state = state;
    }
    public String getCommunity() {
        return community;
    }
    public void setCommunity(String community) {
        this.community = community;
    }
    public String getPlace() {
        return place;
    }
    public void setPlace(String place) {
        this.place = place;
    }
    public String getStreet() {
        return street;
    }
    public void setStreet(String street) {
        this.street = street;
    }
    public String getHouseNumber() {
        return houseNumber;
    }
    public void setHouseNumber(String houseNumber) {
        this.houseNumber = houseNumber;
    }
    public String getHouseLetter() {
        return houseLetter;
    }
    public void setHouseLetter(String houseLetter) {
        this.houseLetter = houseLetter;
    }
    public String getEntrance() {
        return entrance;
    }
    public void setEntrance(String entrance) {
        this.entrance = entrance;
    }
    public String getFloor() {
        return floor;
    }
    public void setFloor(String floor) {
        this.floor = floor;
    }
    public String getAppartmentNumber() {
        return appartmentNumber;
    }
    public void setAppartmentNumber(String appartmentNumber) {
        this.appartmentNumber = appartmentNumber;
    }
    
    // For debug
    @Override
    public String toString() {
        return  givenName + " " + parentGivenName + " " + surname + "(" + personalNumber + "/" +
                sex + ")\n" + docRegNo  + " " + issuingDate + "-" + expiryDate + ", " + 
                issuingAuthority + "\n" + state + ", " + place + ", " + community + ", " + street +
                " " + houseNumber + houseLetter + " " + entrance + " " + floor + " " +
                appartmentNumber + "\n" + dateOfBirth + ", " + placeOfBirth + ", " + 
                communityOfBirth + " " + stateOfBirth + "\n";
    }
}
