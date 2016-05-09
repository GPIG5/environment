package comms;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.*;

public class Receiver {

	private final int PORT = 5555;
	private final int SIZE_BYTES = 4;
	
	public Receiver() {}

	private void recData() {
		try (ServerSocket ss = new ServerSocket(PORT);
				Socket cs = ss.accept();
				BufferedInputStream in = new BufferedInputStream(cs.getInputStream())) {

			ByteBuffer br = ByteBuffer.allocateDirect(SIZE_BYTES);
			br.order(ByteOrder.BIG_ENDIAN);
			
			for (int i = 0; i != SIZE_BYTES; ++i) {
				int readByte = in.read();
				if (readByte == -1) {
					throw new Exception();
				}
				br.put((byte) readByte);
				
			}
			
			
			int msgLength = br.getInt();
			
			byte[] byteBuf = new byte[msgLength];
			
			int bytesRead = in.read(byteBuf, 0, msgLength);
			
			if (bytesRead != msgLength) {
				throw new Exception();
			}
			
			Gson gson = new Gson();
			
			String decodedStr = gson.fromJson(new String(byteBuf, "UTF-8"), String.class);
			
			System.out.println(decodedStr);
			

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {


		Receiver rec = new Receiver();
		rec.recData();

	}

}
