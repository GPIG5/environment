package mesh;

import java.awt.*;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.*;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import com.google.gson.*;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;

public class Drone implements Runnable {

    //TODO add watchdog timer

    public BlockingQueue<String> dataToSend = new ArrayBlockingQueue<>(20);

	private final int sizeBytes = 4;
    private final Integer uuid;

    private Socket clientSoc;
    private Gson gson = new Gson();
    private Vector3f location = new Vector3f(0, 20, 0);
    private Mesh mesh;
    private volatile boolean terminate = false;

	public Drone(Socket clientSoc, Mesh mesh) throws IOException {
        this.clientSoc = clientSoc;
        this.mesh = mesh;
        try (BufferedInputStream in = new BufferedInputStream(clientSoc.getInputStream())) {
            String encodedStr = rxData(in);
            JsonObject jobj = gson.fromJson(encodedStr, JsonObject.class);
            uuid = jobj.get("uuid").getAsInt();
            mesh.drones.put(uuid, this);
        } finally {
            try {
                clientSoc.close();
            } catch (IOException e) {
                System.err.println("Could not close client socket");
                e.printStackTrace();
            }
        }
	}

    @Override
    public void run() {
        try (BufferedInputStream in = new BufferedInputStream(clientSoc.getInputStream());
             BufferedOutputStream out = new BufferedOutputStream(clientSoc.getOutputStream())) {
            //Main loop
            while (!terminate) {
                //Check if data to receive from actual drone
                if (in.available() > 0) {
                    String encodedStr = rxData(in);
                    processRxMsg(encodedStr, out);
                }

                //Check if data to send from other drones
                while (!dataToSend.isEmpty()) {
                    String msgToSend = dataToSend.take();
                    txData(out, msgToSend);
                }

            }
        } catch (InterruptedException | IOException | IllegalStateException e) {
            e.printStackTrace();
        } finally {
            mesh.drones.remove(uuid);
            try {
                clientSoc.close();
            } catch (IOException e) {
                System.err.println("Could not close client socket");
                e.printStackTrace();
            }
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
                jobj = gson.fromJson(encodedStr, JsonObject.class);
                String dataType = jobj.get("datatype").getAsString();
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
        JsonElement locationJE = jobj.get("location");
        Vector2f newLocation = gson.fromJson(locationJE, Vector2f.class);
        setLocation(newLocation);

        List<Vector2f> PINORLocs = mesh.checkForPINOR(location);

        PINORJSON pj = new PINORJSON(PINORLocs);

        return gson.toJson(pj);
    }

	private String rxData(BufferedInputStream in) throws IOException {
        int size = 0;
        byte[] sizeBuf = new byte[sizeBytes];
        int bytesRead = in.read(sizeBuf, 0, sizeBytes);
        if (bytesRead != sizeBytes) {
            throw new IOException("Did not read the correct amount of size bytes");
        }
        for (int i = 0; i != sizeBytes; ++i) {
            size |= sizeBuf[i] << 8 * (sizeBytes - i - 1);
        }

        byte[] msgBuf = new byte[size];
        bytesRead = in.read(msgBuf, 0, size);
        if (bytesRead != sizeBytes) {
            throw new IOException("Did not read the correct amount of message bytes");
        }

        return new String(msgBuf, "UTF-8");
	}

	private void txData(BufferedOutputStream out, String toSend) throws IOException {
        byte[] strBytes = toSend.getBytes("UTF-8");
        int size = strBytes.length;

        if (size > (2 ^ (8 * sizeBytes))) {
            throw new IllegalStateException("Tried to encode message with size larger than sizeBytes");
        }

        for (int i = 0; i != sizeBytes; ++i) {
            out.write(size >>> 8 * (sizeBytes - i - 1));
        }
        out.write(strBytes, 0, size);
    }

    private void setLocation(Vector2f newLocation) {
        synchronized (location) {
            //stupid 3D map
            location.setZ(newLocation.getX());
            location.setX(newLocation.getY());
        }
    }

    public Vector3f getLocation() {
        synchronized (location) {
            return location;
        }
    }

    public void terminate() {
        terminate = true;
    }

    public int getUuid() {
        return uuid;
    }

    private class PINORJSON {
        final String datatype = "pinor";
        final List<Vector2f> PINOR;

        private PINORJSON(List<Vector2f> PINORLocs) {
            PINOR = PINORLocs;
        }

    }

}
