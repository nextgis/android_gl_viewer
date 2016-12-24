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

package com.nextgis.dialog;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;
import com.nextgis.adapter.LocalResourceNativeDirectoryContainer;
import com.nextgis.adapter.LocalResourceNativeListAdapter;
import com.nextgis.libngui.R;
import com.nextgis.libngui.activity.NGActivity;
import com.nextgis.libngui.adapter.ListSelectorAdapter;
import com.nextgis.libngui.adapter.SimpleDividerItemDecoration;
import com.nextgis.libngui.dialog.StyledDialogFragment;
import com.nextgis.store.bindings.Api;
import com.nextgis.store.bindings.DirectoryContainer;
import com.nextgis.store.bindings.DirectoryContainerLoadCallback;

import java.util.ArrayList;
import java.util.List;


public class LocalResourceNativeSelectDialog
        extends StyledDialogFragment
        implements LocalResourceNativeListAdapter.OnChangePathListener,
                   ListSelectorAdapter.OnSelectionChangedListener
{
    protected final static int CREATE_PREVIEW_DONE = 0;

    protected final static String KEY_PATH           = "path";
    protected final static String KEY_MASK           = "mask";
    protected final static String KEY_CAN_SEL_MULTI  = "can_select_multi";
    protected final static String KEY_WRITABLE       = "can_write";
    protected final static String KEY_SELECTED_ITEMS = "selected_items";

    protected String  mPath;
    protected int     mItemTypeMask;
    protected boolean mCanSelectMulti;
    protected boolean mCanWrite;

    protected LocalResourceNativeListAdapter mAdapter;
    protected ArrayList<String>              mSavedPathList;
    protected DirectoryContainerLoadCallback mLoadCallback;
    protected OnSelectionListener            mOnSelectionListener;

    private final Object mSrcContainerLock = new Object();


    public LocalResourceNativeSelectDialog setItemTypeMask(int itemTypeMask)
    {
        mItemTypeMask = itemTypeMask;
        return this;
    }


    public LocalResourceNativeSelectDialog setCanSelectMultiple(boolean can)
    {
        mCanSelectMulti = can;
        return this;
    }


    public LocalResourceNativeSelectDialog setWritable(boolean can)
    {
        mCanWrite = can;
        return this;
    }


    public LocalResourceNativeSelectDialog setPath(String path)
    {
        mPath = path;
        return this;
    }


    @Override
    public void onSaveInstanceState(Bundle outState)
    {
        super.onSaveInstanceState(outState);

        LocalResourceNativeDirectoryContainer container = mAdapter.getContainer();
        mSavedPathList = new ArrayList<>(mAdapter.getSelectedItemCount());
        List<Integer> ids = mAdapter.getSelectedItemsIds();
        for (Integer id : ids) {
            mSavedPathList.add(container.getItemPath(id));
        }

        outState.putString(KEY_PATH, mPath);
        outState.putInt(KEY_MASK, mItemTypeMask);
        outState.putBoolean(KEY_CAN_SEL_MULTI, mCanSelectMulti);
        outState.putBoolean(KEY_WRITABLE, mCanWrite);
        outState.putStringArrayList(KEY_SELECTED_ITEMS, mSavedPathList);
    }


    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        setKeepInstance(true);
        setThemeDark(NGActivity.isDarkTheme(getActivity()));

        super.onCreate(savedInstanceState);

        if (null != savedInstanceState) {
            mPath = savedInstanceState.getString(KEY_PATH);
            mItemTypeMask = savedInstanceState.getInt(KEY_MASK);
            mCanSelectMulti = savedInstanceState.getBoolean(KEY_CAN_SEL_MULTI);
            mCanWrite = savedInstanceState.getBoolean(KEY_WRITABLE);
            mSavedPathList = savedInstanceState.getStringArrayList(KEY_SELECTED_ITEMS);
        }

        mAdapter = new LocalResourceNativeListAdapter();
        mAdapter.setSingleSelectable(!mCanSelectMulti);
        mAdapter.setOnChangePathListener(this);
        mAdapter.addOnSelectionChangedListener(this);

        mLoadCallback = createLoadCallback();

        runLoader();
    }


    @Nullable
    @Override
    public View onCreateView(
            LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState)
    {
        View view = inflateThemedLayout(R.layout.dialog_local_resource);

        RecyclerView list = (RecyclerView) view.findViewById(R.id.list);

        list.setLayoutManager(
                new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false));
        list.setHasFixedSize(false);
        list.addItemDecoration(new SimpleDividerItemDecoration(getActivity()));
        list.setAdapter(mAdapter);

        LinearLayout pathLayout = (LinearLayout) view.findViewById(R.id.path_view);
        mAdapter.setPathAdapter(pathLayout, mPath);


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
                LocalResourceNativeDirectoryContainer container = mAdapter.getContainer();
                List<Integer> ids = mAdapter.getSelectedItemsIds();
                for (Integer id : ids) {
                    sb.append(container.getItemPath(id)).append("\n");
                }
                Toast.makeText(mContext, "Selected items:\n" + sb.toString(), Toast.LENGTH_LONG)
                        .show();

                String path = container.getItemPath(ids.get(0));
                if (null != mOnSelectionListener) {
                    mOnSelectionListener.onSelection(path);
                }
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

        View retView = super.onCreateView(inflater, container, savedInstanceState);
        mButtonPositive.setEnabled(false);

        return retView;
    }


    private void runLoader()
    {
        Api.ngsDirectoryContainerLoad(mPath, mLoadCallback);
    }


    DirectoryContainer mSrcContainer;


    protected DirectoryContainerLoadCallback createLoadCallback()
    {
        final Handler handler = new Handler()
        {
            public void handleMessage(Message msg)
            {
                switch (msg.what) {
                    case CREATE_PREVIEW_DONE:
                        synchronized (mSrcContainerLock) {
                            mAdapter.setContainer(mSrcContainer, mItemTypeMask, mCanWrite);
                        }

                        if (null != mSavedPathList) {
                            for (String path : mSavedPathList) {
                                mAdapter.setSelection(path, true);
                            }
                        }
                        break;
                }
            }
        };


        return new DirectoryContainerLoadCallback()
        {
            @Override
            public void run(DirectoryContainer container)
            {
                synchronized (mSrcContainerLock) {
                    mSrcContainer = container;
                }
                handler.sendEmptyMessage(CREATE_PREVIEW_DONE);
            }
        };
    }


    @Override
    public void onChangePath(String path)
    {
        mPath = path;
        runLoader();
    }


    @Override
    public void onSelectionChanged(
            int position,
            boolean selection)
    {
        mButtonPositive.setEnabled(mAdapter.hasSelectedItems());
    }


    public void setOnSelectionListener(OnSelectionListener onSelectionListener)
    {
        mOnSelectionListener = onSelectionListener;
    }


    public interface OnSelectionListener
    {
        void onSelection(String path);
    }
}
