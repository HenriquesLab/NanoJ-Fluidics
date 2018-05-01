package nanoj.pumpControl.java.sequentialProtocol;

import nanoj.pumpControl.java.pumps.PumpManager;
import org.micromanager.utils.ReportingUtils;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Observer;

public class Sequence extends ArrayList<Step> {
    private Observer stepObserver;
    private String[] NO_PUMPS = new String[]{PumpManager.NO_PUMP_CONNECTED};
    private Step suckStep;
    private boolean suck;

    Sequence(Observer changer) {
        super();

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            ReportingUtils.logError(e);
        }

        this.stepObserver = changer;

        this.suckStep = new Step(0, "suck", false,10, 0, 0, "7", 0,1,NO_PUMPS);
        this.suckStep.setIsSuckStep();

        add(new Step(1, "Start", true, 1, 0, 0,  "0.5", 1,0, NO_PUMPS));
        add(new Step(2, "Middle", true, 1, 0, 1, "1", 1,0, NO_PUMPS));
        add(new Step(3, "Finish", true,1, 0, 2,"100", 0,0, NO_PUMPS));

    }

    public boolean isSuckTrue() {
        return suck;
    }

    public void setSuck(boolean suck) {
        this.suck = suck;
    }

    public Step getSuckStep() {
        return suckStep;
    }

    @Override
    public void add(int index, Step element) {
        super.add(index, element);
        element.addObserver(stepObserver);
    }

    @Override
    public boolean add(Step step) {
        step.addObserver(stepObserver);
        return super.add(step);
    }
}