package gov.nasa.arc.dert.state;

import gov.nasa.arc.dert.scene.tool.Tool;

import java.awt.Color;
import java.util.Map;

/**
 * Base class for state objects for Tools.
 *
 */
public abstract class ToolState extends MapElementState {

	/**
	 * Constructor
	 * 
	 * @param id
	 * @param mapElementType
	 * @param prefix
	 * @param size
	 * @param color
	 * @param labelVisible
	 * @param pinned
	 * @param position
	 */
	public ToolState(int id, MapElementState.Type mapElementType, String prefix, double size, Color color,
		boolean labelVisible) {
		super(id, mapElementType, prefix, size, color, labelVisible);
	}
	
	/**
	 * Constructor for hash map.
	 */
	public ToolState(Map<String,Object> map) {
		super(map);
	}
		

	/**
	 * Get the Tool.
	 * 
	 * @return
	 */
	public Tool getTool() {
		return ((Tool) mapElement);
	}

}
