package nanoj.pumpControl.java.sequentialProtocol.tabs;

import nanoj.pumpControl.java.pumps.ConnectedSubPump;
import nanoj.pumpControl.java.pumps.Pump;
import nanoj.pumpControl.java.pumps.PumpManager;
import nanoj.pumpControl.java.pumps.SyringeList;
import nanoj.pumpControl.java.sequentialProtocol.FlowRateSlider;
import nanoj.pumpControl.java.sequentialProtocol.GUI;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Observable;
import java.util.Observer;
import java.util.prefs.Preferences;

public class DirectControl extends JPanel implements Observer, ActionListener {
    private Preferences prefs = Preferences.userRoot().node(this.getClass().getName());
    private PumpManager pumpManager = PumpManager.INSTANCE;
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
    public FlowRateSlider rateSlider;
    JLabel targetLabel;
    JTextField targetVolume;
    JLabel actionLabel;
    JRadioButton infuse;
    JRadioButton withdraw;
    JButton startPumpButton;
    JButton stopPumpButton;

    public StopPump stopPump;

    private boolean editing = false;

    public DirectControl(GUI gui) {
        super();
        this.gui = gui;
        
        pumpStatusLabel = new JLabel("Pump status:");
        pumpStatus = new JLabel("Pump not started.");
        pumpSelectionLabel = new JLabel("Select pump to control: ");
        syringeLabel = new JLabel("Syringe");
        rateLabel = new JLabel("Rate ("+ PumpManager.FLOW_RATE_UNITS+")");
        rateSlider = new FlowRateSlider();
        targetLabel = new JLabel("Target Volume (" + PumpManager.VOLUME_UNITS + ")");
        targetVolume = new JTextField(prefs.get(TARGET, "500"), 6);
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

        setLayout(new DirectControlLayout(this));

        stopPump = new StopPump();

        pumpSelection.addActionListener(this);
        syringeComboBox.addActionListener(this);
        startPumpButton.addActionListener(this);
        stopPumpButton.addActionListener(stopPump);
        gui.stopPumpOnSeqButton.addActionListener(stopPump);

        pumpManager.addObserver(this);
    }

    public void rememberPreferences() {
        prefs.put(TARGET, targetVolume.getText());
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
                editing = true;
                pumpSelection.removeAllItems();
                for (ConnectedSubPump pump: pumpManager.getConnectedPumpsList())
                    pumpSelection.addItem(pump.getFullName());

                editing = false;
            }

            if (pumpManager.noPumpsConnected())
                return;

            rateSlider.setPumpSelection(pumpSelection.getSelectedIndex());
            rateSlider.setSyringeDiameter(SyringeList.getDiameter(syringeComboBox.getSelectedIndex()));
        } else if (arg.equals(PumpManager.NEW_STATUS_AVAILABLE)) {
            pumpStatus.setText(pumpManager.getStatus());
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {

        if (e.getSource().equals(syringeComboBox) && pumpManager.anyPumpsConnected())
            rateSlider.setSyringeDiameter(SyringeList.getDiameter(syringeComboBox.getSelectedIndex()));

        else if (e.getSource().equals(startPumpButton)) {
            int index = pumpSelection.getSelectedIndex();
            prefs.put(TARGET, targetVolume.getText());
            if (gui.pumpManager.isConnected(index)) {
                try {
                    Pump.Action action;
                    if (infuse.isSelected())
                        action = Pump.Action.Infuse;
                    else action = Pump.Action.Withdraw;
                    rateSlider.setPumpSelection(index);
                    rateSlider.setSyringeDiameter(SyringeList.getDiameter(syringeComboBox.getSelectedIndex()));
                    gui.log.message("" + gui.pumpManager.startPumping(
                            index,
                            syringeComboBox.getSelectedIndex(),
                            rateSlider.getCurrentFlowRate(),
                            Double.parseDouble(targetVolume.getText()),
                            action
                    ));
                } catch (Exception e1) {
                    gui.log.message("Error, problem with starting the pump.");
                    e1.printStackTrace();
                }
            } else gui.log.message("Can't do anything until pump is connected.");
        }

        else if (e.getSource().equals(pumpSelection)) {
            if (pumpSelection.getItemCount() >= 0 &&
                    pumpManager.anyPumpsConnected() &&
                    !editing)
                rateSlider.setPumpSelection(pumpSelection.getSelectedIndex());
        }

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
