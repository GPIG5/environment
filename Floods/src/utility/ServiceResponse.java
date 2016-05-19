package utility;

import java.awt.image.BufferedImage;
import java.util.List;

public class ServiceResponse extends Message {
    List<Location> pinors;
    BufferedImage image;

    public ServiceResponse(String uuid, List<Location> pinors, BufferedImage image) {
        super(uuid);
        this.pinors = pinors;
        this.image = image;
    }

    public List<Location> getPinors() {
        return pinors;
    }

    public BufferedImage getImage() {
        return image;
    }
}
