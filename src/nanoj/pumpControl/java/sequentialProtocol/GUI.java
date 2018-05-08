package nanoj.pumpControl.java.sequentialProtocol;

import gnu.io.NRSerialPort;
import mmcorej.CMMCore;
import nanoj.pumpControl.java.pumps.Pump;
import nanoj.pumpControl.java.pumps.PumpManager;
import nanoj.pumpControl.java.pumps.SyringeList;
import org.apache.commons.lang3.SystemUtils;
import org.micromanager.utils.ReportingUtils;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import javax.swing.text.DefaultCaret;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.io.*;
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
    private PumpManager pumpManager;
    private String[] connectedPumps = new String[]{PumpManager.NO_PUMP_CONNECTED};
    private static final String ADD_STEP = "+";
    private static final String REMOVE_STEP = "-";
    private static final String VIRTUAL_PUMP = "Virtual Pump";
    private static final String VIRTUAL_PORT = "Virtual port";
    private boolean closeOnExit = false;
    private double syringeMin = 1;
    private double syringeRate = 1;

    // Preference Keys
    private static final String PORT = "com";
    private static final String PUMP = "pump";
    private static final String TARGET = "target";
    private static final String SUCK = "suck";
    private static final String SAVE_LOCATION = "location";

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


    //Pump set-up tab objects
    private String pumpConnectionsName = "Pump Connections";
    private JPanel pumpConnectionsPanel;

    private JLabel pumpListLabel;
    private JComboBox availablePumpsList;
    private JLabel connectLabel;
    private JComboBox portsList;
    private JButton connectButton;
    private JButton disconnectButton;
    private JLabel connectedPumpsLabel;
    private DefaultTableModel connectedPumpsTableModel;
    private JTable connectedPumpsTable;
    private JScrollPane connectedPumpsListPane;


    //Direct Control tab objects
    private String directPumpControlName = "Direct Pump Control";
    private JPanel directPumpControlPanel;
    private UpdateSyringeInformation updateSyringeInformation = new UpdateSyringeInformation();;
    private StopPump stopPump = new StopPump();

    private JLabel pumpStatusLabel;
    private JLabel pumpStatus;
    private JLabel pumpSelectionLabel;
    private JComboBox pumpSelection;
    private JLabel syringeLabel;
    private JComboBox syringeComboBox;
    private JLabel rateLabel;
    private JSlider rateSlider;
    private JLabel rateText;
    private JLabel targetLabel;
    private JTextField targetField;
    private JLabel actionLabel;
    private JRadioButton inf;
    private JRadioButton wit;
    private ButtonGroup buttons;
    private JButton startPumpButton;
    private JButton stopPumpButton;


    //Sequential Labelling tab objects
    private String sequentialLabellingName = "Sequential Protocol";
    private JPanel sequentialLabellingPanel;
    private JPanel sequentialLabellingTopPanel;
    private JPanel sequentialLabellingStepsPanel;
    private JScrollPane sequentialLabellingStepsScrollPane;
    private StepChanger stepChanger = new StepChanger();
    private Sequence sequence = new Sequence(stepChanger);
    private UpdateStepPump updateStepPump = new UpdateStepPump();

    private JButton protocolLoad;
    private JButton protocolSave;
    private JLabel stepsLabel;
    private JTextField numberOfSteps;
    private JButton addStepButton;
    private JButton removeStepButton;
    private JButton startSeqButton;
    private JButton stopSeqButton;
    private JButton stopPumpOnSeqButton;
    private JLabel syringeReadyLabel;
    private JButton syringeReadyButton;
    private JLabel seqStatus;
    private JLabel pumpStatusOnSeqLabel;
    private JLabel pumpStatusOnSeq;
    private JLabel suckBetweenStepsLabel;
    private JCheckBox suckBetweenSteps;
    private JLabel suckStepLabel;
    private JPanel suckStepPanel;

    private SequenceManager sequenceManager;
    private PanelListener panelListener = new PanelListener();

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
        {
            // GUI objects and layout.
            mainFrame = new JFrame("Pump Control and Sequential Protocol");
            mainPanel = new JSplitPane(JSplitPane.VERTICAL_SPLIT);

            logPane = new JScrollPane(
                    log, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
            topPane = new JTabbedPane();


            //Pump set-up tab objects
            pumpConnectionsPanel = new JPanel();

            pumpListLabel = new JLabel("Pump type");
            connectLabel = new JLabel("Serial port");
            portsList = new JComboBox();
            connectButton = new JButton("Connect");
            disconnectButton = new JButton("Disconnect");
            connectedPumpsLabel = new JLabel("List of currently connected pumps");
            connectedPumpsTableModel = new DefaultTableModel(
                    new String[][]{{PumpManager.NO_PUMP_CONNECTED,"",""}},
                    new String[]{"Pump","Sub-Pump","COM port"}
            );
            connectedPumpsTable = new JTable(connectedPumpsTableModel);
            connectedPumpsListPane = new JScrollPane(connectedPumpsTable);


            //Direct Control tab objects
            directPumpControlPanel = new JPanel();

            pumpStatusLabel = new JLabel("Pump status:");
            pumpStatus = new JLabel("Pump not started.");
            pumpSelectionLabel = new JLabel("Select pump to control: ");
            syringeLabel = new JLabel("Syringe");
            rateLabel = new JLabel("Rate ("+PumpManager.FLOW_RATE_UNITS+")");
            rateSlider = new JSlider(JSlider.HORIZONTAL, Integer.MIN_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE);
            rateText = new JLabel("");
            targetLabel = new JLabel("Target Volume (" + PumpManager.VOLUME_UNITS + ")");
            targetField = new JTextField(prefs.get(TARGET, "500"), 6);
            actionLabel = new JLabel("Action to Perform");
            inf = new JRadioButton("Infuse", true);
            wit = new JRadioButton("Withdraw");
            buttons = new ButtonGroup();
            startPumpButton = new JButton("Pump!");
            stopPumpButton = new JButton("Stop!");


            //Sequential Labelling tab objects
            sequentialLabellingPanel = new JPanel();
            sequentialLabellingTopPanel = new JPanel();
            sequentialLabellingStepsPanel = new JPanel();
            sequentialLabellingStepsScrollPane = new JScrollPane
                    (
                            sequentialLabellingStepsPanel,
                            JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                            JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED
                    );

            protocolLoad = new JButton("Load Protocol");
            protocolSave = new JButton("Save Protocol");
            stepsLabel = new JLabel("Steps: ");
            numberOfSteps = new JTextField("", 2);
            addStepButton = new JButton(ADD_STEP);
            removeStepButton = new JButton(REMOVE_STEP);
            startSeqButton = new JButton("Start!");
            stopSeqButton = new JButton("Stop sequence!");
            stopPumpOnSeqButton = new JButton("Stop Pump!");
            syringeReadyLabel = new JLabel("");
            syringeReadyLabel.setBackground(Color.RED);
            syringeReadyButton = new JButton("Syringe Ready");
            seqStatus = new JLabel("Sequence not yet started.");
            pumpStatusOnSeqLabel = new JLabel("Pump status: ");
            pumpStatusOnSeq = new JLabel("Pump not started.");
            suckBetweenStepsLabel = new JLabel("Withdraw between steps?");
            suckBetweenSteps = new JCheckBox("", prefs.getBoolean(SUCK, true));
            suckStepLabel = new JLabel();
            suckStepPanel = sequence.getSuckStep().getStepPanel();

            mainFrame.addComponentListener(panelListener);

            topPane.getModel().addChangeListener(panelListener);

            topPane.setPreferredSize(connectionsDimensions);
            topPane.setMinimumSize(connectionsDimensions);

            logPane.setMinimumSize(logDimensions);
            logPane.setPreferredSize(logDimensions);

            suckStepPanel.setVisible(suckBetweenSteps.isSelected());
            suckBetweenSteps.addChangeListener(panelListener);

            numberOfSteps.setText("" + sequence.size());

            log.set("NanoJ Sequential Labelling");

            //Initiate PumpManager
            pumpManager = PumpManager.INSTANCE;
            pumpManager.setCore(core);
            pumpManager.loadPlugins();
            Thread pumpThread = new Thread(pumpManager);
            pumpThread.start();

            //Initiate SequenceManager
            sequenceManager = SequenceManager.INSTANCE;
            sequenceManager.setPumpManager(pumpManager);
            Thread sequenceThread = new Thread(sequenceManager);
            sequenceThread.start();

            availablePumpsList = new JComboBox(pumpManager.getAvailablePumpsList());
            availablePumpsList.setSelectedItem(prefs.get(PUMP, VIRTUAL_PUMP));
            portsList = new JComboBox(new Vector(NRSerialPort.getAvailableSerialPorts()));
            portsList.addItem(VIRTUAL_PORT);
            portsList.setSelectedItem(prefs.get(PORT, VIRTUAL_PORT));
            syringeComboBox = new JComboBox(SyringeList.getBrandedNames(0));
            pumpSelection = new JComboBox(new String[]{PumpManager.NO_PUMP_CONNECTED});
            buttons.add(inf);
            buttons.add(wit);

            //Pump connection listeners
            connectButton.addActionListener(new Connect());
            disconnectButton.addActionListener(new Disconnect());

            //Direct Control listeners
            pumpSelection.addActionListener(new UpdateCurrentPump());
            availablePumpsList.addActionListener(updateSyringeInformation);
            syringeComboBox.addActionListener(updateSyringeInformation);
            rateSlider.addChangeListener(updateSyringeInformation);
            startPumpButton.addActionListener(new StartPump());
            stopPumpButton.addActionListener(stopPump);

            //Sequential labelling listeners
            protocolLoad.addActionListener(new LoadProtocol());
            protocolSave.addActionListener(new SaveProtocol());
            numberOfSteps.addActionListener(stepChanger);
            addStepButton.addActionListener(stepChanger);
            removeStepButton.addActionListener(stepChanger);
            startSeqButton.addActionListener(new StartSequence());
            stopSeqButton.addActionListener(new StopSequence());
            stopPumpOnSeqButton.addActionListener(stopPump);
            syringeReadyButton.addActionListener(new ToggleMonkeyState());

            //Status observers
            sequenceManager.addObserver(new SequenceObserver());
            pumpManager.addObserver(new UpdatePumpStatus());
            pumpManager.addObserver(updateSyringeInformation);

        } //GUI Objects

        // Layout size variables
        int rowHeight = 24;
        int largeButtonWidth = 150;
        int smallButtonWidth = 50;
        int sizeSecondColumn = 250;

        {
            GroupLayout pumpConnectionsLayout = new GroupLayout(pumpConnectionsPanel);
            pumpConnectionsLayout.setAutoCreateGaps(true);
            pumpConnectionsLayout.setAutoCreateContainerGaps(true);
            pumpConnectionsPanel.setLayout(pumpConnectionsLayout);
            pumpConnectionsPanel.add(pumpListLabel);
            pumpConnectionsPanel.add(availablePumpsList);
            pumpConnectionsPanel.add(connectLabel);
            pumpConnectionsPanel.add(portsList);
            pumpConnectionsPanel.add(connectButton);
            pumpConnectionsPanel.add(disconnectButton);
            pumpConnectionsPanel.add(connectedPumpsListPane);
            pumpConnectionsPanel.add(connectedPumpsLabel);

            pumpConnectionsLayout.setVerticalGroup(
                pumpConnectionsLayout.createSequentialGroup()
                    .addGroup(pumpConnectionsLayout.createParallelGroup()
                        .addComponent(pumpListLabel)
                        .addComponent(availablePumpsList, GroupLayout.PREFERRED_SIZE, rowHeight, GroupLayout.PREFERRED_SIZE)
                    )
                    .addGroup(pumpConnectionsLayout.createParallelGroup()
                        .addComponent(connectLabel)
                        .addComponent(portsList, GroupLayout.PREFERRED_SIZE, rowHeight, GroupLayout.PREFERRED_SIZE)
                        .addComponent(connectButton)
                        .addComponent(disconnectButton)
                    )
                    .addGroup(pumpConnectionsLayout.createParallelGroup().addComponent(connectedPumpsLabel))
                    .addGroup(pumpConnectionsLayout.createParallelGroup().addComponent(connectedPumpsListPane))
            );
            pumpConnectionsLayout.setHorizontalGroup(
                pumpConnectionsLayout.createParallelGroup()
                    .addGroup(pumpConnectionsLayout.createSequentialGroup()
                        .addGroup(pumpConnectionsLayout.createParallelGroup()
                           .addComponent(pumpListLabel, GroupLayout.Alignment.TRAILING)
                           .addComponent(connectLabel, GroupLayout.Alignment.TRAILING)
                        )
                        .addGroup(pumpConnectionsLayout.createParallelGroup()
                            .addComponent(availablePumpsList, GroupLayout.PREFERRED_SIZE, sizeSecondColumn, GroupLayout.PREFERRED_SIZE)
                            .addComponent(portsList, GroupLayout.PREFERRED_SIZE, sizeSecondColumn, GroupLayout.PREFERRED_SIZE)
                        )
                        .addGroup(pumpConnectionsLayout.createSequentialGroup()
                            .addComponent(connectButton)
                            .addComponent(disconnectButton)
                        )
                    )
                    .addGroup(pumpConnectionsLayout.createParallelGroup().addComponent(connectedPumpsLabel))
                    .addGroup(pumpConnectionsLayout.createParallelGroup().addComponent(connectedPumpsListPane))
            );

        } // Pump Connections Tab layout
        {
            GroupLayout directPumpControlLayout = new GroupLayout(directPumpControlPanel);
            directPumpControlLayout.setAutoCreateGaps(true);
            directPumpControlLayout.setAutoCreateContainerGaps(true);
            directPumpControlPanel.setLayout(directPumpControlLayout);
            directPumpControlPanel.add(pumpStatusLabel);
            directPumpControlPanel.add(pumpStatus);
            directPumpControlPanel.add(pumpSelectionLabel);
            directPumpControlPanel.add(pumpSelection);
            directPumpControlPanel.add(syringeLabel);
            directPumpControlPanel.add(syringeComboBox);
            directPumpControlPanel.add(rateLabel);
            directPumpControlPanel.add(rateSlider);
            directPumpControlPanel.add(rateText);
            directPumpControlPanel.add(targetLabel);
            directPumpControlPanel.add(targetField);
            directPumpControlPanel.add(actionLabel);
            directPumpControlPanel.add(inf);
            directPumpControlPanel.add(wit);
            directPumpControlPanel.add(startPumpButton);
            directPumpControlPanel.add(stopPumpButton);

            directPumpControlLayout.setVerticalGroup(
                    directPumpControlLayout.createSequentialGroup()
                            .addGroup(
                                    directPumpControlLayout.createSequentialGroup()
                                            .addGroup(
                                                    directPumpControlLayout.createParallelGroup()
                                                            .addComponent(pumpStatusLabel)
                                                            .addComponent(pumpStatus)
                                            )
                            )
                            .addGroup(
                                    directPumpControlLayout.createParallelGroup()
                                            .addComponent(pumpSelectionLabel)
                                            .addComponent(pumpSelection, GroupLayout.PREFERRED_SIZE, rowHeight, GroupLayout.PREFERRED_SIZE)
                            )
                            .addGroup(
                                    directPumpControlLayout.createParallelGroup()
                                            .addComponent(syringeLabel)
                                            .addComponent(syringeComboBox, GroupLayout.PREFERRED_SIZE, rowHeight, GroupLayout.PREFERRED_SIZE)
                            )
                            .addGroup(
                                    directPumpControlLayout.createParallelGroup()
                                            .addComponent(rateLabel)
                                            .addComponent(rateText)
                                            .addComponent(rateSlider, GroupLayout.PREFERRED_SIZE, rowHeight, GroupLayout.PREFERRED_SIZE)
                            )
                            .addGroup(
                                    directPumpControlLayout.createParallelGroup()
                                            .addComponent(targetLabel)
                                            .addComponent(targetField, GroupLayout.PREFERRED_SIZE, rowHeight, GroupLayout.PREFERRED_SIZE)
                            )
                            .addGroup(
                                    directPumpControlLayout.createParallelGroup()
                                            .addComponent(actionLabel)
                                            .addComponent(inf)
                                            .addComponent(wit)
                            )
                            .addGroup(
                                    directPumpControlLayout.createParallelGroup()
                                            .addComponent(startPumpButton)
                                            .addComponent(stopPumpButton)
                            )
            );

            directPumpControlLayout.setHorizontalGroup(
                    directPumpControlLayout.createSequentialGroup()
                            .addGroup(
                                    directPumpControlLayout.createParallelGroup()
                                            .addGroup(
                                                    directPumpControlLayout.createSequentialGroup()
                                                            .addComponent(pumpStatusLabel)
                                                            .addComponent(pumpStatus)
                                            )
                                            .addGroup(
                                                    directPumpControlLayout.createSequentialGroup()
                                                            .addGroup(
                                                                    directPumpControlLayout.createParallelGroup()
                                                                            .addComponent(pumpSelectionLabel, GroupLayout.Alignment.TRAILING)
                                                                            .addComponent(syringeLabel, GroupLayout.Alignment.TRAILING)
                                                                            .addComponent(rateLabel, GroupLayout.Alignment.TRAILING)
                                                                            .addComponent(targetLabel, GroupLayout.Alignment.TRAILING)
                                                                            .addComponent(actionLabel, GroupLayout.Alignment.TRAILING)
                                                                            .addComponent(startPumpButton, GroupLayout.PREFERRED_SIZE, largeButtonWidth, GroupLayout.PREFERRED_SIZE))
                                                            .addGroup(
                                                                    directPumpControlLayout.createParallelGroup()
                                                                            .addComponent(pumpSelection, GroupLayout.PREFERRED_SIZE, sizeSecondColumn, GroupLayout.PREFERRED_SIZE)
                                                                            .addComponent(syringeComboBox, GroupLayout.PREFERRED_SIZE, sizeSecondColumn, GroupLayout.PREFERRED_SIZE)
                                                                            .addGroup(
                                                                                    directPumpControlLayout.createSequentialGroup()
                                                                                            .addComponent(rateText, GroupLayout.PREFERRED_SIZE, (int) (sizeSecondColumn*0.2), GroupLayout.PREFERRED_SIZE)
                                                                                            .addComponent(rateSlider, GroupLayout.PREFERRED_SIZE, (int) (sizeSecondColumn*0.8), GroupLayout.PREFERRED_SIZE)
                                                                            )
                                                                            .addComponent(targetField, GroupLayout.PREFERRED_SIZE, sizeSecondColumn, GroupLayout.PREFERRED_SIZE)
                                                                            .addGroup(
                                                                                    directPumpControlLayout.createSequentialGroup()
                                                                                            .addComponent(inf)
                                                                                            .addComponent(wit)
                                                                            )
                                                                            .addComponent(stopPumpButton, GroupLayout.PREFERRED_SIZE, largeButtonWidth, GroupLayout.PREFERRED_SIZE))
                                            )
                            )
            );
        } // Direct Labeling Tab layout
        {
            // .Top Panel, with step number chooser and control buttons
            GroupLayout topPanelLayout = new GroupLayout(sequentialLabellingTopPanel);
            topPanelLayout.setAutoCreateGaps(true);
            topPanelLayout.setAutoCreateContainerGaps(true);


            sequentialLabellingTopPanel.setLayout(topPanelLayout);
            sequentialLabellingTopPanel.add(protocolLoad);
            sequentialLabellingTopPanel.add(protocolSave);
            sequentialLabellingTopPanel.add(stepsLabel);
            sequentialLabellingTopPanel.add(numberOfSteps);
            sequentialLabellingTopPanel.add(addStepButton);
            sequentialLabellingTopPanel.add(removeStepButton);
            sequentialLabellingTopPanel.add(startSeqButton);
            sequentialLabellingTopPanel.add(stopSeqButton);
            sequentialLabellingTopPanel.add(stopPumpOnSeqButton);
            sequentialLabellingTopPanel.add(syringeReadyButton);
            sequentialLabellingTopPanel.add(seqStatus);
            sequentialLabellingTopPanel.add(syringeReadyLabel);
            sequentialLabellingTopPanel.add(pumpStatusOnSeqLabel);
            sequentialLabellingTopPanel.add(pumpStatusOnSeq);
            sequentialLabellingTopPanel.add(suckBetweenStepsLabel);
            sequentialLabellingTopPanel.add(suckBetweenSteps);
            sequentialLabellingTopPanel.add(suckStepLabel);
            sequentialLabellingTopPanel.add(suckStepPanel);

            topPanelLayout.setVerticalGroup(
                topPanelLayout.createSequentialGroup()
                    .addGroup(
                        topPanelLayout.createParallelGroup()
                            .addComponent(protocolLoad)
                            .addComponent(protocolSave)
                    )
                    .addGroup(
                        topPanelLayout.createParallelGroup()
                            .addComponent(stepsLabel, GroupLayout.Alignment.CENTER)
                            .addComponent(numberOfSteps, GroupLayout.PREFERRED_SIZE, rowHeight, GroupLayout.PREFERRED_SIZE)
                            .addComponent(addStepButton)
                            .addComponent(removeStepButton)
                            .addComponent(startSeqButton)
                            .addComponent(stopSeqButton)
                            .addComponent(stopPumpOnSeqButton)
                            .addComponent(syringeReadyButton)
                    )
                    .addComponent(syringeReadyLabel)
                    .addComponent(seqStatus)
                    .addGroup(
                        topPanelLayout.createParallelGroup()
                            .addComponent(pumpStatusOnSeqLabel)
                            .addComponent(pumpStatusOnSeq)
                    )
                    .addGroup(
                            topPanelLayout.createParallelGroup()
                                    .addComponent(suckBetweenStepsLabel)
                                    .addComponent(suckBetweenSteps)
                    )
                    .addGroup(
                            topPanelLayout.createParallelGroup()
                                    .addComponent(suckStepLabel)
                                    .addComponent(suckStepPanel, GroupLayout.PREFERRED_SIZE, rowHeight+5, GroupLayout.PREFERRED_SIZE)
                    )
            );

            topPanelLayout.setHorizontalGroup(
                topPanelLayout.createParallelGroup()
                    .addGroup(
                        topPanelLayout.createSequentialGroup()
                            .addComponent(protocolLoad)
                            .addComponent(protocolSave)
                    )
                    .addGroup(
                        topPanelLayout.createSequentialGroup()
                            .addComponent(stepsLabel)
                            .addComponent(numberOfSteps, GroupLayout.PREFERRED_SIZE, 50, GroupLayout.PREFERRED_SIZE)
                            .addComponent(addStepButton, GroupLayout.PREFERRED_SIZE, smallButtonWidth, GroupLayout.PREFERRED_SIZE)
                            .addComponent(removeStepButton, GroupLayout.PREFERRED_SIZE, smallButtonWidth, GroupLayout.PREFERRED_SIZE)
                            .addComponent(startSeqButton)
                            .addComponent(stopSeqButton)
                            .addComponent(stopPumpOnSeqButton)
                            .addComponent(syringeReadyButton)
                    )
                    .addGroup(
                        topPanelLayout.createParallelGroup()
                            .addComponent(syringeReadyLabel)
                            .addComponent(seqStatus)

                    )
                    .addGroup(
                        topPanelLayout.createSequentialGroup()
                            .addComponent(pumpStatusOnSeqLabel)
                            .addComponent(pumpStatusOnSeq)
                    )
                    .addGroup(
                            topPanelLayout.createSequentialGroup()
                                    .addComponent(suckBetweenStepsLabel)
                                    .addComponent(suckBetweenSteps)
                    )
                        .addGroup(
                                topPanelLayout.createSequentialGroup()
                                        .addComponent(suckStepLabel)
                                        .addComponent(suckStepPanel)
                        )
            );

            //Panel with list of steps
            GridLayout steps_layout = new GridLayout(0, 1);
            sequentialLabellingStepsPanel.setLayout(steps_layout);

            for (Step item: sequence) {
                item.addActionListener(updateStepPump);
                sequentialLabellingStepsPanel.add(item.getStepPanel());
            }

            //Bring panels together
            sequentialLabellingPanel.add(sequentialLabellingTopPanel);
            sequentialLabellingPanel.add(sequentialLabellingStepsScrollPane);

            GroupLayout sequential_layout = new GroupLayout(sequentialLabellingPanel);
            sequential_layout.setAutoCreateContainerGaps(true);
            sequential_layout.setAutoCreateGaps(true);
            sequentialLabellingPanel.setLayout(sequential_layout);

            sequential_layout.setVerticalGroup(
                    sequential_layout.createSequentialGroup()
                            .addGroup(
                                    sequential_layout.createSequentialGroup()
                                            .addComponent(sequentialLabellingTopPanel)
                                            .addComponent(sequentialLabellingStepsScrollPane))
            );

            sequential_layout.setHorizontalGroup(
                    sequential_layout.createParallelGroup()
                            .addGroup(
                                    sequential_layout.createParallelGroup()
                                            .addComponent(sequentialLabellingTopPanel)
                                            .addComponent(sequentialLabellingStepsScrollPane)

                            )
            );
        } // Sequential Labeling tab layout

        topPane.addTab(pumpConnectionsName, pumpConnectionsPanel);
        topPane.addTab(directPumpControlName, directPumpControlPanel);
        topPane.addTab(sequentialLabellingName, sequentialLabellingPanel);

        mainPanel.setTopComponent(topPane);
        mainPanel.setBottomComponent(logPane);
        mainPanel.setResizeWeight(1);
        mainFrame.add(mainPanel);

        if (closeOnExit) mainFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        updateGUI();

        mainFrame.setVisible(true);

        //Minor GUI initialization steps
        sequenceManager.defineSequence(sequence);
        sequenceManager.isSyringeExchangeRequiredOnSequence();
        new UpdateSyringeInformation().actionPerformed(new ActionEvent(syringeComboBox, 0, ""));
    }

    public void dispose() {
        try {
            pumpManager.stopPumping();
        } catch (Exception e) {
            e.printStackTrace();
        }
        prefs.put(TARGET, targetField.getText());
        prefs.put(PORT, (String) portsList.getSelectedItem());
        prefs.put(PUMP, (String) availablePumpsList.getSelectedItem());
    }

    public ArrayList<Step> getSteps() {
        return sequence;
    }

    public SequenceManager getSequenceManager() {
        return sequenceManager;
    }

    public String startSequence() {
        if (pumpManager.isConnected()) {
            sequence.setSuck(suckBetweenSteps.isSelected());
            sequenceManager.start(sequence);
            return "Started sequence!";
        }
        else return "Can't do anything until pump is connected.";
    }

    public boolean isRunning() { return sequenceManager.isStarted(); }

    private void updateGUI() {
        mainFrame.validate();
        mainFrame.pack();
    }

    private void updatePumpSelection() {
        String[][] result = pumpManager.getConnectedPumpsList();
        if (result[0][0].equals(PumpManager.NO_PUMP_CONNECTED)) {
            connectedPumps = new String[]{PumpManager.NO_PUMP_CONNECTED};
            pumpSelection.removeAllItems();
            pumpSelection.addItem(PumpManager.NO_PUMP_CONNECTED);
        }
        else {
            connectedPumps = new String[result.length];
            pumpSelection.removeAllItems();
            for (int s= 0; s<connectedPumps.length; s++){
                String entry = result[s][1] + ", " + result[s][2];
                pumpSelection.addItem(entry);
                connectedPumps[s] = entry;
            }

        }
        for (Step step: sequence) step.setPumps(connectedPumps);
        sequence.getSuckStep().setPumps(connectedPumps);
        updateSyringeInformation.actionPerformed(new ActionEvent(connectedPumpsTableModel, 0, ""));
    }

    private void updateStepPanel(ArrayList<HashMap<String,String>> newStepList) {
        // The first item on the list is the suck step information, so we updat the step accordingly
        sequence.getSuckStep().updateStepInformation(newStepList.get(0));
        newStepList.remove(0);

        // Update the number of steps text field
        numberOfSteps.setText("" + newStepList.size());

        // Inform stepChanger so it updates the panel accordingly
        stepChanger.actionPerformed(new ActionEvent(numberOfSteps, 0 ,""));

        // Update each step on the panel with the information given
        for (int s = 0; s<newStepList.size(); s++) {
            sequence.get(s).updateStepInformation(newStepList.get(s));
        }
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

    private class StepChanger implements ActionListener, Observer{
         /*
         When you change the number of steps by entering a number on the text field OR cliking the "+" or "-"
         buttons, this action listener is called. It will change the layout of the protocol in a manner which
         doesn't rewrite the entire protocol list: this means it won't lose what was entered previously.
         */

        @Override
        public void actionPerformed(ActionEvent e) {

            if (e.getActionCommand().equals(ADD_STEP)) {
                int number = Integer.parseInt(numberOfSteps.getText());
                number++;
                numberOfSteps.setText(Integer.toString(number));
            }

            if (e.getActionCommand().equals(REMOVE_STEP)) {
                int number = Integer.parseInt(numberOfSteps.getText());

                // Sanity check: you can't have negative or 0 steps, so only works
                // if there is more than 1 step on the GUI field
                if (number > 1) {
                    number--;
                    //  Update GUI field.
                    numberOfSteps.setText(Integer.toString(number));
                }
            }

            /*
            Get the number of wanted steps. We only need to read the text on the box to know the current number
            of wanted steps since the "+" and "-" buttons change the text on the text box before calling this
            listener;
            */
            int wanted_steps = Integer.parseInt(numberOfSteps.getText());
            int steps_on_list = sequence.size();

            if (steps_on_list != wanted_steps) {
                int difference = wanted_steps - steps_on_list;
                if (difference > 0) {
                    for (int s = 0; s < difference; s++) {
                        int currentStep = steps_on_list + s;
                        // Add to the current step list a new step object with the current index.
                        // The name of the step will be it's index on the list.
                        Step step = new Step(currentStep + 1,connectedPumps);
                        step.addActionListener(updateStepPump);
                        sequence.add(step);
                        // Add this step to the panel on the GUI
                        sequentialLabellingStepsPanel.add(step.getStepPanel());
                    }
                }
                if (difference < 0) {
                    difference = -difference;
                    for (int s = 0; s < difference; s++) {
                        int currentStep = steps_on_list - s - 1;
                        sequentialLabellingStepsPanel.remove(sequence.get(currentStep).getStepPanel());
                        sequence.remove(currentStep);
                    }
                }
            }
            sequenceManager.defineSequence(sequence);
            sequenceManager.isSyringeExchangeRequiredOnSequence();
            updateGUI();
        }

        @Override
        public void update(Observable o, Object arg) {
            Step step = (Step) o;

            if (arg.equals(Step.DOWN_TEXT)) {
                int index = step.getNumber();
                if (index == sequence.size()) return;

                Step nextStep = sequence.get(index);
                step.setNumber(index+1);
                nextStep.setNumber(index);

                sequence.remove(index-1);
                sequence.add(index, step);

                sequentialLabellingStepsPanel.remove(index-1);
                sequentialLabellingStepsPanel.add(step.getStepPanel(), index);

                updateGUI();
            }

            if (arg.equals(Step.UP_TEXT)) {
                int index = step.getNumber() - 1;
                if (index < 1) return;

                Step previousStep = sequence.get(index-1);
                step.setNumber(index);
                previousStep.setNumber(index+1);

                sequence.remove(index);
                sequence.add(index-1, step);

                sequentialLabellingStepsPanel.remove(index);
                sequentialLabellingStepsPanel.add(step.getStepPanel(), index-1);

                updateGUI();
            }

            if (arg.equals(Step.DUPLICATE_TEXT)) {
                int index = step.getNumber();
                Step newStep = new Step();
                newStep.updateStepInformation(step.getStepInformation());
                newStep.setNumber(index + 1);
                newStep.setPumps(connectedPumps);

                sequence.add(index, newStep);

                sequentialLabellingStepsPanel.add(newStep.getStepPanel(), index);

                for (int i = 0; i < sequence.size(); i++) {
                    sequence.get(i).setNumber(i+1);
                }

                numberOfSteps.setText(sequence.size() + "");

                updateGUI();
            }

            if (arg.equals(Step.EXPIRE_TEXT)) {
                int index = step.getNumber()-1;
                sequence.remove(index);
                sequentialLabellingStepsPanel.remove(index);

                for (int i = 0; i < sequence.size(); i++) {
                    sequence.get(i).setNumber(i+1);
                }

                numberOfSteps.setText(sequence.size() + "");

                updateGUI();
            }
        }
    }

    private class Connect implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            String port = (String) portsList.getSelectedItem();
            prefs.put(PORT, port);
            try {
                String isItConnected = pumpManager.connect((String) availablePumpsList.getSelectedItem(), port);
                if (isItConnected == PumpManager.PORT_ALREADY_CONNECTED) {
                    log.message("Tried to connect to " +
                        availablePumpsList.getSelectedItem() + " on port " +
                            port + ", but that port is already in use!");
                    return;
                } else if (isItConnected == Pump.FAILED_TO_CONNECT) {
                    log.message("Tried to connect to " +
                            availablePumpsList.getSelectedItem() + " on port " +
                            port + ", but there was an error!");
                    return;
                }
                if (connectedPumpsTableModel.getRowCount() > 0) {
                    for (int i = connectedPumpsTableModel.getRowCount() - 1; i > -1; i--) {
                        connectedPumpsTableModel.removeRow(i);
                    }
                }
                String[][] results = pumpManager.getConnectedPumpsList();
                for (String[] result: results){
                    connectedPumpsTableModel.addRow(result);
                }
                updatePumpSelection();
                log.message("Connected to " + pumpManager.getAvailablePumpsList()[availablePumpsList.getSelectedIndex()] +
                        " on port " + port);
            } catch (Exception e1) {
                log.message("Error, can not connect, check core log.");
                e1.printStackTrace();
            }
        }
    }

    private class Disconnect implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            boolean success = false;
            if (connectedPumpsTable.getSelectedRow() < 0) {
                return;
            }
            String pump = null;
            String port = null;
            try {
                int selection = connectedPumpsTable.getSelectedRow();
                success = pumpManager.disconnect(selection);
                pump = (String) connectedPumpsTableModel.getValueAt(selection,0);
                port = (String) connectedPumpsTableModel.getValueAt(selection,2);
            } catch (Exception e1) {
                log.message("Error, failed to disconnect properly.");
                e1.printStackTrace();
            }
            if (success) {
                if (connectedPumpsTableModel.getRowCount() > 0) {
                    for (int i = connectedPumpsTableModel.getRowCount() - 1; i > -1; i--) {
                        connectedPumpsTableModel.removeRow(i);
                    }
                }
                String[][] results = pumpManager.getConnectedPumpsList();
                for (String[] result: results){
                    connectedPumpsTableModel.addRow(result);
                }
                log.message("Disconnected from " + pump + " on port " + port + ".");
            }
            updatePumpSelection();
        }
    }

    private class StartPump implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            prefs.put(TARGET, targetField.getText());
            if (pumpManager.isConnected()) {
                try {
                    log.message("" + pumpManager.startPumping(
                            syringeComboBox.getSelectedIndex(),
                            Double.parseDouble(rateText.getText()),
                            Double.parseDouble(targetField.getText()),
                            inf.isSelected()
                    ));
                } catch (Exception e1) {
                    log.message("Error, problem with starting the pump.");
                    e1.printStackTrace();
                }
            } else log.message("Can't do anything until pump is connected.");

        }
    }

    private class StopPump implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            if (pumpManager.isConnected()) {
                try {
                    int pump = sequenceManager.getCurrentPump();
                    if (e.getSource().equals(stopPumpOnSeqButton)  && pump != -1) {
                        pumpManager.setCurrentPump(pump);
                        pumpSelection.setSelectedIndex(pump);
                        pumpManager.stopPumping();
                    }
                    else pumpManager.stopPumping();
                    log.message("Told pump to stop!");
                } catch (Exception e1) {
                    log.message("Error, did not properly stop pump.");
                    e1.printStackTrace();
                }
            } else log.message("Can't do anything until pump is connected.");
        }
    }

    private class LoadProtocol implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            // Create .nsp file chooser
            JFileChooser chooser = new JFileChooser();
            chooser.setFileFilter(new FileNameExtensionFilter("NanoJ SeqLab Protocol file", "nsp"));
            chooser.setDialogTitle("Choose Protocol to load");

            // Get working directory from preferences
            chooser.setCurrentDirectory(new File(prefs.get(SAVE_LOCATION, System.getProperty("user.home"))));

            // Get save location from user
            int returnVal = chooser.showOpenDialog(protocolLoad);

            // If successful, load protocol
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                // Save location in preferences so it is loaded next time the software is loaded
                prefs.put(SAVE_LOCATION,chooser.getSelectedFile().getParent());

                try {
                    // Create file opener
                    FileInputStream fileIn = new FileInputStream(chooser.getSelectedFile().getAbsolutePath());
                    ObjectInputStream in = new ObjectInputStream(fileIn);

                    // Update information with information from file
                    updateStepPanel((ArrayList<HashMap<String,String>>) in.readObject());

                    // Close file
                    in.close();
                    fileIn.close();

                } catch (FileNotFoundException f) {
                    log.message("Error, File Not Found.");
                    f.printStackTrace();
                } catch (IOException i) {
                    log.message("Error, can not read from location.");
                    i.printStackTrace();
                } catch (ClassNotFoundException c) {
                    log.message("Error, File type is incorrect.");
                    c.printStackTrace();
                }
                log.message("Loaded Protocol.");
            }
        }
    }

    private class SaveProtocol implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            // Get working directory from preferences
            File dir = new File(prefs.get(SAVE_LOCATION, System.getProperty("user.home")));

            // Create .nsp file chooser
            JFileChooser chooser = new JFileChooser();
            chooser.setFileFilter(new FileNameExtensionFilter("NanoJ SeqLab Protocol file", "nsp"));
            chooser.setDialogTitle("Choose where to save protocol");
            chooser.setCurrentDirectory(dir);

            // Get save location from user
            int returnVal = chooser.showSaveDialog(protocolSave);

            // If successful, save protocol
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                // Save location in preferences so it is loaded next time the software is loaded
                prefs.put(SAVE_LOCATION,chooser.getSelectedFile().getParent());

                // Make sure file has only one .nsp termination
                if (!chooser.getSelectedFile().getAbsolutePath().endsWith(".nsp")) {
                    dir = new File(chooser.getSelectedFile() + ".nsp");
                } else dir = chooser.getSelectedFile();

                try {
                    // Open file streams
                    FileOutputStream fileOut = new FileOutputStream(dir);
                    ObjectOutputStream out = new ObjectOutputStream(fileOut);

                    // Create output file information
                    ArrayList<HashMap<String,String>> output = new ArrayList<HashMap<String,String>>();
                    // First put in the suck step information
                    output.add(sequence.getSuckStep().getStepInformation());

                    // Then add all the rest of the sequence
                    for (Step step : sequence)
                        output.add(step.getStepInformation());

                    //Write to file
                    out.writeObject(output);
                    out.close();
                    fileOut.close();

                } catch (FileNotFoundException f) {
                    log.message("Error, file not found.");
                    f.printStackTrace();
                } catch (IOException i) {
                    log.message("Error, can not write to target location.");
                    i.printStackTrace();
                }
                log.message("Saved protocol.");
            }
        }
    }

    private class StartSequence implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            prefs.putBoolean(SUCK, suckBetweenSteps.isSelected());
            if (pumpManager.isConnected()) {
                sequence.setSuck(suckBetweenSteps.isSelected());
                sequenceManager.start(sequence);
            }
            else seqStatus.setText("Can't do anything until pump is connected.");
        }
    }

    private class StopSequence implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            if (pumpManager.isConnected()) sequenceManager.stop();
            else seqStatus.setText("Can't do anything until pump is connected.");
        }
    }

    private class SequenceObserver implements Observer {

        @Override
        public void update(Observable o, Object arg) {
            String message = (String) arg;

            if (message.equals(SequenceManager.WAITING_MESSAGE)) {
                seqStatus.setText(sequenceManager.getWaitingMessage());
                return;
            }

            if (message.equals(SequenceManager.SYRINGE_STATUS_CHANGED)) {
                // We reset the background color in case it was set earlier
                syringeReadyLabel.setOpaque(false);

                if(sequenceManager.isSyringeExchangeNeeded() && !sequenceManager.isStarted()) {
                    syringeReadyButton.setEnabled(false);
                    syringeReadyLabel.setText("One or more pumps will be used in more than one step; click " +
                            "'Syringe ready' when syringe has been exchanged.");
                } else if(sequenceManager.isSyringeExchangeNeeded() && sequenceManager.isStarted()) {
                    syringeReadyButton.setEnabled(true);
                    syringeReadyLabel.setText("The next step requires that the syringe is exchanged before pumping!");
                    syringeReadyLabel.setOpaque(true);
                } else if(!sequenceManager.isSyringeExchangeNeeded() && sequenceManager.isStarted()) {
                    syringeReadyButton.setEnabled(false);
                    syringeReadyLabel.setText("This step doesn't require a syringe exchange.");
                } else {
                    syringeReadyButton.setEnabled(false);
                    syringeReadyLabel.setText("There is a different pump for each step, syringe exchange isn't needed.");
                }
            }

            if (message.equals(SequenceManager.MONKEY_CHANGED))
                if (sequenceManager.isSyringeExchangeNeeded()) {
                    // We reset the background color in case it was set earlier
                    syringeReadyLabel.setOpaque(false);
                    syringeReadyLabel.setText(sequenceManager.getMonkeyStatus());
                }

            if (message.equals(SequenceManager.SEQUENCE_STOPPED) || message.equals(SequenceManager.SEQUENCE_FINISHED)) {
                log.message(message);
                seqStatus.setText(message);
            }
        }
    }

    private class ToggleMonkeyState implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            sequenceManager.toggleMonkeyReady();
            syringeReadyLabel.setText(sequenceManager.getMonkeyStatus());
        }
    }

    private class UpdateCurrentPump implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            if (pumpSelection.getSelectedIndex() > -1) pumpManager.setCurrentPump(pumpSelection.getSelectedIndex());
        }
    }

    private class UpdateStepPump implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            sequenceManager.isSyringeExchangeRequiredOnSequence();
        }
    }

    private class UpdateSyringeInformation implements ActionListener, Observer, ChangeListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            if (!(connectedPumpsTableModel.getValueAt(pumpSelection.getSelectedIndex(), 0))
                    .equals(PumpManager.NO_PUMP_CONNECTED)) {
                double[] newInformation = pumpManager.getMaxMin(
                        (String) connectedPumpsTableModel.getValueAt(pumpSelection.getSelectedIndex(),0),
                        SyringeList.getDiameter(syringeComboBox.getSelectedIndex()));

                syringeMin = newInformation[1];
                syringeRate = (newInformation[0] - newInformation[1]);

                double sliderValue = (double) rateSlider.getValue() + (double) Integer.MAX_VALUE + 1;
                sliderValue = sliderValue/ ((2*((double) Integer.MAX_VALUE)) +1);

                double rate = (syringeRate*sliderValue)+syringeMin;
                rateText.setText("" + new BigDecimal(rate).setScale(3, RoundingMode.HALF_EVEN).toPlainString());
            }

            prefs.put(PORT, (String) portsList.getSelectedItem());
            prefs.put(PUMP, (String) availablePumpsList.getSelectedItem());
        }

        @Override
        public void update(Observable o, Object arg) {
            if(arg == PumpManager.CURRENT_PUMP_CHANGED) actionPerformed(new ActionEvent(pumpSelection,0,""));
        }

        @Override
        public void stateChanged(ChangeEvent e) {
            double sliderValue = (double) rateSlider.getValue() + (double) Integer.MAX_VALUE + 1;
            sliderValue = sliderValue/ ((2*((double) Integer.MAX_VALUE)) +1);

            double rate = (syringeRate*sliderValue)+syringeMin;
            rateText.setText("" + new BigDecimal(rate).setScale(3, RoundingMode.HALF_EVEN).toPlainString());
        }
    }

    private class UpdatePumpStatus implements Observer {

        @Override
        public void update(Observable o, Object arg) {
            if (arg != PumpManager.NEW_STATUS_AVAILABLE) return;
            pumpStatus.setText(pumpManager.getStatus());
            pumpStatusOnSeq.setText(pumpManager.getStatus());
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

            if (e.getSource().equals(suckBetweenSteps)) {
                prefs.putBoolean(SUCK, suckBetweenSteps.isSelected());
                suckStepPanel.setVisible(suckBetweenSteps.isSelected());
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
