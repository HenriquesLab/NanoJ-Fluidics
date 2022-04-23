package nanoj.pumpControl.java.sequentialProtocol.tabs;

import nanoj.pumpControl.java.pumps.ConnectedSubPump;
import nanoj.pumpControl.java.pumps.Pump;
import nanoj.pumpControl.java.pumps.PumpManager;
import nanoj.pumpControl.java.pumps.Syringe;
import nanoj.pumpControl.java.sequentialProtocol.FlowRateSlider;
import nanoj.pumpControl.java.sequentialProtocol.GUI;
import nanoj.pumpControl.java.sequentialProtocol.StopButton;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Observable;
import java.util.Observer;
import java.util.prefs.Preferences;

public class DirectControl extends JPanel implements Observer, ActionListener {
    private final Preferences preferences = Preferences.userRoot().node(this.getClass().getName());
    private final PumpManager pumpManager = PumpManager.INSTANCE;
    private final GUI gui;

    public static final String TAB_NAME = "Direct Pump Control";

    private static final String TARGET = "target";
    
    final JLabel pumpStatusLabel = new JLabel("Pump status:");
    final JLabel pumpStatus = new JLabel("Pump not started.");
    final JLabel pumpSelectionLabel = new JLabel("Select pump to control: ");
    final JComboBox<String> pumpSelection;
    final JLabel syringeLabel = new JLabel("Syringe");
    final JComboBox<String> syringeComboBox;
    final JLabel rateLabel = new JLabel("Rate ("+ PumpManager.FLOW_RATE_UNITS+")");
    final FlowRateSlider rateSlider = new FlowRateSlider();
    final JLabel targetLabel = new JLabel("Target Volume (" + PumpManager.VOLUME_UNITS + ")");
    final JTextField targetVolume = new JTextField(preferences.get(TARGET, "500"), 6);
    final JLabel actionLabel = new JLabel("Action to Perform");
    final JRadioButton infuse = new JRadioButton("Infuse", true);
    final JRadioButton withdraw = new JRadioButton("Withdraw");
    final JButton startPumpButton = new JButton("Pump!");
    final StopButton stopPumpButton;

    private boolean editing = false;

    public DirectControl(GUI gui) {
        super();
        this.gui = gui;

        ButtonGroup buttons = new ButtonGroup();

        syringeComboBox = new JComboBox<>(Syringe.getAllBrandedNames());
        pumpSelection = new JComboBox<>(new String[]{PumpManager.NO_PUMP_CONNECTED});
        buttons.add(infuse);
        buttons.add(withdraw);

        stopPumpButton = new StopButton(gui,pumpSelection);

        setLayout(new DirectControlLayout(this));

        pumpSelection.addActionListener(this);
        syringeComboBox.addActionListener(this);
        startPumpButton.addActionListener(this);

        pumpManager.addObserver(this);
    }

    public void rememberPreferences() {
        preferences.put(TARGET, targetVolume.getText());
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
            rateSlider.setSyringeDiameter(Syringe.values()[syringeComboBox.getSelectedIndex()].diameter);
        } else if (arg.equals(PumpManager.NEW_STATUS_AVAILABLE)) {
            pumpStatus.setText(pumpManager.getStatus());
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {

        if (e.getSource().equals(syringeComboBox) && pumpManager.anyPumpsConnected())
            rateSlider.setSyringeDiameter(Syringe.values()[syringeComboBox.getSelectedIndex()].diameter);

        else if (e.getSource().equals(startPumpButton)) {
            int index = pumpSelection.getSelectedIndex();
            preferences.put(TARGET, targetVolume.getText());
            if (gui.pumpManager.isConnected(index)) {
                try {
                    Pump.Action action;
                    if (infuse.isSelected())
                        action = Pump.Action.Infuse;
                    else action = Pump.Action.Withdraw;
                    rateSlider.setPumpSelection(index);
                    rateSlider.setSyringeDiameter(Syringe.values()[syringeComboBox.getSelectedIndex()].diameter);
                    double volume = Double.parseDouble(targetVolume.getText());
                    gui.log.message("" + gui.pumpManager.startPumping(
                            index,
                            Syringe.values()[syringeComboBox.getSelectedIndex()],
                            rateSlider.getCurrentFlowRate(),
                            volume,
                            action
                    ));
                    gui.log.message("Estimated pumping time: " + (volume/rateSlider.getCurrentFlowRate()) + " secs");
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


}
