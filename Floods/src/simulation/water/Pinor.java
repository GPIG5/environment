package simulation.water;

import com.jme3.bounding.BoundingVolume;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.renderer.Camera.FrustumIntersect;
import com.jme3.scene.Geometry;
import com.jme3.scene.shape.Box;

public class Pinor extends Geometry {
    public Pinor(Vector3f loc) {
        this.move(loc);
        this.setCullHint(CullHint.Dynamic);
    }
    
    public boolean isVisible(Camera c) {
        BoundingVolume bv = this.getWorldBound();
        int state = bv.getCheckPlane();
        bv.setCheckPlane(0);
        FrustumIntersect fi =  c.contains(bv);
        bv.setCheckPlane(state);
        return fi != FrustumIntersect.Outside;
    }

}
