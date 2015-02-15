/*
 * jfreesteel: Serbian eID Viewer GUI Application (GNU AGPLv3)
 * Copyright (C) 2011 Goran Rakic
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License version
 * 3.0 as published by the Free Software Foundation.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public
 * License along with this software; if not, see
 * http://www.gnu.org/licenses/.
 */
package net.devbase.jfreesteel.viewer;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

import javax.smartcardio.CardException;
import javax.smartcardio.CardTerminal;
import javax.smartcardio.TerminalFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.imageio.ImageIO;

import net.devbase.jfreesteel.EidCard;
import net.devbase.jfreesteel.EidInfo;
import net.devbase.jfreesteel.Reader;
import net.devbase.jfreesteel.Reader.ReaderListener;
import net.devbase.jfreesteel.gui.GUIPanel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.itextpdf.text.DocumentException;

/**
 * EidViewer is a singleton class behind EidViewer application
 * 
 * @author Goran Rakic (grakic@devbase.net)
 */
@SuppressWarnings("restriction")  // Access to restricted card APIs
public class EidViewer extends JPanel implements ReaderListener {
    
    private static final long serialVersionUID = -2497143822816312498L;

    private static final String[] ICON_FILES = {
            "eidviewer20.png", "eidviewer26.png", "eidviewer32.png"};

    private static final String ICON_RESOURCE = 
        "/net/devbase/jfreesteel/viewer/smart-card-reader2.jpg";

    private final static Logger logger = LoggerFactory.getLogger(EidCard.class);
    
    private static final ResourceBundle bundle = ResourceBundle.getBundle(
        "net.devbase.jfreesteel.viewer.viewer");

    private EidInfo info;
    private Image photo;
    
    private JFrame frame;
    private GUIPanel details;
    private JButton button;

    private static EidViewer instance;
    
    public EidViewer() {    	
        setSize(new Dimension(720, 350));
        setLayout(new CardLayout(0, 0));

        /* Create "insert card" splash screen */
        JPanel splash = new JPanel();
        splash.setBackground(Color.WHITE);
        splash.setLayout(new GridBagLayout());
        ImageIcon insertCardIcon = new ImageIcon(getClass().getResource(ICON_RESOURCE));
        JLabel label = getLabel(insertCardIcon);
        splash.add(label, new GridBagConstraints());

        add(splash, "splash");

        /* Add card details screen */
        details = new EidViewerPanel();
        add(details, "details");
    }

    private JLabel getLabel(ImageIcon insertCardIcon) {
        JLabel label = new JLabel(
                bundle.getString("InsertCard"), insertCardIcon, SwingConstants.CENTER);
        Font labelFont = label.getFont();
        label.setFont(
            labelFont.deriveFont(labelFont.getStyle() | Font.BOLD,
            labelFont.getSize() + 4f));
        return label;
    }

    public static EidViewer getInstance()
    {
        if(instance == null) {
            instance = new EidViewer();
        }
        return instance;
    }
    
    public void setFrame(JFrame frame) {
        this.frame = frame;
    }
    
    public static void main(String[] args) {
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                createAndShowGUI();
            }
        });
    }

    /**
     * Return JVM version
     */
    private static double getVersion () {
        String version = System.getProperty("java.version");
        int pos = 0, count = 0;
        for ( ; pos<version.length() && count < 2; pos ++) {
            if (version.charAt(pos) == '.') count ++;
        }
        return Double.parseDouble(version.substring(0, pos-1));
    }
     
    /**
     * Create the GUI and show it.
     */
    private static void createAndShowGUI() {
        // Enable font anti aliasing
        System.setProperty("awt.useSystemAAFontSettings","on");
        System.setProperty("swing.aatext", "true");
        
        // Set sr_RS locale as default
        Locale.setDefault(new Locale("sr", "RS"));
        
        // Create and set up the window
        JFrame frame = new JFrame(bundle.getString("FreesteelTitle"));
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);

        // Set window icon
        List<Image> icons = new ArrayList<Image>();
        for (String iconFile : ICON_FILES) {
            try {
                icons.add(ImageIO.read(frame.getClass().getResource(
                    "/net/devbase/jfreesteel/viewer/" + iconFile)));
            } catch (IOException e) {
                logger.error("Could not find icon file "+iconFile, e);
            }
        }
        frame.setIconImages(icons);

        // Set default look and feel
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            JOptionPane.showMessageDialog(frame,
                    bundle.getString("GUIError") + ": " + e.getMessage(),
                    bundle.getString("GUIErrorTitle"),
                    JOptionPane.WARNING_MESSAGE);
            logger.error("Error setting look and feel", e);
        }
        
        // Test for Java 1.6 or newer
        if(getVersion() < 1.6) {
            JOptionPane.showMessageDialog(frame,
                    bundle.getString("JavaError"),
                    bundle.getString("JavaErrorTitle"),
                    JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }

        // Get the list of terminals
        CardTerminal terminal = null;
        try {
            TerminalFactory factory = TerminalFactory.getDefault();
            terminal = pickTerminalGUI(frame, factory.terminals().list());

        } catch (Exception e) {
            JOptionPane.showMessageDialog(frame,
                bundle.getString("ReaderError") + ": " + e.getMessage(),
                bundle.getString("ReaderErrorTitle"),
                JOptionPane.ERROR_MESSAGE);
            logger.error("Reader error", e);
            System.exit(1);
        }

        // Create and set up the content pane
        EidViewer app = EidViewer.getInstance();
        app.setFrame(frame);
        frame.getContentPane().add(app, BorderLayout.CENTER);
        frame.pack();

        // Create reader and add GUI as the listener
        Reader reader = new Reader(terminal);
        reader.addCardListener(app);

        // Display the window
        frame.setVisible(true);
    }

    public static CardTerminal pickTerminalGUI(JFrame frame, List<CardTerminal> terminals) {
        if (terminals.size() == 1) {
            return terminals.get(0);
        }
        
        CardTerminal terminal = (CardTerminal) JOptionPane.showInputDialog(
            frame,
            bundle.getString("SelectReader"),
            bundle.getString("SelectReaderTitle"),
            JOptionPane.PLAIN_MESSAGE,
            null,
            terminals.toArray(),
            terminals.get(0));

        // Cancel clicked
        if(terminal == null) System.exit(1);

        return terminal;
    }

    private void showCardError(Exception e) {
        JOptionPane.showMessageDialog(this,
            bundle.getString("CardError") + ": " + e.getMessage(),
            bundle.getString("CardErrorTitle"),
            JOptionPane.ERROR_MESSAGE);
        logger.error("Card error", e);
    }

    public void inserted(final EidCard card) {
        logger.info("Card inserted");
        CardLayout cl = (CardLayout) this.getLayout();
        cl.show(this, "details");        

        try {
            info = card.readEidInfo();            
            details.setDetails(info);
            photo = card.readEidPhoto();            
            details.setPhoto(photo);
            button.setEnabled(true);
        } catch (CardException e) {
            showCardError(e);
        } catch (Exception e) {
            showCardError(e);        	
        }
    }

    public void removed() {
        logger.info("Card removed");

        CardLayout cl = (CardLayout) this.getLayout();
        cl.show(this, "splash");
        
        button.setEnabled(false);
        info = null;
        photo = null;
        details.clearDetailsAndPhoto();
    }

    /** UI panel for the application */
    class EidViewerPanel extends GUIPanel {

        private static final long serialVersionUID = 1L;

        public EidViewerPanel() {
            super();
            button = newButton();
            toolbar.add(button, BorderLayout.WEST);
        }

        private JButton newButton() {
            JButton button = new JButton(bundle.getString("SavePDF"));
            button.setEnabled(false);
            button.setPreferredSize(new Dimension(130, 36));
            button.setSize(new Dimension(200, 0));
            button.addActionListener(new ButtonActionListener());
            return button;
        }
    }

    /** The action taken on the UI action button press. */
    private class ButtonActionListener implements ActionListener {
        @Override public void actionPerformed(ActionEvent ev) {

            final JFileChooser fc = new JFileChooser();
            fc.setSelectedFile(new File("report_" + info.getPersonalNumber() + ".pdf"));
            FileNameExtensionFilter filter = new FileNameExtensionFilter(
                "PDF", "pdf");
            fc.setFileFilter(filter);
            int returnVal = fc.showSaveDialog(frame);
            
            if(returnVal == JFileChooser.APPROVE_OPTION) {
                // Append correct extension if missing
                String filename = fc.getSelectedFile().toString();
                if(!filename.toLowerCase().endsWith(".pdf")) {
                    filename += ".pdf";
                }

                try {
                    logger.info("Saving " + filename);
                    PdfReport report = new PdfReport(info, photo);
                    report.write(filename);
                } catch (IOException e) {
                    JOptionPane.showMessageDialog(frame,
                        bundle.getString("SavePDFError") + ": " + e.getMessage(),
                        bundle.getString("SavePDFErrorTitle"),
                        JOptionPane.ERROR_MESSAGE);
                    logger.error("Error saving PDF file", e);
                } catch (DocumentException e) {
                    JOptionPane.showMessageDialog(frame,
                        bundle.getString("CreatePDFError") + ": " + e.getMessage(),
                        bundle.getString("CreatePDFErrorTitle"),
                        JOptionPane.ERROR_MESSAGE);
                    logger.error("Error creating PDF file", e);
                }
            }
        }
    }
}
