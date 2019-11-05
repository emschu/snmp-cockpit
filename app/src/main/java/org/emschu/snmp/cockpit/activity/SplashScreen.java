/*
 * SNMP Cockpit Android App
 *
 * Copyright (C) 2018-2019
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package org.emschu.snmp.cockpit.activity;

import android.app.Activity;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.PixelFormat;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;

import org.emschu.snmp.cockpit.CockpitMainActivity;
import org.emschu.snmp.cockpit.R;
import org.emschu.snmp.cockpit.persistence.CockpitDbHelper;

/**
 * splash screen activity of this app. this is the de-facto launcher activity
 */
@SuppressWarnings("squid:MaximumInheritanceDepth")
public class SplashScreen extends Activity {

    public static final String TAG = SplashScreen.class.getName();

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        Window window = getWindow();
        window.setFormat(PixelFormat.RGBA_8888);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splashscreen);

        Log.d(TAG, "db init started for 3 tables");
        SQLiteDatabase readableDatabase = new CockpitDbHelper(SplashScreen.this).getReadableDatabase();
        readableDatabase.close();
        Log.d(TAG, "db init finished for 3 tables");

        startAnimations();
    }

    /**
     * Starting the animation
     */
    private void startAnimations() {
        //Setting the background of the splash screen
        Animation splashAnimation = AnimationUtils.loadAnimation(this, R.anim.alpha);
        splashAnimation.reset();
        LinearLayout splashLayout = findViewById(R.id.splash_layout);
        splashLayout.clearAnimation();
        splashLayout.startAnimation(splashAnimation);

        //Setting the splash picture and animation
        splashAnimation = AnimationUtils.loadAnimation(this, R.anim.translate);
        splashAnimation.reset();
        ImageView splashImage = findViewById(R.id.splash_image);
        splashImage.clearAnimation();
        splashImage.startAnimation(splashAnimation);

        //splashTread which sets the timer of the splash screen.
        new Handler().postDelayed(() -> {
            Intent intent = new Intent(SplashScreen.this,
                    CockpitMainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            startActivity(intent);
            SplashScreen.this.finish();
        }, 2000);
    }

}