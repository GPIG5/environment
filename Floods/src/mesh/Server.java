package mesh;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Created by hm649 on 10/05/16.
 */
public class Server implements Runnable {

    private final int PORT = 5555;

    private Mesh mesh;
    private ExecutorService threadPool = Executors.newCachedThreadPool();

    public Server(Mesh mesh) {
        this.mesh = mesh;
    }

    @Override
    public void run() {

        try (ServerSocket serverSoc = new ServerSocket(PORT)) {
            while (true) {
                Socket clientSoc = serverSoc.accept();
                //Drone takes care of closing clientSoc
                Drone drone = new Drone(clientSoc, mesh);
                threadPool.submit(drone);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
