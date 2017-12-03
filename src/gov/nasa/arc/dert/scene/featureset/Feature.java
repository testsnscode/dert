package gov.nasa.arc.dert.scene.featureset;

import gov.nasa.arc.dert.icon.Icons;
import gov.nasa.arc.dert.landscape.Landscape;
import gov.nasa.arc.dert.scene.MapElement;
import gov.nasa.arc.dert.scenegraph.FigureMarker;
import gov.nasa.arc.dert.scenegraph.LineStrip;
import gov.nasa.arc.dert.scenegraph.Marker;
import gov.nasa.arc.dert.state.FeatureState;
import gov.nasa.arc.dert.state.MapElementState;
import gov.nasa.arc.dert.state.MapElementState.Type;
import gov.nasa.arc.dert.terrain.QuadTree;
import gov.nasa.arc.dert.util.SpatialUtil;
import gov.nasa.arc.dert.view.world.GroundEdit;
import gov.nasa.arc.dert.viewpoint.BasicCamera;
import gov.nasa.arc.dert.viewpoint.ViewDependent;

import java.awt.Color;
import java.util.HashMap;

import javax.swing.Icon;

import com.ardor3d.bounding.BoundingVolume;
import com.ardor3d.math.Vector3;
import com.ardor3d.math.type.ReadOnlyVector3;
import com.ardor3d.scenegraph.Node;
import com.ardor3d.scenegraph.Spatial;
import com.ardor3d.scenegraph.event.DirtyType;
import com.ardor3d.scenegraph.hint.CullHint;

public class Feature
	extends Node
	implements MapElement, ViewDependent {

	public static final Icon icon = Icons.getImageIcon("lineset_16.png");

	// Line color
	private Color color;

	// The location of the origin
	private Vector3 location;
	
	// Feature properties
	private HashMap<String,Object> properties;
	
	private boolean labelVisible;
	
	private FeatureState state;
	
	public Feature(FeatureState state, HashMap<String,Object> properties) {
		this(state.name, state.color, properties);
		this.state = state;
		state.setMapElement(this);
	}

	/**
	 * Constructor
	 * 
	 * @param state
	 * @param elevAttrName
	 */
	public Feature(String name, Color color, HashMap<String,Object> properties) {
		super(name);
		location = new Vector3();
		this.properties = properties;
		this.color = color;
		getSceneHints().setCullHint(CullHint.Dynamic);
	}

	/**
	 * Get the MapElement state
	 */
	@Override
	public MapElementState getState() {
		return (state);
	}

	/**
	 * Is this visible
	 */
	@Override
	public boolean isVisible() {
		return (SpatialUtil.isDisplayed(this));
	}

	/**
	 * Set visible
	 */
	@Override
	public void setVisible(boolean visible) {
		if (visible) {
			getSceneHints().setCullHint(CullHint.Dynamic);
		} else {
			getSceneHints().setCullHint(CullHint.Always);
		}
		markDirty(DirtyType.RenderState);
	}

	/**
	 * Pin this Feature (does nothing)
	 */
	@Override
	public void setLocked(boolean pinned) {
		// nothing here
	}

	/**
	 * Find out if pinned
	 */
	@Override
	public boolean isLocked() {
		return (false);
	}

	/**
	 * Get the color
	 */
	@Override
	public Color getColor() {
		return (color);
	}

	/**
	 * Set the color
	 * 
	 * @param color
	 */
	public void setColor(Color color) {
		this.color = color;
		setColor(this);
	}

	private void setColor(Node node) {
		int n = node.getNumberOfChildren();
		for (int i = 0; i < n; ++i) {
			Spatial child = node.getChild(i);
			if (child instanceof LineStrip) {
				((LineStrip) child).setColor(color);
			}
			else if (child instanceof FigureMarker) {
				((FigureMarker) child).setColor(color);
			}
			else if (child instanceof Node) {
				setColor((Node) child);
			}
		}
	}

	/**
	 * Update the elevation (Z coordinate) for the lines
	 */
	@Override
	public boolean updateElevation(QuadTree quadTree) {
		boolean modified = false;
		for (int i = 0; i < getNumberOfChildren(); ++i) {
			Spatial child = getChild(i);
			if (child instanceof LineStrip) {
				LineStrip lineStrip = (LineStrip) child;
				if (lineStrip.intersects(quadTree)) {
					lineStrip.updateElevation(quadTree);
					modified = true;
				}
			}
			else if (child instanceof FigureMarker) {
				FigureMarker fm = (FigureMarker)child;
				modified |= fm.updateElevation(quadTree);
			}
		}
		return (modified);
	}

	/**
	 * Set the vertical exaggeration
	 */
	@Override
	public void setVerticalExaggeration(double vertExag, double oldVertExag, double minZ) {
		for (int i = 0; i < getNumberOfChildren(); ++i) {
			Spatial child = getChild(i);
			if (child instanceof LineStrip) {
				LineStrip lineStrip = (LineStrip) child;
				lineStrip.setScale(1, 1, vertExag);
			}
			else if (child instanceof FigureMarker) {
				FigureMarker fm = (FigureMarker)child;
				fm.setVerticalExaggeration(vertExag, oldVertExag, minZ);
			}
		}
	}

	/**
	 * Get the map element type
	 */
//	@Override
	public Type getType() {
		return (Type.Feature);
	}

	/**
	 * Get the point to seek for this lineset (center).
	 */
	@Override
	public double getSeekPointAndDistance(Vector3 point) {
		Spatial child = getChild(0);
		double distance = 1;
		if (child instanceof Marker) {
			point.set(child.getWorldTranslation());
			distance = Math.max(((Marker)child).getSize(), 20);
		}
		else {
			BoundingVolume bv = child.getWorldBound();
			point.set(bv.getCenter());
			distance = bv.getRadius();
		}
		return (distance);
	}

	/**
	 * Set the label visibility (does nothing).
	 */
	@Override
	public void setLabelVisible(boolean visible) {
		labelVisible = visible;
		setLabelVisible(this);
	}

	private void setLabelVisible(Node node) {
		int n = node.getNumberOfChildren();
		for (int i = 0; i < n; ++i) {
			Spatial child = node.getChild(i);
			if (child instanceof FigureMarker) {
				((FigureMarker) child).setLabelVisible(labelVisible);
			}
			else if (child instanceof Node) {
				setLabelVisible((Node) child);
			}
		}
	}

	/**
	 * Find out if the label is visible
	 */
	@Override
	public boolean isLabelVisible() {
		return (labelVisible);
	}
	
	public void setSize(float size) {
		setSize(this, size);
	}

	private void setSize(Node node, float size) {
		int n = node.getNumberOfChildren();
		for (int i = 0; i < n; ++i) {
			Spatial child = node.getChild(i);
			if (child instanceof FigureMarker) {
				((FigureMarker) child).setSize(size);
			}
			else if (child instanceof Node) {
				setSize((Node) child, size);
			}
		}
	}
	
	public void setLineWidth(float lineWidth) {
		setLineWidth(this, lineWidth);
	}

	private void setLineWidth(Node node, float lineWidth) {
		int n = node.getNumberOfChildren();
		for (int i = 0; i < n; ++i) {
			Spatial child = node.getChild(i);
			if (child instanceof LineStrip) {
				((LineStrip) child).setLineWidth(lineWidth);
				child.markDirty(DirtyType.RenderState);
			}
			else if (child instanceof Node) {
				setLineWidth((Node) child, lineWidth);
			}
		}
	}

	/**
	 * Get the size (returns 1).
	 */
	@Override
	public double getSize() {
		return (1);
	}

	/**
	 * Get the lineset icon
	 * 
	 * @return
	 */
	public Icon getIcon() {
		return (null);
	}

	/**
	 * Get the location of the origin in planetary coordinates
	 */
	public ReadOnlyVector3 getLocationInWorld() {
		location.set(getWorldTranslation());
		Landscape.getInstance().localToWorldCoordinate(location);
		return (location);
	}
	
	public GroundEdit ground() {
		return(null);
	}
	
	public void setZOffset(double zOff, boolean doTrans) {
		// do nothing
	}
	
	public double getZOffset() {
		return(0);
	}
	
	public HashMap<String,Object> getProperties() {
		return(properties);
	}
	
	public String toString() {
		return(getName());
	}
	
	public void update(BasicCamera camera) {
		if (!isVisible())
			return;
		for (int i = 0; i < getNumberOfChildren(); ++i) {
			Spatial child = getChild(i);
			if (child instanceof ViewDependent)
				((ViewDependent)child).update(camera);
		}
	}
	
	public FeatureSet getFeatureSet() {
		Node parent = getParent();
		while (parent != null) {
			if (parent instanceof FeatureSet)
				return((FeatureSet)parent);
		}
		return(null);
	}
}
