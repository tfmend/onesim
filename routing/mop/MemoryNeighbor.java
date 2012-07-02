package routing.mop;

import core.Coord;

public class MemoryNeighbor {

	public MemoryNeighbor(int address, long time, Coord coord) {
		this.time = time;
		this.address = address;
		this.coord = coord;
	}
	
	public long time;
	public int address;
	public Coord coord;
	
	@Override
	public boolean equals(Object obj) {
		if ( obj instanceof MemoryNeighbor ) {
			return this.address == ( (MemoryNeighbor) obj).address;
		}
		if ( obj instanceof Integer ) {
			return this.address == Integer.valueOf(obj.toString()).intValue();
		}
		return false;
	}
	
}
