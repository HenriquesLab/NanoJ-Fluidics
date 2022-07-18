package nanoj.pumpControl.java.sequentialProtocol;

import org.scijava.command.Command;

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
