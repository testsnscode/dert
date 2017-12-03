package gov.nasa.arc.dert.ui;

import gov.nasa.arc.dert.icon.Icons;

import java.awt.Component;
import java.awt.Container;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.filechooser.FileFilter;

/**
 * A JFileChooser for Landscapes.
 *
 */
public class DertFileChooser extends JFileChooser {

	/**
	 * Filter all file except directories.
	 * 
	 * @author lkeelyme
	 *
	 */
	public class ConfigFileFilter extends FileFilter {

		public ConfigFileFilter() {
		}

		@Override
		public String getDescription() {
			return ("");
		}

		@Override
		public boolean accept(File f) {
			if (!f.isDirectory()) {
				return (false);
			}
			return (true);
		}
	}

	private boolean directoryOnly;
	protected JDialog theDialog;

	/**
	 * Constructor
	 * 
	 * @param lastPath
	 * @param directoryOnly
	 * @param newLandscape
	 */
	public DertFileChooser(String lastPath, boolean dirOnly) {
		super(new File(lastPath));
		this.directoryOnly = dirOnly;
		setMultiSelectionEnabled(false);

		if (directoryOnly) {
			setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			setFileFilter(new ConfigFileFilter());
			removeFileType(getComponents());
		}
	}

	@Override
	public Icon getIcon(File f) {
		// use landscape icon
		File dertFile = new File(f, ".landscape");
		if (dertFile.exists()) {
			return (Icons.getImageIcon("landscape-icon.png"));
		}
		return (super.getIcon(f));
	}

	private void removeFileType(Component[] child) {
		for (int i = 0; i < child.length; ++i) {
			if (child[i] instanceof JLabel) {
				JLabel label = (JLabel) child[i];
				if (label.getText().equals("File Format:")) {
					child[i].getParent().getParent().remove(child[i].getParent());
				}
			} else if (child[i] instanceof Container) {
				removeFileType(((Container) child[i]).getComponents());
			}
		}
	}

	public void addNewDirectoryButton(Container parent) {
		Component[] child = null;
		if (parent == null)
			child = getComponents();
		else
			child = parent.getComponents();
		for (int i = 0; i < child.length; ++i) {
			if (child[i] instanceof JButton) {
				JButton button = (JButton) child[i];
				String s = button.getText();
				if ((s != null) && s.equals("Cancel")) {
					JButton nd = new JButton("New Directory");
					nd.addActionListener(new ActionListener() {
						@Override
						public void actionPerformed(ActionEvent event) {
							String str = OptionDialog.showSingleInputDialog((Window)getTopLevelAncestor(), "Please enter the directory name (no spaces).", "");
							if (str == null) {
								return;
							}
							str = str.trim();
							if (!str.isEmpty()) {
								try {
									File file = new File(getCurrentDirectory(), str.trim());
									file.mkdirs();
									rescanCurrentDirectory();
									setSelectedFiles(new File[] { file });
								}
								catch (Exception e) {
									OptionDialog.showErrorMessageDialog((Window)getTopLevelAncestor(), "Error creating directory "+str+".");
									e.printStackTrace();
								}
							}
							else {
								OptionDialog.showErrorMessageDialog((Window)getTopLevelAncestor(), "Invalid directory name.");
							}
						}
					});
					button.getParent().add(nd, 0);
				}
			} else if ((child[i] != null) && (child[i] instanceof Container)) {
				addNewDirectoryButton((Container)child[i]);
			}
		}
	}

}
