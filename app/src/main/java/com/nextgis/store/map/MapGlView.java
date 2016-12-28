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

package com.nextgis.store.map;

import android.content.Context;
import android.graphics.PointF;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.widget.Scroller;
import android.widget.Toast;
import com.nextgis.glviewer.Constants;
import com.nextgis.glviewer.MainActivity;
import com.nextgis.glviewer.MainApplication;
import com.nextgis.store.bindings.DrawState;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGL11;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.egl.EGLSurface;
import javax.microedition.khronos.opengles.GL10;
import java.io.IOException;


public class MapGlView
        extends GLSurfaceView
        implements MapDrawing.OnRequestRenderListener,
                   GestureDetector.OnGestureListener,
                   GestureDetector.OnDoubleTapListener,
                   ScaleGestureDetector.OnScaleGestureListener
{
    // EGL14.EGL_CONTEXT_CLIENT_VERSION
    protected static final int EGL_CONTEXT_CLIENT_VERSION = 0x3098;
    protected static final int EGL_OPENGL_ES2_BIT         = 4;

    protected static int[] glConfigAttribs = {
            EGL10.EGL_RENDERABLE_TYPE,
            EGL_OPENGL_ES2_BIT,
            EGL10.EGL_RED_SIZE,
            8,
            EGL10.EGL_GREEN_SIZE,
            8,
            EGL10.EGL_BLUE_SIZE,
            8,
            EGL10.EGL_NONE};

    protected static final boolean DEBUG = false;

    protected static final int DRAW_STATE_none           = 0;
    protected static final int DRAW_STATE_drawing_normal = 1;
    protected static final int DRAW_STATE_panning        = 3;
    protected static final int DRAW_STATE_panning_fling  = 4;
    protected static final int DRAW_STATE_zooming        = 5;

    protected MainApplication mApp;

    protected EGL10 mEgl;

    protected EGLDisplay mEglDisplay;
    protected EGLSurface mEglSurface;
    protected EGLContext mEglContext;

    protected MapDrawing mMapDrawing;

    protected GestureDetector      mGestureDetector;
    protected ScaleGestureDetector mScaleGestureDetector;
    protected Scroller             mScroller;

    protected PointF mStartDragLocation;
    protected PointF mCurrentDragOffset;
    protected PointF mCurrentFocusLocation;
    //protected PointF mCurrentFocusOffset;

    protected int    mDrawingState;
    protected double mScaleFactor;
    protected double mCurrentSpan;


    public MapGlView(Context context)
    {
        super(context);
        init();
    }


    public MapGlView(
            Context context,
            AttributeSet attrs)
    {
        super(context, attrs);
        init();
    }


    protected void init()
    {
        Context context = getContext();
        mApp = (MainApplication) ((MainActivity) getContext()).getApplication();

        mGestureDetector = new GestureDetector(context, this);
        mScaleGestureDetector = new ScaleGestureDetector(context, this);
        mScroller = new Scroller(context);

        mStartDragLocation = new PointF();
        mCurrentDragOffset = new PointF();
        mCurrentFocusLocation = new PointF();
//        mCurrentFocusOffset = new PointF();

        mDrawingState = DRAW_STATE_none;

        setEGLConfigChooser(new ConfigChooser(8, 8, 8, 0, 0, 0));
        setEGLContextFactory(new ContextFactory());
        setEGLWindowSurfaceFactory(new SurfaceFactory());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            setPreserveEGLContextOnPause(true); // TODO: may be preserved, none guaranty, see docs
        }

        if (DEBUG) {
            setDebugFlags(GLSurfaceView.DEBUG_CHECK_GL_ERROR | GLSurfaceView.DEBUG_LOG_GL_CALLS);
        }

        setRenderer(new MapRenderer());
        setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY); // update with requestRender()

        mMapDrawing = new MapDrawing(mApp.getMapPath());

        if (!mMapDrawing.openMap()) {
            mMapDrawing.createMap();
//            loadFile(null); // for debug
        }
    }


    public void loadFile(String path)
    {
        try {
            mMapDrawing.loadMap(path);
        } catch (IOException e) {
            Toast.makeText(mApp, e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
        }
    }


    @Override
    public void onPause()
    {
        mMapDrawing.setOnRequestRenderListener(null);
        mMapDrawing.onPause();
        super.onPause();
    }


    @Override
    public void onResume()
    {
        super.onResume();
        mMapDrawing.onResume();
        mMapDrawing.setOnRequestRenderListener(this);
    }


    public MapDrawing getMapDrawing()
    {
        return mMapDrawing;
    }


    protected void checkEglError(
            EGL10 egl,
            String prompt)
    {
        int error;
        while ((error = egl.eglGetError()) != EGL10.EGL_SUCCESS) {
            Log.e(Constants.TAG, String.format("%s: EGL error: %s", prompt, getErrorString(error)));
        }
    }


    public static void throwEglException(
            EGL10 egl,
            String function)
    {
        int error;
        StringBuilder sb = new StringBuilder();
        while ((error = egl.eglGetError()) != EGL10.EGL_SUCCESS) {
            sb.append(getErrorString(error)).append("\n");
        }

        String message = function + " failed";
        if (sb.length() > 0) {
            message += ": " + sb.toString();
        }

        Log.e(Constants.TAG, message);
        throw new RuntimeException(message);
    }


    public static String formatEglError(
            String function,
            int error)
    {
        return function + " failed: " + getErrorString(error);
    }


    public static String getErrorString(int error)
    {
        switch (error) {
            case EGL10.EGL_SUCCESS:
                return "EGL_SUCCESS";
            case EGL10.EGL_NOT_INITIALIZED:
                return "EGL_NOT_INITIALIZED";
            case EGL10.EGL_BAD_ACCESS:
                return "EGL_BAD_ACCESS";
            case EGL10.EGL_BAD_ALLOC:
                return "EGL_BAD_ALLOC";
            case EGL10.EGL_BAD_ATTRIBUTE:
                return "EGL_BAD_ATTRIBUTE";
            case EGL10.EGL_BAD_CONFIG:
                return "EGL_BAD_CONFIG";
            case EGL10.EGL_BAD_CONTEXT:
                return "EGL_BAD_CONTEXT";
            case EGL10.EGL_BAD_CURRENT_SURFACE:
                return "EGL_BAD_CURRENT_SURFACE";
            case EGL10.EGL_BAD_DISPLAY:
                return "EGL_BAD_DISPLAY";
            case EGL10.EGL_BAD_MATCH:
                return "EGL_BAD_MATCH";
            case EGL10.EGL_BAD_NATIVE_PIXMAP:
                return "EGL_BAD_NATIVE_PIXMAP";
            case EGL10.EGL_BAD_NATIVE_WINDOW:
                return "EGL_BAD_NATIVE_WINDOW";
            case EGL10.EGL_BAD_PARAMETER:
                return "EGL_BAD_PARAMETER";
            case EGL10.EGL_BAD_SURFACE:
                return "EGL_BAD_SURFACE";
            case EGL11.EGL_CONTEXT_LOST:
                return "EGL_CONTEXT_LOST";
            default:
                return getHex(error);
        }
    }


    private static String getHex(int value)
    {
        return "0x" + Integer.toHexString(value);
    }


    protected class ConfigChooser
            implements GLSurfaceView.EGLConfigChooser
    {
        // Subclasses can adjust these values:
        protected int mRedSize;
        protected int mGreenSize;
        protected int mBlueSize;
        protected int mAlphaSize;
        protected int mDepthSize;
        protected int mStencilSize;
        protected int[] mValue = new int[1];


        public ConfigChooser(
                int r,
                int g,
                int b,
                int a,
                int depth,
                int stencil)
        {
            mRedSize = r;
            mGreenSize = g;
            mBlueSize = b;
            mAlphaSize = a;
            mDepthSize = depth;
            mStencilSize = stencil;
        }


        @Override
        public EGLConfig chooseConfig(
                EGL10 egl,
                EGLDisplay display)
        {
            mEgl = egl;
            mEglDisplay = display;

            // Get the number of minimally matching EGL configurations
            int[] num_config = new int[1];
            egl.eglChooseConfig(display, glConfigAttribs, null, 0, num_config);

            int numConfigs = num_config[0];

            if (numConfigs <= 0) {
                throw new IllegalArgumentException("No configs match configSpec");
            }

            // Allocate then read the array of minimally matching EGL configs
            EGLConfig[] configs = new EGLConfig[numConfigs];
            egl.eglChooseConfig(display, glConfigAttribs, configs, numConfigs, num_config);

            if (DEBUG) {
                printConfigs(egl, display, configs);
            }

            // Now return the "best" one
            EGLConfig config = chooseConfig(egl, display, configs);
            if (config == null) {
                throw new IllegalArgumentException("No config chosen");
            }
            return config;
        }


        public EGLConfig chooseConfig(
                EGL10 egl,
                EGLDisplay display,
                EGLConfig[] configs)
        {
            for (EGLConfig config : configs) {
                int d = findConfigAttrib(egl, display, config, EGL10.EGL_DEPTH_SIZE, 0);
                int s = findConfigAttrib(egl, display, config, EGL10.EGL_STENCIL_SIZE, 0);

                // We need at least mDepthSize and mStencilSize bits
                if (d < mDepthSize || s < mStencilSize) { continue; }

                // We want an *exact* match for red/green/blue/alpha
                int r = findConfigAttrib(egl, display, config, EGL10.EGL_RED_SIZE, 0);
                int g = findConfigAttrib(egl, display, config, EGL10.EGL_GREEN_SIZE, 0);
                int b = findConfigAttrib(egl, display, config, EGL10.EGL_BLUE_SIZE, 0);
                int a = findConfigAttrib(egl, display, config, EGL10.EGL_ALPHA_SIZE, 0);

                if (r == mRedSize && g == mGreenSize && b == mBlueSize && a == mAlphaSize) {
                    return config;
                }
            }
            return null;
        }


        protected int findConfigAttrib(
                EGL10 egl,
                EGLDisplay display,
                EGLConfig config,
                int attribute,
                int defaultValue)
        {

            if (egl.eglGetConfigAttrib(display, config, attribute, mValue)) {
                return mValue[0];
            }
            return defaultValue;
        }


        protected void printConfigs(
                EGL10 egl,
                EGLDisplay display,
                EGLConfig[] configs)
        {
            int numConfigs = configs.length;
            Log.w(Constants.TAG, String.format("%d configurations", numConfigs));
            for (int i = 0; i < numConfigs; i++) {
                Log.w(Constants.TAG, String.format("Configuration %d:\n", i));
                printConfig(egl, display, configs[i]);
            }
        }


        protected void printConfig(
                EGL10 egl,
                EGLDisplay display,
                EGLConfig config)
        {
            int[] attributes = {
                    EGL10.EGL_BUFFER_SIZE,
                    EGL10.EGL_ALPHA_SIZE,
                    EGL10.EGL_BLUE_SIZE,
                    EGL10.EGL_GREEN_SIZE,
                    EGL10.EGL_RED_SIZE,
                    EGL10.EGL_DEPTH_SIZE,
                    EGL10.EGL_STENCIL_SIZE,
                    EGL10.EGL_CONFIG_CAVEAT,
                    EGL10.EGL_CONFIG_ID,
                    EGL10.EGL_LEVEL,
                    EGL10.EGL_MAX_PBUFFER_HEIGHT,
                    EGL10.EGL_MAX_PBUFFER_PIXELS,
                    EGL10.EGL_MAX_PBUFFER_WIDTH,
                    EGL10.EGL_NATIVE_RENDERABLE,
                    EGL10.EGL_NATIVE_VISUAL_ID,
                    EGL10.EGL_NATIVE_VISUAL_TYPE,
                    // EGL10.EGL_PRESERVED_RESOURCES,
                    0x3030,
                    EGL10.EGL_SAMPLES,
                    EGL10.EGL_SAMPLE_BUFFERS,
                    EGL10.EGL_SURFACE_TYPE,
                    EGL10.EGL_TRANSPARENT_TYPE,
                    EGL10.EGL_TRANSPARENT_RED_VALUE,
                    EGL10.EGL_TRANSPARENT_GREEN_VALUE,
                    EGL10.EGL_TRANSPARENT_BLUE_VALUE,
                    // EGL10.EGL_BIND_TO_TEXTURE_RGB,
                    0x3039,
                    // EGL10.EGL_BIND_TO_TEXTURE_RGBA,
                    0x303A,
                    // EGL10.EGL_MIN_SWAP_INTERVAL,
                    0x303B,
                    // EGL10.EGL_MAX_SWAP_INTERVAL,
                    0x303C,
                    EGL10.EGL_LUMINANCE_SIZE,
                    EGL10.EGL_ALPHA_MASK_SIZE,
                    EGL10.EGL_COLOR_BUFFER_TYPE,
                    EGL10.EGL_RENDERABLE_TYPE,
                    // EGL10.EGL_CONFORMANT
                    0x3042};
            String[] names = {
                    "EGL_BUFFER_SIZE",
                    "EGL_ALPHA_SIZE",
                    "EGL_BLUE_SIZE",
                    "EGL_GREEN_SIZE",
                    "EGL_RED_SIZE",
                    "EGL_DEPTH_SIZE",
                    "EGL_STENCIL_SIZE",
                    "EGL_CONFIG_CAVEAT",
                    "EGL_CONFIG_ID",
                    "EGL_LEVEL",
                    "EGL_MAX_PBUFFER_HEIGHT",
                    "EGL_MAX_PBUFFER_PIXELS",
                    "EGL_MAX_PBUFFER_WIDTH",
                    "EGL_NATIVE_RENDERABLE",
                    "EGL_NATIVE_VISUAL_ID",
                    "EGL_NATIVE_VISUAL_TYPE",
                    "EGL_PRESERVED_RESOURCES",
                    "EGL_SAMPLES",
                    "EGL_SAMPLE_BUFFERS",
                    "EGL_SURFACE_TYPE",
                    "EGL_TRANSPARENT_TYPE",
                    "EGL_TRANSPARENT_RED_VALUE",
                    "EGL_TRANSPARENT_GREEN_VALUE",
                    "EGL_TRANSPARENT_BLUE_VALUE",
                    "EGL_BIND_TO_TEXTURE_RGB",
                    "EGL_BIND_TO_TEXTURE_RGBA",
                    "EGL_MIN_SWAP_INTERVAL",
                    "EGL_MAX_SWAP_INTERVAL",
                    "EGL_LUMINANCE_SIZE",
                    "EGL_ALPHA_MASK_SIZE",
                    "EGL_COLOR_BUFFER_TYPE",
                    "EGL_RENDERABLE_TYPE",
                    "EGL_CONFORMANT"};
            int[] value = new int[1];
            for (int i = 0; i < attributes.length; i++) {
                int attribute = attributes[i];
                String name = names[i];
                if (egl.eglGetConfigAttrib(display, config, attribute, value)) {
                    Log.w(Constants.TAG, String.format("  %s: %d\n", name, value[0]));
                } else {
                    // Log.w(TAG, String.format("  %s: failed\n", name));
                    while (egl.eglGetError() != EGL10.EGL_SUCCESS) { ; }
                }
            }
        }
    }


    protected class ContextFactory
            implements GLSurfaceView.EGLContextFactory
    {
        @Override
        public EGLContext createContext(
                EGL10 egl,
                EGLDisplay display,
                EGLConfig eglConfig)
        {
            Log.w(Constants.TAG, "creating OpenGL ES 2.0 context");
            checkEglError(egl, "Before eglCreateContext");
            int[] attrib_list = {EGL_CONTEXT_CLIENT_VERSION, 2, EGL10.EGL_NONE};
            mEglContext =
                    egl.eglCreateContext(display, eglConfig, EGL10.EGL_NO_CONTEXT, attrib_list);
            if (EGL10.EGL_NO_CONTEXT == mEglContext) {
                throwEglException(egl, "eglCreateContext");
            }
            return mEglContext;
        }


        @Override
        public void destroyContext(
                EGL10 egl,
                EGLDisplay display,
                EGLContext context)
        {
            if (!egl.eglDestroyContext(display, context)) {
                throwEglException(egl, "eglDestroyContex");
            }
            mEglContext = null;
        }
    }


    protected class SurfaceFactory
            implements GLSurfaceView.EGLWindowSurfaceFactory
    {

        public EGLSurface createWindowSurface(
                EGL10 egl,
                EGLDisplay display,
                EGLConfig config,
                Object nativeWindow)
        {
            mEglSurface = null;
            try {
                mEglSurface = egl.eglCreateWindowSurface(display, config, nativeWindow, null);
                if (EGL10.EGL_NO_SURFACE == mEglSurface) {
                    throwEglException(egl, "eglCreateWindowSurface");
                }
            } catch (IllegalArgumentException e) {
                Log.e(Constants.TAG, "eglCreateWindowSurface", e);
                throw new RuntimeException("eglCreateWindowSurface", e);
            }
            return mEglSurface;
        }


        public void destroySurface(
                EGL10 egl,
                EGLDisplay display,
                EGLSurface surface)
        {
//            mMapDrawing.releaseCurrent();
            if (!egl.eglDestroySurface(display, surface)) {
                throwEglException(egl, "eglDestroySurface");
            }
            mEglSurface = null;
        }
    }


    protected class MapRenderer
            implements GLSurfaceView.Renderer
    {
        @Override
        public void onSurfaceCreated(
                GL10 gl,
                EGLConfig config)
        {
//            mMapDrawing.openMap();
            mMapDrawing.initDrawContext(mEgl, mEglDisplay, mEglSurface, mEglContext);
        }


        @Override
        public void onSurfaceChanged(
                GL10 gl,
                int width,
                int height)
        {
            mMapDrawing.setSize(width, height);
        }


        @Override
        public void onDrawFrame(GL10 gl)
        {
            //Log.d(Constants.TAG, "+++ onDrawFrame");
            mMapDrawing.draw();
        }
    }


    @Override
    public void requestRender()
    {
        throw new RuntimeException("Don't use requestRender(), use requestRender(drawState)");
    }


    public void requestRender(int drawState)
    {
        //Log.d(Constants.TAG, "+++ requestRender, drawState: " + drawState);
        mMapDrawing.setDrawState(drawState);
        super.requestRender();
    }


    @Override
    public void onRequestRender()
    {
        //Log.d(Constants.TAG, "+++ onRequestRender, mDrawingState: " + mDrawingState);
        super.requestRender();
    }


    public void panStart(final MotionEvent e)
    {
        if (mDrawingState == DRAW_STATE_panning) {
            return;
        }

        mDrawingState = DRAW_STATE_panning;
        mMapDrawing.onDrawStart();

        mStartDragLocation.set(e.getX(), e.getY());
        mCurrentDragOffset.set(0, 0);
    }


    public void panMoveTo(
            final MotionEvent e,
            float distanceX,
            float distanceY)
    {
        if (mDrawingState == DRAW_STATE_panning) {
            float x = mStartDragLocation.x - e.getX();
            float y = mStartDragLocation.y - e.getY();

            mCurrentDragOffset.set(x, y);

            mMapDrawing.offset(distanceX, distanceY);
            requestRender(DrawState.DS_PRESERVED);
        }
    }


    public void panStop()
    {
        if (mDrawingState == DRAW_STATE_panning) {
            requestRender(DrawState.DS_NORMAL);
            mDrawingState = DRAW_STATE_drawing_normal;
        }
    }


    public void zoomStart(ScaleGestureDetector scaleGestureDetector)
    {
        if (mDrawingState == DRAW_STATE_zooming) {
            return;
        }

        mDrawingState = DRAW_STATE_zooming;
        mMapDrawing.onDrawStart();

        mCurrentSpan = scaleGestureDetector.getCurrentSpan();
        mCurrentFocusLocation.set(
                scaleGestureDetector.getFocusX(), scaleGestureDetector.getFocusY());
//        mCurrentFocusOffset.set(0, 0);
        mScaleFactor = 1.0;
    }


    public void zoom(ScaleGestureDetector scaleGestureDetector)
    {
        if (mDrawingState != DRAW_STATE_zooming) {
            zoomStart(scaleGestureDetector);
        }

        if (mDrawingState == DRAW_STATE_zooming) {
//            mScaleFactor =
//                    scaleGestureDetector.getScaleFactor() * scaleGestureDetector.getCurrentSpan()
//                            / mCurrentSpan;
            mScaleFactor = scaleGestureDetector.getScaleFactor();

//            double invertScale = 1 / mScaleFactor;
//            double offX = (1 - invertScale) * (mCurrentFocusLocation.x);
//            double offY = (1 - invertScale) * (mCurrentFocusLocation.y);
//            mCurrentFocusOffset.set((float) offX, (float) offY);

            mMapDrawing.scale(mScaleFactor, mCurrentFocusLocation.x, mCurrentFocusLocation.y);
            requestRender(DrawState.DS_PRESERVED);
        }
    }


    public void zoomStop()
    {
        if (mDrawingState == DRAW_STATE_zooming) {
            requestRender(DrawState.DS_NORMAL);
            mDrawingState = DRAW_STATE_drawing_normal;
        }
    }


    @Override
    public boolean onDoubleTap(MotionEvent e)
    {
        //Log.d(Constants.TAG, "=== onDoubleTap, true, mDrawingState: " + mDrawingState);

        //mDrawingState = DRAW_STATE_zooming;
        mMapDrawing.onDrawStart();
        mCurrentFocusLocation.set(e.getX(), e.getY());
        mScaleFactor = 2.0;

//        double invertScale = 1 / mScaleFactor;
//        double offX = (1 - invertScale) * (mCurrentFocusLocation.x);
//        double offY = (1 - invertScale) * (mCurrentFocusLocation.y);
//        mCurrentFocusOffset.set((float) offX, (float) offY);

        mMapDrawing.scale(mScaleFactor, mCurrentFocusLocation.x, mCurrentFocusLocation.y);

        requestRender(DrawState.DS_NORMAL);
        mDrawingState = DRAW_STATE_drawing_normal;
        return true;
    }


    // delegate the event to the gesture detector
    @Override
    public boolean onTouchEvent(MotionEvent event)
    {
        mScaleGestureDetector.onTouchEvent(event);

        if (!mGestureDetector.onTouchEvent(event)) {
            // TODO: get action can be more complicated:
            // TODO: if ((event.getAction() & MotionEvent.ACTION_MASK) == MotionEvent.ACTION_DOWN)

            int action = event.getAction();

            switch (action) {
                case MotionEvent.ACTION_DOWN:
                    //Log.d(Constants.TAG,
                    //      "=== onTouchEvent, ACTION_DOWN, false, mDrawingState:" + mDrawingState);
                    if (!mScroller.isFinished()) {
                        //Log.d(Constants.TAG,
                        //      "=== onTouchEvent, ACTION_DOWN, false, mDrawingState:" + mDrawingState + "!mScroller.isFinished()");
                        mScroller.forceFinished(true);
                    }
                    return false;

                case MotionEvent.ACTION_MOVE:
                    //Log.d(Constants.TAG,
                    //      "=== onTouchEvent, ACTION_MOVE, false, mDrawingState:" + mDrawingState);
                    return false;

                case MotionEvent.ACTION_UP:
                    //Log.d(Constants.TAG, "onTouchEvent, ACTION_UP, true, mDrawingState:" + mDrawingState);
                    panStop();
                    return true;

                default:
                    if ((action & MotionEvent.ACTION_MASK) == MotionEvent.ACTION_UP) {
                        //Log.d(
                        //        Constants.TAG,
                        //        "=== onTouchEvent, ACTION_MASK & ACTION_UP, true, mDrawingState:"
                        //                + mDrawingState);
                        panStop();
                        return true;
                    }
                    //Log.d(Constants.TAG,
                    //      "=== onTouchEvent, default, false, action: " + action + ", mDrawingState:"
                    //              + mDrawingState);
                    return false;
            }
        }

        //Log.d(
        //        Constants.TAG,
        //        "=== onTouchEvent, mGestureDetector.onTouchEvent(), true, mDrawingState:"
        //                + mDrawingState);
        return true;
    }


    @Override
    public boolean onScroll(
            MotionEvent event1,
            MotionEvent event2,
            float distanceX,
            float distanceY)
    {
        if (event2.getPointerCount() > 1) {
            //Log.d(Constants.TAG, "=== onScroll, false, mDrawingState: " + mDrawingState);
            return false;
        }

        //Log.d(Constants.TAG, "=== onScroll, true, mDrawingState: " + mDrawingState);
        panStart(event1);
        panMoveTo(event2, distanceX, distanceY);
        return true;
    }


    @Override
    public boolean onFling(
            MotionEvent e1,
            MotionEvent e2,
            float velocityX,
            float velocityY)
    {
        if (mDrawingState == DRAW_STATE_zooming) {
            //Log.d(Constants.TAG, "=== onFling, false, mDrawingState: " + mDrawingState);
            return false;
        }
        //Log.d(Constants.TAG, "=== onFling, true, mDrawingState: " + mDrawingState);

        //mDrawingState = DRAW_STATE_panning_fling;
        float x = mCurrentDragOffset.x;
        float y = mCurrentDragOffset.y;

        mScroller.forceFinished(true);
        mScroller.fling((int) x, (int) y, (int) -velocityX, (int) -velocityY, 0,
                        mMapDrawing.getWidth(), 0, mMapDrawing.getHeight());

        mDrawingState = DRAW_STATE_panning;
        panStop();
        return true;
    }


// !!! the method is never called by the user's actions
//@Override
//public void computeScroll()
//{
//    super.computeScroll();
//    if (mDrawingState == DRAW_STATE_panning_fling) {
//        if (mScroller.computeScrollOffset()) {
//            if (mScroller.isFinished()) {
//                //mDrawingState = DRAW_STATE_panning; // !!! must not be here
//                panStop();
//            } else {
//                float x = mScroller.getCurrX();
//                float y = mScroller.getCurrY();
//                mCurrentDragOffset.set(x, y);
//            }
//        } else if (mScroller.isFinished()) {
//            //mDrawingState = DRAW_STATE_panning; // !!! must not be here
//            panStop();
//        }
//    }
//}


    @Override
    public boolean onScaleBegin(ScaleGestureDetector detector)
    {
        //Log.d(Constants.TAG, "=== onScaleBegin, true, mDrawingState:" + mDrawingState);
        zoomStart(detector);
        return true;
    }


    @Override
    public boolean onScale(ScaleGestureDetector detector)
    {
        //Log.d(Constants.TAG, "=== onScale, true, mDrawingState:" + mDrawingState);
        zoom(detector);
        return true;
    }


    @Override
    public void onScaleEnd(ScaleGestureDetector detector)
    {
        //Log.d(Constants.TAG, "=== onScaleEnd, mDrawingState:" + mDrawingState);
        zoomStop();
    }


    @Override
    public boolean onDown(MotionEvent e)
    {
        //Log.d(Constants.TAG, "=== onDown, true, mDrawingState:" + mDrawingState);
        return true;
    }


    @Override
    public void onShowPress(MotionEvent e)
    {
        //Log.d(Constants.TAG, "=== onShowPress, mDrawingState:" + mDrawingState);
    }


    @Override
    public boolean onSingleTapUp(MotionEvent e)
    {
        //Log.d(Constants.TAG, "=== onSingleTapUp, false, mDrawingState:" + mDrawingState);
        return false;
    }


    @Override
    public void onLongPress(MotionEvent e)
    {
        //Log.d(Constants.TAG, "=== onLongPress, mDrawingState:" + mDrawingState);
    }


    @Override
    public boolean onSingleTapConfirmed(MotionEvent e)
    {
        //Log.d(Constants.TAG, "=== onSingleTapConfirmed, false, mDrawingState:" + mDrawingState);
        return false;
    }


    @Override
    public boolean onDoubleTapEvent(MotionEvent e)
    {
        //Log.d(Constants.TAG, "=== onDoubleTapEvent, false, mDrawingState:" + mDrawingState);
        return false;
    }
}
