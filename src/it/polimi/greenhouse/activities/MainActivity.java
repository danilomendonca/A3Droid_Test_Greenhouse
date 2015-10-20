package it.polimi.greenhouse.activities;

import it.polimi.greenhouse.a3.groups.ActuatorsDescriptor;
import it.polimi.greenhouse.a3.groups.ControlDescriptor;
import it.polimi.greenhouse.a3.groups.MonitoringDescriptor;
import it.polimi.greenhouse.a3.groups.ServerDescriptor;
import it.polimi.greenhouse.a3.roles.ActuatorFollowerRole;
import it.polimi.greenhouse.a3.roles.ActuatorSupervisorRole;
import it.polimi.greenhouse.a3.roles.ControlFollowerRole;
import it.polimi.greenhouse.a3.roles.ControlSupervisorRole;
import it.polimi.greenhouse.a3.roles.SensorFollowerRole;
import it.polimi.greenhouse.a3.roles.SensorSupervisorRole;
import it.polimi.greenhouse.a3.roles.ServerFollowerRole;
import it.polimi.greenhouse.a3.roles.ServerSupervisorRole;
import it.polimit.greenhouse.R;

import java.util.ArrayList;

import a3.a3droid.A3DroidActivity;
import a3.a3droid.A3Message;
import a3.a3droid.A3Node;
import a3.a3droid.GroupDescriptor;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.view.Menu;
import android.view.View;
import android.widget.EditText;

public class MainActivity extends A3DroidActivity{
	
	public static final String EXPERIMENT_PREFIX = "A3Test3_";
	public static final int NUMBER_OF_EXPERIMENTS = 32;
	
	public static final int CREATE_GROUP = 31;
	public static final int STOP_EXPERIMENT = 32;
	public static final int CREATE_GROUP_USER_COMMAND = 33;
	public static final int LONG_RTT = 34;
	public static final int STOP_EXPERIMENT_COMMAND = 35;
	public static final int PING = 36;
	public static final int PONG = 37;
	public static final int NEW_PHONE = 38;
	public static final int START_EXPERIMENT_USER_COMMAND = 39;
	public static final int START_EXPERIMENT = 40;
	public static final int DATA = 41;
	public static final int START_SENSOR = 42;
	public static final int START_ACTUATOR = 43;
	public static final int START_SERVER = 50;
	
	private A3Node node;
	private EditText inText;
	private Handler toGuiThread;
	private Handler fromGuiThread;
	private EditText experiment;
	public static int runningExperiment;
	private boolean experimentRunning = false;
	//private EditText numberOfGroupsToCreate;
	
	public static void setRunningExperiment(int runningExperiment) {
		MainActivity.runningExperiment = runningExperiment;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.activity_main);
		
		toGuiThread = new Handler() {
			@Override
			public void handleMessage(Message msg) {
				
				inText.append(msg.obj + "\n");
			}
		};
		
		HandlerThread thread = new HandlerThread("Handler");
		thread.start();
		fromGuiThread = new Handler(thread.getLooper()){
			@Override
			public void handleMessage(Message msg){
				ArrayList<String> roles = new ArrayList<String>();
				roles.add(ControlSupervisorRole.class.getName());
				roles.add(ControlFollowerRole.class.getName());
				ArrayList<GroupDescriptor> groupDescriptors = new ArrayList<GroupDescriptor>();
				groupDescriptors.add(new ControlDescriptor());
				switch (msg.what) {
					case STOP_EXPERIMENT_COMMAND:
						node.sendToSupervisor(new A3Message(LONG_RTT, ""), "control");
						experimentRunning = false;
						break;
					case START_EXPERIMENT_USER_COMMAND:
						if(experimentRunning)
							node.sendToSupervisor(new A3Message(START_EXPERIMENT_USER_COMMAND, ""), "control");
						break;
					case START_SENSOR:
						if(experimentRunning)
							break;
						roles.add(SensorSupervisorRole.class.getName());
						roles.add(SensorFollowerRole.class.getName());		
						roles.add(ServerFollowerRole.class.getName());
						groupDescriptors.add(new MonitoringDescriptor());
						groupDescriptors.add(new ServerDescriptor());
						node = new A3Node(MainActivity.this, roles, groupDescriptors);
						node.connect("control", true, true);
						node.connect("monitoring_" + experiment.getText().toString(), false, true);
						node.sendToSupervisor(new A3Message(CREATE_GROUP_USER_COMMAND, "monitoring_" + experiment.getText().toString()), "control");
						experimentRunning = true;
						break;	
					case START_ACTUATOR:
						if(experimentRunning)
							break;
						roles.add(ActuatorSupervisorRole.class.getName());
						roles.add(ActuatorFollowerRole.class.getName());
						roles.add(ServerFollowerRole.class.getName());
						groupDescriptors.add(new ActuatorsDescriptor());
						node = new A3Node(MainActivity.this, roles, groupDescriptors);
						node.connect("control", true, true);
						node.connect("actuators_" + experiment.getText().toString(), false, true);
						node.sendToSupervisor(new A3Message(CREATE_GROUP_USER_COMMAND, "actuators_" + experiment.getText().toString()), "control");
						experimentRunning = true;
						break;
					case START_SERVER:	
						if(experimentRunning)
							break;
						roles.add(ServerSupervisorRole.class.getName());
						roles.add(ServerFollowerRole.class.getName());	
						groupDescriptors.add(new ServerDescriptor());
						node = new A3Node(MainActivity.this, roles, groupDescriptors);
						node.connect("control", true, true);
						node.connect("server", false, true);
						experimentRunning = true;
						break;
					default:
						break;
				}		
					
			}
		};

		inText=(EditText)findViewById(R.id.oneInEditText);
		experiment = (EditText)findViewById(R.id.editText1);
	}

	public void createGroup(View v){
		fromGuiThread.sendEmptyMessage(CREATE_GROUP_USER_COMMAND);
	}
	
	public void stopExperiment(View v){
		fromGuiThread.sendEmptyMessage(STOP_EXPERIMENT_COMMAND);
	}
	
	public void startExperiment(View v){
		fromGuiThread.sendEmptyMessage(START_EXPERIMENT_USER_COMMAND);
	}
	
	public void startSensor(View v){
		if(!experimentRunning)
			fromGuiThread.sendEmptyMessage(START_SENSOR);
	}
	
	public void startActuator(View v){
		if(!experimentRunning)
			fromGuiThread.sendEmptyMessage(START_ACTUATOR);
	}
	
	public void startServer(View v){
		if(!experimentRunning)
			fromGuiThread.sendEmptyMessage(START_SERVER);
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	protected void onDestroy(){
		if(experimentRunning)
			node.disconnect("control", true);
		System.exit(0);
	}
	
	@Override
	public void showOnScreen(String message) {
		toGuiThread.sendMessage(toGuiThread.obtainMessage(0, message));
	}
}