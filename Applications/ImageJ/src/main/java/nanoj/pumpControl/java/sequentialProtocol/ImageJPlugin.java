package nanoj.pumpControl.java.sequentialProtocol;

import ij.IJ;
import ij.plugin.PlugIn;
import org.scijava.command.Command;
import org.scijava.plugin.Plugin;

@Plugin(type=Command.class, menuPath = "NanoJ>Fluidics")
public class ImageJPlugin implements Command, PlugIn {

    @Override
    public void run() {
        try {
            GUI userInterface = GUI.INSTANCE;
            userInterface.setCloseOnExit(false);
            userInterface.create();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run(String s) {
        run();
    }
}
