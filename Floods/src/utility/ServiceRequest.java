package utility;

import java.util.concurrent.CompletableFuture;

public class ServiceRequest extends Message {
    Location loc;
    boolean removed;
    CompletableFuture<ServiceResponse> future;

    public ServiceRequest(String uuid, Location loc, boolean removed, CompletableFuture<ServiceResponse> future) {
        super(uuid);
        this.loc = loc;
        this.removed = removed;
        this.future = future;

    }

    public Location getLocation() {
        return loc;
    }

    public boolean isRemoved() {
        return removed;
    }
}
