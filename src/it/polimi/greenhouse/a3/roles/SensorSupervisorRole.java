package it.polimi.greenhouse.a3.roles;

import it.polimi.greenhouse.activities.MainActivity;
import a3.a3droid.A3Message;
import a3.a3droid.A3SupervisorRole;

public class SensorSupervisorRole extends A3SupervisorRole {

	private int currentExperiment;
	private boolean startExperiment;
	
	public SensorSupervisorRole() {
		// TODO Auto-generated constructor stub
		super();		
	}

	@Override
	public void onActivation() {
		// TODO Auto-generated method stub
		
		currentExperiment = Integer.valueOf(getGroupName().split("_")[1]);
		node.connect("server", false, true);
		startExperiment = true;		
	}	

	@Override
	public void logic() {
		showOnScreen("[" + getGroupName() + "_SupRole]");
		active = false;
	}

	@Override
	public void receiveApplicationMessage(A3Message message) {
		// TODO Auto-generated method stub
		switch(message.reason){
		case MainActivity.PING:
			showOnScreen("Forwarding sensor data to server");
			message.object = message.senderAddress + "#" + (String)message.object;
			node.sendToSupervisor(message, "server");
			break;
			
		case MainActivity.PONG:
			showOnScreen("Forwarding server response to sensor");
			String sensorAddress = ((String)message.object).split("#")[0];
			String experiment = ((String)message.object).split("#")[1];
			String sendTime = ((String)message.object).split("#")[2];
			message.object = sendTime;
			channel.sendUnicast(message, sensorAddress);
			break;

		case MainActivity.START_EXPERIMENT:
			if(startExperiment){
				startExperiment = false;
				channel.sendBroadcast(message);
			}
			else
				startExperiment = true;
			
			break;
			
		case MainActivity.STOP_EXPERIMENT:
			break;
			
		case MainActivity.LONG_RTT:
			
			channel.sendBroadcast(new A3Message(MainActivity.STOP_EXPERIMENT_COMMAND, ""));
			break;
		}
	}
}