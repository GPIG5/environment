package utility;

import java.awt.image.BufferedImage;
import java.util.ArrayList;

import mesh.PINOR;

public class ServiceResponse extends Message {
	ArrayList<PINOR> pinors;
	BufferedImage image;
	
	public ServiceResponse(String uuid, ArrayList<PINOR> pinors, BufferedImage image) {
		super(uuid);
		this.pinors = pinors;
		this.image = image;
	}
	
	public ArrayList<PINOR> getPinors() {
		return pinors;
	}
	
	public BufferedImage getImage() {
		return image;
	}
}
