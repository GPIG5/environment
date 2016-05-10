package water;

import java.util.Comparator;

public class CRCompare implements Comparator<CollisionResult> {
	@Override
	public int compare(CollisionResult o1, CollisionResult o2) {
		float t1 = o1.getTime();
		float t2 = o2.getTime();
		if (t1 == t2) {
			return 0;
		}
		else if (t1 < t2) {
			return -1;
		}
		else {
			return 1;
		}
	}
}