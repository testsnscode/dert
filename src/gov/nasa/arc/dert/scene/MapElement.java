package gov.nasa.arc.dert.scene;

import gov.nasa.arc.dert.state.MapElementState;
import gov.nasa.arc.dert.state.MapElementState.Type;
import gov.nasa.arc.dert.terrain.QuadTree;
import gov.nasa.arc.dert.view.world.GroundEdit;

import java.awt.Color;

import javax.swing.Icon;

import com.ardor3d.math.Vector3;
import com.ardor3d.math.type.ReadOnlyVector3;

/**
 * Interface for map elements. These include objects that are not part of the
 * landscape itself. Those are landmarks (placemarks, figures, and billboards),
 * tools (path, plane, camera, grids, profile), and line sets.
 *
 */
public interface MapElement {

	public String getName();

	public MapElementState getState();

	public void setName(String name);

	public boolean isVisible();

	public void setVisible(boolean visible);

	public Type getType();

	public boolean isLocked();

	public void setLocked(boolean locked);

	public boolean updateElevation(QuadTree quadTree);

	public void setVerticalExaggeration(double vertExag, double oldVertExag, double minZ);

	public double getSeekPointAndDistance(Vector3 point);

	public double getSize();

	public Color getColor();
	
	public void setColor(Color color);

	public boolean isLabelVisible();

	public void setLabelVisible(boolean visible);

	public ReadOnlyVector3 getLocationInWorld();
	
	public GroundEdit ground();
	
	public double getZOffset();
	
	public void setZOffset(double zOff, boolean doTrans);
	
	public Icon getIcon();

}
