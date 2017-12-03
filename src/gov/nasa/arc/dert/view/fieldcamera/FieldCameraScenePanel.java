package gov.nasa.arc.dert.view.fieldcamera;

import gov.nasa.arc.dert.action.edit.CoordAction;
import gov.nasa.arc.dert.landscape.Landscape;
import gov.nasa.arc.dert.render.SceneCanvasPanel;
import gov.nasa.arc.dert.scene.tool.fieldcamera.FieldCamera;
import gov.nasa.arc.dert.scene.tool.fieldcamera.FieldCameraInfo;
import gov.nasa.arc.dert.state.FieldCameraState;
import gov.nasa.arc.dert.state.State;
import gov.nasa.arc.dert.ui.CoordTextField;
import gov.nasa.arc.dert.ui.DoubleArrayTextField;
import gov.nasa.arc.dert.ui.DoubleSpinner;
import gov.nasa.arc.dert.ui.DoubleTextField;
import gov.nasa.arc.dert.viewpoint.BasicCamera;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;

import com.ardor3d.math.Rectangle2;
import com.ardor3d.math.Vector3;
import com.ardor3d.math.type.ReadOnlyVector3;
import com.ardor3d.util.ReadOnlyTimer;

/**
 * SceneCanvasPanel for the FieldCameraView. Provides camera pointing controls.
 *
 */
public class FieldCameraScenePanel extends SceneCanvasPanel {

	// Ardor3D Scene
	private FieldCameraScene fieldCameraScene;

	// FieldCamera map element
	private FieldCamera fieldCamera;

	// Camera pointing controls
	private DoubleSpinner azSpinner, tiltSpinner, heightSpinner;
	private DoubleArrayTextField fovDirectionText;
	private CoordTextField seekText, fovLocationText;
	private DoubleTextField distanceText;

	private Vector3 seekPoint = new Vector3();
	private Vector3 coord = new Vector3();

	/**
	 * Constructor
	 * 
	 * @param state
	 */
	public FieldCameraScenePanel(FieldCameraState state) {
		super(state.getViewData().getWidth(), state.getViewData().getHeight(), new FieldCameraScene(state), false);
		FieldCameraInfo instInfo = state.getInfo();
		fieldCamera = (FieldCamera) state.getMapElement();
		fieldCameraScene = (FieldCameraScene) scene;
		setState(state);

		JPanel controlPanel = new JPanel(new GridLayout(3, 1));

		JPanel panel = new JPanel();
		panel.setLayout(new GridLayout(1, 6, 10, 0));

		panel.add(new JLabel("Pan", SwingConstants.RIGHT));
		azSpinner = new DoubleSpinner(instInfo.tripodPan, instInfo.panRange[0], instInfo.panRange[1], 1, true) {
			@Override
			public void stateChanged(ChangeEvent event) {
				Double val = (Double) getValue();
				if (val == null) {
					Toolkit.getDefaultToolkit().beep();
				} else if (Double.isNaN(val.doubleValue())) {
					Toolkit.getDefaultToolkit().beep();
				} else {
					fieldCamera.setAzimuth(-val.doubleValue());
				}
			}
		};
		panel.add(azSpinner);

		panel.add(new JLabel("Tilt", SwingConstants.RIGHT));
		tiltSpinner = new DoubleSpinner(instInfo.tripodTilt, instInfo.tiltRange[0], instInfo.tiltRange[1], 1, false) {
			@Override
			public void stateChanged(ChangeEvent event) {
				Double val = (Double) getValue();
				if (val == null) {
					Toolkit.getDefaultToolkit().beep();
				} else if (Double.isNaN(val.doubleValue())) {
					Toolkit.getDefaultToolkit().beep();
				} else {
					fieldCamera.setElevation(val.doubleValue());
				}
			}
		};
		panel.add(tiltSpinner);

		panel.add(new JLabel("Height", SwingConstants.RIGHT));
		double step = Landscape.getInstance().getPixelWidth();
		double min = instInfo.heightRange[0];
		double max = instInfo.heightRange[1];
		double hgt = instInfo.tripodHeight;
		String fmt = "###0.00";
		if (step < 1) {
			min *= step;
			max *= step;
			hgt *= step;
			fmt = Landscape.format;
		}
		else
			step = 1;
		heightSpinner = new DoubleSpinner(hgt, min, max, step, false, fmt) {
			@Override
			public void stateChanged(ChangeEvent event) {
				Double val = (Double) getValue();
				if (val == null) {
					Toolkit.getDefaultToolkit().beep();
				} else if (Double.isNaN(val.doubleValue())) {
					Toolkit.getDefaultToolkit().beep();
				} else {
					fieldCamera.setHeight(val.doubleValue());
				}
			}
		};
		heightSpinner.setToolTipText("set height of camera above ground");
		panel.add(heightSpinner);

		controlPanel.add(panel);

		panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		JButton seekButton = new JButton("Point At");
		seekButton.setToolTipText("point camera at given coordinates");
		seekButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
				Vector3 store = new Vector3(seekText.getValue());
				seekText.handleChange(store);
			}
		});
		panel.add(seekButton);
		seekText = new CoordTextField(20, "pointing coordinates", seekPoint, Landscape.format, true) {
			@Override
			public void doChange(ReadOnlyVector3 seekPoint) {
				Vector3 angle = fieldCamera.getSyntheticCameraNode().getAzElAngles(seekPoint);
				double az = Math.toDegrees(angle.getX());
				if (az > 180)
					az -= 360;
				azSpinner.setValue(az);
				tiltSpinner.setValue(Math.toDegrees(angle.getY()));	
				scene.sceneChanged.set(true);
			}
		};
		panel.add(seekText);
		CoordAction.listenerList.add(seekText);
		JButton distanceButton = new JButton("Distance");
		distanceButton.setToolTipText("press to display distance to crosshair");
		distanceButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
				double dist = fieldCamera.getSyntheticCameraNode().getDistanceToSurface();
				distanceText.setValue(dist);
			}
		});
		panel.add(distanceButton);
		distanceText = new DoubleTextField(8, 0, false, Landscape.format);
		distanceText.setEditable(false);
		distanceText.setToolTipText("distance to crosshair");
		panel.add(distanceText);
		controlPanel.add(panel);

		panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		panel.add(new JLabel("VwPt Location:"));
		fovLocationText = new CoordTextField(20, "location of camera viewpoint", Landscape.format, true) {
			@Override
			public void doChange(ReadOnlyVector3 coord) {
				// nothing here
			}
		};
		CoordAction.listenerList.add(fovLocationText);
		fovLocationText.setBackground(panel.getBackground());
		fovLocationText.setEditable(false);
		panel.add(fovLocationText);
		panel.add(new JLabel("  Direction:"));
		fovDirectionText = new DoubleArrayTextField(12, new double[3], "0.000");
		fovDirectionText.setToolTipText("direction camera is pointing");
		fovDirectionText.setBackground(panel.getBackground());
		fovDirectionText.setEditable(false);
		panel.add(fovDirectionText);
		controlPanel.add(panel);

		add(controlPanel, BorderLayout.SOUTH);

		azSpinner.setValue(fieldCamera.getAzimuth());
		tiltSpinner.setValue(fieldCamera.getElevation());
		heightSpinner.setValue(fieldCamera.getHeight());
	}
	
	@Override
	public void initialize() {
		super.initialize();
		canvasRenderer.setCamera(fieldCameraScene.getCamera());
	}
	
	public void setRange(FieldCameraInfo info) {
		resize(0, 0, (int)canvasWidth, (int)canvasHeight);
		azSpinner.setMinimum(info.panRange[0]);
		azSpinner.setMaximum(info.panRange[1]);
		tiltSpinner.setMinimum(info.tiltRange[0]);
		tiltSpinner.setMaximum(info.tiltRange[1]);
		heightSpinner.setMinimum(info.heightRange[0]);
		heightSpinner.setMaximum(info.heightRange[1]);
	}

	@Override
	public void setState(State state) {
		super.setState(state);
//		canvasRenderer.setCamera(fieldCameraScene.getCamera());
//		Dimension size = canvas.getSize();
//		scene.resize(size.width, size.height);
	}

	@Override
	public void update(ReadOnlyTimer timer) {
		super.update(timer);
		if (fieldCamera.changed.get()) {
			updateFOV();
		}
		fieldCamera.changed.set(false);
	}

	/**
	 * Update the FOV text fields.
	 */
	public void updateFOV() {
		BasicCamera cam = fieldCamera.getSyntheticCameraNode().getCamera();
		coord.set(cam.getLocation());
		Landscape.getInstance().localToWorldCoordinate(coord);
		fovLocationText.setValue(coord);
		coord.set(cam.getDirection());
		fovDirectionText.setValue(coord);
	}
	
	@Override
	public void dispose() {
		super.dispose();
		if (seekText != null) 
			CoordAction.listenerList.remove(seekText);
		if (fovLocationText != null)
			CoordAction.listenerList.remove(fovLocationText);
	}
	
	@Override
	public void resize(int x, int y, int width, int height) {
		BasicCamera cam = fieldCamera.getSyntheticCameraNode().getCamera();
		super.resize(x, y, width, height);
		int[] vp = cam.getViewport();
		canvasRenderer.setClipRectangle(new Rectangle2(vp[0], vp[1], vp[2], vp[3]));
	}

}
