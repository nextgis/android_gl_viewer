package com.nextgis.glviewer;

import android.app.Application;
import android.content.pm.PackageManager;
import android.util.Log;
import com.nextgis.ngsandroid.NgsAndroidJni;
import com.nextgis.store.bindings.Api;

import java.io.File;


public class MainApplication
        extends Application
{
    @Override
    public void onCreate()
    {
        super.onCreate();

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
        Log.d(Constants.TAG, "NGS formats: " + Api.ngsGetVersionString("formats"));
    }
}
