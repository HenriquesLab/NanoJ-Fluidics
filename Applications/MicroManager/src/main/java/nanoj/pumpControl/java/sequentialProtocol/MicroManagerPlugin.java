package nanoj.pumpControl.java.sequentialProtocol;

import ij.IJ;
import mmcorej.CMMCore;
import org.micromanager.api.MMPlugin;
import org.micromanager.api.ScriptInterface;
import org.micromanager.utils.ReportingUtils;

@SuppressWarnings("unused")
public class MicroManagerPlugin implements MMPlugin {
    @SuppressWarnings("unused")
    public static final String menuName = "NanoJ-Fluidics";
    @SuppressWarnings("unused")
    public static final String tooltipDescription = "Plugin to drive a sequential protocol of microfluidics steps.";
    private CMMCore core;
    private final GUI userInterface = GUI.INSTANCE;

    @Override
    public void dispose() {
        userInterface.dispose();
    }

    @Override
    public void setApp(ScriptInterface app) {
        core = app.getMMCore();
    }

    @Override
    public void show() {
        try {
            userInterface.create(core);
        } catch (Exception e) {
            IJ.log("Error, problem when initiating GUI.");
            ReportingUtils.logError(e);
        }
    }

    @Override
    public String getDescription() {
        return "A plugin to design a protocol of steps to control a set of microfluidics devices in sequence.";
    }

    @Override
    public String getInfo() {
        return "A plugin to design a protocol of steps to control a set of microfluidics devices in sequence.";
    }

    @Override
    public String getVersion() {
        return "0.1";
    }

    @Override
    public String getCopyright() {
        return "(C) 2016 MRC LMCB, UCL";
    }

}