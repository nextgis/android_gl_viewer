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

package com.nextgis.glviewer;

import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.nextgis.dialog.LocalResourceNativeSelectDialog;
import com.nextgis.store.map.MapDrawing;
import com.nextgis.store.map.MapGlView;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;


public class MapFragment
        extends Fragment
        implements MapDrawing.OnDrawTimeChangeListener,
                   MapDrawing.OnFeatureCountChangeListener,
                   LocalResourceNativeSelectDialog.OnSelectionListener
{
    protected static final String WAIT_STRING = "*****";

    protected MainActivity   mActivity;
    protected MapGlView      mMapGlView;
    protected RelativeLayout mMapRelativeLayout;
    protected TextView       mDrawTimeView;
    protected TextView       mFeatureCountView;

    protected DecimalFormat mTimeFormat;
    protected DecimalFormat mCountFormat;


    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        mActivity = (MainActivity) getActivity();

        mMapGlView = new MapGlView(mActivity);
        mMapGlView.setId(R.id.gl_map_view);

        NumberFormat f = NumberFormat.getInstance(Locale.getDefault());
        mTimeFormat = null;
        if (f instanceof DecimalFormat) {
            mTimeFormat = (DecimalFormat) f;
            mTimeFormat.setGroupingUsed(true);
            mTimeFormat.setDecimalSeparatorAlwaysShown(true);
            mTimeFormat.setMinimumFractionDigits(3);
            mTimeFormat.setMaximumFractionDigits(3);
        }

        f = NumberFormat.getInstance(Locale.getDefault());
        mCountFormat = null;
        if (f instanceof DecimalFormat) {
            mCountFormat = (DecimalFormat) f;
            mCountFormat.setGroupingUsed(true);
            mCountFormat.setDecimalSeparatorAlwaysShown(false);
        }
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
        if (null == drawTime) {
            mDrawTimeView.setText(" " + WAIT_STRING + " ");
            return;
        }

        float time = drawTime / 1000.0f;
        String timeString = null != mTimeFormat ? mTimeFormat.format(time) : "" + time;
        mDrawTimeView.setText(" " + timeString + " ");
    }


    @Override
    public void onFeatureCountChange(Integer featureCount)
    {
        if (null == featureCount) {
            mFeatureCountView.setText(" " + WAIT_STRING + " ");
            return;
        }

        String countString = null != mCountFormat ? mCountFormat.format(featureCount) : "" + featureCount;
        mFeatureCountView.setText(" " + countString + " ");
    }


    @Override
    public void onSelection(String path)
    {
        mMapGlView.loadFile(path);
    }
}
