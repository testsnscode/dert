package gov.nasa.arc.dert.render;

import gov.nasa.arc.dert.landscape.Landscape;
import gov.nasa.arc.dert.scene.World;
import gov.nasa.arc.dert.util.MathUtil;

import com.ardor3d.bounding.BoundingVolume;
import com.ardor3d.image.Texture.DepthTextureCompareFunc;
import com.ardor3d.math.Vector3;
import com.ardor3d.math.type.ReadOnlyVector3;
import com.ardor3d.renderer.Camera;
import com.ardor3d.renderer.Renderer;
import com.ardor3d.renderer.state.TextureState;
import com.ardor3d.scenegraph.Spatial;
import com.ardor3d.scenegraph.event.DirtyType;

/**
 * Projected depth texture that implements a shadow map.
 *
 */
public class ShadowMap extends ProjectedDepthTexture {

	// Texture unit reserved for shadows
	public static final int SHADOW_MAP_UNIT = 7;

	// projection direction fields
	private Vector3 lightCameraLocation, lightDirection, dirTmp;

	// center of shadowed region
	private Vector3 center;

	// radius of shadowed region
	private double radius;

	// this shadow map is visible
	private boolean isEnabled;
	
	// Vector to store solar azimuth and elevation
	private Vector3 angle;

	public ShadowMap(ReadOnlyVector3 center, double radius, Spatial occluder, Spatial target) {
		super(occluder, target, DepthTextureCompareFunc.LessThanEqual, true, true);
		this.center = new Vector3(center);
		if (radius < 1) {
			radius = 1;
		}
		this.radius = radius;
		lightCameraLocation = new Vector3();
		lightDirection = new Vector3();
		dirTmp = new Vector3();
		angle = new Vector3();
	}

	/**
	 * Find out if this is visible
	 * 
	 * @return
	 */
	public boolean getEnabled() {
		return (isEnabled);
	}

	/**
	 * Make this shadow map visible
	 * 
	 * @param enabled
	 */
	public void setEnabled(boolean enabled) {
		isEnabled = enabled;
		World world = World.getInstance();
		TextureState textureState = world.getTextureState();
		if (enabled) {
			textureState.setTexture(texture, SHADOW_MAP_UNIT);
		} else {
			textureState.setTexture(null, SHADOW_MAP_UNIT);
		}
		target.markDirty(DirtyType.RenderState);
		Landscape.getInstance().getLayerManager().enableShadow(enabled);
	}

	/**
	 * Set the center of the shadowed region
	 * 
	 * @param center
	 */
	public void setCenter(ReadOnlyVector3 center) {
		if (center == null)
			this.center.set(Landscape.getInstance().getCenter());
		else
			this.center.set(center);
		target.markDirty(DirtyType.RenderState);
	}

	/**
	 * Get the center of the shadowed region
	 * 
	 * @return
	 */
	public ReadOnlyVector3 getCenter() {
		return (center);
	}

	/**
	 * Set the radius of the shadowed region
	 * 
	 * @param radius
	 */
	public void setRadius(double radius) {
		if (radius == 0) {
			BoundingVolume bv = World.getInstance().getContents().getWorldBound();
			radius = bv.getRadius();
		}
		if (radius < 1) {
			radius = 1;
		}
		this.radius = radius;
		target.markDirty(DirtyType.RenderState);
	}

	/**
	 * Get the radius of the shadowed region
	 * 
	 * @return
	 */
	public double getRadius() {
		return (radius);
	}

	/**
	 * Set the light direction for the shadows
	 * 
	 * @param dir
	 */
	public void updateLightDirection(ReadOnlyVector3 dir) {
		lightDirection.set(dir);
		angle = MathUtil.directionToAzEl(dir, angle);
	}

	@Override
	public void doPrerender(Renderer renderer) {
		if (isEnabled) {
			super.doPrerender(renderer);
		}
	}

	@Override
	protected void updateProjection() {

		// set location of camera
		lightCameraLocation.set(center);
		dirTmp.set(lightDirection);
		dirTmp.negateLocal();
		dirTmp.multiplyLocal(radius);
		lightCameraLocation.addLocal(dirTmp);

		// setup the camera
		Camera camera = textureRenderer.getCamera();
		camera.setLocation(lightCameraLocation);
		camera.lookAt(center, Vector3.UNIT_Y);
		camera.setFrustum(0.1, 2 * radius, -radius, radius, radius, -radius);
		camera.update();

		projectionMatrix.set(camera.getModelViewProjectionMatrix());
		projectionMatrix.multiplyLocal(BIAS);
	}

	@Override
	public void update(final Renderer r) {
		if (angle.getY() >= 0)
			Landscape.getInstance().getLayerManager().setAllDark(true);
		else
			Landscape.getInstance().getLayerManager().setAllDark(false);
		super.update(r);
		World.getInstance().getTextureState().setTexture(texture, SHADOW_MAP_UNIT);

		target.markDirty(DirtyType.RenderState);

	}

	@Override
	public void dispose() {
		setEnabled(false);
		super.dispose();
	}

}
