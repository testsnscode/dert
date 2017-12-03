package gov.nasa.arc.dert.render;

import gov.nasa.arc.dert.scenegraph.GroupNode;
import gov.nasa.arc.dert.util.ImageUtil;
import gov.nasa.arc.dert.util.SpatialUtil;
import gov.nasa.arc.dert.viewpoint.BasicCamera;

import java.util.concurrent.atomic.AtomicBoolean;

import com.ardor3d.framework.CanvasRenderer;
import com.ardor3d.framework.Scene;
import com.ardor3d.image.Image;
import com.ardor3d.intersection.PickResults;
import com.ardor3d.math.Ray3;
import com.ardor3d.renderer.Renderer;
import com.ardor3d.renderer.queue.RenderBucketType;
import com.ardor3d.renderer.state.BlendState;
import com.ardor3d.renderer.state.RenderState;
import com.ardor3d.renderer.state.ZBufferState;
import com.ardor3d.scenegraph.hint.NormalsMode;
import com.ardor3d.util.ReadOnlyTimer;
import com.jogamp.opengl.GL;

/**
 * Provides an abstract base class for 3D graphics scenes by implementing
 * Ardor3D Scene interface.
 *
 */
public abstract class BasicScene implements Scene {

	// Path to image presented at startup
	public static String imagePath;
	public static volatile Image dertImage;

	public final AtomicBoolean sceneChanged;

	protected GroupNode rootNode;
	protected int width, height;

	/**
	 * Constructor
	 */
	public BasicScene() {
		// load the default image
		if (dertImage == null) {
			dertImage = ImageUtil.loadImage(imagePath, true);
		}
		sceneChanged = new AtomicBoolean(true);
	}

	/**
	 * Initialize this scene
	 * 
	 * @param canvasRenderer
	 */
	public abstract void init(CanvasRenderer canvasRenderer);

	/**
	 * Set the root node of the scene graph
	 * 
	 * @param root
	 */
	public void setRootNode(GroupNode root) {
		rootNode = root;

		// Initialize root node
		ZBufferState buf = (ZBufferState) rootNode.getLocalRenderState(RenderState.StateType.ZBuffer);
		if (buf == null) {
			buf = new ZBufferState();
			buf.setFunction(ZBufferState.TestFunction.LessThanOrEqualTo);
		}
		buf.setEnabled(true);
		rootNode.setRenderState(buf);

		BlendState as = (BlendState) rootNode.getLocalRenderState(RenderState.StateType.Blend);
		if (as == null) {
			as = new BlendState();
		}
		as.setBlendEnabled(true);
		rootNode.setRenderState(as);

		rootNode.getSceneHints().setRenderBucketType(RenderBucketType.Opaque);
		rootNode.getSceneHints().setNormalsMode(NormalsMode.NormalizeIfScaled);

		rootNode.updateGeometricState(0, true);
	}

	/**
	 * Perform a rendering event
	 */
	@Override
	public boolean renderUnto(final Renderer renderer) {

		// the scene graph is empty, show the default image
		if (rootNode == null) {
			renderer.clearBuffers(Renderer.BUFFER_COLOR_AND_DEPTH);
			int x = (int)(width - dertImage.getWidth()) / 2;
			int y = (int)(height - dertImage.getHeight()) / 2;
			switch (dertImage.getDataFormat()) {
			case BGR:
				((JoglRendererDouble) renderer).drawImage(x, y, dertImage.getWidth(), dertImage.getHeight(), 1.0f,
						GL.GL_BGR, GL.GL_UNSIGNED_BYTE, dertImage.getData(0));
				break;
			case BGRA:
				((JoglRendererDouble) renderer).drawImage(x, y, dertImage.getWidth(), dertImage.getHeight(), 1.0f,
						GL.GL_BGRA, GL.GL_UNSIGNED_BYTE, dertImage.getData(0));
				break;
			case RGB:
				((JoglRendererDouble) renderer).drawImage(x, y, dertImage.getWidth(), dertImage.getHeight(), 1.0f,
						GL.GL_RGB, GL.GL_UNSIGNED_BYTE, dertImage.getData(0));
				break;
			case RGBA:
				((JoglRendererDouble) renderer).drawImage(x, y, dertImage.getWidth(), dertImage.getHeight(), 1.0f,
						GL.GL_RGBA, GL.GL_UNSIGNED_BYTE, dertImage.getData(0));
			case Luminance:
				((JoglRendererDouble) renderer).drawImage(x, y, dertImage.getWidth(), dertImage.getHeight(), 1.0f,
						GL.GL_LUMINANCE, GL.GL_UNSIGNED_BYTE, dertImage.getData(0));
				break;
			case LuminanceAlpha:
				((JoglRendererDouble) renderer).drawImage(x, y, dertImage.getWidth(), dertImage.getHeight(), 1.0f,
						GL.GL_LUMINANCE_ALPHA, GL.GL_UNSIGNED_BYTE, dertImage.getData(0));
				break;
			default:
				break;
			}
		}
		else {
			render(renderer);
		}

		return (true);
	}

	/**
	 * Render the scene graph
	 * 
	 * @param renderer
	 */
	public abstract void render(Renderer renderer);

	/**
	 * Perform a pick in the scene
	 */
	@Override
	public PickResults doPick(final Ray3 pickRay) {
		return (SpatialUtil.doPick(rootNode, pickRay));
	}

	/**
	 * Get the root node of the scene graph
	 * 
	 * @return
	 */
	public final GroupNode getRootNode() {
		return (rootNode);
	}

	/**
	 * Update the objects in the scene graph
	 * 
	 * @param timer
	 */
	public void update(ReadOnlyTimer timer) {
	}

	/**
	 * Get the camera associated with this scene
	 * 
	 * @return
	 */
	public abstract BasicCamera getCamera();

	/**
	 * Resize this scene
	 * 
	 * @param width
	 * @param height
	 */
	public void resize(int width, int height) {
		this.width = width;
		this.height = height;
		sceneChanged.set(true);
	}
	
	public int getWidth() {
		return(width);
	}
	
	public int getHeight() {
		return(height);
	}
	
	public boolean needsRender() {
		return(sceneChanged.getAndSet(false));
	}
	
	public void preRender(Renderer renderer) {
		// do nothing
	}

}