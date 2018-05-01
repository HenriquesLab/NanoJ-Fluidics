package nanoj.pumpControl.java.sequentialProtocol;

import ij.IJ;
import org.micromanager.MMStudio;
import org.micromanager.api.MMPlugin;
import org.micromanager.api.ScriptInterface;

public class MicroManagerPlugin implements MMPlugin {
    public static final java.lang.String menuName = "Sequential Protocol Plugin";
    public static final java.lang.String tooltipDescription = "" +
            "Plugin to drive a sequential protocol of microfluidics steps.";
    private mmcorej.CMMCore core;
    private org.micromanager.MMStudio app;
    private GUI userInterface = GUI.INSTANCE;

    @Override
    public void dispose() {
        userInterface.dispose();
    }

    @Override
    public void setApp(ScriptInterface app) {
        this.app = (MMStudio) app;
        core = app.getMMCore();
    }

    @Override
    public void show() {
        try {
            userInterface.create(core);
        } catch (Exception e) {
            IJ.log("Error, problem when initiating GUI.");
            e.printStackTrace();
        }
    }

    @Override
    public java.lang.String getDescription() {
        return "A plugin to design a protocol of steps to control a set of microfluidics devices in sequence.";
    }

    @Override
    public java.lang.String getInfo() {
        return "A plugin to design a protocol of steps to control a set of microfluidics devices in sequence.";
    }

    @Override
    public java.lang.String getVersion() {
        return "0.1";
    }

    @Override
    public java.lang.String getCopyright() {
        return "(C) 2016 MRC LMCB, UCL";
    }

}