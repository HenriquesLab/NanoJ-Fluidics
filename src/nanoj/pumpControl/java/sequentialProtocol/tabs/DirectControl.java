package nanoj.pumpControl.java.sequentialProtocol.tabs;

import nanoj.pumpControl.java.pumps.ConnectedSubPump;
import nanoj.pumpControl.java.pumps.Pump;
import nanoj.pumpControl.java.pumps.PumpManager;
import nanoj.pumpControl.java.pumps.SyringeList;
import nanoj.pumpControl.java.sequentialProtocol.GUI;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Observable;
import java.util.Observer;
import java.util.prefs.Preferences;

public class DirectControl extends JPanel implements Observer, ActionListener, ChangeListener {
    private Preferences prefs = Preferences.userRoot().node(this.getClass().getName());
    private GUI gui;
    private PumpManager pumpManager = PumpManager.INSTANCE;

    public String name = "Direct Pump Control";

    private static final String TARGET = "target";
    
    JLabel pumpStatusLabel;
    public JLabel pumpStatus;
    JLabel pumpSelectionLabel;
    public JComboBox pumpSelection;
    JLabel syringeLabel;
    public JComboBox syringeComboBox;
    JLabel rateLabel;
    public JSlider rateSlider;
    public JLabel rateText;
    JLabel targetLabel;
    JTextField targetField;
    JLabel actionLabel;
    JRadioButton infuse;
    JRadioButton withdraw;
    JButton startPumpButton;
    JButton stopPumpButton;

    public StopPump stopPump;
    
    public DirectControl(GUI gui) {
        super();
        this.gui = gui;
        
        pumpStatusLabel = new JLabel("Pump status:");
        pumpStatus = new JLabel("Pump not started.");
        pumpSelectionLabel = new JLabel("Select pump to control: ");
        syringeLabel = new JLabel("Syringe");
        rateLabel = new JLabel("Rate ("+ PumpManager.FLOW_RATE_UNITS+")");
        rateSlider = new JSlider(JSlider.HORIZONTAL, Integer.MIN_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE);
        rateText = new JLabel("");
        targetLabel = new JLabel("Target Volume (" + PumpManager.VOLUME_UNITS + ")");
        targetField = new JTextField(prefs.get(TARGET, "500"), 6);
        actionLabel = new JLabel("Action to Perform");
        infuse = new JRadioButton("Infuse", true);
        withdraw = new JRadioButton("Withdraw");
        ButtonGroup buttons = new ButtonGroup();
        startPumpButton = new JButton("Pump!");
        stopPumpButton = new JButton("Stop!");

        syringeComboBox = new JComboBox(SyringeList.getBrandedNames(0));
        pumpSelection = new JComboBox(new String[]{PumpManager.NO_PUMP_CONNECTED});
        buttons.add(infuse);
        buttons.add(withdraw);

        setLayout(new DirectControLayout(this));

        stopPump = new StopPump();

        syringeComboBox.addActionListener(this);
        startPumpButton.addActionListener(this);
        stopPumpButton.addActionListener(stopPump);
        rateSlider.addChangeListener(this);
    }

    public void rememberPreferences() {
        prefs.put(TARGET, targetField.getText());
    }

    private void updateFlowRateInformation() {
        double[] newInformation = pumpManager.getMaxMin(
                pumpSelection.getSelectedIndex(),
                SyringeList.getDiameter(syringeComboBox.getSelectedIndex()));

        double syringeMin = newInformation[1];
        double syringeRate = (newInformation[0] - newInformation[1]);

        double sliderValue = (double) rateSlider.getValue() + (double) Integer.MAX_VALUE + 1;
        sliderValue = sliderValue/ ((2*((double) Integer.MAX_VALUE)) +1);

        double rate = (syringeRate*sliderValue)+syringeMin;
        rateText.setText("" + new BigDecimal(rate).setScale(3, RoundingMode.HALF_EVEN).toPlainString());

    }

    @Override
    public void update(Observable o, Object arg) {
        if (arg.equals(PumpManager.NEW_PUMP_CONNECTED) ||
            arg.equals(PumpManager.PUMP_DISCONNECTED)) {
            if (pumpManager.noPumpsConnected()) {
                pumpSelection.removeAllItems();
                pumpSelection.addItem(PumpManager.NO_PUMP_CONNECTED);
            }
            else {
                pumpSelection.removeAllItems();
                for (ConnectedSubPump pump: pumpManager.getConnectedPumpsList())
                    pumpSelection.addItem(pump.getFullName());

            }

            if (pumpManager.noPumpsConnected())
                return;

            updateFlowRateInformation();
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {

        if (e.getSource().equals(syringeComboBox) && pumpManager.anyPumpsConnected())
            updateFlowRateInformation();

        else if (e.getSource().equals(startPumpButton)) {
            int index = pumpSelection.getSelectedIndex();
            prefs.put(TARGET, targetField.getText());
            if (gui.pumpManager.isConnected(index)) {
                try {
                    Pump.Action action;
                    if (infuse.isSelected())
                        action = Pump.Action.Infuse;
                    else action = Pump.Action.Withdraw;
                    gui.log.message("" + gui.pumpManager.startPumping(
                            index,
                            syringeComboBox.getSelectedIndex(),
                            Double.parseDouble(rateText.getText()),
                            Double.parseDouble(targetField.getText()),
                            action
                    ));
                } catch (Exception e1) {
                    gui.log.message("Error, problem with starting the pump.");
                    e1.printStackTrace();
                }
            } else gui.log.message("Can't do anything until pump is connected.");
        }

    }

    @Override
    public void stateChanged(ChangeEvent e) {
        if (e.getSource().equals(rateSlider) && pumpManager.anyPumpsConnected())
            updateFlowRateInformation();
    }

    private class StopPump implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            try {
                int pump = gui.getSequenceManager().getCurrentPump();
                if (e.getSource().equals(gui.stopPumpOnSeqButton)  && pump != -1) {
                    if (!pumpManager.isConnected(pump)) {
                        gui.log.message("Can't do anything until pump is connected.");
                        return;
                    }
                    pumpSelection.setSelectedIndex(pump);
                    gui.pumpManager.stopPumping(pump);
                }
                else {
                    pump = pumpSelection.getSelectedIndex();
                    if (!pumpManager.isConnected(pump)) {
                        gui.log.message("Can't do anything until pump is connected.");
                    }
                    gui.pumpManager.stopPumping(pump);
                }
                gui.log.message("Told pump to stop!");
            } catch (Exception e1) {
                gui.log.message("Error, did not properly stop pump.");
                e1.printStackTrace();
            }
        }
    }

}
