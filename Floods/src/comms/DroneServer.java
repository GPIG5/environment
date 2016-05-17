package comms;

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
    private ServerSocket serverSoc;
    private volatile boolean terminate = false;
    private MeshServer mesh;
    private ExecutorService threadPool = Executors.newCachedThreadPool();

    public DroneServer(MeshServer mesh) {
        this.mesh = mesh;
    }

    @Override
    public void run() {

        try {
            serverSoc = new ServerSocket(PORT);
            // Drone takes care of closing clientSoc
            while (!terminate) {
                Socket clientSoc = null;
                try {
                    clientSoc = serverSoc.accept();
                    System.out.println("new drone connected");
                    Drone drone = new Drone(clientSoc, mesh);
                    threadPool.submit(drone);
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
