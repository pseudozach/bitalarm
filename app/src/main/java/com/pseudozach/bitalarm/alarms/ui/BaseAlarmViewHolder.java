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

import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import androidx.annotation.IdRes;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

//import com.philliphsu.clock2.R;
import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.pseudozach.bitalarm.R;
import com.pseudozach.bitalarm.alarms.Alarm;
import com.pseudozach.bitalarm.alarms.misc.AlarmController;
import com.pseudozach.bitalarm.alarms.misc.AlarmPreferences;
import com.pseudozach.bitalarm.dialogs.AddLabelDialog;
import com.pseudozach.bitalarm.dialogs.AddLabelDialogController;
import com.pseudozach.bitalarm.dialogs.TimePickerDialogController;
import com.pseudozach.bitalarm.list.BaseViewHolder;
import com.pseudozach.bitalarm.list.OnListItemInteractionListener;
import com.philliphsu.bottomsheetpickers.time.BottomSheetTimePickerDialog.OnTimeSetListener;
import com.pseudozach.bitalarm.stopwatch.ui.ChronometerWithMillis;
import com.pseudozach.bitalarm.timepickers.Utils;
import com.pseudozach.bitalarm.util.FragmentTagUtils;
import com.pseudozach.bitalarm.util.TimeTextUtils;

import org.json.JSONObject;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import butterknife.BindView;
import butterknife.OnCheckedChanged;
import butterknife.OnClick;
import butterknife.OnTouch;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static com.pseudozach.bitalarm.util.TimeFormatUtils.formatTime;

/**
 * Created by Phillip Hsu on 7/31/2016.
 */
public abstract class BaseAlarmViewHolder extends BaseViewHolder<Alarm> {
    private static final String TAG = "BaseAlarmViewHolder";

    private FirebaseAuth mAuth;

    private final AlarmController mAlarmController;
    private final AddLabelDialogController mAddLabelDialogController;
    private final TimePickerDialogController mTimePickerDialogController;

    // TODO: Should we use VectorDrawable type?
    private final Drawable mDismissNowDrawable;
    private final Drawable mCancelSnoozeDrawable;

    final FragmentManager mFragmentManager;

    @BindView(R.id.time) TextView mTime;
    @BindView(R.id.on_off_switch) SwitchCompat mSwitch;
    @BindView(R.id.label) TextView mLabel;
    @BindView(R.id.dismiss) Button mDismissButton;

    @BindView(R.id.elapsed_time)
    ChronometerWithMillis mElapsedTime;

    @Nullable
    @BindView(R.id.snoozecountertv) TextView snoozecountertv;

    public BaseAlarmViewHolder(ViewGroup parent, @LayoutRes int layoutRes,
                               OnListItemInteractionListener<Alarm> listener,
                               AlarmController controller) {
        super(parent, layoutRes, listener);
        mAlarmController = controller;
        // Because of VH binding, setting drawable resources on views would be bad for performance.
        // Instead, we create and cache the Drawables once.
        mDismissNowDrawable = ContextCompat.getDrawable(getContext(), R.drawable.ic_dismiss_alarm_24dp);
        mCancelSnoozeDrawable = ContextCompat.getDrawable(getContext(), R.drawable.ic_cancel_snooze);

        // TODO: This is bad! Use a Controller/Presenter instead...
        // or simply pass in an instance of FragmentManager to the ctor.
        AppCompatActivity act = (AppCompatActivity) getContext();
        mFragmentManager = act.getSupportFragmentManager();
        mAddLabelDialogController = new AddLabelDialogController(mFragmentManager,
            new AddLabelDialog.OnLabelSetListener() {
                @Override
                public void onLabelSet(String label) {
                    final Alarm oldAlarm = getAlarm();
                    Alarm newAlarm = oldAlarm.toBuilder()
                            .label(label)
                            .build();
                    oldAlarm.copyMutableFieldsTo(newAlarm);
                    persistUpdatedAlarm(newAlarm, false);
                }
            }
        );
        mTimePickerDialogController = new TimePickerDialogController(mFragmentManager, getContext(),
            new OnTimeSetListener() {
                @Override
                public void onTimeSet(ViewGroup viewGroup, int hourOfDay, int minute) {
                    final Alarm oldAlarm = getAlarm();
                    // I don't think we need this; scheduling a new alarm that is considered
                    // equal to a previous alarm will overwrite the previous alarm.
//                        mAlarmController.cancelAlarm(oldAlarm, false);
                    Alarm newAlarm = oldAlarm.toBuilder()
                            .hour(hourOfDay)
                            .minutes(minute)
                            .build();
                    oldAlarm.copyMutableFieldsTo(newAlarm);
                    // -------------------------------------------
                    // TOneverDO: precede copyMutableFieldsTo()
                    newAlarm.setEnabled(true); // Always enabled, esp. if oldAlarm is not enabled
                    // ----------------------------------------------
                    persistUpdatedAlarm(newAlarm, true);
                }
            }
        );
    }

    @Override
    public void onBind(Alarm alarm) {
        super.onBind(alarm);
        // Items that are not in view will not be bound. If in one orientation the item was in view
        // and in another it is out of view, then the callback for that item will not be restored
        // for the new orientation.
        mAddLabelDialogController.tryRestoreCallback(makeTag(R.id.label));
        mTimePickerDialogController.tryRestoreCallback(makeTag(R.id.time));
        bindTime(alarm);
        bindSwitch(alarm.isEnabled());
        bindDismissButton(alarm);
        bindLabel(alarm.label());
        bindSnoozeCounter(alarm);
        bindElapsedTime(alarm, alarm.ringsAt());

        mAuth = FirebaseAuth.getInstance();
    }

    private void bindSnoozeCounter(Alarm alarm) {

    }

    /**
     * Exposed to subclasses if they have different visibility criteria.
     * The default criteria for visibility is if {@code label} has
     * a non-zero length.
     */
    protected void bindLabel(boolean visible, String label) {
        setVisibility(mLabel, visible);
        mLabel.setText(label);
    }

    /**
     * Exposed to subclasses if they have visibility logic for their views.
     */
    protected final void setVisibility(@NonNull View view, boolean visible) {
        view.setVisibility(visible ? VISIBLE : GONE);
    }

    protected final Alarm getAlarm() {
        return getItem();
    }

    @OnClick(R.id.dismiss)
    void dismiss() {
        Alarm alarm = getAlarm();

        //no need because dismiss also unchecks the switch! -only when it's a 1 day alarm!!!
        if(alarm.isSnoozed()){
            Log.e("BAVdismiss","1 while snoozed!!!");
            sendSnoozedSats();
        } else {
            Log.e("BAVdismiss","1 while NOT snoozed!!!");
        }

        if (!alarm.hasRecurrence()) {
            // This is a single-use alarm, so turn it off completely.
            mSwitch.setPressed(true); // needed so the OnCheckedChange event calls through
            bindSwitch(false); // fires OnCheckedChange to turn off the alarm for us
        } else {
            // Dismisses the current upcoming alarm and handles scheduling the next alarm for us.
            // Since changes are saved to the database, this prompts a UI refresh.
            mAlarmController.cancelAlarm(alarm, false, true);
        }





        // TOneverDO: AlarmUtils.cancelAlarm() otherwise it will be called twice
        /*
        AlarmUtils.cancelAlarm(getContext(), getAlarm());
        if (!getAlarm().isEnabled()) {
            // TOneverDO: mSwitch.setPressed(true);
            bindSwitch(false); // will fire OnCheckedChange, but switch isn't set as pressed so nothing happens.
            bindCountdown(false, -1);
        }
        bindDismissButton(false, ""); // Will be set to correct text the next time we bind.
        // If cancelAlarm() modified the alarm's fields, then it will save changes for you.
        */
    }

    @OnTouch(R.id.on_off_switch)
    boolean slide(MotionEvent event) {
        if (event.getActionMasked() == MotionEvent.ACTION_MOVE) {
            mSwitch.setPressed(true); // needed so the OnCheckedChange event calls through
        }

        /*if(!mSwitch.isChecked()) {
            Log.e("BAVon_off_switch","2");
            sendSnoozedSats();
        }*/


        return false; // proceed as usual
    }

    private void sendSnoozedSats() {

        Alarm alarm = getAlarm();
        alarm.stopSnoozing(); //to avoid double triggers!

        String snoozedvalue = mElapsedTime.getText().toString();

        if(mAuth.getCurrentUser() == null){
            Log.e("BAVsnoozed", "No logged in user, do nothing!");
            return;
        }

        Log.e("BAV", "sendSnoozedSats snoozedvalue: " + snoozedvalue);
        double  a = Double.parseDouble(snoozedvalue);
        long b = Math.round(a);
        Log.e("BAV", "wtf:: " + b);
        alarm.setSnoozedDuration(b);

        String amount = String.valueOf(b);
        if(amount.equals("0")){
            Log.e("BAVsnoozed", "0 no snooze, do nothing!");
            return;
        }

        //cancelling alarm so send any snoozed sats to the donation place!
        Log.e(TAG, "sending snoozed sats...");
        SharedPreferences prefs =
                PreferenceManager.getDefaultSharedPreferences(getContext());
        String donateto = prefs.getString("donateto", "torproject");

        //check if 1 sec = 1 msat
        String brokeorwoke = prefs.getString("satspersecond", "Woke: 1 sec = 1 sat");
        if (brokeorwoke != null && brokeorwoke.contains("Broke")) {
            if(Math.round(b / 1000) < 1){
                Log.e("BAV", Math.round(b / 1000) + " is smaller than 1, cant send!");
                Snackbar.make(mTime, "Unable to donate less than 1 satoshi.", Snackbar.LENGTH_LONG).show();

                mElapsedTime.stop();
                mElapsedTime.init();
                alarm.setSnoozedDuration(0);
                mElapsedTime.setText("0.000");

                //mElapsedTime.setBase(SystemClock.elapsedRealtime());
                return;
            }
            amount = String.valueOf(Math.round(b / 1000));
        }

        Log.e(TAG,"donateto:: " + donateto + ", amount: " + amount);

        Snackbar.make(mTime, "Requesting invoice, please wait...", Snackbar.LENGTH_LONG).show();
        //String userId = mAuth.getCurrentUser().getUid();
        // Instantiate the RequestQueue.
        RequestQueue queue = Volley.newRequestQueue(getContext());
        String url ="https://aqueous-fjord-19834.herokuapp.com/bitalarmdonate?donateto="+donateto + "&amount="+amount
                + "&userId=" + mAuth.getCurrentUser().getUid();
        Log.e("BAV", "requesting url:: " + url);

        String finalAmount = amount;
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Log.e("BAV", "stringresponse: " + response);
                String[] splitted = response.split(",");
                if(splitted[0].equals("OK")){
                    Log.e("BAV", "donation DONE!");
                    Snackbar.make(mTime, finalAmount + " Satoshis sent to " + donateto, Snackbar.LENGTH_SHORT).show();
                } else {
                    Log.e("BAV", "issue with donation!");
                    Snackbar.make(mTime, "Issue with donation.", Snackbar.LENGTH_SHORT).show();
                }
                mElapsedTime.stop();
                mElapsedTime.init();
                alarm.setSnoozedDuration(0);
                mElapsedTime.setText("0.000");
                //String payreq = splitted[1].split(">")[0];
                //Log.e("BAV", "payreq:: " + payreq);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse (VolleyError error){
                Log.e("BAV", "nope: " + error.getMessage());
                //textView.setText("That didn't work!");
            }
        });
        /*{ //no semicolon or coma
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> params = new HashMap<String, String>();
                params.put("Content-Type", "application/json");
                params.put("User-agent", "curl/7.54.0");
                return params;
            }*/
        //};
        queue.add(stringRequest);




        // Request a string response from the provided URL.
        /*JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url,null, new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {
                    Log.e("BAV", "jsonresponse: " + response.toString());


                    //trigger call to pay the invoice!

                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse (VolleyError error){
                Log.e("BAV", "nope: " + error.getMessage());
                //textView.setText("That didn't work!");
            }
        });*/
        /*{ //no semicolon or coma
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> params = new HashMap<String, String>();
                params.put("Content-Type", "application/json");
                params.put("User-agent", "curl/7.54.0");
                return params;
            }
        };*/
        //queue.add(jsonObjectRequest);

    }

//    // Changed in favor of OnCheckedChanged
//    @Deprecated
//    @OnClick(R.id.on_off_switch)
//    void toggle() {
//        Alarm alarm = getAlarm();
//        alarm.setEnabled(mSwitch.isChecked());
//        if (alarm.isEnabled()) {
//            AlarmUtils.scheduleAlarm(getContext(), alarm);
//            bindCountdown(true, alarm.ringsIn());
//            bindDismissButton(alarm);
//        } else {
//            AlarmUtils.cancelAlarm(getContext(), alarm); // might save repo
//            bindCountdown(false, -1);
//            bindDismissButton(false, "");
//        }
//        save();
//    }

    @OnCheckedChanged(R.id.on_off_switch)
    void toggle(boolean checked) {
        Alarm alarm = getAlarm();

        if(!checked && alarm.isSnoozed()){
            Log.e("BAVoncheckedchange", "switch turned off while snoozing!!!");
            sendSnoozedSats();
        } else {
            Log.e("BAVoncheckedchange", "switch turned off while NOT snoozing!!!");

            //clear the counter
            mElapsedTime.stop();
            mElapsedTime.init();
            alarm.setSnoozedDuration(0);
            mElapsedTime.setText("0.000");
        }

        // http://stackoverflow.com/q/27641705/5055032
        if (mSwitch.isPressed()) { // filters out automatic calls from VH binding
            // don't need to toggle the switch state

            alarm.setEnabled(checked);
            if (alarm.isEnabled()) {
                // TODO: On 21+, upcoming notification doesn't post immediately
                persistUpdatedAlarm(alarm, true);
            } else {
                mAlarmController.cancelAlarm(alarm, false, false);
                // cancelAlarm() already calls save() for you.
            }
            mSwitch.setPressed(false); // clear the pressed focus, esp. if setPressed(true) was called manually

        }
    }

    @OnClick(R.id.time)
    void openTimePicker() {
        Alarm alarm = getAlarm();
        mTimePickerDialogController.show(alarm.hour(), alarm.minutes(), makeTag(R.id.time));
    }

    @OnClick(R.id.label)
    void openLabelEditor() {
        mAddLabelDialogController.show(mLabel.getText(), makeTag(R.id.label));
    }

    /**
     * Helper method that should be called each time a change is made to the underlying alarm.
     * We should schedule a new alarm with the AlarmManager any time a change is made, even when
     * it was not the alarm's time that changed. This is so that we cancel and update the
     * PendingIntent's extra data with the most up-to-date Alarm's values. The effect of this
     * is to guarantee that the Intent that will launch RingtoneActivity has the most up-to-date
     * extra data about the updated alarm.
     *
     * @param newAlarm The new alarm that has the updated values
     */
    final void persistUpdatedAlarm(Alarm newAlarm, boolean showSnackbar) {
        mAlarmController.scheduleAlarm(newAlarm, showSnackbar);
        mAlarmController.save(newAlarm);
    }

    private void bindTime(Alarm alarm) {
        String time = DateFormat.getTimeFormat(getContext()).format(new Date(alarm.ringsAt()));
        if (DateFormat.is24HourFormat(getContext())) {
            mTime.setText(time);
        } else {
            TimeTextUtils.setText(time, mTime);
        }

        // Use a mock TextView to get our colors, because its ColorStateList is never
        // mutated for the lifetime of this ViewHolder (even when reused).
        // This solution is robust against dark/light theme changes, whereas using
        // color resources is not.
        TextView colorsSource = itemView.findViewById(R.id.colors_source);
        ColorStateList colors = colorsSource.getTextColors();
        int def = colors.getDefaultColor();
        // Too light
//        int disabled = colors.getColorForState(new int[] {-android.R.attr.state_enabled}, def);
        // Material guidelines say text hints and disabled text should have the same color.
        int disabled = colorsSource.getCurrentHintTextColor();
        // However, digging around in the system's textColorHint for 21+ says its 50% black for our
        // light theme. I'd like to follow what the guidelines says, but I want code that is robust
        // against theme changes. Alternatively, override the attribute values to what you want
        // in both your dark and light themes...
//        int disabled = ContextCompat.getColor(getContext(), R.color.text_color_disabled_light);
        // We only have two states, so we don't care about losing the other state colors.
        mTime.setTextColor(alarm.isEnabled() ? def : disabled);
    }

    private void bindSwitch(boolean enabled) {
        mSwitch.setChecked(enabled);
    }

    private void bindDismissButton(Alarm alarm) {
        final int hoursBeforeUpcoming = AlarmPreferences.hoursBeforeUpcoming(getContext());
        boolean upcoming = hoursBeforeUpcoming > 0 && alarm.ringsWithinHours(hoursBeforeUpcoming);
        boolean snoozed = alarm.isSnoozed();
        boolean visible = alarm.isEnabled() && (upcoming || snoozed);
        String buttonText = snoozed
                ? getContext().getString(R.string.title_snoozing_until, formatTime(getContext(), alarm.snoozingUntil()))
                : getContext().getString(R.string.dismiss_now);
        setVisibility(mDismissButton, visible);
        mDismissButton.setText(buttonText);
        // Set drawable start
        Drawable icon = upcoming ? mDismissNowDrawable : mCancelSnoozeDrawable;
        Utils.setTint(icon, mDismissButton.getCurrentTextColor());
        mDismissButton.setCompoundDrawablesRelativeWithIntrinsicBounds(icon, null, null, null);
    }

    private void bindLabel(String label) {
        boolean visible = label.length() > 0;
        bindLabel(visible, label);
    }

    private String makeTag(@IdRes int viewId) {
        return FragmentTagUtils.makeTag(BaseAlarmViewHolder.class, viewId, getItemId());
    }

    private void bindElapsedTime(Alarm alarm, long ringsAt) {

        //mElapsedTime.setCountDown(false);

        //mElapsedTime.setFormat("SS");
        //Log.e("BAV", "bindElapsedTime: " + mElapsedTime.getFormat());

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
        Log.e("CAV", "ringsAt: " + ringsAt); //next alarm timestamp - tomorrow
        Log.e("CAV", "currenttimemillis: " + System.currentTimeMillis());


        long additionthing = 24*60*60*1000;
        //check if snoozing and then start it
        if(alarm.isSnoozed()){
            //Log.e("BAV", "issnoozed start");

            //1 day in ms

            do {
                ringsAt = ringsAt - additionthing;
            } while (ringsAt - System.currentTimeMillis() > additionthing);

            if(System.currentTimeMillis() - ringsAt > 0){
                mElapsedTime.setDuration(System.currentTimeMillis() - ringsAt);
            } else {
                mElapsedTime.setDuration(System.currentTimeMillis() - ringsAt + additionthing);
            }

            mElapsedTime.start();
        }

        //mElapsedTime.setDuration(System.currentTimeMillis() - ringsAt);
        //if (lap.isRunning()) {
        //mElapsedTime.start();
        //}
    }

}
