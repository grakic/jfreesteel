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

import netscape.javascript.JSObject;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

public class EidApplet extends Applet implements ReaderListener {

    private static final long serialVersionUID = -8975515949350240407L;
    private static final Logger logger = Logger.getLogger(EidApplet.class);
    JSObject window = null;
    EidCard card = null;

    @Override
    public void init() {
        configureLog4j();

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
	
    private static void configureLog4j() {
        Properties properties = new Properties();
        InputStream propertiesStream = EidApplet.class.getResourceAsStream("/net/devbase/jfreesteel/applet/log4j.properties");
        if(propertiesStream != null) {
            try {
                properties.load(propertiesStream);
            } catch (IOException e) {
                System.err.println("Logger configuration error");
            }
            PropertyConfigurator.configure(properties);
        } else {
            System.err.println("Logger configuration missing");
        }
    }

    public void inserted(EidCard card) {
        this.card = card;

        showStatus("Card inserted.");
        try {
            EidInfo info = card.readEidInfo();
            String name = info.getNameFull();

            Image image = card.readEidPhoto();

            BufferedImage bufferedImage = new BufferedImage(
                image.getWidth(null), image.getHeight(null),
                BufferedImage.TYPE_INT_BGR);
            bufferedImage.createGraphics().drawImage(image, 0, 0, null);

            ByteArrayOutputStream output = new ByteArrayOutputStream();
            ImageIO.write(bufferedImage, "jpg", output);
            byte[] bytes = output.toByteArray();
            String photo = DatatypeConverter.printBase64Binary(bytes);

            window.call("inserted", new Object[] {name, photo});
        } catch (Exception e) { 
            logger.error("Read info exception", e);
            stop();
        }
    }

    public void removed() {
        showStatus("Card removed.");
        window.call("removed", null);
    }
}
