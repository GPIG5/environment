package comms;

import utility.Location;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

/**
 * Created by hm649 on 10/05/16.
 */
public class C2Server implements Runnable {

    private final int port;
    private final Location location;
    private final String addr;
    private MeshServer mesh;
    private ServerSocket serverSoc;


    public C2Server(MeshServer mesh) {
        this.mesh = mesh;

        String location = mesh.getProperty("c2ServerLocation");
        if (location == null) {
            System.err.println("c2 server location property not found, using default value");
            this.location = new Location(53.929472f, -1.165084f, 2);
        } else {
            location = location.replaceAll("[()]","");
            List<String> vals = Arrays.asList(location.split(","));
            this.location = new Location(Float.valueOf(vals.get(0)), Float.valueOf(vals.get(1)),
                    Float.valueOf(vals.get(2)));
        }

        String portStr = mesh.getProperty("c2ServerPort");
        if (portStr == null) {
            System.err.println("C2 server port property not found, using default value");
            this.port = 5556;
        } else {
            this.port = Integer.valueOf(portStr);
        }

        String addr = mesh.getProperty("c2ServerPOSTAddr");
        if (portStr == null) {
            System.err.println("C2 server POST address property not found, using default value");
            this.addr = "http://127.0.0.1/c2gui/send_drone_data";
        } else {
            this.addr = addr;
        }
    }

    public void txData(String toSend) throws IOException {
        URL url = new URL("http", addr, 8000, "/c2gui/send_drone_data");
        URLConnection con = url.openConnection();
        HttpURLConnection tx = (HttpURLConnection) con;

        tx.setRequestMethod("POST");
        tx.setDoOutput(true);

        byte[] strBytes = toSend.getBytes(StandardCharsets.UTF_8);
        tx.setFixedLengthStreamingMode(strBytes.length);
        tx.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        tx.connect();

        try (BufferedOutputStream out = new BufferedOutputStream(tx.getOutputStream())) {
            out.write(strBytes);
        }
    }

    @Override
    public void run() {
        try (ServerSocket serverSoc = new ServerSocket(port)) {
            this.serverSoc = serverSoc;
            while (!Thread.interrupted()) {
                try (SocCom soc = new SocCom(serverSoc.accept())) {
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
            System.err.println("C2 Server exception: " + e.getMessage());
        }
    }

    public Location getLocation() {
        return location;
    }

    public void terminate() {
        if (serverSoc != null) {
            try {
                serverSoc.close();
            } catch (IOException e) {
                //yolo
            }
        }
    }
}
