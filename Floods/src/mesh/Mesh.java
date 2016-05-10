package mesh;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by hm649 on 10/05/16.
 */
public class Mesh {

    public ConcurrentHashMap<Integer, Drone> drones = new ConcurrentHashMap<>();

    public Mesh() {

    }

    public void start() {
        Server server = new Server(this);

    }

    public void messageSpecificDrone() {
        ;
    }

    /**
     * Send a message to all drones except the one specified by txId
     * @param txId  - null if *every* drone is to be messaged
     * @param msg   - the message
     */
    public void messageGlobal(Integer txId, String msg) {
        drones.forEach( (k,v) -> {
            if (txId != null && (k != txId)) {
                v.dataToSend.add(msg);
            }
        });
    }

    public static void main(String[] args) {
        Mesh m = new Mesh();
        m.start();
    }

}
