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
import com.jme3.scene.Spatial;
import com.jme3.scene.Spatial.CullHint;
import com.jme3.scene.shape.Box;
import com.jme3.system.JmeSystem;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture.WrapMode;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.List;
import java.util.Queue;

import org.lwjgl.opengl.Display;
import simulation.terrain.Terrain;
import simulation.water.Cells;
import simulation.water.Pinor;
import simulation.water.Water;
import utility.ServiceInterface;
import utility.ServiceRequest;
import utility.ServiceResponse;
import simulation.services.DroneCam;

public class Simulation extends SimpleApplication {
    Water water;
    Terrain terrain;
    Geometry gter;
    DroneCam droneCamera;
    ServiceInterface si;
    Cells cells;
    Queue<ServiceRequest> requests;
    AbstractMap<String,Spatial> drones;
    Spatial drone;
    Material droneMat;

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
        droneCamera = new DroneCam(renderManager, rootNode, terrain);
        requests = si.getRequestQueue();
        makeDrone();
    }
    
    private void makeDrone() {
        // Load drone
        Spatial drone = assetManager.loadModel("drone.obj");
        drone.scale(0.2f);
        Material droneMat = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
        droneMat.setBoolean("UseMaterialColors", true);
        droneMat.setColor("Diffuse", new ColorRGBA(0.2f, 0.2f, 0.2f, 0.0f)); // minimum material color
        droneMat.setColor("Specular", ColorRGBA.Gray); // for shininess
        droneMat.setFloat("Shininess", 32f); // [1,128] for shininess
        drone.setMaterial(droneMat);
        drones = new HashMap<String, Spatial>();
    }

    private void makeTerrain() {
        Material terMat = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
        terMat.setColor("Diffuse", ColorRGBA.White); // minimum material color
        terMat.setColor("Specular", ColorRGBA.LightGray); // for shininess
        terMat.setFloat("Shininess", 32f); // [1,128] for shininess
        Texture gtex = assetManager.loadTexture("Textures/yorktex.jpg");
        gtex.setWrap(WrapMode.Repeat);
        terMat.setTexture("DiffuseMap", gtex);
        terrain = new Terrain(terMat, "lidar.zip");
        rootNode.attachChild(terrain.getGeometry());
    }

    private void makeWater() {
        // Water material
        Material waterMat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        waterMat.setColor("Color", new ColorRGBA(0, 0, 1, 0.5f));
        waterMat.getAdditionalRenderState().setBlendMode(BlendMode.Alpha);
        // Pinor material
        Material pinormat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        pinormat.setColor("Color", ColorRGBA.Red);
        // make cells.
        cells = terrain.makeCells("");

        water = new Water(cells, Display.getDrawable());
        Geometry g = new Geometry("Water", water);
        g.setMaterial(waterMat);
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
        sun.setColor(new ColorRGBA(1, 0.973f, 0.617f, 0.0f));
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
            String uuid = req.getUuid();
            // Drone Rendering stuff
            if (req.isRemoved() && drones.containsKey(uuid)) {
                // Drone is to be removed and is in map.
                rootNode.detachChild(drones.remove(uuid));
            }
            else if (!req.isRemoved()) {
                // Get response packet.
                ServiceResponse resp = droneCamera.process(tpf, req, cells.getPinors());
                Spatial d;
                if (drones.containsKey(uuid)) {
                    // This is a new drone.
                    d = drone.clone();
                    rootNode.attachChild(d);
                    drones.put(uuid, d);
                }
                else {
                    // Existing drone.
                    d = drones.get(uuid);
                }
                // Move drone
                d.setLocalTranslation(droneCamera.getPos());
                // Notify drone via future.
                req.getFuture().complete(resp);
            }
        }
    }

}
