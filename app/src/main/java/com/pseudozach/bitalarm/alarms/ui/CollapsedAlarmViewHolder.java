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

package com.pseudozach.bitalarm.alarms.ui;

import android.os.SystemClock;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.TextView;

import com.pseudozach.bitalarm.R;
import com.pseudozach.bitalarm.list.OnListItemInteractionListener;
//import com.philliphsu.clock2.R;
import com.pseudozach.bitalarm.alarms.Alarm;
import com.pseudozach.bitalarm.alarms.misc.AlarmController;
import com.pseudozach.bitalarm.alarms.misc.DaysOfWeek;
import com.pseudozach.bitalarm.stopwatch.Lap;
import com.pseudozach.bitalarm.stopwatch.ui.ChronometerWithMillis;

import butterknife.BindView;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static com.pseudozach.bitalarm.alarms.misc.DaysOfWeek.NUM_DAYS;

/**
 * Created by Phillip Hsu on 5/31/2016.
 */
public class CollapsedAlarmViewHolder extends BaseAlarmViewHolder {

    @BindView(R.id.countdown) AlarmCountdown mCountdown;
    @BindView(R.id.countup) AlarmCountup mCountup;
    /*@BindView(R.id.elapsed_time)
    ChronometerWithMillis mElapsedTime;*/
    @BindView(R.id.recurring_days) TextView mDays; // TODO: use `new DateFormatSymbols().getShortWeekdays()` to set texts

    public CollapsedAlarmViewHolder(ViewGroup parent, OnListItemInteractionListener<Alarm> listener,
                                    AlarmController alarmController) {
        super(parent, R.layout.item_collapsed_alarm, listener, alarmController);
    }

    @Override
    public void onBind(Alarm alarm) {
        super.onBind(alarm);
        // TOneverDO: do custom binding before super call, or else NPEs.
        //bindCountdown(alarm.isEnabled(), alarm.ringsAt());
        //bindCountup(alarm.isSnoozed(), alarm.isEnabled(), alarm.ringsAt());
        //bindElapsedTime(alarm, alarm.ringsAt());
        bindDays(alarm);
    }

    private void bindCountdown(boolean enabled, long ringsAt) {
        if (enabled) {
            mCountdown.setBase(ringsAt);
            mCountdown.start();
            mCountdown.setVisibility(VISIBLE);
        } else {
            mCountdown.stop();
            mCountdown.setVisibility(GONE);
        }
    }

    private void bindCountup(boolean isSnoozed, boolean enabled, long ringsAt) {
        if (isSnoozed && enabled) {
            mCountup.setBase(ringsAt);
            mCountup.start();
            mCountup.setVisibility(VISIBLE);
        } else {
            mCountup.stop();
            mCountup.setVisibility(GONE);
        }
    }

    @Override
    protected void bindLabel(boolean visible, String label) {
        // Should also be visible even if label has zero length so mCountdown is properly positioned
        // next to mLabel. That is, mCountdown's layout position is dependent on mLabel being present.

        // The countdown is visible if the alarm is enabled. We must keep this invariant in sync
        // with our bindCountdown() logic. If we test against the
        // visibility of the countdown view itself, we will find it is always visible
        // at this point, because bindCountdown() has not been called yet. As such, that is
        // not a valid solution. We unfortunately
        // cannot change the order of the view binding done in onBind().
        super.bindLabel(visible || getAlarm().isEnabled(), label);
    }

    private void bindDays(Alarm alarm) {
        int num = alarm.numRecurringDays();
        String text;
        if (num == NUM_DAYS) {
            text = getContext().getString(R.string.every_day);
        } else if (num == 0) {
            text = "";
        } else {
            StringBuilder sb = new StringBuilder();
            for (int i = 0 /* Ordinal days*/; i < NUM_DAYS; i++) {
                // What day is at this position in the week?
                int weekDay = DaysOfWeek.getInstance(getContext()).weekDayAt(i);
                if (alarm.isRecurring(weekDay)) {
                    sb.append(DaysOfWeek.getLabel(weekDay)).append(", ");
                }
            }
            // Cut off the last comma and space
            sb.delete(sb.length() - 2, sb.length());
            text = sb.toString();
        }
        bindDays(num > 0, text);
    }

    private void bindDays(boolean visible, String text) {
        setVisibility(mDays, visible);
        mDays.setText(text);
    }

    @Override
    void openLabelEditor() {
        // DO NOT IMPLEMENT
    }

    @Override
    void openTimePicker() {
        super.openTimePicker();
        // Pretend we also clicked the itemView, so we get expanded.
        onClick(itemView);
    }


    private void bindElapsedTime(Alarm alarm, long ringsAt) {
        //mElapsedTime.setFormat("SS");
        Log.e("CAV", "current format: " + mElapsedTime.getFormat());

        // In case we're reusing a chronometer instance that could be running:
        // If the lap is not running, this just guarantees the chronometer
        // won't tick, regardless of whether it was running.
        // If the lap is running, we don't care whether the chronometer is
        // also running, because we call start() right after. Stopping it just
        // guarantees that, if it was running, we don't deliver another set of
        // concurrent messages to its handler.
        mElapsedTime.stop();

        // We're going to forget about the + sign in front of the text. I think
        // the 'Elapsed' header column is sufficient to convey what this timer means.
        // (Don't want to figure out a solution)

        //check if ringsat is at future, subtract 1 day!

        //Log.e("CAV", "ringsAt: " + ringsAt); //next alarm timestamp - tomorrow
        //Log.e("CAV", "currenttimemillis: " + System.currentTimeMillis());


        //check if snoozing and then start it
        if(alarm.isSnoozed()){
            mElapsedTime.setDuration(System.currentTimeMillis() - ringsAt + 24*60*60*1000);
            mElapsedTime.start();
        }

        //mElapsedTime.setDuration(System.currentTimeMillis() - ringsAt);
        //if (lap.isRunning()) {
            //mElapsedTime.start();
        //}
    }
}
