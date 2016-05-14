import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferInt;
import java.awt.image.Raster;
import java.awt.image.SampleModel;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

import javax.imageio.ImageIO;

import org.lwjgl.opengl.Display;

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
import com.jme3.renderer.Camera;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
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
import com.jme3.texture.FrameBuffer;
import com.jme3.texture.Image;
import com.jme3.texture.Image.Format;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture.WrapMode;
import com.jme3.util.BufferUtils;

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
	Camera droneCam;
	FrameBuffer droneFb;
	ViewPort droneVp;
	Texture droneTex;
    private final ByteBuffer cpuBuf = BufferUtils.createByteBuffer(480 * 640 * 4);

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
        cam.setLocation(new Vector3f(0.0f, 4f, 0.0f));
        cam.setAxes(new Quaternion(0.0f, 1.0f, 0.0f, 1.0f));
        flyCam.setMoveSpeed(4.0f);
        addLights();
        testCamera();
	}
	
	private void testCamera() {
		// https://github.com/Suinos/heaven-rts-jmegame/blob/d67c84fe3081c1101699b50a101ee07c7588891e/src/rts/ui/MiniView.java
		// https://github.com/cosmolev/ComoFlyer/blob/73a7f5963037e59a7400a64f8210d025f17608db/src/comoflyer/OffscreenComoFlyer.java
		droneCam = new Camera(640, 480);
		droneCam.setFrustumPerspective(45f, 640.0f / 480.0f, 1f, 1000f);
		droneCam.setLocation(new Vector3f(10f, 20f, 10f));
		droneCam.lookAt(new Vector3f(10f, 0f, 10f), Vector3f.UNIT_Y);
		droneVp = renderManager.createPreView("Default", droneCam);
		droneVp.setClearFlags(true, true, true);
		droneVp.attachScene(rootNode);
		droneFb = new FrameBuffer(640, 480, 1);
		droneFb.setDepthBuffer(Format.Depth);
		droneFb.setColorBuffer(Format.ARGB8);
		droneVp.setOutputFrameBuffer(droneFb);
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
		water = new Water(terrain, Display.getDrawable());
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
        // Do drone stuff.
        cpuBuf.clear();
        renderer.readFrameBuffer(droneFb, cpuBuf);
       BufferedImage bi = new BufferedImage(640, 480, BufferedImage.TYPE_INT_ARGB);
       cpuBuf.rewind();
       int[] data = new int[640*480];
       for (int i = 0; i < (640*480); i++) {
    	   int r = 0xFF & cpuBuf.get();
    	   int g = 0xFF & cpuBuf.get();
    	   int b = 0xFF & cpuBuf.get();
    	   int a = 0xFF & cpuBuf.get();
    	   data[i] = (a << 24) | (r << 16) | (g << 8) | b;
       }
       bi.getRaster().setDataElements(0, 0, 640, 480, data);
       try {
    	   ImageIO.write(bi, "png", new File("/tmp/out.png"));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		super.simpleUpdate(tpf);
	}

}
