package com.tripplanner.alarm;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.MediaPlayer;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.lifecycle.ViewModelProviders;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.tripplanner.Constants;
import com.tripplanner.MainActivity;
import com.tripplanner.R;
import com.tripplanner.data_layer.local_data.entity.Note;
import com.tripplanner.data_layer.local_data.entity.Trip;
import com.tripplanner.util.NotificationUtil;
import com.tripplanner.util.TripNotification;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class NotificationActivity extends AppCompatActivity {
    private static final int CODE_DRAW_OVER_OTHER_APP_PERMISSION = 2084;
    AlarmViewModel alarmViewModel;
    Trip trip;
    ArrayList<Note> tripNotes;
    Map<String, Object> hm;
    TripNotification tripNotification;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        alarmViewModel = ViewModelProviders.of(this).get(AlarmViewModel.class);
        trip = alarmViewModel.getTrip(getIntent().getIntExtra(Constants.TRIPS,0));
        tripNotes = new ArrayList<>(alarmViewModel.getNotes(getIntent().getIntExtra(Constants.TRIPS,0)));
        tripNotification = new TripNotification(getApplicationContext(), trip);
        displayAlert();
    }

    private void displayAlert() {
        Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
        Ringtone r = RingtoneManager.getRingtone(getApplicationContext(), notification);
        r.play();
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        builder.setMessage("Time of " + trip.getName() + "Trip !").setCancelable(false)
                .setPositiveButton("Start", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        hm = new HashMap<>();
                        hm.put("tripStatus", Constants.DATE);
                        alarmViewModel.updateTrip(trip, hm);
                        setPermation();
                        tripNotification.cancelNotification();
                        r.stop();
                    }
                }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                hm = new HashMap<>();
                hm.put("tripStatus", Constants.STATUS_CANCELED);
                alarmViewModel.updateTrip(trip, hm);
                tripNotification.cancelNotification();
                r.stop();
            }
        }).setNeutralButton("Snoze", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                tripNotification.sendNotification();
                r.stop();
            }
        });

        builder.show();
    }

    private void openGoogleMapDierction(double latatute, double longatute) {
        Intent intent = new Intent(android.content.Intent.ACTION_VIEW,
                Uri.parse("http://maps.google.com/maps?daddr=" + latatute + "," + longatute + ""));
        startActivity(intent);
    }


    public void setPermation() {
        Log.d("ddd", "setPermation: dddd");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {

            //If the draw over permission is not available open the settings screen
            //to grant the permission.
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, CODE_DRAW_OVER_OTHER_APP_PERMISSION);
        } else {
            Log.d("ffff", "setPermation: fffff");
            Intent intent = new Intent(NotificationActivity.this, FloatingViewService.class);
            intent.putParcelableArrayListExtra("notes", tripNotes);
            startService(intent);
            openGoogleMapDierction(trip.getEndPoint().getLat(), trip.getEndPoint().getLng());


        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == CODE_DRAW_OVER_OTHER_APP_PERMISSION) {
            //Check if the permission is granted or not.
            if (resultCode == RESULT_OK) {
                Log.d("result", "onActivityResult: ");
                Intent intent = new Intent(NotificationActivity.this, FloatingViewService.class);
                intent.putParcelableArrayListExtra("notes", tripNotes);
                startService(intent);
                openGoogleMapDierction(trip.getEndPoint().getLat(), trip.getEndPoint().getLng());
            } else { //Permission is not available
                Toast.makeText(this,
                        "Draw over other app permission not available. Closing the application",
                        Toast.LENGTH_SHORT).show();

                finish();
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }


    public static class TripAlarmReciver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
             int  tripId=intent.getIntExtra(Constants.TRIPS,0);
            Intent i = new Intent(context, NotificationActivity.class);
            i.putExtra(Constants.TRIPS,tripId);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(i);
        }
    }


}
