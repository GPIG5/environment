package mesh;

import utility.Location;
import utility.ServiceInterface;
import utility.ServiceResponse;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;

/**
 * Created by hm649 on 10/05/16.
 */
public class Mesh {

    private final int range = 40;
    public ConcurrentHashMap<String, Drone> drones = new ConcurrentHashMap<>();
    private DroneServer droneServer;

    public Mesh() {
        this.droneServer = new DroneServer(this);
    }

    public static void main(String[] args) {
        Mesh mesh = new Mesh();
        mesh.start(new ServiceInterface());
    }

    public void start(ServiceInterface si) {
        Executors.newSingleThreadExecutor().submit(droneServer);
    }

    /**
     * Send a message to all drones in range of tx. If tx is null send to all drones.
     *
     * @param tx  - null if *every* drone is to be messaged
     * @param msg - the message
     */
    public void messageGlobal(Drone tx, String msg) {
        drones.forEach((k, v) -> {
            if (tx != null && k != tx.getUuid() && inRange(v.getLocation(), tx.getLocation())) {
                v.dataToSend.add(msg);
            }
        });
    }

    public ServiceResponse checkForPINOR(Drone drone, Location location) {

        ServiceResponse test = new ServiceResponse(drone.getUuid(), new ArrayList<Location>(), null);

        return test;
    }

    public void terminate() {
        droneServer.terminate();
    }

    private boolean inRange(Location loc1, Location loc2) {
        return true;
        //return loc1.distance(loc2) < range;
    }
}
