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

package com.pseudozach.bitalarm;

import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.pseudozach.bitalarm.timepickers.Utils;

import androidx.annotation.CallSuper;
import androidx.annotation.LayoutRes;
import androidx.annotation.MenuRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.drawable.DrawableCompat;
import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by Phillip Hsu on 5/31/2016.
 */
public abstract class BaseActivity extends AppCompatActivity {

    @Nullable
    @BindView(R.id.toolbar)
    Toolbar mToolbar;

    private Menu mMenu;
    private FirebaseAuth mAuth;
    private String TAG = "BA";

    @Nullable
    @BindView(R.id.tabs)
    TabLayout mTabLayout;

    @LayoutRes protected abstract int layoutResId();
    @MenuRes   protected abstract int menuResId();

    @CallSuper
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Initialize the associated SharedPreferences file with default values
        // for each preference when the user first opens your application.
        // When false, the system sets the default values only if this method has
        // never been called in the past (or the KEY_HAS_SET_DEFAULT_VALUES in the
        // default value shared preferences file is false).
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        // ========================================================================================
        // TOneverDO: Set theme after setContentView()
        final String themeDark = getString(R.string.theme_dark);
        final String themeBlack = getString(R.string.theme_black);
        String theme = PreferenceManager.getDefaultSharedPreferences(this).getString(
                getString(R.string.key_theme), null);
        if (themeDark.equals(theme)) {
            setTheme(R.style.AppTheme_Dark);
        } else if (themeBlack.equals(theme)) {
            setTheme(R.style.AppTheme_Black);
        }
        // ========================================================================================
        setContentView(layoutResId());
        // Direct volume changes to the alarm stream
        setVolumeControlStream(AudioManager.STREAM_ALARM);
        ButterKnife.bind(this);
        if (mToolbar != null) {
            setSupportActionBar(mToolbar);
            ActionBar ab = getSupportActionBar();
            if (ab != null) {
                ab.setDisplayHomeAsUpEnabled(isDisplayHomeUpEnabled());
                ab.setDisplayShowTitleEnabled(isDisplayShowTitleEnabled());
            }
        }

        Log.e("BA", "baseactivity started, logging in...");
        mAuth = FirebaseAuth.getInstance();
        if(mAuth.getCurrentUser() == null){
            mAuth.signInAnonymously()
                    .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (task.isSuccessful()) {
                                // Sign in success, update UI with the signed-in user's information
                                Log.d(TAG, "signInAnonymously:success");
                                FirebaseUser user = mAuth.getCurrentUser();
                                updateUI(user);
                            } else {
                                // If sign in fails, display a message to the user.
                                Log.w(TAG, "signInAnonymously:failure", task.getException());
                                Toast.makeText(BaseActivity.this, "Authentication failed.",
                                        Toast.LENGTH_SHORT).show();
                                updateUI(null);
                            }

                            // ...
                        }
                    });
        } else {
            Log.e("BA", "already logged in: " + mAuth.getCurrentUser().getUid());
        }

    }

    @Override
    public final boolean onCreateOptionsMenu(Menu menu) {
        if (menuResId() != 0) {
            getMenuInflater().inflate(menuResId(), menu);
            mMenu = menu;
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Nullable
    public final Menu getMenu() {
        return mMenu;
    }

    protected boolean isDisplayHomeUpEnabled() {
        return true;
    }

    protected boolean isDisplayShowTitleEnabled() {
        return false;
    }


    @Override
    protected void onStart() {
        super.onStart();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        updateUI(currentUser);
    }

    private void updateUI(FirebaseUser currentUser){
        if(currentUser == null){
            Log.e("BA", "user null");
        } else {
            Log.e("BA", "logged in: " + currentUser.getUid());
        }
    }
}
