package gov.nasa.arc.dert.test;

import gov.nasa.arc.dert.landscape.Landscape;
import gov.nasa.arc.dert.scene.World;
import gov.nasa.arc.dert.scene.landmark.Placemark;
import gov.nasa.arc.dert.scene.tool.Path.BodyType;
import gov.nasa.arc.dert.scene.tool.Path.LabelType;
import gov.nasa.arc.dert.scene.tool.fieldcamera.FieldCameraInfoManager;
import gov.nasa.arc.dert.scenegraph.Shape.ShapeType;
import gov.nasa.arc.dert.state.AnimationState;
import gov.nasa.arc.dert.state.Configuration;
import gov.nasa.arc.dert.state.ConfigurationManager;
import gov.nasa.arc.dert.state.FeatureSetState;
import gov.nasa.arc.dert.state.FieldCameraState;
import gov.nasa.arc.dert.state.FigureState;
import gov.nasa.arc.dert.state.GridState;
import gov.nasa.arc.dert.state.ImageBoardState;
import gov.nasa.arc.dert.state.MapElementState;
import gov.nasa.arc.dert.state.MapElementsState;
import gov.nasa.arc.dert.state.PathState;
import gov.nasa.arc.dert.state.PlacemarkState;
import gov.nasa.arc.dert.state.PlaneState;
import gov.nasa.arc.dert.state.ProfileState;
import gov.nasa.arc.dert.state.ViewData;
import gov.nasa.arc.dert.state.ViewpointState;
import gov.nasa.arc.dert.state.WaypointState;
import gov.nasa.arc.dert.util.ColorMap;
import gov.nasa.arc.dert.viewpoint.FlyThroughParameters;
import gov.nasa.arc.dert.viewpoint.ViewpointStore;

import java.awt.Color;
import java.io.File;

import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.Vector3;

public class PersistenceTest {
	
	public boolean testPersistence(String testLoc) {
		
		System.err.println("Test Persistence");
		File testDir = new File(testLoc);
		if (!testDir.exists()) {
			System.err.println("Test landscape does not exist.");
			return(false);
		}
		
		ConfigurationManager cm = ConfigurationManager.getInstance();
		Configuration config = cm.getCurrentConfiguration();
		config.setLabel("Test");
		config.setLandscapePath(testLoc);
		File dertFile = new File(testLoc, "dert");
		String configLocation = dertFile.getAbsolutePath();
		ColorMap.setConfigLocation(configLocation);
		FieldCameraInfoManager.getInstance().setConfigLocation(configLocation);
		config.worldState.createWorld(testLoc, config);
		World.getInstance().initialize();
		
		// WorldState
		World.getInstance().getLighting().setBackgroundColor(ColorRGBA.RED);
		World.getInstance().setVerticalExaggeration(3.2);
		Landscape.getInstance().setSurfaceColor(Color.blue);
		config.worldState.currentViewpoint = new ViewpointStore();
		config.worldState.currentViewpoint.distance = 2000;
		
		// PanelState
		config.consoleState.setViewData(new ViewData(100, 400, 700, 450, false));
		config.consoleState.getViewData().setVisible(true);
		
		// MapElementsState
		Placemark placemark = new Placemark(new PlacemarkState(Vector3.ZERO));
		MapElementsState meState = (MapElementsState)config.getState("MapElementsState");
		meState.setLastMapElement(placemark);
		
		// ViewpointState
		ViewpointStore store = new ViewpointStore();
		store.name = null;
		store.location = new Vector3(1, 2, 3);
		store.direction = new Vector3(4, 5, 6);
		store.lookAt = new Vector3(7, 8, 9);
		store.frustumLeft = -2;
		store.frustumRight = 2;
		store.frustumBottom = -1;
		store.frustumTop = 1;
		store.frustumNear = 0.1;
		store.frustumFar = 1001;
		store.distance = 999;
		store.azimuth = 0.8;
		store.elevation = 0.9;
		store.magIndex = 2;
		ViewpointState vpState = (ViewpointState)config.getState("ViewpointState");
		vpState.viewpointList.add(store);
		
		// AnimationState
		AnimationState aState = (AnimationState)config.getState("AnimationState");
		FlyThroughParameters flyParams = aState.getFlyParams();
		flyParams.numFrames = 13;
		flyParams.millisPerFrame = 444;
		flyParams.pathHeight = 17;
		flyParams.loop = true;
		
		// MapElementStates
		FieldCameraState fc = new FieldCameraState(new Vector3(1, 2, 3));
		fc.fieldCameraDef = "CameraDef2";
		fc.fovVisible = true;
		fc.lineVisible = true;
		fc.crosshairVisible = true;
		fc.azimuth = 0.1;
		fc.tilt = 0.2;
		fc.height = 11;
		config.mapElementStateList.add(fc);
		
		FigureState f = new FigureState(new Vector3(4, 5, 6), new Vector3(1, 1, 1));
		f.normal = new Vector3(0,1,0);
		f.azimuth = 0.22;
		f.tilt = 0.33;
		f.shape = ShapeType.cylinder;
		f.showNormal = true;
		f.autoScale = false;
		f.visible = false;
		f.locked = true;
		f.labelVisible = false;
		f.size = 10;
		f.color = Color.pink;
		f.setAnnotation("blah");
		config.mapElementStateList.add(f);
		
		GridState g = GridState.createCartesianGridState(new Vector3(7, 8, 9));
		g.rings = 17;
		g.columns = 2;
		g.rows = 8;
		g.compassRose = true;
		config.mapElementStateList.add(g);
		
		ImageBoardState ib = new ImageBoardState(new Vector3(10, 11, 12), testLoc);
		ib.position = new Vector3(13, 14, 15);
		config.mapElementStateList.add(ib);
		
		FeatureSetState ls = new FeatureSetState("Test", testLoc, Color.red, false, null);
		ls.filePath = testLoc+"/lineset";
		config.mapElementStateList.add(ls);
		
		PathState p = new PathState(new Vector3(16,17,18));
		p.bodyType = BodyType.Polygon;
		p.labelType = LabelType.Elevation;
		p.waypointsVisible = false;
		p.lineWidth = 20;
		config.mapElementStateList.add(p);	
		
		PlacemarkState pl = new PlacemarkState(new Vector3(19,20,21));
		pl.textureIndex = 5;
		config.mapElementStateList.add(pl);
		
		PlaneState pn = new PlaneState(new Vector3(22,23,24));
		pn.triangleVisible = false;
		pn.p0.set(-999, 0, 0);
		pn.p1.set(0, -999, 0);
		pn.p2.set(0, 0, -999);
		pn.lengthScale = 100;
		pn.widthScale = 200;
		pn.colorMapName = "default1";
		pn.gradient = true;
		pn.minimum = 10;
		pn.maximum = 90;
		config.mapElementStateList.add(pn);
		
		ProfileState pr = new ProfileState(new Vector3(25,26,27));
		pr.p0.set(-999, 0, 0);
		pr.p1.set(0, -999, 0);
		config.mapElementStateList.add(pr);
		
		WaypointState wp = new WaypointState(1, new Vector3(28,29,30), "Test", 1, Color.gray, false, true);
		wp.location.set(0, -1, 0);
		p.pointList.add(wp);
		
		cm.saveConfiguration(config);
		System.err.println("Original WorldState: "+config.worldState);
		System.err.println("Original ConsoleState: "+config.consoleState);
		System.err.println("Original MapElementsState: "+meState);
		System.err.println("Original ViewpointState: "+vpState);
		System.err.println("Original FieldCameraState: "+fc);
		System.err.println("Original FigureState: "+f);
		System.err.println("Original GridState: "+g);
		System.err.println("Original ImageBoardState: "+ib);
		System.err.println("Original FeatureSetState: "+ls);
		System.err.println("Original PathState: "+p);
		System.err.println("Original PlacemarkState: "+pl);
		System.err.println("Original PlaneState: "+pn);
		System.err.println("Original ProfileState: "+pr);
		System.err.println("Original WaypointState: "+wp);
		Configuration newConfig = cm.loadConfiguration(testLoc+"/dert/config/Test");
		System.err.println("Saved WorldState: "+newConfig.worldState);
		System.err.println("Saved ConsoleState: "+newConfig.consoleState);
		meState = (MapElementsState)newConfig.getState("MapElementsState");
		System.err.println("Saved MapElementsState: "+meState);
		vpState = (ViewpointState)newConfig.getState("ViewpointState");
		System.err.println("Saved ViewpointState: "+vpState);
		System.err.println("Saved FieldCameraState: "+newConfig.mapElementStateList.get(0));
		System.err.println("Saved FigureState: "+newConfig.mapElementStateList.get(1));
		System.err.println("Saved GridState: "+newConfig.mapElementStateList.get(2));
		System.err.println("Saved ImageBoardState: "+newConfig.mapElementStateList.get(3));
		System.err.println("Saved FeatureSetState: "+newConfig.mapElementStateList.get(4));
		PathState sp = (PathState)newConfig.mapElementStateList.get(5);
		System.err.println("Saved PathState: "+sp);
		System.err.println("Saved PlacemarkState: "+newConfig.mapElementStateList.get(6));
		System.err.println("Saved PlaneState: "+newConfig.mapElementStateList.get(7));
		System.err.println("Saved ProfileState: "+newConfig.mapElementStateList.get(8));
		System.err.println("Saved WaypointState: "+sp.pointList.get(1));
		if (!isEqualTo(config, newConfig)) {
			System.err.println("Saved configuration is not equal to the original. Persistence test failed.");
			return(false);
		}
		return(true);
	}
	
	public boolean isEqualTo(Configuration config1, Configuration config2) {
		if (!config1.toString().equals(config2.toString())) {
			System.err.println("Configuration labels not eaual ("+config1.toString()+","+config2.toString()+")");
			return(false);
		}
//		if (config1.mapElementCount.length != config2.mapElementCount.length) {
//			System.err.println("Configuration map element counts not equal ("+config1.mapElementCount.length+","+config2.mapElementCount.length+")");
//			return(false);
//		}
		MapElementState.Type[] t = MapElementState.Type.values();
		for (int i=0; i<t.length; ++i)
			if (config1.getMapElementCount(t[i]) != config2.getMapElementCount(t[i])) {
				System.err.println("Configuration map element count "+i+" not equal ("+config1.getMapElementCount(t[i])+","+config2.getMapElementCount(t[i])+")");
				return(false);
			}
		if (!config1.consoleState.isEqualTo(config2.consoleState)) { 
			System.err.println("Configuration console state not equal");
			return(false);
		}
		if (!config1.getState("MarbleState").isEqualTo(config2.getState("MarbleState"))) {
			System.err.println("Configuration marble state not equal");
			return(false);
		}
		if (!config1.worldState.isEqualTo(config2.worldState)) {
			System.err.println("Configuration world state not equal");
			return(false);
		}
		if (!config1.getState("HelpState").isEqualTo(config2.getState("HelpState"))) {
			System.err.println("Configuration world state not equal");
			return(false);
		}
		if (!config1.getState("SurfaceAndLayersState").isEqualTo(config2.getState("SurfaceAndLayersState"))) {
			System.err.println("Configuration surface and layer state not equal");
			return(false);
		}
		if (!config1.getState("MapElementsState").isEqualTo(config2.getState("MapElementsState"))) {
			System.err.println("Configuration map elements state not equal");
			return(false);
		}
		if (!config1.getState("ColorBarsState").isEqualTo(config2.getState("ColorBarsState"))) {
			System.err.println("Configuration color bars state not equal");
			return(false);
		}
		if (!config1.getState("LightingState").isEqualTo(config2.getState("LightingState"))) {
			System.err.println("Configuration lighting state not equal");
			return(false);
		}
		if (!config1.getState("LightPositionState").isEqualTo(config2.getState("LightPositionState"))) {
			System.err.println("Configuration light position state not equal");
			return(false);
		}
		if (!config1.getState("ViewpointState").isEqualTo(config2.getState("ViewpointState"))) {
			System.err.println("Configuration viewpoint state not equal");
			return(false);
		}
		if (!config1.getState("AnimationState").isEqualTo(config2.getState("AnimationState"))) {
			System.err.println("Configuration animation state not equal");
			return(false);
		}
		if (config1.mapElementStateList.size() != config2.mapElementStateList.size()) {
			System.err.println("Configuration map elements state list size not equal");
			return(false);
		}
		for (int i=0; i<config1.mapElementStateList.size(); ++i) {
			MapElementState mps0 = config1.mapElementStateList.get(i);
			MapElementState mps1 = config2.mapElementStateList.get(i);
			if (!mps0.isEqualTo(mps1)) {
				System.err.println("Configuration map element state "+i+" not equal");
				return(false);
			}
		}
		return(true);
	}

}
