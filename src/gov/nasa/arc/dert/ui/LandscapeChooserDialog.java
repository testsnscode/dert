package gov.nasa.arc.dert.ui;

import gov.nasa.arc.dert.Dert;
import gov.nasa.arc.dert.view.Console;

import java.awt.BorderLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileOutputStream;
import java.util.Properties;

import javax.swing.JButton;
import javax.swing.JFileChooser;

/**
 * Provides a dialog for choosing a DERT configuration or a landscape.
 *
 */
public class LandscapeChooserDialog extends AbstractDialog {

	// The last directory chosen
	protected static String lastPath = System.getProperty("user.dir");

	// File chooser panel
	private DertFileChooser fileChooser;

	// Paths
	private String landscapePath;

	/**
	 * Constructor
	 * 
	 * @param vrsn
	 * @param del
	 */
	public LandscapeChooserDialog(String path) {
		super(Dert.getMainWindow(), "Select Landscape", true, false);
		width = 600;
		height = 400;
		if (path != null) {
			File dotFile = new File(path, ".landscape");
			if (dotFile.exists())
				path = new File(path).getParent();
			lastPath = path;
		}
	}

	@Override
	protected void build() {
		super.build();
		contentArea.setLayout(new BorderLayout());

		// Landscape file chooser
		fileChooser = new DertFileChooser(lastPath, true);
		fileChooser.setControlButtonsAreShown(false);
		contentArea.add(fileChooser, BorderLayout.CENTER);
		addNewLandscapeButton();
		okButton.setEnabled(false);

		fileChooser.addPropertyChangeListener(new PropertyChangeListener() {
			/**
			 * A selection was made in the file chooser.
			 */
			@Override
			public void propertyChange(PropertyChangeEvent event) {
				landscapePath = null;
				
				// double click
				if (event.getPropertyName().equals(JFileChooser.DIRECTORY_CHANGED_PROPERTY)) {
					File f = (File) event.getNewValue();
					if (f == null) {
						return;
					}
					landscapePath = f.getAbsolutePath();
					// Check if the selection is a landscape directory.
					// If so, the user has double-clicked on the landscape so we will return that landscape.
					File idFile = new File(f, ".landscape");
					if (idFile.exists()) {
						lastPath = f.getParent();
						close();
					}
					return;
				}
				
				// single click
				if (!event.getPropertyName().equals(JFileChooser.SELECTED_FILE_CHANGED_PROPERTY)) {
					return;
				}
				File f = (File) event.getNewValue();
				if (f == null) {
					return;
				}

				// check if the selection is a landscape directory
				File idFile = new File(f, ".landscape");
				if (!idFile.exists()) {
					return;
				}
				landscapePath = f.getAbsolutePath();
				okButton.setEnabled(true);

			}
		});
	}

	private void addNewLandscapeButton() {
		JButton nl = new JButton("New Landscape");
		nl.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
				String str = OptionDialog.showSingleInputDialog((Window)buttonsPanel.getTopLevelAncestor(), "Please enter the landscape name (no spaces).", "");
				if (str == null) {
					return;
				}
				str = str.trim();
				File file = null;
				if (!str.isEmpty()) {
					try {
						file = new File(fileChooser.getCurrentDirectory(), str);
						file.mkdirs();
						fileChooser.rescanCurrentDirectory();
// Can't seem to get the following line to work
//						fileChooser.setSelectedFile(file);
					}
					catch (Exception e) {
						OptionDialog.showErrorMessageDialog((Window)fileChooser.getTopLevelAncestor(), "Error creating landscape "+str+".");
						e.printStackTrace();
					}
				}
				else {
					OptionDialog.showErrorMessageDialog((Window)fileChooser.getTopLevelAncestor(), "Invalid landscape name.");
				}
				if (file == null)
					return;

				// add landscape identifier
				Properties landscapeProperties = new Properties();
				File propFile = new File(file, ".landscape");
				landscapeProperties.setProperty("LastWrite", System.getProperty("user.name"));
				try {
					landscapeProperties.store(new FileOutputStream(propFile), null);
				} catch (Exception e) {
					Console.println("Error creating landscape "+file);
					e.printStackTrace();
				}
			}
		});
		buttonsPanel.add(nl, 0);
	}

	/**
	 * User made a selection.
	 */
	@Override
	public boolean okPressed() {
		setLastPath(fileChooser.getCurrentDirectory().getAbsolutePath());
		return (landscapePath != null);
	}
	
	@Override
	public boolean cancelPressed() {
		landscapePath = null;
		return(super.cancelPressed());
	}

	public String getLandscape() {
		return (landscapePath);
	}

	private static void setLastPath(String path) {
		lastPath = path;
	}
	
	public String getLastFilePath() {
		return(lastPath);
	}

}
