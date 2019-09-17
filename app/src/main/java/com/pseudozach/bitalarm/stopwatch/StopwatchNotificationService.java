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

package com.pseudozach.bitalarm.stopwatch;

import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.util.Log;

import com.pseudozach.bitalarm.R;
import com.pseudozach.bitalarm.chronometer.ChronometerNotificationService;
//import com.philliphsu.clock2.R;
import com.pseudozach.bitalarm.chronometer.ChronometerDelegate;
import com.pseudozach.bitalarm.stopwatch.data.AsyncLapsTableUpdateHandler;
import com.pseudozach.bitalarm.stopwatch.ui.StopwatchFragment;

public class StopwatchNotificationService extends ChronometerNotificationService {
    private static final String TAG = "StopwatchNotifService";

    public static final String ACTION_ADD_LAP = "com.pseudozach.bitalarm.stopwatch.action.ADD_LAP";
    public static final String ACTION_UPDATE_LAP_TITLE = "com.pseudozach.bitalarm.stopwatch.action.UPDATE_LAP_TITLE";

    public static final String EXTRA_LAP_NUMBER = "com.pseudozach.bitalarm.stopwatch.extra.LAP_NUMBER";

    private AsyncLapsTableUpdateHandler mUpdateHandler;
    private SharedPreferences mPrefs;
    private final ChronometerDelegate mDelegate = new ChronometerDelegate();
    private Lap mCurrentLap;

    @Override
    public void onCreate() {
        super.onCreate();
        setContentTitle(getNoteId(), getString(R.string.stopwatch));
        mUpdateHandler = new AsyncLapsTableUpdateHandler(this, null);
        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        mDelegate.init();
        mDelegate.setShowCentiseconds(true, false);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // The base implementation returns START_STICKY, so this null intent
        // signifies that the service is being recreated after its process
        // had ended previously.
        if (intent == null) {
            Log.d(TAG, "Recreated service, starting chronometer again.");
            // Restore the current lap
            new Thread(new Runnable() {
                @Override
                public void run() {
                    // If the service is being restarted, there is AT LEAST one lap in the table.
                    // This is the first lap that was inserted when the service was first started.
                    // The Cursor has already been moved to its first row.
                    mCurrentLap = mUpdateHandler.getTableManager().queryCurrentLap().getItem();
                    Log.d(TAG, "Restored current lap " + mCurrentLap);
                }
            }).start();
            // Start the ticking again from where we left off.
            // The default actions will be set on the Builder.
            //
            // For simplicity, even if there were laps
            // in this stopwatch before the process was destroyed, we won't be restoring
            // the lap number in the title. If the user is leaving this service in the background
            // long enough that the system can kill its process, they probably aren't
            // recording laps. As such, a solution for this is a waste of time.
            boolean running = mPrefs.getBoolean(StopwatchFragment.KEY_CHRONOMETER_RUNNING, false);
            syncNotificationWithStopwatchState(running);
        }
        // If this service is being recreated and the above if-block called through,
        // then the call to super won't run any commands, because it will see
        // that the intent is null.
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    protected int getSmallIcon() {
        return R.drawable.ic_stopwatch_24dp;
    }

    @Nullable
    @Override
    protected PendingIntent getContentIntent() {

//        return ContentIntentUtils.create(this, MainActivity.PAGE_STOPWATCH, -1);
        return null;
    }

    @Override
    protected boolean isCountDown() {
        return false;
    }

    @Override
    protected int getNoteId() {
        return R.id.stopwatch_notification_service;
    }

    @Override
    protected void handleDefaultAction(Intent intent, int flags, int startId) {
        // TODO: Why do we need this check? Won't KEY_START_TIME always have a value of 0 here?
        if (mPrefs.getLong(StopwatchFragment.KEY_START_TIME, 0) == 0) {
            mCurrentLap = new Lap();
            mUpdateHandler.asyncInsert(mCurrentLap);
        }
        syncNotificationWithStopwatchState(true/*always true*/);
        // We don't need to write anything to SharedPrefs because if we're here, StopwatchFragment
        // will start this service again with ACTION_START_PAUSE, which will do the writing.
    }

    @Override
    protected void handleStartPauseAction(Intent intent, int flags, int startId) {
        boolean running = mPrefs.getBoolean(StopwatchFragment.KEY_CHRONOMETER_RUNNING, false);
        SharedPreferences.Editor editor = mPrefs.edit();
        editor.putBoolean(StopwatchFragment.KEY_CHRONOMETER_RUNNING, !running);
        if (running) {
            editor.putLong(StopwatchFragment.KEY_PAUSE_TIME, SystemClock.elapsedRealtime());
            mCurrentLap.pause();
            mUpdateHandler.asyncUpdate(mCurrentLap.getId(), mCurrentLap);
        } else {
            long startTime = mPrefs.getLong(StopwatchFragment.KEY_START_TIME, 0);
            long pauseTime = mPrefs.getLong(StopwatchFragment.KEY_PAUSE_TIME, 0);
            startTime += SystemClock.elapsedRealtime() - pauseTime;
            editor.putLong(StopwatchFragment.KEY_START_TIME, startTime);
            editor.putLong(StopwatchFragment.KEY_PAUSE_TIME, 0);
            // TODO: Why do we need this check? Won't this lap always be paused here?
            if (!mCurrentLap.isRunning()) {
                mCurrentLap.resume();
                mUpdateHandler.asyncUpdate(mCurrentLap.getId(), mCurrentLap);
            }
        }
        editor.apply();
        syncNotificationWithStopwatchState(!running);
    }

    @Override
    protected void handleStopAction(Intent intent, int flags, int startId) {
        mPrefs.edit()
                .putLong(StopwatchFragment.KEY_START_TIME, 0)
                .putLong(StopwatchFragment.KEY_PAUSE_TIME, 0)
                .putBoolean(StopwatchFragment.KEY_CHRONOMETER_RUNNING, false)
                .apply();
        // If after this we restart the application, and then start the stopwatch in StopwatchFragment,
        // we will see that first and second laps appear in the list immediately. This is because
        // the laps from before we made this stop action are still in our SQLite database, because
        // they weren't cleared.
        //
        // We can either clear the laps table here, as we've done already, or do as the TODO above
        // says and tell StopwatchFragment to stop itself. The latter would also stop the
        // chronometer view if the fragment is still in view (i.e. app is still open).
        mCurrentLap = null;
        // If this service instance is running in a process different from the one it
        // was originally started in, then this AsyncTask will not finish executing
        // before stopSelf() is called; as such, the laps table will NOT be cleared.
        // This problem does not occur when the service is running in its
        // original process.
//        mUpdateHandler.asyncClear();
//        stopSelf();
        // A workaround is to place both calls in the SAME thread.
        new Thread(new Runnable() {
            @Override
            public void run() {
                mUpdateHandler.getTableManager().clear();
                stopSelf();
            }
        }).start();
    }

    @Override
    protected void handleAction(@NonNull String action, Intent intent, int flags, int startId) {
        if (ACTION_ADD_LAP.equals(action)) {
            if (mPrefs.getBoolean(StopwatchFragment.KEY_CHRONOMETER_RUNNING, false)) {
                mDelegate.setBase(mPrefs.getLong(StopwatchFragment.KEY_START_TIME, SystemClock.elapsedRealtime()));
                String timestamp = mDelegate.formatElapsedTime(SystemClock.elapsedRealtime(),
                        null/*Resources not needed here*/).toString();
                mCurrentLap.end(timestamp);
                mUpdateHandler.asyncUpdate(mCurrentLap.getId(), mCurrentLap);

                Lap newLap = new Lap();
                mUpdateHandler.asyncInsert(newLap);
                mCurrentLap = newLap;
            }
        } else if (ACTION_UPDATE_LAP_TITLE.equals(action)) {
            int lapNumber = intent.getIntExtra(EXTRA_LAP_NUMBER, 0);
            if (lapNumber == 0) {
                Log.w(TAG, "Lap number was not passed in with intent");
            }
            setContentTitle(getNoteId(), getString(R.string.stopwatch_and_lap_number, lapNumber));
            updateNotification(getNoteId(), true);
        } else {
            throw new IllegalArgumentException("StopwatchNotificationService cannot handle action " + action);
        }
    }

    private void syncNotificationWithStopwatchState(boolean running) {
        clearActions(getNoteId());
        addAction(ACTION_ADD_LAP, R.drawable.ic_add_lap_24dp, getString(R.string.lap), getNoteId());
        addStartPauseAction(running, getNoteId());
        addStopAction(getNoteId());

        quitCurrentThread(getNoteId());
        if (running) {
            long startTime = mPrefs.getLong(StopwatchFragment.KEY_START_TIME, SystemClock.elapsedRealtime());
            startNewThread(getNoteId(), startTime);
        }
    }
}
