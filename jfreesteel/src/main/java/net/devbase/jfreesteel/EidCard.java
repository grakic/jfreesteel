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
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.smartcardio.Card;
import javax.smartcardio.CardChannel;
import javax.smartcardio.CardException;
import javax.smartcardio.CommandAPDU;
import javax.smartcardio.ResponseAPDU;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;

import net.devbase.jfreesteel.EidInfo.Tag;

/**
 * Smart card wrapper.
 * 
 * EidCard is a wrapper providing an interface for reading data
 * from the Serbian eID card. Public read*() methods allow you to
 * get specific data about card holder and certificates stored on
 * the card.
 *
 * It is not advised to initialize this class directly. Instead you
 * should initialize Reader class and assign the listener for the
 * card insertion/removal events. The listener will receive EidCard
 * object when the card is inserted into the terminal.
 *
 * @author Goran Rakic (grakic@devbase.net)
 */
@SuppressWarnings("restriction") // Various javax.smartcardio.*
public class EidCard {

    private final static Logger logger = LoggerFactory.getLogger(EidCard.class);

    private Card card;
    private CardChannel channel;
    private static int cardVersion = 0; // 1 - Apollo, 2 - Gemalto 
    
    public EidCard(final Card card)
        throws IllegalArgumentException, SecurityException, IllegalStateException {
        // Check if the card ATR is recognized
        final byte[] atrBytes = card.getATR().getBytes();
        if(!isKnownATR(atrBytes)) {
            throw new IllegalArgumentException(
                String.format("EidCard: Card is not recognized as Serbian eID. Card ATR: %s",
                    Utils.bytes2HexString(atrBytes)));
        }

        this.card = card;
        channel = card.getBasicChannel();
    }

    private boolean isKnownATR(byte[] card_atr) {
        for(int i=0; i< KNOWN_EID_ATRS.length; i++) {
            if(Arrays.equals(card_atr, KNOWN_EID_ATRS[i])) {
            	cardVersion = i+1;
                return true;
            }
        }
        return false;
    }

    /** The list of known eID card ATRs, used to identify smartcards. */
    @VisibleForTesting static final byte[][] KNOWN_EID_ATRS = {
        // Add more as more become available.
        {(byte) 0x3B, (byte) 0xB9, (byte) 0x18, (byte) 0x00, (byte) 0x81, (byte) 0x31, (byte) 0xFE,
         (byte) 0x9E, (byte) 0x80, (byte) 0x73, (byte) 0xFF, (byte) 0x61, (byte) 0x40, (byte) 0x83,
         (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xDF},
        {(byte) 0x3B, (byte) 0xFF, (byte) 0x94, (byte) 0x00, (byte) 0x00, (byte) 0x81, (byte) 0x31,
         (byte) 0x80, (byte) 0x43, (byte) 0x80, (byte) 0x31, (byte) 0x80, (byte) 0x65, (byte) 0xB0,
         (byte) 0x85, (byte) 0x02, (byte) 0x01, (byte) 0xF3, (byte) 0x12, (byte) 0x0F, (byte) 0xFF,
		 (byte) 0x82, (byte) 0x90, (byte) 0x00, (byte) 0x79 },		 
    };

    /** Document data */
    private static final byte[] DOCUMENT_FILE  = {0x0F, 0x02};

    /** Personal data */
    private static final byte[] PERSONAL_FILE  = {0x0F, 0x03};

    /** Place of residence */
    private static final byte[] RESIDENCE_FILE = {0x0F, 0x04};

    /** Personal photo in JPEG format */
    private static final byte[] PHOTO_FILE     = {0x0F, 0x06};

    /** Intermediate CA gradjani public X.509 certificate */
    @SuppressWarnings("unused")
    private static final byte[] INTERM_CERT_FILE  = {0x0F, 0x11};

    /** Public X.509 certificate for qualified (Non Repudiation) signing */
    @SuppressWarnings("unused")
    private static final byte[] SIGNING_CERT_FILE = {0x0F, 0x10};

    /** Public X.509 certificate for authentication */
    @SuppressWarnings("unused")
    private static final byte[] AUTH_CERT_FILE    = {0x0F, 0x08};

    private static int BLOCK_SIZE = 0xFF;

    
    /**
     * Subdivides the byte array into byte sub-arrays, keyed by their tags
     * 
     * Encoding sequence is a repeated sequence of the following.
     * <ol>
     *   <li>The tag, encoded as little-endian unsigned 16-bit number (just liek char in Java)
     *   <li>The length of data, in bytes, as unsigned little-endian 16-bit number
     *   <li>The data bytes, as many as determined by length.
     * </ol> 
     * [tag 16bit LE] [len 16bit LE] [len bytes of data] | [fld] [06] ...
     * 
     * @return a map of integer tags to corresponding byte chunks.
     */
    @VisibleForTesting
    static Map<Integer, byte[]> parseTlv(byte[] bytes) {
        HashMap<Integer, byte[]> out = new HashMap<Integer, byte[]>();

        // [fld 16bit LE] [len 16bit LE] [len bytes of data] | [fld] [06] ...

        ByteBuffer buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
    
        if ( cardVersion==2) {
        	byte[] header = new byte[4];
        	buffer.get(header);
        }
        // repeat as long as we have next tag and len...
        while (buffer.remaining() > 4) {
            int tag = buffer.getChar();
            int length = buffer.getChar();
            if (length > buffer.remaining()) {
            	break;
            }            
            byte[] range = new byte[length];
            
            buffer.get(range);
            out.put((int) tag, range);
        }

        return out;
    }

    /**
     * Read EF contents, selecting by file path.
     * 
     * The file length is read at 4B offset from the file. The method is not thread safe. Exclusive
     * card access should be enabled before calling the method.
     * 
     * TODO: Refactor to be in line with ISO7816-4 and BER-TLV, removing "magic" headers
     */
    private byte[] readElementaryFile(byte[] name, boolean stripHeader) throws CardException {

        selectFile(name);

        // Read first 6 bytes from the EF
        byte[] header;
        if ( cardVersion==2) {
        	header = readBinary(0, 4);
        	BLOCK_SIZE = 0x80;
        } else {
        	header = readBinary(0, 6);
        }

        // Missing files have header filled with 0xFF
        int i = 0;
        while (i < header.length && header[i] == 0xFF) {
            i++;
        }
        if (i == header.length) {
            throw new CardException("Read EF file failed: File header is missing");
        }

        // Total EF length: data as 16bit LE at 4B offset
        final int length;
        final int offset;

        if ( cardVersion==2 ) { 
            length = ((0xFF&header[3])<<8) + (0xFF&header[2])+4;
            offset = 0;        	
        } else {
            length = ((0xFF&header[5])<<8) + (0xFF&header[4]);
            offset = stripHeader ? 10 : 6;        	
        }
        // Read binary into buffer
        return readBinary(offset, length);
    }

    /** Reads the content of the selected file starting at offset, at most length bytes */
    private byte[] readBinary(int offset, int length) throws CardException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        while (length > 0) {
            int readSize = Math.min(length, BLOCK_SIZE);
            ResponseAPDU response = channel.transmit(
                new CommandAPDU(0x00, 0xB0, offset >> 8, offset & 0xFF, readSize));
            if (response.getSW() != 0x9000) {
                throw new CardException(
                    String.format("Read binary failed: offset=%d, length=%d, status=%s", 
                        offset, length, Utils.int2HexString(response.getSW())));
            }

            try {
                byte[] data = response.getData();
                out.write(data);
                offset += data.length;
                length -= data.length;
            } catch (IOException e) {
                throw new CardException("Read binary failed: Could not write byte stream");
            }
        }

        return out.toByteArray();
    }

    /** Selects the elementary file to read, based on the name passed in. */
    private void selectFile(byte[] name) throws CardException {
    	ResponseAPDU response;
    	if ( cardVersion==2 ) {
            // ide mantra
        	byte[] mb = new byte[] {0x00, (byte) 0xA4, 0x04, 0x00, 0x0B, (byte) 0xF3, (byte) 0x81,0x00,0x00,0x02,0x53,0x45,0x52,0x49,0x44,0x01, 0x00};    	
        	ResponseAPDU m1 = channel.transmit(new CommandAPDU(mb));
        	//System.out.println("MANTRA 1 -> "+Utils.byte2hex(m1.getBytes()));
        	
        	mb = new byte[] {0x00, (byte) 0xA4, 0x04, 0x00, 0x08, (byte) 0xA0, 0x00, 0x00, 0x00, 0x03, 0x00, 0x00, 0x00, 0x00};
        	m1 = channel.transmit(new CommandAPDU(mb));
        	//System.out.println("MANTRA 2 -> "+Utils.byte2hex(m1.getBytes()));

        	mb = new byte[] {0x00, (byte) 0xA4, 0x04, 0x00, 0x08, (byte) 0xA0, 0x00, 0x00, 0x00, 0x18, 0x43, 0x4D, 0x00, 0x00};
        	m1 = channel.transmit(new CommandAPDU(mb));
        	//System.out.println("MANTRA 3 -> "+Utils.byte2hex(m1.getBytes()));
        	

        	mb = new byte[] {(byte)0x80, (byte)0xCA, (byte)0x9F, 0x7F, 0x00};
        	m1 = channel.transmit(new CommandAPDU(mb));
        	//System.out.println("MANTRA 4 -> "+Utils.byte2hex(m1.getBytes()));

        	mb = new byte[] {0x00, (byte) 0xA4, 0x04, 0x00, 0x0B, (byte) 0xF3, (byte) 0x81, 0x00, 0x00, 0x02, 0x53, 0x45, 0x52, 0x49, 0x44, 0x01, 0x00};
        	m1 = channel.transmit(new CommandAPDU(mb));
        	//System.out.println("MANTRA 5 -> "+Utils.byte2hex(m1.getBytes()));

            // ode pravi poziv
        	byte[] ba = new byte[20];
        	int i = 0;
        	ba[i++] = 0x00;
        	ba[i++] = (byte) 0xA4;
        	ba[i++] = 0x00;
        	ba[i++] = 0x00;
        	ba[i++] = (byte) name.length;
        	System.arraycopy(name, 0, ba, i, name.length);
        	i += name.length;
        	ba[i++] = 0x00;
        	
            //ResponseAPDU response = channel.transmit(new CommandAPDU(0x00, 0xA4, 0x00, 0x00, name, 0x00));
        	response = channel.transmit(new CommandAPDU(ba,0,i));
        	//System.out.println("SELECT FILE RESPONSE -> "+Utils.byte2hex(response.getBytes()));
    	} else {
	        response = channel.transmit(new CommandAPDU(0x00, 0xA4, 0x08, 0x00, name));	        
    	}
        if(response.getSW() != 0x9000) {
            throw new CardException(
                String.format("Select failed: name=%s, status=%s", 
                    Utils.bytes2HexString(name), Utils.int2HexString(response.getSW())));
        }
    }

    /** Reads the photo data from the card. */
    public Image readEidPhoto() throws CardException {
        try {
            logger.info("photo exclusive");
            card.beginExclusive();

            // Read binary into buffer
            byte[] bytes = readElementaryFile(PHOTO_FILE, true);
            int offset = 0;
            if (cardVersion==2) {
            	offset=8;
            }
            try {
                return ImageIO.read(new ByteArrayInputStream(bytes,offset, bytes.length-offset));
            } catch (IOException e) {
                throw new CardException("Photo reading error", e);
            }
        } finally {
            card.endExclusive();
            logger.info("photo exclusive free");
        }
    }

    void writeFile(byte[] aInput, int offset, String aOutputFileName){
        try {
          OutputStream output = null;
          try {
            output = new BufferedOutputStream(new FileOutputStream(aOutputFileName));
            output.write(aInput,offset, aInput.length-offset);
          }
          finally {
            output.close();
          }
        }
        catch(FileNotFoundException ex){
          ex.printStackTrace();
        }
        catch(IOException ex){
          ex.printStackTrace();
        }
      }
    
    public void copyEidPhotoToFile(String name) throws CardException {
        try {
            logger.info("photo exclusive");
            card.beginExclusive();

            // Read binary into buffer
            byte[] bytes = readElementaryFile(PHOTO_FILE, true);
            int offset = 0;
            if (cardVersion==2) {
            	offset=8;
            }
            writeFile(bytes, offset, name);
        } finally {
            card.endExclusive();
            logger.info("photo exclusive free");
        }
    }

    // tags: 1545 - 1553
    private static final ImmutableMap<Integer, Tag> DOCUMENT_TAGMAPPER =
        new ImmutableMap.Builder<Integer, Tag>()
            .put(1545, Tag.NULL) // = SRB (issuing authority country code?)
            .put(1546, Tag.DOC_REG_NO)
            .put(1547, Tag.NULL) // = ID
            .put(1548, Tag.NULL) // = ID<docRegNo>
            .put(1549, Tag.ISSUING_DATE)
            .put(1550, Tag.EXPIRY_DATE)
            .put(1551, Tag.ISSUING_AUTHORITY)
            .put(1552, Tag.NULL) // = SC
            .put(1553, Tag.NULL) // = SC
            .build();

    // tags: 1558 - 1567
    private static final ImmutableMap<Integer, Tag> PERSONAL_TAGMAPPER =
        new ImmutableMap.Builder<Integer, Tag>()
            .put(1558, Tag.PERSONAL_NUMBER)
            .put(1559, Tag.SURNAME)
            .put(1560, Tag.GIVEN_NAME)
            .put(1561, Tag.PARENT_GIVEN_NAME)
            .put(1562, Tag.SEX)
            .put(1563, Tag.PLACE_OF_BIRTH)
            .put(1564, Tag.COMMUNITY_OF_BIRTH)
            .put(1565, Tag.STATE_OF_BIRTH)
            .put(1566, Tag.DATE_OF_BIRTH)
            .put(1567, Tag.NULL) // = SRB (state of birth country code?)
            .build();

    // tags: 1568 .. 1578
    private static final ImmutableMap<Integer, Tag> RESIDENCE_TAGMAPPER =
        new ImmutableMap.Builder<Integer, Tag>()
            .put(1568, Tag.STATE)
            .put(1569, Tag.COMMUNITY)
            .put(1570, Tag.PLACE)
            .put(1571, Tag.STREET)
            .put(1572, Tag.HOUSE_NUMBER)
            .put(1573, Tag.HOUSE_LETTER)
            .put(1574, Tag.ENTRANCE)
            .put(1575, Tag.FLOOR)
            .put(1578, Tag.APPARTMENT_NUMBER)
            .build();

    /**
     * Add all raw tags to EidInfo builder.
     *
     * @param builder EidInfo builder
     * @param rawTagMap Parsed map of raw byte strings by TLV code
     * @param tagMapper Map translating TLV codes into EidInfo tags; use {@code Tag.NULL} 
     *     if tag should be silently ignored
     * @return Raw map of unknown tags
     */
    private Map<Integer, byte[]> addAllToBuilder(
            EidInfo.Builder builder,
            final Map<Integer, byte[]> rawTagMap,
            final Map<Integer, Tag> tagMapper) {

        Map<Integer, byte[]> unknownTags = new HashMap<Integer, byte[]>();

        for (Map.Entry<Integer, byte[]> entry : rawTagMap.entrySet()) {
            if (tagMapper.containsKey(entry.getKey())) {
                // tag is known, ignore if null or decode and add value to the builder
                Tag tag = tagMapper.get(entry.getKey());
                if (tag == Tag.NULL) {
                    continue;
                }

                String value = Utils.bytes2UTF8String(entry.getValue());
                builder.addValue(tag, value);
            } else {
                // tag is unknown, copy for return
                unknownTags.put(entry.getKey(), entry.getValue());
            }
        }

        return unknownTags;
    }

    public EidInfo readEidInfo() throws CardException {
        try {
            logger.info("exclusive");
            card.beginExclusive();
            channel = card.getBasicChannel();

            Map<Integer, byte[]> document = parseTlv(readElementaryFile(DOCUMENT_FILE, false));
            Map<Integer, byte[]> personal = parseTlv(readElementaryFile(PERSONAL_FILE, false));
            Map<Integer, byte[]> residence = parseTlv(readElementaryFile(RESIDENCE_FILE, false));

            EidInfo.Builder builder = new EidInfo.Builder();
            document = addAllToBuilder(builder, document, DOCUMENT_TAGMAPPER);
            personal = addAllToBuilder(builder, personal, PERSONAL_TAGMAPPER);
            residence = addAllToBuilder(builder, residence, RESIDENCE_TAGMAPPER);

            // log all unknown tags so all users can report bugs easily
            StringBuilder unknownString = new StringBuilder();
            if (!document.isEmpty()) {
                unknownString.append("DOCUMENT:\n" + Utils.map2UTF8String(document));
            }
            if (!personal.isEmpty()) {
                unknownString.append("PERSONAL:\n" + Utils.map2UTF8String(personal));
            }
            if (!residence.isEmpty()) {
                unknownString.append("RESIDENCE:\n" + Utils.map2UTF8String(residence));
            }
            if (unknownString.length() > 0) {
                logger.error(
                    "Some unknown tags found on a card. Please send this info to " +
                    "<grakic@devbase.net> and contribute to the development.\n" +
                    unknownString.toString());
            }

            return builder.build();

        } finally {
            card.endExclusive();
            logger.info("exclusive free");
        }
    }

    /** Returns a debug string consisting of per-file debug info. */
    public String debugEidInfo() throws CardException {
        EidInfo info = readEidInfo();
        return info.toString();
    }

    /** Disconnects, but doesn't reset the card. */
    public void disconnect() throws CardException {
        disconnect(false);
    }

    public void disconnect(boolean reset) throws CardException {
        card.disconnect(reset);
        card = null;
    }

    @Override
    protected void finalize() {
        try {
            if (card != null) {
                disconnect(false);
            }
        } catch (CardException error) {
            // Can't throw an exception from within finalize, else object finalization
            // will be halted by JVM, which is bad.
            // can't log to instance logger because logger may already have been destroyed.  So just
            // write log output and hope for the best.
            LoggerFactory.getLogger(EidCard.class).warn(error.getMessage());
        }
    }
    
    public int getVersion() {
    	return cardVersion;
    }
}
