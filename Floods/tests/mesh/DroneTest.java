package mesh;

import com.google.gson.Gson;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import org.junit.*;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static org.junit.Assert.*;

/**
 * Created by hm649 on 11/05/16.
 */
public class DroneTest {

    private final static int PORT = 4444;
    private final static int SIZE_BYTES = Drone.SIZE_BYTES;
    private Gson gson = new Gson();
    private List<BufferedOutputStream> outStreamsToClose = new ArrayList<>();
    private List<BufferedInputStream> inStreamsToClose = new ArrayList<>();
    private List<Socket> clientSocsToClose = new ArrayList<>();
    private List<Socket> droneSocsToCheck = new ArrayList<>();
    private volatile boolean crappySignal = false;
    private ExecutorService droneThreadPool;

    private Mesh mesh;
    private int droneID;

    @Before
    public void setUp() throws Exception {
        mesh = new Mesh();
        droneID = 0;
        outStreamsToClose.clear();
        inStreamsToClose.clear();
        clientSocsToClose.clear();
        droneSocsToCheck.clear();
        droneThreadPool = Executors.newCachedThreadPool();
    }

    @After
    public void tearDown() throws Exception {

        for (BufferedOutputStream stream : outStreamsToClose) {
            stream.close();
        }

        for (BufferedInputStream stream : inStreamsToClose) {
            stream.close();
        }

        for (Socket soc : clientSocsToClose) {
            soc.close();
        }

        for (Socket soc : droneSocsToCheck) {
            assertTrue(soc.isClosed());
        }

        droneThreadPool.shutdownNow();
        assertTrue(droneThreadPool.awaitTermination(50, TimeUnit.MILLISECONDS));

        assertTrue(mesh.drones.isEmpty());
    }

    @Test
    public void droneSetupTest() throws Exception {
        DroneSockets socs1 = createSockets();
        Drone drone1 = connectDrone(socs1);
        assertTrue(drone1.getUuid().equals("4c9c12ed-947a-4fcf-8c3a-c82214234600"));
        drone1.closeResources();

        DroneSockets socs2 = createSockets();
        Drone drone2 = connectDrone(socs2);
        assertTrue(drone2.getUuid().equals("4c9c12ed-947a-4fcf-8c3a-c82214234601"));
        drone2.closeResources();


    }

    @Test
    public void droneStatusUpdateTest() throws Exception {
        DroneSockets socs = createSockets();
        Drone drone = connectDrone(socs);
        Future<?> droneF = droneThreadPool.submit(drone);

        txData( "{\"data\": {\"location\": {\"y\": 0, \"x\": 0, \"z\": 0}, \"datatype\": \"status\", " +
                "\"battery\": 1799.998011066}, \"uuid\": \"1ca1ee1e-b717-43de-9011-87df0a9d8aaf\", \"type\": " +
                "\"direct\"}", socs.out);

        rxData(socs.in);

        drone.terminate();
        droneF.get(20, TimeUnit.MILLISECONDS);
    }

    @Test
    public void locationUpdateTest() throws Exception {
        DroneSockets socs = createSockets();
        Drone drone = connectDrone(socs);
        Future<?> droneF = droneThreadPool.submit(drone);

        Vector3f newLoc = new Vector3f(50, 40, 30);
        String test = gson.toJson(newLoc);

        String toSend = "{\"type\": \"direct\", \"data\": {\"datatype\": \"status\", " +
                "\"location\": {\"x\": 50, \"y\": 40, \"z\": 30}}}";
        txData(toSend, socs.out);

        //consume data
        rxData(socs.in);
        Vector3f droneLocation = drone.getLocation();

        assertTrue(droneLocation.equals(newLoc));
        drone.terminate();
        droneF.get(20, TimeUnit.MILLISECONDS);
    }

    @Test
    public void meshMessageTest() throws Exception {
        DroneSockets socs = createSockets();
        Drone drone = connectDrone(socs);
        Future<?> droneF = droneThreadPool.submit(drone);

        txData( "{\"data\": {\"location\": {\"y\": 0, \"x\": 0, \"z\": 0}, \"datatype\": \"status\", " +
                "\"battery\": 1799.998011066}, \"uuid\": \"1ca1ee1e-b717-43de-9011-87df0a9d8aaf\", \"type\": " +
                "\"mesh\"}", socs.out);

        drone.terminate();
        droneF.get(20, TimeUnit.MILLISECONDS);

    }

    private Drone connectDrone(DroneSockets socs) throws Exception {
        String connectMsg = "{\"type\": \"direct\", \"uuid\": \"4c9c12ed-947a-4fcf-8c3a-c8221423460" + droneID++ +
                "\", \"data\": {}}";

        txData(connectMsg, socs.out);
        Drone drone = new Drone(socs.drone, mesh);


        return drone;
    }


    private DroneSockets createSockets() throws Exception {
        crappySignal = false;
        Future<Socket> futureSoc = Executors.newSingleThreadExecutor().submit(new DummyServer());
        while (!crappySignal);
        Socket clientSoc = new Socket("127.0.0.1", PORT);
        BufferedOutputStream out = new BufferedOutputStream(clientSoc.getOutputStream());
        BufferedInputStream in = new BufferedInputStream(clientSoc.getInputStream());
        clientSocsToClose.add(clientSoc);
        outStreamsToClose.add(out);
        inStreamsToClose.add(in);

        Socket droneSoc = futureSoc.get();
        droneSocsToCheck.add(droneSoc);
        return new DroneSockets(droneSoc, clientSoc, in, out);
    }

    private String rxData(BufferedInputStream in) throws Exception {
        int size = 0;
        byte[] sizeBuf = new byte[SIZE_BYTES];
        in.read(sizeBuf, 0, SIZE_BYTES);

        for (int i = 0; i != SIZE_BYTES; ++i) {
            size |= Byte.toUnsignedInt(sizeBuf[i]) << 8 * (SIZE_BYTES - i - 1);
        }

        byte[] msgBuf = new byte[size];
        in.read(msgBuf, 0, size);

        return new String(msgBuf, "UTF-8");
    }

    private void txData(String toSend, BufferedOutputStream out) throws Exception {
        byte[] strBytes = toSend.getBytes("UTF-8");
        int size = strBytes.length;

        for (int i = 0; i != SIZE_BYTES; ++i) {
            out.write(size >> 8 * (SIZE_BYTES - i - 1));
        }

        out.write(strBytes, 0, size);
        out.flush();
    }

    private class DroneSockets {

        Socket drone;
        Socket client;
        BufferedInputStream in;
        BufferedOutputStream out;

        private DroneSockets(Socket drone, Socket client, BufferedInputStream in, BufferedOutputStream out)
                throws Exception {
            this.drone = drone;
            this.client = client;
            this.in = in;
            this.out = out;
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