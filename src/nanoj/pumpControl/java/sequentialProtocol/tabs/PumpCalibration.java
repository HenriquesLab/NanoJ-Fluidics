package nanoj.pumpControl.java.sequentialProtocol.tabs;

import nanoj.pumpControl.java.pumps.ConnectedSubPump;
import nanoj.pumpControl.java.pumps.PumpManager;
import nanoj.pumpControl.java.sequentialProtocol.GUI;

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableModel;
import java.util.Observable;
import java.util.Observer;
import java.util.prefs.Preferences;

public class PumpCalibration extends JPanel implements Observer, TableModelListener {
    private Preferences prefs = Preferences.userRoot().node(this.getClass().getName());
    private PumpManager pumpManager = PumpManager.INSTANCE;
    private GUI gui;
    public String name = "Pump Calibration";

    JLabel tableLabel;
    private JTable table;
    private CalibrationTable tableModel;
    JScrollPane tableScrollPane;

    private int NAME = 0;
    private int SUB_PUMP = 1;
    private int PORT = 2;
    private int DIAMETER = 3;
    private int MAX_FLOW_RATE = 4;
    private int MIN_FLOW_RATE = 5;

    public PumpCalibration(GUI gui) {
        super();

        this.gui = gui;

        tableLabel = new JLabel("List of currently connected pumps. Flow rates are in ul/sec");
        tableModel = new CalibrationTable();
        table = new JTable(tableModel);
        tableScrollPane = new JScrollPane(table);

        pumpManager.addObserver(this);
        tableModel.addTableModelListener(this);

        setLayout(new PumpCalibrationLayout(this));
    }

    @Override
    public void update(Observable o, Object arg) {
        if (arg.equals(PumpManager.NEW_PUMP_CONNECTED) || arg.equals(PumpManager.PUMP_DISCONNECTED)) {

            tableModel.setRowCount(0);

            for (ConnectedSubPump subPump: pumpManager.getConnectedPumpsList())
                tableModel.addRow(subPump.asCalibrationArray());

        }
    }

    @Override
    public void tableChanged(TableModelEvent e) {
        if (e.getType() == TableModelEvent.UPDATE || e.getType() == TableModelEvent.INSERT ) {
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

            gui.log.message("Updated calibration of " + name + ", " + subPump + " on port " + port + " to: " +
                    diameter + ", " + maxFlowRate + ", " + minFlowRate);
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
