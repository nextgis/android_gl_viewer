package com.nextgis.glviewer;

import android.app.Application;
import android.content.pm.PackageManager;
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


    public String getApkPath()
    {
        try {
            return getPackageManager().getApplicationInfo(getPackageName(), 0).sourceDir;
        } catch (PackageManager.NameNotFoundException e) {
            Log.d(Constants.TAG, e.getLocalizedMessage());
            return null;
        }
    }
}
