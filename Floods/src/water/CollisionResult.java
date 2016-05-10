package water;

import com.jme3.math.Vector3f;

public class CollisionResult {
	Vector3f point;
	float time;
	
	public CollisionResult(Vector3f p, float t) {
		point = p;
		time = t;
	}
	
	public Vector3f getPoint() {
		return point;
	}
	
	public float getTime() {
		return time;
	}
	
}
