package nanoj.pumpControl.java.sequentialProtocol;

import ij.plugin.PlugIn;
import mmcorej.CMMCore;

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
public class ImageJPlugin implements PlugIn {
    private static CMMCore core;
    private static GUI userInterface;

    @Override
    public void run(String s) {
        core = new CMMCore();

        try {
            userInterface = GUI.INSTANCE;
            userInterface.setCloseOnExit(false);
            userInterface.create(core);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
