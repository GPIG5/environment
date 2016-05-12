package mesh;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.security.spec.ECField;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import com.google.gson.*;
import com.jme3.math.Vector3f;

public class Drone implements Runnable {

    //TODO add watchdog timer 2000

    public BlockingQueue<String> dataToSend = new ArrayBlockingQueue<>(20);

    public  final static int SIZE_BYTES = 4;
    private final String uuid;

    private Socket clientSoc;
    private BufferedInputStream in;
    private BufferedOutputStream out;
    private Gson gson = new Gson();
    private Vector3f location = new Vector3f(0, 0, 0);
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
            mesh.drones.put(uuid, this);
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
        Vector3f newLocation = gson.fromJson(locationJE, Vector3f.class);
        setLocation(newLocation);

        List<Vector3f> PINORLocs = mesh.checkForPINOR(location);

        PINORJSON pj = new PINORJSON(PINORLocs);

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
            size |= ((int) (0xFF & sizeBuf[i]) << 8 * (SIZE_BYTES - i - 1));
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

        if (size > (2 ^ (8 * SIZE_BYTES))) {
            throw new IllegalStateException("Tried to encode message with size larger than SIZE_BYTES");
        }

        for (int i = 0; i != SIZE_BYTES; ++i) {
            out.write(size >>> 8 * (SIZE_BYTES - i - 1));
        }
        out.write(strBytes, 0, size);
        out.flush();
    }

    private void setLocation(Vector3f newLocation) {
        synchronized (location) {
            //stupid 3D map
            location.set(newLocation);
        }
    }

    public Vector3f getLocation() {
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

    private class PINORJSON {
        final String datatype = "pinor";
        final List<Vector3f> PINOR;

        private PINORJSON(List<Vector3f> PINORLocs) {
            PINOR = PINORLocs;
        }

    }

}
