package mesh;

import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;

/**
 * Created by hm649 on 10/05/16.
 */
public class Mesh {

    public ConcurrentHashMap<Integer, Drone> drones = new ConcurrentHashMap<>();

    private final int range = 40;


    public void start() {
        Executors.newSingleThreadExecutor().submit(new Server(this));
    }

    /**
     * Send a message to all drones in range of tx. If tx is null send to all drones.
     * @param tx  - null if *every* drone is to be messaged
     * @param msg   - the message
     */
    public void messageGlobal(Drone tx, String msg) {
        drones.forEach( (k,v) -> {
            if (tx != null || (k != tx.getUuid() && inRange(v.getLocation(), tx.getLocation())) ) {
                if (!v.dataToSend.offer(msg)) {
                    System.err.println("Message queue for drone is full!");
                }
            }
        });
    }

    public List<Vector2f> checkForPINOR(Vector3f location) {

        List<Vector2f> PINORLocs = new ArrayList<>();

        return PINORLocs;
    }

    private boolean inRange(Vector3f loc1, Vector3f loc2) {
        return loc1.distance(loc2) < range;
    }

    public static void main(String[] args) {
        Mesh m = new Mesh();
        m.start();
    }

}
