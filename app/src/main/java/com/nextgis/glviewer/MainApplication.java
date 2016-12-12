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

package com.nextgis.glviewer;

import android.app.Application;
import android.content.pm.PackageManager;
import android.util.Log;
import com.nextgis.ngsandroid.NgsAndroidJni;
import com.nextgis.store.bindings.Api;
import com.nextgis.store.bindings.Options;
//import com.testfairy.TestFairy;

import java.io.File;


public class MainApplication
        extends Application
{
    @Override
    public void onCreate()
    {
        super.onCreate();

//        TestFairy.begin(this, "1ff0bcca6efd0ec1280a02e0276e89ecc7293d27");

        NgsAndroidJni.initLogger();
        Log.d(Constants.TAG, "NGS version: " + Api.ngsGetVersionString(null));

        initNgs();
    }


    public String getApkPath()
    {
        try {
            return getPackageManager().getApplicationInfo(getPackageName(), 0).sourceDir;
        } catch (PackageManager.NameNotFoundException e) {
            Log.d(Constants.TAG, e.getLocalizedMessage());
            return null;
        }
    }


    public String getMapPath()
    {
        File defaultPath = getExternalFilesDir("");
        if (defaultPath == null) {
            defaultPath = new File(getFilesDir(), "");
        }
        return defaultPath.getPath();
    }


    public String getGdalPath()
    {
        return "/vsizip" + getApkPath() + "/assets/gdal_data";
    }


    protected void initNgs()
    {
        Api.ngsInit(getGdalPath(), null);
        Api.ngsSetOptions(Options.OPT_DEBUGMODE);
        Log.d(Constants.TAG, "NGS formats: " + Api.ngsGetVersionString("formats"));
    }


    @Override
    public void onLowMemory()
    {
        // TODO: ngsOnLowMemory
        Log.d(Constants.TAG, "onLowMemory() is fired");
//        Api.ngsOnLowMemory();
        super.onLowMemory();
    }
}
