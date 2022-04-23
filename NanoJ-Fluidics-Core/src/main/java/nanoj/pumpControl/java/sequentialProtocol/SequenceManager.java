package nanoj.pumpControl.java.sequentialProtocol;

import nanoj.pumpControl.java.pumps.PumpManager;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Observable;
import java.util.TimeZone;

public class SequenceManager extends Observable implements Runnable {
    public static final String NEW_STEP_STARTED = "New step started.";
    public static final String MONKEY_CHANGED = "Monkey status changed.";
    public static final String SYRINGE_STATUS_CHANGED = "Syringe status changed.";
    public static final String WAITING_MESSAGE = "Waiting.";
    public static final String SEQUENCE_FINISHED = "Sequence Finished.";
    public static final String SEQUENCE_STOPPED = "Sequence Stopped.";
    public static final String SYRINGE_READY = "Syringe is marked as ready.";
    public static final String SYRINGE_NOT_READY = "Syringe is not marked as ready.";

    // Foreign objects
    private final PumpManager pumpManager = PumpManager.INSTANCE;

    private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm:ss");

    static {
        TIME_FORMAT.setTimeZone(TimeZone.getTimeZone("GMT"));
    }

    private boolean monkeyReady = true;
    private Sequence sequence;
    private int startStep = 0;
    private int endStep = 0;
    private int currentPump = 0;
    private int currentStep = -1;
    /**
     * Signals whether the thread has been started. Should never be reset to false and should never be
     * set to true more than once (at thread start)
     */
    private boolean started = false;
    private boolean syringeExchangeNeeded = false;

    private String waitingMessage = "Sequence not yet started.";

    private final Thread thread;

    public SequenceManager(Sequence stepSequence) {
        sequence = stepSequence;
        isSyringeExchangeRequiredOnSequence();
        thread = new Thread(this);
    }

    public synchronized void startSequence() {
        startSequence(0, sequence.size());
    }

    public synchronized void startSequence(int startStep, int endStep) {
        if (thread.isAlive() || started) return;

        this.startStep = Math.max(startStep, 0);
        this.endStep = Math.min(endStep, sequence.size());
        isSyringeExchangeRequiredOnSequence();
        thread.start();
    }

    @Override
    public void run(){
        started = true;
        try {
            for (currentStep = startStep; currentStep < endStep; currentStep++) {
                if (thread.isInterrupted()) throw new InterruptedException();

                Step step = sequence.get(currentStep);

                //Tell suck pump to start
                float suckDuration;
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

                suckDuration *= 1000;

                long startTime = System.currentTimeMillis();

                while ((System.currentTimeMillis() - startTime) < suckDuration) {
                    if (thread.isInterrupted()) throw new InterruptedException();

                    //Calculate how much time there is still to go in seconds.
                    float timeToGo = (suckDuration - (float) (System.currentTimeMillis() - startTime));

                    String formattedTime = TIME_FORMAT.format(new Date((long) timeToGo));

                    if (timeToGo > 60000)
                        setWaitingMessage("Withdrawal step. Waiting for: " + formattedTime);
                    else
                        setWaitingMessage("Withdrawal step. Waiting for: " +
                                Math.round(timeToGo/1000) + " seconds");
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

                startTime = System.currentTimeMillis();

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
                while ( (System.currentTimeMillis() - startTime) < step.getDuration()* 1000L ||
                        !monkeyReady && syringeExchangeNeeded) {
                    if (thread.isInterrupted()) throw new InterruptedException();

                    //Calculate how much time there is still to go in seconds.
                    float timeToGo = (step.getDuration()*1000 - (float) (System.currentTimeMillis() - startTime));

                    String formattedTime = TIME_FORMAT.format(new Date((long) timeToGo));

                    timeToGo /= 1000;
                    timeToGo = Math.round(timeToGo);

                    // This and the next "else if" let you know how long the pump has been waiting for the syringe
                    //  in minutes after the first minute waiting for the syringe ready signal
                    if (timeToGo < -60) setWaitingMessage("Step: " + step.getNumber() + ", "
                            + step.getName() + ". Waiting for syringe for " + formattedTime);
                        //  but before that it's in seconds.
                    else if (timeToGo < 0) setWaitingMessage("Step: " + step.getNumber() + ", "
                            + step.getName() + ". Waiting for syringe for the past " + -timeToGo + " seconds.");
                        //  And before that it's seconds to set duration
                    else if (timeToGo < 60) setWaitingMessage("Step: " + step.getNumber() + ", "
                            + step.getName() + ". Waiting for: " + timeToGo + " seconds.");
                        //  and before that it's minutes to set duration
                    else setWaitingMessage("Step: " + step.getNumber() + ", "
                                + step.getName() + ". Waiting for: " + formattedTime);
                }
            }
        } catch (InterruptedException ignored) {
        } finally {
            setChanged();
            notifyObservers(SEQUENCE_FINISHED);
        }
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
        thread.interrupt();
        setChanged();
        notifyObservers(SEQUENCE_STOPPED);
    }

    public synchronized void setMonkeyReady(boolean state) {
        monkeyReady = state;
        setChanged();
        notifyObservers(MONKEY_CHANGED);
    }

    public synchronized void toggleMonkeyReady() {
        monkeyReady = !monkeyReady;
        setChanged();
        notifyObservers(MONKEY_CHANGED);
    }

    private synchronized void setWaitingMessage(String newStatus) {
        waitingMessage = newStatus;
        setChanged();
        notifyObservers(WAITING_MESSAGE);
    }

    public synchronized boolean isSyringeExchangeNeeded() { return syringeExchangeNeeded; }

    public synchronized boolean isStarted() { return thread.isAlive(); }

    public synchronized String getWaitingMessage() {
        return waitingMessage;
    }

    public synchronized String getMonkeyStatus() {
        if (monkeyReady) return SYRINGE_READY;
        else return SYRINGE_NOT_READY;
    }

    public int getCurrentPump() {
        if (thread.isAlive()) {
            return currentPump;
        } else return -1;
    }

    public int getCurrentStep() {
        return currentStep;
    }

    public void defineSequence(Sequence givenSteps) { sequence = givenSteps; }
}