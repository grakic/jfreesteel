/*
 * jfreesteel: Serbian eID Viewer Library Demo (GNU LGPLv3)
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

import java.util.List;
import java.util.Scanner;

import javax.smartcardio.Card;
import javax.smartcardio.CardException;
import javax.smartcardio.CardTerminal;
import javax.smartcardio.TerminalFactory;

import net.devbase.jfreesteel.lib.EidCard;
import net.devbase.jfreesteel.lib.EidInfo;
import net.devbase.jfreesteel.lib.Utils;

/**
 * This is just a simple demonstration how one can use jfreesteel library
 * to include eID viewer support into other applications. Reader interface
 * was not used, instead a direct connection to the Card is made.
 * 
 * See the jFreesteelGUI for the example how to use the Reader interface. 
 * Please note that the JFreesteelGUI is released under GNU Affero GPLv3
 * license, while this class and the rest of the jfreesteel library is
 * released under the more permissive GNU Lesser GPL license version 3.
 * 
 * @author Goran Rakic <grakic@devbase.net>
 */
public class JFreesteel {

	public static CardTerminal pickTerminal(List<CardTerminal> terminals)
	{
		if(terminals.size() > 1) {
			System.out.println("Available readers:\n");
			int c = 1;
			for(Object t:terminals.toArray())
			{
				System.out.format("%d) %s\n", c++, t);
			}

			Scanner in = new Scanner(System.in);
			while(true)
			{
				System.out.print("Select number: ");
				System.out.flush();

				c = in.nextInt();
				if(c > 0 && c < terminals.size()) return terminals.get(c);
			}
		}
		else return terminals.get(0);
	}
		
	public static void main(String[] args)
	{
		CardTerminal terminal = null;
		
        // get the terminal
		try {
	        TerminalFactory factory = TerminalFactory.getDefault();
	        terminal = pickTerminal(factory.terminals().list());

			System.out.println("Using reader   : " + terminal);
		} catch(CardException e) {
			System.err.println("Missing card reader.");
		}
		
		try {
	        // establish a connection with the card
			Card card = terminal.connect("*");

			// read eid data
			EidCard eidcard = new EidCard(card);
			EidInfo info    = eidcard.readEidInfo();

		    System.out.format("ATR            : %s\n", Utils.bytes2HexString(card.getATR().getBytes()));
		    System.out.format("eID number     : %s\n", info.getDocRegNo());
		    System.out.format("Issued         : %s\n", info.getIssuingDate());
		    System.out.format("Valid          : %s\n", info.getExpiryDate());
		    System.out.format("Issuer         : %s\n", info.getIssuingAuthority());
		    System.out.format("JMBG           : %s\n", info.getPersonalNumber());
		    System.out.format("Family name    : %s\n", info.getSurname());
		    System.out.format("First name     : %s\n", info.getGivenName());
		    System.out.format("Middle name    : %s\n", info.getParentGivenName());
		    System.out.format("Gender         : %s\n", info.getSex());
		    System.out.format("Place od birth : %s\n", info.getPlaceOfBirthFull().replace("\n", ", "));
		    System.out.format("Date of birth  : %s\n", info.getDateOfBirth());
		    System.out.format("Street address : %s, %s\n", info.getStreet(), info.getHouseNumber());
		    System.out.format("City           : %s, %s, %s\n", info.getCommunity(), info.getPlace(), info.getState());

		} catch (CardException e) {
			e.printStackTrace();
		}
	}

}
