package comms;

import utility.Location;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.concurrent.*;

/**
 * Created by hm649 on 10/05/16.
 */
public class C2Server implements Runnable {

    private final int PORT = 5556;
    private final Location location;
    private volatile boolean terminate = false;
    private ServerSocket serverSoc;
    private MeshServer mesh;


    public C2Server(MeshServer mesh, Location location) {
        this.mesh = mesh;
        this.location = location;
    }

    public void txData(String toSend) {
        ;
    }

    @Override
    public void run() {
        try {
            serverSoc = new ServerSocket(PORT);
            while (!terminate) {
                try (SocCom soc = new SocCom(serverSoc.accept())){
                    System.out.println("C2 connected");
                    String encodedStr = soc.rxData();
                    mesh.messageGlobal(null, encodedStr);
                } catch (Exception e) {
                    System.err.println(e.getMessage());
                } finally {
                    System.out.println("C2 disconnected");
                }
            }
        } catch (Exception e) {
            //We care about the server socket dying
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

    public Location getLocation() {
        return location;
    }

    public static void main(String[] args) {
        MeshServer mesh = new MeshServer();
        C2Server c2 = new C2Server(mesh, new Location(0,0,0));
        Executors.newSingleThreadExecutor().submit(c2);

    }
}
