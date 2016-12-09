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

package com.nextgis.libngui.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.nextgis.libngui.R;

import java.io.File;

import static com.nextgis.libngui.util.ConstantsUI.FILETYPE_FOLDER;
import static com.nextgis.libngui.util.ConstantsUI.FILETYPE_PARENT;


/**
 * A path view class. the path is a resources names divide by arrows in head of dialog. If user
 * click on name, the dialog follow the specified path.
 */
public class PathAdapter
        implements LocalResourceListAdapter.OnResClickListener
{
    protected LinearLayout mLinearLayout;
    protected Context      mContext;
    protected File         mPath;

    protected OnPathClickListener mOnPathClickListener;


    public PathAdapter()
    {
    }


    public File getPath()
    {
        return mPath;
    }


    public void setLinearLayout(LinearLayout linearLayout)
    {
        mLinearLayout = linearLayout;
        mContext = linearLayout.getContext();
    }


    public void onUpdate(File path)
    {
        mPath = path;
        if (null == mLinearLayout || null == path) {
            return;
        }
        mLinearLayout.removeAllViewsInLayout();

        File parent = path;
        while (null != parent) {
            final File parentPath = parent;

            LayoutInflater inflater = LayoutInflater.from(mContext);

            TextView nameView =
                    (TextView) inflater.inflate(R.layout.item_path_name, mLinearLayout, false);
            nameView.setText(parent.getName());
            nameView.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View v)
                {
                    onUpdate(parentPath);
                    if (null != mOnPathClickListener) {
                        mOnPathClickListener.onPathClick(parentPath);
                    }
                }
            });
            mLinearLayout.addView(nameView, 0);

            parent = parent.getParentFile();
            if (null != parent) {
                ImageView imageView =
                        (ImageView) inflater.inflate(R.layout.item_path_icon, mLinearLayout, false);
                mLinearLayout.addView(imageView, 0);
            }
        }
    }


    @Override
    public void onResClick(LocalResourceListItem item)
    {
        int type = item.getType();
        if (FILETYPE_PARENT == type || FILETYPE_FOLDER == type) {
            File path = item.getFile();
            onUpdate(path);
            if (null != mOnPathClickListener) {
                mOnPathClickListener.onPathClick(path);
            }
        }
    }


    public void setOnPathClickListener(OnPathClickListener listener)
    {
        mOnPathClickListener = listener;
    }


    public interface OnPathClickListener
    {
        void onPathClick(File path);
    }
}
