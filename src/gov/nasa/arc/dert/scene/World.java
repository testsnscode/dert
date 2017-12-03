package gov.nasa.arc.dert.scene;

import gov.nasa.arc.dert.action.edit.CoordAction;
import gov.nasa.arc.dert.landscape.Landscape;
import gov.nasa.arc.dert.lighting.Lighting;
import gov.nasa.arc.dert.render.DirtyEventHandler;
import gov.nasa.arc.dert.render.SelectionHandler;
import gov.nasa.arc.dert.scene.featureset.FeatureSets;
import gov.nasa.arc.dert.scene.landmark.Landmarks;
import gov.nasa.arc.dert.scene.tapemeasure.TapeMeasure;
import gov.nasa.arc.dert.scene.tool.Tools;
import gov.nasa.arc.dert.scenegraph.GroupNode;
import gov.nasa.arc.dert.state.ConfigurationManager;
import gov.nasa.arc.dert.state.MarbleState;
import gov.nasa.arc.dert.util.SpatialPickResults;
import gov.nasa.arc.dert.util.SpatialUtil;

import com.ardor3d.math.Ray3;
import com.ardor3d.math.Vector3;
import com.ardor3d.renderer.queue.RenderBucketType;
import com.ardor3d.renderer.state.BlendState;
import com.ardor3d.renderer.state.ColorMaskState;
import com.ardor3d.renderer.state.TextureState;
import com.ardor3d.renderer.state.ZBufferState;
import com.ardor3d.scenegraph.Node;
import com.ardor3d.scenegraph.Spatial;
import com.ardor3d.scenegraph.event.DirtyType;
import com.ardor3d.scenegraph.event.SceneGraphManager;
import com.ardor3d.scenegraph.hint.CullHint;
import com.ardor3d.scenegraph.hint.NormalsMode;

public class World extends GroupNode {

	// Defaults
	public static double defaultStereoFocalDistance = 1;
	public static double defaultStereoEyeSeparation = 0.0333333;
	public static boolean defaultHiddenDashed = false;

	// The world is a singleton, holds viewpoint node
	private static World INSTANCE;

	// Root node of scene graph, holds light and contents nodes
	private GroupNode root;

	// Contents of scene graph, holds landscape and map elements
	private GroupNode contents;

	// Landmark map elements group
	private Landmarks landmarks;

	// Tool map elements group
	private Tools tools;

	// FeatureSet map elements group
	private FeatureSets featureSets;

	// Special figure
	private Marble marble;

	// The current time
	private long timeUTC;

	// Lighting for this world
	private Lighting lighting;

	// Handler for events from the scene framework
	private DirtyEventHandler dirtyEventHandler;

	// This world has been initialized
	private boolean initialized;

	// The tape measure
	private TapeMeasure ruler;
	
	// Show hidden lines as dashed lines
	private boolean hiddenDashed;

	// The texture state for shadows
	private TextureState textureState;

	// The color mask state for stereo
	private ColorMaskState colorMaskState;

	// The selection handler for picking
	private SelectionHandler selectionHandler;

	// Stereo parameters
	public double stereoFocalDistance = defaultStereoFocalDistance;
	public double stereoEyeSeparation = defaultStereoEyeSeparation;
	
	// Coordinate display
	private boolean useLonLat;

	/**
	 * Create a new world
	 * 
	 * @param name
	 * @param landmarks
	 * @param tools
	 * @param featureSets
	 * @param lighting
	 * @param timeUTC
	 * @return
	 */
	public static World createInstance(String name, Landmarks landmarks, Tools tools,
		FeatureSets featureSets, Lighting lighting, long timeUTC) {
		INSTANCE = new World(name, landmarks, tools, featureSets, lighting, timeUTC);
		return (INSTANCE);
	}

	/**
	 * Get the singleton instance of this world
	 * 
	 * @return
	 */
	public static World getInstance() {
		return (INSTANCE);
	}

	/**
	 * Constructor
	 * 
	 * @param name
	 * @param landscape
	 * @param landmarks
	 * @param tools
	 * @param featureSets
	 * @param lighting
	 * @param background
	 * @param timeUTC
	 */
	protected World(String name, Landmarks landmarks, Tools tools, FeatureSets featureSets, Lighting lighting, long timeUTC) {
		super(name);
		this.landmarks = landmarks;
		this.tools = tools;
		this.featureSets = featureSets;
		this.lighting = lighting;
		this.timeUTC = timeUTC;

		// create the scenegraph
		root = new GroupNode("Root");
		contents = new GroupNode("Contents");
		textureState = new TextureState();
		textureState.setEnabled(true);
		contents.setRenderState(textureState);
		colorMaskState = new ColorMaskState();
		colorMaskState.setEnabled(true);
		lighting.setTarget(contents);
		root.attachChild(lighting.getLight());
		root.attachChild(contents);
		attachChild(root);

		// set up event and pick handling
		dirtyEventHandler = new DirtyEventHandler(contents);
		SceneGraphManager.getSceneGraphManager().addDirtyEventListener(dirtyEventHandler);
		selectionHandler = new SelectionHandler();
	}

	/**
	 * Initialize this world. This is carried out after an OpenGL context is
	 * created.
	 */
	public void initialize() {
		// been here before
		if (initialized) {
			return;
		}

		// initialize the scenegraph
		Landscape.getInstance().initialize();
		contents.attachChild(Landscape.getInstance());
		updateGeometricState(0); // create a world bounds

		// initialize the map elements
		landmarks.initialize();
		tools.initialize();
		featureSets.initialize();
		contents.attachChild(landmarks);
		contents.attachChild(tools);
		contents.attachChild(featureSets);

		// initialize lighting
		root.setRenderState(lighting.getGlobalLightState());
		lighting.getLight().setTranslation(Landscape.getInstance().getWorldBound().getCenter());

		// initialize the marble
		MarbleState marbleState = (MarbleState)ConfigurationManager.getInstance().getCurrentConfiguration().getState("MarbleState");
		marble = new Marble(marbleState);
		marble.setNormal(Vector3.UNIT_Z);
		marble.setTranslation(Landscape.getInstance().getCenter());
		marble.setSolarDirection(lighting.getLightDirection());
		contents.attachChild(marble);

		// initialize the ruler
		ruler = new TapeMeasure();
		ruler.getSceneHints().setCullHint(CullHint.Always);
		CoordAction.listenerList.add(ruler);
		contents.attachChild(ruler);

		// initialize shadows
		setTime(timeUTC);
		lighting.enableShadow(lighting.isShadowEnabled());

		SceneGraphManager.getSceneGraphManager().listenOnSpatial(root);
		updateGeometricState(0);

		// Initialize root node
		ZBufferState buf = new ZBufferState();
		buf.setFunction(ZBufferState.TestFunction.LessThanOrEqualTo);
		buf.setEnabled(true);
		root.setRenderState(buf);

		BlendState as = new BlendState();
		as.setBlendEnabled(true);
		root.setRenderState(as);

		root.getSceneHints().setRenderBucketType(RenderBucketType.Opaque);
		root.getSceneHints().setNormalsMode(NormalsMode.NormalizeIfScaled);

		updateGeometricState(0, true);
		initialized = true;
	}

	/**
	 * Select a point in the scene
	 * 
	 * @param pickRay
	 * @param position
	 * @param normal
	 * @param pickTop
	 * @param noQuadTree
	 * @return
	 */
	public Spatial select(Ray3 pickRay, Vector3 position, Vector3 normal, Node pickTop, boolean shiftDown) {
		SpatialPickResults boundsPick = SpatialUtil.pickBounds(contents, pickRay, pickTop);
		if (boundsPick != null) {
			return (selectionHandler.doSelection(pickRay, position, normal, boundsPick, shiftDown));
		}
		return (null);
	}

	/**
	 * Get the dirty event handler
	 * 
	 * @return
	 */
	public DirtyEventHandler getDirtyEventHandler() {
		return (dirtyEventHandler);
	}

	/**
	 * Get the texture state for the world
	 * 
	 * @return
	 */
	public TextureState getTextureState() {
		return (textureState);
	}

	/**
	 * Dispose of resources (landscape and lighting)
	 */
	public void dispose() {
		SceneGraphManager.getSceneGraphManager().removeDirtyEventListener(dirtyEventHandler);
		Landscape.getInstance().dispose();
		lighting.dispose();
		if (ruler != null)
			CoordAction.listenerList.remove(ruler);
	}

	/**
	 * Get the colormask state for stereo
	 * 
	 * @return
	 */
	public ColorMaskState getColorMaskState() {
		return (colorMaskState);
	}

	/**
	 * Get the group of landmarks
	 * 
	 * @return
	 */
	public Landmarks getLandmarks() {
		return (landmarks);
	}

	/**
	 * Get the group of tools
	 * 
	 * @return
	 */
	public Tools getTools() {
		return (tools);
	}

	/**
	 * Get the group of line sets
	 * 
	 * @return
	 */
	public FeatureSets getFeatureSets() {
		return (featureSets);
	}

	/**
	 * Get the contents of the scene graph
	 * 
	 * @return
	 */
	public GroupNode getContents() {
		return (contents);
	}

	/**
	 * Get the root node of the scene graph
	 * 
	 * @return
	 */
	public GroupNode getRoot() {
		return (root);
	}

	/**
	 * Set the vertical exaggeration of the terrain
	 * 
	 * @param vertExag
	 */
	public void setVerticalExaggeration(double vertExag) {
		if (vertExag <= 0) {
			return;
		}
		Landscape landscape = Landscape.getInstance();
		if (landscape == null) {
			return;
		}
		double oldVal = landscape.getVerticalExaggeration();
		landscape.setVerticalExaggeration(vertExag);
		double minZ = landscape.getMinimumElevation();
		contents.setVerticalExaggeration(vertExag, oldVal, minZ);
	}

	/**
	 * Set the vertical exaggeration of a new map element
	 * 
	 * @param vertExag
	 */
	public void setVerticalExaggeration(MapElement me) {
		Landscape landscape = Landscape.getInstance();
		if (landscape == null) {
			return;
		}
		double oldVal = landscape.getVerticalExaggeration();
		double minZ = landscape.getMinimumElevation();
		me.setVerticalExaggeration(oldVal, oldVal, minZ);
		((Spatial)me).updateGeometricState(0);
	}

	/**
	 * Get the vertical exaggeration of the terrain
	 * 
	 * @return
	 */
	public double getVerticalExaggeration() {
		Landscape landscape = Landscape.getInstance();
		if (landscape == null) {
			return (1);
		}
		return (landscape.getVerticalExaggeration());
	}

	/**
	 * Get the marble
	 * 
	 * @return
	 */
	public Marble getMarble() {
		return (marble);
	}

	/**
	 * Set the current time
	 * 
	 * @param timeUTC
	 */
	public void setTime(long timeUTC) {
		this.timeUTC = timeUTC;
		lighting.setTime(timeUTC);
	}

	/**
	 * Get the current time
	 * 
	 * @return
	 */
	public long getTime() {
		return (timeUTC);
	}

	/**
	 * Get the lighting object
	 * 
	 * @return
	 */
	public Lighting getLighting() {
		return (lighting);
	}

	/**
	 * Get the tape measure
	 * 
	 * @return
	 */
	public TapeMeasure getTapeMeasure() {
		return (ruler);
	}
	
	/**
	 * Get the hidden line representation flag
	 */
	public boolean isHiddenDashed() {
		return(hiddenDashed);
	}
	
	/**
	 * Set the hidden line representation flag
	 */
	public void setHiddenDashed(boolean hiddenDashed) {
		this.hiddenDashed = hiddenDashed;
		tools.setHiddenDashed(hiddenDashed);
		ruler.setHiddenDashed(hiddenDashed);
		root.markDirty(DirtyType.RenderState);
	}
	
	public void setMapElementsOnTop(boolean onTop) {
		landmarks.setOnTop(onTop);
		tools.setOnTop(onTop);
		featureSets.setOnTop(onTop);
		root.markDirty(DirtyType.RenderState);
	}
	
	public boolean isMapElementsOnTop() {
		return(landmarks.isOnTop());
	}
	
	public void setUseLonLat(boolean useLonLat) {
		this.useLonLat = useLonLat;
	}
	
	public boolean getUseLonLat() {
		return(useLonLat);
	}
	
	public static void markClean() {
		if (INSTANCE == null)
			return;
		INSTANCE.dirtyEventHandler.changed.set(false);
		INSTANCE.dirtyEventHandler.terrainChanged.set(false);
	}
}
