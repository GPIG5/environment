package simulation;

import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.Renderer;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Node;
import com.jme3.texture.FrameBuffer;
import com.jme3.texture.Image.Format;
import com.jme3.util.BufferUtils;

import simulation.terrain.Terrain;
import simulation.water.Pinor;
import utility.Location;
import utility.ServiceRequest;
import utility.ServiceResponse;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

//https://github.com/Suinos/heaven-rts-jmegame/blob/d67c84fe3081c1101699b50a101ee07c7588891e/src/rts/ui/MiniView.java
// https://github.com/cosmolev/ComoFlyer/blob/73a7f5963037e59a7400a64f8210d025f17608db/src/comoflyer/OffscreenComoFlyer.java

public class DroneCam {
    static final int width = 640;
    static final int height = 480;
    static final Vector3f up = new Vector3f(-1, 0, 0);
    private final ByteBuffer cpubuffer = BufferUtils.createByteBuffer(width * height * 4);
    int i = 0;
    Camera cam;
    ViewPort vp;
    Renderer rend;
    RenderManager rm;
    FrameBuffer fb;
    Node root;
    Terrain terrain;
    Vector3f pos;

    public DroneCam(RenderManager renderman, Node root, Terrain t) {
        // Setup camera
        this.root = root;
        rm = renderman;
        terrain = t;
        cam = new Camera(width, height);
        cam.setFrustumPerspective(45f, (float) width / (float) height, 1f, 1000f);
        vp = new ViewPort("Drone View", cam);
        vp.setClearFlags(true, true, true);
        vp.attachScene(root);
        fb = new FrameBuffer(width, height, 1);
        fb.setDepthBuffer(Format.Depth);
        fb.setColorBuffer(Format.ARGB8);
        rend = rm.getRenderer();
    }

    public ServiceResponse process(float tpf, ServiceRequest req, List<Pinor> pinors) {
        Location loc = req.getLocation();
        if (req != null && req.getLocation() != null) {
            // Move the camera
            try {
                System.out.println("Drone at: Lat: " + loc.getLat() + " Lon: " + loc.getLon() + " Alt: " + loc.getAlt());
                pos = terrain.osgbTo3D(loc.getOSGB(), loc.getAlt());
                cam.setLocation(pos);
                cam.lookAt(new Vector3f(pos.x, 0f, pos.z), up);
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("Location conversion failed in DroneCam");
            }
            // Render
            vp.setOutputFrameBuffer(fb);
            root.updateGeometricState();
            rm.renderViewPort(vp, tpf);
            cpubuffer.rewind();
            // Read framebuffer
            rend.readFrameBuffer(fb, cpubuffer);
            // Make BufferedImage - 3 byte BGR preferred...
            BufferedImage bi = new BufferedImage(640, 480, BufferedImage.TYPE_3BYTE_BGR);
            cpubuffer.rewind();
            byte[] data = new byte[3 * width * height];
            // buffer is X flipped and rgba.
            for (int row = 0; row < height; row++) {
                for (int col = 0; col < width; col++) {
                    byte r = cpubuffer.get();
                    byte g = cpubuffer.get();
                    byte b = cpubuffer.get();
                    // alpha
                    cpubuffer.get();
                    // BGR
                    int base = 3 * ((row * width) + width - col - 1);
                    data[base] = r;
                    data[base + 1] = g;
                    data[base + 2] = b;
                }
            }
            bi.getRaster().setDataElements(0, 0, width, height, data);
            ArrayList<Location> locs = new ArrayList<Location>();
            // Detect pinors
            for (Pinor p : pinors) {
                int state = cam.getPlaneState();
                cam.setPlaneState(0);
                if (p.isVisible(cam)) {
                    Location l = terrain.pointToLoc(p.getLocalTranslation());
                    locs.add(l);
                    System.out.println("PINOR: " + l.getLat() + " " + l.getLon() + " Alt: " + l.getAlt());
                }
                cam.setPlaneState(state);
            }
            return new ServiceResponse(req.getUuid(), locs, bi);
        }
        return null;
    }
    
    public Vector3f getPos() {
        return pos;
    }

}
