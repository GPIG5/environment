package mesh;

import utility.ServiceInterface;
import utility.Location;
import utility.ServiceResponse;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;

/**
 * Created by hm649 on 10/05/16.
 */
public class Mesh {

    public ConcurrentHashMap<String, Drone> drones = new ConcurrentHashMap<>();

    private final int range = 40;


    public void start(ServiceInterface si) {
        Executors.newSingleThreadExecutor().submit(new Server(this));
    }

    /**
     * Send a message to all drones in range of tx. If tx is null send to all drones.
     * @param tx  - null if *every* drone is to be messaged
     * @param msg   - the message
     */
    public void messageGlobal(Drone tx, String msg) {
        drones.forEach( (k,v) -> {
            if (tx != null && k != tx.getUuid() && inRange(v.getLocation(), tx.getLocation())) {
                v.dataToSend.add(msg);
            }
        });
    }

    public ServiceResponse checkForPINOR(Drone drone, Location location) {

        ServiceResponse test = new ServiceResponse(drone.getUuid(), new ArrayList<Location>(), null);

        return test;
    }

    private boolean inRange(Location loc1, Location loc2) {
        return true;
        //return loc1.distance(loc2) < range;
    }

    public static void main(String[] args) {
        Mesh mesh = new Mesh();
        mesh.start(new ServiceInterface());
    }
}
