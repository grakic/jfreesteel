package net.devbase.jfreesteel;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;

import javax.smartcardio.Card;
import javax.smartcardio.CardException;
import javax.smartcardio.CommandAPDU;
import javax.smartcardio.ResponseAPDU;

/**
 * Smart card wrapper for Gemalto MultiApp ID smart card
 * 
 * EidCardSmartCafeExpert7 implements EidCard abstract interface for reading
 * data from the Serbian eID cards based on G&D CardSmartCafeExpert7 JavaCard that
 * are issued after Jul 2021.
 *
 * You should not initialize this class directly. Use EidCard.fromCard()
 * factory method for "direct" access, or initialize a Reader class and
 * assign the listener for the card insertion/removal events. The listener
 * will receive EidCard object when the card is inserted into the terminal.
 *
 * @author krstom (krstom@gmail.com)
 * @author Goran Rakic (grakic@devbase.net)
 */
@SuppressWarnings("restriction") // Various javax.smartcardio.*
public class EidCardSmartCafeExpert7 extends EidCard {

    /** The list of known card ATRs, used to identify this smartcard. */

	public static final byte[] CARD_ATR = {
        (byte) 0x3B, (byte) 0xF9, (byte) 0x96, (byte) 0x00, (byte) 0x00, (byte) 0x80, (byte) 0x31,
        (byte) 0xFE, (byte) 0x45, (byte) 0x53, (byte) 0x43, (byte) 0x45, (byte) 0x37, (byte) 0x20,
        (byte) 0x47, (byte) 0x43, (byte) 0x4E, (byte) 0x33, (byte) 0x5E
    };

	public static final byte[] CARD_ATR2 = {
		(byte) 0x3B, (byte) 0x9E, (byte) 0x96, (byte) 0x80, (byte) 0x31, (byte) 0xFE, (byte) 0x45,
		(byte) 0x53, (byte) 0x43, (byte) 0x45, (byte) 0x20, (byte) 0x38, (byte) 0x2E, (byte) 0x30,
		(byte) 0x2D, (byte) 0x43, (byte) 0x31, (byte) 0x56, (byte) 0x30, (byte) 0x0D, (byte) 0x0A,
		(byte) 0x6F
	};

    /** Factory "selection" method */
    protected static boolean isKnownAtr(byte[] atrBytes) {
        return Arrays.equals(atrBytes, CARD_ATR) || Arrays.equals(atrBytes, CARD_ATR2);
    }

    static final byte[] LICNA_KARTA_AID = {
        (byte) 0xF3, (byte) 0x81, (byte) 0x00, (byte) 0x00, (byte) 0x02, (byte) 0x53, (byte) 0x45, 
        (byte) 0x52, (byte) 0x49, (byte) 0x44, (byte) 0x01
    };

    protected EidCardSmartCafeExpert7(Card card) throws CardException {
        super(card);

        // Select aid
        ResponseAPDU response = channel.transmit(
                new CommandAPDU(0x00, 0xA4, 0x04, 0x00, LICNA_KARTA_AID));
        if (response.getSW() != 0x9000) {
            throw new CardException(
                    String.format("Select AID failed: status=%s", 
                            Utils.int2HexString(response.getSW())));
        }        
    }

    protected byte[] readElementaryFile(final byte[] name, boolean strip_tag) throws CardException {

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        byte[] fileinfo = selectFile(name, 4);

        // length hint from file info
        int length = ((0xFF&fileinfo[2])<<8) + (0xFF&fileinfo[3]);
        int offset = 0;
        boolean known_real_length = false;

        while (length > 0) {
            byte[] data = readBinary(offset, length);

            if (!known_real_length) {
                // get length from outher tag, skip first 4 bytes (outher tag + length)
                // if strip_tag is true, skip 4 more bytes (inner tag + length) and return content 
                length = ((0xFF&data[3])<<8) + (0xFF&data[2]);
                int skip = strip_tag ? 8 : 4;
                out.write(data, skip, data.length-skip);
                known_real_length = true;
            }
            else out.write(data, 0, data.length);

            offset += data.length;
            length -= data.length;
        }

        return out.toByteArray();
    }   
}
