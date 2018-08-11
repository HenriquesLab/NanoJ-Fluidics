package nanoj.pumpControl.java.sequentialProtocol.tabs;

import nanoj.pumpControl.java.pumps.ConnectedSubPump;
import nanoj.pumpControl.java.pumps.PumpManager;
import nanoj.pumpControl.java.sequentialProtocol.GUI;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.util.Observable;
import java.util.Observer;
import java.util.prefs.Preferences;

public class PumpCalibration extends JPanel implements Observer {
    private Preferences prefs = Preferences.userRoot().node(this.getClass().getName());
    private PumpManager pumpManager = PumpManager.INSTANCE;
    private GUI gui;
    public String name = "Pump Calibration";

    JLabel tableLabel;
    private JTable table;
    private CalibrationTable tableModel;
    JScrollPane tableScrollPane;

    public PumpCalibration(GUI gui) {
        super();

        this.gui = gui;

        tableLabel = new JLabel("List of currently connected pumps");
        tableModel = new CalibrationTable();
        table = new JTable(tableModel);
        tableScrollPane = new JScrollPane(table);

        pumpManager.addObserver(this);

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

    public class CalibrationTable extends DefaultTableModel {

        protected CalibrationTable() {
            super(new String[][]{{PumpManager.NO_PUMP_CONNECTED,"","","",""}},
                    new String[]{"Pump","Sub-Pump","COM port","Ref. Diam.","Ref. Rate"});
        }

        public boolean isCellEditable(int row, int column){
            if (column > 2)
                return true;
            else return false;
        }

    }
}
