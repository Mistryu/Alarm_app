package com.learning.Clock_app;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.learning.Clock_app.Helpers.AlarmModel;
import com.learning.Clock_app.Helpers.AlarmScheduler;
import com.learning.Clock_app.Helpers.DatabaseHelper;
import com.learning.Clock_app.Adapters.FragmentAdapter;
import com.learning.Clock_app.Fragments.FragmentAlarms;
import com.learning.Clock_app.Helpers.NotificationReceiver;

public class MainActivity extends AppCompatActivity {

    public static final String TAG = "Main";
    private ViewPager2 viewPager2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.d(TAG, "onCreate: Started");
        createNotificationChannel(); // this needs to run only once

        BottomNavigationView bottomNav = findViewById(R.id.upper_navigation);
        viewPager2 = findViewById(R.id.main_vp2);
        FragmentManager fm = getSupportFragmentManager();

        FragmentAdapter fragmentAdapter = new FragmentAdapter(fm, getLifecycle());
        viewPager2.setAdapter(fragmentAdapter);

        bottomNav.setOnItemSelectedListener(item -> {

            int itemId = item.getItemId();

            if (itemId == R.id.alarms)
                viewPager2.setCurrentItem(0);

            else if (itemId == R.id.stopper)
                viewPager2.setCurrentItem(1);

            else if (itemId == R.id.timer)
                viewPager2.setCurrentItem(2);

            return true;
        });
    }

    @SuppressLint("UnspecifiedImmutableFlag")
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        FragmentAlarms fragmentAlarms = getFragmentAlarms();

        if (intent.hasExtra("hour")) {
            int hour = intent.getIntExtra("hour", 12);
            int minute = intent.getIntExtra("minute", 0);
            String label = intent.getStringExtra("label");
            String days = intent.getStringExtra("days");

            AlarmModel alarmModel;
            DatabaseHelper databaseHelper = new DatabaseHelper(this);

            try {
                alarmModel = new AlarmModel(-1, hour, minute, label, days, true);
                int id = databaseHelper.addOne(alarmModel); //I get the id and set it here
                alarmModel.setId(id);

                AlarmScheduler alarmScheduler = new AlarmScheduler(id, hour, minute, days, this);
                alarmScheduler.scheduleAlarm();

            } catch (Exception e) {
                Toast.makeText(this, "ERROR", Toast.LENGTH_SHORT).show();
                alarmModel = null;
            }

            if (alarmModel != null && fragmentAlarms != null)
                fragmentAlarms.addAlarmToList(alarmModel);
        }


        if (intent.hasExtra("delete") && fragmentAlarms != null) {
            int id = intent.getIntExtra("delete", -1);

            fragmentAlarms.deleteFromAlarmList(id);
            AlarmManager alarmManager = (AlarmManager) this.getSystemService(Context.ALARM_SERVICE);
            alarmManager.cancel(PendingIntent.getBroadcast(this, id, new Intent(this, NotificationReceiver.class), PendingIntent.FLAG_UPDATE_CURRENT));
        }
    }

    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        CharSequence name = getString(R.string.channel_name);
        String description = getString(R.string.channel_description);
        int importance = NotificationManager.IMPORTANCE_HIGH;

        NotificationChannel channel = new NotificationChannel("Alarm", name, importance);
        channel.setVibrationPattern(new long[]{0, 100, 1000, 300, 200, 100, 500, 200, 100});
        channel.setDescription(description);

//        Uri alarmSound = Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE+ "://" +getPackageName()+"/"+R.raw.alarm_sound);
//
//        AudioAttributes audioAttributes = new AudioAttributes.Builder()
//                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
//                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
//                .build();
//        channel.setSound(alarmSound, audioAttributes);

        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        notificationManager.createNotificationChannel(channel);
    }

    private FragmentAlarms getFragmentAlarms() {
        FragmentAdapter adapter = (FragmentAdapter) viewPager2.getAdapter();
        if (adapter != null)
            return (FragmentAlarms) adapter.getFragment(0);
        else
            return null;
    }
}
