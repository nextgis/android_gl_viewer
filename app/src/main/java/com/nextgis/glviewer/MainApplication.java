package com.nextgis.glviewer;

import android.app.Application;
import android.util.Log;
import com.nextgis.ngsandroid.NgsAndroidJni;
import com.nextgis.store.bindings.Api;


public class MainApplication
        extends Application
{
    @Override
    public void onCreate()
    {
        super.onCreate();

        NgsAndroidJni.initLogger();
        Log.d(Constants.TAG, "NGS version: " + Api.ngsGetVersionString(null));
    }
}
