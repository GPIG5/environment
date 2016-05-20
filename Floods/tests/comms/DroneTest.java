package comms;

import com.google.gson.Gson;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import utility.Location;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static org.junit.Assert.assertTrue;

/**
 * Created by hm649 on 11/05/16.
 */
public class DroneTest {

    private final static int PORT = 4444;
    private Gson gson = new Gson();
    private List<SocCom> clientSocsToClose = new ArrayList<>();
    private List<Socket> droneSocsToCheck = new ArrayList<>();
    private volatile boolean crappySignal = false;
    private ExecutorService droneThreadPool;

    private MeshServer mesh;
    private int droneID;

    @Before
    public void setUp() throws Exception {
        mesh = new MeshServer();
        droneID = 0;
        clientSocsToClose.clear();
        droneSocsToCheck.clear();
        droneThreadPool = Executors.newCachedThreadPool();
    }

    @After
    public void tearDown() throws Exception {

        droneThreadPool.shutdownNow();
        assertTrue(droneThreadPool.awaitTermination(50, TimeUnit.MILLISECONDS));

        assertTrue(mesh.drones.isEmpty());

        for (SocCom soc : clientSocsToClose) {
            soc.close();
        }

        for (Socket soc : droneSocsToCheck) {
            assertTrue(soc.isClosed());
        }
    }

    @Test
    public void droneSetupTest() throws Exception {
        DroneSockets socs1 = createSockets();
        Drone drone1 = connectDrone(socs1);
        Future<?> droneF = droneThreadPool.submit(drone1);
        while (mesh.drones.isEmpty()) ;
        assertTrue(drone1.getUuid().equals("4c9c12ed-947a-4fcf-8c3a-c82214234600"));

        DroneSockets socs2 = createSockets();
        Drone drone2 = connectDrone(socs2);
        Future<?> droneF2 = droneThreadPool.submit(drone2);
        while (mesh.drones.size() == 1) ;
        assertTrue(drone2.getUuid().equals("4c9c12ed-947a-4fcf-8c3a-c82214234601"));

        droneF.cancel(true);
        droneF2.cancel(true);
    }

    @Test
    public void droneStatusUpdateTest() throws Exception {
        //this test won't work if the C2 server does not exist
        DroneSockets socs = createSockets();
        Drone drone = connectDrone(socs);
        Future<?> droneF = droneThreadPool.submit(drone);

        socs.client.txData("{\"data\": {\"location\": {\"lat\": 0, \"lon\": 0, \"alt\": 0}, \"datatype\": \"status\", " +
                "\"battery\": 1799.998011066}, \"uuid\": \"1ca1ee1e-b717-43de-9011-87df0a9d8aaf\", \"type\": " +
                "\"direct\"}");

        socs.client.rxData();

        droneF.cancel(true);
    }

    @Test
    public void locationUpdateTest() throws Exception {
        DroneSockets socs = createSockets();
        Drone drone = connectDrone(socs);
        Future<?> droneF = droneThreadPool.submit(drone);

        Location newLoc = new Location(50, 30, 50);
        String test = gson.toJson(newLoc);

        String toSend = "{\"type\": \"direct\", \"data\": {\"datatype\": \"status\", " +
                "\"location\": {\"lat\": 50, \"lon\": 30, \"alt\": 50}}}";
        socs.client.txData(toSend);

        //consume data
        socs.client.rxData();
        Location droneLocation = drone.getLocation();

        assertTrue(droneLocation.equals(newLoc));
        droneF.cancel(true);
    }

    @Test
    public void meshMessageTest() throws Exception {
        DroneSockets socs = createSockets();
        Drone drone = connectDrone(socs);
        Future<?> droneF = droneThreadPool.submit(drone);

        socs.client.txData("{\"data\": {\"location\": {\"lat\": 0, \"lon\": 0, \"alt\": 0}, \"datatype\": \"status\", " +
                "\"battery\": 1799.998011066}, \"uuid\": \"1ca1ee1e-b717-43de-9011-87df0a9d8aaf\", \"type\": " +
                "\"mesh\"}");

        Thread.sleep(100);
        droneF.cancel(true);
    }

    private Drone connectDrone(DroneSockets socs) throws Exception {
        String connectMsg = "{\"type\": \"direct\", \"uuid\": \"4c9c12ed-947a-4fcf-8c3a-c8221423460" + droneID++ +
                "\", \"data\": {}}";



        socs.client.txData(connectMsg);
        Drone drone = new Drone(socs.drone, mesh, 2000);

        return drone;
    }


    private DroneSockets createSockets() throws Exception {
        crappySignal = false;
        Future<Socket> futureSoc = Executors.newSingleThreadExecutor().submit(new DummyServer());
        while (!crappySignal) ;
        Socket clientSoc = new Socket("127.0.0.1", PORT);
        SocCom socs = new SocCom(clientSoc);
        clientSocsToClose.add(socs);
        Socket droneSoc = futureSoc.get();
        droneSocsToCheck.add(droneSoc);
        return new DroneSockets(droneSoc, socs);
    }

    private class DroneSockets {

        Socket drone;
        SocCom client;

        private DroneSockets(Socket drone, SocCom client) {
            this.drone = drone;
            this.client = client;
        }
    }

    private class DummyServer implements Callable<Socket> {

        @Override
        public Socket call() throws Exception {
            try (ServerSocket serverSoc = new ServerSocket(PORT)) {
                crappySignal = true;
                return serverSoc.accept();
            }
        }
    }

}