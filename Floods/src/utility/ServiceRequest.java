package utility;

public class ServiceRequest extends Message {
	Location loc;
	boolean removed;
	
	public ServiceRequest(String uuid, Location loc, boolean removed) {
		super(uuid);
		this.loc = loc;
		this.removed = removed;
	}
}
