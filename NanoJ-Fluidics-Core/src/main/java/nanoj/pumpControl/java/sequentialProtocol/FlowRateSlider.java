package nanoj.pumpControl.java.sequentialProtocol;

import nanoj.pumpControl.java.pumps.PumpManager;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.math.BigDecimal;
import java.math.RoundingMode;

import static nanoj.pumpControl.java.pumps.ConnectedSubPumpsList.PumpIndexNotFound;
import static nanoj.pumpControl.java.pumps.ConnectedSubPumpsList.PumpNotFoundException;

public class FlowRateSlider extends JPanel implements ChangeListener {
    private final PumpManager pumpManager = PumpManager.INSTANCE;
    double syringeDiameter = 1;
    int pumpSelection = 0;

    private final JSlider slider = new JSlider(JSlider.HORIZONTAL,
            Integer.MIN_VALUE,
            Integer.MAX_VALUE,
            Integer.MAX_VALUE);
    private final JLabel text = new JLabel("0.0");

    public FlowRateSlider() {
        super();

        setLayout(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();

        constraints.weightx = 0.1;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        add(text, constraints);

        constraints.weightx = 0.9;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        add(slider,constraints);

        slider.addChangeListener(this);
    }

    public void setFlowRate(int value) {
        slider.setValue(value);
    }

    public void setSyringeDiameter(double syringeDiameter) {
        this.syringeDiameter = syringeDiameter;
        update();
    }

    public void setPumpSelection(int pumpSelection) {
        this.pumpSelection = pumpSelection;
        update();
    }

    public double getCurrentFlowRate() {
        return Double.parseDouble(text.getText());
    }

    public int getSliderValue() {
        return slider.getValue();
    }

    private void update() {
        double[] newInformation;
        try {
            newInformation = pumpManager.getMaxMin(
                    pumpSelection,
                    syringeDiameter);
        } catch (PumpNotFoundException | PumpIndexNotFound e) {
            GUI.Log.INSTANCE.message("Error updating flow-rate for given pump");
            e.printStackTrace();
            return;
        }

        double syringeMin = newInformation[1];
        double syringeRate = (newInformation[0] - newInformation[1]);

        double sliderValue = (double) slider.getValue() + (double) Integer.MAX_VALUE + 1;
        sliderValue = sliderValue/ ((2*((double) Integer.MAX_VALUE)) +1);

        double rate = (syringeRate*sliderValue)+syringeMin;
        text.setText("" + new BigDecimal(rate).setScale(3, RoundingMode.HALF_EVEN).toPlainString());
    }

    @Override
    public void stateChanged(ChangeEvent e) {
        if (pumpManager.noPumpsConnected() || !pumpManager.isConnected(pumpSelection))
            return;

        update();
    }

}
