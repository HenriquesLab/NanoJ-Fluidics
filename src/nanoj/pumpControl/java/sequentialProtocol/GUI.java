package nanoj.pumpControl.java.sequentialProtocol;

import mmcorej.CMMCore;
import nanoj.pumpControl.java.pumps.PumpManager;
import nanoj.pumpControl.java.sequentialProtocol.tabs.DirectControl;
import nanoj.pumpControl.java.sequentialProtocol.tabs.PumpCalibration;
import nanoj.pumpControl.java.sequentialProtocol.tabs.PumpConnections;
import nanoj.pumpControl.java.sequentialProtocol.tabs.SequentialLabelling;
import org.apache.commons.lang3.SystemUtils;
import org.micromanager.utils.ReportingUtils;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.text.DefaultCaret;
import java.awt.*;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.text.SimpleDateFormat;
import java.util.*;

public final class GUI {
    public PumpManager pumpManager;
    private boolean closeOnExit = false;

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

    //Tab objects
    private PumpConnections pumpConnections;
    private DirectControl directControl;
    private PumpCalibration pumpCalibration;
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
        // GUI objects and layout.
        mainFrame = new JFrame("Pump Control and Sequential Protocol");
        mainPanel = new JSplitPane(JSplitPane.VERTICAL_SPLIT);

        logPane = new JScrollPane(
                log, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        topPane = new JTabbedPane();

        // Stop button
        stopPumpOnSeqButton =  new JButton("Stop Pump!");

        //Tab objects
        pumpConnections = new PumpConnections(this);
        directControl = new DirectControl(this);
        pumpCalibration = new PumpCalibration(this);
        sequentialLabelling = new SequentialLabelling(this);

        mainFrame.addComponentListener(panelListener);

        topPane.getModel().addChangeListener(panelListener);

        topPane.setPreferredSize(connectionsDimensions);
        topPane.setMinimumSize(connectionsDimensions);

        logPane.setMinimumSize(logDimensions);
        logPane.setPreferredSize(logDimensions);

        log.set("NanoJ Sequential Labelling");

        topPane.addTab(pumpConnections.name, pumpConnections);
        topPane.addTab(directControl.name, directControl);
        topPane.addTab(pumpCalibration.name, pumpCalibration);
        topPane.addTab(sequentialLabelling.name, sequentialLabelling);

        mainPanel.setTopComponent(topPane);
        mainPanel.setBottomComponent(logPane);
        mainPanel.setResizeWeight(1);
        mainFrame.add(mainPanel);

        if (closeOnExit) mainFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        updateGUI();

        mainFrame.setVisible(true);
    }

    public void dispose() {
        try {
            pumpManager.stopAllPumps();
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
        if (pumpManager.noPumpsConnected()) {
            sequentialLabelling.sequence.setSuck(sequentialLabelling.suckBetweenSteps.isSelected());
            sequentialLabelling.sequenceManager.start(sequentialLabelling.sequence);
            return "Started sequence!";
        }
        else return "Can't do anything until a pump is connected.";
    }

    public boolean isRunning() { return sequentialLabelling.sequenceManager.isStarted(); }

    public void updateGUI() {
        mainFrame.validate();
        mainFrame.pack();
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

            }
            if (topPane.getSelectedIndex() == 3) {
                setSequenceDimensions(topPane.getSize());
            }

            setSizes();
        }

        @Override
        public void componentMoved(ComponentEvent e) { }

        @Override
        public void componentShown(ComponentEvent e) { }

        @Override
        public void componentHidden(ComponentEvent e) { }
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
