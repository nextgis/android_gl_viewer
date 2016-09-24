package com.nextgis.glviewer;

import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;


public class MapFragment
        extends Fragment
{
    protected MainActivity   mActivity;
    protected GlMapView      mMapView;
    protected RelativeLayout mMapRelativeLayout;


    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        mActivity = (MainActivity) getActivity();

        mMapView = new GlMapView(mActivity);
        mMapView.setId(R.id.gl_map_view);
    }


    @Override
    public View onCreateView(
            LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState)
    {
        View view = inflater.inflate(R.layout.fragment_map, container, false);

        mMapRelativeLayout = (RelativeLayout) view.findViewById(R.id.rl_map);

        if (mMapRelativeLayout != null) {
            mMapRelativeLayout.addView(mMapView, 0, new RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.MATCH_PARENT,
                    RelativeLayout.LayoutParams.MATCH_PARENT));
        }
        mMapView.invalidate();

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
        if (mMapView != null) {
            if (mMapRelativeLayout != null) {
                mMapRelativeLayout.removeView(mMapView);
            }
        }

        super.onDestroyView();
    }


    @Override
    public void onPause()
    {
        mMapView.onPause();
        super.onPause();
    }


    @Override
    public void onResume()
    {
        super.onResume();
        mMapView.onResume();
    }
}
