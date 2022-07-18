package nanoj.pumpControl.java.sequentialProtocol;

import java.io.File;

public class Main {

    public static void main(String[] args) {
        try {
            GUI userInterface = GUI.INSTANCE;
            userInterface.setCloseOnExit(true);
            userInterface.create();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

}