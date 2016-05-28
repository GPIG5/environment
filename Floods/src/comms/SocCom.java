package comms;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * Created by hm649 on 17/05/16.
 */
public class SocCom implements AutoCloseable {

    public final static int SIZE_BYTES = 4;

    private Socket clientSoc;
    private DataInputStream in;
    private BufferedOutputStream out;

    public SocCom(Socket clientSoc) throws Exception {
        this.clientSoc = clientSoc;
        try {
            in = new DataInputStream(clientSoc.getInputStream());
            out = new BufferedOutputStream(clientSoc.getOutputStream());
        } catch (IOException e) {
            try {
                close();
            } catch (Exception e2) {
                //if can't close resources cry deeply
            }
            throw e;
        }
    }

    public String rxData() throws IOException {
        int size = 0;
        byte[] sizeBuf = new byte[SIZE_BYTES];
        int bytesRead = in.read(sizeBuf, 0, SIZE_BYTES);

        if (bytesRead != SIZE_BYTES) {
            throw new IOException("Did not read the correct amount of size bytes");
        }

        for (int i = 0; i != SIZE_BYTES; ++i) {
            size |= (Byte.toUnsignedInt(sizeBuf[i]) << 8 * (SIZE_BYTES - i - 1));
        }

        if (size <= 0) {
            throw new IllegalStateException("message length was zero or below");
        }

        byte[] msgBuf = new byte[size];
        in.readFully(msgBuf, 0, size);
        /*if (bytesRead != size) {
            throw new IOException("Did not read the correct amount of message bytes");
        }*/

        return new String(msgBuf, StandardCharsets.UTF_8);
    }

    public void txData(String toSend) throws IOException {
        byte[] strBytes = toSend.getBytes(StandardCharsets.UTF_8);
        int size = strBytes.length;

        for (int i = 0; i != SIZE_BYTES; ++i) {
            out.write(size >>> 8 * (SIZE_BYTES - i - 1));
        }

        out.write(strBytes, 0, size);
        out.flush();
    }

    public int available() throws IOException {
        return in.available();
    }

    @Override
    public void close() throws Exception {
        if (out != null) {
            try {
                out.close();
            } catch (IOException e) {
                //who cares
            }
        }

        if (in != null) {
            try {
                in.close();
            } catch (IOException e) {
                //who cares
            }
        }

        if (clientSoc != null && !clientSoc.isClosed()) {
            try {
                clientSoc.close();
            } catch (IOException e) {
                //who cares
            }
        }
    }
}
