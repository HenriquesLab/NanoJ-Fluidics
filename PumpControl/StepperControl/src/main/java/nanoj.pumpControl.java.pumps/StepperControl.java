package nanoj.pumpControl.java.pumps;

public class StepperControl extends SerialPump {

	public StepperControl() {
		super(SerialConnection.BaudRate.B_57600);
		subPumps = new String[]{"Single"};
		name = "NanoJ 3D printed pump";
		timeOut = 2000;
	}

	@Override
	public String connectToPump(String connectionIdentifier) throws Exception {
		super.connectToPump(connectionIdentifier);

		String answer;

		try {
			answer = sendCommand("Hello");
			connected = true;
		} catch (Exception e) {
			e.printStackTrace();
			return FAILED_TO_CONNECT;
		}

		return answer;
	}

	@Override
	public void setFlowRate(double flowRate) throws Exception {
		int rate = (int) flowRate;
		sendCommand("s" + rate);
	}

	@Override
	public void setTargetVolume(double target) {
		targetVolume = target;  //Target volume should be given in ul
	}

	@Override
	public void startPumping(Action forward) throws Exception {

		int direction = 2;
		if(forward.equals(Action.Infuse)) direction = 1;

		int volume = (int) targetVolume;

		sendCommand("r" + direction + "" + volume);
	}

	@Override
	public void startPumping(int seconds, Action direction) throws Exception {
		targetVolume = seconds;
		startPumping(direction);
	}

	@Override
	public void stopPump() throws Exception {
		sendCommand("a");
	}

	@Override
	public String getStatus() {
		return "All is well, friend.";
	}

}
