package nanoj.pumpControl.java.sequentialProtocol;

import org.scijava.command.Command;
import org.scijava.plugin.Plugin;

@SuppressWarnings("unused")
@Plugin(type = Command.class, menuPath = "NanoJ>NanoJ-Fluidics")
public class ImageJPlugin implements Command {

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

}
