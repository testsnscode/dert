package gov.nasa.arc.dert.view.surfaceandlayers;

import gov.nasa.arc.dert.icon.Icons;
import gov.nasa.arc.dert.landscape.Landscape;
import gov.nasa.arc.dert.terrain.LayerInfo;
import gov.nasa.arc.dert.terrain.LayerManager;
import gov.nasa.arc.dert.terrain.LayerInfo.LayerType;
import gov.nasa.arc.dert.ui.IntSpinner;

import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;

import com.ardor3d.scenegraph.event.DirtyType;

/**
 * Provides the layer controls for the SurfaceAndLayersView.
 *
 */
public class LayerPanel extends JPanel {
	
	// Lock icon
	protected static ImageIcon lockedIcon = Icons.getImageIcon("locked.png");
	protected static ImageIcon unlockedIcon = Icons.getImageIcon("unlocked.png");

	// Controls
	private JLabel layerLabel;
	private IntSpinner opacity;
	private JCheckBox lockBox;
	private JCheckBox showBox;
	private int index;
	
	private LayersPanel parent;

	/**
	 * Constructor
	 * 
	 * @param s
	 */
	public LayerPanel(LayersPanel parent, int index) {
		this.index = index;
		this.parent = parent;
		setLayout(new FlowLayout(FlowLayout.LEFT));
		add(new JLabel(" " + (index+1) + " "));
		addShowBox();
		addLockBox();
		addOpacitySpinner();
		layerLabel = new JLabel();
		add(layerLabel);
	}

	public void set(LayerInfo layerInfo) {
		String str = layerInfo.toString();
		if (layerInfo.type != LayerType.none)
			str += ", "+layerInfo.type;
		if (layerInfo.colorMap != null)
			str += ", colormap=" + layerInfo.colorMap.getName();
		layerLabel.setText(str);
		showBox.setSelected(layerInfo.show == 1);
		showBox.setEnabled(layerInfo.type != LayerType.none);
		lockBox.setSelected(!layerInfo.autoblend);
		lockBox.setEnabled(layerInfo.type != LayerType.none);
		opacity.setValueNoChange((int)(layerInfo.opacity*100));
		opacity.setEnabled(layerInfo.type != LayerType.none);
	}
	
	public void setOpacity(int value) {
		opacity.setValueNoChange(value);
	}
	
	private void addShowBox() {
		showBox = new JCheckBox();
		showBox.setToolTipText("Layer is visible");
		showBox.setSelected(true);
		showBox.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
				LayerManager layerManager = Landscape.getInstance().getLayerManager();
				LayerInfo layerInfo = layerManager.getVisibleLayers().get(index);
				layerInfo.show = showBox.isSelected() ? 1 : 0;
				layerManager.setLayerBlendFactor(index, (float)layerInfo.opacity);
				Landscape.getInstance().markDirty(DirtyType.RenderState);
			}
		});
		add(showBox);
	}
	
	private void addLockBox() {
		lockBox = new JCheckBox(unlockedIcon);
		lockBox.setToolTipText("unlock for automatic opacity adjustment for this layer");
		lockBox.setSelectedIcon(lockedIcon);
		if (index == 0) {
			lockBox.setSelected(true);
			lockBox.setEnabled(false);			
		}
		else {
			lockBox.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent event) {
					LayerManager layerManager = Landscape.getInstance().getLayerManager();
					LayerInfo layerInfo = layerManager.getVisibleLayers().get(index);
					layerInfo.autoblend = !lockBox.isSelected();
				}
			});
		}		
		add(lockBox);
	}

	private void addOpacitySpinner() {
		opacity = new IntSpinner(0, 0, 100, 1, false, "###") {
			@Override
			public void stateChanged(ChangeEvent event) {
				int value = (Integer) getValue();
				double blendValue = value/100.0;
				LayerManager layerManager = Landscape.getInstance().getLayerManager();
				layerManager.setLayerBlendFactor(index, (float) blendValue);
				parent.adjustBlendFactors(value-lastValue, index);
				Landscape.getInstance().markDirty(DirtyType.RenderState);
				lastValue = value;
			}
		};
		opacity.setToolTipText("percent opacity of this layer");
		add(opacity);
	}

}
