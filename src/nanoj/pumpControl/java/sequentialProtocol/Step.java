package nanoj.pumpControl.java.sequentialProtocol;

import nanoj.pumpControl.java.pumps.*;
import org.micromanager.utils.ReportingUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
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

    public enum TimeUnit {
        SECS("secs"),
        MINS("mins"),
        HOURS("hours");

        public final String name;
        TimeUnit(String text) {
            this.name = text;
        }
        public String toString() {
            return name;
        }
    }

    public enum VolumeUnit {
        UL("ul"),
        ML("ml");

        public final String name;
        VolumeUnit(String text) {
            this.name = text;
        }
        public String toString() {
            return name;
        }
    }

    public static final String UP_TEXT = "Replace previous step with this one";
    public static final String DOWN_TEXT = "Replace next step with this one";
    public static final String DUPLICATE_TEXT = "Duplicate this step";
    public static final String EXPIRE_TEXT = "Remove step";
    public static final String EXCHANGE_STATUS_CHANGED = "Exchange status changed";

    private FlowLayout step_layout = new FlowLayout(FlowLayout.LEADING);
    private JPanel step = new JPanel(step_layout);
    private JLabel numberLabel = new JLabel("");
    private JTextField name;
    private JCheckBox suck = new JCheckBox("Wd",false);
    private JTextField time;
    private JComboBox timeUnitsList;
    private JCheckBox exchange = new JCheckBox("Ex");
    private JComboBox pumpList;
    private JComboBox syringeList = new JComboBox(Syringe.getAllBrandedNames());
    private FlowRateSlider rateSlider;
    private JTextField volume;
    private JComboBox volumeUnitsList;
    private JLabel peristalticLabel = new JLabel("secs.");
    private JComboBox action = new JComboBox(Pump.Action.values());
    private int number;

    private boolean editing = false;

    // Constructors
    public Step() {
        this(1);
    }

    public Step(int num) {
        this(num,"Name",true,false,1,TimeUnit.SECS, Syringe.PERISTALTIC,500,VolumeUnit.ML, Pump.Action.Infuse);
    }

    Step(int num, String name, boolean suck, boolean exchange, int time, TimeUnit timeUnit,
         Syringe syringe, double volume, VolumeUnit volumeUnit, Pump.Action action)
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

        this.name = new JTextField(name, 7);
        step.add(this.name);

        this.suck.setSelected(suck);
        this.suck.setToolTipText("Withdraw before this step?");
        step.add(this.suck);

        this.time = new JTextField(Integer.toString(time), 2);
        Listener listener = new Listener();
        this.time.addActionListener(listener);
        step.add(this.time);

        timeUnitsList = new JComboBox(TimeUnit.values());
        timeUnitsList.setSelectedIndex(timeUnit.ordinal());
        step.add(timeUnitsList);

        String pumps[];
        if (pumpManager.noPumpsConnected())
            pumps = new String[]{PumpManager.NO_PUMP_CONNECTED};
        else
            pumps = pumpManager.getAllFullNames();

        this.exchange.setSelected(exchange);
        this.exchange.addActionListener(this);
        step.add(this.exchange);

        pumpList = new JComboBox(pumps);
        pumpList.setPrototypeDisplayValue(PumpManager.NO_PUMP_CONNECTED);
        pumpList.addActionListener(this);
        step.add(pumpList);

        rateSlider = new FlowRateSlider();
        rateSlider.setPreferredSize(new Dimension(150,28));
        step.add(rateSlider);

        syringeList.setSelectedIndex(syringe.ordinal());
        syringeList.addActionListener(listener);
        step.add(syringeList);

        this.volume = new JTextField(""+volume, 3);
        step.add(this.volume);

        volumeUnitsList = new JComboBox(VolumeUnit.values());
        volumeUnitsList.setSelectedIndex(volumeUnit.ordinal());
        step.add(volumeUnitsList);
        step.add(peristalticLabel);

        this.action.setSelectedItem(action);
        step.add(this.action);

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
            rateSlider.setSyringeDiameter(Syringe.values()[syringeList.getSelectedIndex()].diameter);
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
        if (e.getSource().equals(pumpList)) {
            if (pumpList.getItemCount() >= 0 &&
                    pumpManager.anyPumpsConnected() &&
                    !editing)
                rateSlider.setPumpSelection(pumpList.getSelectedIndex());
        }
        else if (e.getSource().equals(exchange)) {
            setChanged();
            notifyObservers(EXCHANGE_STATUS_CHANGED);
        }
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

    class Listener implements ActionListener {

        void updateSyringeInformation() {
            Syringe syringe = Syringe.values()[syringeList.getSelectedIndex()];

            if (syringe.equals(Syringe.PERISTALTIC)) {
                volumeUnitsList.setSelectedIndex(VolumeUnit.UL.ordinal());

                volumeUnitsList.setEnabled(false);
                volumeUnitsList.setVisible(false);
                peristalticLabel.setVisible(true);

            } else {
                volumeUnitsList.setEnabled(true);
                volumeUnitsList.setVisible(true);
                peristalticLabel.setVisible(true);
            }

            if (pumpManager.anyPumpsConnected()) {
                rateSlider.setPumpSelection(pumpList.getSelectedIndex());
                rateSlider.setSyringeDiameter(syringe.diameter);
            }
        }


        @Override
        public void actionPerformed(ActionEvent e) {
            if (e.getSource().equals(syringeList)) {
                updateSyringeInformation();
            }

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
        info.put("exchange", "" + exchange.isSelected());
        info.put("pump", (String) pumpList.getSelectedItem());
        info.put("name",name.getText());
        info.put("suck", "" + suck.isSelected());
        info.put("time",time.getText());
        info.put("timeUnits","" + timeUnitsList.getSelectedIndex());
        info.put("syringe","" + syringeList.getSelectedIndex());
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

        exchange.setSelected(Boolean.parseBoolean(givenInformation.get("exchange")));
        numberLabel.setText(givenInformation.get("number"));
        name.setText(givenInformation.get("name"));
        suck.setSelected(Boolean.parseBoolean(givenInformation.get("suck")));
        time.setText(givenInformation.get("time"));
        timeUnitsList.setSelectedIndex(Integer.parseInt(givenInformation.get("timeUnits")));
        syringeList.setSelectedIndex(Integer.parseInt(givenInformation.get("syringe")));
        rateSlider.setFlowRate(Integer.parseInt(givenInformation.get("rate")));
        volume.setText(givenInformation.get("volume"));
        volumeUnitsList.setSelectedIndex(Integer.parseInt(givenInformation.get("volumeUnits")));
        action.setSelectedIndex(Integer.parseInt(givenInformation.get("action")));
    }

    public double getTargetVolume() {
        double targetVolume = Double.parseDouble(volume.getText());
        if (volumeUnitsList.getSelectedItem() == VolumeUnit.ML.name)
            targetVolume = targetVolume * 1000;
        return targetVolume;
    }

    public float getDuration() {
        float duration = Integer.parseInt(time.getText());
        if(timeUnitsList.getSelectedIndex() == TimeUnit.MINS.ordinal())
            duration = duration*60;
        else if(timeUnitsList.getSelectedIndex() == TimeUnit.HOURS.ordinal())
            duration = duration*3600;
        return duration;
    }

    public void setIsSuckStep() {
        step.remove(exchange);
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

    public TimeUnit getTimeUnit() {
        return TimeUnit.values()[timeUnitsList.getSelectedIndex()];
    }

    public int getSelectedPump() {
        return pumpList.getSelectedIndex();
    }

    public Syringe getSyringeIndex() {
        return Syringe.values()[syringeList.getSelectedIndex()];
    }

    public Pump.Action getAction() {
        return Pump.Action.valueOf(action.getSelectedItem().toString());
    }

    public boolean isSyringeExchangeRequired() {
        return exchange.isSelected();
    }


}
