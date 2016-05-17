package comms;

import utility.Location;
import utility.ServiceInterface;
import utility.ServiceResponse;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;

/**
 * Created by hm649 on 10/05/16.
 */
public class MeshServer {

    private final int range = 40;
    public ConcurrentHashMap<String, Drone> drones = new ConcurrentHashMap<>();
    private DroneServer droneServer;
    private C2Server c2Server;

    public MeshServer() {
        droneServer = new DroneServer(this);
        c2Server = new C2Server(this, new Location(0,0,0));
        //todo read in config file with server location
    }

    public void start(ServiceInterface si) {
        Executors.newSingleThreadExecutor().submit(droneServer);
        Executors.newSingleThreadExecutor().submit(c2Server);
    }

    /**
     * Send a message to all drones in range of tx. If tx is null send to all drones.
     *
     * @param tx  - null if message is from C2
     * @param msg - the message
     */
    public void messageGlobal(Drone tx, String msg) {
        drones.forEach((k, v) -> {
            if (tx == null || !k.equals(tx.getUuid()) && inRange(v.getLocation(), tx.getLocation())) {
                v.dataToSend.add(msg);
            }
        });

        //Send to C2 Server if in range
        if (tx != null && inRange(c2Server.getLocation(), tx.getLocation())) {
            c2Server.txData(msg);
        }
    }

    public ServiceResponse checkForPINOR(Drone drone, Location location) {

        ServiceResponse test = new ServiceResponse(drone.getUuid(), new ArrayList<Location>(), null);

        return test;
    }

    public void terminate() {
        droneServer.terminate();
        c2Server.terminate();
    }

    private boolean inRange(Location loc1, Location loc2) {
        //TODO use geotool
        return true;
        //return loc1.distance(loc2) < range;
    }

    public static void main(String[] args) {
        MeshServer mesh = new MeshServer();
        mesh.start(new ServiceInterface());
    }

}
