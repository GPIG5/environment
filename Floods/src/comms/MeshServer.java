package comms;

import utility.Location;
import utility.ServiceInterface;
import utility.ServiceRequest;
import utility.ServiceResponse;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * Created by hm649 on 10/05/16.
 */
public class MeshServer {
    private AbstractMap<String, Drone> drones = new HashMap<>();
    private float range;
    private DroneServer droneServer;
    private C2Server c2Server;
    private List<Future<?>> futureList = new ArrayList<>();
    private Properties properties = new Properties();
    private Queue<ServiceRequest> queueRequests;

    public void start(ServiceInterface si, String propertiesLoc) {


        if (propertiesLoc != null) {
            try (FileInputStream in = new FileInputStream(new File(propertiesLoc))) {
                properties.load(in);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            try (InputStream in = getClass().getResourceAsStream("/config.properties")) {
                if (in != null) {
                    properties.load(in);
                } else {
                    System.err.println("Could not load config file");
                }
            } catch (IOException e) {
                System.err.println("Exception loading config file: " + e.getMessage());
            }
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
        queueRequests = si.getRequestQueue();
    }

    /**
     * Send a message to all drones in range of tx. If tx is null send to all drones.
     *
     * @param tx  - null if message is from C2
     * @param msg - the message
     */
    public void messageGlobal(Drone tx, String msg) {
    	synchronized (drones) {
	        drones.forEach((k, v) -> {
	            if ((tx == null && inRange(c2Server.getLocation(), v.getLocation())) ||
	                    (tx != null && !k.equals(tx.getUuid()) && inRange(v.getLocation(), tx.getLocation()))) {
	                v.addMsgToSend(msg);
	            }
	        });
    	}

        //Send to C2 Server if in range
        if (tx != null && inRange(c2Server.getLocation(), tx.getLocation())) {
           messageC2(msg);
        }
    }

    public void messageC2(String msg) {
        try {
            c2Server.txData(msg);
        } catch (IOException e) {
            System.err.println("Sending data to C2 server exception: " + e.getMessage());
        }
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
    
    // Try and add a drone, returning false if drone already exists.
    public boolean addDrone(String uuid, Drone drone) {
        synchronized (drones) {
            if (drones.containsKey(uuid)) {
                return false;
            } else {
                drones.put(uuid, drone);
                return true;
            }
        }
    }

    // Remove a drone and send a remove message to the simulation.
    public void removeDrone(String uuid) {
        synchronized (drones) {
            drones.remove(uuid);
            queueRequests.offer(new ServiceRequest(uuid, null, true, null));
        }
    }

    public Map<String, Drone> getDrones() {
    	return this.drones;
    }
    
    public Queue<ServiceRequest> getRequestQueue() {
    	return queueRequests;
    }
}
