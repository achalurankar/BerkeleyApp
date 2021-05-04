package com.padfoot.berkeley;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link Slave#newInstance} factory method to
 * create an instance of this fragment.
 */
public class Slave extends Fragment {

    /**
     * View fields
     */
    public EditText time;
    public Button sendBtn;
    public TextView syncedTime;
    public TextView currentSlave;

    /**
     * topic for sending device time data
     * */
    public String topic = "com/padfoot/berkeley/" + MainActivity.slaveName + "/client";

    public Slave() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment Slave.
     */
    public static Slave newInstance() {
        return new Slave();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        time = view.findViewById(R.id.slave_time);
        sendBtn = view.findViewById(R.id.send_time);
        syncedTime = view.findViewById(R.id.avg_time);
        currentSlave = view.findViewById(R.id.current_slave_username);
        currentSlave.setText(MainActivity.slaveName);
        sendBtn.setOnClickListener(v -> {
            publishDeviceTime();
        });
        subscribeToSyncedTime();
    }

    private void publishDeviceTime() {
        MqttMessage message = new MqttMessage(time.getText().toString().getBytes());
        message.setQos(2);
        try {
        MainActivity.mqttClient.publish(topic, message);
        } catch (MqttException e){
            e.printStackTrace();
        }
    }

    private void subscribeToSyncedTime() {
        try {
            MainActivity.mqttClient.subscribe(Master.AverageTimeTopic);
            MainActivity.mqttClient.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {

                }

                @Override
                public void messageArrived(String topic, MqttMessage message) throws Exception {
                    getActivity().runOnUiThread(() -> syncedTime.setText("" + message));
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {

                }
            });
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_slave, container, false);
    }
}