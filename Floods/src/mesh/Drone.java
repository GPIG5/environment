package mesh;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import com.google.gson.*;
import com.jme3.math.Vector3f;

public class Drone implements Runnable {

    
    public BlockingQueue<String> dataToSend = new ArrayBlockingQueue<>(20);

    public  final static int SIZE_BYTES = 4;
    private final String uuid;

    private Socket clientSoc;
    private BufferedInputStream in;
    private BufferedOutputStream out;
    private Gson gson = new Gson();
    private Location location = new Location(0, 0, 0);
    private Mesh mesh;
    private volatile boolean terminate = false;

    public Drone(Socket clientSoc, Mesh mesh) throws IOException {
        this.clientSoc = clientSoc;
        this.mesh = mesh;
        try {
            in = new BufferedInputStream(clientSoc.getInputStream());
            out = new BufferedOutputStream(clientSoc.getOutputStream());
            String encodedStr = rxData();
            JsonObject jobj = gson.fromJson(encodedStr, JsonObject.class);
            uuid = jobj.get("uuid").getAsString();
            //as object is not locked on reads another drone may put the same UUID in just after this check
            //but what are the chances...
            if (mesh.drones.containsKey(uuid)) {
                throw new IllegalStateException("Drone with uuid already exists");
            } else {
                mesh.drones.put(uuid, this);
            }
        } catch (Exception e) {
            e.printStackTrace();
            closeResources();
            //rethrow back to whoever called
            throw e;
        }
    }

    @Override
    public void run() {
        try {
            //Main loop
            while (!terminate) {
                //Check if data to receive from actual drone
                if (in.available() > 0) {
                    String encodedStr = rxData();
                    processRxMsg(encodedStr, out);
                }

                //Check if data to send from other drones
                while (!dataToSend.isEmpty()) {
                    String msgToSend = dataToSend.take();
                    txData(out, msgToSend);
                }

            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            mesh.drones.remove(uuid);
            closeResources();
            System.out.println("Drone disconnected");
        }
    }

    private void processRxMsg(String encodedStr, BufferedOutputStream out) throws IOException {

        JsonObject jobj = gson.fromJson(encodedStr, JsonObject.class);
        String type = jobj.get("type").getAsString();

        switch (type) {
            case "mesh":
                mesh.messageGlobal(this, encodedStr);
                return;
            case "direct":
                String dataType = jobj.getAsJsonObject("data").get("datatype").getAsString();
                switch (dataType) {
                    case "status":
                        String toSend = processStatusMsg(encodedStr);
                        txData(out, toSend);
                        return;
                    default:
                        throw new IOException("Received unspecified datatype in JSON " + dataType);
                }
            default:
                throw new IOException("Received unspecified type in JSON " + type);
        }
    }

    private String processStatusMsg(String encodedStr) {
        JsonObject jobj = gson.fromJson(encodedStr, JsonObject.class);
        JsonElement locationJE = jobj.getAsJsonObject("data").get("location");
        Location newLocation = gson.fromJson(locationJE, Location.class);
        setLocation(newLocation);

        List<Location> PINORLocs = mesh.checkForPINOR(location);

        DirectPINORMessage pj = new DirectPINORMessage(PINORLocs);

        return gson.toJson(pj);
    }

    private String rxData() throws IOException {
        int size = 0;
        byte[] sizeBuf = new byte[SIZE_BYTES];
        int bytesRead = in.read(sizeBuf, 0, SIZE_BYTES);

        if (bytesRead != SIZE_BYTES) {
            throw new IOException("Did not read the correct amount of size bytes");
        }
        for (int i = 0; i != SIZE_BYTES; ++i) {
            size |= ( Byte.toUnsignedInt(sizeBuf[i]) << 8 * (SIZE_BYTES - i - 1));
        }

        if (size <= 0) {
            throw new IllegalStateException("message length was zero or below");
        }

        byte[] msgBuf = new byte[size];
        bytesRead = in.read(msgBuf, 0, size);
        if (bytesRead != size) {
            throw new IOException("Did not read the correct amount of message bytes");
        }

        return new String(msgBuf, "UTF-8");
    }

    private void txData(BufferedOutputStream out, String toSend) throws IOException {
        byte[] strBytes = toSend.getBytes("UTF-8");
        int size = strBytes.length;

        for (int i = 0; i != SIZE_BYTES; ++i) {
            out.write(size >>> 8 * (SIZE_BYTES - i - 1));
        }

        out.write(strBytes, 0, size);
        out.flush();
    }

    private void setLocation(Location newLocation) {
        synchronized (location) {
            //stupid 3D map
            location = newLocation;
        }
    }

    public Location getLocation() {
        synchronized (location) {
            return location;
        }
    }

    void closeResources() {
        //The internet tells me the order is important
        if (out != null) {
            try {
                out.close();
            } catch (IOException e) {
                System.err.println("Could not close output stream");
                e.printStackTrace();
            }
        }

        if (in != null) {
            try {
                in.close();
            } catch (IOException e) {
                System.err.println("Could not close input stream");
                e.printStackTrace();
            }
        }

        if (clientSoc != null && !clientSoc.isClosed()) {
            try {
                clientSoc.close();
            } catch (IOException e) {
                System.err.println("Could not close client socket");
                e.printStackTrace();
            }
        }
    }

    public void terminate() {
        terminate = true;
    }

    public String getUuid() {
        return uuid;
    }

    private class DirectPINORMessage {
        final String uuid = getUuid();
        final String type = "direct";
        final PINORData data;

        private DirectPINORMessage(List<Location> PINORLocs) {
            data = new PINORData(PINORLocs);
        }

    }

    private class PINORData {
        final String datatype = "pinor";
        final List<Location> PINOR;

        private PINORData(List<Location> PINORLocs) {
            PINOR = PINORLocs;
        }
    }

}