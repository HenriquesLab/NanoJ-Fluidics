package nanoj.pumpControl.java.sequentialProtocol;

import nanoj.pumpControl.java.pumps.Pump;
import nanoj.pumpControl.java.pumps.Syringe;
import org.micromanager.internal.utils.ReportingUtils;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Observer;

public class Sequence extends ArrayList<Step> {
    private final Observer stepObserver;
    private final Step suckStep;
    private boolean suck;

    public Sequence(Observer changer) {
        super();

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            ReportingUtils.logError(e);
        }

        this.stepObserver = changer;

        this.suckStep = new Step(0, "suck", false,false,10, Step.TimeUnit.SECS, Syringe.PERISTALTIC, 7, Step.VolumeUnit.UL, Pump.Action.Withdraw);
        this.suckStep.setIsSuckStep();

        add(new Step(1, "Start", true, false, 1, Step.TimeUnit.SECS, Syringe.BD50,  0.5, Step.VolumeUnit.ML,Pump.Action.Infuse));
        add(new Step(2, "Middle", true, false,1, Step.TimeUnit.SECS, Syringe.BD10, 1, Step.VolumeUnit.ML,Pump.Action.Infuse));
        add(new Step(3, "Finish", true,false,1, Step.TimeUnit.SECS, Syringe.BD1,100, Step.VolumeUnit.UL,Pump.Action.Infuse));

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