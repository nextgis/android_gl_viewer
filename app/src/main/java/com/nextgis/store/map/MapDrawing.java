package com.nextgis.store.map;

import android.graphics.PointF;
import com.nextgis.store.bindings.Api;
import com.nextgis.store.bindings.Coordinate;
import com.nextgis.store.bindings.DrawState;
import com.nextgis.store.bindings.ErrorCodes;
import com.nextgis.store.bindings.ProgressCallback;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.egl.EGLSurface;


public class MapDrawing
        extends DataStore
{
    protected EGL10     mEgl;
    protected EGLConfig mEglConfig;

    protected EGLDisplay mEglDisplay = EGL10.EGL_NO_DISPLAY;
    protected EGLSurface mEglSurface = EGL10.EGL_NO_SURFACE;
    protected EGLContext mEglContext = EGL10.EGL_NO_CONTEXT;

    protected int mWidth;
    protected int mHeight;
    protected int mYOrient;

    protected int mDrawState = DrawState.DS_NORMAL;

    protected ProgressCallback         mDrawCallback;
    protected double                   mDrawComplete;
    protected OnMapDrawListener        mOnMapDrawListener;
    protected OnRequestMapDrawListener mOnRequestMapDrawListener;

    protected Coordinate mMapCenter;
    protected PointF     mCenter;


    public MapDrawing(String mapPath)
    {
        super(mapPath);

        mCenter = new PointF();

        mDrawComplete = 0;
        mDrawCallback = createDrawCallback();
    }


    protected void makeCurrent()
    {
        if (null != mEgl && EGL10.EGL_NO_DISPLAY == mEgl.eglGetCurrentDisplay()
                && EGL10.EGL_NO_SURFACE == mEgl.eglGetCurrentSurface(EGL10.EGL_DRAW)
                && EGL10.EGL_NO_SURFACE == mEgl.eglGetCurrentSurface(EGL10.EGL_READ)
                && EGL10.EGL_NO_CONTEXT == mEgl.eglGetCurrentContext()) {
            mEgl.eglMakeCurrent(mEglDisplay, mEglSurface, mEglSurface, mEglContext);
        }
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

        makeCurrent();
        Api.ngsMapInit(mMapId);
    }


    public void initMapProps()
    {
        if (0 == mMapId) { return; }

        Api.ngsMapSetBackgroundColor(mMapId, (short) 0, (short) 255, (short) 0, (short) 255);

//        if (Api.ngsMapSetSize(mMapId, mWidth, mHeight, mYOrient) == ErrorCodes.EC_SUCCESS) {
//            mMapCenter = Api.ngsMapGetCenter(mMapId);
//        }

//        makeCurrent();
//        Api.ngsMapInit(mMapId);
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
            mDrawState = DrawState.DS_REDRAW;
            initMapProps();
            return true;
        }
        return false;
    }


    @Override
    public boolean closeMap()
    {
        if (0 != mMapId) {
            makeCurrent();
            return super.closeMap();
        }
        return false;
    }


    @Override
    protected boolean onLoadFinished(int taskId)
    {
        if (super.onLoadFinished(taskId)) {
            requestDraw(DrawState.DS_NORMAL);
            return true;
        }
        return false;
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

        mDrawState = DrawState.DS_PRESERVED;

        mWidth = width;
        mHeight = height;
        mCenter.set(width / 2, height / 2);

        if (Api.ngsMapSetSize(mMapId, width, height, mYOrient) == ErrorCodes.EC_SUCCESS) {
            mMapCenter = Api.ngsMapGetCenter(mMapId);
        }
        return true;
    }


    public void requestDraw(int drawState)
    {
        mDrawComplete = 0;
        mDrawState = drawState;

        if (null != mOnRequestMapDrawListener) {
            mOnRequestMapDrawListener.onRequestMapDraw();
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

                    if (null != mOnMapDrawListener) {
                        mOnMapDrawListener.onMapDraw();
                    }

//                    MapNativeView.this.postInvalidate();

//                    mDrawTime = System.currentTimeMillis() - mDrawTime;
//                    Log.d(TAG, "Native map draw time: " + mDrawTime);
                }

                return 1;
            }
        };
    }


    public void setOnMapDrawListener(OnMapDrawListener onMapDrawListener)
    {
        mOnMapDrawListener = onMapDrawListener;
    }


    interface OnMapDrawListener
    {
        void onMapDraw();
    }


    public void setOnRequestMapDrawListener(OnRequestMapDrawListener onRequestMapDrawListener)
    {
        mOnRequestMapDrawListener = onRequestMapDrawListener;
    }


    interface OnRequestMapDrawListener
    {
        void onRequestMapDraw();
    }


//
//
//    public void sizedDraw(
//            int width,
//            int height)
//    {
//        if (!setSize(width, height)) {
//            return;
//        }
//
////        Point center = new Point();
////        center.setX(308854.653167);
////        center.setY(4808439.3765);
////        Api.ngsSetMapCenter(mMapId, center);
////        Api.ngsSetMapScale(mMapId, 0.0003);
//
//        requestDraw();
//    }
//
//
//    public void centeredDraw(
//            double centerX,
//            double centerY)
//    {
//        setCenter(centerX, centerY);
//        requestDraw();
//    }
//
//
//    public void scaledDraw(
//            double scaleFactor,
//            double focusLocationX,
//            double focusLocationY)
//    {
//        setScaleByFactor(scaleFactor);
//        setScaledFocusLocation(scaleFactor, focusLocationX, focusLocationY);
//        requestDraw();
//    }
//
//
//    public void setCenter(
//            double x,
//            double y)
//    {
//        RawPoint center = new RawPoint();
//        center.setX(x);
//        center.setY(y);
//        Api.ngsMapSetDisplayCenter(mMapId, center);
//    }
//
//
//    public PointF getCenter()
//    {
//        RawPoint center = new RawPoint();
//        Api.ngsMapGetDisplayCenter(mMapId, center);
//        PointF point = new PointF();
//        point.x = (float) center.getX();
//        point.y = (float) center.getY();
//        return point;
//    }
//
//
//    public void setScaleByFactor(double scaleFactor)
//    {
//        double[] scale = new double[1];
//        Api.ngsMapGetScale(mMapId, scale);
//        scale[0] *= scaleFactor;
//        Api.ngsMapSetScale(mMapId, scale[0]);
//    }
//
//
//    public void setScaledFocusLocation(
//            double scaleFactor,
//            double x,
//            double y)
//    {
//        RawPoint center = new RawPoint();
//        Api.ngsMapGetDisplayCenter(mMapId, center);
//
//        double distX = center.getX() - x;
//        double distY = center.getY() - y;
//        double scaledDistX = distX * scaleFactor;
//        double scaledDistY = distY * scaleFactor;
//        double offX = scaledDistX - distX;
//        double offY = scaledDistY - distY;
//
//        center.setX(center.getX() - offX);
//        center.setY(center.getY() - offY);
//        Api.ngsMapSetDisplayCenter(mMapId, center);
//    }
}
