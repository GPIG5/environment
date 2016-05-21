package comms;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import utility.Location;
import utility.ServiceRequest;
import utility.ServiceResponse;

import javax.imageio.ImageIO;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class Drone implements Runnable {

    //in milliseconds
    private final long timeOut;

    private ConcurrentLinkedQueue<String> dataToSend = new ConcurrentLinkedQueue<>();
    private Socket socket;
    private volatile String uuid;
    private Gson gson = new Gson();
    private Location location = new Location(0, 0, 0);
    private MeshServer mesh;
    private volatile int battery = 0;
    private volatile boolean killComms = false;

    public Drone(Socket clientSoc, MeshServer mesh, long timeOut) {
        this.socket = clientSoc;
        this.mesh = mesh;
        this.timeOut = timeOut;
    }

    @Override
    public void run() {
        try (SocCom soc = new SocCom(socket)) {
            String encodedStr = soc.rxData();
            parseAndSetUuid(encodedStr);
            long lastRx = System.nanoTime();
            //Main loop
            while (!Thread.interrupted()) {
                //Check if data to receive from actual drone
                if (soc.available() > 0) {
                    lastRx = System.nanoTime();
                    encodedStr = soc.rxData();
                    String toSend = processRxMsg(encodedStr);
                    if (toSend != null) {
                        soc.txData(toSend);
                    }
                } else if ((lastRx + TimeUnit.MILLISECONDS.toNanos(timeOut)) <= System.nanoTime()) {
                	System.out.println("Outta time");
                    throw new IllegalStateException("Drone timed out");
                }

                //Check if data to send from other drones
                String msgToSend = dataToSend.poll();
                if (msgToSend != null) {
                    soc.txData(msgToSend);
                }
            }
        } catch (Exception e) {
        	e.printStackTrace();
        	System.out.println(e.getMessage());
        } finally {
        	mesh.removeDrone(uuid);
            System.out.println("Drone disconnected");
        }
    }

    private void parseAndSetUuid(String encodedStr) {
        // Get uuid.
        JsonObject jobj = gson.fromJson(encodedStr, JsonObject.class);
        uuid = jobj.get("uuid").getAsString();
        System.out.println("UUID: " + uuid);
        // Try and add this drone.
        if (!mesh.addDrone(uuid, this)) {
            System.out.println("Drone with uuid "+ uuid + " already exists!");
            throw new IllegalStateException("Drone with uuid already exists");
        }
    }

    private String processRxMsg(String encodedStr) throws IOException, InterruptedException, ExecutionException {

        JsonObject jobj = gson.fromJson(encodedStr, JsonObject.class);
        String type = jobj.get("type").getAsString();

        switch (type) {
            case "mesh":
                if (!killComms) {
                    mesh.messageGlobal(this, encodedStr);
                }
                return null;
            case "direct":
                String dataType = jobj.getAsJsonObject("data").get("datatype").getAsString();
                switch (dataType) {
                    case "status":
                        String toSend = processStatusMsg(encodedStr);
                        return toSend;
                    default:
                        throw new IOException("Received unspecified datatype in JSON " + dataType);
                }
            default:
                throw new IOException("Received unspecified type in JSON " + type);
        }
    }

    private String processStatusMsg(String encodedStr) throws IOException, ExecutionException, InterruptedException {
        JsonObject jobj = gson.fromJson(encodedStr, JsonObject.class);
        JsonElement locationJE = jobj.getAsJsonObject("data").get("location");
        Location newLocation = gson.fromJson(locationJE, Location.class);
        location = newLocation;

        if (jobj.has("battery")) {
        	battery = jobj.getAsJsonObject("battery").getAsInt();
        }
        
        ServiceResponse sr = mesh.checkForPINOR(uuid, location);

        DirectPINORMessage pj = new DirectPINORMessage(sr);

        return gson.toJson(pj);
    }


    public Location getLocation() {
            return location;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public void setBattery(int battery) {
        this.battery = battery;
    }

    public int getBattery() {
        return battery;
    }

    public void setKillComms(boolean value) {
        killComms = value;
    }

    public void addMsgToSend(String msg) {
        if (!killComms) {
            dataToSend.add(msg);
        }
    }

    private class DirectPINORMessage {
        final String uuid = getUuid();
        final String type = "direct";
        final PINORData data;

        private DirectPINORMessage(ServiceResponse data) throws IOException {
            this.data = new PINORData(data);
        }

    }

    private class PINORData {
        final String datatype = "pinor";
        final List<Location> pinor;
        final String img;

        private PINORData(ServiceResponse data) throws IOException {
            pinor = data.getPinors();

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(data.getImage(), "jpg", baos);
            img = Base64.getEncoder().encodeToString(baos.toByteArray());
            baos.close();
        }
    }
}