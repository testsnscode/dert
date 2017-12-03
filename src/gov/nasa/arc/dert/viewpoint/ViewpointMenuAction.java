package gov.nasa.arc.dert.viewpoint;

import gov.nasa.arc.dert.Dert;
import gov.nasa.arc.dert.action.CheckBoxMenuItemAction;
import gov.nasa.arc.dert.action.MenuItemAction;
import gov.nasa.arc.dert.action.PopupMenuAction;
import gov.nasa.arc.dert.icon.Icons;
import gov.nasa.arc.dert.scene.World;
import gov.nasa.arc.dert.state.AnimationState;
import gov.nasa.arc.dert.state.ConfigurationManager;
import gov.nasa.arc.dert.state.ViewpointState;
import gov.nasa.arc.dert.viewpoint.Viewpoint.ViewpointMode;

import java.awt.PopupMenu;
import java.awt.Toolkit;

import javax.swing.ImageIcon;

/**
 * Activates hike mode.
 *
 */
public class ViewpointMenuAction extends PopupMenuAction {
	
	protected static ViewpointMenuAction INSTANCE;
	
	public static ImageIcon vpIcon, vpHikeIcon, vpMapIcon, bootIcon, mapIcon;
	
	static {
		vpIcon = Icons.getImageIcon("viewpoint.png");
		vpHikeIcon = Icons.getImageIcon("viewpointonfoot.png");
		vpMapIcon = Icons.getImageIcon("viewpointmap.png");
		bootIcon = Icons.getImageIcon("boot.png");
		mapIcon = Icons.getImageIcon("map.png");
	}

	protected boolean oldOnTop;
	protected CheckBoxMenuItemAction nominal, map, hike;
	
	public static ViewpointMenuAction getInstance() {
		if (INSTANCE == null)
			INSTANCE = new ViewpointMenuAction();
		return(INSTANCE);
	}

	/**
	 * Constructor
	 */
	protected ViewpointMenuAction() {
		super("set viewpoint mode", null, vpIcon);

	}

	@Override
	protected void fillMenu(PopupMenu menu) {
		ViewpointController controller = Dert.getWorldView().getScenePanel().getViewpointController();
		ViewpointMode currentMode = controller.getViewpoint().getMode();
		nominal = new CheckBoxMenuItemAction("Model-centric") {			
			@Override
			protected void run() {
				setMode(ViewpointMode.Nominal);
				map.setState(false);
				hike.setState(false);
			}
		};
		nominal.setState(currentMode == ViewpointMode.Nominal);
		menu.add(nominal);
		map = new CheckBoxMenuItemAction("Map") {			
			@Override
			protected void run() {
				setMode(ViewpointMode.Map);
				nominal.setState(false);
				hike.setState(false);
			}
		};
		map.setState(currentMode == ViewpointMode.Map);
		menu.add(map);
		hike = new CheckBoxMenuItemAction("First-person") {			
			@Override
			protected void run() {
				setMode(ViewpointMode.Hike);
				nominal.setState(false);
				map.setState(false);
			}
		};
		hike.setState(currentMode == ViewpointMode.Hike);
		menu.add(hike);
		
		menu.addSeparator();

		// Open the viewpoint view.
		MenuItemAction viewpointListAction = new MenuItemAction("Open Viewpoint List") {
			@Override
			public void run() {
				ViewpointState vpState = (ViewpointState)ConfigurationManager.getInstance().getCurrentConfiguration().getState("ViewpointState");
				vpState.open(true);
			}
		};
		menu.add(viewpointListAction);
		// Open the animation view.
		MenuItemAction animationAction = new MenuItemAction("Open Animation Control Panel") {
			@Override
			public void run() {
				AnimationState aState = (AnimationState)ConfigurationManager.getInstance().getCurrentConfiguration().getState("AnimationState");
				aState.open(true);
			}
		};
		menu.add(animationAction);
	}
	
	protected void setMode(ViewpointMode mode) {
		ViewpointController controller = Dert.getWorldView().getScenePanel().getViewpointController();
		ViewpointMode currentMode = controller.getViewpoint().getMode();
		if (currentMode == mode)
			return;
		if (!Dert.getWorldView().getViewpoint().setMode(mode)) {
			Toolkit.getDefaultToolkit().beep();
			return;
		}
		switch (mode) {
		case Nominal:
			World.getInstance().setMapElementsOnTop(oldOnTop);
			break;
		case Hike:
			World.getInstance().setMapElementsOnTop(oldOnTop);
			controller.updateCoR();
			break;
		case Map:
			oldOnTop = World.getInstance().isMapElementsOnTop();
			World.getInstance().setMapElementsOnTop(true);
			break;
		}
		setModeIcon(mode);
	}
	
	protected void setModeIcon(ViewpointMode mode) {
		switch (mode) {
		case Nominal:
			setIcon(vpIcon);
			break;
		case Hike:
			setIcon(vpHikeIcon);
			break;
		case Map:
			setIcon(vpMapIcon);
			break;
		}
	}

}
