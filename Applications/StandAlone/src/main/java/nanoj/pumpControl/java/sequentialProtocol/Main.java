package nanoj.pumpControl.java.sequentialProtocol;

import mmcorej.CMMCore;
import nanoj.pumpControl.java.Loader;

import java.io.File;
import java.net.URL;

public class Main {

    public static void main(String[] args) {
        String root = "file:" + System.getProperty("user.dir") + File.separator;

        try {
            // Load java native libraries
            Loader.loadLibrary(new URL(root + "Libraries"));

        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        CMMCore core = new CMMCore();

        try {
            GUI userInterface = GUI.INSTANCE;
            userInterface.setCloseOnExit(true);
            userInterface.create(core);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

}