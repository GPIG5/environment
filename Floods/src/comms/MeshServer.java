package comms;

import utility.Location;
import utility.ServiceInterface;
import utility.ServiceRequest;
import utility.ServiceResponse;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.*;

/**
 * Created by hm649 on 10/05/16.
 */
public class MeshServer {

    public AbstractMap<String, Drone> drones = new ConcurrentHashMap<>();
    //in metres
    private float range;
    private DroneServer droneServer;
    private C2Server c2Server;
    private ServiceInterface si;
    private List<Future<?>> futureList = new ArrayList<>();
    private Properties properties = new Properties();

    public void start(ServiceInterface si) {

        try (InputStream in = getClass().getResourceAsStream("/config.properties")) {
            properties.load(in);
        } catch (IOException e) {
            System.err.println("Exception loading config file: " + e.getMessage());
        }

        String rangeStr = getProperty("commRange");
        if (rangeStr == null) {
            System.err.println("Range property not found, using default value");
            this.range = 1000000000;
        } else {
            this.range = Float.valueOf(rangeStr);
        }

        droneServer = new DroneServer(this);
        c2Server = new C2Server(this);

        futureList.add(Executors.newSingleThreadExecutor().submit(droneServer));
        futureList.add(Executors.newSingleThreadExecutor().submit(c2Server));
        this.si = si;
    }

    /**
     * Send a message to all drones in range of tx. If tx is null send to all drones.
     *
     * @param tx  - null if message is from C2
     * @param msg - the message
     */
    public void messageGlobal(Drone tx, String msg) {
        drones.forEach((k, v) -> {
            if ((tx == null && inRange(c2Server.getLocation(), v.getLocation())) ||
                    (tx != null && !k.equals(tx.getUuid()) && inRange(v.getLocation(), tx.getLocation()))) {
                v.addMsgToSend(msg);
            }
        });

        //Send to C2 Server if in range
        if (tx != null && inRange(c2Server.getLocation(), tx.getLocation())) {
            try {
                c2Server.txData(msg);
            } catch (IOException e) {
                System.err.println("Sending data to C2 server exception: " + e.getMessage());
            }
        }
    }

    public ServiceResponse checkForPINOR(String uuid, Location loc) throws InterruptedException, ExecutionException {

        CompletableFuture<ServiceResponse> future = new CompletableFuture<>();

        ServiceRequest sr = new ServiceRequest(uuid, loc, false, future);
        addServiceRequest(sr);

        ServiceResponse resp = future.get();
        return resp;
    }

    public void addServiceRequest(ServiceRequest sr) {
        si.getRequestQueue().add(sr);
    }

    public void terminate() {
        futureList.forEach(future -> future.cancel(true));
        c2Server.terminate();
        droneServer.terminate();
    }

    public String getProperty(String key) {
        return properties.getProperty(key);
    }

    private boolean inRange(Location loc1, Location loc2) {
        return loc1.distance(loc2) <= range;
    }


}
