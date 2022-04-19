package nanoj.pumpControl.java.sequentialProtocol;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class StopButton extends JButton implements ActionListener {
    private JComboBox pumpSelection = null;
    private GUI gui;
    private int pump = -1;

    public StopButton(GUI gui) {
        super("Stop Pump!");
        this.gui = gui;
        this.pumpSelection = null;

        addActionListener(this);
    }

    public StopButton(GUI gui, JComboBox pumpSelection) {
        super("Stop Pump!");
        this.gui = gui;
        this.pumpSelection = pumpSelection;

        addActionListener(this);
    }

    public void setCurrentPump(int currentPump) {
        this.pump = currentPump;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        try {
            if (pumpSelection != null)
                pump = pumpSelection.getSelectedIndex();

            if (!gui.pumpManager.isConnected(pump)) {
                gui.log.message("Can't do anything until pump is connected.");
                return;
            }

            gui.pumpManager.stopPumping(pump);
            gui.log.message("Told pump " + gui.pumpManager.getAllFullNames()[pump] + " to stop!");

        } catch (Exception e1) {
            gui.log.message("Error, did not properly stop pump.");
            e1.printStackTrace();
        }

    }
}
