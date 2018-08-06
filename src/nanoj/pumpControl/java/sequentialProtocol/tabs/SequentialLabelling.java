package nanoj.pumpControl.java.sequentialProtocol.tabs;

import nanoj.pumpControl.java.sequentialProtocol.GUI;
import nanoj.pumpControl.java.sequentialProtocol.Sequence;
import nanoj.pumpControl.java.sequentialProtocol.SequenceManager;
import nanoj.pumpControl.java.sequentialProtocol.Step;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Observable;
import java.util.Observer;
import java.util.prefs.Preferences;

public class SequentialLabelling extends JPanel {
    private Preferences prefs = Preferences.userRoot().node(this.getClass().getName());
    GUI gui;
    public String name = "Sequential Protocol";
    
    // Button labels
    private static final String ADD_STEP = "+";
    private static final String REMOVE_STEP = "-";

    // Preferences keys
    public static final String SUCK = "suck";
    public static final String SAVE_LOCATION = "location";

    JPanel topPanel;
    JPanel stepsPanel;
    JScrollPane scrollPane;
    private StepChanger stepChanger = new StepChanger();
    public Sequence sequence = new Sequence(stepChanger);
    UpdateStepPump updateStepPump = new UpdateStepPump();

    JButton protocolLoad;
    JButton protocolSave;
    JLabel stepsLabel;
    JTextField numberOfSteps;
    JButton addStepButton;
    JButton removeStepButton;
    JButton startSeqButton;
    JButton stopSeqButton;
    JLabel syringeReadyLabel;
    JButton syringeReadyButton;
    JLabel seqStatus;
    JLabel pumpStatusOnSeqLabel;
    public JLabel pumpStatusOnSeq;
    JLabel suckBetweenStepsLabel;
    public JCheckBox suckBetweenSteps;
    JLabel suckStepLabel;
    public JPanel suckStepPanel;
    
    public SequenceManager sequenceManager;

    public SequentialLabelling(GUI gui) {
        super();
        this.gui = gui;

        topPanel = new JPanel();
        stepsPanel = new JPanel();
        scrollPane = new JScrollPane
                (
                        stepsPanel,
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
        
        suckStepPanel.setVisible(suckBetweenSteps.isSelected());
        suckBetweenSteps.addChangeListener(gui.panelListener);

        numberOfSteps.setText("" + sequence.size());

        protocolLoad.addActionListener(new LoadProtocol());
        protocolSave.addActionListener(new SaveProtocol());
        numberOfSteps.addActionListener(stepChanger);
        addStepButton.addActionListener(stepChanger);
        removeStepButton.addActionListener(stepChanger);
        startSeqButton.addActionListener(new StartSequence());
        stopSeqButton.addActionListener(new StopSequence());
        syringeReadyButton.addActionListener(new ToggleMonkeyState());
        
        //Initiate SequenceManager
        sequenceManager = SequenceManager.INSTANCE;
        sequenceManager.setPumpManager(gui.pumpManager);
        Thread sequenceThread = new Thread(sequenceManager);
        sequenceThread.start();

        sequenceManager.addObserver(new SequenceObserver());

        new SequentialLabellingLayout(this);
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
                gui.log.message(message);
                seqStatus.setText(message);
            }
        }
    }

    private class StepChanger implements ActionListener, Observer {
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
                        Step step = new Step(currentStep + 1,gui.connectedPumps);
                        step.addActionListener(updateStepPump);
                        sequence.add(step);
                        // Add this step to the panel on the GUI
                        stepsPanel.add(step.getStepPanel());
                    }
                }
                if (difference < 0) {
                    difference = -difference;
                    for (int s = 0; s < difference; s++) {
                        int currentStep = steps_on_list - s - 1;
                        stepsPanel.remove(sequence.get(currentStep).getStepPanel());
                        sequence.remove(currentStep);
                    }
                }
            }
            sequenceManager.defineSequence(sequence);
            sequenceManager.isSyringeExchangeRequiredOnSequence();
            gui.updateGUI();
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

                stepsPanel.remove(index-1);
                stepsPanel.add(step.getStepPanel(), index);

                gui.updateGUI();
            }

            if (arg.equals(Step.UP_TEXT)) {
                int index = step.getNumber() - 1;
                if (index < 1) return;

                Step previousStep = sequence.get(index-1);
                step.setNumber(index);
                previousStep.setNumber(index+1);

                sequence.remove(index);
                sequence.add(index-1, step);

                stepsPanel.remove(index);
                stepsPanel.add(step.getStepPanel(), index-1);

                gui.updateGUI();
            }

            if (arg.equals(Step.DUPLICATE_TEXT)) {
                int index = step.getNumber();
                Step newStep = new Step();
                newStep.updateStepInformation(step.getStepInformation());
                newStep.setNumber(index + 1);
                newStep.setPumps(gui.connectedPumps);

                sequence.add(index, newStep);

                stepsPanel.add(newStep.getStepPanel(), index);

                for (int i = 0; i < sequence.size(); i++) {
                    sequence.get(i).setNumber(i+1);
                }

                numberOfSteps.setText(sequence.size() + "");

                gui.updateGUI();
            }

            if (arg.equals(Step.EXPIRE_TEXT)) {
                int index = step.getNumber()-1;
                sequence.remove(index);
                stepsPanel.remove(index);

                for (int i = 0; i < sequence.size(); i++) {
                    sequence.get(i).setNumber(i+1);
                }

                numberOfSteps.setText(sequence.size() + "");

                gui.updateGUI();
            }
        }
    }
    
    private class UpdateStepPump implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            sequenceManager.isSyringeExchangeRequiredOnSequence();
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
                    gui.log.message("Error, File Not Found.");
                    f.printStackTrace();
                } catch (IOException i) {
                    gui.log.message("Error, can not read from location.");
                    i.printStackTrace();
                } catch (ClassNotFoundException c) {
                    gui.log.message("Error, File type is incorrect.");
                    c.printStackTrace();
                }
                gui.log.message("Loaded Protocol.");
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
                    gui.log.message("Error, file not found.");
                    f.printStackTrace();
                } catch (IOException i) {
                    gui.log.message("Error, can not write to target location.");
                    i.printStackTrace();
                }
                gui.log.message("Saved protocol.");
            }
        }
    }

    private class StartSequence implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            prefs.putBoolean(SUCK, suckBetweenSteps.isSelected());
            if (gui.pumpManager.isConnected()) {
                sequence.setSuck(suckBetweenSteps.isSelected());
                sequenceManager.start(sequence);
            }
            else seqStatus.setText("Can't do anything until pump is connected.");
        }
    }

    private class StopSequence implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            if (gui.pumpManager.isConnected()) sequenceManager.stop();
            else seqStatus.setText("Can't do anything until pump is connected.");
        }
    }
    
    private class ToggleMonkeyState implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            sequenceManager.toggleMonkeyReady();
            syringeReadyLabel.setText(sequenceManager.getMonkeyStatus());
        }
    }

}
