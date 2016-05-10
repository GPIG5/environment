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

    public void messageGlobal() {
        ;
    }

    public boolean inRange(Drone tx, Drone rx) {

        return false;
    }


}
