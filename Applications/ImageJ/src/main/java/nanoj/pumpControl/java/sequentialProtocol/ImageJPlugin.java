package nanoj.pumpControl.java.sequentialProtocol;

import mmcorej.CMMCore;
import org.scijava.command.Command;

/**
 * Created by palmada on 22/05/2017.
 *
 * Requires:
 *  In /jars
 *      MMcoreJ.jar
 *      nrjavaserial.jar
 *      *All relevant pump plugins
 *  In root
 *      libMMCoreJ_wrap.jnilib (MAC OSX) or
 *      MMCoreJ_wrap.dll (WINDOWS)
 *      libmmgr_dal_SerialManager
 *
 */
public class ImageJPlugin implements Command {

    @Override
    public void run() {
        CMMCore core = new CMMCore();

        try {
            GUI userInterface = GUI.INSTANCE;
            userInterface.setCloseOnExit(false);
            userInterface.create(core);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
