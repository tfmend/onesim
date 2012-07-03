package routing.fresh;

public class LastEncounter {

	public LastEncounter(int address, long time) 
	{
		this.time = time;
		this.address = address;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + address;
		long temp;
		temp = Double.doubleToLongBits(address);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		LastEncounter other = (LastEncounter) obj;
		if (address != other.address)
			return false;
		return true;
	}
	
	public long time;
	public int address;
	
}
