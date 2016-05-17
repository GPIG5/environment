package simulation;
import com.jme3.app.SimpleApplication;
import com.jme3.asset.plugins.ClasspathLocator;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.material.RenderState.BlendMode;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Spatial.CullHint;
import com.jme3.system.JmeSystem;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture.WrapMode;
import org.lwjgl.opengl.Display;
import simulation.terrain.Terrain;
import simulation.water.Water;
import utility.ServiceInterface;


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
        dronecamera = new DroneCam(renderManager, rootNode);
//		ConcurrentLinkedQueue<CamRequest> requests = dronecamera.getRequestQueue();
//		requests.add(new CamRequest(10, 10));
//		requests.add(new CamRequest(30, 40));
//		requests.add(new CamRequest(10, 40));
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
        Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        ColorRGBA c = ColorRGBA.Blue;
        mat.setColor("Color", new ColorRGBA(0, 0, 1, 0.5f));
        mat.getAdditionalRenderState().setBlendMode(BlendMode.Alpha);
        water = new Water(terrain, Display.getDrawable(), "");
        Geometry g = new Geometry("Water", water);
        g.setMaterial(mat);
        g.setCullHint(CullHint.Never);
        rootNode.attachChild(g);
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
        //dronecamera.process(tpf);
    }

}
