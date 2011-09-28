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
import java.io.ByteArrayOutputStream;
import java.io.IOException;
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

import com.google.common.collect.ImmutableMap;

import net.devbase.jfreesteel.EidInfo.Tag;

/**
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

    private Card card = null;
    private CardChannel channel;

    public EidCard(final Card card)
        throws IllegalArgumentException, SecurityException, IllegalStateException {
        // Check if the card ATR is recognized
        if(!knownATR(card.getATR().getBytes())) {
            throw new IllegalArgumentException(
                "EidCard: Card is not recognized as Serbian eID. Card ATR: " +
                Utils.bytes2HexString(card.getATR().getBytes()));
        }

        this.card = card;
        channel = card.getBasicChannel();
    }

    private boolean knownATR(byte[] card_atr) {
        for(byte[] eid_atr:known_eid_atrs) {
            if(Arrays.equals(card_atr, eid_atr)) {
                return true;
            }
        }
        return false;
    }

    private static final byte[][] known_eid_atrs = {
        {(byte) 0x3B, (byte) 0xB9, (byte) 0x18, (byte) 0x00, (byte) 0x81, (byte) 0x31, (byte) 0xFE,
         (byte) 0x9E, (byte) 0x80, (byte) 0x73, (byte) 0xFF, (byte) 0x61, (byte) 0x40, (byte) 0x83,
         (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xDF},
    };

    private static final byte[] DOCUMENT_FILE  = {0x0F, 0x02}; // Document data
    private static final byte[] PERSONAL_FILE  = {0x0F, 0x03}; // Personal data
    private static final byte[] RESIDENCE_FILE = {0x0F, 0x04}; // Place of residence, var length
    private static final byte[] PHOTO_FILE     = {0x0F, 0x06}; // Personal photo in JPEG format

    // Intermediate CA gradjani public X.509 certificate
    @SuppressWarnings("unused")
    private static final byte[] INTERM_CERT_FILE  = {0x0F, 0x11};

    // Public X.509 certificate for qualified (Non Repudiation) signing
    @SuppressWarnings("unused")
    private static final byte[] SIGNING_CERT_FILE = {0x0F, 0x10};

    // Public X.509 certificate for authentication
    @SuppressWarnings("unused")
    private static final byte[] AUTH_CERT_FILE    = {0x0F, 0x08};

    private static final int BLOCK_SIZE = 0xFF;

    private Map<Integer, byte[]> parseTLV(byte[] bytes) {
        HashMap<Integer, byte[]> out = new HashMap<Integer, byte[]>();

        // [fld 16bit LE] [len 16bit LE] [len bytes of data] | [fld] [06] ...

        int i = 0;
        while(i+3 < bytes.length) {
            int len = ((0xFF&bytes[i+3])<<8) + (0xFF&bytes[i+2]);
            int tag = ((0xFF&bytes[i+1])<<8) + (0xFF&bytes[i+0]);
            
            // is there a new tag?
            if(len >= bytes.length) break;

            out.put(tag, Arrays.copyOfRange(bytes, i+4, i+4+len));

            i += 4+len;
        }

        return out;
    }

    /**
     * Read EF content, selecting by file path.
     *
     * The file length is read at 4B offset from the file. The method is not thread safe. Exclusive
     * card access should be enabled before calling the method.
     *
     * TODO: Refactor to be in line with ISO7816-4 and BER-TLV, removing "magic" headers
     */
    private byte[] readElementaryFile(byte[] name, boolean strip_heading_tlv) throws CardException {

        selectFile(name);

        // Read first 6 bytes from the EF
        byte[] header = readBinary(0, 6);

        // Missing files have header filled with 0xFF
        int i = 0;
        while (i < header.length && header[i] == 0xFF) {
            i++;
        }
        if (i == header.length) {
            throw new CardException("Read EF file failed: File header is missing");
        }

        // Total EF length: data as 16bit LE at 4B offset
        final int length = ((0xFF&header[5])<<8) + (0xFF&header[4]);
        final int offset = strip_heading_tlv ? 10 : 6;

        // Read binary into buffer
        return readBinary(offset, length);
    }

    private byte[] readBinary(int offset, int length) throws CardException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        while (length > 0) {
            int block = Math.min(length, BLOCK_SIZE);
            ResponseAPDU r = channel.transmit(new CommandAPDU(0x00, 0xB0, offset >> 8, offset & 0xFF, block));
            if (r.getSW() != 0x9000) {
                throw new CardException("Read binary failed: " + Utils.int2HexString(r.getSW()));
            }

            try {
                byte[] data = r.getData();
                int data_len = data.length;

                out.write(data);
                offset += data_len;
                length -= data_len;
            } catch (IOException e) {
                throw new CardException("Read binary failed: Could not write byte stream");
            }
        }

        return out.toByteArray();
    }

    private void selectFile(byte[] name) throws CardException {
        ResponseAPDU r = channel.transmit(new CommandAPDU(0x00, 0xA4, 0x08, 0x00, name));
        if(r.getSW() != 0x9000) {
            throw new CardException("Select failed: " + Utils.int2HexString(r.getSW()));
        }
    }

    public Image readEidPhoto() throws CardException {
        try {
            logger.info("photo exclusive");
            card.beginExclusive();

            // Read binary into buffer
            byte[] bytes = readElementaryFile(PHOTO_FILE, true);

            try {
                return ImageIO.read(new ByteArrayInputStream(bytes));
            } catch (IOException e) {
                throw new CardException("Photo reading error: " + e.getMessage(), e);
            }
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
            // TODO: What about tags 1573 .. 1577?
            .put(1573, Tag.HOUSE_LETTER) // ??
            .put(1576, Tag.ENTRANCE) // ??
            .put(1577, Tag.FLOOR) // ??
            .put(1578, Tag.APPARTMENT_NUMBER)
            .build();

    /**
     * Add all raw tags to EidInfo builder.
     *
     * @param builder EidInfo builder
     * @param rawTagMap Parsed map of raw byte strings by TLV code
     * @param tagMapper Map translating TLV codes into EidInfo tags (Use Tag.NULL if tag should be silently ignored)
     * @return Raw map of unknown tags
     *
     * FIXME: http://code.google.com/p/google-collections/issues/detail?id=234 requires us to have
     * Tag.NULL. Alternatively, tagMapper may be modified to allow null tag values.
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
                if (tag == Tag.NULL) continue;

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

            Map<Integer, byte[]> document = parseTLV(readElementaryFile(DOCUMENT_FILE, false));
            Map<Integer, byte[]> personal = parseTLV(readElementaryFile(PERSONAL_FILE, false));
            Map<Integer, byte[]> residence = parseTLV(readElementaryFile(RESIDENCE_FILE, false));

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

    public String debugEidInfo() throws CardException {
        EidInfo info = readEidInfo();
        return info.toString();
    }

    public void disconnect(boolean reset) throws CardException {
        card.disconnect(reset);
        card = null;
    }

    @Override
    protected void finalize() throws Throwable {
        if (card != null) {
            disconnect(false);
        }
    }
}
