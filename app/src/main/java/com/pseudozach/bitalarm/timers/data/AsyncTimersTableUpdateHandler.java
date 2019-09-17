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

package com.pseudozach.bitalarm.timers.data;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.pseudozach.bitalarm.data.AsyncDatabaseTableUpdateHandler;
import com.pseudozach.bitalarm.timers.Timer;
import com.pseudozach.bitalarm.list.ScrollHandler;
import com.pseudozach.bitalarm.timers.TimerNotificationService;
import com.pseudozach.bitalarm.ringtone.TimesUpActivity;
import com.pseudozach.bitalarm.util.ParcelableUtil;

/**
 * Created by Phillip Hsu on 8/2/2016.
 */
public final class AsyncTimersTableUpdateHandler extends AsyncDatabaseTableUpdateHandler<Timer, TimersTableManager> {
    private static final String TAG = "TimersTableUpdater"; // TAG max 23 chars

    public AsyncTimersTableUpdateHandler(Context context, ScrollHandler scrollHandler) {
        super(context, scrollHandler);
    }

    @Override
    protected TimersTableManager onCreateTableManager(Context context) {
        return new TimersTableManager(context);
    }

    @Override
    protected void onPostAsyncDelete(Integer result, Timer timer) {
        cancelAlarm(timer, true);
    }

    @Override
    protected void onPostAsyncInsert(Long result, Timer timer) {
        if (timer.isRunning()) {
            scheduleAlarm(timer);
        }
    }

    @Override
    protected void onPostAsyncUpdate(Long result, Timer timer) {
        Log.d(TAG, "onPostAsyncUpdate, timer = " + timer);
        if (timer.isRunning()) {
            // We don't need to cancel the previous alarm, because this one
            // will remove and replace it.
            scheduleAlarm(timer);
        } else {
            boolean removeNotification = !timer.hasStarted();
            cancelAlarm(timer, removeNotification);
            if (!removeNotification) {
                // Post a new notification to reflect the paused state of the timer
                TimerNotificationService.showNotification(getContext(), timer);
            }
        }
    }

    // TODO: Consider changing to just a long id param
    private PendingIntent createTimesUpIntent(Timer timer) {
        Intent intent = new Intent(getContext(), TimesUpActivity.class);
        intent.putExtra(TimesUpActivity.EXTRA_RINGING_OBJECT, ParcelableUtil.marshall(timer));
        // There's no point to determining whether to retrieve a previous instance, because
        // we chose to ignore it since we had issues with NPEs. TODO: Perhaps these issues
        // were caused by you using the same reference variable for every Intent/PI that
        // needed to be recreated, and you reassigning the reference each time you were done with
        // one of them, which leaves the one before unreferenced and hence eligible for GC.
        return PendingIntent.getActivity(getContext(), timer.getIntId(), intent, PendingIntent.FLAG_CANCEL_CURRENT);
    }

    private void scheduleAlarm(Timer timer) {
        AlarmManager am = (AlarmManager) getContext().getSystemService(Context.ALARM_SERVICE);
        am.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP, timer.endTime(), createTimesUpIntent(timer));
        TimerNotificationService.showNotification(getContext(), timer);
    }

    private void cancelAlarm(Timer timer, boolean removeNotification) {
        // Cancel the alarm scheduled. If one was never scheduled, does nothing.
        AlarmManager am = (AlarmManager) getContext().getSystemService(Context.ALARM_SERVICE);
        PendingIntent pi = createTimesUpIntent(timer);
        // Now can't be null
        am.cancel(pi);
        pi.cancel();
        if (removeNotification) {
            TimerNotificationService.cancelNotification(getContext(), timer.getId());
        }
        // Won't do anything if not actually started
        // This was actually a problem for successive Timers. We actually don't need to
        // manually stop the service in many cases. See usages of TimerController.stop().
//        getContext().stopService(new Intent(getContext(), TimerRingtoneService.class));
        // TODO: Do we need to finish TimesUpActivity?
    }
}
