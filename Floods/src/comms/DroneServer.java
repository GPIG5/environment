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

    private final int PORT = 5555;
    private MeshServer mesh;
    private ExecutorService threadPool = Executors.newCachedThreadPool();
    private List<Future<?>> droneFutures = new ArrayList<>();
    private ServerSocket serverSoc = null;

    public DroneServer(MeshServer mesh) {
        this.mesh = mesh;
    }

    @Override
    public void run() {

        try (ServerSocket serverSoc = new ServerSocket(PORT)) {
            this.serverSoc = serverSoc;
            //main loop
            while (!Thread.interrupted()) {
                Socket clientSoc = null;
                try {
                    clientSoc = serverSoc.accept();
                    System.out.println("new drone connected");
                    Drone drone = new Drone(clientSoc, mesh);
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
