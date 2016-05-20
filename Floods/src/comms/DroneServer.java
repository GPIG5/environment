package comms;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Created by hm649 on 10/05/16.
 */
public class DroneServer implements Runnable {

    private final int port;
    private final long droneTimeout;
    private MeshServer mesh;
    private ExecutorService threadPool = Executors.newCachedThreadPool();
    private List<Future<?>> droneFutures = new ArrayList<>();
    private ServerSocket serverSoc = null;

    public DroneServer(MeshServer mesh) {
        this.mesh = mesh;

        String droneTimeoutStr = mesh.getProperty("droneTimeOut");
        if (droneTimeoutStr == null) {
            System.err.println("Drone time out property not found, using default value");
            this.droneTimeout = 2000;
        } else {
            this.droneTimeout = Integer.valueOf(droneTimeoutStr);
        }

        String portStr = mesh.getProperty("envServerPort");
        if (portStr == null) {
            System.err.println("Environment server port property not found, using default value");
            this.port = 5555;
        } else {
            this.port = Integer.valueOf(portStr);
        }
    }

    @Override
    public void run() {

        try (ServerSocket serverSoc = new ServerSocket(port)) {
            this.serverSoc = serverSoc;
            //main loop
            while (!Thread.interrupted()) {
                Socket clientSoc = null;
                try {
                    clientSoc = serverSoc.accept();
                    System.out.println("new drone connected");
                    Drone drone = new Drone(clientSoc, mesh, droneTimeout);
                    droneFutures.add(threadPool.submit(drone));
                } catch (Exception e) {
                    //if the thread did not start clientSoc will not be closed
                    if (clientSoc != null && !clientSoc.isClosed()) {
                        try {
                            clientSoc.close();
                        } catch (IOException e2) {
                            //do nothing as want server to continue
                        }
                    }
                }
            }
        } catch (IOException e) {
            //We care about the server socket not working
            System.err.println("Drone server exception: " + e.getMessage());
        } finally {
            droneFutures.forEach(future -> future.cancel(true));
        }
    }

    public void terminate() {
        if (serverSoc != null) {
            try {
                serverSoc.close();
            } catch (IOException e) {
                //yolo
            }
        }
    }
}
