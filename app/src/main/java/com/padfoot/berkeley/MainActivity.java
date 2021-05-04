package com.padfoot.berkeley;

import android.app.Dialog;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentTransaction;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

/**
 * @author ACHAL URANKAR -  @padfoot
 * Main activity which will load fragments according to selection of options
 * */
public class MainActivity extends AppCompatActivity {

    /**
     * user joining mods constants
     */
    public static final int MASTER = 1;
    public static final int SLAVE = 2;

    public static String slaveName = "";
    /**
     * tag for logs
     */
    private static final String TAG = "MainActivity";

    /**
     * Static Mqtt client
     */
    public static MqttClient mqttClient;

    EditText username, room;
    Button master, slave;
    Dialog dialog;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        createMQTTClient();
        openDialogForRegistration();
    }

    private void createMQTTClient() {
        String broker = "tcp://broker.mqtt-dashboard.com";
        try {
            MemoryPersistence persistence = new MemoryPersistence();
            mqttClient = new MqttClient(broker, MqttClient.generateClientId(), persistence);
            MqttConnectOptions mqOptions = new MqttConnectOptions();
            mqOptions.setCleanSession(true);
            mqttClient.connect(mqOptions);
            Log.d(TAG, "createMQTTClient: client connected");
        } catch (MqttException me) {
            me.printStackTrace();
        }
    }

    private void openDialogForRegistration() {
        dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_info);
        dialog.setCanceledOnTouchOutside(false);
        username = dialog.findViewById(R.id.username);
//        room = dialog.findViewById(R.id.room);
        master = dialog.findViewById(R.id.master);
        slave = dialog.findViewById(R.id.slave);

        master.setOnClickListener(v -> {
            connect(MASTER);
        });
        slave.setOnClickListener(v -> {
            connect(SLAVE);
        });
        dialog.show();
    }

    private void connect(int type) {
        if (!mqttClient.isConnected()) {
            Toast.makeText(this, "Not connected, check your internet connection", Toast.LENGTH_SHORT).show();
            return;
        }
        slaveName = username.getText().toString().trim();
        if(slaveName.length() == 0) {
            Toast.makeText(this, "Enter name", Toast.LENGTH_SHORT).show();
            return;
        }
        dialog.hide();
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        switch (type) {
            case MASTER:
                transaction.replace(R.id.frame_layout, Master.newInstance());
                break;
            case SLAVE:
                transaction.replace(R.id.frame_layout, Slave.newInstance());
                break;
        }
        transaction.commit();
    }
}