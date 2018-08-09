package nanoj.pumpControl.java.sequentialProtocol.tabs;

import nanoj.pumpControl.java.sequentialProtocol.GUI;

import javax.swing.*;
import java.util.prefs.Preferences;

public class PumpCalibration extends JPanel {
    private Preferences prefs = Preferences.userRoot().node(this.getClass().getName());
    private GUI gui;
    public String name = "Pump Calibration";

    PumpCalibration(GUI gui) {
        super();

        this.gui = gui;


        setLayout(new PumpCalibrationLayout(this));
    }
}
