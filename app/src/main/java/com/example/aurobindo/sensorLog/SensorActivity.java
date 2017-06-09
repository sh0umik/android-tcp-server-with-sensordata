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

public class SensorActivity extends AppCompatActivity implements SensorEventListener {

    private boolean toggle = false;
    private SensorManager sensormanager = null;
    private File externalStorageDirectory;
    private FileWriter filewriterAcc, filewriterGyro;
    private TextView dispAcc, dispGyro;
    private Button track, stop;

    // Server Items

    private TextView tvClientMsg, tvServerIP, tvServerPort;
    private final int SERVER_PORT = 6789;
    private String Server_Name = "testapp";

    SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());

    // Sensor Data

    private float x, y, z;
    private String acc, gyro;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sensormanager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        externalStorageDirectory = Environment.getExternalStorageDirectory();

        dispAcc = (TextView) findViewById(R.id.tv_acc);
        dispGyro = (TextView) findViewById(R.id.tv_gyro);
        track = (Button) findViewById(R.id.button_Track);
        stop = (Button) findViewById(R.id.button_Stop);
        try {
            filewriterAcc = new FileWriter(new File(externalStorageDirectory, "accelerometer_"+dateFormat.format(new Date())+".tsv"), true);
            filewriterGyro = new FileWriter(new File(externalStorageDirectory, "gyroscope_"+dateFormat.format(new Date())+".tsv"), true);
        } catch (IOException e) {}

        // Start the TCP Server

        tvClientMsg = (TextView) findViewById(R.id.textViewClientMessage);
        tvServerIP = (TextView) findViewById(R.id.textViewServerIP);
        tvServerPort = (TextView) findViewById(R.id.textViewServerPort);
        tvServerPort.setText(Integer.toString(SERVER_PORT));
        getDeviceIpAddress();

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
    public void onAccuracyChanged(Sensor sensor, int accuracy)
    {

    }

    @Override
    public void onSensorChanged(SensorEvent event) {

        /**************************** Accelerometer ***************************/
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            x = event.values[0];
            y = event.values[1];
            z = event.values[2];
            if(toggle)  {
                acc = "x=" + x + "\ty=" + y + "\tz=" + z;
                dispAcc.setText(acc);

//                try {
//                    filewriterAcc.write("\n" + dateFormat.format(new Date()) + "\t" + x + "\t" + y + "\t" + z);
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
            }
        }

        /***************************** Gyroscope ******************************/
       if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
            x = event.values[0];
            y = event.values[1];
            z = event.values[2];
            if(toggle)  {
                gyro = "x=" + x + "\ty=" + y + "\tz=" + z;

                dispAcc.setText(gyro);

//                try {
//                    filewriterGyro.write("\n" + dateFormat.format(new Date()) + "\t" + x + "\t" + y + "\t" + z);
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
            }
        }
    }


    public void trackStart (View view) {
        toggle = true;
        // Sensor start to listen
        sensormanager.registerListener(this,
                sensormanager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                SensorManager.SENSOR_DELAY_FASTEST);
        sensormanager.registerListener(this,
                sensormanager.getDefaultSensor(Sensor.TYPE_GYROSCOPE),
                SensorManager.SENSOR_DELAY_FASTEST);
    }

    public void trackStop (View view) {
        toggle = false;
        sensormanager.unregisterListener(this);
        try {
            filewriterAcc.close();
            filewriterGyro.close();
        } catch (IOException e) {}
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

    /**
     * AsyncTask which handles the commiunication with clients
     */
    class ServerAsyncTask extends AsyncTask<Socket, Void, String> {
        @Override
        protected String doInBackground(Socket... params) {
            String result = null;
            Socket mySocket = params[0];
            while(true) {
                try {

                    DataOutputStream responseStream = new DataOutputStream(mySocket.getOutputStream());
                    responseStream.writeBytes("Accelorometer : "+ acc + "\n\n" + "Gyroscope : " + gyro + "\n");
                    responseStream.flush();

                    //mySocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            //return result;
        }

        @Override
        protected void onPostExecute(String s) {

        }
    }
}
