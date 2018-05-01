package nanoJ.pumpControl.java;

import mmcorej.CMMCore;
import nanoj.pumpControl.java.sequentialProtocol.GUI;

import java.io.File;
import java.net.URL;

public class Main {
    private static CMMCore core;
    private static GUI userInterface;

    public static void main(String[] args) {
        String root = "file:" + System.getProperty("user.dir") + File.separator;

        try {
            // Load extension jars for pumps and acquisition systems
            Loader.loadJars(new URL(root + "Jars"));

            // Load library jars, including apache commons used in loadLibrary
            Loader.loadJars(new URL(root + "Libraries"));

            // Load java native libraries
            Loader.loadLibrary(new URL(root + "Libraries"));

        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        core = new CMMCore();

        try {
            userInterface = GUI.INSTANCE;
            userInterface.setCloseOnExit(true);
            userInterface.create(core);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

}