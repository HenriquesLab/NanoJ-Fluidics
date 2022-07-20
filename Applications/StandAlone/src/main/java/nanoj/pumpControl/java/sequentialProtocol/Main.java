package nanoj.pumpControl.java.sequentialProtocol;

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