package gov.nasa.arc.dert.view.mapelement;

import gov.nasa.arc.dert.scene.MapElement;
import gov.nasa.arc.dert.scene.tool.fieldcamera.FieldCamera;
import gov.nasa.arc.dert.scene.tool.fieldcamera.FieldCameraInfoManager;
import gov.nasa.arc.dert.view.fieldcamera.FieldCameraView;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.SwingConstants;

/**
 * Provides controls for setting options for field cameras.
 *
 */
public class FieldCameraPanel extends MapElementBasePanel {

	// Controls
	private JCheckBox fovCheckBox, lineCheckBox, crosshairCheckBox;
	private JComboBox defCombo;

	// FieldCamera
	private FieldCamera fieldCamera;

	/**
	 * Constructor
	 * 
	 * @param parent
	 */
	public FieldCameraPanel(MapElement mapElement) {
		super(mapElement);
	}

	@Override
	protected void addFields(ArrayList<Component> compList) {
		super.addFields(compList);
		fieldCamera = (FieldCamera)mapElement;
		
		compList.add(new JLabel("Definition", SwingConstants.RIGHT));
		String[] names = FieldCameraInfoManager.getInstance().getFieldCameraNames();
		defCombo = new JComboBox(names);
		defCombo.setToolTipText("select camera definition file");
		defCombo.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
				String fieldCameraDef = (String) defCombo.getSelectedItem();
				if (fieldCamera.setFieldCameraDefinition(FieldCameraInfoManager.getInstance().getFieldCameraInfo(fieldCameraDef))) {
					FieldCameraView view = (FieldCameraView)fieldCamera.getState().getViewData().getView();
					if (view != null)
						view.setRange(fieldCamera.getFieldCameraInfo());					
				}
			}
		});
		compList.add(defCombo);

		compList.add(new JLabel("Crosshair", SwingConstants.RIGHT));
		crosshairCheckBox = new JCheckBox("visible");
		crosshairCheckBox.setSelected(fieldCamera.getSyntheticCameraNode().isCrosshairVisible());
		crosshairCheckBox.setToolTipText("display crosshair");
		crosshairCheckBox.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
				fieldCamera.getSyntheticCameraNode().setCrosshairVisible(crosshairCheckBox.isSelected());
			}
		});
		compList.add(crosshairCheckBox);

		compList.add(new JLabel("Field of View", SwingConstants.RIGHT));
		fovCheckBox = new JCheckBox("visible");
		fovCheckBox.setToolTipText("display the field of view frustum");
		fovCheckBox.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
				fieldCamera.getSyntheticCameraNode().setFovVisible(fovCheckBox.isSelected());
			}
		});
		compList.add(fovCheckBox);

		compList.add(new JLabel("Site Line", SwingConstants.RIGHT));
		lineCheckBox = new JCheckBox("visible");
		lineCheckBox.setToolTipText("display a line to the lookat point");
		lineCheckBox.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
				fieldCamera.getSyntheticCameraNode().setSiteLineVisible(lineCheckBox.isSelected());
			}
		});
		compList.add(lineCheckBox);
	}

	@Override
	public void setMapElement(MapElement mapElement) {
		super.setMapElement(mapElement);
		fieldCamera = (FieldCamera) mapElement;
		setLocation(locationText, locLabel, fieldCamera.getTranslation());
		defCombo.setSelectedItem(fieldCamera.getFieldCameraDefinition());
		fovCheckBox.setSelected(fieldCamera.getSyntheticCameraNode().isFovVisible());
		lineCheckBox.setSelected(fieldCamera.getSyntheticCameraNode().isSiteLineVisible());
		crosshairCheckBox.setSelected(fieldCamera.getSyntheticCameraNode().isCrosshairVisible());
	}

}
