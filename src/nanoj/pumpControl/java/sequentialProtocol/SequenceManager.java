package nanoj.pumpControl.java.sequentialProtocol;

import nanoj.pumpControl.java.pumps.PumpManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Observable;

public class SequenceManager extends Observable implements Runnable {
    public static final String MONKEY_CHANGED = "Monkey status changed.";
    public static final String SYRINGE_STATUS_CHANGED = "Syringe status changed.";
    public static final String WAITING_MESSAGE = "Waiting.";
    public static final String SEQUENCE_FINISHED = "Sequence Finished.";
    public static final String SEQUENCE_STOPPED = "Sequence Stopped.";
    public static final String SYRINGE_READY = "Syringe is marked as ready.";
    public static final String SYRINGE_NOT_READY = "Syringe is not marked as ready.";

    public final static SequenceManager INSTANCE = new SequenceManager();

    // Foreign objects
    private PumpManager pumpManager = PumpManager.INSTANCE;

    private boolean monkeyReady = true;
    private Sequence sequence;
    private int suckDuration;
    private int currentPump = 0;
    private boolean started = false;
    private boolean alive = true;
    private boolean syringeExchangeNeeded = false;

    private String waitingMessage = "Sequence not yet started.";

    private SequenceManager() {
    } //The no-argument constructor is required for the singleton design pattern.

    @Override
    public void run(){
        while (alive) {
            if (started) {
                for (int current = 0; current < sequence.size(); current++) {
                    if (!started) break;

                    //Tell suck pump to start
                    if (sequence.isSuckTrue() && sequence.get(current).suckBefore()) {
                        suckDuration = sequence.getSuckStep().getDuration();
                        try {
                            currentPump = sequence.getSuckStep().getSelectedPump();

                            pumpManager.startPumping(
                                    currentPump,
                                    sequence.getSuckStep().getSyringeIndex(),
                                    sequence.getSuckStep().getFlowRate(),
                                    sequence.getSuckStep().getTargetVolume(),
                                    sequence.getSuckStep().getAction()
                            );
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    } else suckDuration = 0;

                    long startTime = System.currentTimeMillis() / 1000;

                    while ((System.currentTimeMillis() / 1000 - startTime) < suckDuration) {
                        //Stop sequence if the "Stop" button was pressed.
                        if (!started) break;

                        //Calculate how much time there is still to go in seconds.
                        int timeToGo = (suckDuration - (int) (System.currentTimeMillis() / 1000 - startTime));
                        if (timeToGo < 60) setWaitingMessage("Withdrawal step. Waiting for: " + timeToGo + " seconds.");
                        else if (timeToGo >= 60) setWaitingMessage("Withdrawal step. Waiting for: "
                                + timeToGo / 60 + " more minutes.");
                        try {
                            Thread.sleep(300);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }

                    try {
                        currentPump = sequence.get(current).getSelectedPump();
                        pumpManager.startPumping(
                            currentPump,
                            sequence.get(current).getSyringeIndex(),
                            sequence.get(current).getFlowRate(),
                            sequence.get(current).getTargetVolume(),
                            sequence.get(current).getAction()
                        );
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    //Get starting time in millisecs, convert to seconds
                    startTime = System.currentTimeMillis() / 1000;

                    // If this isn't the last step, then set the "syringe ready" status to false. This makes
                    // the sequence wait until the user has confirmed the syringe exchange.
                    if (current != sequence.size() - 1) setMonkeyReady(false);
                        // If it IS the last step, then we can stop now.
                    else {
                        stop();
                        break;
                    }

                    syringeExchangeNeeded = sequence.get(current+1).isSyringeExchangeRequired();

                    setChanged();
                    notifyObservers(SYRINGE_STATUS_CHANGED);

                    // While the time between steps hasn't passed OR the syringe hasn't been readied...
                    while ((System.currentTimeMillis() / 1000 - startTime) < sequence.get(current).getDuration() || !monkeyReady && syringeExchangeNeeded) {
                        //Stop sequence if the "Stop" button was pressed.
                        if (!started) break;

                        //Calculate how much time there is still to go in seconds.
                        int timeToGo = (sequence.get(current).getDuration() - (int) (System.currentTimeMillis() / 1000 - startTime));
                        // This and the next "else if" let you know how long the pump has been waiting for the syringe
                        //  in minutes after the first minute waiting for the syringe ready signal
                        if (timeToGo < -60) setWaitingMessage("Step: " + sequence.get(current).getNumber() + ", "
                                + sequence.get(current).getName() + ". Waiting for syringe for the past " + - (1 + (timeToGo / 60)) + " minutes.");
                            //  but before that it's in seconds.
                        else if (timeToGo < 0) setWaitingMessage("Step: " + sequence.get(current).getNumber() + ", "
                                + sequence.get(current).getName() + ". Waiting for syringe for the past " + -timeToGo + " seconds.");
                            //  And before that it's seconds to set duration
                        else if (timeToGo < 60) setWaitingMessage("Step: " + sequence.get(current).getNumber() + ", "
                                + sequence.get(current).getName() + ". Waiting for: " + timeToGo + " seconds.");
                            //  and before that it's minutes to set duration
                        else if (timeToGo >= 60) setWaitingMessage("Step: " + sequence.get(current).getNumber() + ", "
                                + sequence.get(current).getName() + ". Waiting for: " + (1 + (timeToGo / 60)) + " more minutes.");
                        try {
                            Thread.sleep(300);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
                setChanged();
                notifyObservers(SEQUENCE_FINISHED);
            }
            try {
                Thread.sleep(300);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public synchronized void start(Sequence givenSteps) {
        this.sequence = givenSteps;
        isSyringeExchangeRequiredOnSequence();
        started = true;
    }

    public void isSyringeExchangeRequiredOnSequence() {
        ArrayList<Boolean> exchangeRequired = new ArrayList<Boolean>(sequence.size());
        //Determine which steps will require a syringe exchange
        ArrayList<String> pumpsPerStep = new ArrayList<String>(sequence.size());
        for (Step step: sequence) {
            pumpsPerStep.add("" + step.getSelectedPump());
        }
        for (int p = 0; p < pumpsPerStep.size(); p++) {
            String pump = pumpsPerStep.get(p);
            if (p != pumpsPerStep.indexOf(pump)) {
                exchangeRequired.add(Collections.frequency(pumpsPerStep, pump) > 1);
                sequence.get(p).setSyringeExchangeRequired(true);
            } else {
                sequence.get(p).setSyringeExchangeRequired(false);
            }
        }
        if (syringeExchangeNeeded != exchangeRequired.contains(true))
            syringeExchangeNeeded = exchangeRequired.contains(true);
        setChanged();
        notifyObservers(SYRINGE_STATUS_CHANGED);
    }

    public synchronized void stop() {
        started = false;
        setChanged();
        notifyObservers(SEQUENCE_STOPPED);
    }

    public synchronized void setMonkeyReady(boolean state) {
        monkeyReady = state;
        setChanged();
        notifyObservers(MONKEY_CHANGED);
    }

    public synchronized boolean toggleMonkeyReady() {
        monkeyReady = !monkeyReady;
        setChanged();
        notifyObservers(MONKEY_CHANGED);
        return monkeyReady;
    }

    private synchronized void setWaitingMessage(String newStatus) {
        waitingMessage = newStatus;
        setChanged();
        notifyObservers(WAITING_MESSAGE);
    }

    public synchronized boolean isSyringeExchangeNeeded() { return syringeExchangeNeeded; }

    public synchronized boolean isStarted() { return started; }

    public synchronized String getWaitingMessage() {
        return waitingMessage;
    }

    public synchronized String getMonkeyStatus() {
        if (monkeyReady) return SYRINGE_READY;
        else return SYRINGE_NOT_READY;
    }

    public int getCurrentPump() {
        if (started) {
            return currentPump;
        } else return -1;
    }

    public void defineSequence(Sequence givenSteps) { sequence = givenSteps; }
}