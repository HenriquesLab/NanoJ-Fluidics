package nanoj.pumpControl.java.sequentialProtocol;

import nanoj.pumpControl.java.pumps.PumpManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Observable;

public class SequenceManager extends Observable implements Runnable {
    public static final String NEW_STEP_STARTED = "New step started.";
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
    private float suckDuration;
    private int startStep = 0;
    private int endStep = 0;
    private int currentPump = 0;
    private int currentStep = -1;
    private boolean started = false;
    private boolean alive = true;
    private boolean syringeExchangeNeeded = false;

    private String waitingMessage = "Sequence not yet started.";

    private SequenceManager() { }

    @Override
    public void run(){
        while (alive) {
            if (started) {
                for (currentStep = startStep; currentStep < endStep; currentStep++) {
                    if (!started) break;

                    Step step = sequence.get(currentStep);

                    //Tell suck pump to start
                    if (sequence.isSuckTrue() && step.suckBefore()) {
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

                            setChanged();
                            notifyObservers(NEW_STEP_STARTED);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    } else suckDuration = 0;

                    long startTime = System.currentTimeMillis() / 1000;

                    while ((System.currentTimeMillis() / 1000 - startTime) < suckDuration) {
                        //Stop sequence if the "Stop" button was pressed.
                        if (!started) break;

                        //Calculate how much time there is still to go in seconds.
                        float timeToGo = (suckDuration - (float) (System.currentTimeMillis() / 1000 - startTime));
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
                        currentPump = step.getSelectedPump();
                        pumpManager.startPumping(
                                currentPump,
                                step.getSyringeIndex(),
                                step.getFlowRate(),
                                step.getTargetVolume(),
                                step.getAction()
                        );

                        setChanged();
                        notifyObservers(NEW_STEP_STARTED);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    //Get starting time in millisecs, convert to seconds
                    startTime = System.currentTimeMillis() / 1000;

                    // If this isn't the last step, then set the "syringe ready" status to false. This makes
                    // the sequence wait until the user has confirmed the syringe exchange.
                    if (currentStep < endStep) setMonkeyReady(false);
                        // If it IS the last step, then we can stop now.
                    else {
                        stop();
                        setWaitingMessage("Sequence finished.");
                        break;
                    }

                    if (currentStep < endStep-1)
                        syringeExchangeNeeded = sequence.get(currentStep+1).isSyringeExchangeRequired();

                    setChanged();
                    notifyObservers(SYRINGE_STATUS_CHANGED);

                    // While the time between steps hasn't passed OR the syringe hasn't been readied...
                    while ((System.currentTimeMillis() / 1000 - startTime) < step.getDuration() ||
                            !monkeyReady && syringeExchangeNeeded) {
                        //Stop sequence if the "Stop" button was pressed.
                        if (!started) break;

                        //Calculate how much time there is still to go in seconds.
                        float timeToGo = (step.getDuration() - (float) (System.currentTimeMillis() / 1000 - startTime));
                        // This and the next "else if" let you know how long the pump has been waiting for the syringe
                        //  in minutes after the first minute waiting for the syringe ready signal
                        if (timeToGo < -60) setWaitingMessage("Step: " + step.getNumber() + ", "
                                + step.getName() + ". Waiting for syringe for the past " + - (1 + (timeToGo / 60)) + " minutes.");
                            //  but before that it's in seconds.
                        else if (timeToGo < 0) setWaitingMessage("Step: " + step.getNumber() + ", "
                                + step.getName() + ". Waiting for syringe for the past " + -timeToGo + " seconds.");
                            //  And before that it's seconds to set duration
                        else if (timeToGo < 60) setWaitingMessage("Step: " + step.getNumber() + ", "
                                + step.getName() + ". Waiting for: " + timeToGo + " seconds.");
                            //  and before that it's minutes to set duration
                        else if (timeToGo >= 60) setWaitingMessage("Step: " + step.getNumber() + ", "
                                + step.getName() + ". Waiting for: " + (1 + (timeToGo / 60)) + " more minutes.");
                        try {
                            Thread.sleep(300);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
                currentStep = -1;
                endStep = 0;
                started = false;
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
        startStep = 0;
        endStep = givenSteps.size();
        started = true;
    }

    public synchronized void start(Sequence givenSteps, int start, int end) {
        this.sequence = givenSteps;
        isSyringeExchangeRequiredOnSequence();

        start = (start < 0) ? 0 : start;
        end = (end > givenSteps.size()) ? givenSteps.size() : end;

        startStep = start;
        endStep = end;
        started = true;
    }

    public void isSyringeExchangeRequiredOnSequence() {
        syringeExchangeNeeded = false;
        for (Step step: sequence) {
            if (step.isSyringeExchangeRequired()) {
                syringeExchangeNeeded = true;
                break;
            }
        }

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

    public int getCurrentStep() {
        return currentStep;
    }

    public void defineSequence(Sequence givenSteps) { sequence = givenSteps; }
}