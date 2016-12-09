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
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.nextgis.libngui.R;

import java.util.ArrayList;
import java.util.List;

import static com.nextgis.libngui.util.ConstantsUI.*;


/**
 * Adapter to represent folders and files
 */
public class LocalResourceListAdapter
        extends ListSelectorAdapter
{
    protected List<LocalResourceListItem> mResources;
    protected OnResClickListener          mOnResClickListener;


    public LocalResourceListAdapter()
    {
        super();
    }


    @Override
    protected int getItemViewResId()
    {
        return R.layout.item_local_resource;
    }


    @Override
    protected ListSelectorAdapter.ViewHolder getViewHolder(View itemView)
    {
        //final Context context = itemView.getContext();

        setOnItemClickListener(new ListSelectorAdapter.ViewHolder.OnItemClickListener()
        {
            @Override
            public void onItemClick(int position)
            {
                LocalResourceListItem item = mResources.get(position);

                if (null != mOnResClickListener) {
                    mOnResClickListener.onResClick(item);
                }
            }
        });

        setOnItemLongClickListener(null);

        return new LocalResourceListAdapter.ViewHolder(
                itemView, mSingleSelectable, mOnItemClickListener, mOnItemLongClickListener);
    }


//    @Override
//    public void setSelection(
//            int position,
//            boolean selection)
//    {
//        LocalResourceListItem item = mResources.get(position);
//
//        switch (item.mType) {
//            case FILETYPE_FOLDER:
//            case FILETYPE_UNKNOWN:
//                selection = false;
//                break;
//        }
//
//        super.setSelection(position, selection);
//    }


    @Override
    public void onBindViewHolder(
            ListSelectorAdapter.ViewHolder holder,
            int position)
    {
        super.onBindViewHolder(holder, position);

        Context context = holder.mCheckBox.getContext();

        LocalResourceListAdapter.ViewHolder viewHolder =
                (LocalResourceListAdapter.ViewHolder) holder;
        LocalResourceListItem item = mResources.get(position);

        switch (item.mType) {
            case FILETYPE_PARENT:
                viewHolder.mFileIcon.setImageDrawable(
                        ContextCompat.getDrawable(context, R.drawable.ic_folder));
                viewHolder.mFileName.setText(context.getString(R.string.up_dots));
                viewHolder.mFileDesc.setText(context.getString(R.string.up));
                break;
            case FILETYPE_FOLDER:
                viewHolder.mFileIcon.setImageDrawable(
                        ContextCompat.getDrawable(context, R.drawable.ic_folder));
                viewHolder.mFileName.setText(item.getFile().getName());
                viewHolder.mFileDesc.setText(context.getString(R.string.folder));
                break;
            case FILETYPE_ZIP:
                viewHolder.mFileIcon.setImageDrawable(
                        ContextCompat.getDrawable(context, R.drawable.ic_zip));
                viewHolder.mFileName.setText(item.getFile().getName());
                viewHolder.mFileDesc.setText(context.getString(R.string.zip_file));
                break;
            case FILETYPE_FB:
                viewHolder.mFileIcon.setImageDrawable(
                        ContextCompat.getDrawable(context, R.drawable.ic_formbuilder));
                viewHolder.mFileName.setText(item.getFile().getName());
                viewHolder.mFileDesc.setText(context.getString(R.string.formbuilder_file));
                break;
            case FILETYPE_GEOJSON:
                viewHolder.mFileIcon.setImageDrawable(
                        ContextCompat.getDrawable(context, R.drawable.ic_geojson));
                viewHolder.mFileName.setText(item.getFile().getName());
                viewHolder.mFileDesc.setText(context.getString(R.string.geojson_file));
                break;
            case FILETYPE_UNKNOWN:
                viewHolder.mFileIcon.setImageDrawable( // TODO: icon for unknown file
                                                       ContextCompat.getDrawable(
                                                               context, R.drawable.ic_formbuilder));
                viewHolder.mFileName.setText(item.getFile().getName());
                viewHolder.mFileDesc.setText(context.getString(R.string.unknown_file));
                break;
        }

        viewHolder.mCheckBox.setVisibility(item.isVisibleCheckBox() ? View.VISIBLE : View.GONE);
        viewHolder.mCheckBox.setEnabled(item.isSelectable());
    }


    public static class ViewHolder
            extends ListSelectorAdapter.ViewHolder
            implements View.OnClickListener
    {
        LinearLayout mItemLayout;
        ImageView    mFileIcon;
        TextView     mFileName;
        TextView     mFileDesc;


        public ViewHolder(
                View itemView,
                boolean singleSelectable,
                OnItemClickListener clickListener,
                OnItemLongClickListener longClickListener)
        {
            super(itemView, singleSelectable, clickListener, longClickListener);

            mItemLayout = (LinearLayout) itemView.findViewById(R.id.item_layout);
            mFileIcon = (ImageView) itemView.findViewById(R.id.file_icon);
            mFileName = (TextView) itemView.findViewById(R.id.file_name);
            mFileDesc = (TextView) itemView.findViewById(R.id.file_desc);
        }
    }


    public void setResources(List<LocalResourceListItem> resources)
    {
        if (resources == mResources) {
            return;
        }

        clearSelectionForAll();

        mResources = resources;
        notifyDataSetChanged();
    }


    @Override
    public long getItemId(int position)
    {
        if (null == mResources) {
            return super.getItemId(position);
        }

        return position;
    }


    @Override
    public int getItemCount()
    {
        if (null == mResources) {
            return 0;
        }
        return mResources.size();
    }


    public List<LocalResourceListItem> getSelectedItems()
    {
        List<Integer> selIds = super.getSelectedItemsIds();
        List<LocalResourceListItem> selRes = new ArrayList<>(selIds.size());
        for (Integer id : selIds) {
            selRes.add(mResources.get(id));
        }
        return selRes;
    }


    public void setOnResClickListener(OnResClickListener listener)
    {
        mOnResClickListener = listener;
    }


    public interface OnResClickListener
    {
        void onResClick(LocalResourceListItem item);
    }
}
