package nanoj.pumpControl.java.sequentialProtocol.tabs;

import nanoj.pumpControl.java.pumps.ConnectedSubPump;
import nanoj.pumpControl.java.pumps.Pump;
import nanoj.pumpControl.java.pumps.PumpManager;
import nanoj.pumpControl.java.sequentialProtocol.GUI;
import nanoj.pumpControl.java.sequentialProtocol.StopButton;

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.util.ArrayList;
import java.util.Observable;
import java.util.Observer;
import java.util.prefs.Preferences;

public class PumpCalibration extends JPanel implements Observer, TableModelListener, ActionListener {
    private final Preferences preferences = Preferences.userRoot().node(this.getClass().getName());
    private final PumpManager pumpManager = PumpManager.INSTANCE;
    private final GUI gui;
    public static final String TAB_NAME = "Pump Calibration";

    final JLabel tableLabel;
    private final CalibrationTable tableModel;
    final JScrollPane tableScrollPane;

    final JButton loadCalibration = new JButton("Load Previous Calibration from file");
    final JButton saveCalibration = new JButton("Save Current Calibration to file");
    final JButton resetCalibration = new JButton("Reset Calibration");

    final JComboBox<String> pumpList;
    final JLabel timeToPumpLabel = new JLabel("Time to pump (seconds)");
    final JTextField timeToPump = new JTextField("10");
    final JButton calibrateButton = new JButton("Start pumping");
    final StopButton stopButton;

    private static final String CAL = "Cal";
    private static final String SAVE_LOCATION = "location";

    private final int NAME = 0;
    private final int SUB_PUMP = 1;
    private final int PORT = 2;
    private final int DIAMETER = 3;
    private final int MAX_FLOW_RATE = 4;
    private final int MIN_FLOW_RATE = 5;

    private boolean editing = false;

    public PumpCalibration(GUI gui) {
        super();

        this.gui = gui;

        pumpList = new JComboBox<>(new String[]{PumpManager.NO_PUMP_CONNECTED});
        stopButton = new StopButton(gui,pumpList);

        tableLabel = new JLabel("Currently connected pumps. Diameter is in mm. Flow rates are in ul/sec.");
        tableModel = new CalibrationTable();
        JTable table = new JTable(tableModel);
        tableScrollPane = new JScrollPane(table);

        loadCalibration.addActionListener(this);
        saveCalibration.addActionListener(this);
        resetCalibration.addActionListener(this);
        calibrateButton.addActionListener(this);

        pumpManager.addObserver(this);
        tableModel.addTableModelListener(this);

        setLayout(new PumpCalibrationLayout(this));
    }

    @Override
    public void update(Observable o, Object arg) {
        if (arg.equals(PumpManager.NEW_PUMP_CONNECTED) || arg.equals(PumpManager.PUMP_DISCONNECTED)) {
            editing = true;

            pumpList.removeAllItems();
            if (pumpManager.noPumpsConnected()) {
                pumpList.addItem(PumpManager.NO_PUMP_CONNECTED);
            }

            tableModel.setRowCount(0);

            int index = 0;
            for (ConnectedSubPump subPump: pumpManager.getConnectedPumpsList()) {
                pumpList.addItem(subPump.getFullName());
                tableModel.addRow(subPump.asCalibrationArray());

                String key = subPump.name + subPump.subPump + subPump.port;

                double diameter = preferences.getDouble(CAL+DIAMETER+key,
                        Double.parseDouble(subPump.asCalibrationArray()[DIAMETER]));

                tableModel.setValueAt(""+diameter,index,DIAMETER);

                double maxFlowRate = preferences.getDouble(CAL+MAX_FLOW_RATE+key,
                        Double.parseDouble(subPump.asCalibrationArray()[MAX_FLOW_RATE]));

                tableModel.setValueAt(""+maxFlowRate,index,MAX_FLOW_RATE);

                double minFlowRate = preferences.getDouble(CAL+MIN_FLOW_RATE+key,
                        Double.parseDouble(subPump.asCalibrationArray()[MIN_FLOW_RATE]));

                tableModel.setValueAt(""+minFlowRate,index,MIN_FLOW_RATE);

                pumpManager.updateReferenceRate(index,new double[]{diameter,maxFlowRate,minFlowRate});

                index++;
            }

            editing = false;
        }
    }

    @Override
    public void tableChanged(TableModelEvent e) {
        if (e.getType() == TableModelEvent.UPDATE || e.getType() == TableModelEvent.INSERT ) {
            if (!editing) {
                double diameter;
                double maxFlowRate;
                double minFlowRate;

                diameter = Double.parseDouble((String) tableModel.getValueAt(e.getFirstRow(),DIAMETER));
                maxFlowRate = Double.parseDouble((String) tableModel.getValueAt(e.getFirstRow(),MAX_FLOW_RATE));
                minFlowRate = Double.parseDouble((String) tableModel.getValueAt(e.getFirstRow(),MIN_FLOW_RATE));

                String name = (String) tableModel.getValueAt(e.getFirstRow(),NAME);
                String subPump = (String) tableModel.getValueAt(e.getFirstRow(),SUB_PUMP);
                String port = (String) tableModel.getValueAt(e.getFirstRow(),PORT);

                pumpManager.updateReferenceRate(e.getFirstRow(),new double[] {diameter,maxFlowRate,minFlowRate});

                preferences.putDouble(CAL+DIAMETER+name+subPump+port,diameter);
                preferences.putDouble(CAL+MAX_FLOW_RATE+name+subPump+port,maxFlowRate);
                preferences.putDouble(CAL+MIN_FLOW_RATE+name+subPump+port,minFlowRate);

                gui.log.message("Updated calibration of " + name + ", " + subPump + " on port " + port + " to: " +
                        diameter + ", " + maxFlowRate + ", " + minFlowRate);
            }
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource().equals(loadCalibration)) {
            // Create .nsc file chooser
            JFileChooser chooser = new JFileChooser();
            chooser.setFileFilter(new FileNameExtensionFilter("NanoJ SeqLab Calibration file", "nsc"));
            chooser.setDialogTitle("Choose Calibration file to load");

            // Get working directory from preferences
            chooser.setCurrentDirectory(new File(preferences.get(SAVE_LOCATION, System.getProperty("user.home"))));

            // Get save location from user
            int returnVal = chooser.showOpenDialog(loadCalibration);

            // If successful, load protocol
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                // Save location in preferences, so it is loaded next time the software is loaded
                preferences.put(SAVE_LOCATION,chooser.getSelectedFile().getParent());

                try {
                    // Create file opener
                    FileInputStream fileIn = new FileInputStream(chooser.getSelectedFile().getAbsolutePath());
                    ObjectInputStream in = new ObjectInputStream(fileIn);

                    //noinspection unchecked
                    ArrayList<String[]> input = (ArrayList<String[]>) in.readObject();

                    editing = true;
                    int index = 0;
                    int matches = 0;
                    for (ConnectedSubPump subPump: pumpManager.getConnectedPumpsList()) {
                        for (String[] entry: input) {
                            if (entry[NAME].equals(subPump.name) &&
                                    entry[SUB_PUMP].equals(subPump.subPump) &&
                                    entry[PORT].equals(subPump.port)) {
                                tableModel.setValueAt(entry[DIAMETER],index,DIAMETER);
                                tableModel.setValueAt(entry[MAX_FLOW_RATE],index,MAX_FLOW_RATE);
                                tableModel.setValueAt(entry[MIN_FLOW_RATE],index,MIN_FLOW_RATE);
                                matches++;
                            }
                        }
                        index ++;
                    }
                    editing = false;

                    if (matches == 0) {
                        gui.log.message("WARNING: Loaded calibration file doesn't match any connected pump.");
                    } else if (matches < index) {
                        gui.log.message("WARNING: Loaded calibration only matched a few of the connected pump.");
                    } else if (matches == index) {
                        gui.log.message("Loaded calibration file.");
                    } else {
                        gui.log.message("WARNING: Calibration matches more than the number of connected pumps?.");
                    }

                    // Close file
                    in.close();
                    fileIn.close();

                } catch (FileNotFoundException f) {
                    gui.log.message("Error, File Not Found.");
                    f.printStackTrace();
                } catch (IOException i) {
                    gui.log.message("Error, can not read from location.");
                    i.printStackTrace();
                } catch (ClassNotFoundException c) {
                    gui.log.message("Error, File type is incorrect.");
                    c.printStackTrace();
                }
            }
        }

        else if (e.getSource().equals(saveCalibration)) {
            // Get working directory from preferences
            File dir = new File(preferences.get(SAVE_LOCATION, System.getProperty("user.home")));

            // Create .nsp file chooser
            JFileChooser chooser = new JFileChooser();
            chooser.setFileFilter(new FileNameExtensionFilter("NanoJ SeqLab Calibration file", "nsc"));
            chooser.setDialogTitle("Choose where to save calibration file");
            chooser.setCurrentDirectory(dir);

            // Get save location from user
            int returnVal = chooser.showSaveDialog(saveCalibration);

            // If successful, save protocol
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                // Save location in preferences, so it is loaded next time the software is loaded
                preferences.put(SAVE_LOCATION,chooser.getSelectedFile().getParent());

                // Make sure file has only one .nsc termination
                if (!chooser.getSelectedFile().getAbsolutePath().endsWith(".nsc")) {
                    dir = new File(chooser.getSelectedFile() + ".nsc");
                } else dir = chooser.getSelectedFile();

                try {
                    // Open file streams
                    FileOutputStream fileOut = new FileOutputStream(dir);
                    ObjectOutputStream out = new ObjectOutputStream(fileOut);

                    // Create output file information
                    ArrayList<String[]> output = new ArrayList<>();

                    for (int i = 0; i < tableModel.getRowCount(); i++) {
                        String name = (String) tableModel.getValueAt(i,NAME);
                        String subPump = (String) tableModel.getValueAt(i,SUB_PUMP);
                        String port = (String) tableModel.getValueAt(i,PORT);
                        String diameter = (String) tableModel.getValueAt(i,DIAMETER);
                        String max = (String) tableModel.getValueAt(i,MAX_FLOW_RATE);
                        String min = (String) tableModel.getValueAt(i,MIN_FLOW_RATE);

                        output.add(new String[]{name,subPump,port,diameter,max,min});
                    }

                    //Write to file
                    out.writeObject(output);
                    out.close();
                    fileOut.close();

                } catch (FileNotFoundException f) {
                    gui.log.message("Error, file not found.");
                    f.printStackTrace();
                } catch (IOException i) {
                    gui.log.message("Error, can not write to target location.");
                    i.printStackTrace();
                }
                gui.log.message("Saved calibration.");
            }
        }

        else if (e.getSource().equals(resetCalibration)) {
            editing = true;

            int index = 0;

            for (ConnectedSubPump subPump: pumpManager.getConnectedPumpsList()) {
                String key = subPump.name+subPump.subPump+subPump.port;

                double diameter = subPump.pump.getDefaultRate()[0];
                tableModel.setValueAt("" + diameter,index,DIAMETER);
                preferences.putDouble(CAL+DIAMETER+key,diameter);

                double maxFlowRate = subPump.pump.getDefaultRate()[1];
                tableModel.setValueAt("" + maxFlowRate,index,MAX_FLOW_RATE);
                preferences.putDouble(CAL+MAX_FLOW_RATE+key,maxFlowRate);

                double minFlowRate = subPump.pump.getDefaultRate()[2];
                tableModel.setValueAt("" + minFlowRate,index,MIN_FLOW_RATE);
                preferences.putDouble(CAL+MIN_FLOW_RATE+key,minFlowRate);

                pumpManager.updateReferenceRate(index,new double[]{diameter,maxFlowRate,minFlowRate});

                index++;
            }

            editing = false;

            gui.log.message("Reset calibration of all pumps to default values.");

        }

        else if (e.getSource().equals(calibrateButton)) {
            int index = pumpList.getSelectedIndex();
            if (!pumpManager.isConnected(index)){
                gui.log.message("Can't do anything until pump is connected.");
                return;
            }

            if (index >= 0 && index < pumpManager.getConnectedPumpsList().size()) {
                try {
                    pumpManager.startPumping(
                            index,
                            Integer.parseInt(timeToPump.getText()),
                            Pump.Action.Infuse);
                } catch (Exception e1) {
                    gui.log.message("Error while starting the pump.");
                    e1.printStackTrace();
                }
            }
        }
    }

    private static class CalibrationTable extends DefaultTableModel {

        protected CalibrationTable() {
            super(new String[][]{{PumpManager.NO_PUMP_CONNECTED,"","","",""}},
                    new String[]{"Pump","Sub-Pump","COM port","Ref. Diam.","Max Ref. Rate","Min Ref. Rate"});
        }

        public boolean isCellEditable(int row, int column){
            return column > 2;
        }

    }

}
