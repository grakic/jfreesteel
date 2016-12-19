package net.devbase.jfreesteel.applet;

import java.applet.Applet;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;

import javax.smartcardio.CardTerminal;
import javax.smartcardio.TerminalFactory;
import net.devbase.jfreesteel.Reader;
import net.devbase.jfreesteel.Reader.ReaderListener;
import net.devbase.jfreesteel.EidCard;
import net.devbase.jfreesteel.EidInfo;

import java.awt.Image;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.ByteArrayOutputStream;
import javax.xml.bind.DatatypeConverter;

import net.devbase.jfreesteel.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import netscape.javascript.JSObject;
import org.json.simple.JSONObject;

public class EidApplet extends Applet implements ReaderListener {

    private static final long serialVersionUID = -8975515949350240407L;
    private final static Logger logger = LoggerFactory.getLogger(EidApplet.class);
    JSObject window = null;
    EidCard card = null;
    String removedCallback = "removed";
    String insertedCallback = "inserted";

    @Override
    public void init() {
        // pick the first terminal
        CardTerminal terminal = null;
        try {
            TerminalFactory factory = TerminalFactory.getDefault();
            List<CardTerminal> terminals = factory.terminals().list();
            terminal = terminals.get(0);
        } catch(Exception e) {
            logger.error("Terminal error", e);
            stop();
        }

        // build a reader
        Reader reader = new Reader(terminal);
        reader.addCardListener(this);
        logger.info("Terminal initialized.");

        // get callbacks
        removedCallback = getParameter("RemovedCallback");
        insertedCallback = getParameter("InsertedCallback");
    }

    /* export public function as the Mozilla Bug 606737 workaround */
    public void setupJSObject() {
        if((window = JSObject.getWindow(this)) == null) {
            logger.error("JSObject is null!");
            stop();
        }
        logger.info("JSObject initialized");
    }

    @Override
    public void start() {
        setupJSObject();
        logger.info("Applet started");
    }

    public void inserted(EidCard card) {
        this.card = card;

        showStatus("Card inserted.");
        try {
            EidInfo info = card.readEidInfo();
            String infoJson = info.toJSON().toString();

            Image image = card.readEidPhoto();
            String photo = Utils.image2Base64String(image);

            window.call(insertedCallback, new Object[] {infoJson, photo});
        } catch (Exception e) {
            logger.error("Read info exception", e);
            stop();
        }
    }

    public void removed() {
        showStatus("Card removed.");
        window.call(removedCallback, null);
    }
}
