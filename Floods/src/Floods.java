import java.util.concurrent.ConcurrentLinkedQueue;

import org.lwjgl.opengl.Display;

import com.jme3.app.DebugKeysAppState;
import com.jme3.app.FlyCamAppState;
import com.jme3.app.SimpleApplication;
import com.jme3.app.StatsAppState;
import com.jme3.app.state.AppState;
import com.jme3.asset.plugins.ClasspathLocator;
import com.jme3.audio.AudioListenerState;
import com.jme3.input.FlyByCamera;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.material.RenderState.BlendMode;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial.CullHint;
import com.jme3.system.AppSettings;
import com.jme3.system.JmeContext.Type;
import com.jme3.system.JmeSystem;
import com.jme3.texture.FrameBuffer;
import com.jme3.texture.Image;
import com.jme3.texture.Image.Format;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture.WrapMode;
import com.jme3.util.BufferUtils;

import drones.CamRequest;
import drones.DroneCam;
import mesh.Mesh;
import services.ServiceInterface;
import terrain.Terrain;
import water.Water;

import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
 

public class Floods extends SimpleApplication {
	Water water;
	Terrain terrain;
	Geometry gter;
	DroneCam dronecamera;

	public static void main(String[] args) {
		ServiceInterface si;
//        Mesh mesh = new Mesh();
//        mesh.start();
		Floods app = new Floods();
		app.start();
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
        ConcurrentLinkedQueue<CamRequest> requests = dronecamera.getRequestQueue();
        requests.add(new CamRequest(10, 10));
        requests.add(new CamRequest(30, 40));
        requests.add(new CamRequest(10, 40));
	}

	private void makeTerrain() {
        Material mat2 = new Material(assetManager,
                "Common/MatDefs/Light/Lighting.j3md");  // create a simple material
        		
        //mat2.setBoolean("UseMaterialColors",true);    - This breaks things
        mat2.setColor("Diffuse",ColorRGBA.White);  // minimum material color
        mat2.setColor("Specular",ColorRGBA.LightGray); // for shininess
        mat2.setFloat("Shininess", 32f); // [1,128] for shininess
        Texture gtex = assetManager.loadTexture("Textures/yorktex.jpg");
        gtex.setWrap(WrapMode.Repeat);
        mat2.setTexture("DiffuseMap", gtex);
       
        terrain = new Terrain(mat2, "lidar.zip");
        rootNode.attachChild(terrain.getGeometry());
	}
	
	private void makeWater() {
		Material mat = new Material(assetManager,
		          "Common/MatDefs/Misc/Unshaded.j3md");  // create a simple material
		ColorRGBA c = ColorRGBA.Blue;
		mat.setColor("Color", new ColorRGBA(0,0,1,0.5f));   // set color of material to blue
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
        sun.setDirection(new Vector3f(-.5f,-.5f,-.5f).normalizeLocal());
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
        dronecamera.process(tpf);
	}

}
