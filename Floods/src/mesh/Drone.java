package mesh;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import com.google.gson.*;

public class Drone implements Runnable {

	private final int sizeBytes = 4;
    private final Integer id;
    private final int queueSize = 30;

    private Socket clientSoc;
    private Gson gson = new Gson();
    private Location location = new Location(0, 0, 0);
    private Mesh mesh;

    public BlockingQueue<String> dataToSend = new ArrayBlockingQueue<>(queueSize);

	public Drone(Socket clientSoc, Mesh mesh) throws IOException {
        this.clientSoc = clientSoc;
        this.mesh = mesh;
        try (BufferedInputStream in = new BufferedInputStream(clientSoc.getInputStream())) {
            String encodedStr = rxData(in);
            id = gson.fromJson(encodedStr, Integer.class);
            mesh.drones.put(id, this);
        }
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

    public Location getLocation() {
        synchronized (location) {
            return location;
        }
    }

    public void setLocation(double x, double y, double z) {
        synchronized (location) {
            location.setX(x);
            location.setY(y);
            location.setZ(z);
        }
    }

	@Override
	public void run() {
        try (BufferedInputStream in = new BufferedInputStream(clientSoc.getInputStream());
             BufferedOutputStream out = new BufferedOutputStream(clientSoc.getOutputStream())) {
            //Main loop
            while (true) {
                //Check if data to receive from actual drone
                if (in.available() > 0) {
                    String encodedStr = rxData(in);
                    //do something with data
                }

                //Check if data to send from other drones
                while (!dataToSend.isEmpty()) {
                    String msgToSend = dataToSend.take();
                    txData(out, msgToSend);
                }

            }
        } catch (InterruptedException | IOException e) {
            e.printStackTrace();
        } finally {
            mesh.drones.remove(id);
            try {
                clientSoc.close();
            } catch (IOException e) {
                System.err.println("Could not close client socket");
                e.printStackTrace();
            }
        }
	}
}
