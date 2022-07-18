package nanoj.pumpControl.java.sequentialProtocol;

import ij.IJ;
import org.micromanager.MenuPlugin;
import org.micromanager.Studio;
import org.micromanager.internal.utils.ReportingUtils;
import org.scijava.command.Command;
import org.scijava.plugin.Plugin;

@SuppressWarnings("unused")
@Plugin(type = Command.class, menuPath = "NanoJ>Fluidics")
public class MicroManagerPlugin implements MenuPlugin {

    private final GUI userInterface = GUI.INSTANCE;

    @Override
    public void setContext(Studio studio) {
    }

    @Override
    public String getName() {
        return "NanoJ-Fluidics";
    }

    @Override
    public String getHelpText() {
        return "A plugin to design a protocol of steps to control a set of microfluidics devices in sequence.";
    }

    @Override
    public String getVersion() {
        return GUI.Version();
    }

    @Override
    public String getCopyright() {
        return "(C) 2016 MRC LMCB, UCL";
    }

    @Override
    public String getSubMenu() {
        return "";
    }

    @Override
    public void onPluginSelected() {
        try {
            userInterface.create();
        } catch (Exception e) {
            IJ.log("Error, problem when initiating GUI.");
            ReportingUtils.logError(e);
        }
    }
}