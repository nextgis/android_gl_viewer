/*
 * Project:  NextGIS Mobile
 * Purpose:  Mobile GIS for Android.
 * Author:   Dmitry Baryshnikov (aka Bishop), bishop.dev@gmail.com
 * Author:   NikitaFeodonit, nfeodonit@yandex.com
 * Author:   Stanislav Petriakov, becomeglory@gmail.com
 * ****************************************************************************
 * Copyright (c) 2012-2016 NextGIS, info@nextgis.com
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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.nextgis.libngui;


import android.support.test.filters.LargeTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import com.nextgis.libngui.activity.NGActivity;
import com.nextgis.libngui.dialog.LocalResourceSelectDialog;
import com.nextgis.libngui.util.ConstantsUI;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.assertion.ViewAssertions.doesNotExist;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.*;
import static org.hamcrest.Matchers.allOf;


/**
 * Instrumentation test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 * @see <a href="https://developer.android.com/training/testing/unit-testing/instrumented-unit-tests.html">Building
 * Instrumented Unit Tests</a>
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class LocalResourceSelectDialogTest
{
    @Rule
    public ActivityTestRule<NGActivity> mActivityRule = new ActivityTestRule<>(NGActivity.class);


    @Before
    public void showDialog()
    {
        NGActivity activity = mActivityRule.getActivity();

        File path = activity.getExternalFilesDir("");

        LocalResourceSelectDialog dialog = new LocalResourceSelectDialog();
        dialog.setPath(path);
        dialog.setTypeMask(ConstantsUI.FILETYPE_ALL_FILE_TYPES);
        dialog.setCanSelectMultiple(false);
        dialog.show(activity.getSupportFragmentManager(), ConstantsUI.FRAGMENT_SELECT_RESOURCE);
    }


    @Test
    public void dialogIsCanceled()
            throws Exception
    {
        // Dialog is displayed
        onView(allOf(withId(R.id.title_text), withText(R.string.select))).check(
                matches(isDisplayed()));
        // Click on the Cancel button
        onView(allOf(withId(R.id.button_negative), withText(R.string.cancel))).perform(click());
        // Dialog is not displayed
        // http://stackoverflow.com/a/28432205
        // If the view you're looking for is there in the view hierarchy but invisible,
        // then you need to use not(isDisplayed).
        // However, if the view is not there in the view hierarchy, you need to use doesNotExist().
        onView(allOf(withId(R.id.title_text), withText(R.string.select))).check(doesNotExist());
    }
}
