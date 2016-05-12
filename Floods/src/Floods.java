import com.jme3.app.DebugKeysAppState;
import com.jme3.app.FlyCamAppState;
import com.jme3.app.LegacyApplication;
import com.jme3.app.SimpleApplication;
import com.jme3.app.StatsAppState;
import com.jme3.app.state.AppState;
import com.jme3.asset.plugins.ClasspathLocator;
import com.jme3.audio.AudioListenerState;
import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.input.FlyByCamera;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.light.SpotLight;
import com.jme3.material.Material;
import com.jme3.material.RenderState.BlendMode;
import com.jme3.material.TechniqueDef.ShadowMode;
import com.jme3.profile.AppStep;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.queue.RenderQueue.Bucket;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial.CullHint;
import com.jme3.scene.shape.Box;
import com.jme3.shadow.DirectionalLightShadowFilter;
import com.jme3.shadow.DirectionalLightShadowRenderer;
import com.jme3.shadow.SpotLightShadowFilter;
import com.jme3.system.AppSettings;
import com.jme3.system.JmeContext.Type;
import com.jme3.system.JmeSystem;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture.WrapMode;

import terrain.Terrain;
import water.Water;

import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
 

public class Floods extends SimpleApplication {
	
	SpotLight spot;
	Water water;
	Terrain terrain;
	Geometry gter;

	public static void main(String[] args) {
		Floods app = new Floods();
		app.start();
	}

	@Override
	public void simpleInitApp() {
		assetManager.registerLocator("assets", ClasspathLocator.class);
		System.out.print(JmeSystem.getPlatformAssetConfigURL());
        makeTerrain();
        makeWater();
        cam.setLocation(new Vector3f(0.0f, 5.0f, 0.0f));
        cam.setAxes(new Quaternion(0.0f, 1.0f, 0.0f, 1.0f));
        flyCam.setMoveSpeed(4.0f);
        addLights();
	}
	
	private void loadScreen() {
		
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
       
        terrain = new Terrain(mat2);
        rootNode.attachChild(terrain.getGeometry());
	}
	
	private void makeWater() {
		Material mat = new Material(assetManager,
		          "Common/MatDefs/Misc/Unshaded.j3md");  // create a simple material
		ColorRGBA c = ColorRGBA.Blue;
		mat.setColor("Color", new ColorRGBA(0,0,1,0.5f));   // set color of material to blue
		mat.getAdditionalRenderState().setBlendMode(BlendMode.Alpha);
		//
		water = new Water(terrain, mat);
		//water.addWater(0, 0, 20);
		rootNode.attachChild(water);
	}
	
	private void addLights() {
        DirectionalLight sun = new DirectionalLight();
        sun.setColor(ColorRGBA.Orange);
        sun.setDirection(new Vector3f(-.5f,-.5f,-.5f).normalizeLocal());
        rootNode.addLight(sun);
        
//        final int SHADOWMAP_SIZE=1024;
//        DirectionalLightShadowRenderer dlsr = new DirectionalLightShadowRenderer(assetManager, SHADOWMAP_SIZE, 3);
        //dlsr.setLight(sun);
       // viewPort.addProcessor(dlsr);
        viewPort.setBackgroundColor(ColorRGBA.LightGray);
 
//        DirectionalLightShadowFilter dlsf = new DirectionalLightShadowFilter(assetManager, SHADOWMAP_SIZE, 3);
//        dlsf.setLight(sun);
//        dlsf.setEnabled(true);
//        FilterPostProcessor fpp = new FilterPostProcessor(assetManager);
//        fpp.addFilter(dlsf);
//        viewPort.addProcessor(fpp);
        
        spot = new SpotLight();
        spot.setSpotRange(100f);                           // distance
        spot.setSpotInnerAngle(15f * FastMath.DEG_TO_RAD); // inner light cone (central beam)
        spot.setSpotOuterAngle(35f * FastMath.DEG_TO_RAD); // outer light cone (edge of the light)
        spot.setColor(ColorRGBA.White.mult(0.3f));         // light color
        spot.setPosition(cam.getLocation());               // shine from camera loc
        spot.setDirection(cam.getDirection());             // shine forward from camera loc
        rootNode.addLight(spot);
        
        AmbientLight al = new AmbientLight();
        al.setColor(ColorRGBA.White.mult(0.5f));
        rootNode.addLight(al);
	}
	
	@Override
	public void simpleUpdate(float tpf) {
        spot.setPosition(cam.getLocation());               // shine from camera loc
        spot.setDirection(cam.getDirection());             // shine forward from camera loc
        water.process();
		super.simpleUpdate(tpf);
	}

}
