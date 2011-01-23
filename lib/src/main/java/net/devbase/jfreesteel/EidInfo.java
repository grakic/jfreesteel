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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple class to hold and reformat data read from eID
 * 
 * @author Nikolic Aleksandar <nikolic.alek@gmail.com>
 */
public class EidInfo {

    private final static Logger logger = LoggerFactory.getLogger(EidInfo.class);
    
    private String docRegNo;
    private String issuingDate;
    private String expiryDate;
    private String issuingAuthority;
    
    private String personalNumber;
    private String surname;
    private String givenName;
    private String parentGivenName;
    private String sex;
    private String placeOfBirth;
    private String communityOfBirth;
    private String stateOfBirth;
    private String dateOfBirth;
    
    private String state;
    private String community;
    private String place;
    private String street;
    private String houseNumber;
    private String houseLetter;
    private String entrance;
    private String floor;
    private String appartmentNumber;
    
    public EidInfo(final Map<Integer, byte[]> document, final Map<Integer, byte[]> personal, final Map<Integer, byte[]> residence) throws Exception
    {
        setDocumentInfo(document);
        setPersonalInfo(personal);
        setResidenceInfo(residence);        
    }

    private void setDocumentInfo(final Map<Integer, byte[]> document)
    {
                            // tags: 1545 - 1553
                            // 1545 = SRB
                setDocRegNo(Utils.bytes2UTF8String(document.get(1546)));
                            // 1547 = ID
                            // 1548 = ID<docRegNo>
             setIssuingDate(Utils.bytes2UTF8String(document.get(1549)));
              setExpiryDate(Utils.bytes2UTF8String(document.get(1550)));
        setIssuingAuthority(Utils.bytes2UTF8String(document.get(1551)));
                            // 1552 = SC
                            // 1553 = SC
    }

    private void setPersonalInfo(final Map<Integer, byte[]> personal) throws Exception
    {
                            // tags: 1558 - 1567
    	  setPersonalNumber(Utils.bytes2UTF8String(personal.get(1558)));
                 setSurname(Utils.bytes2UTF8String(personal.get(1559)));
               setGivenName(Utils.bytes2UTF8String(personal.get(1560)));
         setParentGivenName(Utils.bytes2UTF8String(personal.get(1561)));
                     setSex(Utils.bytes2UTF8String(personal.get(1562)));
            setPlaceOfBirth(Utils.bytes2UTF8String(personal.get(1563)));
        setCommunityOfBirth(Utils.bytes2UTF8String(personal.get(1564)));
            setStateOfBirth(Utils.bytes2UTF8String(personal.get(1565)));        
             setDateOfBirth(Utils.bytes2UTF8String(personal.get(1566)));
                            // 1567 = SRB (stateOfBirth code?)
    }

    private void setResidenceInfo(final Map<Integer, byte[]> residence)
    {
        logger.error(Utils.map2UTF8String(residence));
        
                       // tags: 1568 .. 1578
              setState(Utils.bytes2UTF8String(residence.get(1568)));
          setCommunity(Utils.bytes2UTF8String(residence.get(1569)));
              setPlace(Utils.bytes2UTF8String(residence.get(1570)));
             setStreet(Utils.bytes2UTF8String(residence.get(1571)));
        setHouseNumber(Utils.bytes2UTF8String(residence.get(1572)));
        
        // FIXME: Get tags
        // 1573 .. 1577 ???
        // setHouseLetter(Utils.bytes2UTF8String(residence.get(1573))); // ??
        // setEntrance(Utils.bytes2UTF8String(residence.get(1576))); // ??
        // setFloor(Utils.bytes2UTF8String(residence.get(1577))); // ??
        houseLetter = ""; entrance = ""; floor = "";

        setAppartmentNumber(Utils.bytes2UTF8String(residence.get(1578)));
    }

    public String getNameFull()
    {
        return givenName + " " + parentGivenName + " " + surname;
    }

    /**
     * Get place of residence as multiline string. Format paramters
     * can be used to provide better output or null/empty strings can
     * be passed for no special formating.
     * 
     * For example if floorLabelFormat is "%s. sprat" returned string
     * will contain "5. sprat" for floor number 5.
     * 
     * Recommended values for Serbian are "ulaz %s", "%s. sprat" and "br. %s"
     * 
     * @param entranceLabelFormat
     * @param floorLabelFormat
     * @param appartmentLabelFormat
     * @return
     */
    public String getPlaceFull(String entranceLabelFormat, String floorLabelFormat, String appartmentLabelFormat)
    {
        StringBuilder out = new StringBuilder();

        if(entranceLabelFormat.isEmpty())   entranceLabelFormat = "%s";
        if(floorLabelFormat.isEmpty())      floorLabelFormat = "%s";
        if(appartmentLabelFormat.isEmpty()) appartmentLabelFormat = "%s";
        
        if(!street.isEmpty()) out.append(street);                   // Neka ulica
        if(!houseNumber.isEmpty()) {                                // Neka ulica 11
            out.append(" ");
            out.append(houseNumber);
        }
        if(!houseLetter.isEmpty()) out.append(houseLetter);        // Neka ulica 11A
        
        // For entranceLabel = "ulaz %s" gives "Neka ulica 11A ulaz 2"
        if(!entrance.isEmpty()) {
            out.append(" ");
            out.append(String.format(entranceLabelFormat, entrance));
        }

        // For floorLabel = "%s. sprat" gives "Neka ulica 11 ulaz 2, 5. sprat"
        if(!floor.isEmpty()) {
            out.append(", ");
            out.append(String.format(floorLabelFormat, floor));
        }

        if(!appartmentNumber.isEmpty()) {
            // For appartmentLabel "br. %s" gives "Neka ulica 11 ulaz 2, 5. sprat, br. 4"
            if(!entrance.isEmpty() || !floor.isEmpty()) out.append(", " + String.format(appartmentLabelFormat, appartmentNumber));
            else out.append("/" + appartmentNumber); // short form: Neka ulica 11A/4
        }

        out.append("\n");
        out.append(place);
        if(!community.isEmpty()) {
            out.append(", ");
            out.append(community);
        }

        out.append("\n");
        if(state.contentEquals("SRB")) {
            // small cheat for better output
            out.append("REPUBLIKA SRBIJA");
        }
        else out.append(state);
        
        return out.toString();
    }

    public String getPlaceOfBirthFull()
    {
        String out = new String(placeOfBirth);

        if(!communityOfBirth.isEmpty()) out += ", " + communityOfBirth;
        if(!stateOfBirth.isEmpty()) out += "\n" + stateOfBirth;
        
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
    	Pattern pattern = Pattern.compile("^[0-9]{13}$", Pattern.CASE_INSENSITIVE);  
        Matcher matcher = pattern.matcher(personalNumber);  
        if(matcher.matches()) {  
        	this.personalNumber = personalNumber;
        }  
        else throw new Exception("Invalid personal number.");
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
    
    // For testing
    @Override
    public String toString()
    {
        return  givenName + " " + parentGivenName + " " + surname + "(" + personalNumber + "/" + sex + ")\n" +
                docRegNo  + " " + issuingDate + "-" + expiryDate + ", " + issuingAuthority + "\n" +
                state + ", " + place + ", " + community + ", " + street + " " + houseNumber + houseLetter + " " + entrance + " " + floor + " " + appartmentNumber + "\n" +
                dateOfBirth + ", " + placeOfBirth + ", " + communityOfBirth + " " + stateOfBirth + "\n";
    }
}
