package simulation.services;

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

import org.opengis.geometry.MismatchedDimensionException;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.operation.TransformException;

//https://github.com/Suinos/heaven-rts-jmegame/blob/d67c84fe3081c1101699b50a101ee07c7588891e/src/rts/ui/MiniView.java
// https://github.com/cosmolev/ComoFlyer/blob/73a7f5963037e59a7400a64f8210d025f17608db/src/comoflyer/OffscreenComoFlyer.java

public class DroneCam {
    static final int width = 640;
    static final int height = 480;
    private final ByteBuffer cpubuffer = BufferUtils.createByteBuffer(width * height * 4);
    int i = 0;
    Camera cam;
    ViewPort vp;
    Renderer rend;
    RenderManager rm;
    FrameBuffer fb;
    Node root;
    Terrain terrain;

    public DroneCam(RenderManager renderman, Node root, Terrain t) {
        // Setup camera
        this.root = root;
        rm = renderman;
        terrain = t;
        cam = new Camera(width, height);
        cam.setFrustumPerspective(45f, (float) width / (float) height, 1f, 1000f);
        cam.setLocation(new Vector3f(0f, 20f, 0f));
        cam.lookAt(new Vector3f(0f, 0f, 0f), Vector3f.UNIT_Y);
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
                Vector3f pos = terrain.osgbTo3D(loc.getOSGB(), loc.getAlt());
                cam.setLocation(pos);
                cam.lookAt(new Vector3f(pos.x, 0f, pos.z), Vector3f.UNIT_Y);
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
            // Make BufferedImage
            BufferedImage bi = new BufferedImage(640, 480, BufferedImage.TYPE_INT_ARGB);
            cpubuffer.rewind();
            int[] data = new int[width * height];
            for (int i = 0; i < data.length; i++) {
                // Note: Framebuffer is rgba
                int r = 0xFF & cpubuffer.get();
                int g = 0xFF & cpubuffer.get();
                int b = 0xFF & cpubuffer.get();
                int a = 0xFF & cpubuffer.get();
                data[i] = (a << 24) | (r << 16) | (g << 8) | b;
            }
            bi.getRaster().setDataElements(0, 0, width, height, data);
            try {
                ImageIO.write(bi, "png", new File("/tmp/output.png"));
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            ArrayList<Location> locs = new ArrayList<Location>(); 
            // Look over the Pinors
            System.out.println(pinors.size() + " pinors total");
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

}
