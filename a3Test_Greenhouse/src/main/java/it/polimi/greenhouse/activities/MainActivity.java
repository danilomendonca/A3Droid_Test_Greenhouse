package it.polimi.greenhouse.activities;

import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.EditText;

import java.util.ArrayList;

import it.polimi.deepse.a3droid.A3DroidActivity;
import it.polimi.deepse.a3droid.a3.A3Application;
import it.polimi.deepse.a3droid.a3.A3GroupDescriptor;
import it.polimi.deepse.a3droid.a3.A3Message;
import it.polimi.deepse.a3droid.a3.A3Node;
import it.polimi.deepse.a3droid.a3.exceptions.A3ChannelNotFoundException;
import it.polimi.deepse.a3droid.a3.exceptions.A3InvalidOperationParameters;
import it.polimi.deepse.a3droid.a3.exceptions.A3InvalidOperationRole;
import it.polimi.deepse.a3droid.a3.exceptions.A3NoGroupDescriptionException;
import it.polimi.greenhouse.a3.groups.ActuatorsDescriptor;
import it.polimi.greenhouse.a3.groups.ControlDescriptor;
import it.polimi.greenhouse.a3.groups.MonitoringDescriptor;
import it.polimi.greenhouse.a3.groups.ServerDescriptor;
import it.polimi.greenhouse.a3.groups.TestControlDescriptor;
import it.polimi.greenhouse.a3.nodes.TestControlNode;
import it.polimi.greenhouse.a3.roles.ActuatorFollowerRole;
import it.polimi.greenhouse.a3.roles.ActuatorSupervisorRole;
import it.polimi.greenhouse.a3.roles.ControlFollowerRole;
import it.polimi.greenhouse.a3.roles.ControlSupervisorRole;
import it.polimi.greenhouse.a3.roles.SensorFollowerRole;
import it.polimi.greenhouse.a3.roles.SensorSupervisorRole;
import it.polimi.greenhouse.a3.roles.ServerFollowerRole;
import it.polimi.greenhouse.a3.roles.ServerSupervisorRole;
import it.polimi.greenhouse.a3.roles.TestControlFollowerRole;
import it.polimi.greenhouse.a3.roles.TestControlSupervisorRole;
import it.polimi.greenhouse.util.AppConstants;
import it.polimit.greenhouse.R;

public class MainActivity extends A3DroidActivity {

    private A3Node node;
    private EditText inText, sensorsFrequency, actuatorsFrequency, sensorsPayload, actuatorsPayload;
    private HandlerThread fromGUIThread;
    private Handler toGuiHandler;
    private Handler fromGuiHandler;
    private EditText experiment;
    public static int runningExperiment;
    protected TestControlNode testNode;
    public static final String TAG = "MainActivity";
    private boolean experimentRunning = false;
    private A3Application application = null;

    public static void setRunningExperiment(int runningExperiment) {
        MainActivity.runningExperiment = runningExperiment;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        application = ((A3Application) getApplication());
        application.checkin();

        toGuiHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case 0:
                        inText.append(msg.obj + "\n");
                        break;
                    case 1:
                        boolean groupReady = (Boolean) msg.obj;
                        if(groupReady) {
                            try {
                                testNode.disconnect("test_control");
                            } catch (A3ChannelNotFoundException e) {
                                e.printStackTrace();
                            }
                            Log.i(TAG, "Test group ready, starting test");
                        }
                        break;
                    default:
                        break;
                }

            }
        };

        fromGUIThread = new HandlerThread("GreenhouseGUIHandler");
        fromGUIThread.start();
        fromGuiHandler = new Handler(fromGUIThread.getLooper()) {

            it.polimi.deepse.a3droid.a3.A3Node nodeV2;

            @Override
            public void handleMessage(Message msg) {

                ArrayList<String> roles = new ArrayList<String>();
                roles.add(ControlSupervisorRole.class.getName());
                roles.add(ControlFollowerRole.class.getName());
                ArrayList<A3GroupDescriptor> groupDescriptors = new ArrayList<A3GroupDescriptor>();
                groupDescriptors.add(new ControlDescriptor());

                switch (msg.what) {
                    case AppConstants.STOP_EXPERIMENT_COMMAND:
                        if (experimentRunning) {
                            node.sendToSupervisor(new A3Message(AppConstants.LONG_RTT, ""), "control");
                            experimentRunning = false;
                        }
                        break;
                    case AppConstants.START_EXPERIMENT_USER_COMMAND:

                        if (!experimentRunning) {
                            experimentRunning = true;
                            if (node.isConnectedForApplication("server_0"))
                                node.sendToSupervisor(new A3Message(AppConstants.SET_PARAMS_COMMAND, "A_" + actuatorsFrequency.getText().toString() + "_" + actuatorsPayload.getText().toString()),
                                        "control");
                            if (node.isConnectedForApplication("monitoring_" + experiment.getText().toString()))
                                node.sendToSupervisor(new A3Message(AppConstants.SET_PARAMS_COMMAND, "S_" + sensorsFrequency.getText().toString() + "_" + sensorsPayload.getText().toString()),
                                        "control");
                            node.sendToSupervisor(new A3Message(AppConstants.START_EXPERIMENT_USER_COMMAND, ""), "control");
                        }
                        break;
                    case AppConstants.START_SENSOR:
                        if (experimentRunning)
                            break;
                        roles.add(SensorSupervisorRole.class.getName());//removed to easy tests
                        roles.add(SensorFollowerRole.class.getName());
                        roles.add(ServerFollowerRole.class.getName());
                        groupDescriptors.add(new MonitoringDescriptor() {
                            @Override
                            public int getSupervisorFitnessFunction() {
                                return 0;
                            }
                        });
                        groupDescriptors.add(new ServerDescriptor());

                        nodeV2 = ((A3Application) getApplication()).createNode(
                                groupDescriptors,
                                roles);
                        try {
                            //nodeV2.connect("control");
                            nodeV2.connect("monitoring_" + experiment.getText().toString());
                        } catch (A3NoGroupDescriptionException e) {
                            e.printStackTrace();
                        }

                        //node = new A3Node(getUUID(), MainActivity.this, roles, groupDescriptors);
                        //node.connect("control", true, true);
                        //node.connect("monitoring_" + experiment.getText().toString(), true, true);
                        break;
                    case AppConstants.START_ACTUATOR:
                        if (experimentRunning)
                            break;
                        roles.add(ActuatorSupervisorRole.class.getName());
                        roles.add(ActuatorFollowerRole.class.getName());
                        roles.add(ServerFollowerRole.class.getName());
                        groupDescriptors.add(new ActuatorsDescriptor());
                        groupDescriptors.add(new ServerDescriptor());
                        //node = new A3Node(getUUID(), MainActivity.this, roles, groupDescriptors);
                        //nodeV2 = ((A3Application) getApplication()).createNode(
                        //groupDescriptors,
                        //roles);
                        try {
                            //nodeV2.stack("control", "monitoring_" + experiment.getText().toString());
                            nodeV2.merge("control", "monitoring_" + experiment.getText().toString());
                        } catch (A3NoGroupDescriptionException a3NoGroupDescriptionException) {
                            a3NoGroupDescriptionException.printStackTrace();
                        } catch (A3InvalidOperationParameters a3InvalidOperationParameters) {
                            a3InvalidOperationParameters.printStackTrace();
                        } catch (A3InvalidOperationRole a3InvalidOperationRole) {
                            a3InvalidOperationRole.printStackTrace();
                        } catch (A3ChannelNotFoundException e) {
                            e.printStackTrace();
                        }
                        //node.connect("control", true, true);
                        //node.connect("actuators_" + experiment.getText().toString(), true, true);
                        break;
                    case AppConstants.START_SERVER:
                        if (experimentRunning)
                            break;
                        roles.add(ServerSupervisorRole.class.getName());
                        roles.add(ServerFollowerRole.class.getName());
                        roles.add(SensorSupervisorRole.class.getName());//added to easy tests
                        roles.add(SensorFollowerRole.class.getName());//added to easy tests
                        groupDescriptors.add(new ServerDescriptor());
                        groupDescriptors.add(new MonitoringDescriptor() {

                            @Override
                            public int getSupervisorFitnessFunction() {
                                return 0;
                            }

                        });
                        //node = new A3Node(getUUID(), MainActivity.this, roles, groupDescriptors);
                        nodeV2 = ((A3Application) getApplication()).createNode(
                                groupDescriptors,
                                roles);
                        nodeV2 = ((A3Application) getApplication()).createNode(
                                groupDescriptors,
                                roles);
                        try {
                            nodeV2.connect("control");
                            //nodeV2.connect("monitoring_" + experiment.getText().toString());
                        } catch (A3NoGroupDescriptionException e) {
                            e.printStackTrace();
                        }
                        //node.connect("control", true, true);
                        ////node.connect("server_0", true, true);
                        //node.connect("monitoring_" + experiment.getText().toString(), true, true);
                        break;
                    default:
                        break;
                }

            }
        };


        inText = (EditText) findViewById(R.id.oneInEditText);
        experiment = (EditText) findViewById(R.id.editText1);
        sensorsFrequency = (EditText) findViewById(R.id.editText2);
        sensorsPayload = (EditText) findViewById(R.id.editText4);
        actuatorsFrequency = (EditText) findViewById(R.id.editText3);
        actuatorsPayload = (EditText) findViewById(R.id.editText5);

        Log.i(TAG, Build.MANUFACTURER);
        Log.i(TAG, Build.PRODUCT);
        Log.i(TAG, Build.MODEL);
    }

    public void createTestControlGroup(int size, boolean server){
        ArrayList<String> roles = new ArrayList<String>();
        roles.add(TestControlSupervisorRole.class.getName());
        roles.add(TestControlFollowerRole.class.getName());
        ArrayList<A3GroupDescriptor> groupDescriptors = new ArrayList<A3GroupDescriptor>();
        groupDescriptors.add(new TestControlDescriptor());
        testNode = new TestControlNode((A3Application) getApplication(), server, size, toGuiHandler, getUUID(), roles, groupDescriptors);
        try {
            testNode.connect(TestControlDescriptor.TEST_GROUP_NAME);
        } catch (A3NoGroupDescriptionException e) {
            e.printStackTrace();
        }
    }

    public void createGroup(View v) {
        fromGuiHandler.sendEmptyMessage(AppConstants.CREATE_GROUP_USER_COMMAND);
    }

    public void stopExperiment(View v) {
        fromGuiHandler.sendEmptyMessage(AppConstants.STOP_EXPERIMENT_COMMAND);
    }

    public void startExperiment(View v) {
        fromGuiHandler.sendEmptyMessage(AppConstants.START_EXPERIMENT_USER_COMMAND);
    }

    public void startSensor(View v) {
        if (!experimentRunning)
            fromGuiHandler.sendEmptyMessage(AppConstants.START_SENSOR);
    }

    public void startActuator(View v) {
        if (!experimentRunning)
            fromGuiHandler.sendEmptyMessage(AppConstants.START_ACTUATOR);
    }

    public void startServer(View v) {
        if (!experimentRunning)
            fromGuiHandler.sendEmptyMessage(AppConstants.START_SERVER);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        fromGUIThread.quit();
        if (experimentRunning)
            try {
                node.disconnect("control");
            } catch (A3ChannelNotFoundException e) {
                e.printStackTrace();
            }
        application.quit();
        System.exit(0);
    }


    public boolean isTestGroupReady() {
        return testNode.isReady();
    }
}
