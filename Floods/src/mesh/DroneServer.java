package mesh;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by hm649 on 10/05/16.
 */
public class DroneServer implements Runnable {

    private final int PORT = 5555;
    ServerSocket serverSoc;
    private volatile boolean terminate = false;
    private Mesh mesh;
    private ExecutorService threadPool = Executors.newCachedThreadPool();

    public DroneServer(Mesh mesh) {
        this.mesh = mesh;
    }

    @Override
    public void run() {

        try {
            serverSoc = new ServerSocket(PORT);
            while (!terminate) {
                // Drone takes care of closing clientSoc
                try {
                    Socket clientSoc = serverSoc.accept();
                    System.out.println("new drone connected");
                    Drone drone = new Drone(clientSoc, mesh);
                    threadPool.submit(drone);
                } catch (Exception e) {
                    // Don't do anything when an individual socket or drone
                    // fails
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                serverSoc.close();
            } catch (IOException e) {
                //who cares
            }
        }
    }

    public void terminate() {
        terminate = true;
        try {
            serverSoc.close();
        } catch (IOException e) {
            //who cares
        }
    }

}
