package simulation;

import com.jme3.app.SimpleApplication;
import com.jme3.asset.plugins.ClasspathLocator;
import com.jme3.bounding.BoundingBox;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.material.RenderState.BlendMode;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Spatial.CullHint;
import com.jme3.scene.shape.Box;
import com.jme3.system.JmeSystem;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture.WrapMode;

import java.util.List;
import java.util.Queue;

import org.lwjgl.opengl.Display;
import simulation.terrain.Terrain;
import simulation.water.Cells;
import simulation.water.Pinor;
import simulation.water.Water;
import utility.Location;
import utility.ServiceInterface;
import utility.ServiceRequest;
import utility.ServiceResponse;
import simulation.services.DroneCam;
import simulation.terrain.Terrain;
import utility.ServiceInterface;
import simulation.water.Water;

public class Simulation extends SimpleApplication {
    Water water;
    Terrain terrain;
    Geometry gter;
    DroneCam dronecamera;
    ServiceInterface si;
    Cells cells;
    Queue<ServiceRequest> requests;
    Queue<ServiceResponse> responses;
    int i = 0;

    public void start(ServiceInterface si) {
        this.si = si;
        super.start();
    }

    @Override
    public void simpleInitApp() {
        assetManager.registerLocator("assets", ClasspathLocator.class);
        System.out.print(JmeSystem.getPlatformAssetConfigURL());
        makeTerrain();
        makeWater();
        cam.setLocation(new Vector3f(0.0f, 4f, 0.0f));
        cam.setAxes(new Quaternion(0.0f, 1.0f, 0.0f, 1.0f));
        flyCam.setMoveSpeed(4.0f);
        addLights();
        dronecamera = new DroneCam(renderManager, rootNode, terrain);
        requests = si.getRequestQueue();
        responses = si.getResponseQueue();
    }

    private void makeTerrain() {
        Material mat2 = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
        mat2.setColor("Diffuse", ColorRGBA.White); // minimum material color
        mat2.setColor("Specular", ColorRGBA.LightGray); // for shininess
        mat2.setFloat("Shininess", 32f); // [1,128] for shininess
        Texture gtex = assetManager.loadTexture("Textures/yorktex.jpg");
        gtex.setWrap(WrapMode.Repeat);
        mat2.setTexture("DiffuseMap", gtex);
        terrain = new Terrain(mat2, "lidar.zip");
        rootNode.attachChild(terrain.getGeometry());
    }

    private void makeWater() {
        // Water material
        Material watermat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        watermat.setColor("Color", new ColorRGBA(0, 0, 1, 0.5f));
        watermat.getAdditionalRenderState().setBlendMode(BlendMode.Alpha);
        // Pinor material
        Material pinormat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        pinormat.setColor("Color", ColorRGBA.Red);
        // make cells.
        cells = terrain.makeCells("");

        water = new Water(cells, Display.getDrawable());
        Geometry g = new Geometry("Water", water);
        g.setMaterial(watermat);
        g.setCullHint(CullHint.Never);
        rootNode.attachChild(g);
        // Add Pinors from cells
        List<Pinor> pinors = cells.getPinors();
        for (Pinor p : pinors) {
            p.setMesh(new Box(0.01f, 0.1f, 0.01f));
            p.setModelBound(new BoundingBox());
            p.updateModelBound();
            p.setMaterial(pinormat);
            rootNode.attachChild(p);
        }
    }

    private void addLights() {
        DirectionalLight sun = new DirectionalLight();
        sun.setColor(ColorRGBA.Orange);
        sun.setDirection(new Vector3f(-.5f, -.5f, -.5f).normalizeLocal());
        rootNode.addLight(sun);
        viewPort.setBackgroundColor(ColorRGBA.LightGray);
        AmbientLight al = new AmbientLight();
        al.setColor(ColorRGBA.White.mult(0.5f));
        rootNode.addLight(al);
    }

    @Override
    public void simpleUpdate(float tpf) {
        water.process();
        super.simpleUpdate(tpf);
        ServiceRequest req = requests.poll();

        if (req != null) {
            responses.offer(dronecamera.process(tpf, req, cells.getPinors()));
        }
        i++;

        if ((i % 100) == 0) {
            requests.offer(new ServiceRequest("XYZ" + i, new Location(53.947117f, -1.128785f, 400), false));
        }
    }

}
