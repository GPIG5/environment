package mesh;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.Socket;

import com.google.gson.*;

public class Drone implements Runnable {


    //TODO drone add remove itself to mesh list
    //TODO drone keeps location
    //TODO drone somehow checks if data to send
	private final int sizeBytes = 4;
    private final int id;

    private Socket clientSoc;
    private Gson gson = new Gson();
    private int location = 0;

	public Drone(Socket clientSoc, Mesh mesh) throws IOException {
        this.clientSoc = clientSoc;
        String encodedStr = rxData();
        id = gson.fromJson(encodedStr, Integer.class);
	}

	private String rxData() throws IOException {
		try (BufferedInputStream in = new BufferedInputStream(clientSoc.getInputStream())) {
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
	}

	private void txData() throws IOException {

		try (BufferedOutputStream out = new BufferedOutputStream(clientSoc.getOutputStream())) {
            String toSend = "Hello";
            String encodedStr = gson.toJson(toSend);

            byte[] strBytes = encodedStr.getBytes("UTF-8");
            int size = strBytes.length;

            if (size > (2 ^ (8 * sizeBytes))) {
                throw new IllegalStateException("Tried to encode message with size larger than sizeBytes");
            }

            for (int i = 0; i != sizeBytes; ++i) {
                out.write(size >>> 8 * (sizeBytes - i - 1));
            }
            out.write(strBytes, 0, size);
		}
    }

	@Override
	public void run() {
        try {
            while (true) {
                String encodedStr = rxData();
                //do something with data
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                clientSoc.close();
            } catch (IOException e) {
                System.err.println("Could not close client socket");
                e.printStackTrace();
            }
        }
	}
}
