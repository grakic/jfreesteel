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

import java.util.LinkedList;
import java.util.ListIterator;

import javax.smartcardio.CardException;
import javax.smartcardio.CardTerminal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Reader class maintains the connection with the terminal and provides
 * an interface for your code to receive card insertion/removal events.
 * 
 * To assign the listeners for the event, pass the object implementing
 * ReaderListener interface to the Reader.addCardListener() method.
 * 
 * On card insertion insert() method of the listener will be called
 * passing new EidCard object that can be used to read data from the
 * Serbian eID card. Listener should assume that card is removed in the
 * default state.
 * 
 * Reader will ignore cards with an unknown ATR.
 * 
 * @author Goran Rakic <grakic@devbase.net>
 */
public class Reader {
	
	private static final Logger logger = LoggerFactory.getLogger(Reader.class);
	/**
	 * CardTerminal this Reader is assigned to
	 */
	private CardTerminal terminal;
	
	/**
	 * EidCard is not null when the card is inserted
	 */
	private EidCard eidcard = null;

	/**
	 * List of card listeners to be notifies on card insertion/removal
	 */
	private LinkedList<ReaderListener> listeners;

	public interface ReaderListener
	{
		/**
		 * Card is inserted into the reader terminal. Use EidCard object
		 * to read data from the eID card.
		 * 
		 * @param card EidCard object
		 */
		public void inserted(EidCard card);
		
		/**
		 * Card is removed from the reader terminal
		 */
		public void removed();
	}

	/**
	 * Thread waiting for card insert/removal
	 */
	private Thread listenerThread;

	/**
	 * Add new card listener to be notified on card insertion/removal.
	 * Listeners should assume that the card is removed in default state.
	 * 
	 * @param listener Card listener object to be added
	 */
	public void addCardListener(ReaderListener listener)
	{
		listeners.add(listener);
		
		// if the card is inserted, notify the listener about current state
		notifyCardListener(listener, true);
	}
	
	/**
	 * Remove card listener from the list of listeners. Does nothing
	 * if the listener is not present in the list.
	 * 
	 * @param listener Previously added card listener object to be removed
	 */
	public void removeCardListener(ReaderListener listener)
	{
		listeners.remove(listener);
	}
	
	private void notifyCardListener(ReaderListener listener, boolean inserted_only)
	{
		if(eidcard != null) listener.inserted(eidcard);
		else if(!inserted_only) listener.removed();
	}
	
    public void connect() throws CardException
    {
    	logger.info("Reader CONNECT");
    	eidcard = new EidCard(terminal.connect("*"));    	
    }
    
    public void disconnect() throws CardException
    {
    	logger.info("Reader DISCONNECT");
    	eidcard.disconnect(false);
    	eidcard = null;
    }
	
	public Reader(final CardTerminal terminal)
	{		
		this.terminal = terminal;
		listeners = new LinkedList<ReaderListener>();
		
		// start card connection in a new thread
		listenerThread = new Thread(new Runnable()
		{
			public void run()
			{
				try
				{
			    	// main thread loop
			    	while(true)
			    	{
			    	    // wait for a status change
			    		try {
			            	if(eidcard == null) {
			            		terminal.waitForCardPresent(0);
			            		connect();
			            	}
			            	else {
			            		terminal.waitForCardAbsent(0);
			            		disconnect();
			            	}
			        	}
			        	catch(CardException e1) {
			        		// try to re-connect, will step out on new exception
			            	if(terminal.isCardPresent()) connect();
			        	}

			    		// notify status to all listeners
			    	    ListIterator<ReaderListener> i = listeners.listIterator();
			    	    while(i.hasNext())
			    	    {
			    	    	ReaderListener listener = i.next();
			    	    	notifyCardListener(listener, false);
			    	    }
			    	}
				}
				catch(CardException e2) {
					// Break the loop, exit thread
				}	
			}
		});
		listenerThread.start();
	}
}
