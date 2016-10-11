package com.nextgis.glviewer;

import android.os.Handler;
import android.os.Message;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.nextgis.store.map.MapDrawing;
import com.nextgis.store.map.MapGlView;


public class MapFragment
        extends Fragment
        implements MapDrawing.OnDrawTimeChangeListener,
                   MapDrawing.OnIndicesCountChangeListener

{
    protected final static int DRAW_TIME     = 0;
    protected final static int INDEXES_COUNT = 1;

    protected MainActivity   mActivity;
    protected MapGlView      mMapGlView;
    protected RelativeLayout mMapRelativeLayout;
    protected TextView       mDrawTimeView;
    protected TextView       mTrianglesView;

    protected static Handler mHandler;


    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        mActivity = (MainActivity) getActivity();

        mMapGlView = new MapGlView(mActivity);
        mMapGlView.setId(R.id.gl_map_view);
    }


    @Override
    public View onCreateView(
            LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState)
    {
        View view = inflater.inflate(R.layout.fragment_map, container, false);

        mMapRelativeLayout = (RelativeLayout) view.findViewById(R.id.rl_map);
        mDrawTimeView = (TextView) view.findViewById(R.id.draw_time);
        mTrianglesView = (TextView) view.findViewById(R.id.triangles_count);

        if (mMapRelativeLayout != null) {
            mMapRelativeLayout.addView(mMapGlView, 0, new RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.MATCH_PARENT,
                    RelativeLayout.LayoutParams.MATCH_PARENT));
        }
        mMapGlView.invalidate();

        FloatingActionButton fab = (FloatingActionButton) view.findViewById(R.id.reload_map);
        fab.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                // TODO:
            }
        });

        mHandler = new Handler()
        {
            public void handleMessage(Message msg)
            {
                switch (msg.what) {
                    case DRAW_TIME:
                        mDrawTimeView.setText(" " + msg.obj.toString() + " ");
                        break;

                    case INDEXES_COUNT:
                        mTrianglesView.setText(" " + msg.obj.toString() + " ");
                        break;
                }
            }
        };

        return view;
    }


    @Override
    public void onDestroyView()
    {
        if (mMapGlView != null) {
            if (mMapRelativeLayout != null) {
                mMapRelativeLayout.removeView(mMapGlView);
            }
        }

        super.onDestroyView();
    }


    @Override
    public void onPause()
    {
        mMapGlView.getMapDrawing().setOnDrawTimeChangeListener(null);
        mMapGlView.getMapDrawing().setOnIndicesCountChangeListener(null);
        mMapGlView.onPause();
        super.onPause();
    }


    @Override
    public void onResume()
    {
        super.onResume();
        mMapGlView.onResume();
        mMapGlView.getMapDrawing().setOnDrawTimeChangeListener(this);
        mMapGlView.getMapDrawing().setOnIndicesCountChangeListener(this);
    }


    @Override
    public void onDrawTimeChange(String drawTime)
    {
        Message msg = mHandler.obtainMessage(DRAW_TIME, drawTime);
        msg.sendToTarget();
    }


    @Override
    public void onIndicesCountChange(String indicesCount)
    {
        Message msg = mHandler.obtainMessage(INDEXES_COUNT, indicesCount);
        msg.sendToTarget();
    }
}
