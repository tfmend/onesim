/*
 * 
 * University of São Paulo
 * 
 * jteodoro at ime.usp.br
 * thiagofurtado17 at gmail.com
 * 
 */
package routing;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import routing.fresh.LastEncounter;
import routing.mop.MemoryNeighbor;
import core.Connection;
import core.Coord;
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
public class MOPRouter extends ActiveRouter {
	
	private List<MemoryNeighbor> lastNeighbors;
	public static final String MAX_NEIGHBOR_MEMORY = "sizeOfMemoriesTable";
	public static int maxEncounters = 100;
	
	/**
	 * Constructor. Creates a new message router based on the settings in
	 * the given Settings object.
	 * @param s The settings object
	 */
	public MOPRouter(Settings s) {
		super(s);
		maxEncounters = s.getInt(MAX_NEIGHBOR_MEMORY);
		lastNeighbors = new LinkedList<MemoryNeighbor>();
	}

	/**
	 * Copyconstructor.
	 * @param r The router prototype where setting values are copied from
	 */
	protected MOPRouter(MOPRouter r) {
		super(r);
		//be careful with deepcopy
		this.lastNeighbors = new LinkedList<MemoryNeighbor>();
	}

	@Override
	public void changedConnection(Connection con) {
		if (con.isUp()) {
			
			DTNHost otherHost = con.getOtherNode(getHost());
			updateNeighborTable(otherHost, this.lastNeighbors );
			
		}
		
	}
	
	//update neighbortable and verify the table size
	private void updateNeighborTable(DTNHost otherHost, List<MemoryNeighbor> neighborTable) {
		
		Date date = new Date();
		MemoryNeighbor neighbor = null;
		if ( this.lastNeighbors.contains(otherHost.getAddress()) ) {
			int pos = this.lastNeighbors.indexOf(otherHost.getAddress());
			neighbor = this.lastNeighbors.get( pos );
			//remove da posicao atual
			this.lastNeighbors.remove(pos);
			//atualiza posicao e tempo
			neighbor.coord = otherHost.getLocation();
			neighbor.time = date.getTime();
		}
		else {
			neighbor = new MemoryNeighbor(otherHost.getAddress(), date.getTime() , otherHost.getLocation());
		}
		
		//insere no final da fila
		this.lastNeighbors.add(neighbor);
		//remove o elemento mais antigo se a fila estiver maior do que deveria
		if ( this.lastNeighbors.size() > maxEncounters ) {
			this.lastNeighbors.remove(0);
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
		
		//send messages to current neihbors
		checkTimeAndPosition(this.getConnections(), new ArrayList<Message>(this.getMessageCollection()) );
		
	}
	
	private void checkTimeAndPosition(List<Connection> currentConnections, List<Message> currentMessages) {
		
		//para cada mensagem cheque os valores e envie o flood
		for ( Message m : currentMessages ) {
			
			//se ninguem conhece o destino
			if ( m.memoryTime == -1 && m.hopsToSearch > 0 && m.floodAgain ) {
				//se eu fizer broadCast eu devo setar o flag para que nao envie novamente
				m.hopsToSearch--;
				m.floodAgain = false;
				sendMessageToConnections(currentConnections, m);
				return;
			}
			
			if ( this.lastNeighbors.contains( m.getTo().getAddress() ) ) {
				//se eu conheco o destino
				int position = this.lastNeighbors.indexOf(m.getTo().getAddress());
				MemoryNeighbor neihbor = this.lastNeighbors.get(position);
				
				//meu tempo eh menor
				if ( m.memoryTime > neihbor.time ) {
					m.lastHopCoord = this.getHost().getLocation();
					m.memoryTime = neihbor.time;
					m.destinationCoord = neihbor.coord;
					sendMessageToConnections(currentConnections, m);
					return;
				}
				
			}
			//minha posicao esta mais perto do destino mesmo que eu nao conheca
			//ou que meu tempo seja maior
			if ( m.memoryTime > 0 && m.destinationCoord.distance(this.getHost().getLocation()) > m.destinationCoord.distance(m.lastHopCoord) ) {
				m.lastHopCoord = this.getHost().getLocation();
				sendMessageToConnections(currentConnections, m);
				return;
			}
			
		}
		
	}
	
	private void sendMessageToConnections(List<Connection> currentConnections, Message currentMessage) {
		
		for (Connection c : currentConnections ) {
			this.startTransfer(currentMessage, c);
		}
		
	}

	
	@Override
	public MessageRouter replicate() {
		MOPRouter r = new MOPRouter(this);
		return r;
	}

}
