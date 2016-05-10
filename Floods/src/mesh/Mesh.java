package mesh;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by hm649 on 10/05/16.
 */
public class Mesh {

    public ConcurrentHashMap<Integer, Drone> drones = new ConcurrentHashMap<>();


    public void start() {
        Server server = new Server(this);

    }

    /**
     * Send a message to all drones in range of tx. If tx is null send to all drones.
     * @param tx  - null if *every* drone is to be messaged
     * @param msg   - the message
     */
    public void messageGlobal(Drone tx, String msg) {
        drones.forEach( (k,v) -> {
            if (tx != null || (k != tx.getId() && inRange(v.getLocation(), tx.getLocation())) ) {
                if (!v.dataToSend.offer(msg)) {
                    System.err.println("Message queue for drone is full!");
                }
            }
        });
    }

    private boolean inRange(Location loc1, Location loc2) {

        return true;
    }

    public static void main(String[] args) {
        Mesh m = new Mesh();
        m.start();
    }

}
