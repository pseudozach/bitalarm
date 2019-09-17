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

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.os.SystemClock;
import android.util.Log;

import com.pseudozach.bitalarm.timers.Timer;
import com.pseudozach.bitalarm.data.DatabaseTableManager;

/**
 * Created by Phillip Hsu on 7/30/2016.
 */
public class TimersTableManager extends DatabaseTableManager<Timer> {
    public static final String TAG = "TimersTableManager";

    public TimersTableManager(Context context) {
        super(context);
    }

    @Override
    protected String getQuerySortOrder() {
        return TimersTable.SORT_ORDER;
    }

    @Override
    public TimerCursor queryItem(long id) {
        return wrapInTimerCursor(super.queryItem(id));
    }

    @Override
    public TimerCursor queryItems() {
        return wrapInTimerCursor(super.queryItems());
    }

    public TimerCursor queryStartedTimers() {
        String where = TimersTable.COLUMN_END_TIME + " > " + SystemClock.elapsedRealtime()
                + " OR " + TimersTable.COLUMN_PAUSE_TIME + " > 0";
        return queryItems(where, null);
    }

    @Override
    protected TimerCursor queryItems(String where, String limit) {
        return wrapInTimerCursor(super.queryItems(where, limit));
    }

    @Override
    protected String getTableName() {
        return TimersTable.TABLE_TIMERS;
    }

    @Override
    protected ContentValues toContentValues(Timer timer) {
        ContentValues cv = new ContentValues();
        cv.put(TimersTable.COLUMN_HOUR, timer.hour());
        cv.put(TimersTable.COLUMN_MINUTE, timer.minute());
        cv.put(TimersTable.COLUMN_SECOND, timer.second());
        Log.d(TAG, "toContentValues() label = " + timer.label());
        cv.put(TimersTable.COLUMN_LABEL, timer.label());
//        cv.put(TimersTable.COLUMN_GROUP, timer.group());
        Log.d(TAG, "endTime = " + timer.endTime() + ", pauseTime = " + timer.pauseTime());
        cv.put(TimersTable.COLUMN_END_TIME, timer.endTime());
        cv.put(TimersTable.COLUMN_PAUSE_TIME, timer.pauseTime());
        cv.put(TimersTable.COLUMN_DURATION, timer.duration());
        return cv;
    }

    @Override
    protected String getOnContentChangeAction() {
        return TimersListCursorLoader.ACTION_CHANGE_CONTENT;
    }

    private TimerCursor wrapInTimerCursor(Cursor c) {
        return new TimerCursor(c);
    }
}
