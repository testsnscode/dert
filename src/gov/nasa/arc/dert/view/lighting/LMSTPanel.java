package gov.nasa.arc.dert.view.lighting;

import gov.nasa.arc.dert.scene.World;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Date;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * Provides local mean solar time controls for positioning the solar light in
 * the LightPositionView.
 *
 */
public class LMSTPanel extends JPanel {

	// Time controls
	private JSpinner hour, minute, second, sol;
	private JButton current;

	// Time and date fields
	private int lastSecond, lastMinute;
	private Date selectedDate;
	
	private boolean doSetTime;

	/**
	 * Constructor
	 */
	public LMSTPanel() {
		setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		setLayout(new BorderLayout());

		JPanel mainPanel = new JPanel(new GridLayout(2, 1));
		JPanel solPart = new JPanel(new FlowLayout(FlowLayout.LEFT));
		solPart.add(new JLabel("Sol", SwingConstants.RIGHT));
		SpinnerNumberModel model = new SpinnerNumberModel(new Integer(1), new Integer(1),
			new Integer(Integer.MAX_VALUE), new Integer(1));
		sol = new JSpinner(model);
		sol.setSize(100, -1);
		sol.setToolTipText("sol (starts at 1)");
		sol.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent event) {
				if (doSetTime)
					World.getInstance().setTime(getSelectedDate().getTime());
			}
		});
		solPart.add(sol);

		mainPanel.add(solPart);

		JPanel timePart = new JPanel();
		timePart.setLayout(new FlowLayout());

		JLabel label = new JLabel("H:M:S ");
		timePart.add(label);
		hour = new JSpinner(new SpinnerNumberModel(new Integer(0), new Integer(0), new Integer(23), new Integer(1)));
		timePart.add(hour);
		hour.setToolTipText("hours (0-23)");
		hour.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent event) {
				if (doSetTime)
					World.getInstance().setTime(getSelectedDate().getTime());
			}
		});
		minute = new JSpinner(new SpinnerNumberModel(new Integer(0), new Integer(0), new Integer(59), new Integer(1)));
		timePart.add(minute);
		minute.setToolTipText("minutes (0-59)");
		minute.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent event) {
				if ((Integer) minute.getValue() == 0) {
					if (lastMinute == 59) {
						hour.setValue(new Integer((Integer) hour.getValue() + 1));
					}
				} else if ((Integer) minute.getValue() == 59) {
					if (lastMinute == 0) {
						hour.setValue(new Integer((Integer) hour.getValue() - 1));
					}
				}
				lastMinute = (Integer) minute.getValue();
				if (doSetTime)
					World.getInstance().setTime(getSelectedDate().getTime());
			}
		});
		second = new JSpinner(new SpinnerNumberModel(new Integer(0), new Integer(0), new Integer(59), new Integer(1)));
		timePart.add(second);
		second.setToolTipText("seconds (0-59)");
		second.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent event) {
				if ((Integer) second.getValue() == 0) {
					if (lastSecond == 59) {
						minute.setValue(new Integer((Integer) minute.getValue() + 1));
					}
				} else if ((Integer) second.getValue() == 59) {
					if (lastSecond == 0) {
						minute.setValue(new Integer((Integer) minute.getValue() - 1));
					}
				}
				lastSecond = (Integer) second.getValue();
				if (doSetTime)
					World.getInstance().setTime(getSelectedDate().getTime());
			}
		});
		current = new JButton();
		timePart.add(current);
		current.setText("Now");
		current.setToolTipText("set to current date and time");
		current.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
				doSetTime = false;
				setCurrentDate(new Date());
				doSetTime = true;
				World.getInstance().setTime(getSelectedDate().getTime());
			}
		});

		mainPanel.add(timePart);

		add(mainPanel, BorderLayout.NORTH);

		selectedDate = new Date(World.getInstance().getTime());
		setCurrentDate(selectedDate);
		doSetTime = true;

	}

	/**
	 * Set the current date
	 * 
	 * @param currentDate
	 */
	public void setCurrentDate(Date currentDate) {
		selectedDate = currentDate;
		int[] lmst = World.getInstance().getLighting().dateToLMST(selectedDate);
		sol.setValue(lmst[0]);
		lastSecond = lmst[3];
		second.setValue(lastSecond);
		lastMinute = lmst[2];
		minute.setValue(lastMinute);
		int hr = lmst[1];
		hour.setValue(hr);
	}

	private Date getSelectedDate() {
		selectedDate = World
			.getInstance()
			.getLighting()
			.lmstToDate((Integer) sol.getValue(), (Integer) hour.getValue(), (Integer) minute.getValue(),
				(Integer) second.getValue());
		return (selectedDate);
	}

}
