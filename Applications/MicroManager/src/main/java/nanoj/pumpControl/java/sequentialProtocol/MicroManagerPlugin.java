package nanoj.pumpControl.java.sequentialProtocol;

import ij.IJ;
import org.micromanager.MenuPlugin;
import org.micromanager.Studio;
import org.micromanager.internal.utils.ReportingUtils;
import org.scijava.command.Command;
import org.scijava.plugin.Plugin;

@Plugin(type = Command.class, menuPath = "NanoJ>Fluidics")
public class MicroManagerPlugin implements MenuPlugin {

    private Studio studio;
    private final GUI userInterface = GUI.INSTANCE;

    @Override
    public void setContext(Studio studio) {
        this.studio = studio;
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
        return "0.2";
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
            userInterface.create(studio.core());
        } catch (Exception e) {
            IJ.log("Error, problem when initiating GUI.");
            ReportingUtils.logError(e);
        }
    }
}