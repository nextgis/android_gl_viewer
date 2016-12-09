/*
 * Project:  NextGIS Mobile
 * Purpose:  Mobile GIS for Android.
 * Author:   Dmitry Baryshnikov (aka Bishop), bishop.dev@gmail.com
 * Author:   NikitaFeodonit, nfeodonit@yandex.com
 * Author:   Stanislav Petriakov, becomeglory@gmail.com
 * *****************************************************************************
 * Copyright (c) 2012-2016 NextGIS, info@nextgis.com
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser Public License for more details.
 *
 * You should have received a copy of the GNU Lesser Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.nextgis.libngui.dialog;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;
import com.nextgis.libngui.R;
import com.nextgis.libngui.activity.NGActivity;
import com.nextgis.libngui.adapter.LocalResourceListAdapter;
import com.nextgis.libngui.adapter.LocalResourceListItem;
import com.nextgis.libngui.adapter.LocalResourceListLoader;
import com.nextgis.libngui.adapter.PathAdapter;
import com.nextgis.libngui.adapter.SimpleDividerItemDecoration;

import java.io.File;
import java.util.List;


public class SelectLocalResourceDialog
        extends StyledDialogFragment
        implements LoaderManager.LoaderCallbacks<List<LocalResourceListItem>>
{
    protected final static String KEY_MASK          = "mask";
    protected final static String KEY_CAN_SEL_MULTI = "can_multiselect";
    protected final static String KEY_WRITABLE      = "can_write";
    protected final static String KEY_PATH          = "path";

    protected File    mPath;
    protected int     mTypeMask;
    protected boolean mCanSelectMulti;
    protected boolean mCanWrite;

    protected LocalResourceListAdapter mAdapter;
    protected PathAdapter              mPathAdapter;


    @Override
    public void onSaveInstanceState(Bundle outState)
    {
        super.onSaveInstanceState(outState);
        outState.putSerializable(KEY_PATH, mPath);
        outState.putInt(KEY_MASK, mTypeMask);
        outState.putBoolean(KEY_CAN_SEL_MULTI, mCanSelectMulti);
        outState.putBoolean(KEY_WRITABLE, mCanWrite);
    }


    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        setKeepInstance(true);
        setThemeDark(NGActivity.isDarkTheme(getActivity()));

        super.onCreate(savedInstanceState);

        mAdapter = new LocalResourceListAdapter();
        mAdapter.setSingleSelectable(!mCanSelectMulti);

        mPathAdapter = new PathAdapter();

        runLoader();
    }


    public SelectLocalResourceDialog setTypeMask(int typeMask)
    {
        mTypeMask = typeMask;
        return this;
    }


    public SelectLocalResourceDialog setCanSelectMultiple(boolean can)
    {
        mCanSelectMulti = can;
        return this;
    }


    public SelectLocalResourceDialog setWritable(boolean can)
    {
        mCanWrite = can;
        return this;
    }


    public SelectLocalResourceDialog setPath(File path)
    {
        mPath = path;
        return this;
    }


    @Nullable
    @Override
    public View onCreateView(
            LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState)
    {
        if (null != savedInstanceState) {
            mPath = (File) savedInstanceState.getSerializable(KEY_PATH);
            mTypeMask = savedInstanceState.getInt(KEY_MASK);
            mCanSelectMulti = savedInstanceState.getBoolean(KEY_CAN_SEL_MULTI);
            mCanWrite = savedInstanceState.getBoolean(KEY_WRITABLE);
        }

        View view = inflateThemedLayout(R.layout.dialog_local_resource);

        RecyclerView list = (RecyclerView) view.findViewById(R.id.list);

        list.setLayoutManager(
                new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false));
        list.setHasFixedSize(false);
        list.addItemDecoration(new SimpleDividerItemDecoration(getActivity()));
        list.setAdapter(mAdapter);

        LinearLayout pathLayout = (LinearLayout) view.findViewById(R.id.path_view);
        mPathAdapter.setLinearLayout(pathLayout);
        mPathAdapter.onUpdate(mPath);
        mAdapter.setOnResClickListener(mPathAdapter);


        if (isThemeDark()) {
            setIcon(R.drawable.abc_ic_menu_selectall_mtrl_alpha);
        } else {
            setIcon(R.drawable.abc_ic_menu_selectall_mtrl_alpha);
        }

        setTitle(R.string.select);
        setView(view, false);
        setPositiveText(R.string.select);
        setNegativeText(R.string.cancel);

        setOnPositiveClickedListener(new OnPositiveClickedListener()
        {
            @Override
            public void onPositiveClicked()
            {
                StringBuilder sb = new StringBuilder();
                List<LocalResourceListItem> items = mAdapter.getSelectedItems();
                for (LocalResourceListItem item : items) {
                    sb.append(item.getFile().getAbsolutePath()).append("\n");
                }
                Toast.makeText(mContext, "Selected items:\n" + sb.toString(), Toast.LENGTH_LONG)
                        .show();
            }
        });

        setOnNegativeClickedListener(new OnNegativeClickedListener()
        {
            @Override
            public void onNegativeClicked()
            {
                // just cancel
            }
        });

        return super.onCreateView(inflater, container, savedInstanceState);
    }


    private void runLoader()
    {
        int id = R.id.local_resources_loader;
        Loader loader = getLoaderManager().getLoader(id);
        if (null != loader && loader.isStarted()) {
            getLoaderManager().restartLoader(id, null, this);
        } else {
            getLoaderManager().initLoader(id, null, this);
        }
    }


    @Override
    public Loader<List<LocalResourceListItem>> onCreateLoader(
            int id,
            Bundle args)
    {
        LocalResourceListLoader loader =
                new LocalResourceListLoader(getActivity(), mPath, mPathAdapter);
        loader.setTypeMask(mTypeMask);
        loader.setCanSelectMulti(mCanSelectMulti);
        loader.setCanWrite(mCanWrite);
        return loader;
    }


    @Override
    public void onLoadFinished(
            Loader<List<LocalResourceListItem>> loader,
            List<LocalResourceListItem> resources)
    {
        mPath = mPathAdapter.getPath();
        mAdapter.setResources(resources);
    }


    @Override
    public void onLoaderReset(Loader<List<LocalResourceListItem>> loader)
    {
        mAdapter.setResources(null);
    }
}
