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

import java.util.concurrent.CopyOnWriteArrayList;

import javax.smartcardio.CardException;
import javax.smartcardio.CardTerminal;
import javax.smartcardio.Card;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Reader class maintains the connection with the terminal and provides an
 * interface for your code to receive card insertion/removal events.
 *
 * To assign the listeners for the event, pass the object implementing
 * ReaderListener interface to the Reader.addCardListener() method.
 *
 * On card insertion insert() method of the listener will be called passing new
 * EidCard object that can be used to read data from the Serbian eID card.
 * Listener should assume that card is removed in the default state.
 *
 * Reader will ignore cards with an unknown ATR.
 *
 * @author Goran Rakic (grakic@devbase.net)
 */
@SuppressWarnings("restriction")  // Various javax.smartcardio.*
public class Reader {

    private static final Logger logger = LoggerFactory.getLogger(Reader.class);
    /** CardTerminal this Reader is assigned to */
    private CardTerminal terminal;

    /** EidCard is not null when the card is inserted */
    private volatile EidCard eidcard;

    /** List of card listeners to be notifies on card insertion/removal. */
    // Must be a copy-on-write array list, as list notification will crash if
    // someone adds a listener while notification is in progress; due to
    // concurrent modification of the iterated collection.
    private CopyOnWriteArrayList<ReaderListener> listeners;

    /** Thread waiting for card insert/removal */
    private Thread listenerThread;

    public interface ReaderListener {
        /**
         * Card is inserted into the reader terminal. Use EidCard object to read
         * data from the eID card.
         *
         * @param card EidCard object
         */
        public void inserted(EidCard card);

        /** Card is removed from the reader terminal */
        public void removed();
    }

    public Reader(final CardTerminal terminal) {
        this.terminal = terminal;
        listeners = new CopyOnWriteArrayList<ReaderListener>();

        // start card connection in a new thread
        listenerThread = new Thread(new Runnable() {
            public void run() {

                logger.info(String.format("Init Reader on terminal %s", terminal.getName()));
                try {
                    // sometimes reader is not blocking on waitForCard*, we start with inf. timeout
                    int timeoutMs = 0;

                    boolean wrongCardPresent = false;

                    // fix issues with Java on Mac OS X
                    boolean buggyJava = isMacWithBuggyJava();
                    if (buggyJava) {
                        logger.info("Working with buggy Java, doing my best");
                    }

                    // main thread loop
                    while (true) {

                        boolean statusChanged = true;
                        logger.info("Loop entry");

                        try {

                            logger.info("Inside try...");
                            boolean cardPresent = isCardPresent(buggyJava);
                            logger.info(String.format("Card present %b", cardPresent));

                            // wait for status change if not on buggy java
                            if (!buggyJava) {
                                if (!cardPresent) {
                                    logger.info("Card not present, wait for insertion");
                                    terminal.waitForCardPresent(timeoutMs);
                                } else if ((eidcard != null || wrongCardPresent) && cardPresent) {
                                    logger.info("Card present, wait for removal");
                                    terminal.waitForCardAbsent(timeoutMs);
                                }
                                cardPresent = isCardPresent(buggyJava);
                            }

                            // change the status
                            if (eidcard == null && cardPresent) {
                                connect();
                            } else if (eidcard != null && !cardPresent) {
                                disconnect();
                            } else if (!wrongCardPresent){
                                // either we are with buggyJava, or there is another bug in PC/SC
                                // and waitForCard*(0) is not blocking and returns immediately!
                                // Increase the timeout not to burn cpu
                                logger.info("Setting timeout to 3 seconds");
                                timeoutMs = 3000;
                                statusChanged = false;
                            } else {
                                statusChanged = false;
                            }

                        } catch (IllegalArgumentException e1) {
                            // wrong card
                            logger.info("WRONG CARD");
                            statusChanged = false;
                            wrongCardPresent = true;

                        } catch (CardException e1) {
                            // force "disconnect"
                            eidcard = null;

                            // try to reconnect if card is present and continue the loop
                            if (isCardPresent(buggyJava)) {
                                logger.info("RE-CONNECT");
                                // will step out on repeated exception
                                connect();
                            }
                        }

                        if (statusChanged) {
                            notifyListeners();
                        }

                        if (buggyJava) {
                            try {
                                Thread.sleep(300);
                            } catch(InterruptedException ex) {
                                Thread.currentThread().interrupt();
                            }
                        }
                    }
                } catch (CardException e2) {
                    // Break the loop, exit thread
                    // TODO: Should we notify our listeners that there is no Reader thread?
                }
            }

            /** Notify all listeners. */
            private void notifyListeners() {
                for (ReaderListener listener : listeners) {
                    notifyCardListener(listener, false);
                }
            }

        });
        listenerThread.start();
    }

    /**
     * Add new card listener to be notified on card insertion/removal. Listeners
     * should assume that the card is removed in default state.
     *
     * @param listener Card listener object to be added
     */
    public void addCardListener(ReaderListener listener) {
        listeners.add(listener);

        // if the card is inserted, notify the listener about the current state
        notifyCardListener(listener, true);
    }

    /**
     * Remove card listener from the list of listeners. Does nothing if the
     * listener is not present in the list.
     *
     * @param listener Previously added card listener object to be removed
     * @return true if the removal succeeded; false otherwise
     */
    public boolean removeCardListener(ReaderListener listener) {
        return listeners.remove(listener);
    }

    private void notifyCardListener(ReaderListener listener, boolean inserted_only) {
        if (eidcard != null) {
            listener.inserted(eidcard);
        } else if (!inserted_only) {
            listener.removed();
        }
    }

    public void connect() throws CardException {
        logger.info("CONNECT");
        eidcard = EidCard.fromCard(terminal.connect("*"));
    }

    public void disconnect() throws CardException {
        logger.info("DISCONNECT");
        eidcard.disconnect();
        eidcard = null;
    }

    /**
     * JDK bug #7195480: http://bugs.java.com/bugdatabase/view_bug.do?bug_id=7195480
     *
     * pcscd runs in 32bit mode, and 64bit Oracle Java is not compiled correctly and mistakes message
     * sizes and what else not... terminal.isCardPresent() always returns false, terminal.waitForCard()
     * and terminals.waitForChange() do not wait, and terminals.list() occasionally causes SIGSEGV!
     *
     *All should be fixed in JRE 7u80, 8u20, and 9
     */
    private boolean isMacWithBuggyJava() {
        final String os = System.getProperty("os.name").toLowerCase();
        final String version = System.getProperty("java.version");

        return (os.indexOf("mac") >= 0) && (
            (version.startsWith("1.7") && Utils.compareVersions("1.7.0_80", version) < 0) ||
            (version.startsWith("1.8") && Utils.compareVersions("1.8.0_20", version) < 0)
        );
    }

    private synchronized boolean isCardPresent(boolean buggyJava) throws CardException {
        if (!buggyJava) return terminal.isCardPresent();

	    try {
            Card card = terminal.connect("*");
            card.disconnect(false);
            return true;
        } catch (CardException e) {
            return false;
        }
    }

}

