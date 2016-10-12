package com.nextgis.store.map;

import android.graphics.PointF;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import com.nextgis.glviewer.Constants;
import com.nextgis.store.bindings.Api;
import com.nextgis.store.bindings.Coordinate;
import com.nextgis.store.bindings.DrawState;
import com.nextgis.store.bindings.ErrorCodes;
import com.nextgis.store.bindings.Options;
import com.nextgis.store.bindings.ProgressCallback;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.egl.EGLSurface;


public class MapDrawing
        extends DataStore
{
    protected final static int DRAW_MSG = 10;

    protected EGL10 mEgl;

    protected EGLDisplay mEglDisplay = EGL10.EGL_NO_DISPLAY;
    protected EGLSurface mEglSurface = EGL10.EGL_NO_SURFACE;
    protected EGLContext mEglContext = EGL10.EGL_NO_CONTEXT;

    protected int    mWidth;
    protected int    mHeight;
    protected PointF mCenter;
    protected int mYOrient = 0;

    private int    mDrawState;
    private double mDrawComplete;
    private long   mDrawTimeSync, mDrawTime;
    private int mFeatureCountSync, mFeatureCount;
    private final Object mDrawStateLock    = new Object();
    private final Object mDrawCompleteLock = new Object();
    private final Object mDrawTimeLock     = new Object();

    protected ProgressCallback             mDrawCallback;
    protected OnRequestRenderListener      mOnRequestRenderListener;
    protected OnDrawTimeChangeListener     mOnDrawTimeChangeListener;
    protected OnIndicesCountChangeListener mOnIndicesCountChangeListener;

    protected boolean mNgsDebugMode;

    protected static Handler mHandler;


    public MapDrawing(String mapPath)
    {
        super(mapPath);

        mNgsDebugMode = (Api.ngsGetOptions() & Options.OPT_DEBUGMODE) != 0;
        mCenter = new PointF();
        mDrawCallback = createDrawCallback();
        mDrawTimeSync = mDrawTime = 0;
        mFeatureCountSync = mFeatureCount = 0;
        mHandler = createHandler();

        setDrawState(DrawState.DS_REDRAW);
    }


    public void initMapProps()
    {
        if (0 == mMapId) { return; }

        Api.ngsMapSetBackgroundColor(mMapId, (short) 192, (short) 192, (short) 192, (short) 255);
    }


    @Override
    public boolean createMap()
    {
        if (super.createMap()) {
            initMapProps();
            return true;
        }
        return false;
    }


    @Override
    public boolean openMap()
    {
        if (super.openMap()) {
            initMapProps();
            setDrawState(DrawState.DS_REDRAW);
            return true;
        }
        return false;
    }


    @Override
    public boolean closeMap()
    {
        if (0 != mMapId) {
//            makeCurrent();
            return super.closeMap();
        }
        return false;
    }


    @Override
    protected boolean onLoadFinished(int taskId)
    {
        if (super.onLoadFinished(taskId)) {
            setDrawState(DrawState.DS_NORMAL);
            requestRender();
            return true;
        }
        return false;
    }


    public double invertY(double y)
    {
        return mHeight - y;
    }


    public PointF getCenter()
    {
        return mCenter;
    }


    public void setCenter(
            double x,
            double y)
    {
        Coordinate center = Api.ngsMapGetCoordinate(mMapId, x, invertY(y));
        Api.ngsMapSetCenter(mMapId, center.getX(), center.getY());
    }


    public void offset(
            float x,
            float y)
    {
        PointF pt = new PointF(mCenter.x, mCenter.y);
        pt.offset(x, y);
        setCenter(pt.x, pt.y);
    }


    public int getHeight()
    {
        return mHeight;
    }


    public int getWidth()
    {
        return mWidth;
    }


    public boolean isSizeChanged(
            int width,
            int height)
    {
        return !(width == mWidth && height == mHeight);
    }


    public boolean setSize(
            int width,
            int height)
    {
        if (0 == mMapId || !isSizeChanged(width, height)) {
            return false;
        }

        mWidth = width;
        mHeight = height;
        mCenter.set(width / 2, height / 2);

        setDrawState(DrawState.DS_NORMAL); // TODO: to DS_PRESERVED for performance

        return Api.ngsMapSetSize(mMapId, width, height, mYOrient) == ErrorCodes.EC_SUCCESS;
    }


    public void scale(
            double scaleFactor,
            double focusLocationX,
            double focusLocationY)
    {
        setScaleByFactor(scaleFactor);
        setScaledFocusLocation(scaleFactor, focusLocationX, focusLocationY);
    }


    public void setScaleByFactor(double scaleFactor)
    {
        double scale = Api.ngsMapGetScale(mMapId);
        scale *= scaleFactor;
        Api.ngsMapSetScale(mMapId, scale);
    }


    public void setScaledFocusLocation(
            double scaleFactor,
            double x,
            double y)
    {
        double centerX = mCenter.x;
        double centerY = mCenter.y;
        double distX = centerX - x;
        double distY = centerY - y;
        double scaledDistX = distX * scaleFactor;
        double scaledDistY = distY * scaleFactor;
        double offX = scaledDistX - distX;
        double offY = scaledDistY - distY;

        centerX -= offX;
        centerY -= offY;

        setCenter(centerX, centerY);
    }


    public void makeCurrent()
    {
        if (null != mEgl && EGL10.EGL_NO_DISPLAY == mEgl.eglGetCurrentDisplay()
                && EGL10.EGL_NO_SURFACE == mEgl.eglGetCurrentSurface(EGL10.EGL_DRAW)
                && EGL10.EGL_NO_SURFACE == mEgl.eglGetCurrentSurface(EGL10.EGL_READ)
                && EGL10.EGL_NO_CONTEXT == mEgl.eglGetCurrentContext()) {
            mEgl.eglMakeCurrent(mEglDisplay, mEglSurface, mEglSurface, mEglContext);
        }
    }


    public void releaseCurrent()
    {
        mEgl.eglMakeCurrent(
                mEglDisplay, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_CONTEXT);
    }


    public void initDrawContext(
            EGL10 egl,
            EGLDisplay eglDisplay,
            EGLSurface eglSurface,
            EGLContext eglContext)
    {
        mEgl = egl;
        mEglDisplay = eglDisplay;
        mEglSurface = eglSurface;
        mEglContext = eglContext;

        if (0 == mMapId) { return; }

//        makeCurrent();
        Api.ngsMapInit(mMapId);
    }


    public void setDrawState(int drawState)
    {
        synchronized (mDrawCompleteLock) {
            mDrawComplete = 0;
        }

        synchronized (mDrawStateLock) {
            mDrawState = drawState;

            switch (drawState) {
                case DrawState.DS_REDRAW:
                case DrawState.DS_NORMAL:
                    synchronized (mDrawTimeLock) {
                        mDrawTimeSync = System.currentTimeMillis();
                    }
                    break;

                case DrawState.DS_PRESERVED:
                    break;
            }

            //Log.d(Constants.TAG, "+++ setDrawState, drawState: " + mDrawState);
        }
    }


    protected void requestRender()
    {
        if (null != mOnRequestRenderListener) {
            mOnRequestRenderListener.onRequestRender();
        }
    }


    public void draw()
    {
        synchronized (mDrawStateLock) {
            //Log.d(Constants.TAG, "+++ draw 01, mMapId: " + mMapId + ", mDrawState: " + mDrawState);

            Api.ngsMapDraw(mMapId, mDrawState, mDrawCallback);
            mDrawState = DrawState.DS_PRESERVED; // draw from cache on display update

            //Log.d(Constants.TAG, "+++ draw 02, mMapId: " + mMapId + ", mDrawState: " + mDrawState);
        }
    }


    protected ProgressCallback createDrawCallback()
    {
        return new ProgressCallback()
        {
            @Override
            public int run(
                    int taskId,
                    double complete,
                    String message)
            {
                boolean requested = false;
                synchronized (mDrawCompleteLock) {
                    if (complete - mDrawComplete > 0.045) { // each 5% redraw
                        mDrawComplete = complete;
                        requested = true;
                    }
                }
                if (requested) {
                    requestRender();
                }

                if (complete > 1.999) {
                    long time;
                    synchronized (mDrawTimeLock) {
                        mDrawTimeSync = System.currentTimeMillis() - mDrawTimeSync;
                        time = mDrawTimeSync;
                        Log.d(Constants.TAG, "Native map draw time: " + time);
                    }

                    int count = Integer.valueOf(message);
                    if (-1 < count) {
                        mFeatureCountSync = count;
                    }

                    Bundle bundle = new Bundle();
                    bundle.putLong("time", time);
                    bundle.putInt("count", mFeatureCountSync);

                    Message msg = mHandler.obtainMessage(DRAW_MSG);
                    msg.setData(bundle);
                    msg.sendToTarget();
                }

                return 1;
            }
        };
    }


    protected Handler createHandler()
    {
        return new Handler()
        {
            public void handleMessage(Message msg)
            {
                switch (msg.what) {
                    case DRAW_MSG:
                        Bundle bundle = msg.getData();
                        mDrawTime = bundle.getLong("time");
                        mFeatureCount = bundle.getInt("count");

                        onDrawStop();
                        break;
                }
            }
        };
    }


    public void onDrawStart()
    {
        if (mNgsDebugMode) {
            if (null != mOnDrawTimeChangeListener) {
                mOnDrawTimeChangeListener.onDrawTimeChange("*****");
            }

            if (null != mOnIndicesCountChangeListener) {
                mOnIndicesCountChangeListener.onIndicesCountChange("*****");
            }
        }
    }


    public void onDrawStop()
    {
        if (mNgsDebugMode) {
            if (null != mOnDrawTimeChangeListener) {
                mOnDrawTimeChangeListener.onDrawTimeChange("" + mDrawTime);
            }

            if (null != mOnIndicesCountChangeListener) {
                mOnIndicesCountChangeListener.onIndicesCountChange("" + mFeatureCount);
            }
        }
    }


    public void onPause()
    {
        // TODO: ngsOnPause()
//        Api.ngsOnPause();
    }


    public void onResume()
    {
        // TODO: onResume()
    }


    public void setOnRequestRenderListener(OnRequestRenderListener listener)
    {
        mOnRequestRenderListener = listener;
    }


    public interface OnRequestRenderListener
    {
        void onRequestRender();
    }


    public void setOnDrawTimeChangeListener(OnDrawTimeChangeListener listener)
    {
        mOnDrawTimeChangeListener = listener;
    }


    public interface OnDrawTimeChangeListener
    {
        void onDrawTimeChange(String drawTime);
    }


    public void setOnIndicesCountChangeListener(OnIndicesCountChangeListener listener)
    {
        mOnIndicesCountChangeListener = listener;
    }


    public interface OnIndicesCountChangeListener
    {
        void onIndicesCountChange(String indicesCount);
    }
}
