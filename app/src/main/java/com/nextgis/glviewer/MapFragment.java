package com.nextgis.glviewer;

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
                   MapDrawing.OnFeatureCountChangeListener

{
    protected static final String WAIT_STRING = "*****";

    protected MainActivity   mActivity;
    protected MapGlView      mMapGlView;
    protected RelativeLayout mMapRelativeLayout;
    protected TextView       mDrawTimeView;
    protected TextView       mFeatureCountView;


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
        mFeatureCountView = (TextView) view.findViewById(R.id.feature_count);

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
        mMapGlView.getMapDrawing().setOnFeatureCountChangeListener(null);
        mMapGlView.onPause();
        super.onPause();
    }


    @Override
    public void onResume()
    {
        super.onResume();
        mMapGlView.onResume();
        mMapGlView.getMapDrawing().setOnDrawTimeChangeListener(this);
        mMapGlView.getMapDrawing().setOnFeatureCountChangeListener(this);
    }


    @Override
    public void onDrawTimeChange(Long drawTime)
    {
        mDrawTimeView.setText(" " + (null == drawTime ? WAIT_STRING : drawTime) + " ");
    }


    @Override
    public void onFeatureCountChange(Integer featureCount)
    {
        mFeatureCountView.setText(" " + (null == featureCount ? WAIT_STRING : featureCount) + " ");
    }
}
