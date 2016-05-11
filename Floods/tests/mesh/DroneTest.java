package mesh;

import org.junit.*;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Created by hm649 on 11/05/16.
 */
public class DroneTest {

    private final int PORT = 4444;
    private Socket clientSoc;
    private Socket droneSoc;
    private Mesh mesh;

    @Before
    public void setUp() throws Exception {
        Future<Socket> futureSoc = Executors.newSingleThreadExecutor().submit(new DummyServer());
        clientSoc = new Socket("127.0.0.1", PORT);
        droneSoc = futureSoc.get();
        mesh = new Mesh();
    }

    @After
    public void tearDown() throws Exception {
        if (clientSoc != null) {
            clientSoc.close();
        }

        if (droneSoc != null) {
            droneSoc.close();
        }
    }

    @Test
    public void run() throws Exception {
        Drone drone = new Drone(droneSoc, mesh);



    }

    private class DummyServer implements Callable<Socket> {

        @Override
        public Socket call() throws Exception {
            try (ServerSocket serverSoc = new ServerSocket(PORT)) {
                return serverSoc.accept();
            }
        }
    }

}