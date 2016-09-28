package com.nextgis.store.map;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.util.Log;
import com.nextgis.glviewer.Constants;
import com.nextgis.glviewer.SettingConstants;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.egl.EGLSurface;
import javax.microedition.khronos.opengles.GL10;
import java.io.File;


public class MapGlView
        extends GLSurfaceView
        implements MapDrawing.OnMapDrawListener,
                   MapDrawing.OnRequestMapDrawListener
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

    protected EGL10     mEgl;
    protected EGLConfig mEglConfig;

    protected EGLDisplay mEglDisplay;
    protected EGLSurface mEglSurface;
    protected EGLContext mEglContext;

    protected MapDrawing mMapDrawing;

    protected long mDrawTime;


    // TODO: to MainApplication
    protected String getMapPath()
    {
        Context context = getContext();
        File defaultPath = getContext().getExternalFilesDir(SettingConstants.KEY_PREF_MAP);
        if (defaultPath == null) {
            defaultPath = new File(context.getFilesDir(), SettingConstants.KEY_PREF_MAP);
        }
        return defaultPath.getPath();
    }


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
        setEGLConfigChooser(new ConfigChooser(8, 8, 8, 0, 0, 0));
        setEGLContextFactory(new ContextFactory());
        setEGLWindowSurfaceFactory(new SurfaceFactory());

        setRenderer(new MapRenderer());
        setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY); // update with requestRender()

        mMapDrawing = new MapDrawing(getMapPath());
        mMapDrawing.setOnMapDrawListener(this);
        mMapDrawing.setOnRequestMapDrawListener(this);

        mMapDrawing.createMap();
//        mMapDrawing.loadMap();
        mMapDrawing.openMap();
    }


    protected void checkEglError(
            String prompt,
            EGL10 egl)
    {
        int error;
        while ((error = egl.eglGetError()) != EGL10.EGL_SUCCESS) {
            Log.e(Constants.TAG, String.format("%s: EGL error: 0x%x", prompt, error));
        }
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
            mEglConfig = chooseConfig(egl, display, configs);
            return mEglConfig;
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
            checkEglError("Before eglCreateContext", egl);
            int[] attrib_list = {EGL_CONTEXT_CLIENT_VERSION, 2, EGL10.EGL_NONE};
            mEglContext =
                    egl.eglCreateContext(display, eglConfig, EGL10.EGL_NO_CONTEXT, attrib_list);
            checkEglError("After eglCreateContext", egl);
            return mEglContext;
        }


        @Override
        public void destroyContext(
                EGL10 egl,
                EGLDisplay display,
                EGLContext context)
        {
            egl.eglDestroyContext(display, context);
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
            } catch (IllegalArgumentException e) {
                Log.e(Constants.TAG, "eglCreateWindowSurface", e);
            }
            return mEglSurface;
        }


        public void destroySurface(
                EGL10 egl,
                EGLDisplay display,
                EGLSurface surface)
        {
            egl.eglDestroySurface(display, surface);
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
            mMapDrawing.draw();
        }
    }


    @Override
    public void onMapDraw()
    {
        requestRender();

        mDrawTime = System.currentTimeMillis() - mDrawTime;
        Log.d(Constants.TAG, "Native map draw time: " + mDrawTime);
    }


    @Override
    public void onRequestMapDraw()
    {
        requestRender();
    }
}
