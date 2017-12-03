package gov.nasa.arc.dert.view.viewpoint;

import gov.nasa.arc.dert.icon.Icons;
import gov.nasa.arc.dert.scene.World;
import gov.nasa.arc.dert.scene.tool.Path;
import gov.nasa.arc.dert.state.AnimationState;
import gov.nasa.arc.dert.state.PathState;
import gov.nasa.arc.dert.ui.DoubleTextField;
import gov.nasa.arc.dert.ui.FieldPanel;
import gov.nasa.arc.dert.ui.FileInputField;
import gov.nasa.arc.dert.ui.OptionDialog;
import gov.nasa.arc.dert.util.FileHelper;
import gov.nasa.arc.dert.viewpoint.Animator;
import gov.nasa.arc.dert.viewpoint.FlyThroughParameters;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

/**
 * Provides a dialog for setting fly through animation options.
 *
 */
public class AnimationPanel extends JPanel {

//	private ViewpointController controller;
	private Animator animator;

	// Path to fly through
	private Path path;

	// Controls
	private DoubleTextField heightText;
	private JLabel heightLabel;
	private JButton playButton, pauseButton, stopButton;
	private JSpinner framesSpinner, millisSpinner;
	private JCheckBox loop, grab;
	private JLabel statusField;
	private FileInputField fif;
	private JComboBox subjectBox;
	private DefaultComboBoxModel subjectModel;
	
	// Parameters for fly through animation
	private FlyThroughParameters vplFlyParams;
	private FlyThroughParameters flyParams;
	private boolean paused;

	/**
	 * Constructor
	 * 
	 * @param state
	 */
	public AnimationPanel(AnimationState state) {
		super();
		setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		animator = new Animator(this);
		vplFlyParams = state.getFlyParams();
		
		setLayout(new BorderLayout());

		JPanel controlBar = new JPanel(new FlowLayout(FlowLayout.LEFT));
		playButton = new JButton(Icons.getImageIcon("play.png"));
		playButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
				if (!paused) {
					if (!setParameters())
						return;
				}
				paused = false;
				animator.startFlyThrough(statusField);
			}
		});
		controlBar.add(playButton);
		pauseButton = new JButton(Icons.getImageIcon("pause.png"));
		pauseButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
				animator.pauseFlyThrough();
				paused = !paused;
			}
		});
		controlBar.add(pauseButton);
		stopButton = new JButton(Icons.getImageIcon("stop.png"));
		stopButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
				paused = false;
				animator.stopFlyThrough();
			}
		});
		controlBar.add(stopButton);

		statusField = new JLabel("          ", SwingConstants.LEFT);
		controlBar.add(statusField);
		add(controlBar, BorderLayout.NORTH);

		ArrayList<Component> compList = new ArrayList<Component>();
		
		compList.add(new JLabel("Subject", SwingConstants.RIGHT));
		subjectBox = new JComboBox();
		subjectBox.setEditable(false);
		subjectBox.addPopupMenuListener(new PopupMenuListener() {
			@Override
			public void popupMenuWillBecomeVisible(PopupMenuEvent event) {
				fillSubjectBox();
			}
			@Override
			public void popupMenuWillBecomeInvisible(PopupMenuEvent event) {
				setSubject(subjectBox.getSelectedItem());
			}
			@Override
			public void popupMenuCanceled(PopupMenuEvent event) {
				// nothing here
			}
		});
		subjectModel = new DefaultComboBoxModel();
		fillSubjectBox();
		subjectBox.setModel(subjectModel);
//		subjectBox.addActionListener(new ActionListener() {
//			@Override
//			public void actionPerformed(ActionEvent event) {
//				setSubject(subjectBox.getSelectedItem());
//			}
//		});
		compList.add(subjectBox);

		compList.add(new JLabel("Number of Frames", SwingConstants.RIGHT));
		framesSpinner = new JSpinner(new SpinnerNumberModel(100, 10, 10000, 1));
		compList.add(framesSpinner);

		compList.add(new JLabel("Milliseconds Per Frame", SwingConstants.RIGHT));
		millisSpinner = new JSpinner(new SpinnerNumberModel(100, 10, 100000, 10));
		millisSpinner.setEditor(new JSpinner.NumberEditor(millisSpinner, "###0.###"));
		compList.add(millisSpinner);
		
		heightLabel = new JLabel("Height above waypoint", SwingConstants.RIGHT);
		compList.add(heightLabel);
		heightText = new DoubleTextField(8, 5, false, "0.000");
		compList.add(heightText);

		compList.add(new JLabel("Loop", SwingConstants.RIGHT));
		loop = new JCheckBox("enable");
		loop.setSelected(false);
		compList.add(loop);

		compList.add(new JLabel("Image Sequence", SwingConstants.RIGHT));
		grab = new JCheckBox("create");
		grab.setSelected(false);
		compList.add(grab);
		add(new FieldPanel(compList), BorderLayout.CENTER);
		
		compList = new ArrayList<Component>();
		compList.add(new JLabel("Image Directory", SwingConstants.RIGHT));
		fif = new FileInputField("", "enter directory path for image sequence") {
			@Override
			public void setFile() {
				String sequencePath = FileHelper.getDirectoryPathForSave("Image Sequence Directory");
				if (sequencePath != null)
					fileText.setText(sequencePath);
			}
		};
		compList.add(fif);
		
		add(new FieldPanel(compList), BorderLayout.SOUTH);
		
		setSubject(state.getSubject());
	}
	
	private void fillSubjectBox() {
		subjectModel.removeAllElements();
		subjectModel.addElement("Viewpoint List");
		ArrayList<Path> paths = World.getInstance().getTools().getFlyablePaths();
		for (int i=0; i<paths.size(); ++i)
			subjectModel.addElement(paths.get(i));
	}
	
	private void setSubject(Object subject) {
		if (subject instanceof Path) {
			path = (Path)subject;
			subjectBox.setSelectedItem(subject);
		}
		else {
			path = null;
			subjectBox.setSelectedItem("Viewpoint List");
		}
		paused = false;
		if (path != null) {
			flyParams = ((PathState)path.getState()).flyParams;
		}
		else {
			flyParams = vplFlyParams;
		}
		heightLabel.setEnabled(path != null);
		framesSpinner.getModel().setValue(flyParams.numFrames);
		millisSpinner.getModel().setValue(flyParams.millisPerFrame);
		heightText.setValue(flyParams.pathHeight);
		loop.setSelected(flyParams.loop);
		grab.setSelected(flyParams.grab);
		fif.setFilePath(flyParams.imageSequencePath);
	}

	/**
	 * Make the apply button usable.
	 * 
	 * @param enable
	 */
	public void enableParameters(boolean enable) {
		heightLabel.setEnabled(path != null);
		heightText.setEnabled(enable);
		framesSpinner.setEnabled(enable);
		millisSpinner.setEnabled(enable);
		grab.setEnabled(enable);
		loop.setEnabled(enable);
		fif.setEnabled(enable);
	}
	
	private boolean setParameters() {
		int numFrames = (Integer) framesSpinner.getValue();
		int millis = (Integer) millisSpinner.getValue();
		double height = heightText.getValue();
		if (Double.isNaN(height)) {					
			return(false);
		}
		if (grab.isSelected()) {
			if (fif.getFilePath().isEmpty()) {
				OptionDialog.showErrorMessageDialog((Window)getTopLevelAncestor(), "Please enter a directory for the image sequence.");
				return(false);
			}
		}
		
		flyParams.numFrames = numFrames;
		flyParams.millisPerFrame = millis;
		flyParams.pathHeight = height;
		flyParams.grab = grab.isSelected();
		flyParams.loop = loop.isSelected();
		flyParams.imageSequencePath = fif.getFilePath();
		
		String msg = null;
		if (path == null) {
			msg = animator.flyViewpoints(flyParams);
		} else {
			msg = animator.flyPath(path, flyParams);
		}
		if (msg != null)
			statusField.setText(msg);
		else
			statusField.setText("");
		return(msg == null);
		
	}
	
	public void close() {
		animator.stopFlyThrough();
	}
	
	public FlyThroughParameters getViewpointFlyParams() {
		return(vplFlyParams);
	}
	
	public Object getSubject() {
		return(path);
	}

}
