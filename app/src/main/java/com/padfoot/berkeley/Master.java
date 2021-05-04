package com.padfoot.berkeley;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author ACHAL URANKAR -  @padfoot
 * A simple {@link Fragment} subclass.
 * Use the {@link Master#newInstance} factory method to
 * create an instance of this fragment.
 */
public class Master extends Fragment {

    /**
     * topic to subscribe for receiving time from connected slaves
     */
    public static final String MasterSubscriptionTopic = "com/padfoot/berkeley/+/client";

    /**
     * topic to publish average time for subscribed/connected slaves
     */
    public static final String AverageTimeTopic = "com/padfoot/berkeley/avg_time";

    /**
     * TAG for logs
     */
    private static final String TAG = "Master Fragment";

    /**
     * view fields
     */
    public TextView slaveTimeList;
    public TextView syncStatus;
    public TextView avgTime;
    public TextView syncClocks;
    public EditText masterTime;

    /**
     * list to save slave times received
     */
    List<Integer> minutes = new ArrayList<>();

    public Master() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment Master.
     */
    public static Master newInstance() {
        return new Master();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        minutes.clear();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_master, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        slaveTimeList = view.findViewById(R.id.slave_list);
        syncStatus = view.findViewById(R.id.status);
        avgTime = view.findViewById(R.id.avg_time);
        syncClocks = view.findViewById(R.id.sync_clocks);
        masterTime = view.findViewById(R.id.master_time);
        masterTime.setText(new SimpleDateFormat("hh:mm").format(new Date()));
        syncClocks.setOnClickListener(v -> publishAverageTime());
        subscribeForTime();
    }

    private void publishAverageTime() {
        try {
            String time = avgTime.getText().toString();
            String[] timeArr = time.split(":");
            int hr = Integer.parseInt(timeArr[0]);
            int min = Integer.parseInt(timeArr[1]);
            minutes.add(
                    (
                            (hr * 60) + min
                    )
            );
            calculateAvgTime();
            time = avgTime.getText().toString();
            MqttMessage mqttMessage = new MqttMessage(time.getBytes());
            mqttMessage.setQos(2);
            MainActivity.mqttClient.publish(AverageTimeTopic, mqttMessage);
            syncStatus.setText("clock synced with all the nodes");
        } catch (MqttException | NumberFormatException e) {
            if(e instanceof NumberFormatException)
                Toast.makeText(getActivity(), "Enter valid time", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    private void subscribeForTime() {
        MainActivity.mqttClient.setCallback(new MqttCallback() {
            @Override
            public void connectionLost(Throwable cause) {
                Log.e(TAG, "connectionLost");
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) {
                String[] arr = topic.split("/");
                try {
                    String username = arr[3];
                    String msg = message.toString();
                    Log.e(TAG, "messageArrived: " + msg + " from " + username);
                    String[] time = message.toString().split(":");
                    int hr = Integer.parseInt(time[0]);
                    int min = Integer.parseInt(time[1]);
                    minutes.add(
                            (
                                    (hr * 60) + min
                            )
                    );
                    getActivity().runOnUiThread(() -> {
                        slaveTimeList.append(username + " -> " + msg + " \n");
                        calculateAvgTime();
                    });
                } catch (ArrayIndexOutOfBoundsException | NumberFormatException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {

            }
        });
        try {
            MainActivity.mqttClient.subscribe(MasterSubscriptionTopic);
            Log.d(TAG, "subscribeForTime: subscribed to topic ->" + MasterSubscriptionTopic);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void calculateAvgTime() {
        int total = 0;
        for (int min : minutes) {
            total += min;
        }
        total /= minutes.size();
        int avg_hr = total / 60;
        int avg_min = total % 60;
        avgTime.setText(avg_hr + ":" + avg_min);
    }
}