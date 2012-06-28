/*
 * 
 * University of Sao Paulo
 * 
 * jteodoro at ime.usp.br
 * thiagofurtado17 at gmail.com
 * 
 */
package routing;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import routing.fresh.LastEncounter;
import core.Connection;
import core.DTNHost;
import core.Message;
import core.Settings;

/**
 * Implementation of FRESH router as described in 
 * <I>Age Matters: Efficient Route Discovery in Mobile Ad Hoc Networks 
 * Using Encounter Ages</I> by
 * Henri DuboisFerriere, Matthias Grossglauser and Martin Vetterli
 * 
 */
public class FRESHRouter extends ActiveRouter {
	
	private List<Message> fowardMessages = null;
	private List<Message> removeMessages = null;
	//circular queue with length 100
	private LastEncounter[] encounters = null;
	private static final int MAX_ENCOUNTERS = 100;
	private int counter = 0;
	
	/**
	 * Constructor. Creates a new message router based on the settings in
	 * the given Settings object.
	 * @param s The settings object
	 */
	public FRESHRouter(Settings s) {
		super(s);
		this.fowardMessages = new ArrayList<Message>();
		this.removeMessages = new ArrayList<Message>();
		this.encounters = new LastEncounter[MAX_ENCOUNTERS];
	}

	/**
	 * Copyconstructor.
	 * @param r The router prototype where setting values are copied from
	 */
	protected FRESHRouter(FRESHRouter r) {
		super(r);
		//be careful with deepcopy
		this.fowardMessages = new ArrayList<Message>();
		this.removeMessages = new ArrayList<Message>();
		this.encounters = new LastEncounter[MAX_ENCOUNTERS];
	}

	@Override
	public void changedConnection(Connection con) {
		if (con.isUp()) {
			Date now = new Date();
			long mill = now.getTime();
			int otherAddress = con.getOtherNode(getHost()).getAddress();
			//search by previous connection
			
			for ( int i = 0 ; i < MAX_ENCOUNTERS; i++ ) {
				
				if ( this.encounters[i] != null && otherAddress == this.encounters[i].address ) {
					//update encounter's time
					this.encounters[i].time = mill;
					return;
				}
				
			}
			
			//add a new connection in FIFO mode
			this.encounters[ ++counter % MAX_ENCOUNTERS ] = new LastEncounter(otherAddress, mill);
			
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
		
		//send messages to next anchor
		notifyNextAnchors();
		
	}
	
	private void notifyNextAnchors() {
		
		//try delivery messages to neighbors
		this.exchangeDeliverableMessages();
		fowardMessages.clear();
		removeMessages.clear();
		//for all other messages
		//if didnt found the destination send message to next hop
		//only sends message to next hop if this time is lower than the previous time
		//renew TTL before send message
		for ( Message message : getMessageCollection() ) {
						
			long prevTime = getPrevTime(message);
			long time = this.getTimeOfLastEncounterWithDestination(message);
			
			//se eu ja mandei essa mensagem eu nao encaminho de novo
			if ( this.isMessageIterant(message) ) {
				removeMessages.add(message);
				continue;
			}
			
			//se eu nao conheco o cara eu sento o cacete e mando a mensagem para 
			//todo mundo que conheco
			//mas escapa de tudo se o tempo for mais antigo que o cara
			
			if ( time <= prevTime ) {
				
				//add lastEncounterTime property on message
				message.addProperty(String.valueOf( this.getHost().getAddress() )  , Long.valueOf( time ));
				fowardMessages.add(message);
				
			}
			
		}
		
		/* don't leave a copy for the sender */
		for ( Message message : fowardMessages ) {
			this.deleteMessage(message.getId(), false);
		}
		/* don't leave a copy for the sender */
		for ( Message message : removeMessages ) {
			this.deleteMessage(message.getId(), false);
		}
		
		//send fowardmessages to all neighbors
		trySendMessages(fowardMessages, getConnections());
		
	}
	
	private long getTimeOfLastEncounterWithDestination(Message message) {
		
		int addressDestination = message.getTo().getAddress();
		
		for ( int i = 0 ; i < MAX_ENCOUNTERS; i++ ) {
			
			if ( this.encounters[i] != null && addressDestination == this.encounters[i].address ) {
				return this.encounters[i].time;
			}
			
		}
		
		return Long.MAX_VALUE;
		
	}

	private long getPrevTime(Message message) {
		
		if ( message.getHopCount() > 0 ) {
			//acquire time about last hop
			DTNHost previous = message.getHops().get(message.getHopCount() -1);
			return Long.valueOf( message.getProperty( String.valueOf( previous.getAddress() ) ).toString());
		}
		
		return Long.MAX_VALUE;
		
	}
	
	private boolean isMessageIterant(Message message) {
		
		if ( message.getHopCount() > 0 ) {
			//acquire time about last hop
			return message.getProperty( String.valueOf( this.getHost().getAddress() ) ) != null;
		}
		
		return false;
		
	}

	//send messages to connections
	private void trySendMessages(List<Message> messages, List<Connection> connections) {
		
		for ( Message m : messages ) {
			
			for ( Connection con : connections ) {
				startTransfer(m, con);
			}
			
		}
		
	}
	
	@Override
	public MessageRouter replicate() {
		FRESHRouter r = new FRESHRouter(this);
		return r;
	}

}
