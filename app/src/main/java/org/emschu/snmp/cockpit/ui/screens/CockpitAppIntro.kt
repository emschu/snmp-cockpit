/*
 * snmp-cockpit
 *
 * Copyright (C) 2018-2023
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
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package org.emschu.snmp.cockpit.ui.screens

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import com.github.appintro.AppIntro
import com.github.appintro.AppIntroFragment
import org.emschu.snmp.cockpit.MainActivity
import org.emschu.snmp.cockpit.R

/**
 * activity to show information about the app and how to use it
 */
class CockpitAppIntro : AppIntro() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isSystemBackButtonLocked = true
        isWizardMode = true
        setImmersiveMode()

        askForPermissions(permissions = MainActivity.requiredPermissions(), slideNumber = 5, required = true)
        val backgroundColor = R.color.primaryColor

        addSlide(
            AppIntroFragment.createInstance(
                title = getString(R.string.intro_title1),
                imageDrawable = R.drawable.lettering_logo_violet,
                description = getString(R.string.intro_text1),
                backgroundColorRes = backgroundColor
            )
        )
        addSlide(
            AppIntroFragment.createInstance(
                title = getString(R.string.intro_title2),
                imageDrawable = R.drawable.device_code,
                description = getString(R.string.intro_text2),
                backgroundColorRes = backgroundColor
            )
        )
        addSlide(
            AppIntroFragment.createInstance(
                title = getString(R.string.intro_title3),
                imageDrawable = R.drawable.custom_queries,
                description = getString(R.string.intro_text3),
                backgroundColorRes = backgroundColor
            )
        )
        addSlide(
            AppIntroFragment.createInstance(
                title = getString(R.string.intro_title4),
                imageDrawable = R.drawable.catalog,
                description = getString(R.string.intro_text4),
                backgroundColorRes = backgroundColor
            )
        )
        addSlide(
            AppIntroFragment.createInstance(
                title = getString(R.string.intro_title5),
                imageDrawable = R.drawable.lettering_logo_violet,
                description = getString(R.string.intro_text5),
                backgroundColorRes = backgroundColor
            )
        )
    }

    override fun onSkipPressed(currentFragment: Fragment?) {
        super.onSkipPressed(currentFragment)
        finish()
        startActivity(Intent(this.applicationContext, MainActivity::class.java))
    }

    override fun onDonePressed(currentFragment: Fragment?) {
        super.onDonePressed(currentFragment)
        finish()
        startActivity(Intent(this.applicationContext, MainActivity::class.java))
    }
}