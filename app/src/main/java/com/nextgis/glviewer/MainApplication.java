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
        Api.ngsOnLowMemory();
        super.onLowMemory();
    }
}
