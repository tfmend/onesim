/* 
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details. 
 */
package routing;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import core.Connection;
import core.DTNHost;
import core.Message;
import core.Settings;
import core.SimClock;
import core.Tuple;

/**
 * Implementation of EASE router as described in 
 * <I>Locating Nodes with EASE:
Last Encounter Routing in Ad Hoc Networks through Mobility Diffusion</I> by
 * Grossglauser, M.; Vetterli, M.
 */
public class EASERouter extends ActiveRouter {
	
	/**
	 * Constructor. Creates a new message router based on the settings in
	 * the given Settings object.
	 * @param s The settings object
	 */
	public EASERouter(Settings s) {
		super(s);
	}

	/**
	 * Copyconstructor.
	 * @param r The router prototype where setting values are copied from
	 */
	protected EASERouter(EASERouter r) {
		super(r);
	}

	@Override
	public void changedConnection(Connection con) {
		if (con.isUp()) { //connection started

		}
	}
	
	
	@Override
	public void update() {
		super.update();
		if (!canStartTransfer() ||isTransferring()) {
			return; // nothing to transfer or is currently transferring 
		}
		
		// try messages that could be delivered to final recipient
		if (exchangeDeliverableMessages() != null) {
			return;
		}
		
		tryOtherMessages();		
	}
	
	@Override
	public MessageRouter replicate() {
		EASERouter r = new EASERouter(this);
		return r;
	}

}
