/*
 * Copyright 2017 Phillip Hsu
 *
 * This file is part of ClockPlus.
 *
 * ClockPlus is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ClockPlus is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with ClockPlus.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.pseudozach.bitalarm.ringtone.playback;

import android.app.Notification;
import android.content.Intent;
import android.net.Uri;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.provider.Settings;
import androidx.core.app.NotificationCompat;

//import com.philliphsu.clock2.R;
import com.pseudozach.bitalarm.R;
import com.pseudozach.bitalarm.timers.Timer;
import com.pseudozach.bitalarm.timers.TimerController;
import com.pseudozach.bitalarm.timers.TimerNotificationService;
import com.pseudozach.bitalarm.timers.data.AsyncTimersTableUpdateHandler;

public class TimerRingtoneService extends RingtoneService<Timer> {
    private static final String TAG = "TimerRingtoneService";

    // private because they refer to our foreground notification's actions.
    // we reuse these from TimerNotificationService because they're just constants, the values
    // don't actually matter.
    private static final String ACTION_ADD_ONE_MINUTE = TimerNotificationService.ACTION_ADD_ONE_MINUTE;
    private static final String ACTION_STOP = TimerNotificationService.ACTION_STOP;
    /**
     * The dummy action required for PendingIntents.
     */
    private static final String ACTION_CONTENT_INTENT = "launch_times_up_activity";

    private TimerController mController;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // This has to be first so our Timer is initialized
        int value = super.onStartCommand(intent, flags, startId);
        if (mController == null) {
            mController = new TimerController(getRingingObject(),
                    new AsyncTimersTableUpdateHandler(this, null));
        }
        if (intent.getAction() != null) {
            switch (intent.getAction()) {
                case ACTION_ADD_ONE_MINUTE:
                    mController.addOneMinute();
                    break;
                case ACTION_STOP:
                    mController.stop();
                    break;
                default:
                    throw new UnsupportedOperationException();
            }
            stopSelf(startId);
            finishActivity();
        }
        return value;
    }

    @Override
    protected void onAutoSilenced() {
        mController.stop();
    }

    @Override
    protected Uri getRingtoneUri() {
        String ringtone = PreferenceManager.getDefaultSharedPreferences(this).getString(
                getString(R.string.key_timer_ringtone), Settings.System.ALARM_ALERT);
        return Uri.parse(ringtone);
    }

    @Override
    protected Notification getForegroundNotification() {
        String title = getRingingObject().label();
        if (title.isEmpty()) {
            title = getString(R.string.timer);
        }
        // Not implemented for simplicity and the feature isn't very useful.. future release??
        // TODO: We need to pass in the ringing object as an extra.
//        Intent intent = new Intent(this, TimesUpActivity.class);
        // Since there can only be one Timer at a time for this Activity/Service pair,
        // we don't need to specify a request code.
//        PendingIntent pi = PendingIntent.getActivity(this, 0, intent, 0);
        return new NotificationCompat.Builder(this)
                .setContentTitle(title)
                .setContentText(getString(R.string.times_up))
                .setSmallIcon(R.drawable.ic_timer_24dp)
                .setShowWhen(false) // TODO: Should we show this?
//                .setOngoing(true) // foreground notes are ongoing by default
                .addAction(R.drawable.ic_add_24dp,
                        getString(R.string.add_one_minute),
                        getPendingIntent(ACTION_ADD_ONE_MINUTE, getRingingObject().getIntId()))
                .addAction(R.drawable.ic_stop_24dp,
                        getString(R.string.stop),
                        getPendingIntent(ACTION_STOP, getRingingObject().getIntId()))
//                .setContentIntent(pi)
                .build();
    }

    @Override
    protected boolean doesVibrate() {
        return PreferenceManager.getDefaultSharedPreferences(this).getBoolean(
                getString(R.string.key_timer_vibrate), false);
    }

    @Override
    protected int minutesToAutoSilence() {
        String value = PreferenceManager.getDefaultSharedPreferences(this).getString(
                getString(R.string.key_timer_silence_after), "15");
        return Integer.parseInt(value);
    }

    @Override
    protected Parcelable.Creator<Timer> getParcelableCreator() {
        return Timer.CREATOR;
    }
}
