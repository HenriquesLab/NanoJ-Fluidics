package nanoj.pumpControl.java.sequentialProtocol.tabs;

import nanoj.pumpControl.java.pumps.ConnectedSubPump;
import nanoj.pumpControl.java.pumps.PumpManager;
import nanoj.pumpControl.java.sequentialProtocol.GUI;

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableModel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Observable;
import java.util.Observer;
import java.util.prefs.Preferences;

public class PumpCalibration extends JPanel implements Observer, TableModelListener, ActionListener {
    private Preferences prefs = Preferences.userRoot().node(this.getClass().getName());
    private PumpManager pumpManager = PumpManager.INSTANCE;
    private GUI gui;
    public String name = "Pump Calibration";

    JLabel tableLabel;
    private JTable table;
    private CalibrationTable tableModel;
    JScrollPane tableScrollPane;

    protected JButton loadCalibration = new JButton("Load Previous Calibration from file");
    protected JButton saveCalibration = new JButton("Save Current Calibration to file");
    protected JButton resetCalibration = new JButton("Reset Calibration");

    private static final String CAL = "Cal";

    private int NAME = 0;
    private int SUB_PUMP = 1;
    private int PORT = 2;
    private int DIAMETER = 3;
    private int MAX_FLOW_RATE = 4;
    private int MIN_FLOW_RATE = 5;

    private boolean editing = false;

    public PumpCalibration(GUI gui) {
        super();

        this.gui = gui;

        tableLabel = new JLabel("List of currently connected pumps. Flow rates are in ul/sec");
        tableModel = new CalibrationTable();
        table = new JTable(tableModel);
        tableScrollPane = new JScrollPane(table);

        loadCalibration.addActionListener(this);
        saveCalibration.addActionListener(this);
        resetCalibration.addActionListener(this);

        pumpManager.addObserver(this);
        tableModel.addTableModelListener(this);

        setLayout(new PumpCalibrationLayout(this));
    }

    @Override
    public void update(Observable o, Object arg) {
        if (arg.equals(PumpManager.NEW_PUMP_CONNECTED) || arg.equals(PumpManager.PUMP_DISCONNECTED)) {

            editing = true;
            tableModel.setRowCount(0);

            int index = 0;
            for (ConnectedSubPump subPump: pumpManager.getConnectedPumpsList()) {
                tableModel.addRow(subPump.asCalibrationArray());

                String key = subPump.name + subPump.subPump + subPump.port;

                double diameter = prefs.getDouble(CAL+DIAMETER+key,
                        Double.parseDouble(subPump.asCalibrationArray()[DIAMETER]));

                tableModel.setValueAt(""+diameter,index,DIAMETER);

                double maxFlowRate = prefs.getDouble(CAL+MAX_FLOW_RATE+key,
                        Double.parseDouble(subPump.asCalibrationArray()[MAX_FLOW_RATE]));

                tableModel.setValueAt(""+maxFlowRate,index,MAX_FLOW_RATE);

                double minFlowRate = prefs.getDouble(CAL+MIN_FLOW_RATE+key,
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

                prefs.putDouble(CAL+DIAMETER+name+subPump+port,diameter);
                prefs.putDouble(CAL+MAX_FLOW_RATE+name+subPump+port,maxFlowRate);
                prefs.putDouble(CAL+MIN_FLOW_RATE+name+subPump+port,minFlowRate);

                gui.log.message("Updated calibration of " + name + ", " + subPump + " on port " + port + " to: " +
                        diameter + ", " + maxFlowRate + ", " + minFlowRate);
            }
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource().equals(loadCalibration)) {

        } else if (e.getSource().equals(saveCalibration)) {

        } else if (e.getSource().equals(resetCalibration)) {

            editing = true;

            int index = 0;

            for (ConnectedSubPump subPump: pumpManager.getConnectedPumpsList()) {
                String key = subPump.name+subPump.subPump+subPump.port;

                double diameter = subPump.pump.getDefaultRate()[0];
                tableModel.setValueAt("" + diameter,index,DIAMETER);
                prefs.putDouble(CAL+DIAMETER+key,diameter);

                double maxFlowRate = subPump.pump.getDefaultRate()[1];
                tableModel.setValueAt("" + maxFlowRate,index,MAX_FLOW_RATE);
                prefs.putDouble(CAL+MAX_FLOW_RATE+key,maxFlowRate);

                double minFlowRate = subPump.pump.getDefaultRate()[2];
                tableModel.setValueAt("" + minFlowRate,index,MIN_FLOW_RATE);
                prefs.putDouble(CAL+MIN_FLOW_RATE+key,minFlowRate);

                pumpManager.updateReferenceRate(index,new double[]{diameter,maxFlowRate,minFlowRate});

                index++;
            }

            editing = false;

        }
    }

    private class CalibrationTable extends DefaultTableModel {

        protected CalibrationTable() {
            super(new String[][]{{PumpManager.NO_PUMP_CONNECTED,"","","",""}},
                    new String[]{"Pump","Sub-Pump","COM port","Ref. Diam.","Max Ref. Rate","Min Ref. Rate"});
        }

        public boolean isCellEditable(int row, int column){
            if (column > 2)
                return true;
            else return false;
        }

    }

}
