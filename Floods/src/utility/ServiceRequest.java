package utility;

import java.util.concurrent.CompletableFuture;

import comms.Drone;

public class ServiceRequest extends Message {
    Location loc;
    boolean removed;
    Drone drone;

    public ServiceRequest(String uuid, Location loc, boolean removed, Drone drone) {
        super(uuid);
        this.loc = loc;
        this.removed = removed;
        this.drone = drone;

    }

    public Location getLocation() {
        return loc;
    }
    
    public Drone getDrone() {
        return drone;
    }

    public boolean isRemoved() {
        return removed;
    }
}
