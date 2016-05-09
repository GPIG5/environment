package comms;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
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

	public Receiver() {
	}

	private void recData() {
		try (ServerSocket ss = new ServerSocket(PORT);
				Socket cs = ss.accept();
				BufferedInputStream in = new BufferedInputStream(cs.getInputStream())) {

			int size = 0;
			for (int i = 0; i != SIZE_BYTES; ++i) {
				int readByte = in.read();
				if (readByte == -1) {
					throw new Exception();
				}
				size |= readByte << 8 * (SIZE_BYTES - i - 1);

			}

			int msgLength = size;

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

	private void sendData() {

		try (ServerSocket ss = new ServerSocket(PORT);
				Socket cs = ss.accept();
				BufferedOutputStream out = new BufferedOutputStream(cs.getOutputStream())) {

			String toSend = "Hello";

			Gson gson = new Gson();

			String encodedStr = gson.toJson(toSend);

			try {
				byte[] byteArr = encodedStr.getBytes("UTF-8");
				int size = byteArr.length;

				out.write(0);
				out.write(0);
				out.write(0);
				out.write(size);

				out.write(byteArr, 0, size);

			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();

			}

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public static void main(String[] args) {

		Receiver rec = new Receiver();
		rec.sendData();

	}

}
