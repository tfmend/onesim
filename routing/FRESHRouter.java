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
	
	private List<Message> forwardMessages = null;
	private List<Message> removeMessages = null;

	private List<LastEncounter> lastEncounteredHosts;
	private static final int MAX_TABLE_SIZE = 100;
	
	private int counter = 0;
	
	/**
	 * Constructor. Creates a new message router based on the settings in
	 * the given Settings object.
	 * @param s The settings object
	 */
	public FRESHRouter(Settings s) {
		super(s);
		forwardMessages =  new ArrayList<Message>();
		removeMessages =  new ArrayList<Message>();
		lastEncounteredHosts = new ArrayList<LastEncounter>();
	}

	/**
	 * Copyconstructor.
	 * @param r The router prototype where setting values are copied from
	 */
	protected FRESHRouter(FRESHRouter r) {
		super(r);
		forwardMessages =  new ArrayList<Message>();
		removeMessages =  new ArrayList<Message>();
		lastEncounteredHosts = new ArrayList<LastEncounter>();
	}

	@Override
	public void changedConnection(Connection con) {
		if (con.isUp()) {
			Date now = new Date();
			long t = now.getTime();
			int ad = con.getOtherNode(getHost()).getAddress();
			LastEncounter l = new LastEncounter(ad, t);

			int ret = addLastEncounteredHost(l);			
		}
	}

	private int addLastEncounteredHost(LastEncounter l)
	{
		int i = lastEncounteredHosts.indexOf(l); 
		if(i == -1) // the host was never encountered 
		{
			if(lastEncounteredHosts.size() >= MAX_TABLE_SIZE)
				lastEncounteredHosts.remove(0); // remove the older entry of table
			
			return lastEncounteredHosts.add(l) ? 1 : 0; // return 0 if l was not added	
		}

		// if the host was encountered and is in table the execution bypass to here
		// As always remove a host to add other, is not necessary to verify the size 
		// of the table, because i am replace a passed entry
		lastEncounteredHosts.remove(i);
		return lastEncounteredHosts.add(l) ? 2 : 0;
	}

	private long prevEncounterAge(int addr)
	{
		Date d = new Date();
		for(int i = 0; i < lastEncounteredHosts.size(); i++)
		{
			if(lastEncounteredHosts.get(i).address == addr)
				return d.getTime() - lastEncounteredHosts.get(i).time; 
		}
		return -1; 
	}

	/// to verify
	
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
		forwardMessages.clear();
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
				forwardMessages.add(message);
				
			}
			
		}
		
		/* don't leave a copy for the sender */
		for ( Message message : forwardMessages ) {
			this.deleteMessage(message.getId(), false);
		}
		/* don't leave a copy for the sender */
		for ( Message message : removeMessages ) {
			this.deleteMessage(message.getId(), false);
		}
		
		//send forwardMessages to all neighbors
		trySendMessages(forwardMessages, getConnections());
		
	}
	
	private long getTimeOfLastEncounterWithDestination(Message message) {
		
		int addressDestination = message.getTo().getAddress();
		
		for ( int i = 0 ; i < lastEncounteredHosts.size(); i++ ) {
			
			if ( addressDestination == this.lastEncounteredHosts.get(i).address ) {
				return this.lastEncounteredHosts.get(i).time;
			}
			
		}
		
		return Long.MIN_VALUE;
		
	}

	private long getPrevTime(Message message) {
		
		if ( message.getHopCount() > 0 ) {
			//acquire time about last hop
			DTNHost previous = message.getHops().get(message.getHopCount() -1);
			return Long.valueOf( message.getProperty( String.valueOf( previous.getAddress() ) ).toString());
		}
		
		return Long.MIN_VALUE;
		
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
