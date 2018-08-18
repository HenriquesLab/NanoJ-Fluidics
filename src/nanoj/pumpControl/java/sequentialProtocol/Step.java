package nanoj.pumpControl.java.sequentialProtocol;

import nanoj.pumpControl.java.pumps.*;
import org.micromanager.utils.ReportingUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.util.HashMap;
import java.util.Observable;
import java.util.Observer;

public class Step extends Observable implements Observer, ActionListener {
    /*
    When you add a step on the GUI, it creates a prototypical object of this type.
    It will be able to update itself when any change is  done and it will also be able to report back it's currently
    entered values.
    */

    // Foreign objects
    private PumpManager pumpManager = PumpManager.INSTANCE;

    // Labels
    public static final String[] TIME_UNITS = {"secs","mins","hours"};
    public static final String[] VOLUME_UNITS = {"ul","ml"};
    public static final String UP_TEXT = "Replace previous step with this one";
    public static final String DOWN_TEXT = "Replace next step with this one";
    public static final String DUPLICATE_TEXT = "Duplicate this step";
    public static final String EXPIRE_TEXT = "Remove step";

    private String[] syringes = SyringeList.getBrandedNames(3);
    private FlowLayout step_layout = new FlowLayout(FlowLayout.LEADING);
    private JPanel step = new JPanel(step_layout);
    private JLabel numberLabel = new JLabel("");
    private JTextField name;
    private JCheckBox suck = new JCheckBox("",false);
    private JTextField time;
    private JComboBox timeUnitsList;
    private JComboBox pumpList;
    private JComboBox syringe = new JComboBox(syringes);
    private FlowRateSlider rateSlider;
    private JTextField volume;
    private JComboBox volumeUnitsList;
    private JComboBox action = new JComboBox(Pump.Action.values());
    private boolean syringeExchangeRequired = false;
    private int number;

    private boolean editing = false;

    // Constructors
    public Step() {
        this(1);
    }

    public Step(int num) {
        this(num,"Name",true,1,0, 0,"500",0, Pump.Action.Infuse);
    }

    Step(int num, String givenName, boolean givenSuck, int givenTime, int givenTimeUnits,
         int givenSyringe, String givenVolume, int givenVolumeUnits, Pump.Action givenAction)
    {

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            ReportingUtils.logError(e);
        }

        number = num;
        if (num > 0) {
            step.add(new ButtonPanel());

            numberLabel = new JLabel("" + num);
            step.add(numberLabel);
        }

        name = new JTextField(givenName, 7);
        step.add(name);

        suck.setSelected(givenSuck);
        suck.setToolTipText("Withdraw before this step?");
        step.add(suck);

        time = new JTextField(Integer.toString(givenTime), 2);
        Listener listener = new Listener();
        time.addActionListener(listener);
        step.add(time);

        timeUnitsList = new JComboBox(TIME_UNITS);
        timeUnitsList.setSelectedIndex(givenTimeUnits);
        step.add(timeUnitsList);

        String pumps[];
        if (pumpManager.noPumpsConnected())
             pumps = new String[]{PumpManager.NO_PUMP_CONNECTED};
        else
            pumps = pumpManager.getAllFullNames();

        pumpList = new JComboBox(pumps);
        pumpList.setPrototypeDisplayValue(PumpManager.NO_PUMP_CONNECTED);
        pumpList.addActionListener(this);
        step.add(pumpList);

        rateSlider = new FlowRateSlider();
        rateSlider.setPreferredSize(new Dimension(150,28));
        step.add(rateSlider);

        syringe.setSelectedIndex(givenSyringe);
        syringe.addActionListener(listener);
        step.add(syringe);

        volume = new JTextField(givenVolume, 3);
        step.add(volume);

        volumeUnitsList = new JComboBox(VOLUME_UNITS);
        volumeUnitsList.setSelectedIndex(givenVolumeUnits);
        step.add(volumeUnitsList);

        action.setSelectedItem(givenAction);
        step.add(action);

        listener.updateSyringeInformation();

        pumpManager.addObserver(this);
    }

    @Override
    public void update(Observable o, Object arg) {
        if (arg.equals(PumpManager.NEW_PUMP_CONNECTED) ||
                arg.equals(PumpManager.PUMP_DISCONNECTED)) {

            if (pumpManager.noPumpsConnected()) {
                pumpList.removeAllItems();
                pumpList.addItem(PumpManager.NO_PUMP_CONNECTED);
            }
            else {
                editing = true;
                pumpList.removeAllItems();
                for (ConnectedSubPump pump: pumpManager.getConnectedPumpsList())
                    pumpList.addItem(pump.getFullName());


                editing = false;
            }

            if (pumpManager.noPumpsConnected())
                return;

            rateSlider.setPumpSelection(pumpList.getSelectedIndex());
            rateSlider.setSyringeDiameter(SyringeList.getDiameter(syringe.getSelectedIndex()));
        }
    }

    public double getFlowRate() {
        return rateSlider.getCurrentFlowRate();
    }

    public int getRate() {
        return rateSlider.getSliderValue();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource().equals(pumpList))
            if (pumpList.getItemCount() >= 0 &&
                    pumpManager.anyPumpsConnected() &&
                    !editing)
            rateSlider.setPumpSelection(pumpList.getSelectedIndex());
    }

    // Inner Classes

    class ButtonPanel extends Container {

        ButtonPanel() {

            int height = 21;

            int width = 21;

            ImageIcon up = new ImageIcon();
            ImageIcon down = new ImageIcon();
            ImageIcon duplicate = new ImageIcon();
            ImageIcon expire = new ImageIcon();
            try {
                up = new ImageIcon(Step.class.getResource("/up.png"));
                down  = new ImageIcon(Step.class.getResource("/down.png"));
                duplicate = new ImageIcon(Step.class.getResource("/duplicate.png"));
                expire = new ImageIcon(Step.class.getResource("/expire.png"));

                up = new ImageIcon(up.getImage().getScaledInstance(width, height, Image.SCALE_SMOOTH));
                down  = new ImageIcon(down.getImage().getScaledInstance(width, height, Image.SCALE_SMOOTH));
                duplicate = new ImageIcon(duplicate.getImage().getScaledInstance(width, height, Image.SCALE_SMOOTH));
                expire = new ImageIcon(expire.getImage().getScaledInstance(width, height, Image.SCALE_SMOOTH));

            } catch (Exception e) {
                e.printStackTrace();
            }

            this.setLayout(new GridBagLayout());
            GridBagConstraints constraints = new GridBagConstraints();
            constraints.fill = GridBagConstraints.HORIZONTAL;
            constraints.weightx = 0.5;

            constraints.gridx = 0;
            constraints.gridy = 0;
            this.add(new StepButton(up, UP_TEXT), constraints);

            constraints.gridx = 1;
            constraints.gridy = 0;
            this.add(new StepButton(down, DOWN_TEXT), constraints);

            constraints.gridx = 2;
            constraints.gridy = 0;
            this.add(new StepButton(duplicate, DUPLICATE_TEXT), constraints);

            constraints.gridx = 3;
            constraints.gridy = 0;
            this.add(new StepButton(expire, EXPIRE_TEXT), constraints);

            height = 24;
            width = height * 4;

            this.setPreferredSize(new Dimension(width,height));
        }

        class StepButton extends JButton {

            StepButton(ImageIcon icon, String tooltip) {
                super(icon);
                this.addActionListener(new ButtonListener());
                this.setToolTipText(tooltip);
            }
        }

        class ButtonListener implements ActionListener {

            @Override
            public void actionPerformed(ActionEvent e) {
                setChanged();
                JButton source = (JButton) e.getSource();
                notifyObservers(source.getToolTipText());
            }
        }
    }

    class Listener extends MouseAdapter implements ActionListener {

        void updateSyringeInformation() {
            if (pumpManager.anyPumpsConnected()) {
                rateSlider.setPumpSelection(pumpList.getSelectedIndex());
                rateSlider.setSyringeDiameter(SyringeList.getDiameter(syringe.getSelectedIndex()));
            }
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (e.getSource().equals(syringe))
                updateSyringeInformation();

            if (e.getSource().equals(time)) {
                float duration;

                try {
                    duration = Float.parseFloat(time.getText());
                } catch (Exception e1) {
                    duration = 1;
                }

                if (duration < 0 ) {
                    time.setText("0");
                }
                else time.setText("" + Math.round(duration));
            }
        }

    }

    // Methods

    public HashMap<String,String> getStepInformation() {
        HashMap<String,String> info = new HashMap<String,String>();
        info.put("number", numberLabel.getText());
        info.put("pump", (String) pumpList.getSelectedItem());
        info.put("name",name.getText());
        info.put("suck", "" + suck.isSelected());
        info.put("time",time.getText());
        info.put("timeUnits","" + timeUnitsList.getSelectedIndex());
        info.put("syringe","" + syringe.getSelectedIndex());
        info.put("rate", "" + rateSlider.getSliderValue());
        info.put("volume",volume.getText());
        info.put("volumeUnits","" + volumeUnitsList.getSelectedIndex());
        info.put("action","" + action.getSelectedIndex());
        return info;
    }

    public void addActionListener(ActionListener a) {
        pumpList.addActionListener(a);
    }

    public void updateStepInformation(HashMap<String,String> givenInformation) {
        String pump = givenInformation.get("pump");

        for (int i = 0; i < pumpList.getItemCount(); i++) {
            if (pump.equals((String) pumpList.getItemAt(i))) {
                if (pumpManager.isConnected(i))
                    pumpList.setSelectedItem(pump);
                break;
            }
        }

        numberLabel.setText(givenInformation.get("number"));
        name.setText(givenInformation.get("name"));
        suck.setSelected(Boolean.parseBoolean(givenInformation.get("suck")));
        time.setText(givenInformation.get("time"));
        timeUnitsList.setSelectedIndex(Integer.parseInt(givenInformation.get("timeUnits")));
        syringe.setSelectedIndex(Integer.parseInt(givenInformation.get("syringe")));
        rateSlider.setFlowRate(Integer.parseInt(givenInformation.get("rate")));
        volume.setText(givenInformation.get("volume"));
        volumeUnitsList.setSelectedIndex(Integer.parseInt(givenInformation.get("volumeUnits")));
        action.setSelectedIndex(Integer.parseInt(givenInformation.get("action")));
    }

    public double getTargetVolume() {
        double targetVolume = Double.parseDouble(volume.getText());
        if (volumeUnitsList.getSelectedItem() == VOLUME_UNITS[1])
            targetVolume = targetVolume * 1000;
        return targetVolume;
    }

    public int getDuration() {
        int duration = Integer.parseInt(time.getText());
        if(timeUnitsList.getSelectedItem() == TIME_UNITS[1])
            duration = duration*60;
        else if(timeUnitsList.getSelectedItem() == TIME_UNITS[2])
            duration = duration*3600;
        return duration;
    }

    public void setIsSuckStep() {
        step.remove(suck);
        step.remove(name);
    }

    public JPanel getStepPanel() {
        return step;
    }

    public String getNumberLabel() {
        return numberLabel.getText();
    }

    public int getNumber() {
        return number;
    }

    public void setNumber(int number) {
        numberLabel.setText("" + number);
        this.number = number;
    }

    public String getName() {
        return name.getText();
    }

    public boolean suckBefore() {
        return suck.isSelected();
    }

    public String getTimeUnits() {
        return TIME_UNITS[timeUnitsList.getSelectedIndex()];
    }

    public int getSelectedPump() {
        return pumpList.getSelectedIndex();
    }

    public int getSyringeIndex() {
        return syringe.getSelectedIndex();
    }

    public Pump.Action getAction() {
        return Pump.Action.valueOf(action.getSelectedItem().toString());
    }

    public void setSyringeExchangeRequired(boolean value ) {
        syringeExchangeRequired = value;
    }

    public boolean isSyringeExchangeRequired() {
        return syringeExchangeRequired;
    }


}
