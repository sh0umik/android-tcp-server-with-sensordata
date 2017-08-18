package com.example.aurobindo.sensorLog;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.TextView;

import com.example.aurobindo.sensorLog.orientationProvider.ImprovedOrientationSensor1Provider;
import com.example.aurobindo.sensorLog.orientationProvider.OrientationProvider;
import com.example.aurobindo.sensorLog.representation.Quaternion;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.Locale;

public class SensorActivity extends AppCompatActivity {

    // Server Items

    private boolean toggle = false;

    private TextView tvServerIP, tvServerPort;
    private final int SERVER_PORT = 6789;

    // sensor data

    private OrientationProvider orientationProvider = null;
    private Quaternion quaternion = new Quaternion();

    public String w, x , y, z, data;

    // Socket

    Socket mySocket;
    DataOutputStream responseStream;

    private TextView dispAcc, dispGyro;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // set the provider
        // set which is selected from the dropdown #todo
        orientationProvider = new ImprovedOrientationSensor1Provider((SensorManager) getSystemService(SensorActivity.SENSOR_SERVICE));
        orientationProvider.start();



        // Start the TCP Server

        tvServerIP = (TextView) findViewById(R.id.textViewServerIP);
        tvServerPort = (TextView) findViewById(R.id.textViewServerPort);
        tvServerPort.setText(Integer.toString(SERVER_PORT));
        getDeviceIpAddress();

        dispAcc = (TextView) findViewById(R.id.tv_acc);

        dispAcc.setText(data);

        new Thread(new Runnable() {

            @Override
            public void run() {

                try {
                    ServerSocket socServer = new ServerSocket(SERVER_PORT);
                    Socket socClient = null;
                    while (true) {
                        socClient = socServer.accept();
                        ServerAsyncTask serverAsyncTask = new ServerAsyncTask();
                        serverAsyncTask.execute(new Socket[] { socClient });
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    /**
     * Get ip address of the device
     */
    public void getDeviceIpAddress() {
        try {

            for (Enumeration<NetworkInterface> enumeration = NetworkInterface
                    .getNetworkInterfaces(); enumeration.hasMoreElements();) {
                NetworkInterface networkInterface = enumeration.nextElement();
                for (Enumeration<InetAddress> enumerationIpAddr = networkInterface
                        .getInetAddresses(); enumerationIpAddr
                             .hasMoreElements();) {
                    InetAddress inetAddress = enumerationIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress()
                            && inetAddress.getAddress().length == 4) {
                        tvServerIP.setText(inetAddress.getHostAddress());
                    }
                    Log.i("IP : ", inetAddress.getHostAddress());
                }
            }
        } catch (SocketException e) {
            Log.e("ERROR:", e.toString());
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    public void trackStart (View view) {
        toggle = true;
        // Sensor start to listen
    }

    public void trackStop (View view) {
        toggle = false;
        orientationProvider.stop();
    }

    /**
     * AsyncTask which handles the commiunication with clients
     */
    class ServerAsyncTask extends AsyncTask<Socket, Void, String> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }


        @Override
        protected String doInBackground(Socket... params) {

            mySocket = params[0];
            try {
                responseStream = new DataOutputStream(mySocket.getOutputStream());
            } catch (IOException e) {
                e.printStackTrace();
            }

            while(true) {

                if (toggle == false)
                    break;

                try {
                    orientationProvider.getQuaternion(quaternion);
                    w = String.valueOf(quaternion.getW());
                    x = String.valueOf(quaternion.getX());
                    y = String.valueOf(quaternion.getY());
                    z = String.valueOf(quaternion.getZ());
                    data = w + "|" + x + "|" + y + "|" + z + "\n";

                    responseStream.writeBytes(data);
                    responseStream.flush();


                    //mySocket.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            return null;
        }

        @Override
        protected void onPostExecute(String s) {

        }
    }
}
