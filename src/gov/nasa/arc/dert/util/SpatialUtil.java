package gov.nasa.arc.dert.util;

import gov.nasa.arc.dert.scene.MapElement;

import java.util.HashMap;

import com.ardor3d.intersection.PickData;
import com.ardor3d.intersection.PickResults;
import com.ardor3d.intersection.PickingUtil;
import com.ardor3d.intersection.PrimitivePickResults;
import com.ardor3d.math.Ray3;
import com.ardor3d.scenegraph.Mesh;
import com.ardor3d.scenegraph.Node;
import com.ardor3d.scenegraph.Spatial;
import com.ardor3d.scenegraph.hint.CullHint;

/**
 * Provides helper methods concerned with Ardor3D Spatial classes.
 *
 */
public class SpatialUtil {

	/**
	 * Do a pick operation on a spatial
	 * 
	 * @param root
	 * @param pickRay
	 * @return
	 */
	public static PickResults doPick(Spatial root, final Ray3 pickRay) {
		root.updateWorldBound(true);
		final PrimitivePickResults bpr = new PrimitivePickResults();
		bpr.setCheckDistance(true);
		PickingUtil.findPick(root, pickRay, bpr);
		if (bpr.getNumber() == 0) {
			return (null);
		}
		PickData closest = bpr.getPickData(0);
		for (int i = 1; i < bpr.getNumber(); ++i) {
			PickData pd = bpr.getPickData(i);
			if (closest.getIntersectionRecord().getClosestDistance() > pd.getIntersectionRecord().getClosestDistance()) {
				closest = pd;
			}
		}
		return bpr;
	}

	/**
	 * Do a pick bounds operation on a spatial
	 * 
	 * @param root
	 * @param pickRay
	 * @param pickTop
	 * @return
	 */
	public static SpatialPickResults pickBounds(Node root, Ray3 pickRay, Node pickTop) {
		root.updateWorldBound(true);
		if (pickTop == null) {
			pickTop = root;
		}
		final SpatialPickResults spr = new SpatialPickResults();
		spr.setCheckDistance(true);
		PickingUtil.findPick(pickTop, pickRay, spr);
		if (spr.getMeshList() == null) {
			return (null);
		} else {
			return (spr);
		}
	}

	/**
	 * Set the spatial to return if picked.
	 * 
	 * @param spatial
	 * @param pickHost
	 */
	public static final void setPickHost(Spatial spatial, Spatial pickHost) {
		if (spatial instanceof Node) {
			Node node = (Node) spatial;
			for (int i = 0; i < node.getNumberOfChildren(); ++i) {
				setPickHost(node.getChild(i), pickHost);
			}
		} else if (spatial instanceof Mesh) {
			HashMap<String, Object> map = (HashMap<String, Object>) spatial.getUserData();
			if (map == null) {
				map = new HashMap<String, Object>();
				spatial.setUserData(map);
			}
			map.put("PickHost", pickHost);
		}
	}

	/**
	 * Get the spatial that is returned if picked.
	 * 
	 * @param spatial
	 * @return
	 */
	public static final Spatial getPickHost(Spatial spatial) {
		if (spatial == null) {
			return (null);
		}
		Spatial pickHost = spatial;
//		while (!(pickHost instanceof Tool) && (pickHost.getParent() != null)) {
//			if (pickHost instanceof Marker) {
//				return (pickHost);
//			}
//			pickHost = pickHost.getParent();
//		}
		while (!(pickHost instanceof MapElement) && (pickHost.getParent() != null)) {
//			if (pickHost instanceof Marker) {
//				return (pickHost);
//			}
			pickHost = pickHost.getParent();
		}
		return (pickHost);
	}
	
	public final static boolean isDisplayed(Spatial spatial) {
		return(spatial.getSceneHints().getCullHint() != CullHint.Always);
	}
}
