package net.devbase.jfreesteel;

import java.util.Arrays;
import java.util.Map;

import javax.smartcardio.ATR;
import javax.smartcardio.Card;
import javax.smartcardio.CardChannel;
import javax.smartcardio.CardException;

import junit.framework.TestCase;

import org.easymock.classextension.EasyMock;
import org.easymock.classextension.IMocksControl;

import com.google.common.collect.Iterables;

@SuppressWarnings("restriction") // Various javax.smartcardio.*
public class EidCardTest extends TestCase {
    
    private IMocksControl control;
    
    private Card mockCard;
    private CardChannel mockChannel;
    
    @Override
    public void setUp() {
        control = EasyMock.createControl();
        
        mockCard = control.createMock(Card.class);
        mockChannel = control.createMock(CardChannel.class);
    }
    
    public void testParseTlv_basic() {
        Map<Integer, byte[]> result = EidCard.parseTlv(
            Utils.asByteArray(0xfe, 0xca, 0x01, 0x00, 0xfe));
        
        assertTrue(Arrays.equals(
            Utils.asByteArray(0xfe),
            result.get(0xcafe)));
    }
    
    public void testParseTlv_complex() {
        Map<Integer, byte[]> result = EidCard.parseTlv(
            Utils.asByteArray(
                0xfe, 0xca,  // 0xcafe 
                0x01, 0x00,  // 0x1
                0xfe, // 1-byte data
                0xbe, 0xba,  // 0xaabb
                0x05, 0x00,  // 0x5
                0x01, 0x02, 0x03, 0x04, 0x05, // 5-byte data
                0xff, 0xff   // Some extra crud, ignored
                ));  
        
        assertTrue(Arrays.equals(
            Utils.asByteArray(0xfe), 
            result.get(0xcafe)));
        assertTrue(Arrays.equals(
            Utils.asByteArray(0x01, 0x02, 0x03, 0x04, 0x05), 
            result.get(0xbabe)));
    }
    
    public void testCardInitialization_knownEid() {
        expectSerbianAtr();

        control.replay();
        new EidCard(mockCard);
        control.verify();
    }

    public void testCardInitialization_unknownEid() {
        expectAtr(Utils.asByteArray(0xca, 0xfe));
        control.replay();
        
        try {
            new EidCard(mockCard);
            fail("exception expected");
        } catch (IllegalArgumentException expected) {
            control.verify();
        }
    }
    
    public void testDisconnect() throws Exception {
        expectSerbianAtr();
        mockCard.disconnect(false);

        control.replay();
        EidCard card = new EidCard(mockCard);
        card.disconnect();
        try {
            card.disconnect();
            fail("exception expected");
        } catch (NullPointerException expected) {
            control.verify();
        }
    }
    
    public void testDisconnect_error() throws Exception {
        expectSerbianAtr();
        mockCard.disconnect(false);
        EasyMock.expectLastCall().andThrow(new CardException("boo!"));

        control.replay();
        EidCard card = new EidCard(mockCard);
        try {
            card.disconnect();
            fail("exception expected");
        } catch (CardException expected) {
            control.verify();
        }
    }
/*    
    public void testDebugEidInfo() {
        fail("tbd");
    }

    public void testDebugEidInfo_fail() {
        fail("tbd");
    }
    
    public void testReadEidInfo() {
        fail("tbd");
    }

    public void testReadEidInfo_cardException() {
        fail("tbd");
    }

    public void testReadEidInfo_invalidPersonalNumber() {
        fail("tbd");
    }
    
    public void testReadPhoto() {
        fail("tbd");
    }
*/    

    private void expectSerbianAtr() {
        expectAtr(EidCard.KNOWN_EID_ATRS[0]);
    }

    private void expectAtr(byte[] atrSequence) {
        EasyMock.expect(mockCard.getBasicChannel())
                .andStubReturn(mockChannel);
        EasyMock.expect(mockCard.getATR())
                .andStubReturn(new ATR(atrSequence));
    }
}

