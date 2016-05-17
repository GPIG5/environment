package simulation.water;

import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.renderer.Camera.FrustumIntersect;
import com.jme3.scene.Geometry;
import com.jme3.scene.shape.Box;

public class Pinor extends Geometry {
    public Pinor(Vector3f loc) {
        this.move(loc);
    }
    
    public boolean isVisible(Camera c) {
        FrustumIntersect fi =  c.contains(this.getWorldBound());
        return !fi.equals(FrustumIntersect.Outside);
    }

}
