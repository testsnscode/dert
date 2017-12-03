package gov.nasa.arc.dert.view.world;

import gov.nasa.arc.dert.state.WorldState;
import gov.nasa.arc.dert.view.PanelView;
import gov.nasa.arc.dert.viewpoint.Viewpoint;

import java.awt.BorderLayout;

/**
 * Displays the virtual world in 3D.
 *
 */
public class WorldView extends PanelView {

	protected WorldScenePanel panel;

	/**
	 * Constructor
	 */
	public WorldView() {
		super(null);
		setFocusTraversalKeysEnabled(false);
		panel = new WorldScenePanel(960, 540);
		add(panel, BorderLayout.CENTER);
	}

	/**
	 * Get the display panel
	 * 
	 * @return
	 */
	public WorldScenePanel getScenePanel() {
		return (panel);
	}

	/**
	 * Get the viewpoint
	 * 
	 * @return
	 */
	public Viewpoint getViewpoint() {
		return (panel.getViewpointController().getViewpoint());
	}

	/**
	 * Set the world state
	 * 
	 * @param state
	 */
	public void setState(WorldState state) {
		this.state = state;
		panel.setState(state);
	}

	/**
	 * Close the view
	 */
	@Override
	public void close() {
		panel.dispose();
	}

}
