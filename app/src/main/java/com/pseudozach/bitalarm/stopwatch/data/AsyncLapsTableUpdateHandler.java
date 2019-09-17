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

package com.pseudozach.bitalarm.stopwatch.data;

import android.content.Context;
import android.content.Intent;

import com.pseudozach.bitalarm.data.AsyncDatabaseTableUpdateHandler;
import com.pseudozach.bitalarm.list.ScrollHandler;
import com.pseudozach.bitalarm.stopwatch.Lap;
import com.pseudozach.bitalarm.stopwatch.StopwatchNotificationService;

/**
 * Created by Phillip Hsu on 8/9/2016.
 */
public class AsyncLapsTableUpdateHandler extends AsyncDatabaseTableUpdateHandler<Lap, LapsTableManager> {

    public AsyncLapsTableUpdateHandler(Context context, ScrollHandler scrollHandler) {
        super(context, scrollHandler);
    }

    @Override
    protected LapsTableManager onCreateTableManager(Context context) {
        return new LapsTableManager(context);
    }

    @Override
    protected void onPostAsyncInsert(Long result, Lap item) {
        if (result > 1) {
            // Update the notification's title with this lap number
            Intent intent = new Intent(getContext(), StopwatchNotificationService.class)
                    .setAction(StopwatchNotificationService.ACTION_UPDATE_LAP_TITLE)
                    .putExtra(StopwatchNotificationService.EXTRA_LAP_NUMBER, result.intValue());
            getContext().startService(intent);
        }
    }

    // ===================== DO NOT IMPLEMENT =========================

    @Override
    protected void onPostAsyncDelete(Integer result, Lap item) {
        // Leave blank.
    }

    @Override
    protected void onPostAsyncUpdate(Long result, Lap item) {
        // Leave blank.
    }
}
