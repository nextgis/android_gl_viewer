package com.nextgis.store.map;

import android.graphics.PointF;
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
    protected EGL10 mEgl;

    protected EGLDisplay mEglDisplay = EGL10.EGL_NO_DISPLAY;
    protected EGLSurface mEglSurface = EGL10.EGL_NO_SURFACE;
    protected EGLContext mEglContext = EGL10.EGL_NO_CONTEXT;

    protected int    mWidth;
    protected int    mHeight;
    protected PointF mCenter;
    protected int mYOrient = 0;

    protected int     mDrawState;
    protected double  mDrawComplete;
    protected long    mDrawTime;
    protected Integer mFeatureCount;

    protected ProgressCallback             mDrawCallback;
    protected OnRequestRenderListener      mOnRequestRenderListener;
    protected OnDrawTimeChangeListener     mOnDrawTimeChangeListener;
    protected OnIndicesCountChangeListener mOnIndicesCountChangeListener;

    protected boolean mNgsDebugMode;


    public MapDrawing(String mapPath)
    {
        super(mapPath);

        setDrawState(DrawState.DS_REDRAW);
        mCenter = new PointF();
        mDrawCallback = createDrawCallback();
        mNgsDebugMode = (Api.ngsGetOptions() & Options.OPT_DEBUGMODE) != 0;
        mDrawTime = 0;
        mFeatureCount = 0;
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
        mDrawState = drawState;
        mDrawComplete = 0;
        mDrawTime = System.currentTimeMillis();
    }


    protected void requestRender()
    {
        if (null != mOnRequestRenderListener) {
            mOnRequestRenderListener.onRequestRender();
        }
    }


    public void draw()
    {
        Api.ngsMapDraw(mMapId, mDrawState, mDrawCallback);
        mDrawState = DrawState.DS_PRESERVED; // draw from cache on display update
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
                if (complete - mDrawComplete > 0.045) { // each 5% redraw
                    mDrawComplete = complete;
                    requestRender();
                }

                if (complete > 0.999) {
                    mDrawTime = System.currentTimeMillis() - mDrawTime;
                    Log.d(Constants.TAG, "Native map draw time: " + mDrawTime);
                    onDrawStop(message);
                }

                return 1;
            }
        };
    }


    public void onDrawStart()
    {
        if (mNgsDebugMode) {
            if (null != mOnDrawTimeChangeListener) {
                mOnDrawTimeChangeListener.onDrawTimeChange(0);
            }

            if (null != mOnIndicesCountChangeListener) {
                mOnIndicesCountChangeListener.onIndicesCountChange("0");
            }
        }
    }


    public void onDrawStop(String indicesCount)
    {
        if (mNgsDebugMode) {
            if (null != mOnDrawTimeChangeListener && mDrawTime < 10000000) {
                mOnDrawTimeChangeListener.onDrawTimeChange((int) mDrawTime);
            }

            if (null != mOnIndicesCountChangeListener) {
                int count = Integer.valueOf(indicesCount);
                if (0 != count) {
                    mFeatureCount = count;
                }
                mOnIndicesCountChangeListener.onIndicesCountChange(mFeatureCount.toString());
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
        void onDrawTimeChange(int drawTime);
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
