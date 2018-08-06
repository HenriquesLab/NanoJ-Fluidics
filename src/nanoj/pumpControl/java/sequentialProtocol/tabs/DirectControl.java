package nanoj.pumpControl.java.sequentialProtocol.tabs;

import nanoj.pumpControl.java.pumps.PumpManager;
import nanoj.pumpControl.java.pumps.SyringeList;
import nanoj.pumpControl.java.sequentialProtocol.GUI;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.prefs.Preferences;

public class DirectControl extends JPanel {
    private Preferences prefs = Preferences.userRoot().node(this.getClass().getName());
    private GUI gui;

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

        DirectControLayout layout = new DirectControLayout(this);
        setLayout(layout);

        stopPump = new StopPump();

        pumpSelection.addActionListener(new UpdateCurrentPump());
        startPumpButton.addActionListener(new StartPump());
        stopPumpButton.addActionListener(stopPump);
    }

    public void rememberPreferences() {
        prefs.put(TARGET, targetField.getText());
    }

    private class UpdateCurrentPump implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            if (pumpSelection.getSelectedIndex() > -1) gui.pumpManager.setCurrentPump(pumpSelection.getSelectedIndex());
        }
    }

    private class StartPump implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            prefs.put(TARGET, targetField.getText());
            if (gui.pumpManager.isConnected()) {
                try {
                    gui.log.message("" + gui.pumpManager.startPumping(
                            syringeComboBox.getSelectedIndex(),
                            Double.parseDouble(rateText.getText()),
                            Double.parseDouble(targetField.getText()),
                            infuse.isSelected()
                    ));
                } catch (Exception e1) {
                    gui.log.message("Error, problem with starting the pump.");
                    e1.printStackTrace();
                }
            } else gui.log.message("Can't do anything until pump is connected.");

        }
    }

    private class StopPump implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            if (gui.pumpManager.isConnected()) {
                try {
                    int pump = gui.getSequenceManager().getCurrentPump();
                    if (e.getSource().equals(gui.stopPumpOnSeqButton)  && pump != -1) {
                        gui.pumpManager.setCurrentPump(pump);
                        pumpSelection.setSelectedIndex(pump);
                        gui.pumpManager.stopPumping();
                    }
                    else gui.pumpManager.stopPumping();
                    gui.log.message("Told pump to stop!");
                } catch (Exception e1) {
                    gui.log.message("Error, did not properly stop pump.");
                    e1.printStackTrace();
                }
            } else gui.log.message("Can't do anything until pump is connected.");
        }
    }

}
