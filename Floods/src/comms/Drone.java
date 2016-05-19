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
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class Drone implements Runnable {

    //todo record battery
    //in milliseconds
    //todo add to config file
    private final long timeOut = 2000;
    public ConcurrentLinkedQueue<String> dataToSend = new ConcurrentLinkedQueue<>();
    private Socket socket;
    private String uuid;
    private Gson gson = new Gson();
    private Location location = new Location(0, 0, 0);
    private MeshServer mesh;

    public Drone(Socket clientSoc, MeshServer mesh) {
        this.socket = clientSoc;
        this.mesh = mesh;
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
                    throw new IllegalStateException("Drone timed out");
                }

                //Check if data to send from other drones
                String msgToSend = dataToSend.poll();
                if (msgToSend != null) {
                    soc.txData(msgToSend);
                }
            }
        } catch (Exception e) {
            System.err.println("Drone exception: " + e.getMessage());
        } finally {
            mesh.drones.remove(uuid);
            mesh.addServiceRequest(new ServiceRequest(uuid, location, true, null));
            System.out.println("Drone disconnected");
        }
    }

    private void parseAndSetUuid(String encodedStr) {
        //Get uuid
        JsonObject jobj = gson.fromJson(encodedStr, JsonObject.class);
        uuid = jobj.get("uuid").getAsString();
        if (mesh.drones.containsKey(uuid)) {
            throw new IllegalStateException("Drone with uuid already exists");
        } else {
            mesh.drones.put(uuid, this);
        }
    }

    private String processRxMsg(String encodedStr) throws IOException, InterruptedException, ExecutionException {

        JsonObject jobj = gson.fromJson(encodedStr, JsonObject.class);
        String type = jobj.get("type").getAsString();

        switch (type) {
            case "mesh":
                mesh.messageGlobal(this, encodedStr);
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
        setLocation(newLocation);

        ServiceResponse sr = mesh.checkForPINOR(uuid, location);

        DirectPINORMessage pj = new DirectPINORMessage(sr);

        return gson.toJson(pj);
    }


    public Location getLocation() {
        synchronized (location) {
            return location;
        }
    }

    private void setLocation(Location newLocation) {
        synchronized (location) {
            location = newLocation;
        }
    }

    public String getUuid() {
        return uuid;
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
            String jpgStr = baos.toString(String.valueOf(StandardCharsets.UTF_8));
            baos.close();

            byte[] b64 = Base64.getEncoder().encode(jpgStr.getBytes(StandardCharsets.UTF_8));
            img = new String(b64, StandardCharsets.UTF_8);

        }
    }
}