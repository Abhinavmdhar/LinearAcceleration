package com.example.abhi.linearacceleration;

import android.Manifest;
import android.app.NotificationManager;

import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import android.net.Uri;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.app.NotificationCompat;
import android.util.Log;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;
import android.view.ViewGroup;

import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import java.util.ArrayList;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.widget.Toast;


public class MainActivity extends AppCompatActivity implements SensorEventListener {


    private float[] gravity = new float[3];
    private float[] linear_acceleration = new float[3];

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;

    String TAG = "TEST";

    TextView t;
    ListView l;

    int i =0;
    // number of times phones has moved
    double threshold = 9.81*3;
    // acceleration value considered high enough to be an accident

    ListAdapter listAdapter;
    ArrayList<String> arrayList;
    ArrayAdapter<String> adapter;

    boolean dialogTriggered =false;
    boolean permission = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        SensorManager mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        Sensor mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);

        t = (TextView) findViewById(R.id.textView);
        l = (ListView) findViewById(R.id.listView);
        arrayList = new ArrayList<String>();

        //Adapter: You need three parameters 'the context, id of the layout (it will be where the data is shown),
        // and the array that contains the data
        adapter = new ArrayAdapter<String>(getApplicationContext(), android.R.layout.simple_list_item_1, arrayList) {

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                // Get the Item from ListView
                View view = super.getView(position, convertView, parent);

                // Initialize a TextView for ListView each Item
                TextView tv = (TextView) view.findViewById(android.R.id.text1);

                // Set the text color of TextView (ListView Item)
                tv.setTextColor(Color.BLACK);

                // Generate ListView Item using TextView
                return view;
            }
        };
        l.setAdapter(adapter);

    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        // alpha is calculated as t / (t + dT)
        // with t, the low-pass filter's time-constant
        // and dT, the event delivery rate

        //permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE)
                != PackageManager.PERMISSION_GRANTED) {
            Log.i(TAG,"Access denied");
            PermissionUtils.requestPermission(this, LOCATION_PERMISSION_REQUEST_CODE,
                    Manifest.permission.CALL_PHONE, true);
        }else{
            permission = true;
        }


        final float alpha = 0.8f;

        gravity[0] = alpha * gravity[0] + (1 - alpha) * event.values[0];
        gravity[1] = alpha * gravity[1] + (1 - alpha) * event.values[1];
        gravity[2] = alpha * gravity[2] + (1 - alpha) * event.values[2];

        linear_acceleration[0] = event.values[0] - gravity[0];
        linear_acceleration[1] = event.values[1] - gravity[1];
        linear_acceleration[2] = event.values[2] - gravity[2];


        if(Math.abs(linear_acceleration[0])>threshold){
            arrayList.add("Acceleration x: " + linear_acceleration[0] + "m/s^2");
            adapter.notifyDataSetChanged();
            i++;
        }
        if(Math.abs(linear_acceleration[1])>threshold){
            arrayList.add("Acceleration y: " + linear_acceleration[1] + "m/s^2");
            adapter.notifyDataSetChanged();
            i++;
        }
        if(Math.abs(linear_acceleration[2])>threshold){
            arrayList.add("Acceleration z: " + linear_acceleration[2] + "m/s^2");
            adapter.notifyDataSetChanged();
            i++;
        }
        if(Math.abs(linear_acceleration[2])>threshold||Math.abs(linear_acceleration[1])>threshold
                ||Math.abs(linear_acceleration[0])>threshold){

            //creating notification
            NotificationManager notificationManager= (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            NotificationCompat.Builder mBuilder = (android.support.v7.app.NotificationCompat.Builder)
                    // casting v4 to v7 since the programming is excepting v7 object
                    new NotificationCompat.Builder(this)
                            .setSmallIcon(R.drawable.notification_icon)
                            .setContentTitle("LinearAcceleration")
                            .setContentText("Acceleration detected");

            // The following block of code redirects the users to the MainActivity once the notifications has been pressed
            // Creates an explicit intent for an Activity in your app
            Intent resultIntent = new Intent(this, MainActivity.class);
            // The stack builder object will contain an artificial back stack for the
            // started Activity.
            // This ensures that navigating backward from the Activity leads out of
            // your application to the Home screen.
            TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
            // Adds the back stack for the Intent (but not the Intent itself)
            stackBuilder.addParentStack(MainActivity.class);
            // Adds the Intent that starts the Activity to the top of the stack
            stackBuilder.addNextIntent(resultIntent);
            PendingIntent resultPendingIntent =
                    stackBuilder.getPendingIntent(
                            0,
                            PendingIntent.FLAG_UPDATE_CURRENT
                    );
            mBuilder.setContentIntent(resultPendingIntent);
            NotificationManager mNotificationManager =
                    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

            //displaying notification
            notificationManager.notify(1, mBuilder.build());

            if(!dialogTriggered) {

                dialogTriggered = true;
                //Creating dialog
                AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
                alertDialogBuilder.setMessage("Are You Okay?");

                //Yes Button + Action when pressed
                alertDialogBuilder.setPositiveButton("yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {
                        dialogTriggered = false;
                        Toast.makeText(MainActivity.this, "You clicked the Yes button", Toast.LENGTH_LONG).show();

                    }
                });

                //No Button + Action when pressed
                alertDialogBuilder.setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialogTriggered = false;
                        Intent callIntent = new Intent(Intent.ACTION_CALL);
                        callIntent.setData(Uri.parse("tel:4169847862"));
                        if(permission){
                            startActivity(callIntent);
                        }else{
                            Log.i(TAG,"Permission denied");
                        }

                    }
                });

                AlertDialog alertDialog = alertDialogBuilder.create();
                alertDialog.show();
            }

        }

        t.setText("You have moved the phone " + i + " times");

    }

    public void onAccuracyChanged(Sensor s, int i){

    }

}


