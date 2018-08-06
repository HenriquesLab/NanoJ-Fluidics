package nanoj.pumpControl.java.sequentialProtocol;

import mmcorej.CMMCore;
import nanoj.pumpControl.java.pumps.PumpManager;
import nanoj.pumpControl.java.pumps.SyringeList;
import nanoj.pumpControl.java.sequentialProtocol.tabs.DirectControl;
import nanoj.pumpControl.java.sequentialProtocol.tabs.PumpConnections;
import nanoj.pumpControl.java.sequentialProtocol.tabs.SequentialLabelling;
import org.apache.commons.lang3.SystemUtils;
import org.micromanager.utils.ReportingUtils;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.text.DefaultCaret;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.prefs.Preferences;

/*
  TODO: Automatic way to load syringes from a file
 */

public final class GUI {
    private Preferences prefs = Preferences.userRoot().node(this.getClass().getName());
    public PumpManager pumpManager;
    public String[] connectedPumps = new String[]{PumpManager.NO_PUMP_CONNECTED};
    private boolean closeOnExit = false;
    private double syringeMin = 1;
    private double syringeRate = 1;

    // Log object
    public Log log = Log.INSTANCE;

    // GUI objects and layout. These are standard Java SWING objects.
    private JFrame mainFrame;
    private JSplitPane mainPanel;

    private JScrollPane logPane;
    private JTabbedPane topPane;

    // These default dimensions are for macOS, the constructor then adapts for windows
    private Dimension connectionsDimensions = new Dimension(600, 270);
    private Dimension controlDimensions = new Dimension(500, 280);
    private Dimension sequenceDimensions = new Dimension(1140, 415);
    private Dimension logDimensions = new Dimension(500, 100);

    // Layout size variables
    public static final int rowHeight = 24;
    public static final int largeButtonWidth = 150;
    public static final int smallButtonWidth = 50;
    public static final int sizeSecondColumn = 250;

    //Information tracking objects
    private UpdateSyringeInformation updateSyringeInformation;

    //Tab objects
    private PumpConnections pumpConnections;
    private DirectControl directControl;
    private SequentialLabelling sequentialLabelling;

    public JButton stopPumpOnSeqButton;

    public PanelListener panelListener = new PanelListener();

    public static final GUI INSTANCE = new GUI();

    /*
     * This no-argument constructor is to make the GUI object a singleton.
     * The GUI is created by getting INSTANCE and calling the create() method.
     */
    private GUI() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            ReportingUtils.logError(e);
        }

        // Adapt dimensions to the windows look and feel
        if (SystemUtils.IS_OS_WINDOWS) {
            connectionsDimensions = new Dimension(500, 270);
            controlDimensions = new Dimension(435, 240);
            sequenceDimensions = new Dimension(850, 350);
            logDimensions = new Dimension(500, 100);
        }
    }

    public void create(CMMCore core) throws Exception {
        //Initiate PumpManager
        pumpManager = PumpManager.INSTANCE;
        pumpManager.setCore(core);
        pumpManager.loadPlugins();
        Thread pumpThread = new Thread(pumpManager);
        pumpThread.start();

        // GUI objects and layout.
        mainFrame = new JFrame("Pump Control and Sequential Protocol");
        mainPanel = new JSplitPane(JSplitPane.VERTICAL_SPLIT);

        logPane = new JScrollPane(
                log, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        topPane = new JTabbedPane();

        // General information tracking objects
        updateSyringeInformation = new UpdateSyringeInformation();

        // Stop button
        stopPumpOnSeqButton =  new JButton("Stop Pump!");

        //Tap objects
        pumpConnections = new PumpConnections(this);
        directControl = new DirectControl(this);
        sequentialLabelling = new SequentialLabelling(this);

        //Sequential Labelling tab objects

        mainFrame.addComponentListener(panelListener);

        topPane.getModel().addChangeListener(panelListener);

        topPane.setPreferredSize(connectionsDimensions);
        topPane.setMinimumSize(connectionsDimensions);

        logPane.setMinimumSize(logDimensions);
        logPane.setPreferredSize(logDimensions);

        log.set("NanoJ Sequential Labelling");

        //Listeners
        pumpConnections.availablePumpsList.addActionListener(updateSyringeInformation);
        directControl.syringeComboBox.addActionListener(updateSyringeInformation);
        directControl.rateSlider.addChangeListener(updateSyringeInformation);
        stopPumpOnSeqButton.addActionListener(directControl.stopPump);

        //Status observers
        pumpManager.addObserver(new UpdatePumpStatus());
        pumpManager.addObserver(updateSyringeInformation);

        topPane.addTab(pumpConnections.name, pumpConnections);
        topPane.addTab(directControl.name, directControl);
        topPane.addTab(sequentialLabelling.name, sequentialLabelling);

        mainPanel.setTopComponent(topPane);
        mainPanel.setBottomComponent(logPane);
        mainPanel.setResizeWeight(1);
        mainFrame.add(mainPanel);

        if (closeOnExit) mainFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        updateGUI();

        mainFrame.setVisible(true);

        //Minor GUI initialization steps
        sequentialLabelling.sequenceManager.defineSequence(sequentialLabelling.sequence);
        sequentialLabelling.sequenceManager.isSyringeExchangeRequiredOnSequence();
        new UpdateSyringeInformation().actionPerformed(new ActionEvent(directControl.syringeComboBox, 0, ""));
    }

    public void dispose() {
        try {
            pumpManager.stopPumping();
        } catch (Exception e) {
            e.printStackTrace();
        }
        pumpConnections.rememberSettings();
        directControl.rememberPreferences();
    }

    public ArrayList<Step> getSteps() {
        return sequentialLabelling.sequence;
    }

    public SequenceManager getSequenceManager() {
        return sequentialLabelling.sequenceManager;
    }

    public String startSequence() {
        if (pumpManager.isConnected()) {
            sequentialLabelling.sequence.setSuck(sequentialLabelling.suckBetweenSteps.isSelected());
            sequentialLabelling.sequenceManager.start(sequentialLabelling.sequence);
            return "Started sequence!";
        }
        else return "Can't do anything until pump is connected.";
    }

    public boolean isRunning() { return sequentialLabelling.sequenceManager.isStarted(); }

    public void updateGUI() {
        mainFrame.validate();
        mainFrame.pack();
    }

    public void updatePumpSelection() {
        String[][] result = pumpManager.getConnectedPumpsList();
        if (result[0][0].equals(PumpManager.NO_PUMP_CONNECTED)) {
            connectedPumps = new String[]{PumpManager.NO_PUMP_CONNECTED};
            directControl.pumpSelection.removeAllItems();
            directControl.pumpSelection.addItem(PumpManager.NO_PUMP_CONNECTED);
        }
        else {
            connectedPumps = new String[result.length];
            directControl.pumpSelection.removeAllItems();
            for (int s= 0; s<connectedPumps.length; s++){
                String entry = result[s][1] + ", " + result[s][2];
                directControl.pumpSelection.addItem(entry);
                connectedPumps[s] = entry;
            }

        }
        for (Step step: sequentialLabelling.sequence) step.setPumps(connectedPumps);
        sequentialLabelling.sequence.getSuckStep().setPumps(connectedPumps);
        updateSyringeInformation.actionPerformed(new ActionEvent(pumpConnections.connectedPumpsTableModel, 0, ""));
    }


    // Private classes

    public static class Log extends JTextArea {
        public static Log INSTANCE = new Log();

        private Log() {
            super();
            DefaultCaret caret = (DefaultCaret) this.getCaret();
            caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
        }

        public void set(String text) {
            if (text == null) return;
            String current_time = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(new Date());
            String output = current_time + ": " + text;
            setText(output);
        }

        public void message(String msg){
            if (msg == null) return;
            String current_time = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(new Date());
            String output = current_time + ": " + msg;
            this.append("\n" + output);
            this.setCaretPosition(this.getDocument().getLength());
        }
    }

    public class UpdateSyringeInformation implements ActionListener, Observer, ChangeListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            if (!(pumpConnections.connectedPumpsTableModel.getValueAt(
                    directControl.pumpSelection.getSelectedIndex(), 0))
                    .equals(PumpManager.NO_PUMP_CONNECTED)) {
                double[] newInformation = pumpManager.getMaxMin(
                        (String) pumpConnections.connectedPumpsTableModel.getValueAt(
                                directControl.pumpSelection.getSelectedIndex(),0),
                        SyringeList.getDiameter(directControl.syringeComboBox.getSelectedIndex()));

                syringeMin = newInformation[1];
                syringeRate = (newInformation[0] - newInformation[1]);

                double sliderValue = (double) directControl.rateSlider.getValue() + (double) Integer.MAX_VALUE + 1;
                sliderValue = sliderValue/ ((2*((double) Integer.MAX_VALUE)) +1);

                double rate = (syringeRate*sliderValue)+syringeMin;
                directControl.rateText.setText("" + new BigDecimal(rate).setScale(3, RoundingMode.HALF_EVEN).toPlainString());
            }

            pumpConnections.rememberSettings();
        }

        @Override
        public void update(Observable o, Object arg) {
            if(arg == PumpManager.CURRENT_PUMP_CHANGED) actionPerformed(new ActionEvent(directControl.pumpSelection,0,""));
        }

        @Override
        public void stateChanged(ChangeEvent e) {
            double sliderValue = (double) directControl.rateSlider.getValue() + (double) Integer.MAX_VALUE + 1;
            sliderValue = sliderValue/ ((2*((double) Integer.MAX_VALUE)) +1);

            double rate = (syringeRate*sliderValue)+syringeMin;
            directControl.rateText.setText("" + new BigDecimal(rate).setScale(3, RoundingMode.HALF_EVEN).toPlainString());
        }
    }

    private class UpdatePumpStatus implements Observer {

        @Override
        public void update(Observable o, Object arg) {
            if (arg != PumpManager.NEW_STATUS_AVAILABLE) return;
            directControl.pumpStatus.setText(pumpManager.getStatus());
            sequentialLabelling.pumpStatusOnSeq.setText(pumpManager.getStatus());
        }
    }

    private class PanelListener implements ChangeListener, ComponentListener{

        void setSizes() {

            if (topPane.getSelectedIndex() == 0) {
                topPane.setPreferredSize(getConnectionsDimensions());
            }
            if (topPane.getSelectedIndex() == 1) {
                topPane.setPreferredSize(getControlDimensions());
            }
            if (topPane.getSelectedIndex() == 2) {
                topPane.setPreferredSize(getSequenceDimensions());
            }
        }

        @Override
        public void stateChanged(ChangeEvent e) {
            if (e.getSource().equals(topPane.getModel())) {
                setSizes();
                updateGUI();
            }

            if (e.getSource().equals(sequentialLabelling.suckBetweenSteps)) {
                prefs.putBoolean(SequentialLabelling.SUCK, sequentialLabelling.suckBetweenSteps.isSelected());
                sequentialLabelling.suckStepPanel.setVisible(
                        sequentialLabelling.suckBetweenSteps.isSelected());
            }
        }

        @Override
        public void componentResized(ComponentEvent e) {
            if (topPane.getSize().getHeight() == 0 || topPane.getSize().getWidth() == 0) return;

            if (topPane.getSelectedIndex() == 0) {
                setConnectionsDimensions(topPane.getSize());
            }
            if (topPane.getSelectedIndex() == 1) {
                setControlDimensions(topPane.getSize());
            }
            if (topPane.getSelectedIndex() == 2) {
                setSequenceDimensions(topPane.getSize());
            }

            setSizes();
        }

        @Override
        public void componentMoved(ComponentEvent e) {

        }

        @Override
        public void componentShown(ComponentEvent e) {

        }

        @Override
        public void componentHidden(ComponentEvent e) {

        }
    }

    // Getters and Setters

    private synchronized Dimension getConnectionsDimensions() {
        return connectionsDimensions;
    }

    private synchronized void setConnectionsDimensions(Dimension connectionsDimensions) {
        this.connectionsDimensions = connectionsDimensions;
    }

    private synchronized Dimension getControlDimensions() {
        return controlDimensions;
    }

    private synchronized void setControlDimensions(Dimension controlDimensions) {
        this.controlDimensions = controlDimensions;
    }

    private synchronized Dimension getSequenceDimensions() {
        return sequenceDimensions;
    }

    private synchronized void setSequenceDimensions(Dimension sequenceDimensions) {
        this.sequenceDimensions = sequenceDimensions;
    }

    public boolean isCloseOnExit() {
        return closeOnExit;
    }

    public void setCloseOnExit(boolean closeOnExit) {
        this.closeOnExit = closeOnExit;
    }

}
