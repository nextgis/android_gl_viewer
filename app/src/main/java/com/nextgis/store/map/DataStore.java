package com.nextgis.store.map;

import android.util.Log;
import com.nextgis.glviewer.Constants;
import com.nextgis.store.bindings.Api;
import com.nextgis.store.bindings.ErrorCodes;
import com.nextgis.store.bindings.LoadTaskInfo;
import com.nextgis.store.bindings.ProgressCallback;

import java.io.File;


public class DataStore
{
    protected static final int    DEFAULT_EPSG     = 3857;
    protected static final double DEFAULT_MAX_X    = 20037508.34; // 180.0
    protected static final double DEFAULT_MAX_Y    = 20037508.34; // 90.0
    protected static final double DEFAULT_MIN_X    = -DEFAULT_MAX_X;
    protected static final double DEFAULT_MIN_Y    = -DEFAULT_MAX_Y;
    protected static final String DEFAULT_MAP_NAME = "default";

    protected static final double DELTA = 0.00000001;

    protected short mMapId = 0;
    protected String mMapPath;

    protected ProgressCallback mLoadCallback;


    public DataStore(String mapPath)
    {
        mMapPath = mapPath;
        mLoadCallback = createLoadCallback();

        String storePath = mapPath + "/" + Constants.NGS_NAME;
        if (Api.ngsDataStoreInit(storePath) != ErrorCodes.EC_SUCCESS) {
            Log.d(Constants.TAG, "Error: Storage initialize failed");
            //return; // TODO: throw Ex
        }
    }


    public boolean createMap()
    {
        closeMap();
        mMapId = Api.ngsMapCreate(DEFAULT_MAP_NAME, "test gl map", DEFAULT_EPSG, DEFAULT_MIN_X,
                                  DEFAULT_MIN_Y, DEFAULT_MAX_X, DEFAULT_MAX_Y);
        return 0 != mMapId;
    }


    public boolean openMap()
    {
        closeMap();

        File mapNativePath = new File(mMapPath, Constants.DEFAULT_MAP_NAME);
        mMapId = Api.ngsMapOpen(mapNativePath.getPath());

        if (0 == mMapId) {
            Log.d(Constants.TAG, "Error: Map load failed");
            return false;
        }
        return true;
    }


    public boolean closeMap()
    {
        if (0 != mMapId) {
            if (Api.ngsMapClose(mMapId) == ErrorCodes.EC_SUCCESS) {
                mMapId = 0;
            } else {
                Log.d(Constants.TAG, "Error: Close map failed");
                return false;
            }
        }
        return true;
    }


    public void loadMap()
    {
//        File sceneDir = new File(mMapPath, "scenes");
//        File sceneFile = new File(sceneDir, "scenes.shp");
        File sceneDir = new File(mMapPath, "orbview3_catalog");
        File sceneFile = new File(sceneDir, "orbview3_catalog.shp");
        String sceneFilePath = sceneFile.getPath();
        String filenameArray[] = sceneFile.getName().split("\\.");
        String sceneFileName = filenameArray[0];

        String[] options = {"LOAD_OP=COPY", "FEATURES_SKIP=EMPTY_GEOMETRY"};

        if (Api.ngsDataStoreLoad(sceneFileName, sceneFilePath, "", options, mLoadCallback) == 0) {
            Log.d(Constants.TAG, "Error: Load scene failed");
        }
    }


    protected ProgressCallback createLoadCallback()
    {
        return new ProgressCallback()
        {
            @Override
            public int run(
                    int taskId,
                    double complete,
                    String message)
            {
                if (2 - complete < DELTA) {
                    onLoadFinished(taskId);
                }

                return 1;
            }
        };
    }


    protected boolean onLoadFinished(int taskId)
    {
        LoadTaskInfo info = Api.ngsDataStoreGetLoadTaskInfo(taskId);
        if (info.getStatus() != ErrorCodes.EC_SUCCESS) {
            Log.d(Constants.TAG, "Error: Load " + info.getName() + " failed");
            return false;
        }

        File path = new File(info.getDstPath(), info.getNewNames());

        // add loaded data to map as new layer
        if (Api.ngsMapCreateLayer(mMapId, info.getName(), path.getPath())
                == ErrorCodes.EC_SUCCESS) {
            saveMap();
            return true;
        }

        return false;
    }


    public boolean saveMap()
    {
        File ngmdFile = new File(mMapPath, Constants.DEFAULT_MAP_NAME);

        if (Api.ngsMapSave(mMapId, ngmdFile.getPath()) != ErrorCodes.EC_SUCCESS) {
            Log.d(Constants.TAG, "Error: Map save failed");
            return false;
        }
        return true;
    }
}
