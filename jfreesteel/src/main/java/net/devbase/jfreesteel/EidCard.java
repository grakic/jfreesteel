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
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.smartcardio.Card;
import javax.smartcardio.CardChannel;
import javax.smartcardio.CardException;
import javax.smartcardio.CommandAPDU;
import javax.smartcardio.ResponseAPDU;

import net.devbase.jfreesteel.EidInfo.Tag;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Smart card wrapper, abstract interface.
 * 
 * EidCard is a wrapper around javax.smartcardio.Card providing an interface
 * for reading data from the Serbian eID card. Public read*() methods allow
 * you to get specific data about card holder and certificates stored on the
 * card.
 *
 * There are two implementations, use fromCard() factory method to get an
 * instance supporting the given card.
 * 
 * It is recommended that you do not work with EidCard directly, but use
 * a Reader class and assign the listener for the card insertion/removal
 * events. The listener will receive EidCard object when the card is inserted
 * into the terminal.
 *
 * @author Goran Rakic (grakic@devbase.net)
 */
@SuppressWarnings("restriction") // Various javax.smartcardio.*
public abstract class EidCard {

    protected final static Logger logger = LoggerFactory.getLogger(EidCard.class);

    protected Card card;
    protected CardChannel channel;

    // Constructor
    protected EidCard(final Card card) {
        this.card = card;
        channel = card.getBasicChannel();        
    }

    /**
     * Factory method
     * 
     * Return instance of EidCard implementation supporting the given card
     *
     * @param card
     * @throws SecurityException
     * @throws IllegalStateException
     * @throws IllegalArgumentException Card is not regognized as Serbian eID
     * @return Return a new instance
     * @throws CardException 
     */
    public static EidCard fromCard(final Card card)
            throws IllegalArgumentException, SecurityException, IllegalStateException, CardException {
        final byte[] atrBytes = card.getATR().getBytes();

        if(EidCardApollo.isKnownAtr(atrBytes))
            return new EidCardApollo(card);

        if(EidCardGemalto.isKnownAtr(atrBytes))
            return new EidCardGemalto(card);

        throw new IllegalArgumentException(
                String.format("EidCard: Card is not recognized as Serbian eID. Card ATR: %s",
                        Utils.bytes2HexString(atrBytes)));
    }

    /** Factory "selection" method */
    protected static boolean isKnownAtr(final byte[] atrBytes) {
        return false;
    }

    /** Document data */
    protected static final byte[] DOCUMENT_FILE  = {0x0F, 0x02};

    /** Personal data */
    protected static final byte[] PERSONAL_FILE  = {0x0F, 0x03};

    /** Place of residence */
    protected static final byte[] RESIDENCE_FILE = {0x0F, 0x04};

    /** Personal photo in JPEG format */
    protected static final byte[] PHOTO_FILE     = {0x0F, 0x06};


    /**
     * Subdivides the byte array into byte sub-arrays, keyed by their tags
     * 
     * Encoding sequence is a repeated sequence of the following.
     * <ol>
     *   <li>The tag, encoded as little-endian unsigned 16-bit number (just like char in Java)
     *   <li>The length of data, in bytes, as unsigned little-endian 16-bit number
     *   <li>The data bytes, as many as determined by length.
     * </ol> 
     * [tag 16bit LE] [len 16bit LE] [len bytes of data] | [fld] [06] ...
     * 
     * @return a map of integer tags to corresponding byte chunks.
     */
    public static Map<Integer, byte[]> parseTlv(final byte[] bytes) {
        HashMap<Integer, byte[]> out = new HashMap<Integer, byte[]>();

        // [fld 16bit LE] [len 16bit LE] [len bytes of data] | [fld] [06] ...

        ByteBuffer buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);

        // repeat as long as we have next tag and len...
        while (buffer.remaining() > 4) {
            char tag = buffer.getChar();
            char length = buffer.getChar();
            byte[] range = new byte[(int) length];
            buffer.get(range);
            out.put((int) tag, range);
        }

        return out;
    }

    protected static final int BLOCK_SIZE = 0xFF;

    /**
     * Read EF contents, selecting by file path.
     * 
     * The file length is read at 4B offset from the file. The method is not thread safe. Exclusive
     * card access should be enabled before calling the method.
     */
    abstract protected byte[] readElementaryFile(final byte[] name, boolean strip_tag) throws CardException;

    /** Reads the content of the selected file starting at offset, at most length bytes */
    protected byte[] readBinary(int offset, int length) throws CardException {
        int readSize = Math.min(length, BLOCK_SIZE);
        ResponseAPDU response = channel.transmit(
                new CommandAPDU(0x00, 0xB0, offset >> 8, offset & 0xFF, readSize));
        if (response.getSW() != 0x9000) {
            throw new CardException(
                    String.format("Read binary failed: offset=%d, length=%d, status=%s", 
                            offset, length, Utils.int2HexString(response.getSW())));
        }
        return response.getData();
    }

    /** Selects the elementary file to read, based on the name passed in. */
    protected byte[] selectFile(final byte[] name) throws CardException {
        return selectFile(name, 0);
    }

    protected byte[] selectFile(final byte[] name, int ne) throws CardException {
        ResponseAPDU response = channel.transmit(new CommandAPDU(0x00, 0xA4, 0x08, 0x00, name, ne));
        if(response.getSW() != 0x9000) {
            throw new CardException(
                    String.format("Select failed: name=%s, status=%s", 
                            Utils.bytes2HexString(name), Utils.int2HexString(response.getSW())));
        }
        return response.getData();
    }

    /** Reads the photo data from the card. */
    public Image readEidPhoto() throws CardException {
        try {
            logger.info("photo exclusive");
            card.beginExclusive();

            // Read binary into buffer
            byte[] bytes = readElementaryFile(PHOTO_FILE, true);    

            try {
                return ImageIO.read(new ByteArrayInputStream(bytes));
            } catch (IOException e) {
                throw new CardException("Photo reading error", e);
            }
        } finally {
            card.endExclusive();
            logger.info("photo exclusive free");
        }
    }

    // tags: 1545 - 1553
    @SuppressWarnings("serial")
	static final Map<Integer, Tag> DOCUMENT_TAGMAPPER = new HashMap<Integer, Tag>() {{
        put(1545, Tag.NULL); // = SRB (issuing authority country code?)
        put(1546, Tag.DOC_REG_NO);
        put(1547, Tag.NULL); // = ID
        put(1548, Tag.NULL); // = ID<docRegNo>
        put(1549, Tag.ISSUING_DATE);
        put(1550, Tag.EXPIRY_DATE);
        put(1551, Tag.ISSUING_AUTHORITY);
        put(1552, Tag.NULL); // = SC
        put(1553, Tag.NULL); // = SC    	
    }};

    // tags: 1558 - 1567
    @SuppressWarnings("serial")
	static final Map<Integer, Tag> PERSONAL_TAGMAPPER = new HashMap<Integer, Tag>() {{
        put(1558, Tag.PERSONAL_NUMBER);
        put(1559, Tag.SURNAME);
        put(1560, Tag.GIVEN_NAME);
        put(1561, Tag.PARENT_GIVEN_NAME);
        put(1562, Tag.SEX);
        put(1563, Tag.PLACE_OF_BIRTH);
        put(1564, Tag.COMMUNITY_OF_BIRTH);
        put(1565, Tag.STATE_OF_BIRTH);
        put(1566, Tag.DATE_OF_BIRTH);
        put(1567, Tag.NULL); // = SRB (state of birth country code?)
    }};

    // tags: 1568 .. 1578
    @SuppressWarnings("serial")
	static final Map<Integer, Tag> RESIDENCE_TAGMAPPER = new HashMap<Integer, Tag>() {{
        put(1568, Tag.STATE);
        put(1569, Tag.COMMUNITY);
        put(1570, Tag.PLACE);
        put(1571, Tag.STREET);
        put(1572, Tag.HOUSE_NUMBER);
        put(1573, Tag.HOUSE_LETTER);
        put(1574, Tag.ENTRANCE);
        put(1575, Tag.FLOOR);
        put(1578, Tag.APPARTMENT_NUMBER);
        put(1580, Tag.ADDRESS_DATE); // = default 01010001
        // AddressLabel ?
    }};

    /**
     * Add all raw tags to EidInfo builder.
     *
     * @param builder EidInfo builder
     * @param rawTagMap Parsed map of raw byte strings by TLV code
     * @param tagMapper Map translating Tag codes into EidInfo tags; use {@code Tag.NULL} 
     *     if tag should be silently ignored
     * @return Raw map of unknown tags
     */
    protected Map<Integer, byte[]> addAllToBuilder(
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

            } else if (entry.getValue().length > 0) {
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
}
